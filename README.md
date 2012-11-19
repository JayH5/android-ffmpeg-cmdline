android-ffmpeg-cmdline
======================

Command-line ffmpeg for android (based on https://github.com/guardianproject/android-ffmpeg-java)

Included in this repo is:
-------------------------
1. A build script for ffmpeg for Android (build.sh)

2. Prebuilt static library of ffmpeg for Android (res/raw/ffmpeg)

3. The Java wrapper for the ffmpeg static library, which is really just a cleaned-up version of the guardian project's work.

4. A suggested implementation of how to make a video from a selection of images using this version of ffmpeg (/src/com/example/android\_ffmpeg\_cmdline/VideoMaker.java

5. My attempt at getting JavaCV to do the job (VideoWorker.java). See https://code.google.com/p/javacv/

About this ffmpeg build:
------------------------
- Based on ffmpeg 1.0
- Built using the Android NDK r8c (I have also built it using r8b) with the GCC 4.6 compiler
- A fairly stripped down build. I tried to include only the codecs that Android natively supports (see here: http://developer.android.com/guide/appendix/media-formats.html). Building with all the CODECS results in a file size >8MB, whereas this build is <\4MB.

Other notes:
------------
- While I'm very greatful for the work of the Guardian Project, their implementation of all the command line work is kind of a mess. It needs to be redone. I've tried to clean it up a little.
- The key problem with ffmpeg on command-line is working around the Android permissions. As far as I can see, in Android 4.1, when you run a command line native program it runs as a user of some kind (in the Unix sense) that has virtually no access to any files. I could chmod and gain access to files in the application's directory but only on the internal memory (not the SD card) on my phone.
- Using JavaCV had two problems for me. Firstly, it results in an .apk file size of ~50MB and secondly I couldn't get any of the audio CODECS to work. Trying to set an audio CODEC for the ffmpegFrameRecorder always results in an audioCodecNotFound exception. This is really a pity, because making the video without sound is fairly painless and works quite well.
