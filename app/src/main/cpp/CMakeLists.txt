cmake_minimum_required(VERSION 3.22.1)
project("llama-android")

# Build llama.cpp from source
# The llama.cpp source should be placed at app/src/main/cpp/llama.cpp/
# Clone it: git clone https://github.com/ggerganov/llama.cpp.git app/src/main/cpp/llama.cpp

set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# Core llama.cpp library
add_subdirectory(${LLAMA_DIR} llama.cpp)

# Our JNI bridge library
add_library(llama-android SHARED
        llama-android.cpp)

target_include_directories(llama-android PRIVATE
        ${LLAMA_DIR}/include)

target_link_libraries(llama-android
        llama
        android
        log)
