#!/bin/bash
set -e

# This script is for local development. 
# GitHub Actions uses the workflow file for optimized builds.

echo "Building Rust core..."

# Check for cargo-ndk
if ! command -v cargo-ndk &> /dev/null; then
    echo "cargo-ndk not found, installing..."
    cargo install cargo-ndk
fi

# Set NDK path if not set
if [ -z "$ANDROID_NDK_HOME" ]; then
    # Try to find NDK in default Android SDK location
    if [ -d "$ANDROID_HOME/ndk" ]; then
        export ANDROID_NDK_HOME=$(ls -d $ANDROID_HOME/ndk/* | sort -V | tail -n 1)
        echo "Found NDK at $ANDROID_NDK_HOME"
    else
        echo "Error: ANDROID_NDK_HOME is not set and could not be found."
        exit 1
    fi
fi

cd rust-core

# Add targets if missing
rustup target add aarch64-linux-android armv7-linux-androideabi

# Build
echo "Building for arm64-v8a and armeabi-v7a..."
cargo ndk -t arm64-v8a -t armeabi-v7a -p 28 build --release

# Copy to jniLibs
echo "Copying libraries to app/src/main/jniLibs..."
mkdir -p ../app/src/main/jniLibs/arm64-v8a
mkdir -p ../app/src/main/jniLibs/armeabi-v7a

cp target/aarch64-linux-android/release/libinsta_core.so ../app/src/main/jniLibs/arm64-v8a/
cp target/armv7-linux-androideabi/release/libinsta_core.so ../app/src/main/jniLibs/armeabi-v7a/

cd ..

# Build Android app
echo "Building Android app..."
chmod +x gradlew
./gradlew assembleDebug
