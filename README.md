android-ffmpeg-cmdline
======================

Command-line ffmpeg for android (very loosely based on https://github.com/guardianproject/android-ffmpeg-java).
This implementation has a number of (fairly severe) issues (see 'Other notes') but for a quick-and-dirty application of ffmpeg it may be useful.

![Demo screen](/screenshots/2013-04-01 00.44.25.png)

Included in this repo is:
-------------------------
1. A build script for ffmpeg for Android (build.sh)

2. Prebuilt static library of ffmpeg for Android (res/raw/ffmpeg)

3. The Java wrapper for the ffmpeg static library, which is really just a wrapper for access to the commandline through Java's Process class.

4. A demo app. The screenshot should explain everything. (Requires Android 2.3)

About this ffmpeg build:
------------------------
- Based on ffmpeg 1.2
- Built using the Android NDK r8e with the GCC 4.6 compiler (should be able to build back to r8b by changing build script to use x86 instead of x86_64)
- This is the standard ffmpeg build (so with all the options switched on). By switching off options in the build script you could get the binary file size down to below 4MB from 8MB.

Other notes:
------------
- The key problem with ffmpeg on command-line is working around the Android permissions. As far as I can see, in Android 4.1, when you run a command line native program it runs as a user of some kind (in the Unix sense) that has virtually no access to any files. I could chmod and gain access to files in the application's directory but only on the internal memory (not the SD card) on my phone.
- The binary file size is fairly large - 8MB - and there have to be 2 copies of it once the app is installed.
- There are no nice callbacks from the Java Process that things have gone well or not. We only have stdout and stderr to work with.
