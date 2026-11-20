# Liquid Widgets — Real Liquid Glass Widgets for Android 17

A widget-first Android application built in **Kotlin**, **Jetpack Compose**, **Jetpack Glance**, **Material 3 Expressive**, and **AGSL `RuntimeShader`**.

## Architecture Highlights

- **Real AGSL Liquid Glass Engine (`res/raw/liquid_glass_refraction.agsl` & `engine/`)**:
  - Signed-distance-field (SDF) edge refraction for 5 shapes (`Squircle`, `Organic Blob`, `Pill`, `Sharp Glass`, `Freeform`) that bends and magnifies the backdrop more at curved rims and less at the flat center.
  - RGB chromatic aberration dispersion along curved rims and touch ripples.
  - Gyroscope-driven specular highlight (`GyroscopeTiltProvider`) + opposite-side inner depth shadow.
  - Real-time metaball lens pockets, rising bubbles, sinusoidal surface waves (`MetaballSimulation`), and radial touch ripples.
  - Live wallpaper color sampling (`WallpaperSampler`) that automatically updates on `ACTION_WALLPAPER_CHANGED` and light/dark mode switches.
  - Offscreen `HardwareRenderer` + `RenderNode` AGSL pipeline (`LiquidGlassBitmapRenderer`) for home-screen and lock-screen widgets, paired with a full 60fps `ShaderBrush` live preview in the companion app.
- **All 10 Resizable Home & Lock Screen Widgets (`widgets/`)**:
  1. `LiquidClockWidget` (4×2 / 2×2) — Fluid numerals, rising minute fill, floating bubble seconds, 12h/24h & analog liquid hands.
  2. `LiquidBatteryWidget` (2×2 / 4×2) — Live `BatteryManager` liquid tank, aqua/amber/red shifts, charging wattage, temperature, health, lightning droplet.
  3. `LiquidScreenTimeWidget` (4×2) — On-device `UsageStatsManager` liquid ring, top 3 app droplets (tap to launch), unlocks/pickups, weekly view.
  4. `LiquidWeatherWidget` (4×2 / 4×4) — Free Open-Meteo API with offline DataStore cache, rain droplets, sun refraction glow, snow, hourly bubbles.
  5. `LiquidCalendarWidget` (4×2) — `CalendarContract.Instances` next 3 events in floating glass pills, shrinking countdown droplet, calm water empty state.
  6. `LiquidMusicWidget` (4×2) — `MediaSessionManager` + `NotificationListenerService`, album art refracted behind glass, liquid audio waves, Play/Pause/Next glass buttons.
  7. `LiquidStepsWidget` (2×2) — `Sensor.TYPE_STEP_COUNTER` daily goal liquid fill, bobbing droplet, distance & calories.
  8. `LiquidSystemWidget` (2×2) — Sloshing CPU, RAM, and Storage liquid gauges + in-app live real-time glass graphs.
  9. `LiquidFocusNotesWidget` (2×2) — Draining Pomodoro droplet timer & floating ink Quick Note mode.
  10. `LiquidQuickPillWidget` (4×1) — Wi-Fi, Bluetooth, Flashlight, and DND liquid toggle bubbles + instant glass app search.
- **Customization Engine (`data/model`, `data/prefs`, `ui/screens/LiveEditorScreen.kt`)**:
  - 12 built-in Liquid Themes, 8 optical/fluid physics sliders, 5 shapes, 6 fluid fonts + scale/weight, 3 background modes, persisted per-widget in Jetpack DataStore and applied live without re-adding widgets.
- **Battery & Performance**:
  - Zero persistent background service; uses `WorkManager` (15-min periodic sync) + event-driven `BroadcastReceiver`s (`LiquidSystemReceiver`), pausing animations when screen is off or Battery Saver is active.

## How to Build in Android Studio

1. **Unzip** `LiquidWidgets-Android-Project.zip` and open the `LiquidWidgets` folder in **Android Studio** (Meerkat / Narwhal or newer with JDK 17+).
2. **SDK Requirements**:
   - The project is configured for **Android 17 (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 33`)**.
   - Install Android SDK Platform 37 via **Tools → SDK Manager** (if you are testing on Android 16 / API 36 before installing API 37 preview, temporarily set `compileSdk = 36` and `targetSdk = 36` in `app/build.gradle.kts`).
3. **Sync & Build**:
   - Let Gradle sync (`gradle/libs.versions.toml` manages all versions).
   - Connect an Android 13–17 device or start an emulator and click **Run (`app`)**, or build from terminal:
     ```bash
     ./gradlew assembleDebug
     ```
4. **Permissions (Optional, On-Device Only)**:
   - Open the **Settings** tab inside **Liquid Widgets** to grant Calendar, Activity Recognition (Steps), Coarse Location (Weather), Usage Access (Screen Time), and Notification Access (Music Player).

## Verification Checklist (On Device)

- [ ] Install on Android 17 device, add all 10 widgets from the Gallery's **Add to Home Screen** button (and test adding to Lock Screen).
- [ ] Resize each widget (e.g., Clock 4×2 ↔ 2×2, Battery 2×2 ↔ 4×2) and verify clean re-rendering.
- [ ] Change system wallpaper 3 times (light & dark) — verify glass tint and refraction adapt automatically.
- [ ] Toggle system Light / Dark mode — verify all widget typography remains crisp and legible.
- [ ] Tap every widget — verify touch ripple plays and launches the expected app or control.
- [ ] Plug in charger — verify Liquid Battery shows wattage, lightning droplet, and faster rising bubbles.
- [ ] Tilt phone in the Live Editor — verify top-left specular highlight shifts with the gyroscope.
