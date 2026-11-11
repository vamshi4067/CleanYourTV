# Setup and recovery

## Requirements

Android TV / Google TV running Android 8.0 (API 26) or later, JDK 17, Android SDK platform 34, Build Tools 34.0.0, and Platform Tools (`adb`). This is not for Roku, Tizen, webOS, or VIDAA. Install and sign into streaming apps separately; no accounts are bundled.

## Build

Set `JAVA_HOME` and `ANDROID_HOME` to your installations. Alternatively, set `sdk.dir` to your SDK path in ignored `launcher/local.properties`.

```sh
cd launcher
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

Use `gradlew.bat` on Windows. Gradle 8.9 is pinned with a distribution checksum. Output is `app/build/outputs/apk/release/app-release.apk` relative to `launcher/`.

The current sideloaded release uses the local Android debug signing key, generated locally and excluded from Git. Keep a stable local signer for updates. An APK with a different signer cannot update an existing installation. Do not remove an active Home to work around a signature mismatch; restore a working Home and preserve your data first. A production release should use separately managed signing and a private key.

## Install and choose Home

Enable debugging on your TV and authorize your computer. For paired wireless debugging use the TV's pairing instructions first. Replace the placeholders below with your own connection details:

```sh
adb connect "<TV_IP>:<ADB_PORT>"
adb devices
adb -s "<TV_SERIAL>" install -r launcher/app/build/outputs/apk/release/app-release.apk
adb -s "<TV_SERIAL>" shell am start -a android.intent.action.MAIN -c android.intent.category.HOME -n com.gathin.tvhome/.MainActivity
```

Select the new launcher in Default apps / Home settings. Where firmware supports it:

```sh
adb -s "<TV_SERIAL>" shell cmd package set-home-activity com.gathin.tvhome/.MainActivity
```

Verify Home, Settings, Inputs, remote navigation, and a reboot. Leave the original launcher enabled until a working replacement is confirmed. Firmware may handle Home differently.

## Theme and cleanup

The customized Home activates when the bundled `cricket/photo-1.jpg` exists. Optional config: `themes/cricket-legends/config.json`. See [theme tooling](../themes/README.md) and read `tools/theme.sh` usage before running it. The sample disables idle photo/clock wallpaper mode.

Cleanup is separate from the build. Follow the original [PROMPT.md](../PROMPT.md) and [restore guide](restore.md). Back up first, disable rather than uninstall, and use a list for your exact firmware. Preserve networking, pairing, Inputs, Settings, Play services, and a working Home. On Google TV, setup components can own saved Wi-Fi; preserve the original Home guard checks. Never open a factory/test app as an alternative launcher.

## Recovery

Choose your original Home in Settings if it remains enabled. If you disabled stock packages, follow `tools/restore.sh` / `restore.ps1` with your own local backup; none is published here. Re-enable the known original launcher before switching Home back. Package names vary by firmware: do not guess them. Do not factory reset for a routine install/update. Turn debugging off and revoke authorizations when setup and recovery verification are complete.
