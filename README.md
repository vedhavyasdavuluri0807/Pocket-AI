# Pocket AI

> **Your personal AI. Always in your pocket.**

Pocket AI is an open-source, fully offline Android application built to deliver private, on-device artificial intelligence conversations. By embedding the high-performance `llama.cpp` inference engine directly into the application process via the Java Native Interface (JNI), Pocket AI enables direct execution of quantized GGUF language models on modern ARM64 mobile hardware without transmitting a single byte over the network.

---

## 📖 Project Overview

Most mobile AI applications rely on remote cloud endpoints, subscription accounts, and server-side logging. Pocket AI is engineered on an opposing premise: **absolute privacy through complete offline isolation**.

- **Platform**: Android 8.0+ (API level 26+), optimized for 64-bit ARM devices (`arm64-v8a`)
- **Language**: Kotlin & Modern C++ (C++17)
- **UI Framework**: Jetpack Compose with Material 3 design principles
- **Native Inference Engine**: Embedded `llama.cpp` compiled via the Android NDK
- **Supported Format**: GGUF (GPT-Generated Unified Format)
- **Local Persistence**: Room SQLite Database & Jetpack DataStore Preferences
- **File Interoperability**: Android Storage Access Framework (SAF)

---

## ✨ Features

### Currently Implemented

- 💬 **Private Conversational Interface**: Fluid, single-thread chat experience powered by Jetpack Compose with auto-scrolling, streaming token updates, and markdown-styled code blocks.
- 🧠 **Embedded Native Inference**: Real in-process token generation via `llama.cpp` and `ggml`, with zero cloud backend or network server requirements.
- 📂 **Storage Access Framework (SAF) File Selection**: Select any user-provided `.gguf` model from local device storage or SD cards without duplicate file copying.
- 🔍 **GGUF Header Validation**: Probing of magic bytes (`GGUF`), file versioning, architecture identification, tensor counts, and metadata keys before model loading.
- 🛡️ **Device RAM Safety Guard**: Proactive memory headroom analysis against system RAM to warn users and prevent Out-Of-Memory (OOM) operating system kills.
- ⚙️ **Configurable Generation Parameters**: Fine-tune context window length, CPU inference threads, sampling temperature, and maximum token output limits.
- 📱 **Modern Android Design**: Rounded cards, pill controls, clean typography, system insets handling, and edge-to-edge layout with full Light and Dark theme support.
- 💾 **Local Conversation Persistence**: Messages, conversation status, and model preferences stored purely in an encrypted-ready local SQLite database via Room.
- 🤖 **Original Mascot & Adaptive Icon**: Custom Pocket AI robot companion branding integrated across all Android launcher densities.

### Planned Functionality (Roadmap)

- 📎 **Multimodal Vision Models**: Integration of vision-language models for offline image understanding.
- 📄 **On-Device Document RAG**: Local text extraction (PDF, TXT, DOCX) and vector embeddings for offline retrieval-augmented generation.
- ⚡ **GPU Acceleration**: OpenCL / Vulkan backend evaluation for Qualcomm Adreno and ARM Mali mobile GPUs.
- 🧩 **Multi-Conversation Management**: Drawer navigation for organizing distinct chat sessions and topics.

---

## 📸 Screenshots

| Chat Interface | Model Management | Settings & Preferences |
| :---: | :---: | :---: |
| _[Screenshot Placeholder: Chat Screen]_ | _[Screenshot Placeholder: Model Management Screen]_ | _[Screenshot Placeholder: Settings Screen]_ |

*(Screenshots will be added as physical test device capture passes are recorded.)*

---

## 🏛️ Architecture Overview

Pocket AI follows the official Android architecture guidelines, separating UI, Domain, and Data layers:

```
┌────────────────────────────────────────────────────────┐
│               Jetpack Compose UI Layer                 │
│    (ChatScreen, ModelManagementScreen, SettingsScreen)  │
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────▼────────────────────────────┐
│                  ViewModel Layer                       │
│    (ChatViewModel, ModelManagementViewModel, etc.)     │
└──────────────┬────────────────────────────┬────────────┘
               │                            │
┌──────────────▼────────────┐  ┌────────────▼────────────┐
│   Local Data Layer        │  │ Inference Engine Layer  │
│  - Room Database (SQLite) │  │  - ModelManager         │
│  - DataStore Preferences  │  │  - GgufValidator        │
│  - Storage Access (SAF)   │  │  - MemorySafetyGuard    │
└───────────────────────────┘  └────────────┬────────────┘
                                            │
                               ┌────────────▼────────────┐
                               │     LlamaCppEngine      │
                               │   (Kotlin Coroutines)   │
                               └────────────┬────────────┘
                                            │ JNI
                               ┌────────────▼────────────┐
                               │   libpocket_ai_jni.so   │
                               └────────────┬────────────┘
                                            │ C++ Call
                               ┌────────────▼────────────┐
                               │        llama.cpp        │
                               │  (libllama / libggml)   │
                               └─────────────────────────┘
```

