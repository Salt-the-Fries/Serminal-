# Serminal-

Serminal- is an Android-first hybrid terminal: a custom command layer backed by real Android shell execution, with a glassmorphism interface and an ambient jellyfish video backdrop.

## Current foundation

- Kotlin + Jetpack Compose Android application
- Media3-powered looping jellyfish video background
- Adjustable background blur and shade controls
- Frosted-glass terminal panel
- Initial Serminal commands: `help`, `clear`, `pwd`, `ls`, `cd`, `mkdir`, `cat`, and `echo`
- Other entered commands are passed to Android's `/system/bin/sh`

## Build

Open this repository in Android Studio with JDK 17 and Android SDK 35 installed. Allow Gradle sync, then run the `app` configuration on an Android device or emulator.

The current background player streams `264272_medium.mp4` from this repository's `main` branch, so the first launch requires an internet connection. A later milestone should add local asset caching and background selection.

## Important limitations

This is the first application foundation, not a full Linux distribution. Android shell commands run with the app's normal Android UID and permissions. PRoot/Linux userspace, interactive PTY sessions, robust shell parsing, command cancellation, and persistent multi-tab sessions are planned follow-up work.

## Project direction

- Platform: Android
- Architecture: hybrid custom command engine + real system shell
- UI: dark glassmorphism with animated jellyfish ambience
- Package: `com.saltyfries.serminal`
