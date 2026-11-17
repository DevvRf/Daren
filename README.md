# Game Space

A performance-overlay utility for Android: a home screen where you pick a
game (Mobile Legends and Free Fire pre-filled, or add your own), then
per-game settings for a crosshair overlay, a live FPS/CPU/RAM/temperature/
GPU HUD, system-wide color grading presets (Vivid, Ocean, Warm,
Grayscale), and a render-resolution downscale — all usable while the game
itself is in the foreground.

This is an original app, built clean — it does not touch, decompile, or
depend on any other app. It only reads public system stats and writes
Android's own built-in color-matrix setting.

## Build the APK from your phone (GitHub Actions, no PC needed)

This project includes `.github/workflows/build-apk.yml`, which builds a
debug APK on GitHub's own servers whenever you push to the repo — no
Android SDK on your device required.

**1. Upload the project to your repo (e.g. `louded-space`), from the browser:**
1. Open your repo on github.com (works in Chrome/any mobile browser).
2. Tap **Add file → Upload files**.
3. Tap **choose your files** and pick `GameSpace-android-project.zip`
   directly — GitHub's uploader auto-extracts a `.zip` you select or drop
   there, so you don't need to unzip it yourself first.
4. Wait for the file list to populate (all the folders/files inside),
   then scroll down and tap **Commit changes**.

**2. Let it build:**
- A push to `main`/`master` triggers the workflow automatically. If your
  default branch is named differently, or you want to re-run it: go to
  the **Actions** tab → **Build debug APK** → **Run workflow**.
- Build takes a few minutes. Refresh the Actions tab to watch progress.

**3. Download the APK:**
- Once the run shows a green check, open that run → scroll to
  **Artifacts** → tap **GameSpace-debug-apk** → it downloads a `.zip`
  containing `app-debug.apk`. Extract that (any file manager app) and
  install it (enable "install from unknown sources" if prompted).

