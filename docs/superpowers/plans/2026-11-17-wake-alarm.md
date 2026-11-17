# Wake Alarm Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After the sleep timer pauses playback, resume the same track at its stopped position at a chosen wake-up time with a volume ramp that stops when the listener stops the alarm.

**Architecture:** An `AlarmManager` exact alarm wakes `NaviromPlaybackService`, which restores a snapshot (track, queue, position) from shared preferences and drives playback through `AudioPlayerController`. The ramp is one multiplier in the controller; sleep-timer expiries are suppressed while the alarm runs and snapshot the player state when they do fire. Settings live in the existing Sleep Timer surfaces.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), `AlarmManager`, `Notification.Builder` media style, `SharedPreferences` + Moshi, JUnit 4 (JVM tests under `app/src/test/java/com/labix/`).

**Spec:** `docs/superpowers/specs/2026-11-17-wake-alarm-design.md`

## Global Constraints

- One alarm at a time, one-shot: it turns itself off after firing.
- Ramp: start 5 %, +5 % every 15 s, cap 100 %.
- Ramp holds while playback is paused and continues on resume.
- System media volume is raised to at least 60 % on alarm start and restored on Stop.
- Sleep-timer expiries (manual and night-auto) are suppressed while the alarm runs, and no snapshot is written then.
- Alarm is exact (`setAlarmClock`) where permitted; `SCHEDULE_EXACT_ALARM` fallback is `setAndAllowWhileIdle` plus a visible explanation.
- Re-arm on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`; fire late if the armed time already passed.
- Ramp affects the primary player only; WLAN/UPnP output is out of scope; radios stay off.
- New user-facing strings go into `NaviromStrings` in English, Albanian and German.
- Every task ends with `sh gradlew testDebugUnitTest` green and its own commit.

## Review Focus

- Alarm fires on time after hours in Doze with the screen off; a late start is the worst failure.
- Music still playing at fire time: only the ramp starts, the track is never seeked or restarted.
- Reboot or clock change just before the wake time; the alarm must still fire today.
- Snapshot track deleted or only streamable with radios off: the alarm must still make sound or post a visible failure.
- Night auto-timer starting during the alarm: it must not pause the ramp.

---

### Task 1: Alarm time math and ramp curve

**Files:**
- Create: `app/src/main/java/com/labix/navirom/player/WakeAlarmTime.kt`
- Create: `app/src/main/java/com/labix/navirom/player/WakeAlarmRamp.kt`
- Test: `app/src/test/java/com/labix/WakeAlarmTimeTest.kt`
- Test: `app/src/test/java/com/labix/WakeAlarmRampTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `object WakeAlarmTime { fun nextTriggerAt(nowMs: Long, hour: Int, minute: Int, zone: java.util.TimeZone = TimeZone.getDefault()): Long; fun isDue(nowMs: Long, hour: Int, minute: Int, zone: TimeZone = TimeZone.getDefault()): Boolean }`
  - `object WakeAlarmRamp { const val START_LEVEL: Float = 0.05f; const val STEP: Float = 0.05f; const val TICK_MS: Long = 15_000L; fun levelForTick(tick: Int): Float }`

- [ ] **Step 1: Write the failing tests**

