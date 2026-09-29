#!/usr/bin/env bash
# Prepare the optional, isolated LLM comparison runtime. No app/journal mutation.
set -euo pipefail
source "$(dirname "$0")/../android-env.sh"
cd "$PROJECT_ROOT"
actual_avd="$(adb -s "$ANDROID_SERIAL" emu avd name | head -n 1 | tr -d '\r')"
[[ "$actual_avd" == "$AVD_NAME" ]] || { echo 'Expected dedicated project emulator.' >&2; exit 1; }
cmake_path="$ANDROID_HOME/cmake/3.22.1/bin/cmake"
ndk_path="$ANDROID_HOME/ndk/28.2.13676358"
if [[ ! -x "$cmake_path" || ! -d "$ndk_path" ]]; then
    sdkmanager 'ndk;28.2.13676358' 'cmake;3.22.1'
fi
mkdir -p .local/hybrid
python3 scripts/hybrid/download-model.py
runtime_revision=391fac16460f15233a7740550d858ac96df3419d
source_dir=".local/hybrid/llama.cpp-$runtime_revision"
if [[ ! -f "$source_dir/CMakeLists.txt" ]]; then
    curl -fL --retry 2 "https://github.com/ggml-org/llama.cpp/archive/$runtime_revision.tar.gz" -o .local/hybrid/llama-source.tar.gz
    tar -xzf .local/hybrid/llama-source.tar.gz -C .local/hybrid
fi
"$cmake_path" -S "$source_dir" -B .local/hybrid/build-android -G Ninja \
    -DCMAKE_MAKE_PROGRAM="$ANDROID_HOME/cmake/3.22.1/bin/ninja" \
    -DCMAKE_TOOLCHAIN_FILE="$ndk_path/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 -DANDROID_STL=c++_static \
    -DGGML_NATIVE=OFF -DGGML_OPENMP=OFF -DGGML_LLAMAFILE=OFF -DLLAMA_OPENSSL=OFF \
    -DBUILD_SHARED_LIBS=OFF -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF \
    -DCMAKE_BUILD_TYPE=Release
"$cmake_path" --build .local/hybrid/build-android --target llama-server -j 8
cp .local/hybrid/build-android/bin/llama-server .local/hybrid/llama-server-android
host_tag=darwin-x86_64
[[ "$(uname -s)" == Linux ]] && host_tag=linux-x86_64
"$ndk_path/toolchains/llvm/prebuilt/$host_tag/bin/llvm-strip" .local/hybrid/llama-server-android
adb -s "$ANDROID_SERIAL" shell mkdir -p /data/local/tmp/notetaker-hybrid
adb -s "$ANDROID_SERIAL" push .local/hybrid/llama-server-android .local/hybrid/Qwen3-0.6B-Q8_0.gguf /data/local/tmp/notetaker-hybrid/
adb -s "$ANDROID_SERIAL" shell chmod 755 /data/local/tmp/notetaker-hybrid/llama-server-android
adb -s "$ANDROID_SERIAL" forward tcp:18089 tcp:18089
printf '%s\n' 'Prepared emulator-only inference; model is outside the APK.'
