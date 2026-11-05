# Whole-App Tablet Adaptive Layout — Design

Date: 2026-11-04
Status: Approved

## Intent

Make the existing Android app feel purpose-designed on tablets, especially in landscape, without making phone layouts larger or changing the app's visual identity. The app currently uses one pager-based shell, a modal drawer, and phone-oriented screen compositions; no window-width or orientation adaptation was found.

## Goals

- Adapt structure to available window width rather than device model or orientation alone, including split-screen and resized windows.
- Keep compact phone behavior intact while using tablet width for useful navigation, content density, and side-by-side layouts.
- Make the full player, Library, Playlists, Search, Folders, and Settings comfortable in tablet portrait and landscape.
- Preserve existing playback, selection, back navigation, mini-player, accessibility, and Material 3 behavior.

## Non-goals

- Redesigning the visual brand, changing product copy, or removing features.
- Locking orientation or treating landscape as the only tablet configuration.
- Introducing a separate tablet-only feature set or device-model checks.

## Approaches

1. **Window-size adaptive layouts (chosen):** use Android window size classes / available width to switch navigation and screen composition at compact, medium, and expanded widths. This handles tablet portrait, landscape, foldables, split-screen, and resized windows with one rule set.
2. **Landscape-only rearrangement:** minimal initial implementation, but misses portrait tablets and multi-window configurations; rejected.
3. **Scale existing phone compositions:** least code, but creates stretched content, excessive scan distance, and poor landscape balance; rejected.

## Design direction

- Treat this as an Operate surface: clarity, scanability, and task flow take priority over decorative changes.
- Keep the existing colors, typography, components, and motion. Use Material 3 navigation and window insets.
- At compact widths, retain current navigation and phone compositions.
- At wider widths, replace the modal-only primary navigation with an appropriate persistent rail/sidebar while preserving the app's existing destinations and selection state.
- On expanded screens, use bounded content widths and multi-column grids for browsable collections; use list/detail or side-by-side panes where it makes a current task faster rather than merely filling space.
- Recompose the full player for landscape/expanded width so artwork and lyrics/details can sit alongside playback controls instead of stretching vertically; keep timer and other controls reachable.
- Keep settings readable using adaptive column count and bounded form width. Adapt sheets/dialogs to wider windows without making controls harder to reach.
- Choose breakpoints from available window size classes and measured content requirements, not fixed device dimensions.

## Constraints and verification

- Native Kotlin/Jetpack Compose; use current app state and existing navigation destinations.
- Preserve all existing phone behavior and accessibility semantics/touch targets.
- Build and run unit/instrumentation tests, then capture representative phone and tablet layouts in portrait and landscape, including dark mode and increased font scale. Use a tablet emulator/device if available; report any unavailable hardware verification.
- The existing device check for Sleep Timer persistence is separate and remains pending the user's notification-permission choice.

## Implementation calls left to the builder

- Select maintained AndroidX window-size APIs compatible with this project's Compose/Android dependencies.
- Choose exact breakpoints and per-screen pane arrangements after inspecting each screen's current composition.
- Preserve back-stack and pager behavior while adapting the shell navigation.
