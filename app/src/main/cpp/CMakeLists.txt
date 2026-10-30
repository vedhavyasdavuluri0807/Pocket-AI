cmake_minimum_required(VERSION 3.22.1)

project("pocket_ai" VERSION 1.0.0 LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configure llama.cpp source path
if (NOT DEFINED LLAMA_SRC)
    if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")
        set(LLAMA_SRC "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")
    elseif (EXISTS "F:/AndroidTools/llama.cpp")
        set(LLAMA_SRC "F:/AndroidTools/llama.cpp")
    else()
        message(FATAL_ERROR "llama.cpp source directory not found! Set LLAMA_SRC or put llama.cpp at ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")
    endif()
endif()

message(STATUS "Pocket AI: Using llama.cpp from ${LLAMA_SRC}")

# Build settings for mobile llama.cpp
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(LLAMA_SUBPROCESS OFF CACHE BOOL "" FORCE)

# Architecture & Optimization flags
if (DEFINED ANDROID_ABI)
    message(STATUS "Pocket AI: Detected Android ABI ${ANDROID_ABI}")
    if (ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_SYSTEM_ARCH "ARM" CACHE STRING "" FORCE)
        set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)
        set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
    elseif (ANDROID_ABI STREQUAL "x86_64")
        set(GGML_SYSTEM_ARCH "x86" CACHE STRING "" FORCE)
        set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)
        set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
    endif()
endif()

set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)

add_subdirectory(${LLAMA_SRC} build-llama)

# Shared JNI Bridge
add_library(pocket_ai_jni SHARED
    pocket_ai_jni.cpp
)

target_include_directories(pocket_ai_jni PRIVATE
    ${LLAMA_SRC}
    ${LLAMA_SRC}/common
    ${LLAMA_SRC}/include
    ${LLAMA_SRC}/ggml/include
    ${LLAMA_SRC}/ggml/src
)

target_link_libraries(pocket_ai_jni
    llama
    llama-common
    android
    log
)
