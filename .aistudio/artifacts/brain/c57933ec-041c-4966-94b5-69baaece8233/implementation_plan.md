# Implementation Plan: Restructuring to Continuous Horizontal Space

## Overview
Restructure the application to a continuous horizontal navigation model. The app will be divided into three distinct horizontal zones, navigable via a `HorizontalPager` or equivalent swipeable container:
1. **Left (Player Zone)**: Immersive, focused on active listening (current Astrolabe full player).
2. **Middle (Library Zone)**: Browsing experience for albums, playlists, artists, and tracks (restructured content from existing `TabContent`).
3. **Right (Settings Zone)**: App controls, utilities, audio quality, and account management.

## Key Technical Changes
- **Navigation**: Replace the `Scaffold` + `NavigationBar`/`NavigationRail` architecture with a top-level `HorizontalPager` (using `androidx.compose.foundation.pager.HorizontalPager`).
- **State Management**:
    - Manage `PagerState` to track the user's location in the horizontal space.
    - Persistent `MiniPlayerBar` overlayed at the bottom of the `HorizontalPager` container, visible across all zones.
- **Component Refactoring**:
    - `ContinuousPlayerZone`: Refactor current player UI as the left-most page.
    - `ContinuousLibraryZone`: Extract library-focused content from `TabContent` for the central page.
    - `ContinuousSettingsZone`: Extract settings-focused content from `TabContent` for the right-most page.
- **Visuals**: Incorporate the concentric ring and astrolabe visual language as background/framing elements across the three zones.

## Phases
1. **Infrastructure**: Setup `HorizontalPager` within `NaviromApp`.
2. **Zone Creation**: Implement the 3 screen zones using existing Composables and ViewModel data.
3. **Overlay & Persistence**: Move the `MiniPlayerBar` outside the pager and overlay it globally.
4. **Polish**: Add horizontal navigation gestures, indicator/trail showing location, and ensure smooth transitions.
