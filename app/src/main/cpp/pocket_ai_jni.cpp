#include <android/log.h>
#include <jni.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

#include <atomic>
#include <mutex>
#include <string>
#include <vector>
#include <algorithm>
#include <sstream>

#include "common.h"
#include "sampling.h"
#include "llama.h"
#include "ggml-backend.h"
#include "ggml-cpu.h"

#define LOG_TAG "PocketAiJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::mutex g_error_mtx;
static std::string g_last_error;

static void set_last_error(const std::string& err) {
    std::lock_guard<std::mutex> lock(g_error_mtx);
    g_last_error = err;
    LOGE("PocketAI Error: %s", err.c_str());
}

static std::string get_last_error() {
    std::lock_guard<std::mutex> lock(g_error_mtx);
    return g_last_error;
}

static void pocket_ai_android_log_callback(ggml_log_level level, const char * text, void * /*user_data*/) {
    if (!text) return;
    switch (level) {
        case GGML_LOG_LEVEL_ERROR:
            LOGE("%s", text);
            break;
        case GGML_LOG_LEVEL_WARN:
            LOGW("%s", text);
            break;
        case GGML_LOG_LEVEL_INFO:
        default:
            LOGI("%s", text);
            break;
    }
}

static bool is_valid_utf8(const char *string) {
    if (!string) { return true; }
    const auto *bytes = (const unsigned char *) string;
    int num;
    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            num = 4;
        } else {
            return false;
        }
        bytes += 1;
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                return false;
            }
            bytes += 1;
        }
    }
    return true;
}

struct PocketAiContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    common_sampler* sampler = nullptr;
    std::mutex mtx;
    std::atomic<bool> stop_flag{false};
    int context_length = 2048;
    int thread_count = 4;
    int fd = -1;
};

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeInit(
        JNIEnv *env,
        jclass /*clazz*/,
        jstring jnativeLibDir
) {
    try {
        llama_log_set(pocket_ai_android_log_callback, nullptr);

        if (jnativeLibDir != nullptr) {
            const char* lib_dir = env->GetStringUTFChars(jnativeLibDir, nullptr);
            if (lib_dir && strlen(lib_dir) > 0) {
                LOGI("PocketAI: Loading backends from nativeLibDir: %s", lib_dir);
                ggml_backend_load_all_from_path(lib_dir);
                env->ReleaseStringUTFChars(jnativeLibDir, lib_dir);
            }
        }

        // Always ensure built-in CPU backend is registered
        if (ggml_backend_reg_count() == 0) {
            LOGI("PocketAI: Explicitly registering ggml_backend_cpu_reg()");
            ggml_backend_register(ggml_backend_cpu_reg());
        }

        llama_backend_init();
        LOGI("PocketAI: llama_backend_init() completed successfully. Registered backends: %zu", ggml_backend_reg_count());
        return JNI_TRUE;
    } catch (const std::exception& e) {
        set_last_error(std::string("Init exception: ") + e.what());
        return JNI_FALSE;
    } catch (...) {
        set_last_error("Unknown exception during nativeInit");
        return JNI_FALSE;
    }
}

