# Night Sleep Timer — Design

Date: 2026-11-04
Status: Approved

## Intent

When enabled, automatically pause music after 30 minutes of actual playback if playback begins between 23:00 and 06:00. This gives the listener a nightly sleep timer without having to set one each time.

## Goals

- Add an “Auto-start at night” option to the existing Sleep Timer UI.
- Persist the option so it remains enabled across app restarts.
- Start a 30-minute timer when playback starts during the local-time window 23:00–06:00.
- Count only while playback is active; pause the countdown with playback and resume it with playback.
- Replace any currently active manual sleep timer when the automatic night timer starts.
- Pause playback when 30 minutes of active playback have elapsed.

## Non-goals

- A configurable schedule, duration, or time window.
- Changing existing manual sleep-timer behavior or its end actions.
- A separate Night Mode screen or timer implementation.

## Approaches

1. **Extend the existing Sleep Timer (chosen).** Add a persistent toggle to its current UI and reuse its countdown and pause behavior. This avoids duplicating timer logic.
2. **Create a separate Night Mode setting.** Keeps the settings concept distinct, but duplicates countdown behavior and risks inconsistent timer semantics.

## Behavior and constraints

- The setting is off by default and uses the device’s local time.
- The night window includes 23:00 through 05:59; a playback start at 06:00 does not trigger it.
- Once started, the timer continues beyond 06:00 if needed; the qualifying event is playback starting inside the window.
- Pausing playback freezes the remaining time; resuming continues it.
- An automatic timer replaces any active manual sleep timer. Existing manual timer end actions are not applied to this automatic timer.
- Timer expiry pauses playback and clears the timer, following the existing sleep-timer path.

## Architecture and tests

The existing sleep timer is owned by `AudioPlayerController` (`sleepTimerJob` and `setSleepTimer`), surfaced through `NaviromViewModel`, and configured in `FullPlayerModal`. Extend this path rather than creating a parallel timer module. Keep the enablement preference with the existing sleep-timer preferences.

Tests should cover the overnight time-window boundaries, automatic start on playback start, pause/resume countdown behavior, replacement of a manual timer, and no automatic start outside the window. Preserve the existing manual sleep-timer behavior when the option is off.

## Builder calls

- Keep the option in the existing Sleep Timer UI, beside its timer controls.
- Use the app’s existing playback state as the signal for playback starting/pausing.
- Continue a qualifying countdown if playback crosses 06:00 or is resumed after 06:00.
