# Night Sleep Timer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically pause playback after 30 minutes of actual playback when it starts between 23:00 and 06:00, if the new option is enabled.

**Architecture:** Extend the existing sleep-timer path in `AudioPlayerController`, with a playback-start policy, persisted ViewModel setting, and a switch in the existing sleep-timer dialog. Keep manual timers unchanged; the automatic timer replaces an active timer and has no connection-shutdown actions.

**Tech Stack:** Kotlin, Android, Jetpack Compose, StateFlow/coroutines, JUnit 4, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-11-04-night-sleep-timer-design.md`

## Global Constraints

- Night window is local device time, 23:00 inclusive through 06:00 exclusive.
- Start the automatic timer only on a transition into playback during the night window.
- Count down only while playback is active; pause/resume playback pauses/resumes the automatic timer.
- Once triggered, let the timer finish even after 06:00.
- The automatic timer replaces any current timer and does not run manual timer end actions.
- With the option off, preserve existing manual sleep-timer behavior.
- The feature is off by default and its setting persists across app restarts.

## Review Focus

1. **Overnight boundary and local time** — 22:59 and 06:00 must not qualify; 23:00 and 05:59 must qualify. Pin in Task 1 policy tests.
2. **Playback transition** — only a transition from not-playing to playing starts the timer; pausing/resuming must not reset it. Pin policy transition in Task 1 and timer continuation in Task 2.
3. **Timer replacement** — an active manual timer is replaced without carrying its connection-shutdown actions into the automatic timer. Pin in Task 2.
4. **Existing manual behavior** — when night mode is off, manual timer behavior remains unchanged. Pin with a regression/manual verification in Task 2.

---

## File Map

- Create `app/src/main/java/com/labix/navirom/player/NightSleepTimerPolicy.kt` for pure time-window and start-transition decisions.
- Create `app/src/main/java/com/labix/navirom/player/PlaybackAwareCountdown.kt` for a testable countdown that advances only while playback is active.
- Create `app/src/test/java/com/labix/NightSleepTimerPolicyTest.kt` for boundary and transition tests.
- Create `app/src/test/java/com/labix/PlaybackAwareCountdownTest.kt` for pause/resume countdown tests.
- Modify `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt` to observe playback starts, replace the current timer, and count automatic-timer seconds only during active playback.
- Modify `app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt` to load, expose, and persist the enablement preference and pass changes to the player controller.
- Modify `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt` to collect and pass the setting to the player UI.
- Modify `app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt` to show the option in the existing sleep-timer dialog.
- Modify `app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt` with English, Albanian, and German strings.

### Task 1: Night eligibility policy and setting UI

**Files:**
- Create: `app/src/main/java/com/labix/navirom/player/NightSleepTimerPolicy.kt`
- Create: `app/src/test/java/com/labix/NightSleepTimerPolicyTest.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt`

**Interfaces:**
- Produces `object NightSleepTimerPolicy` with `fun isWithinNightWindow(hour: Int, minute: Int): Boolean` and `fun shouldStart(enabled: Boolean, wasPlaying: Boolean, isPlaying: Boolean, nightTimerActive: Boolean, hour: Int, minute: Int): Boolean`.
- ViewModel exposes `val isNightSleepTimerEnabled: StateFlow<Boolean>` and `fun setNightSleepTimerEnabled(enabled: Boolean)`; persist with key `night_sleep_timer_enabled`, default `false`. Runtime delegation to `AudioPlayerController` is added in Task 2.
- `FullPlayerModal` receives `isNightSleepTimerEnabled: Boolean = false` and `onNightSleepTimerEnabledChange: (Boolean) -> Unit = {}`.

- [x] **Step 1: Write policy tests**

Test `isWithinNightWindow` at 22:59, 23:00, 05:59, and 06:00. Test `shouldStart` is true only when enabled, playback transitions false-to-true, no night timer is already active, and the time is within the window. An active night timer must not restart on resume.

- [x] **Step 2: Run policy tests and verify they fail**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.NightSleepTimerPolicyTest"`
Expected: FAIL because `NightSleepTimerPolicy` does not exist.