1. **Presentation Layer**: Jetpack Compose declarative components collect reactive state flows and deliver user actions to ViewModels.
2. **Domain & Data Layer**: `ChatRepositoryImpl` records user inputs, orchestrates conversation history, builds prompts, and manages streaming tokens.
3. **Inference Runtime**: `LlamaCppEngine` manages model lifecycle states (`Unloaded`, `Loading`, `Loaded`, `Generating`, `Error`) and communicates across JNI to native C++ libraries.

---

## 💻 Technology Stack

- **Core**: Kotlin 1.9.24, Java 17, Android SDK (API 26–34)
- **NDK & Native**: Android NDK r26b (`26.1.10909125`), CMake 3.22.1, C++17
- **UI & Layout**: Jetpack Compose, Material 3, Navigation Compose
- **Concurrency**: Kotlin Coroutines & Kotlin Flow
- **Persistence**: Android Room 2.6.1, Jetpack DataStore Preferences 1.0.0
- **Native Core**: Upstream `llama.cpp` and `ggml`

---

## 📁 Project Directory Structure

```
Pocket-AI/
├── .github/
│   └── ISSUE_TEMPLATE/           # Bug report and feature request templates
├── app/
│   ├── build.gradle.kts          # App-level build and CMake externalNativeBuild configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── cpp/              # C++ JNI Bridge & CMake build script
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   └── pocket_ai_jni.cpp
│   │   │   ├── java/com/pocketai/assistant/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── PocketAIApp.kt
│   │   │   │   ├── attachments/  # File and document handling stubs
│   │   │   │   ├── data/         # Room Database, DAOs, Entities, Preferences
│   │   │   │   ├── domain/       # Domain interfaces, models, and types
│   │   │   │   ├── inference/    # ModelManager, GgufValidator, MemorySafetyGuard, PromptBuilder
│   │   │   │   │   └── engine/   # LlamaCppEngine and LlamaNativeBridge
│   │   │   │   ├── navigation/   # Compose navigation routing
│   │   │   │   └── ui/           # Compose screens, components, and design themes
│   │   │   ├── jniLibs/          # Compiled ARM64 native shared libraries
│   │   │   │   └── arm64-v8a/
│   │   │   └── res/              # Android XML drawables, mipmaps, and mascot assets
│   │   └── test/                 # JVM Unit Tests
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/                  # Gradle Wrapper binaries and properties
├── build.gradle.kts              # Root project build configuration
├── settings.gradle.kts           # Gradle settings and repositories
├── gradle.properties             # JVM arguments and AndroidX flags
├── CONTRIBUTING.md               # Developer and community contribution guidelines
└── README.md
```

---

## 🛠️ Requirements & Development Environment

To build Pocket AI from source, ensure your environment meets the following specifications:

- **Operating System**: Windows 10/11 (or macOS / Linux with appropriate SDK paths)
- **JDK**: OpenJDK 17 (e.g. Microsoft JDK 17 or Eclipse Temurin 17)
- **Android SDK**: Build-Tools `34.0.0`, Platform `android-34`
- **Android NDK**: Version `26.1.10909125` (NDK r26b)
- **CMake**: Version `3.22.1`
- **Git**: Git 2.30+

---

## 🔨 Build Instructions (Windows)

1. **Clone the Repository**:
   ```powershell
   git clone https://github.com/vedhavyasdavuluri0807/Pocket-AI.git
   cd Pocket-AI
   ```

2. **Configure SDK & NDK Path**:
   Create a `local.properties` file in the root directory:
   ```properties
   sdk.dir=C:/Path/To/Your/android-sdk
   ndk.dir=C:/Path/To/Your/android-sdk/ndk/26.1.10909125
   ```

