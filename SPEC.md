# Speed-o-Meter — App Specification

A no-nonsense GPS speedometer for Android. No account, no phone verification, no
subscription, no ads, no network required. It reads your speed from GPS and shows
it big and clear — plus a handful of genuinely useful extras.

> **Status:** Scope locked for MVP (decisions in §0). Remaining open items at the bottom.

---

## 0. Locked decisions

1. **Use case:** general-purpose — car, motorbike, cycling, and boating. So all unit
   systems (km/h, mph, knots) and altitude/heading are first-class, not niche.
2. **Platform:** standard Android with Google Play Services → **Fused Location Provider is
   the primary path.** A raw `LocationManager` fallback stays as a low-priority nicety, not
   an MVP requirement.
3. **Display style:** digital and analog will both be selectable in Settings, but the
   **MVP ships digital only.** Analog dial is M2.
4. **Trip graph:** a **live speed histograph for the current trip** is in the MVP,
   toggleable in Settings. Settings gets a proper set of sane config options (see §6.5).

---

## 1. Motivation

Every "free" speedometer in the Play Store is wrapped in subscriptions, account
sign-ups, phone-number verification, or aggressive ads — none of which a tool this
simple has any reason to need. A GPS speedometer is a self-contained device feature:
the phone already knows its speed, we just display it.

**Design principles**

1. **Zero gatekeeping** — no login, no verification, no internet permission at all.
2. **Offline-first** — everything core works with the radios off (except GPS).
3. **Privacy by construction** — no analytics, no tracking, data never leaves the phone.
4. **Glanceable** — readable at a glance while driving/cycling; large digits, high contrast.
5. **Free forever** — sideloaded onto your own phone; no store account required.

---

## 2. How it works (technical basis)

