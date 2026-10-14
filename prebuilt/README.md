# Prebuilt APK

`app-debug.apk` is a ready-to-install build of **IRL RPG — Step 3 (UI polish)**, produced from the source in
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

## Verified build (Step 3)

| | |
| --- | --- |
| File | `app-debug.apk` |
| Size | 11,135,149 bytes |
| SHA-256 | `97d1c94bf7c96bc80d049b2f20615e21af44260f877525e3053b48fdac255d51` |
| Source | commit for Step 3 on `main` |
| Release | https://github.com/botstelegram7-cmyk/irl-rpg/releases/tag/v1.0-step3 |

A release-mode (unsigned) APK is attached to the same GitHub release for anyone who wants to sign it
with their own key.

## Notes

- It is a **debug build**, signed with the standard Android debug key — perfect for testing and
  sharing, but not suitable for publishing to Google Play. For a store build, create your own
  release keystore and add a `signingConfig` to `app/build.gradle.kts`
  (`assembleRelease` already produces an unsigned 7.2 MB APK).
- Fully offline: the app declares **no permissions at all**.
- First launch seeds six starter categories and six skill attributes only — no sample quests, no
  fake XP or achievements. You begin at Level 1 with 0 XP.
