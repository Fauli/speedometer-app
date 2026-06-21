# Speed-o-Meter

A no-nonsense GPS speedometer for Android. No account, no verification, no
subscription, no ads — and **no internet permission at all**. It reads your speed
straight from the GPS chipset and shows it big and clear.

See [SPEC.md](SPEC.md) for the full design and roadmap.

## Features

- **Live speed** — digital readout or a classic **analog dial** (switchable)
- **Units** — km/h, mph, knots
- **Trip stats** — average, max, distance, time + a **live speed graph**
- **Odometers** — resettable trip + persistent lifetime
- **Themes** — auto / light / true-black night; **HUD windshield-mirror** mode
- **Speed alert** — custom threshold with flash / beep / vibrate
- **Configurable** — color thresholds, secondary metrics, GPS rate, and more
- **Landscape** layout for dashboard mounts

## Privacy

The app declares only `ACCESS_FINE_LOCATION` (+ `VIBRATE`). It has **no `INTERNET`
permission**, so it cannot phone home — all data stays on the device.

## Tech

Kotlin · Jetpack Compose · Fused Location Provider · DataStore · min SDK 26 ·
target SDK 36 (Android 16). No backend, no third-party accounts.

## Build & install

Requires the Android SDK and a JDK (17+).

```bash
# Build a debug APK
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Install to a connected device (USB debugging on)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and Run.

No Play Store or developer account needed — sideload the APK onto your own phone.