```kotlin
class WakeAlarmTimeTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    @Test fun next_trigger_uses_today_when_time_is_ahead() {
        val now = LocalDateTime.of(2026, 11, 17, 22, 0).atZone(zone.toZoneId()).toInstant().toEpochMilli()
        val next = WakeAlarmTime.nextTriggerAt(now, 7, 0, zone)
        assertEquals(
            LocalDateTime.of(2026, 11, 18, 7, 0).atZone(zone.toZoneId()).toInstant().toEpochMilli(),
            next
        )
    }

    @Test fun next_trigger_uses_tomorrow_when_time_already_passed() {
        val now = LocalDateTime.of(2026, 11, 17, 8, 0).atZone(zone.toZoneId()).toInstant().toEpochMilli()
        val next = WakeAlarmTime.nextTriggerAt(now, 7, 0, zone)
        assertEquals(
            LocalDateTime.of(2026, 11, 18, 7, 0).atZone(zone.toZoneId()).toInstant().toEpochMilli(),
            next
        )
    }

    @Test fun is_due_is_true_just_after_the_armed_time() {
        val now = LocalDateTime.of(2026, 11, 18, 7, 0, 30).atZone(zone.toZoneId()).toInstant().toEpochMilli()
        assertTrue(WakeAlarmTime.isDue(now, 7, 0, zone))
        assertFalse(WakeAlarmTime.isDue(now, 7, 5, zone))
    }
}

class WakeAlarmRampTest {
    @Test fun first_tick_is_five_percent() = assertEquals(0.05f, WakeAlarmRamp.levelForTick(0))

    @Test fun each_tick_adds_five_percent() {
        assertEquals(0.10f, WakeAlarmRamp.levelForTick(1))
        assertEquals(0.55f, WakeAlarmRamp.levelForTick(10))
    }

    @Test fun level_never_exceeds_full_volume() = assertEquals(1.0f, WakeAlarmRamp.levelForTick(400))
}
```

- [ ] **Step 2: Run tests, expect failure**

Run: `sh gradlew testDebugUnitTest --tests "com.labix.WakeAlarmTimeTest" --tests "com.labix.WakeAlarmRampTest"`
Expected: FAIL, unresolved `WakeAlarmTime` / `WakeAlarmRamp`.

- [ ] **Step 3: Implement both objects**

`WakeAlarmTime` builds a `Calendar` in the given zone for today at hour/minute; if it is not after `nowMs`, add one day. `isDue` checks the armed time today is at or before `nowMs`.

- [ ] **Step 4: Run tests, expect pass**

Run: `sh gradlew testDebugUnitTest --tests "com.labix.WakeAlarm*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/player/WakeAlarmTime.kt app/src/main/java/com/labix/navirom/player/WakeAlarmRamp.kt app/src/test/java/com/labix/WakeAlarmTimeTest.kt app/src/test/java/com/labix/WakeAlarmRampTest.kt
git commit -m "feat: add wake alarm time math and ramp curve"
```

---

### Task 2: Snapshot persistence

**Files:**
- Create: `app/src/main/java/com/labix/navirom/player/WakeAlarmSnapshot.kt`
- Create: `app/src/main/java/com/labix/navirom/player/WakeAlarmPreferences.kt`
- Test: `app/src/test/java/com/labix/WakeAlarmSnapshotCodecTest.kt`

**Interfaces:**
- Consumes: `NaviromTrack` (`com.labix.navirom.data.model`).
- Produces:
  - `data class WakeAlarmSnapshot(val track: NaviromTrack, val queue: List<NaviromTrack>, val positionMs: Long)`
  - `object WakeAlarmSnapshotCodec { fun encode(snapshot: WakeAlarmSnapshot): String; fun decode(json: String?): WakeAlarmSnapshot? }`
  - `class WakeAlarmPreferences(sharedPreferences: SharedPreferences)` with
    `var enabled: Boolean`, `var hour: Int` (default 7), `var minute: Int` (default 0),
    `fun saveSnapshot(WakeAlarmSnapshot)`, `fun loadSnapshot(): WakeAlarmSnapshot?`,
    `fun clearSnapshot()`, `fun disarm()`.

- [ ] **Step 1: Write the failing codec test**

```kotlin
class WakeAlarmSnapshotCodecTest {
    private val track = NaviromTrack(id = "t1", title = "Song", artist = "Artist", album = "Album", durationSeconds = 200)

    @Test fun round_trips_track_queue_and_position() {
        val snapshot = WakeAlarmSnapshot(track, listOf(track), 42_000L)
        val decoded = WakeAlarmSnapshotCodec.decode(WakeAlarmSnapshotCodec.encode(snapshot))
        assertEquals(snapshot, decoded)
    }

    @Test fun broken_json_decodes_to_null() {
        assertNull(WakeAlarmSnapshotCodec.decode("{not json"))
        assertNull(WakeAlarmSnapshotCodec.decode(null))
    }
}
```

- [ ] **Step 2: Run test, expect failure**

