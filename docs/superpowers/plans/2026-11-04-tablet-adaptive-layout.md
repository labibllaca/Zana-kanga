# Whole-App Tablet Adaptive Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every primary app surface adapt to tablet and landscape windows without stretching phone layouts or changing the existing visual identity.

**Architecture:** Add a small, tested width-class model derived from the current Compose window constraints. Use it in the app shell and screen compositions to adapt navigation, content bounds/density, and player arrangement. Keep existing state, destinations, callbacks, Material 3 theme, and compact-width behavior.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit, Android instrumentation/UI verification, existing Gradle tasks.

**Spec:** `docs/superpowers/specs/2026-11-04-tablet-adaptive-layout-design.md`

## Global Constraints

- Adapt structure to available window width rather than device model or orientation alone, including split-screen and resized windows.
- Keep compact phone behavior intact while using tablet width for useful navigation, content density, and side-by-side layouts.
- Make the full player, Library, Playlists, Search, Folders, and Settings comfortable in tablet portrait and landscape.
- Preserve existing playback, selection, back navigation, mini-player, accessibility, and Material 3 behavior.
- Keep the existing colors, typography, components, and motion. Use Material 3 navigation and window insets.
- Choose breakpoints from available window size classes and measured content requirements, not fixed device dimensions.
- Preserve all existing phone behavior and accessibility semantics/touch targets.

## Review Focus

- Window resizes across compact/medium/expanded widths while on a non-default tab: selected destination and screen state remain stable. Test the width classifier and rotate/resize during device verification.
- Landscape has short height as well as wide width: controls remain reachable without clipping. Verify full player and settings at tablet landscape and increased font scale.
- Wide content does not become too spread out: bound reading/forms and increase grid columns only as room allows. Verify Library, Search, Playlists, and Settings at tablet widths.
- Adaptive navigation preserves Back and mini-player interactions across player and tabs. Verify enter/exit player, switch destinations, and system Back in compact and expanded layouts.
- Keyboard, system bars, and bottom inset coexist with wider layouts: verify Search and Settings with keyboard visible and system navigation insets.

---

## File map

- Create `app/src/main/java/com/labix/navirom/ui/layout/WindowWidthClass.kt`: compact/medium/expanded classification from available width.
- Create `app/src/main/java/com/labix/navirom/ui/components/ResponsivePrimaryNavigation.kt`: Material 3 rail for the five existing `NaviromTab` destinations at expanded widths.
- Modify `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt`: read constraints once, adapt top-level navigation/content frame, and pass width class to screen destinations without changing ViewModel ownership.
- Modify `app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt`: restructure the artwork/lyrics and controls for medium/expanded landscape windows; preserve compact player behavior.
- Modify `app/src/main/java/com/labix/navirom/ui/screens/LibraryScreen.kt`, `PlaylistsScreen.kt`, `SearchScreen.kt`, `MusicFoldersScreen.kt`, and `ServerSettingsScreen.kt`: apply bounded content, suitable grid/column density, and wider-window detail layouts where already supported by state.
- Modify `app/src/main/java/com/labix/navirom/ui/components/ArtistDetailView.kt` and playlist/detail screen components only if needed to complete their split/list-detail adaptation; avoid broad component rewrites.
- Create `app/src/test/java/com/labix/WindowWidthClassTest.kt`: boundary tests for the width-class classifier.
- Create `app/src/androidTest/java/com/labix/ResponsivePrimaryNavigationTest.kt`: rail destination and selection callback contract.
- Add Compose instrumentation assertions for stable screen/player semantics; use device/emulator captures for visual geometry.

### Task 1: Establish responsive width classes

**Files:**
- Create: `app/src/main/java/com/labix/navirom/ui/layout/WindowWidthClass.kt`
- Test: `app/src/test/java/com/labix/WindowWidthClassTest.kt`

**Interfaces:**
- Produces: `enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }` and `fun windowWidthClass(widthDp: Dp): WindowWidthClass`.
- Breakpoints: compact `< 600.dp`; medium `600.dp..<840.dp`; expanded `>= 840.dp`.

- [x] **Step 1: Write boundary tests**