GPS speed is *not* computed by us diffing positions over time (that's noisy and laggy).
Modern Android exposes a **Doppler-derived instantaneous speed** straight from the GNSS
chipset:

- `Location.getSpeed()` → speed in **m/s**, computed by the chipset from satellite
  Doppler shift. Far smoother and more accurate than position-delta math.
- `Location.getSpeedAccuracyMetersPerSecond()` → ± uncertainty (API 26+), used to show
  a confidence indicator and to suppress garbage readings when the signal is weak.
- `Location.hasSpeed()` / `getBearing()` / `getAltitude()` / `getAccuracy()` for the
  extras below.

We obtain locations via the **Fused Location Provider** (Google Play Services) at high
accuracy with a ~1 Hz (1000 ms) update interval. The Fused provider blends GNSS with
sensors and is power-efficient.

> **Decided (§0):** Standard Android + Play Services, so **Fused is the primary provider.**
> A raw `LocationManager` GPS fallback (which also returns `getSpeed()`) is kept as a
> low-priority graceful-degradation path for de-Googled phones — not an MVP requirement.

**Accuracy expectations:** Open sky → readings typically within ~1 km/h of true speed.
Tunnels, urban canyons, and parking garages degrade or drop the signal; the UI must show
this honestly (e.g. dim the number / show "GPS weak") rather than freezing on a stale value.

---

## 3. Permissions (the whole list)

| Permission | Why | When requested |
|---|---|---|
| `ACCESS_FINE_LOCATION` | The one essential — high-accuracy GPS speed | On first launch |
| `ACCESS_COARSE_LOCATION` | Required to be declared alongside fine on modern Android | With fine |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` | Keep measuring with screen off / app backgrounded during trip recording | Only if user starts a recorded trip |
| `POST_NOTIFICATIONS` | Foreground-service notification (Android 13+) | With trip recording |

**Explicitly NOT requested:** `INTERNET`. The app declares no internet permission, which
is a strong, verifiable privacy guarantee — it *cannot* phone home. (This is dropped only
if an optional online feature like real speed-limit lookup is later added — see §5.)

---

## 4. Core features (MVP)

The minimum to ship a daily-usable speedometer:

- **Big current speed** — dominant, full-width digits. Color can shift with speed.
- **Units** — km/h, mph, knots; switchable, remembered.
- **Trip stats** — average speed, max speed, trip distance, moving time, total time.
- **Odometers** — resettable trip odometer + lifetime total distance.
- **GPS status** — satellite/accuracy indicator; clear "acquiring / weak / no signal" states.
- **Keep screen on** while active; sensible auto-brightness behavior.
- **Day / night themes** — auto by time or system dark mode; true-black night mode.
- **HUD mirror mode** — horizontally mirrored digits to reflect off the windshield at night.
- **Reset / start / pause trip** controls.
- **Live trip histograph** — speed-over-time graph for the *current* trip, updating live.
  Toggleable in Settings (default on).
- **Settings** — the config surface described in §6.5.

---

## 5. Enrichment features (the "nice extras")

Grouped by effort. None require a backend.

### Easy wins (local-only)
- **Heading / compass** — N/E/S/W + bearing from `getBearing()`.
- **Altitude** — current elevation; min/max/gain over a trip.
- **Custom speed alert** — set a threshold; visual flash + optional beep/vibration when
  exceeded. Fully offline (your number, not map data).
- **Acceleration timer** — 0→100 km/h (and 0→60 mph) stopwatch, auto-detected. Fun, and a
  genuine differentiator. Also quarter-mile / 0–N configurable.
- **Average-speed modes** — moving average vs. overall average toggle.
- **Large analog dial option** — classic car-style gauge as an alternative to digital.

### Medium effort
- **Trip recording + history** — record a trip, store locally (Room DB), list past trips
  with stats. Export as **GPX/CSV** via Android share sheet (no upload).
- **Route map of a trip** — draw the recorded path. Uses offline-friendly **OpenStreetMap**
  tiles (osmdroid/MapLibre). Optional, behind a setting, since it's the one feature that
  *would* touch the network for tiles.
- **Speed graph** — speed-over-time chart for the current/finished trip.
- **Home-screen widget / always-on glance** — current speed at a glance.

### Stretch / opt-in (would add network)
- **Real speed-limit display & warning** — actual posted limits from OpenStreetMap
  (Overpass) or a downloadable offline limits dataset. This is the *only* feature that
  justifies the INTERNET permission, so it stays **off by default** and clearly flagged.
  Preferred path: offline/cached data so the privacy guarantee holds.

---

## 6. Screens

1. **Speedometer (home)** — the big readout + trip strip (avg / max / dist) + GPS status.
   Single tap cycles secondary metric; long-press for quick settings.
2. **Trip / history list** — past recorded trips, each tappable to a detail view.
3. **Trip detail** — map of route, speed graph, full stats, export button.
4. **Settings** — see §6.5.

### 6.5 Settings (sane config options)

Defaults chosen so the app is fully usable without ever opening this screen.

**Display**
- Units: km/h · mph · knots *(default: km/h)*
- Display style: **digital** *(MVP)* · analog dial *(M2)*
- Decimal places on speed: 0 / 1 *(default: 0)*
- Theme: auto (follow system) · light · true-black night *(default: auto)*
- Speed-based color shifting: on / off *(default: on)*
- HUD mirror mode: on / off *(default: off)*
- Keep screen on while active: on / off *(default: on)*
- Show live trip histograph: on / off *(default: on)*
- Secondary metrics to show (avg / max / distance / altitude / heading / time): multi-select

**Alerts**
- Speed alert threshold: off / custom value *(default: off)*
- Alert style: visual flash · beep · vibrate *(multi-select)*

**Measurement**
- GPS update rate: 1 Hz · 2 Hz *(default: 1 Hz; higher = smoother, more battery)*
- Suppress readings below accuracy threshold: on / off *(default: on)* — hides jittery
  speed when `getSpeedAccuracyMetersPerSecond()` is poor (e.g. standing still / weak signal)
- Stationary cutoff: show 0 below N km/h to kill GPS creep *(default: ~2 km/h)*

**Data**
- Reset trip odometer
- Reset lifetime odometer
- *(M3+)* default export format: GPX · CSV

---

## 7. Tech stack (proposed)

| Concern | Choice | Rationale |
|---|---|---|
| Language | **Kotlin** | Native, modern, first-class Android |
| UI | **Jetpack Compose** | Fast to build big custom readouts & themes |
| Location | **Fused Location Provider** (+ `LocationManager` fallback) | Accurate, power-efficient; Doppler `getSpeed()` |
| Background | **Foreground Service** | Trip recording with screen off |
| Storage | **Room** + DataStore (prefs) | Trips/odometer + settings, local only |
| Maps (optional) | **osmdroid / MapLibre + OSM tiles** | Free, no API-key gatekeeping |
| Charts | Compose-native or MPAndroidChart | Speed graphs |
| Min SDK | **API 26 (Android 8)** | Needed for `getSpeedAccuracyMetersPerSecond()`; ~99% device coverage |
| Target/Compile SDK | **API 36 (Android 16)** | Matches your phone; SDK 36 platform installed locally |
| Build | **Gradle + Android Studio** | Standard |

No backend. No third-party SDKs that require keys/accounts for the core app.

---

## 8. Distribution (no store account needed)

- Build a signed **APK** locally and **sideload** it onto your phone (enable "install
  unknown apps" for the file manager once).
- Self-sign with a local keystore — no Google Play Developer account ($25) required.
- Optional later: publish the APK on **GitHub Releases** or **F-Droid** (F-Droid fits the
  no-tracking, FOSS ethos perfectly).

---

## 9. Non-goals

- No user accounts, cloud sync, or social features.
- No ads, no in-app purchases, no telemetry/analytics.
- No turn-by-turn navigation (this is a speedometer, not a sat-nav).
- No background tracking when not on an explicitly started trip.

---

## 10. Rough roadmap

- **M0 — Spike:** ✅ *Built.* Reads GPS, shows live `getSpeed()`. Pending real-world test.
- **M1 — MVP:** ✅ *Implemented & running on device.* Big digital readout, units
  (km/h/mph/knots), trip avg/max/distance/time, trip + lifetime odometer, GPS status,
  themes (auto/light/night), keep-screen-on, HUD mirror, speed alert (flash/beep/vibrate),
  **live trip histograph**, and the full §6.5 settings. Builds to a sideload-ready APK.
- **M1.1 — Polish:** ✅ Landscape / car-mount layout, unit-aware speed-color thresholds,
  light-theme status-bar contrast, derived-speed fallback (when the chipset gives no
  Doppler speed), and back-button handling out of Settings.
- **M2 — Extras:** Speed alert, heading, altitude, acceleration timer, **analog dial style**.
- **M3 — Trips:** Recording + history + GPX/CSV export + saved per-trip graph (foreground service).
- **M4 — Map & polish:** Route map, widget, settings depth. Optional speed-limit module.

---

## 11. Open questions (remaining)

1. **Android version** of your phone — to confirm min SDK 26 is fine (almost certainly is).
2. **Persisted trip history & route maps (M3/M4)** — still wanted later, or keep the app
   permanently to just live readout + current-trip graph? (MVP doesn't depend on this answer.)
3. **Real speed limits** — worth the optional network dependency down the line, or stay
   100% offline forever?

These don't block the MVP — defaults in §0/§6.5 cover everything needed to start.

---

*Next step: scaffold the Android project (Kotlin + Jetpack Compose), then build the M0
spike — live GPS speed on screen — so we can validate accuracy on a real drive/ride before
fleshing out the M1 MVP.*

## Sources / research

- [Best Free GPS Speedometer Apps for Android (mygpstools)](https://mygpstools.com/best-gps-speedometers-android-features)
- [7 best speedometer apps on Android (Android Police)](https://www.androidpolice.com/best-speedometer-apps-android/)
- [Best speedometer apps for Android (Android Authority)](https://www.androidauthority.com/best-speedometer-apps-android-789270/)
- [Fused Location Provider API (Google for Developers)](https://developers.google.com/location-context/fused-location-provider)
- [Raw GNSS Measurements (Android Developers)](https://developer.android.com/develop/sensors-and-location/sensors/gnss)
- [A Technical Overview of Android's GPS System](https://apexpenn.github.io/2025/02/13/android-gps/)
