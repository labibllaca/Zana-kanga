# Shared Sleep Timer Shutdown Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a persisted GPS shutdown choice and make both manual and automatic night timers execute the same currently selected shutdown actions at expiry.

**Architecture:** Extend `SleepTimerOptions`' existing UI and preference flow, adding GPS to the modal and saving `disableGps`. Centralize timer completion so manual and automatic expiry both pause playback, clear timer state, and execute the selected action set through `DeviceConnectionManager`.

**Tech Stack:** Kotlin, Android, Jetpack Compose, StateFlow, JUnit, existing Gradle test/build tasks.

**Spec:** `docs/superpowers/specs/2026-11-04-night-sleep-timer-design.md`

## Global Constraints

- Use one shared saved set of Bluetooth, Wi-Fi, mobile-data, and GPS choices for manual and night timers.
- GPS and the other shutdown choices default to off.
- Timer expiry pauses playback, clears the timer, then runs selected actions through the existing connection manager.
- Shutdown is best-effort and platform-dependent; do not promise Android will disable every selected radio.
- Preserve existing manual timer duration/countdown behavior and night timer timing behavior.

## Review Focus

- Existing saved preferences with no GPS key must load `disableGps = false`; test the default/round-trip preference behavior in Task 1.
- Changing shared choices while a timer is active must affect the eventual action set; test completion with the latest `SleepTimerOptions` in Task 2.
- Night expiry must run the selected actions after pausing playback; test ordering and options in Task 2, and verify the controller calls the shared completion path.
- Manual expiry must continue using its chosen shared actions without changing countdown timing; test shared completion and inspect the unchanged manual countdown path in Task 2.
- A radio shutdown can fail on the device despite permission; preserve `DeviceConnectionManager`'s existing fallback behavior and report this limitation in on-device verification (Task 3).

---

### Task 1: GPS shutdown choice in the Sleep Timer

**Files:**
- Create: `app/src/main/java/com/labix/navirom/data/local/SleepTimerOptionsPreferences.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt`
- Test: `app/src/androidTest/java/com/labix/SleepTimerOptionsPersistenceTest.kt`

**Interfaces:**
- Consumes: `SleepTimerOptions.disableGps: Boolean`, already present in `app/src/main/java/com/labix/navirom/data/model/Models.kt`.
- Produces: `object SleepTimerOptionsPreferences` with `load(preferences: SharedPreferences): SleepTimerOptions` and `save(preferences: SharedPreferences, options: SleepTimerOptions): Unit`; GPS-aware `sleepTimerOptions` flow persisted under `sleep_timer_disable_gps`; GPS switch labeled “Turn off GPS” in the existing Actions on timer end section.

- [x] **Step 1: Add a test for legacy default and GPS selection persistence**

Using the instrumentation target context's named preferences, call `SleepTimerOptionsPreferences.load/save`; assert absent GPS loads as `false`, `disableGps = true` survives a save/reload, and existing Bluetooth/Wi-Fi/mobile-data preferences continue round-tripping.

- [x] **Step 2: Run the focused test and verify it fails**

Run: `gradle :app:connectedDebugAndroidTest`
Expected: FAIL because GPS is not currently loaded or persisted.

- [x] **Step 3: Wire GPS through ViewModel preferences and modal state**

Implement the preference helper as the single mapping for all four timer options and use it from ViewModel initialization and `updateSleepTimerOptions`. In `FullPlayerModal`, include GPS in `currentOptions`, remembered switch state, timer-set snapshots, and updates from both tap and Switch interactions. Add localized title and concise description in English, Albanian, and German, following neighboring sleep-timer strings.

- [x] **Step 4: Run focused tests and build**

Run: `gradle :app:connectedDebugAndroidTest` and `gradle :app:assembleDebug`.
Expected: tests pass and the debug APK assembles.

### Task 2: Apply shared shutdown actions on both timer expiries

**Files:**
- Create: `app/src/main/java/com/labix/navirom/player/SleepTimerCompletion.kt`
- Modify: `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt`
- Test: `app/src/test/java/com/labix/SleepTimerCompletionTest.kt`

**Interfaces:**
- Consumes: `SleepTimerOptions` and `DeviceConnectionManager.executeSleepTimerActions(options)`.
- Produces: a small suspend completion helper that takes the selected `SleepTimerOptions`, a playback-pause callback, a timer-clear callback, and an action-execution callback; it runs pause, clear, then selected actions exactly once. Both manual and night timer expiry paths call the same helper with the current shared options.

- [x] **Step 1: Write completion tests**

Test that the helper calls pause, clear, then action execution in that order and passes through the exact selected options, including GPS; per-radio filtering remains owned by `DeviceConnectionManager.executeSleepTimerActions`.

- [x] **Step 2: Run the focused test and verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.SleepTimerCompletionTest"`
Expected: FAIL because the common completion helper does not exist.

- [x] **Step 3: Add the helper and route both expiry paths through it**

Implement the helper using the exact ordering above. Replace duplicated completion logic in the manual timer and the night timer with this shared path. At expiry, read the latest `currentSleepTimerOptions` so edits made while a timer is active are honored. Keep manual countdown timing unchanged; after night countdown completion, also preserve its existing timer cleanup and playback pause semantics. Do not reset night timer options to `SleepTimerOptions()` when starting it.

- [x] **Step 4: Run focused and full tests**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.SleepTimerCompletionTest"` and `gradle :app:testDebugUnitTest`.
Expected: all tests pass.

### Task 3: Build and device verification

**Files:**
- Verify: `app/src/main/java/com/labix/navirom/player/DeviceConnectionManager.kt` and the Sleep Timer UI on the connected Android device.

**Interfaces:**
- Consumes: shared GPS preference/UI from Task 1 and unified completion path from Task 2.
- Produces: verified debug build and documented device outcomes for selected GPS/Wi-Fi/Bluetooth actions, including any OS restriction/fallback behavior.

- [x] **Step 1: Build and inspect the diff**

Run: `gradle :app:assembleDebug` and `git diff --check`.
Expected: successful build and no whitespace errors.

- [ ] **Step 2: Install and verify preference/UI behavior (blocked pending user's notification-permission choice)**

Install the debug APK on the connected Android 15/API 35 device; open Sleep Timer; enable GPS and one other shutdown option; close and reopen the modal; confirm both selections remain enabled. Confirm night and manual timers display/use the same selections.

- [ ] **Step 3: Verify expiry behavior where practical (not run: GPS was already off and Wi-Fi ADB/Bluetooth state should not be disturbed)**

Use a short manual timer and a testable night-timer path or debug configuration if available. Confirm playback pauses and selected shutdown actions are attempted; record actual radio state and any platform restriction. Do not wait 30 minutes solely for a device test if an existing test seam can exercise expiry; do not claim shutdown succeeded without observing the resulting system state.