Test exact boundaries at 599, 600, 839, and 840 dp, plus a representative very-wide window.

- [x] **Step 2: Run the focused test and verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.WindowWidthClassTest"`
Expected: compile/test failure because the classifier is not defined.

- [x] **Step 3: Implement the classifier**

Use a pure `Dp` comparison function with no Android device/configuration lookup and no new dependency.

- [x] **Step 4: Run the focused test**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.WindowWidthClassTest"`
Expected: all boundary tests pass.

### Task 2: Adapt the app shell and navigation

**Files:**
- Create: `app/src/main/java/com/labix/navirom/ui/components/ResponsivePrimaryNavigation.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/components/SidebarDrawer.kt` only if the existing drawer content is needed to preserve collection controls.
- Test: `app/src/androidTest/java/com/labix/ResponsivePrimaryNavigationTest.kt`

**Interfaces:**
- Consumes: `WindowWidthClass` from Task 1.
- Produces: `ResponsivePrimaryNavigation(selectedTab: NaviromTab, onTabSelected: (NaviromTab) -> Unit, modifier: Modifier = Modifier)` with the five `NaviromTab` destinations; compact-width navigation unchanged and a persistent Material 3 `NavigationRail` at expanded width. Medium width may use the rail only when actual constraints provide sufficient room.

- [x] **Step 1: Add a shell test for destination selection and selected state**

Add Compose UI tests `expandedRailShowsAllPrimaryDestinations` and `selectingDestinationCallsCallback`. Assert Library, Playlists, Folders, Search, and Settings destinations are present, current selection is marked selected, and selecting a different item invokes the callback with that exact `NaviromTab`.

- [x] **Step 2: Run the focused test and verify the expected responsive navigation is absent**

Run: `gradle :app:connectedDebugAndroidTest`
Expected: compilation/test failure because `ResponsivePrimaryNavigation` is not yet defined.

- [x] **Step 3: Derive width from `BoxWithConstraints` and adapt the shell**

Keep the pager and its player/page state model. Place the new rail beside the page content at expanded width, preserve top-bar actions and compact interactions, and do not create a second source of selected-tab state. Add the navigation at root in the same shell pass, passing `currentTab` and `viewModel.setTab`.

- [x] **Step 4: Verify compact and expanded navigation tests**

Run: `gradle :app:connectedDebugAndroidTest`
Expected: both compact and expanded destination behavior pass.

### Task 3: Make browsable screens use tablet width well

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/ui/screens/LibraryScreen.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/screens/PlaylistsScreen.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/screens/SearchScreen.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/screens/MusicFoldersScreen.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/components/ArtistDetailView.kt` as needed for master/detail.

**Interfaces:**
- Consumes: current `WindowWidthClass` supplied by the app shell; preserve existing callbacks and screen state.
- Produces: `responsiveContentMaxWidth(widthClass)` returning unbounded width for compact, 960.dp for medium, and 1280.dp for expanded, used by a shared centered content frame. Existing adaptive grids then derive their column counts from available width; compact widths retain current single-pane navigation.

- [x] **Step 1: Add unit tests for responsive content bounds**

Test `responsiveContentMaxWidth` at compact, medium, and expanded classes, asserting unbounded, 960.dp, and 1280.dp respectively. Keep the width-change selection interaction as part of final device verification because the pending system permission dialog currently blocks Compose instrumentation.

- [x] **Step 2: Run the focused test and verify the wider layout contract fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.ResponsiveContentFrameTest"`
Expected: compile/test failure because `responsiveContentMaxWidth` is not yet defined.

- [x] **Step 3: Adapt collection and detail composition**

Use a shared `ResponsiveContentFrame` around active tab content. Bound medium and expanded widths, center the frame, and let existing `GridCells.Adaptive` grids use the resulting constraints. Do not change data flow or duplicate ViewModel state.

- [x] **Step 4: Verify focused tests and unit suite**

Run: `gradle :app:testDebugUnitTest`
Expected: the new content-width contract and existing unit tests pass; instrumentation remains pending until the user resolves the system prompt.

