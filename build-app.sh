#!/bin/bash
set -e

# NDK Kurulumunu kontrol et
if [ -z "$ANDROID_NDK_HOME" ]; then
    export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
fi
echo "Using NDK: $ANDROID_NDK_HOME"

# Rust Toolchain
rustup target add aarch64-linux-android armv7-linux-androideabi

# Cargo NDK Kur (Hızlı yöntem)
cargo install cargo-ndk --version 3.5.5 || true # Zaten varsa hata verme

# Rust Derle
cd rust-core
cargo ndk -t arm64-v8a -p 28 build --release
cargo ndk -t armeabi-v7a -p 28 build --release

# JNI Libs Hazırla
mkdir -p ../app/src/main/jniLibs/arm64-v8a
mkdir -p ../app/src/main/jniLibs/armeabi-v7a
cp target/aarch64-linux-android/release/libinsta_core.so ../app/src/main/jniLibs/arm64-v8a/
cp target/armv7-linux-androideabi/release/libinsta_core.so ../app/src/main/jniLibs/armeabi-v7a/

# Android Build
cd ..
chmod +x gradlew
./gradlew assembleDebug --stacktrace
