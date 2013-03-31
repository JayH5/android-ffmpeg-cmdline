#!/bin/bash

# set the base path to your Android NDK (or export NDK to environment)

if [[ "x$NDK_BASE" == "x" ]]; then
    NDK_BASE=/opt/android-ndk
    echo "No NDK_BASE set, using $NDK_BASE"
fi

NDK_PLATFORM_VERSION=8
NDK_ABI=arm
NDK_COMPILER_VERSION=4.6
NDK_SYSROOT=$NDK_BASE/platforms/android-$NDK_PLATFORM_VERSION/arch-$NDK_ABI
NDK_UNAME=`uname -s | tr '[A-Z]' '[a-z]'` # Convert Linux -> linux
HOST=$NDK_ABI-linux-androideabi
NDK_TOOLCHAIN_BASE=$NDK_BASE/toolchains/$HOST-$NDK_COMPILER_VERSION/prebuilt/$NDK_UNAME-x86_64
CC="$NDK_TOOLCHAIN_BASE/bin/$HOST-gcc --sysroot=$NDK_SYSROOT"
LD=$NDK_TOOLCHAIN_BASE/bin/$HOST-ld

BUILD_PATH=build/ffmpeg

./configure \
$DEBUG_FLAG \
--arch=arm \
--target-os=linux \
--enable-runtime-cpudetect \
--enable-pic \
--disable-shared \
--enable-static \
--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/$NDK_ABI-linux-androideabi- \
--sysroot="$NDK_SYSROOT" \
--extra-cflags="-march=armv6" \
--extra-ldflags="" \
--enable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-network \
