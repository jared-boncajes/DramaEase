# DramaEase

A simple, Netflix-style Android TV app made for an elderly viewer on a **Sony BRAVIA (Android TV / Google TV)**.
Kotlin · Jetpack Compose for TV · Media3/ExoPlayer · Room · DataStore · Coil · Coroutines · Gradle Kotlin DSL.

> **Read this first — what is and isn't real**
> * **Working & built:** the whole native app (Home, details, episodes, search, favorites, continue watching, settings, player, English/Filipino, large text, high contrast) with a **Demo** catalog of fictional dramas playing openly licensed sample videos. It compiles, 11 unit tests pass, and a signed release APK builds. **It has not yet been run on an emulator or TV** (see `docs/TEST_RESULTS.md`).
> * **KissKH (`https://kisskh.do`) — NOT natively integrated.** The site answers with a Cloudflare anti-bot challenge and has no documented API or licence for third-party apps, so DramaEase does **not** fetch its catalog or play its streams. What exists: a family-curated list of kisskh.do links shown in the native interface, with an **Open the Website** button that needs a TV browser. Details: `docs/KISSKH_ASSESSMENT.md`.
> * Demo mode is practice content only and is labeled "Practice mode" on screen. It is not a KissKH integration.

## The aunt's flow
Open DramaEase → **Continue Watching** (first row, resumes directly) → watch. Or: pick a drama → the big button is already selected → OK.
Back always goes up one step. Easy Mode (default): large text, high contrast, fewer buttons, friendly labels.

## Project layout
```
app/src/main/java/com/dramaease/tv/
  model/      Drama, Episode, PlaybackOption (NativeStream | ExternalWebsite | Unavailable)
  provider/   DramaContentProvider + DemoContentProvider + KissKhContentProvider (+ KissKhLibrary parser)
  data/       Room (watch entries, favorites, history), DataStore settings, ProgressRules, ResumeLogic, Library
  ui/         theme, Strings (EN/FIL), components, screens, ViewModels
  player/     Media3 player screen (controls, subtitles, progress saving, website-only fallback)
docs/         KISSKH_ASSESSMENT, BUILD_AND_INSTALL (emulator, BRAVIA, ADB, signing), TEST_RESULTS
scripts/      install-to-tv.bat / .sh
```
Adding a real source later = implement `DramaContentProvider`, add it to `ContentSource` and `AppContainer`. The UI adapts to `capabilities`.

## Design decisions worth knowing
* **No fake progress.** Progress/“Watched” are written only from the native player. A website open is recorded as *Opened before* — never watched, never a progress bar.
* **Focus:** Material-TV surfaces with a 5 dp ring; focus restored on Home by remembering the last card; details auto-focuses the primary action; episode grid opens on the row to continue; repeated OK presses are debounced (600 ms); no screen transitions.
* **Data safety:** ids are prefixed per source (`demo:`, `kisskh:`) so switching sources never mixes data; Room has an exported schema and no destructive migration.
* **Player errors** show friendly text only; technical details go to Logcat.

## Build & install
See `docs/BUILD_AND_INSTALL.md` (Android Studio, emulator, developer options, ADB install, updates, signed release). Quick start:
```bash
./gradlew testDebugUnitTest assembleDebug     # APK: app/build/outputs/apk/debug/app-debug.apk
```

## Known limitations
* Not yet run on an emulator/BRAVIA: focus feel, playback, persistence need your confirmation (checklist in `docs/TEST_RESULTS.md`).
* KissKH: no automatic catalog/search/episodes/native playback/subtitles/progress (see assessment). Website playback needs a TV browser, which many Android/Google TV devices lack, and it is unverified whether the site's challenge passes there.
* Demo videos stream from Google-hosted sample files; they need internet and could move.
* Demo dramas have no poster images (the app draws title cards); `Coil` is wired for provider-supplied images and unverified on-device.
* Filipino strings are my translation — please have a native speaker review them.
* "Last scroll position" is approximated by reopening the episode grid on the row of the episode to continue.
* Sony model not verified: check *Settings → About* for Android TV/Google TV and the Android version (`BUILD_AND_INSTALL.md` §0).
* Search relies on the TV's on-screen keyboard: focus the field and press OK. Untested on-device.
