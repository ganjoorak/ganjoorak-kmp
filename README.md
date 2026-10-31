# ganjoorak-kmp

کلاینت Kotlin Multiplatform گنجورک (اندروید و دسکتاپ).

وب: [`ganjoorak/website`](https://github.com/ganjoorak/website) · مجوز: [AGPL-3.0](LICENSE)

---

A Kotlin Multiplatform + Compose Multiplatform client for [Ganjoorak](https://ganjoorak.ir), sharing one codebase
across Android and desktop (Linux, Windows).

## Features

- **Home feed**: swipe vertically through random poems, optionally from a single poet.
- **Poem reader**: proper couplet layout (side by side on wide screens, stacked on phones), band and free-verse
  poems, previous/next navigation, save, share and copy.
- **Recitations**: play Ganjoor recitations with the current verse highlighted and followed; tap a verse to jump to it
  (Android). Desktop opens recitations in the browser.
- **Poets**: all poets grouped by century with search, poet pages with their books and poems, category browsing.
- **Search**: full-text search with paging and highlighted matches, plus semantic (meaning-based) search.
- **Saved poems** and recent searches, stored on the device.
- **Settings**: auto/light/dark/paper themes, four Persian fonts (Vazirmatn, Samim, Shabnam, Gandom), text size,
  couplet layout, verse numbers.
- Right-to-left throughout; a navigation rail replaces the bottom bar on wide windows.

## Structure

```
composeApp/   shared code (KMP library): data layer, UI, platform bits (androidMain, desktopMain)
androidApp/   Android application shell
```

## Build

Requires JDK 17+ (21 recommended) and, for Android, the Android SDK.

```bash
./gradlew :composeApp:desktopTest          # unit tests
./gradlew :composeApp:run                  # run the desktop app
./gradlew :androidApp:assembleDebug        # Android APK
./gradlew :composeApp:packageDeb           # Linux .deb (run on Linux)
./gradlew :composeApp:packageAppImage && bash scripts/package-appimage.sh  # Linux AppImage
./gradlew :composeApp:packageMsi           # Windows .msi (run on Windows)
```

CI (`.github/workflows/build.yml`) runs the tests and builds the APKs, the `.deb`, the AppImage and the `.msi` as artifacts.