Run: `sh gradlew testDebugUnitTest --tests "com.labix.WakeAlarmSnapshotCodecTest"`
Expected: FAIL, unresolved codec.

- [ ] **Step 3: Implement codec and preferences**

Codec uses Moshi (`Moshi.Builder().build()`), with `List<NaviromTrack>` via `Types.newParameterizedType`. Preferences use the same `SharedPreferences` file as the player state (`context.getSharedPreferences("navirom_player_state", Context.MODE_PRIVATE)` passed in) with keys prefixed `wake_alarm_`.

- [ ] **Step 4: Run tests, expect pass**

Run: `sh gradlew testDebugUnitTest --tests "com.labix.WakeAlarmSnapshotCodecTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/player/WakeAlarmSnapshot.kt app/src/main/java/com/labix/navirom/player/WakeAlarmPreferences.kt app/src/test/java/com/labix/WakeAlarmSnapshotCodecTest.kt
git commit -m "feat: persist wake alarm snapshot"
```

---

### Task 3: Scheduler, receivers and manifest

**Files:**
- Create: `app/src/main/java/com/labix/navirom/player/WakeAlarmScheduler.kt`
- Create: `app/src/main/java/com/labix/navirom/player/WakeAlarmReceiver.kt`
- Create: `app/src/main/java/com/labix/navirom/player/WakeAlarmBootReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `WakeAlarmTime`, `WakeAlarmPreferences`.
- Produces:
  - `class WakeAlarmScheduler(context: Context)` with `fun canScheduleExact(): Boolean`, `fun schedule(hour: Int, minute: Int)`, `fun cancel()`, `fun rearmIfPending()`
  - `WakeAlarmReceiver` handling `ACTION_FIRE_WAKE_ALARM = "com.labix.navirom.ACTION_FIRE_WAKE_ALARM"`
  - `WakeAlarmBootReceiver` handling boot/time broadcasts.

- [ ] **Step 1: Implement scheduler**

`schedule` stores hour/minute in preferences, computes `WakeAlarmTime.nextTriggerAt(System.currentTimeMillis(), hour, minute)`, and uses `AlarmManager.setAlarmClock(AlarmClockInfo(triggerAt, showIntent), pendingIntent)` when `canScheduleExact()`; otherwise `setAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)`. The pending intent targets `WakeAlarmReceiver` with `ACTION_FIRE_WAKE_ALARM` at request code 7301.

- [ ] **Step 2: Implement receivers**

`WakeAlarmReceiver.onReceive` reads preferences, marks the alarm disarmed (`preferences.disarm()`), and starts the service:

```kotlin
val intent = Intent(context, NaviromPlaybackService::class.java).setAction(NaviromPlaybackService.ACTION_WAKE_ALARM)
ContextCompat.startForegroundService(context, intent)
```

`WakeAlarmBootReceiver.onReceive` checks `enabled`; when `WakeAlarmTime.isDue(now, hour, minute)` fire immediately via the same intent, otherwise `rearmIfPending()`.

- [ ] **Step 3: Register in the manifest**

Add `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` and `<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />`. Register `WakeAlarmReceiver` (`android:exported="false"`) and `WakeAlarmBootReceiver` (`android:exported="true"`) with `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` and `MY_PACKAGE_REPLACED`.

- [ ] **Step 4: Verify build**

Run: `sh gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/player/WakeAlarmScheduler.kt app/src/main/java/com/labix/navirom/player/WakeAlarmReceiver.kt app/src/main/java/com/labix/navirom/player/WakeAlarmBootReceiver.kt app/src/main/AndroidManifest.xml
git commit -m "feat: schedule exact wake alarms and re-arm on boot"
```

---

### Task 4: Sleep-timer snapshot and suppression

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/player/SleepTimerCompletion.kt`
- Modify: `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt:1802-1894`
- Test: `app/src/test/java/com/labix/SleepTimerCompletionTest.kt`

