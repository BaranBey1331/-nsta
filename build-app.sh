#!/bin/bash
# set -e (Hata olsa da devam etmesi için kapalı tutuyoruz teşhis amaçlı)

echo "--- Debug: Java & Android Home ---"
echo "JAVA_HOME: $JAVA_HOME"
echo "ANDROID_HOME: $ANDROID_HOME"

echo "--- Debug: Rust ---"
rustc --version
cargo --version

# NDK (En yeni olanı bul)
NDK_DIR=$(ls -d $ANDROID_HOME/ndk/* 2>/dev/null | sort -V | tail -n 1)
export ANDROID_NDK_HOME=$NDK_DIR
echo "NDK_HOME: $ANDROID_NDK_HOME"

# JNI Libs (Empty placeholder if rust fails)
mkdir -p app/src/main/jniLibs/arm64-v8a
mkdir -p app/src/main/jniLibs/armeabi-v7a
touch app/src/main/jniLibs/arm64-v8a/libplaceholder.so

# Rust Derlemeyi Dene
cd rust-core
rustup target add aarch64-linux-android armv7-linux-androideabi
cargo install cargo-ndk || echo "Cargo NDK install failed, skipping Rust build"
cargo ndk -t arm64-v8a -p 28 build --release && cp target/aarch64-linux-android/release/libinsta_core.so ../app/src/main/jniLibs/arm64-v8a/ || echo "Rust ARM64 failed"
cargo ndk -t armeabi-v7a -p 28 build --release && cp target/armv7-linux-androideabi/release/libinsta_core.so ../app/src/main/jniLibs/armeabi-v7a/ || echo "Rust ARMv7 failed"

# Android Derleme (Asıl önemli kısım)
cd ..
chmod +x gradlew
./gradlew assembleDebug --stacktrace