### Task 4: Adapt settings and statistics for wide/short windows

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/ui/screens/ServerSettingsScreen.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/screens/ListeningStatsScreen.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt` only to pass constraints/width class to the statistics surface if required.

**Interfaces:**
- Consumes: `WindowWidthClass`; existing settings callbacks and stat state remain authoritative.
- Produces: an expanded-width settings layout with bounded columns/sections rather than a full-width phone form, and statistics content paced for landscape. The single-column settings form remains on compact width.

- [x] **Step 1: Add width-contract unit assertions for settings and statistics**

Verify compact/medium/expanded width bounds for settings and statistics. Defer live control interaction and selected-value retention assertions until the pending system permission prompt no longer blocks Compose instrumentation.

- [x] **Step 2: Run the focused test to observe the missing width contracts**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.ResponsiveContentFrameTest"`
Expected: compilation fails while settings/statistics width contract functions are undefined.

- [x] **Step 3: Implement adaptive settings/statistics composition**

Bound the settings form to 960dp at medium/expanded widths and statistics to 960dp medium / 1120dp expanded, centered in available width. Keep the existing one-column section order, state, keyboard insets, validation, and scrolling to avoid a broad form rewrite.

- [x] **Step 4: Verify instrumentation and unit tests**

Run: `gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`
Expected: width-contract tests and existing JVM tests pass and instrumentation sources compile; connected interaction tests remain pending the system prompt.

### Task 5: Restructure the full player for landscape and expanded width

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/components/LyricsView.kt` only if a content-width constraint is required.
- Test: Compose instrumentation for player controls/lyrics visibility and state preservation across width classes.

**Interfaces:**
- Consumes: available width and height constraints; preserve all existing player callbacks and `PlayerViewMode` behavior.
- Produces: compact phone player unchanged; medium/expanded landscape composition places artwork/metadata and playback/lyrics in balanced panes, with controls and Sleep Timer accessible at short heights.

- [x] **Step 1: Add player width/height layout-decision unit assertions**

Verify the pure wide-layout decision at compact, expanded, and short medium-landscape dimensions. Defer live control/state assertions until the pending system prompt no longer blocks Compose instrumentation.

- [x] **Step 2: Run the focused test before layout changes**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.ResponsiveContentFrameTest.playerUsesWideLayoutForExpandedWindowsOrShortMediumLandscape"`
Expected: compilation fails because the layout decision is not yet defined.

- [ ] **Step 3: Implement a constraint-driven player layout**

Use available height as well as width; do not use a landscape orientation lock. Preserve existing vertical control order, expand the player content bound, scale artwork according to height, and enable scrolling only in short wide windows so controls remain reachable.

- [ ] **Step 4: Verify focused test, unit suite, and debug build**

Run: `gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug`
Expected: JVM tests pass, instrumentation sources compile, and debug APK assembles. Connected UI tests remain pending the system prompt.

### Task 6: Full responsive visual verification and cleanup

**Files:**
- Verify all changed app shell, screen, and player files.
- Update this plan's checkboxes and record unavailable hardware/configurations.

- [x] **Step 1: Inspect diff and run full checks**

Run: `git diff --check` and `gradle :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug`.
Expected: no whitespace errors; all tests/build succeed.

- [ ] **Step 2: Capture a verification matrix**

Capture compact phone portrait, tablet portrait, tablet landscape, dark theme, and increased font scale. Verify the active device/window size in each capture; test split-screen/resizing if supported.

- [ ] **Step 3: Fix all observed clipping, awkward whitespace, reachability, and inset defects**

Make one bounded correction pass, then recapture the affected configurations once.

- [x] **Step 4: Report verification coverage and remaining device limitations**

Explicitly list which hardware/emulator configurations were exercised; do not claim tablet hardware verification if only a phone emulator/device was available.

Visual matrix limitation: The connected Galaxy S21 ran instrumentation successfully but the app activity could not be launched for a manual screenshot (`am start` reported Activity class does not exist). No tablet emulator executable/SDK or tablet device is available. Tablet portrait/landscape, dark-theme/font-scale screenshots and split-screen verification remain unperformed.