No signing is done (it's a debug build), so Android will show the usual
"unverified developer" warning on install — that's normal for a debug
APK you built yourself.

## Alternative: build locally in Android Studio (if you get PC access)

Android Studio → Open → select the `GameSpace` folder → let Gradle sync
(Android Studio offers to regenerate the missing `gradlew` wrapper script
automatically the first time it opens the project) → Run, or
Build → Build Bundle(s)/APK(s) → Build APK(s).

## Using it

Home screen (`GameListActivity`) lists your games. Tap **Izin** (top
right) once to grant permissions (see below). Tap a game to open its
customization screen (`GameCustomizeActivity`): crosshair style, color
preset, a downscale slider (50%-100% of native resolution), and the stats
HUD toggle — each saved per game. **Start in-game overlay** applies all of
it and starts the foreground service; **Buka game ini** launches the
game's app directly if its package is installed. Use **+ Tambah game** on
the home screen to add any other game by name (and optionally its package
name, so "Buka game ini" works for it too).

Downscale needs Shizuku or root (see below) — it changes the system's
actual render resolution via `wm size`, which no app can do with a public
permission alone. Setting it back to 100% restores native resolution.

**Real-time performance HUD:** the stats overlay now draws a live scrolling
CPU/RAM line graph and a color-zoned temperature arc gauge, updating every
500ms, alongside the exact FPS/CPU/RAM/temp/GPU numbers. Every point on
the graph and every needle position comes from a reading taken that tick
(`SystemStatsReader` / `PrivilegedExec`) — nothing is randomized or
pre-set to look busy.

**Game Lab (touch test):** from the home screen, **Game Lab** opens an
8-second real touch-capability test. It records the actual maximum number
of simultaneous fingers your screen reported and the actual average
interval between touch samples (i.e. your digitizer's real report rate),
then shows a score computed from those two real numbers — documented in
`TouchTestActivity`'s comments, not a random or fixed value.

**Resolution safety net:** a downscale's state is now persisted, and
`ResolutionBootReceiver` double-checks it's back to native after any
reboot. The overlay's notification also has a one-tap **Revert
resolution** button (`ResolutionRevertReceiver`) in case a game looks
wrong and you want native resolution back immediately without reopening
the app. Stopping the overlay (however it stops) also resets resolution.

**In-game quick toggle:** once the overlay is started, a small draggable
"GSR" bubble appears on screen. Tap it to expand two quick toggles —
**Crosshair: ON/OFF** and **Stats: ON/OFF** — so you can turn the
crosshair or the FPS/CPU/RAM/temp/GPU HUD on or off without leaving the
game or going back into the app. Tap the bubble again to collapse it.
Drag it anywhere; a drag doesn't count as a tap, so it won't
accidentally expand/collapse while you're repositioning it.

## First-run setup, in the app

1. **Allow overlay** — standard "draw over other apps" permission,
   requested via the normal system dialog.
2. **Enable Shizuku** (recommended privilege mode) — install the
   [Shizuku app](https://shizuku.rikka.app/) separately (Play Store or
   GitHub release). Start it either via wireless debugging (Settings →
   Developer options → Wireless debugging, then pair/run the command
   Shizuku shows you — no PC needed on Android 11+) or, if your device is
   rooted, via Shizuku's own "start via root" button. Then come back to
   Game Space and tap **Enable Shizuku**; it requests the one-time Shizuku
   permission. This covers the FPS estimate and the color presets without
   your device needing to be rooted at all.
3. **Allow color presets (adb/root)** — only needed if you're *not* using
   Shizuku. Either:
   ```
   adb shell pm grant com.gamespace.app android.permission.WRITE_SECURE_SETTINGS
   ```
   or let the app fall back to `su` on a rooted device.
4. Pick a crosshair style, toggle the stats HUD, pick a color preset, then
   **Start in-game overlay**. Switch to your game — the overlay stays on
   top as a foreground service.

Privilege order the app uses automatically: **Shizuku → root (`su`) →
direct permission**, whichever is available, shown live on the main screen
as "Privileged mode: shizuku / root / none".

## What's real vs. approximate, and why

- **CPU %, RAM, battery/CPU temperature** — read from `/proc/stat`,
  `ActivityManager.MemoryInfo`, and `/sys/class/thermal/*`. These are
  public on stock Android, no root needed. Thermal-zone *numbering* is
  chipset-specific, so `SystemStatsReader` tries a short list of common
  paths; add more in `cpuThermalCandidates` if your device's zone isn't
  one of them (check `adb shell cat /sys/class/thermal/thermal_zone*/type`
  on your device to find the CPU one).
- **GPU clock** — same idea, vendor-specific `/sys` nodes
  (`gpuClockCandidates`). Many OEMs lock these down; if none are
  readable you'll see "n/a" rather than a made-up number.
- **FPS of the game underneath** — Android has no public API for "what FPS
  is this other app rendering at." With root, this app estimates it from
  `dumpsys gfxinfo <foreground-package> framestats`, which is how tools
  like this generally do it. Without root it shows "n/a" rather than
  faking a number.
- **Color presets** — these genuinely change the whole screen's color
  grading (including the game), using Android's own accessibility
  color-matrix hook. They're identical in mechanism to the
  "color correction" feature already in Android's Accessibility settings;
  this app just exposes a few named presets for it.

## Project layout

```
app/src/main/aidl/com/gamespace/app/IUserService.aidl   Shizuku service interface
app/src/main/java/com/gamespace/app/
  GameListActivity.kt       home screen: pick/add a game (launcher activity)
  GameCustomizeActivity.kt  per-game crosshair/color/downscale/stats + start
  GameListAdapter.kt        RecyclerView adapter for the home screen
  GameProfile.kt            per-game settings model + JSON (de)serialization
  GameProfileStore.kt       SharedPreferences-backed list of GameProfiles
  DisplayScaler.kt          applies/resets downscale via `wm size`, persists state
  FloatingControlBubble.kt  draggable "GSR" quick-toggle bubble (crosshair/stats)
  PerfGraphView.kt          real-time scrolling CPU/RAM line graph (custom View)
  TempGaugeView.kt          real-time color-zoned temperature arc gauge
  TouchTestView.kt          records real multitouch count + sample interval
  ScoreRingView.kt          renders a score (computed elsewhere) as a ring
  TouchTestActivity.kt      Game Lab: runs the 8s touch test, computes the score
  ResolutionBootReceiver.kt verifies resolution is native after reboot
  ResolutionRevertReceiver.kt handles the notification's "Revert resolution" button
  MainActivity.kt           permission requests screen ("Izin" button)
  OverlayService.kt         foreground service hosting both overlays
  CrosshairOverlayView.kt   draggable crosshair window
  StatsOverlayView.kt       draggable stats HUD window
  SystemStatsReader.kt      CPU/RAM/temp/GPU reads (no privilege needed)
  RootUtils.kt              su wrapper (root backend) + shared output parsers
  UserServiceImpl.kt        runs inside Shizuku's privileged process
  ShizukuManager.kt         Shizuku binder/permission/service lifecycle
  PrivilegedExec.kt         picks Shizuku, falls back to root, exposes exec()
  ColorFilterManager.kt     applies the color-matrix presets
```

## Extending it

- Add more crosshair styles: drop a vector drawable in `res/drawable/`,
  add a `RadioButton` in `activity_main.xml`, wire it in
  `MainActivity.crosshairGroup`'s listener.
- Add more color presets: add an entry to `ColorFilterManager.PRESETS`
  (a 4x4 column-major matrix) and a radio button.
- Crosshair size/opacity controls, a slider for HUD update interval, and
  saving the chosen preset across restarts (`SharedPreferences`) are the
  natural next additions — the current build keeps state in memory only.
