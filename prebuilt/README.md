# Prebuilt APK

`app-debug.apk` is a ready-to-install build of **IRL RPG — Step 1**, produced from the source in
this repository with:

```bash
./gradlew assembleDebug
```

## Install

1. Copy the APK to your Android phone (or download it there directly).
2. Allow installation from unknown sources for whichever app opens the file
   (Settings → Apps → Special access → Install unknown apps).
3. Tap the APK and install, then open **IRL RPG**.

Requires **Android 7.0 (API 24)** or newer.

## Notes

- It is a **debug build**, signed with the standard Android debug key — perfect for testing and
  sharing, but not suitable for publishing to Google Play. For a store build, create your own
  release keystore and add a `signingConfig` to `app/build.gradle.kts`
  (`assembleRelease` already produces an unsigned 7.2 MB APK).
- Fully offline: the app declares **no permissions at all**.
- First launch seeds six starter categories and six skill attributes only — no sample quests, no
  fake XP or achievements. You begin at Level 1 with 0 XP.