JNIEXPORT jlong JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeLoadModel(
        JNIEnv *env,
        jclass /*clazz*/,
        jint fd,
        jstring jmodelPath,
        jint contextLength,
        jint threadCount,
        jboolean useGpu
) {
    // Ensure backends are initialized before loading
    if (ggml_backend_reg_count() == 0) {
        LOGI("PocketAI: Registering CPU backend in nativeLoadModel");
        ggml_backend_register(ggml_backend_cpu_reg());
        llama_backend_init();
    }

    std::string path_str;
    if (jmodelPath != nullptr) {
        const char* p = env->GetStringUTFChars(jmodelPath, nullptr);
        if (p && strlen(p) > 0) {
            path_str = p;
        }
        if (p) {
            env->ReleaseStringUTFChars(jmodelPath, p);
        }
    }

    LOGI("PocketAI: nativeLoadModel called: path='%s', fd=%d, contextLength=%d, threads=%d, useGpu=%d",
         path_str.c_str(), fd, contextLength, threadCount, (int)useGpu);

    llama_model_params model_params = llama_model_default_params();
    if (useGpu) {
        model_params.n_gpu_layers = 999;
    }

    llama_model* model = nullptr;

    // Strategy A: Direct filesystem path if non-empty and readable
    if (!path_str.empty() && access(path_str.c_str(), R_OK) == 0) {
        LOGI("PocketAI: [Strategy A] Loading model from filesystem path: %s", path_str.c_str());
        model = llama_model_load_from_file(path_str.c_str(), model_params);
    }

    // Strategy B: Linux /proc/self/fd/%d symlink
    if (!model && fd >= 0) {
        char proc_path[64];
        snprintf(proc_path, sizeof(proc_path), "/proc/self/fd/%d", fd);
        LOGI("PocketAI: [Strategy B] Loading model from procfs: %s", proc_path);
        model = llama_model_load_from_file(proc_path, model_params);
    }

    // Strategy C: Duplicated descriptor via fdopen and llama_model_load_from_file_ptr
    if (!model && fd >= 0) {
        LOGI("PocketAI: [Strategy C] Loading model via fdopen on dup(fd=%d)", fd);
        int dup_fd = dup(fd);
        if (dup_fd >= 0) {
            lseek(dup_fd, 0, SEEK_SET);
            FILE* fp = fdopen(dup_fd, "rb");
            if (fp) {
                model = llama_model_load_from_file_ptr(fp, model_params);
                if (!model) {
                    fclose(fp);
                }
            } else {
                close(dup_fd);
            }
        }
    }

    if (!model) {
        std::string err = "Failed to load model from file descriptor (" + std::to_string(fd) +
                          ") and path ('" + path_str + "'). Registered backends: " +
                          std::to_string(ggml_backend_reg_count());
        set_last_error(err);
        LOGE("PocketAI: %s", err.c_str());
        return 0;
    }

    const int trained_context = llama_model_n_ctx_train(model);
    int target_ctx = contextLength;
    if (target_ctx <= 0 || target_ctx > trained_context) {
        target_ctx = (trained_context > 0) ? std::min(trained_context, 4096) : 2048;
    }

    int threads = threadCount;
    if (threads <= 0) {
        threads = std::max(2, (int)sysconf(_SC_NPROCESSORS_ONLN) - 2);
    }

    LOGI("PocketAI: Configuring context: n_ctx=%d (trained=%d), threads=%d", target_ctx, trained_context, threads);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = target_ctx;
    ctx_params.n_batch = 512;
    ctx_params.n_ubatch = 512;
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;

    llama_context* ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        llama_model_free(model);
        std::string err = "Failed to initialize llama_context from model";
        set_last_error(err);
        return 0;
    }

    common_params_sampling sparams;
    sparams.temp = 0.7f;
    sparams.top_k = 40;
    sparams.top_p = 0.95f;

    common_sampler* sampler = common_sampler_init(model, sparams);
    if (!sampler) {
        llama_free(ctx);
        llama_model_free(model);
        std::string err = "Failed to initialize common_sampler";
        set_last_error(err);
        return 0;
    }

    PocketAiContext* pctx = new PocketAiContext();
    pctx->model = model;
    pctx->ctx = ctx;
    pctx->sampler = sampler;
    pctx->context_length = target_ctx;
    pctx->thread_count = threads;
    pctx->fd = fd;

    LOGI("PocketAI: Model loaded successfully! Handle=%p", pctx);
    return reinterpret_cast<jlong>(pctx);
}

JNIEXPORT jlong JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeInitModel(
        JNIEnv *env,
        jclass clazz,
        jint fd,
        jint contextLength,
        jint threadCount,
        jboolean useGpu
) {
    return Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeLoadModel(
            env, clazz, fd, nullptr, contextLength, threadCount, useGpu);
}

