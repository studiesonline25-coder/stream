# StreamLite Android

StreamLite Android is a tiny RTMP player app for OBS streams.

It avoids FFmpeg and VLC so the APK can stay below a 15 MB target. Playback uses
Android Media3/ExoPlayer with the RTMP datasource. A software-decoder preference is
included for phones whose hardware decoder is unreliable, but the app can only use
software codecs that already exist on the phone.

## OBS Setup

Use a normal RTMP server or relay on your network, then point the app at its RTMP URL.

Example OBS custom stream:

```text
Server:     rtmp://YOUR-SERVER-IP/live
Stream Key: stream
App URL:    rtmp://YOUR-SERVER-IP/live/stream
```

## OBS Output For Maximum Phone Compatibility

- Encoder: x264
- Rate Control: CBR
- Keyframe Interval: 2 seconds
- CPU Usage Preset: veryfast or faster
- Profile: Baseline first, Main if Baseline is too limited
- B-frames: 0
- Video: 1280x720 or lower, 30 FPS
- Audio: AAC LC, stereo, 44.1 kHz or 48 kHz

## Build On GitHub

Push this repo to GitHub, then open:

```text
Actions -> Android APK -> Run workflow
```

The workflow builds the release APK, signs it when signing secrets are configured,
checks that it is below 15 MB, and uploads it as an artifact named:

```text
streamlite-release-apk
```

The APK inside the artifact is signed when secrets are present. Without secrets, the
artifact is still produced as an unsigned CI build:

```text
app-release-unsigned.apk
```

## Official Release Signing

Android release signing is handled by Gradle when these GitHub repository secrets
exist:

```text
ANDROID_SIGNING_KEY_BASE64
ANDROID_SIGNING_STORE_PASSWORD
ANDROID_SIGNING_KEY_ALIAS
ANDROID_SIGNING_KEY_PASSWORD
```

Create a keystore with Android Studio or `keytool`, then base64 encode the keystore
file and store that text in `ANDROID_SIGNING_KEY_BASE64`.

On Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Clipboard
```

If the secrets are present, GitHub Actions uploads a signed release APK. If they are
missing, it still uploads an unsigned APK so CI can keep checking the build and size.

## Size Notes

This project intentionally does not bundle native decoder libraries. That keeps the
APK small, but it means truly broken H.264/AAC decoding on a device cannot be fixed
inside a sub-15 MB app. The app prefers software decoders when requested and falls
back to normal platform decoding when needed.