3. **Run Unit Tests**:
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```

4. **Compile Debug APK**:
   ```powershell
   .\gradlew.bat assembleDebug
   ```

   The generated APK will be output to:
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

---

## 📥 Installation

1. Copy `app-debug.apk` to your Android device via USB cable, ADB, or file transfer.
2. Install via ADB:
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
3. Alternatively, open the APK file directly in the device's file manager and accept the "Install from unknown sources" prompt.

---

## 📦 Model Selection & Loading Instructions

Pocket AI **does not bundle model weights** inside the APK to respect storage limits and give users freedom of choice.

1. **Acquire a Compatible GGUF Model**:
   - Download any quantized GGUF model designed for chat (such as SmolLM2-360M-Instruct, Qwen2.5-0.5B-Instruct, Llama-3.2-1B-Instruct, or Danube3-500M) from Hugging Face.
   - For smooth performance on mid-range mobile CPUs, models between 300M and 1.5B parameters quantized at `Q4_K_M` or `Q4_0` are recommended.
2. **Transfer to Device**:
   - Save the `.gguf` file to your device's internal storage (e.g., `Download` or `Documents` folder).
3. **Select in Pocket AI**:
   - Open Pocket AI and navigate to **AI Models** (or tap the memory chip icon in the top bar).
   - Tap **Select Model File** and select your `.gguf` file using Android's system file picker.
   - Pocket AI validates the header and displays file specifications.
4. **Load the Model**:
   - Tap **Load Model**. The app memory-maps the model tensors and prepares the sampling context.
   - Return to the Chat screen and start conversing fully offline.

---

## ⚙️ Native llama.cpp Integration Details

Pocket AI compiles official `llama.cpp` sources directly using Android NDK r26b with the following native architecture:

- **Target ABI**: `arm64-v8a` (64-bit ARM).
- **Compute Kernels**: Optimized ARM NEON tensor math (`libggml`, `libggml-cpu`, `libggml-base`).
- **Threading Engine**: Built-in POSIX/C++ `std::thread` pool (`GGML_OPENMP=OFF`) to ensure low overhead across mobile big.LITTLE core configurations without runtime OpenMP dependencies.
- **Three-Tier SAF Access**:
  1. *Direct filesystem path* (used for app-local and cached models).
  2. *Procfs descriptor mapping* (`/proc/self/fd/<fd>`) for direct kernel mmap without copying multi-gigabyte models into private app storage.
  3. *File pointer streaming* (`fdopen(dup(fd), "rb")` via `llama_model_load_from_file_ptr`) as a fallback if OEM SELinux policies restrict procfs access.
- **Packaged Native Shared Libraries**:
  - `libpocket_ai_jni.so`: Dedicated JNI bridge exposing thread-safe C++ bindings.
  - `libllama.so`: Primary inference execution graph.
  - `libllama-common.so`: Tokenizer, chat templates, and sampling algorithms.
  - `libggml-base.so`, `libggml-cpu.so`, `libggml.so`: Tensor math and architecture operations.

---

## 🔒 Offline & Privacy Architecture

- **Zero Cloud Network Calls**: Pocket AI does not connect to any remote server or inference API.
- **No Internet Permission**: Normal chat and model evaluation require zero internet privileges.
- **No Telemetry or Tracking**: No analytics libraries, crash-reporting SDKs, or advertising trackers are included.
- **On-Device Storage**: All conversation histories and user preferences remain inside SQLite databases in the application's private storage sandbox (`/data/data/com.pocketai.assistant/`).
- **User Ownership**: You choose the model, provide the files, and control your data.

---

## 📊 Current Development Status

- **Architecture & UI**: Functional and verified in tests.
- **Native NDK Build**: Verified. All 6 native shared libraries compile and package into the debug APK.
- **Physical Device Validation**: In progress. Native libraries have been compiled, packaged, and verified via automated build pipelines; end-to-end token generation on physical ARM64 hardware remains subject to real-device memory headroom, thermal limits, and model size.

---

## ⚠️ Known Limitations

- **RAM Requirements**: Large language models require substantial continuous physical RAM. Attempting to load models larger than the device's free RAM will trigger the memory safety guard or OS low-memory termination.
- **Single Active Architecture**: Currently optimized and filtered for 64-bit ARM (`arm64-v8a`). 32-bit ARM and x86 emulators are not currently supported by default native build filters.
- **Inference Speed**: Generation speed depends strictly on device CPU clock speeds, core architecture, and memory bandwidth.

---

## 🗺️ Roadmap

- [x] Modern Android interface with clean, minimal design system
- [x] Storage Access Framework (SAF) GGUF selection and header validation
- [x] Memory safety checks and headroom calculation
- [x] Official `llama.cpp` CMake NDK integration and JNI bridge
- [x] Token streaming to Jetpack Compose UI
- [ ] Hardware GPU backend acceleration (OpenCL / Vulkan)
- [ ] Multi-turn session export and backup
- [ ] Support for vision-language models
- [ ] Local document text indexing and retrieval (RAG)

---

## 🤝 Contribution Guidelines

Contributions from developers, on-device AI enthusiasts, and designers are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) for branch management, code style, and pull request procedures.

---

## 📄 License

Pocket AI is currently under active open-source development. A formal software license will be established as part of the initial general release. Upstream `llama.cpp` and `ggml` components remain subject to the MIT License.