JNIEXPORT jint JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeGenerate(
        JNIEnv *env,
        jclass /*clazz*/,
        jlong handle,
        jstring jprompt,
        jint maxTokens,
        jfloat temperature,
        jobject callback
) {
    if (handle == 0) {
        set_last_error("Invalid null handle passed to nativeGenerate");
        return -1;
    }

    auto* pctx = reinterpret_cast<PocketAiContext*>(handle);
    std::lock_guard<std::mutex> lock(pctx->mtx);

    pctx->stop_flag.store(false);

    if (jprompt == nullptr || callback == nullptr) {
        set_last_error("Prompt or callback is null");
        return -1;
    }

    const char* prompt_chars = env->GetStringUTFChars(jprompt, nullptr);
    if (!prompt_chars) {
        set_last_error("Failed to get prompt UTF chars");
        return -1;
    }
    std::string prompt(prompt_chars);
    env->ReleaseStringUTFChars(jprompt, prompt_chars);

    jclass callback_class = env->GetObjectClass(callback);
    jmethodID on_token_mid = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)Z");
    if (!on_token_mid) {
        set_last_error("Failed to find onToken(String) method in callback");
        return -1;
    }

    // Configure sampler temperature
    common_params_sampling sparams;
    sparams.temp = (temperature >= 0.0f) ? temperature : 0.7f;
    sparams.top_k = 40;
    sparams.top_p = 0.95f;

    if (pctx->sampler) {
        common_sampler_free(pctx->sampler);
    }
    pctx->sampler = common_sampler_init(pctx->model, sparams);
    if (!pctx->sampler) {
        set_last_error("Failed to re-initialize sampler");
        return -2;
    }

    // Clear KV cache for a clean generation cycle
    llama_memory_clear(llama_get_memory(pctx->ctx), false);

    // Tokenize prompt
    auto tokens = common_tokenize(pctx->ctx, prompt, true, true);
    if (tokens.empty()) {
        set_last_error("Prompt tokenization resulted in 0 tokens");
        return 0;
    }

    // Headroom check against context size
    const int max_prompt_len = pctx->context_length - 32;
    if ((int)tokens.size() > max_prompt_len) {
        LOGW("PocketAI: Prompt size (%d) exceeds max allowed (%d). Truncating.", (int)tokens.size(), max_prompt_len);
        tokens.erase(tokens.begin(), tokens.begin() + (tokens.size() - max_prompt_len));
    }

    // Ingest prompt tokens in batches
    common_batch batch(pctx->ctx);
    const int BATCH_CHUNK = 512;
    for (size_t i = 0; i < tokens.size(); i += BATCH_CHUNK) {
        size_t chunk_size = std::min(tokens.size() - i, (size_t)BATCH_CHUNK);
        batch.clear();
        for (size_t j = 0; j < chunk_size; j++) {
            llama_token tok = tokens[i + j];
            llama_pos pos = (llama_pos)(i + j);
            bool want_logit = (i + j == tokens.size() - 1);
            batch.add(tok, pos, 0, want_logit);
        }
        if (llama_process(pctx->ctx, LLAMA_PROCESS_TYPE_DECODE, batch.get()) != 0) {
            set_last_error("Prompt decode failed during batch ingest");
            return -3;
        }
    }

    int current_pos = (int)tokens.size();
    int generated_count = 0;
    const int limit = (maxTokens > 0) ? maxTokens : 512;
    std::string pending_utf8;

    while (generated_count < limit && current_pos < pctx->context_length - 4 && !pctx->stop_flag.load()) {
        llama_token next_token = common_sampler_sample(pctx->sampler, pctx->ctx, -1);
        common_sampler_accept(pctx->sampler, next_token, true);

        if (llama_vocab_is_eog(llama_model_get_vocab(pctx->model), next_token)) {
            LOGI("PocketAI: Generated EOG token %d. Halting.", next_token);
            break;
        }

        std::string piece = common_token_to_piece(pctx->ctx, next_token, false);
        pending_utf8 += piece;

        if (is_valid_utf8(pending_utf8.c_str())) {
            jstring jpiece = env->NewStringUTF(pending_utf8.c_str());
            jboolean should_continue = env->CallBooleanMethod(callback, on_token_mid, jpiece);
            env->DeleteLocalRef(jpiece);
            pending_utf8.clear();

            if (env->ExceptionCheck()) {
                env->ExceptionClear();
                break;
            }
            if (!should_continue || pctx->stop_flag.load()) {
                break;
            }
        }

        batch.clear();
        batch.add(next_token, (llama_pos)current_pos, 0, true);
        if (llama_process(pctx->ctx, LLAMA_PROCESS_TYPE_DECODE, batch.get()) != 0) {
            LOGW("PocketAI: Token decode failed at position %d", current_pos);
            break;
        }

        current_pos++;
        generated_count++;
    }

    if (!pending_utf8.empty() && is_valid_utf8(pending_utf8.c_str()) && !pctx->stop_flag.load()) {
        jstring jpiece = env->NewStringUTF(pending_utf8.c_str());
        env->CallBooleanMethod(callback, on_token_mid, jpiece);
        env->DeleteLocalRef(jpiece);
    }

    LOGI("PocketAI: Generation completed. Generated %d tokens.", generated_count);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeStop(
        JNIEnv * /*env*/,
        jclass /*clazz*/,
        jlong handle
) {
    if (handle != 0) {
        auto* pctx = reinterpret_cast<PocketAiContext*>(handle);
        pctx->stop_flag.store(true);
        LOGI("PocketAI: nativeStop requested for handle=%p", pctx);
    }
}

JNIEXPORT void JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeFreeModel(
        JNIEnv * /*env*/,
        jclass /*clazz*/,
        jlong handle
) {
    if (handle == 0) return;

    auto* pctx = reinterpret_cast<PocketAiContext*>(handle);
    pctx->stop_flag.store(true);

    {
        std::lock_guard<std::mutex> lock(pctx->mtx);
        if (pctx->sampler) {
            common_sampler_free(pctx->sampler);
            pctx->sampler = nullptr;
        }
        if (pctx->ctx) {
            llama_free(pctx->ctx);
            pctx->ctx = nullptr;
        }
        if (pctx->model) {
            llama_model_free(pctx->model);
            pctx->model = nullptr;
        }
        if (pctx->fd >= 0) {
            close(pctx->fd);
            pctx->fd = -1;
        }
    }

    delete pctx;
    LOGI("PocketAI: Successfully released native model resources");
}

JNIEXPORT jstring JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeGetLastError(
        JNIEnv *env,
        jclass /*clazz*/
) {
    return env->NewStringUTF(get_last_error().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_pocketai_assistant_inference_engine_LlamaNativeBridge_nativeGetSystemInfo(
        JNIEnv *env,
        jclass /*clazz*/
) {
    return env->NewStringUTF(llama_print_system_info());
}

} // extern "C"
