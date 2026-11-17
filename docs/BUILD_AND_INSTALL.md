# Build, test and install DramaEase (Windows + Android Studio)

## 0. Is your Sony BRAVIA compatible?
Only BRAVIA models running **Android TV or Google TV** can run this app. On the TV: *Settings → System → About* (or
*Device Preferences → About*). If you see "Android TV" / "Google TV" and an Android version, you're fine. Note the
**Android version** — it decides `minSdk` (below). Older/entry-level Sony models with other systems cannot install APKs.
*Not verified for your model: tell me the model number and check Sony's spec page.*

| TV Android version | API | Set `dramaease.minSdk` to |
|---|---|---|
| 8.0 / 8.1 | 26–27 | 26 (default) |
| 9 | 28 | 26 (default is fine) |
| 10 | 29 | default is fine |
| 11+ / Google TV | 30+ | default is fine |
| 7.x | 24–25 | 24 (**untested** below 26) |

The default 26 installs on everything ≥ Android 8. Change it in `gradle.properties` or with `-Pdramaease.minSdk=NN`.

## 1. Build in Android Studio
1. Install **Android Studio** (current stable) and JDK 17 (bundled). 2. *File → Open* the `DramaEase` folder.
3. Let Gradle sync (it downloads the SDK 35 platform if missing; accept licences). 4. Run on an emulator (section 3) or build the APK.

Command line (from the project folder): `gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug`.

## 2. Debug APK
Output: `app\build\outputs\apk\debug\app-debug.apk`. In Android Studio: *Build → Build Bundle(s)/APK(s) → Build APK(s)*.

## 3. Android TV emulator
1. *Tools → Device Manager → Create Device → category **TV** → "Television (1080p)"*.
2. System image: **Android TV (Google APIs), API 34**, x86_64. Finish and start it.
3. Windows needs hardware acceleration (enable *Windows Hypervisor Platform*/virtualization in BIOS).
4. Run the `app` configuration. The emulator window's **side toolbar → "⋯" Extended controls → Virtual sensors/Directional pad** gives a
   virtual remote (D-pad, OK, Back, Home). Keyboard arrows + Enter + Esc also work. See `docs/TEST_RESULTS.md` for the checklist.

## 4. Developer options + ADB on the BRAVIA
1. *Settings → Device Preferences → About → Build* — press OK **7 times** ("You are now a developer").
2. *Settings → Device Preferences → Developer options* → turn on **USB debugging** and, if present, **Network/ADB debugging**
   (on Android 11+/Google TV it may be named **Wireless debugging**).
3. Note the TV's IP: *Settings → Network & Internet → (your network)*. TV and laptop must be on the same network.
4. Install platform-tools (comes with Android Studio: `%LOCALAPPDATA%\Android\Sdk\platform-tools`).

## 5. Install with ADB
Classic network debugging (port 5555):
```bash
adb connect 192.168.1.50:5555
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.dramaease.tv/.MainActivity
```
The TV will show **"Allow USB/network debugging?"** the first time — choose *Always allow*.
If the TV uses **Wireless debugging** with pairing: *Wireless debugging → Pair device with pairing code*, then
`adb pair <ip:pairing-port>` (enter the code), then `adb connect <ip:main-port>` and install as above.
Shortcut: `scripts\install-to-tv.bat 192.168.1.50 app\build\outputs\apk\debug\app-debug.apk`.

Find the app on the TV in the **Apps** row (it has a launcher banner). Tip: use *Settings → Apps → DramaEase* to put it on the home screen if your launcher allows.

## 6. Update without losing data
`adb install -r <new apk>` **keeps** favorites, progress and settings **only if the new APK is signed with the same key** as the installed one.
* Debug builds use a per-computer debug key: updating from a different computer fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
* So for the aunt's TV, **install the signed release APK (section 7) and always update with the same keystore.** Never `adb uninstall` (that wipes data).
* The database never uses destructive migration; schema changes ship with migrations (`app/schemas/` holds the schema history).

## 7. Signed release APK
```bash
keytool -genkeypair -v -keystore dramaease-release.jks -alias dramaease -keyalg RSA -keysize 2048 -validity 10000
```
Copy `keystore.properties.example` to `keystore.properties`, fill it in, then run `gradlew assembleRelease`.
Output: `app\build\outputs\apk\release\app-release.apk`. **Back up `dramaease-release.jks` and its passwords** somewhere safe —
losing it means you can no longer update the installed app without wiping its data. Keep them out of git (already git-ignored).
(Android Studio: *Build → Generate Signed App Bundle or APK → APK*.) Without `keystore.properties` the release build is unsigned and won't install.

## Troubleshooting
* `adb connect` times out → wrong IP, different network, or network debugging off.
* `unauthorized` → accept the prompt on the TV (use remote), or toggle debugging off/on.
* `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → signature differs (section 6). `INSTALL_FAILED_OLDER_SDK` → lower `dramaease.minSdk`.
* App not in the launcher → it's a leanback app: look in the Apps row, not "Google Play".
