# Prism Desktop (Windows)

The Windows desktop port is being developed in the `windows-port-source` branch. Android remains in the `app` module; the desktop entry point and JVM-only platform code live in `shared/src/jvmMain`.

## Current desktop functionality

- Compose Desktop window with Home, Search, Library and Settings navigation.
- Add local audio files through the Windows file picker.
- Local playback queue with play/pause, previous/next, seeking and remove.
- Queue and selected index are saved in Java Preferences and restored on launch.
- Import and export local M3U/M3U8 playlists.
- Playback automatically advances to the next item and stops at the end of the queue.
- Java Sound playback with the MP3 SPI decoder dependency.

Supported inputs are WAV, MP3, AIFF and AU, subject to Java Sound decoder support. The current implementation loads a selected track into a Java Sound Clip, so very large files can consume substantial memory.

## Build a Windows app image

Install JDK 21 and Android SDK, then run from the repository root:

```powershell
.\gradlew.bat :shared:createDistributable
```

The generated app image is under `shared/build/compose/binaries/main/app` and includes a bundled Java runtime. GitHub Actions archives it as `Prism-Desktop-Windows.zip` and uploads it as the `Prism-Desktop-Windows` artifact.

For development, launch the app from the repository root:

```powershell
.\gradlew.bat :shared:run
```

## Not implemented yet

This is an incremental port, not a feature-complete Windows release. YouTube Music search/streaming, login and cookies, online playlists, full Prism database integration, media-session/SMTC controls, notifications, tray integration and a signed Windows installer still need implementation. The Android Media3 service is not used by the desktop player.