- [x] **Step 3: Implement the policy and setting flow**

Use integer hour/minute inputs so tests are independent of wall clock. Load and persist the preference in `NaviromViewModel`. Runtime delegation to `AudioPlayerController` is wired in Task 2 so this task remains independently buildable.

- [x] **Step 4: Add the sleep-timer switch and localized text**

Place an “Auto-start at night” switch and a short “Pauses after 30 minutes of playback, 23:00–06:00” description in the existing sleep-timer dialog. Wire the setting through `NaviromApp`; add English, Albanian, and German strings.

- [x] **Step 5: Run policy tests and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.NightSleepTimerPolicyTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

### Task 2: Automatic active-playback countdown

**Files:**
- Create: `app/src/main/java/com/labix/navirom/player/PlaybackAwareCountdown.kt`
- Modify: `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt`
- Test: `app/src/test/java/com/labix/PlaybackAwareCountdownTest.kt`
- Test: `app/src/test/java/com/labix/NightSleepTimerPolicyTest.kt`

**Interfaces:**
- Consumes `NightSleepTimerPolicy.shouldStart(...)` from Task 1.
- Produces `class PlaybackAwareCountdown` with `suspend fun run(initialSeconds: Int, isPlaying: StateFlow<Boolean>, onTick: (Int) -> Unit, onFinished: () -> Unit)`; decrement once per second only while `isPlaying.value` is true.
- `AudioPlayerController` adds `fun setNightSleepTimerEnabled(enabled: Boolean)`.
- Keep `setSleepTimer(minutes, options)` as the manual timer entry point and its behavior unchanged.

- [x] **Step 1: Add regression tests for timer policy scenarios**

Write coroutine tests with `runTest` and `MutableStateFlow`: a paused countdown does not advance, playback advances one second, pausing preserves the remaining seconds, resuming continues from that remainder, and completion fires once after the requested active-playback seconds. Extend policy tests to prove a disabled option prevents start and an active night timer is not restarted. Implement controller integration to replace a manual timer with a 30-minute automatic timer using default `SleepTimerOptions()`; verify that wiring manually because the controller owns Android `MediaPlayer` state.

- [x] **Step 2: Run targeted tests and verify the new cases fail**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.PlaybackAwareCountdownTest"`
Expected: FAIL because `PlaybackAwareCountdown` does not exist.

- [x] **Step 3: Implement automatic start and playback-aware countdown**

Observe primary playback state transitions in the existing controller scope. On an eligible start, cancel/replace the current timer with 30 minutes and default end-action options. Track whether the active timer is automatic so playback resume continues it instead of restarting it. Use `PlaybackAwareCountdown` for the automatic timer; keep the existing manual countdown path unchanged. At expiry, call `pause()` and clear timer state.

- [x] **Step 4: Run full unit tests and build**

Run: `gradle :app:testDebugUnitTest`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual verification (not run: no connected Android device / 30-minute playback session)**

With the option enabled, start playback at a qualifying local time, pause/resume it, and verify it pauses after 30 minutes of accumulated playback. Verify a start at 06:00 does not create a timer, crossing 06:00 does not cancel a started timer, an active manual timer is replaced, and manual timers still behave the same with the option disabled.

## Self-Review

- **Spec coverage:** setting placement/persistence, local-time window, playback-start trigger, accumulated active playback, timer replacement, expiry pause, no manual end actions, and existing manual behavior are covered by Tasks 1–2.
- **Step scan:** each step has a specific test, implementation action, or verification command. Pure countdown semantics are coroutine-tested; Android playback wiring is verified by build and manual scenarios because the controller constructs Android `MediaPlayer` instances.
- **Type consistency:** policy signatures and ViewModel/UI setting names are consistent across both tasks.
- **Review focus:** boundary and trigger behavior is covered by pure policy tests; replacement, paused countdown, and manual timer regression are called out in Task 2.
- **Proportion:** two tasks keep this small change together without splitting the UI wiring from its setting policy.
