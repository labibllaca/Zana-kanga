# Wake Alarm — Design

Date: 2026-11-17
Status: Draft (awaiting approval)

## Intent

After the sleep timer stops playback, the listener wants music to come back
at a chosen wake-up time, continuing the exact song from the point where the
countdown stopped. The volume starts low and increases on its own until the
listener stops the alarm.

## Goals

- Let the listener set a wake-up time in the existing Sleep Timer area.
- When the sleep timer expires, remember the current track, its position and
  the queue so the alarm can continue from that spot.
- Fire the alarm at the exact chosen time, even if the app was swiped away.
- Start quietly and raise the volume every 15 seconds up to full volume.
- Pause the volume ramp whenever playback is paused; continue it on resume.
- Provide a clear Stop control (notification and in-app) that ends the alarm.
- Keep the alarm armed across reboots and timezone/time changes.

## Non-goals

- Snooze, weekday schedules, multi-alarm lists.
- Wake sounds other than the library track that was stopped.
- Wake-up while the phone is powered off.
- Changing the existing sleep-timer countdown behaviour or its radio-shutdown
  actions.
- Ramping the secondary deck volume.

## Approaches

1. **Exact `AlarmManager` alarm plus the existing foreground playback service
   (chosen).** `setAlarmClock` wakes the service on time through Doze, the
   service restores the snapshot and runs the ramp, and the media notification
   carries a Stop action. Costs one runtime permission prompt
   (`SCHEDULE_EXACT_ALARM`, Android 12+).
2. **WorkManager.** No extra permission, but the start can drift by minutes in
   Doze, which defeats a wake alarm.
3. **In-process timer inside the running service.** No permission prompt, but
   the alarm dies with an app kill or device restart, so it cannot be trusted
   overnight.

## Behaviour and constraints

- One alarm at a time. Setting a new time replaces the old alarm.
- The alarm is one-shot: after it fires, the alarm switch turns off and the
  listener re-arms it explicitly.
- Snapshot rule: when the sleep timer expires, store the current track, the
  paused position and the current queue. If no snapshot exists at fire time,
  fall back to the last persisted player track and position.
- Already playing at fire time: start only the ramp, never seek or restart the
  running track.
- While the alarm is running, sleep-timer expiries (manual and night-auto) are
  suppressed so they cannot pause the ramp, and no snapshot is written.
- Ramp: start at 5 %, add 5 % every 15 seconds, cap at 100 % (about 4¾ minutes
  to full volume). The ramp is a single multiplier applied centrally so
  crossfade and fade code cannot overwrite it.
- System media volume is raised to at least 60 % when the alarm starts and the
  previous value is restored on Stop.
- Pause holds the ramp: pausing playback freezes the level and the 15-second
  tick; resuming continues from the same step.
- Stop ends the alarm, cancels the ramp, disarms it, restores normal and system
  volume and pauses playback. Stop exists in the notification and in an in-app
  player banner.
- Exactness: use `setAlarmClock`. On Android 12+ require
  `SCHEDULE_EXACT_ALARM`; when it is denied, explain why and fall back to
  `setAndAllowWhileIdle` (may drift) instead of failing silently.
- Start from background: the alarm broadcast starts the foreground service with
  `mediaPlayback`, which exact alarms are allowed to do on current Android
  versions.
- Audio focus: the alarm requests focus like normal playback; other audio is
  interrupted.
- Reboot/time: re-arm from `BOOT_COMPLETED`, `TIME_SET` and
  `TIMEZONE_CHANGED` when an alarm is pending. If the re-arm happens after the
  armed time, the alarm fires immediately (late fire) instead of skipping the
  day.
- Ramp output is the phone's own audio path (speaker, wired, Bluetooth). With a
  WLAN/UPnP speaker the alarm plays locally; the speaker volume is not ramped.
- Missing track: if the snapshot track is gone, play the next playable queue
  item; if none, post a notification saying the track is unavailable and
  disarm.
- Radios stay off: the sleep-timer radio shutdowns are not undone at wake time.
  A streaming-only track therefore needs the notification to explain that
  playback failed for lack of network.
- Ramp state is not persisted; a process death during the alarm restores
  playback at full volume rather than resuming mid-ramp.


## UX

- Surface: a "Wecker / Wake alarm" block inside the existing Sleep Timer modal,
  below the duration and night-auto controls: switch, time field (HH:mm) that
  opens a time picker, one line describing the ramp, and the active-alarm badge
  with the next fire time.
- Flow: enable switch -> pick time -> grant exact-alarm permission if prompted
  -> sleep timer expires and the snapshot is stored -> alarm fires at the set
  time -> volume rises -> Stop in the notification or the player ends it.
- Notification: ongoing "Wecker läuft" notification with Stop, alongside the
  usual media controls.
- Words: new `NaviromStrings` keys in English, Albanian and German.
- States: switch reflects the saved alarm; badge shows armed time; after firing,
  the switch is off; permission-denied shows the fallback explanation once.

## Architecture and tests

- `WakeAlarmPreferences` (SharedPreferences, like
  `SleepTimerOptionsPreferences`): enabled flag, hour/minute, snapshot
  track/queue/position.
- `WakeAlarmScheduler`: exact-permission check, `setAlarmClock` scheduling,
  cancel, re-arm on boot/time changes.
- `WakeAlarmReceiver`: receives the alarm and starts
  `NaviromPlaybackService` with a new `ACTION_WAKE_ALARM`.
- `AudioPlayerController`: `startWakeAlarm()` restores snapshot, seeks and plays
  at the ramp start level; ramp job owned next to `sleepTimerJob`; pause/resume
  hooks hold and continue the ramp; `stopWakeAlarm()` cleans up. Sleep-timer
  completion stores the snapshot through the same preferences.
- `NaviromViewModel` and `FullPlayerModal` expose and edit the alarm settings.
- Tests: ramp step arithmetic and cap, ramp pause/resume, snapshot round-trip,
  missing-snapshot fallback, permission-denied fallback path, boot re-arm,
  and unavailable-track fallback.

## Builder calls

- One-shot alarm that turns itself off after firing (not a daily repeat).
- Ramp curve: 5 % start, +5 % every 15 s, 100 % cap.
- Stop pauses playback and restores full volume rather than leaving music
  running.
- Snapshot is taken at sleep-timer expiry only; other pauses do not overwrite
  it.
- Secondary deck volume is untouched by the ramp.
- Keep the alarm settings inside the Sleep Timer modal instead of a new screen.
