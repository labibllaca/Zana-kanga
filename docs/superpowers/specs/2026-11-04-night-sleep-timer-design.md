# Night Sleep Timer — Design

Date: 2026-11-04
Status: Approved (revised)

## Intent

When enabled, automatically pause music after 30 minutes of actual playback if playback begins between 23:00 and 06:00. This gives the listener a nightly sleep timer without having to set one each time.

## Goals

- Add an “Auto-start at night” option to the existing Sleep Timer UI.
- Persist the option so it remains enabled across app restarts.
- Start a 30-minute timer when playback starts during the local-time window 23:00–06:00.
- Count only while playback is active; pause the countdown with playback and resume it with playback.
- Replace any currently active manual sleep timer when the automatic night timer starts.
- Pause playback when 30 minutes of active playback have elapsed.
- Add GPS to the existing Sleep Timer shutdown choices and persist it with the other choices.
- Apply the same selected Bluetooth, Wi-Fi, mobile-data, and GPS shutdown choices when either the manual timer or automatic night timer expires.

## Non-goals

- A configurable schedule, duration, or time window.
- Adding separate night-only shutdown controls; both timer types use the same saved choices.
- Changing existing manual sleep-timer timing behavior.
- A separate Night Mode screen or timer implementation.

## Approaches

1. **Share the existing Sleep Timer choices (chosen).** Add GPS to the current UI and persist it with Bluetooth, Wi-Fi, and mobile-data choices. The night timer uses this same selection at expiry. This avoids duplicate controls and keeps shutdown behavior consistent.
2. **Create a separate Night Mode setting.** Keeps the settings concept distinct, but duplicates countdown behavior and risks inconsistent timer semantics.

## Behavior and constraints

- The setting is off by default and uses the device’s local time.
- The night window includes 23:00 through 05:59; a playback start at 06:00 does not trigger it.
- Once started, the timer continues beyond 06:00 if needed; the qualifying event is playback starting inside the window.
- Pausing playback freezes the remaining time; resuming continues it.
- An automatic timer replaces any active manual sleep timer; the shared currently selected end actions apply at expiry.
- Either timer's expiry pauses playback, clears the timer, and then runs the selected Bluetooth, Wi-Fi, mobile-data, and GPS shutdown actions through the existing connection manager.
- The four choices default to off and are saved in the existing sleep-timer preferences. Updating the choices while a timer is active updates the choices that will be applied when it expires.
- Shutdown is best-effort and platform-dependent. `WRITE_SECURE_SETTINGS` is granted on the connected Android 15 device, but this permission does not bypass every Android restriction (notably Bluetooth disable restrictions on modern target SDKs). Report or preserve existing fallback behavior rather than promising every selected radio can always be switched off.

## UX

- Flow: open Sleep Timer -> choose timer duration and shared end actions (including GPS) -> set or turn on Auto-start at night -> timer expires -> playback pauses and selected actions run.
- Surface: existing Sleep Timer modal; add GPS alongside Bluetooth, Wi-Fi, and mobile-data switches, retaining the existing Auto-start at night control.
- Words: “Turn off GPS” with concise explanatory text; use the existing “Actions on timer end” grouping for all four choices.
- States: selected switches reflect saved preferences on reopen; unavailable system shutdowns retain the connection manager's existing guidance/fallback behavior.
- Access: retain existing switch semantics, labels, and keyboard/Compose accessibility behavior.

## Architecture and tests

The existing sleep timer is owned by `AudioPlayerController` (`sleepTimerJob` and `setSleepTimer`), surfaced through `NaviromViewModel`, and configured in `FullPlayerModal`. Extend this path rather than creating a parallel timer module. Persist GPS alongside existing sleep-timer options and pass the current shared options to both manual and automatic expiry actions.

Tests should cover the overnight time-window boundaries, automatic start on playback start, pause/resume countdown behavior, replacement of a manual timer, and no automatic start outside the window. Also cover GPS preference persistence and verify both expiry paths invoke the selected action set. Preserve existing manual timer duration/countdown behavior.

## Builder calls

- Keep the option in the existing Sleep Timer UI, beside its timer controls.
- Use the app’s existing playback state as the signal for playback starting/pausing.
- Continue a qualifying countdown if playback crosses 06:00 or is resumed after 06:00.
- Use the user-selected shared shutdown actions for automatic expiry rather than resetting the automatic timer to empty/default actions.
