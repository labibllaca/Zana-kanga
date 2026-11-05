# Task Plan: Whole-App Adaptive Layout

Use this file as the durable roadmap for the task. Create it before complex work and keep it current as phases change.

## Goal

Make all primary Android app surfaces adapt to compact, tablet, and landscape windows, especially tablet landscape, while preserving phone behavior and visual identity.


## Next Step

Record the single action that should happen next. Update it whenever the active phase or immediate action changes.

Deliver the implementation with explicit verification and device-coverage limits; tablet screenshots remain unavailable.

## Current Phase

Name the phase currently being worked on.

Phase 5

## Phases

Break the task into three to seven verifiable phases. Use only `pending`, `in_progress`, or `complete` for each status and update the value when work advances.

### Phase 1: Requirements & Discovery

- [x] Understand user intent
- [x] Identify constraints and requirements
- [x] Document findings in findings.md
- **Status:** complete

### Phase 2: Planning & Structure

- [x] Define technical approach
- [x] Create project structure if needed
- [x] Document decisions with rationale
- **Status:** complete

### Phase 3: Implementation

- [x] Execute the plan step by step
- [x] Write code to files before executing
- [x] Test incrementally
- **Status:** complete

### Phase 4: Testing & Verification

- [x] Run JVM, connected instrumentation, build, and whitespace checks
- [x] Document test results and device limitations in progress.md
- [x] Fix issues found in code/build verification
- [ ] Capture tablet visual matrix (unavailable: no tablet target/emulator)
- **Status:** complete with hardware limitation

### Phase 5: Delivery

- [x] Review responsive UI files and preserve pre-existing Sleep Timer work
- [x] Ensure implementation and validation results are recorded
- [x] Deliver outcomes and limitations
- **Status:** complete

## Key Questions

Record important questions and replace them with answers as they are resolved.

1. No tablet target available; `emulator` executable/default SDK path absent. Galaxy S21 is the only connected device; manual launch for screenshot failed.
2. Notification prompt no longer blocks connected tests: fresh connected run passed 4/4.

## Decisions Made

Record significant choices and the reason for each one.

| Decision | Rationale |
|----------|-----------|
| Use width thresholds 600dp and 840dp, not device model/orientation checks. | Standard compact/medium/expanded breakpoints; works for split-screen and resizing. |
| Implement in current checkout. | User previously selected native execution in the current workspace; preserve uncommitted Sleep Timer work. |
| Bounded single-column settings/statistics/player layouts instead of broad two-pane rewrites. | Preserve existing form/player state and avoid risky restructuring of large composables; true multi-pane layouts remain a future iteration. |

## Errors Encountered

Record each distinct error, the attempt number, and the resolution. Change the approach before retrying a failed action.

| Error | Attempt | Resolution |
|-------|---------|------------|
| Worktree check found normal `main` checkout with uncommitted Sleep Timer work. | 1 | Continue in-place under prior explicit current-checkout preference; avoid omitting dirty work during worktree transfer. |

## Notes

- Update phase status as work progresses: `pending` to `in_progress` to `complete`.
- Re-read the goal and next step before major decisions.
- Log errors promptly so failed approaches are not repeated.