**Interfaces:**
- Consumes: `WakeAlarmPreferences`, `WakeAlarmSnapshot`.
- Produces: `SleepTimerCompletion.run(options, pausePlayback, clearTimer, executeActions, onExpirySnapshot: () -> Unit)` — `onExpirySnapshot` runs before `pausePlayback`. `AudioPlayerController` gains `fun setWakeAlarmActive(active: Boolean)` and `private fun saveWakeAlarmSnapshot()`.

- [ ] **Step 1: Extend the existing completion test**

Add a test that records call order: snapshot before pause before clear before actions; keep the existing assertions passing.

- [ ] **Step 2: Run it, expect failure** — `sh gradlew testDebugUnitTest --tests "com.labix.SleepTimerCompletionTest"`

- [ ] **Step 3: Wire the controller**

`saveWakeAlarmSnapshot()` stores `WakeAlarmSnapshot(currentTrack, queue, currentPositionMs)` when a track exists. Call it from the `onExpirySnapshot` lambda in both `setSleepTimer` and `startNightSleepTimer`. Guard both timer starts with `if (wakeAlarmActive) return`, and skip `saveWakeAlarmSnapshot()` when `wakeAlarmActive`.

- [ ] **Step 4: Run tests, expect pass** — `sh gradlew testDebugUnitTest --tests "com.labix.SleepTimerCompletionTest"`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/player/SleepTimerCompletion.kt app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt app/src/test/java/com/labix/SleepTimerCompletionTest.kt
git commit -m "feat: snapshot playback on sleep-timer expiry and suppress timers during alarm"
```

---

### Task 5: Controller alarm playback and ramp

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt`
- Modify: `app/src/main/java/com/labix/navirom/data/model/Models.kt:90-108`

**Interfaces:**
- Consumes: `WakeAlarmRamp`, `WakeAlarmSnapshot`, `WakeAlarmPreferences`.
- Produces: `PlaybackState.isWakeAlarmActive: Boolean = false`; controller methods `fun startWakeAlarm(snapshot: WakeAlarmSnapshot?)`, `fun stopWakeAlarm()`, `val isWakeAlarmActive: Boolean`.

- [ ] **Step 1: Add the state flag** to `PlaybackState` and keep every existing `copy(...)` compiling.

- [ ] **Step 2: Implement `startWakeAlarm`**

Behaviour: if `playbackState.isPlaying` is true, only start the ramp (no seek, no restart). Otherwise restore the snapshot track/queue (`playTrack` with the queue context), `seekTo(snapshot.positionMs)` after the player is prepared, raise system media volume to `max(current, 60 %)` remembering the old value, set `wakeAlarmActive = true`, then launch `alarmRampJob` on the controller scope ticked by `WakeAlarmRamp.TICK_MS`, skipping ticks while `!playbackState.isPlaying`, applying `WakeAlarmRamp.levelForTick(tick)`.

- [ ] **Step 3: Apply the multiplier centrally**

Add `private var alarmVolumeScale = 1f` and multiply it into every primary `mediaPlayer?.setVolume(` write used by crossfade/fade/focus code (`AudioPlayerController.kt` lines around 366-383, 1141-1215, 1634). Secondary player writes stay untouched.

- [ ] **Step 4: Implement `stopWakeAlarm`**

Cancel the ramp, set `alarmVolumeScale = 1f`, restore the remembered system volume, clear `isWakeAlarmActive`, pause playback, and persist `enabled = false` through `WakeAlarmPreferences`.

- [ ] **Step 5: Verify build and existing tests**

Run: `sh gradlew compileDebugKotlin && sh gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt app/src/main/java/com/labix/navirom/data/model/Models.kt
git commit -m "feat: play the wake alarm with a pausing volume ramp"
```

---

