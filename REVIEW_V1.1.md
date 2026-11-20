# LiquidWidgets v1.1.0 — Self-Review

Date: 2026-11-21. Reviewer: the same agent that made the fixes (self-review, not independent).
CI at time of writing: run for the v1.1.0 commit (see commit message) — must be GREEN before this rating counts.

## Verdict: 90 / 100

| Area | Score | Why |
|---|---|---|
| No fake data | 96 | Columbus/21°C fallback deleted; CPU fallback deleted; weather returns null; steps/battery/music all read real system state. |
| Widgets live-ness | 88 | Clock + Pomodoro tick every minute via AlarmManager (was 15–30 min stale). Not per-second — a per-second alarm would drain battery, so MM:SS seconds only refresh each minute. |
| Real liquid glass | 90 | Real wallpaper bitmap is now the AGSL backdrop; live wallpaper service animates behind transparent widgets. Home-screen widgets themselves still cannot run a live shader (Android RemoteViews limit) — renders are cached per minute. |
| Performance / battery | 92 | Render cache (24 MB LRU), per-widget updates only, no persistent service, 15 fps wallpaper only while visible, animations paused when screen off / Battery Saver on. |
| Architecture | 94 | Singleton removed, SupervisorJob app scope, MusicDataProvider split out, sensor listener always unregistered. |
| Buildability / CI | 95 | gradlew + wrapper, workflow installs stable API 36 (API 37 platform does not exist in Google's stable repo yet — verified), unit tests in CI. Release build is unsigned debug only. |
| Privacy | 98 | No QUERY_ALL_PACKAGES, no account/ads/analytics, weather is the only network call (Open-Meteo). |

## What is still NOT done (why not 100)
1. Clock/Pomodoro update once a minute, not once a second.
2. Home-screen widgets cannot animate continuously — platform limitation, mitigated by the live wallpaper.
3. No phone test yet: widgets, AGSL fallback behaviour, and the live wallpaper can only be proven on a real Android 17 phone (Amin's). Green CI proves compilation only.
4. No signed release keystore yet — v1.1.0 ships as a debug APK; a stable signing key must be created before the first release build (per the fresh-app verification skill).
5. Weather needs location permission + network; without either it honestly shows nothing.
6. API 37 bump is blocked on Google publishing the stable platform package.

## Fixes in this release (file:line, approximate)
- Clock staleness: `receivers/ClockTickReceiver.kt` (new), `workers/BatteryAwareScheduler.kt` `scheduleMinuteTicks()`, `LiquidWidgetsApp.kt` (armed on create).
- Minute-fill bug: `widgets/LiquidClockWidget.kt` `minuteFillFor()` (minutes/60) + `ClockMathTest.kt`.
- Weather TTL + no fake data: `data/providers/WeatherDataProvider.kt` (`CACHE_TTL_MS`, nullable `read()`), `data/prefs/WidgetConfigStore.kt` `getCachedWeatherUpdatedMs()`.
- Real wallpaper: `engine/WallpaperSampler.kt` (`realWallpaperBitmap`, `realWallpaperCrop()`).
- Live wallpaper: `wallpaper/LiquidGlassWallpaperService.kt` (new), `res/xml/liquid_wallpaper.xml`, manifest service entry, `ui/screens/SettingsScreen.kt` button.
- Steps leak: `data/providers/StepsDataProvider.kt` (5 s timeout unregister + widget refresh on event).
- Quick Pill honesty: `data/providers/QuickControlsProvider.kt`, `receivers/WidgetActionReceiver.kt`, `MainActivity.kt` `onResume()` refresh.
- CPU fake fallback removed: `data/providers/SystemStatsDataProvider.kt` (`cpuPercent: Int?`, `parseProcStatLine()`).
- Fonts: `res/font/*.ttf` (6 OFL files), `data/model/LiquidModels.kt` (`fontRes`), `engine/LiquidGlassBitmapRenderer.kt` `createTextPaint()`.
- Emoji glyphs: `res/drawable/ic_bolt|ic_play|ic_pause|ic_next|ic_search.xml`, `engine/MetaballSimulation.kt` (`drawIconDroplet()`, `drawVectorIcon()`), battery/music/quick-pill widgets + `ui/components/LiveWidgetPreviews.kt`.
- Render cache: `engine/GlassRenderCache.kt` (new), `widgets/LiquidWidgetBase.kt` (`renderBitmap()` contentKey), all 10 widgets pass a content key.
- Singleton removed: `LiquidWidgetsApp.kt` (`liquidApp()`, `applicationScope`), all 13 call sites.
- Music split: `data/providers/MusicDataProvider.kt` (new), `data/providers/LiquidMediaListenerService.kt` (service only).
- QUERY_ALL_PACKAGES removed: `AndroidManifest.xml` (+ `<queries>` block).
- Tests: `app/src/test/...` (4 test files), `app/build.gradle.kts` junit, workflow runs `testDebugUnitTest`.
