# Shelf for Android

A thin Android wrapper (WebView) around the Shelf web app in `app/src/main/assets/index.html`.

## Build the APK
**Android Studio:** File > Open this folder, wait for Gradle sync, then Build > Build Bundle(s) / APK(s) > Build APK(s).
The file is at `app/build/outputs/apk/debug/app-debug.apk`. Copy it to your phone and install it (allow "install unknown apps").

**No Android Studio:** push this folder to a GitHub repo. The included workflow builds the APK; download it from the Actions tab (artifact "shelf-apk").

## Notes
- PDF.js loads from the cdnjs CDN, so the first launch needs internet. To work fully offline, download `pdf.min.js` and `pdf.worker.min.js` (version 3.11.174) into `assets/`, and change the three CDN URLs in `index.html` to `/assets/pdf.min.js` and `/assets/pdf.worker.min.js`.
- App icon: `res/mipmap-*`. A 512px version is included separately for Play Store use.
- This is a debug-signed APK. For the Play Store you need a signed release build.