### Task 6: Service actions and notification Stop

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/player/NaviromPlaybackService.kt`

**Interfaces:**
- Consumes: controller alarm API, `WakeAlarmPreferences`.
- Produces: `NaviromPlaybackService.ACTION_WAKE_ALARM` and `ACTION_ALARM_STOP` constants.

- [ ] **Step 1: Handle the actions in `onStartCommand`**

`ACTION_WAKE_ALARM` → `player.startWakeAlarm(WakeAlarmPreferences(...).loadSnapshot())` then `updatePlaybackAndNotification()`. `ACTION_ALARM_STOP` → `player.stopWakeAlarm()` then `updatePlaybackAndNotification()`.

- [ ] **Step 2: Add the notification Stop action**

In the media notification builder (around `NaviromPlaybackService.kt:937-985`), when `state.isWakeAlarmActive`, prepend an action with `R.drawable.ic_notif_music`-style icon, label from `NaviromStrings.get("wake_alarm_stop", language)`, targeting the service with `ACTION_ALARM_STOP`.

- [ ] **Step 3: Verify build**

Run: `sh gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/labix/navirom/player/NaviromPlaybackService.kt
git commit -m "feat: start and stop the wake alarm from the playback service"
```

---

### Task 7: Strings and ViewModel surface

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt` (three language blocks)
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt`

**Interfaces:**
- Produces: strings `wake_alarm`, `wake_alarm_desc`, `wake_alarm_enable`, `wake_alarm_time`, `wake_alarm_armed_until`, `wake_alarm_exact_permission`, `wake_alarm_stop`, `wake_alarm_running`; ViewModel state `val wakeAlarmEnabled: StateFlow<Boolean>`, `val wakeAlarmHour: StateFlow<Int>`, `val wakeAlarmMinute: StateFlow<Int>`, and `fun setWakeAlarm(enabled: Boolean, hour: Int, minute: Int)`.

- [ ] **Step 1: Add strings** in English, Albanian and German (e.g. EN "Wake alarm", "Resume the paused song at this time with rising volume", "Enable wake alarm", "Wake time", "Alarm set for %02d:%02d", "Allow exact alarms in system settings for an on-time start", "Stop alarm", "Wake alarm running"; AL/DE mirrors).

- [ ] **Step 2: Add ViewModel state** backed by `WakeAlarmPreferences`; `setWakeAlarm` saves preferences and calls `WakeAlarmScheduler(application).schedule(hour, minute)` or `cancel()`.

- [ ] **Step 3: Pass the state into `FullPlayerModal`** through `NaviromApp` (same pattern as the sleep timer).

- [ ] **Step 4: Verify build** — `sh gradlew compileDebugKotlin`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt app/src/main/java/com/labix/navirom/ui/NaviromApp.kt
git commit -m "feat: expose wake alarm settings to the UI"
```

---

### Task 8: Alarm UI in player and sleep-timer sheet

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/components/CoverQuickActionsSheet.kt`

**Interfaces:**
- Consumes: Task 7 strings and state, existing sleep-timer composables.

- [ ] **Step 1: Add the alarm block to the Sleep Timer dialog** in `FullPlayerModal` below the night-auto row: switch bound to `wakeAlarmEnabled`, a time row opening `TimePickerDialog`, and a status line. When exact alarms are not permitted, show `wake_alarm_exact_permission` and open `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` on tap.

- [ ] **Step 2: Add the running-alarm banner** at the top of the player content when `playbackState.isWakeAlarmActive`: text `wake_alarm_running` and a Stop button calling `onStopWakeAlarm`.

- [ ] **Step 3: Mirror the armed/stop state** in `CoverQuickActionsSheet`'s timer card: show `wake_alarm_armed_until` when armed, Stop when running.

- [ ] **Step 4: Build, install and check visually**

Run: `sh gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: no squeezed text; alarm rows readable in dark and light themes.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt app/src/main/java/com/labix/navirom/ui/components/CoverQuickActionsSheet.kt
git commit -m "feat: add wake alarm controls to the player"
```

---

### Task 9: End-to-end device check

**Files:** none.

- [ ] **Step 1: Arm a wake alarm one minute ahead** with a sleep timer expiring first, and confirm the track pauses, then resumes in place at 5 % volume with 15 s steps.
- [ ] **Step 2: Pause and resume the alarm** and confirm the level holds during pause.
- [ ] **Step 3: Press Stop** in the notification and confirm playback stops and volumes are restored.
- [ ] **Step 4: Reboot the phone** with an alarm armed and confirm it re-arms (and fires late if the time passed).
- [ ] **Step 5: Record the results** in the plan's task checkboxes; file follow-ups for anything that fails.
