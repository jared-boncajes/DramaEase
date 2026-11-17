# Test results

Environment: Linux cloud sandbox, JDK 21, Gradle 8.9 (wrapper), Android SDK 35. **No emulator or TV hardware was available
(no `/dev/kvm`), so nothing below was run on an Android device.** "Not run" means exactly that.

## Ran and passed (2026-11-17)
| What | Result |
|---|---|
| `gradlew testDebugUnitTest` | **11 / 11 passed** — `ProgressAndResumeTest` (7), `ProvidersTest` (4) |
| `gradlew assembleDebug` | ✅ `app-debug.apk` (~12 MB) |
| `gradlew assembleRelease` (R8 + resource shrinking, throwaway test keystore) | ✅ 2.4 MB, `apksigner verify` OK |
| APK inspection (`aapt2 dump badging`) | package `com.dramaease.tv`, leanback launcher activity, `android.software.leanback` required, touchscreen **not** required, banner present, targetSdk 34, minSdk 26 |
| Reachability of kisskh.do | Reachable; Cloudflare managed challenge (HTTP 403, `cf-mitigated: challenge`). See `KISSKH_ASSESSMENT.md` |

What the unit tests actually cover: the 95 % "watched" rule; resume-position rules (never resume unconfirmed or finished progress);
no progress bar for website-only opens; the primary-button target (start / continue / next / watch again);
Demo provider catalog, episodes, search and native playback option; KissKH provider returning **empty** without a list file,
accepting only `https://kisskh.do` links, sorting episodes, and always answering `ExternalWebsite` (never a stream).

## NOT run — please test (laptop emulator first, then the BRAVIA)
Tick these off on the Android TV emulator (virtual remote), then repeat on the TV:

- [ ] App appears in the TV launcher and starts to Home within a few seconds; text is large by default.
- [ ] Home: first card is focused; Up/Down/Left/Right move with a thick visible ring; nothing is clipped at screen edges.
- [ ] Open a drama → the big button is focused automatically → OK plays the demo video. Needs internet (sample MP4s are hosted by Google).
- [ ] Demo subtitles appear (white on dark box); the Subtitles button turns them off/on; Settings → Subtitle Size changes their size.
- [ ] Player: Play/Pause key, Rewind/Fast-forward keys (±10 s), Left/Right with controls hidden, Back exits to the episode list.
- [ ] Quit after ~1 min, reopen: Home shows **Continue Watching** with a progress bar; OK resumes near the same spot.
- [ ] Finish (or seek to the end of) an episode → "Next Episode" focused → plays episode 2; progress for ep 1 shows ✓ Watched.
- [ ] Choose Episode screen opens on the row of the episode to continue; focus lands on it.
- [ ] Add to Favorites → appears in My Favorites on Home; survives app restart; removing works.
- [ ] Return from Details/Player/Settings to Home: focus returns to the card you left.
- [ ] Settings: Easy Mode, Text Size (3 sizes), High Contrast, Language (English/Filipino), Subtitle Size, Source — all survive restart.
- [ ] Turn the TV's Wi-Fi off, press play: a friendly message with **Try Again** and **Go Back** (no technical text).
- [ ] Switch source to KissKH list with no file: friendly empty message. With a list file: dramas appear; **Open the Website** works only if a browser is installed; otherwise shows the "no browser" message.
- [ ] Press the remote's Home button during playback: returns to the TV home; reopening resumes.
- [ ] Update test: install over the top with `adb install -r` (same key) — favorites and progress remain.
- [ ] Performance feel on the actual BRAVIA (scrolling smoothness, startup time).
