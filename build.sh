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
NDK_TOOLCHAIN_BASE=$NDK_BASE/toolchains/$HOST-$NDK_COMPILER_VERSION/prebuilt/$NDK_UNAME-x86
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
\
--enable-version3 \
--enable-gpl \
\
--disable-doc \
--enable-yasm \
\
--disable-decoders \
\
--enable-decoder=aac \
--enable-decoder=amrnb \
--enable-decoder=amrwb \
--enable-decoder=flac \
--enable-decoder=mp3 \
--enable-decoder=vorbis \
--enable-decoder=pcm_s16le \
--enable-decoder=pcm_u16le \
--enable-decoder=pcm_s8 \
--enable-decoder=pcm_u8 \
\
--enable-decoder=mjpeg \
--enable-decoder=gif \
--enable-decoder=png \
--enable-decoder=bmp \
\
--enable-decoder=h263 \
--enable-decoder=h264 \
--enable-decoder=mpeg4 \
--enable-decoder=vp8 \
\
--disable-encoders \
\
--enable-encoder=aac \
\
--enable-encoder=png \
--enable-encoder=mjpeg \
\
--enable-encoder=h263 \
--enable-encoder=mpeg4 \
\
--disable-muxers \
\
--enable-muxer=amr \
--enable-muxer=mp4 \
--enable-muxer=mov \
--enable-muxer=h263 \
--enable-muxer=image2 \
--enable-muxer=mjpeg \
--enable-muxer=h264 \
\
--disable-demuxers \
\
--enable-demuxer=aac \
--enable-demuxer=amr \
--enable-demuxer=flac \
--enable-demuxer=mp3 \
--enable-demuxer=ogg \
--enable-demuxer=wav \
\
--enable-demuxer=mjpeg \
--enable-demuxer=image2 \
\
--enable-demuxer=mov \
\
--disable-parsers \
--enable-parser=aac \
--enable-parser=bmp \
--enable-parser=flac \
--enable-parser=h263 \
--enable-parser=h264 \
--enable-parser=mjpeg \
--enable-parser=mpeg4video \
--enable-parser=mpegaudio \
--enable-parser=mpegvideo \
--enable-parser=png \
--enable-parser=vorbis \
--enable-parser=vp8 \
\
--disable-protocols \
--enable-protocol=cache \
--enable-protocol=concat \
--enable-protocol=file \
--enable-protocol=md5 \
--enable-protocol=pipe \
\
--enable-avresample \
--disable-filter=hqdn3d \
\
--disable-indevs \
--enable-indev=lavfi \
--disable-outdevs \
\
--enable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-network \
\
--enable-zlib
