# Astrolabe Radial Wheel System & Celestial Player

A comprehensive 4-phase transformation introducing a classical astronomical astrolabe and concentric radial dial experience to the Navirom/Zana Subsonic audio client, inspired by celestial planispheres and precision mechanical instruments.

## User Review & Critical Decisions

> [!IMPORTANT]
> The Astrolabe Wheel System introduces a tactile, rotational paradigm alongside the existing modern linear UI. All core music browsing, playback, and server capabilities are preserved, with zero loss of functionality.

- **Confirmed Mode Architecture**: Both the Classic Modern View and the Astrolabe Wheel View will coexist seamlessly. A persistent toggle allows the user to switch back and forth at any time.
- **Reference Aesthetic**: Celestial midnight core with pulsating constellation star map, concentric brass/monochrome engraved rings, Roman numerals, fine azimuth tick marks, and serif typography.
- **Physics & Detents**: Natural rotational drag gestures with physical angular inertia, friction decay, and tactile tick/click haptic detents powered by `NaviromHaptics`.

---

## 1. Overview & Core Concept

- **What It Does**: Reimagines the traditional list-and-grid music client into an interactive, multi-ring celestial astrolabe. Users navigate albums, scrub audio, tune equalizers, and filter artists by rotating concentric dials around a reactive starfield core.
- **Target Audience / Persona**: Music enthusiasts and audiophiles seeking an immersive, aesthetically refined playback experience that feels like operating a high-end vintage astronomical or mechanical chronometer.
- **Key Value**: Provides unique one-handed rotary ergonomics, fluid scrub gestures, rapid A–Z scrubbing across large Subsonic libraries, and mesmerizing audio-reactive visual feedback.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Orbital Playback**:
   - The user opens the Full Player in Astrolabe Mode.
   - At the center, a midnight starfield pulses gently with constellation lines and audio energy.
   - Surrounding the core, a radial vinyl-groove lathe displays the album's tracks with Roman numerals and track titles.
   - Spinning the lathe moves tracks under an illuminated needle indicator; tapping or releasing locks in the track with a solid haptic click.
   - The user scrubs track progress by dragging the outer celestial bead along an engraved minute/second arc.

2. **Radial Library Navigation**:
   - The user browses artists on the outermost planetary ring.
   - As an artist rotates into the focus aperture, the middle dial instantly shifts to display that artist's discography.
   - The inner ring displays the focused album's tracklist. Tapping an album spins it into focus.

3. **Circular Equalizer & Settings**:
   - The user enters settings to reveal a 5/10-band radial frequency rose.
   - Dragging frequency nodes along radial axes adjusts dB gain with real-time audio curve updates.
   - Transcoding bitrates (128k, 192k, 256k, 320k, FLAC) and cache gauges are adjusted via a rotary dial.

4. **A–Z Radial Scrubber**:
   - In Search, a 360-degree alphabet ring enables rapid flick-scrolling with fine tick haptics to jump straight to any letter.

### Visual Identity & Theme

- **Aesthetic Direction**: Celestial Astrolabe / Astronomical Chronometer. Elegant, precise, tactile, and restrained.
- **Color Palette & Atmospheric Accents**:
  - Deep Space Midnight (`#0A0D14`) for the central star disc.
  - Constellation cyan/gold starlight (`#7FE5D9` / `#FFD166`) for stars, indicators, and illuminated pointers.
  - Engraved metal/parchment outlines (`#2A324B` / `#D8C8B8` tones) for concentric dial rims, azimuth tick marks, and radial dividers.
- **Typography**:
  - High-contrast serif numbers and Roman numerals for dial degrees, dates, and track indices.
  - Clean monospace/sans typography for live track elapsed counters and technical bitrate readouts.
- **Motion & Feedback**:
  - Rotational drag with angular momentum decay.
  - Magnetic spring snapping (`dampingRatio = 0.75f`).
  - Haptic ticks on every detent and a firm click on selection lock.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom Polar Math vs. Standard Compose Gestures**
  - *Chosen Approach*: Implement a dedicated `RadialDialEngine` utilizing `atan2` coordinate conversions and polar-to-cartesian projection.
  - *Why*: Standard horizontal/vertical scroll gestures cannot deliver true multi-ring concentric rotation or continuous circular drag.
  - *Alternatives Considered*: Standard Compose `CircularSlider` was rejected because it only supports a single arc and lacks multi-ring hit-testing, inertia decay, and magnetic snapping.

- **Decision 2: Dual Mode (Classic vs. Astrolabe) via Persistent Switcher**
  - *Chosen Approach*: Maintain full state parity and allow instantaneous switching between Classic Modern and Astrolabe modes with an animated header toggle.
  - *Why*: Preserves familiar linear navigation for quick utility while offering the rich orbital experience whenever desired.
  - *Alternatives Considered*: Completely replacing the existing UI was rejected to prevent disorienting users who prefer classic list views.

- **Decision 3: Canvas-Based Celestial Visualizer vs. Video/GIF Assets**
  - *Chosen Approach*: Procedural hardware-accelerated `Canvas` rendering of stars, constellation vectors, and pulsating rings.
  - *Why*: Guarantees crisp rendering at any screen resolution, zero asset bloat, and real-time synchronization with playback state.

---

## 4. Technical Architecture & Data Strategy

### System & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             NaviromViewModel                                │
│   - astrolabeModeEnabled: StateFlow<Boolean>                                │
│   - currentTrack, playbackState, queue, library, searchState                │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             RadialDialEngine                                │
│   - toPolar(center, point) / toCartesian(center, r, theta)                  │
│   - ringIndexAt(distance, ringSpecs) [Multi-ring hit-testing]               │
│   - Modifier.radialRotatable(state) [Angular velocity tracking]             │
│   - Momentum decay physics & magnetic detent snapping                       │
│   - NaviromHaptics integration (tick & click)                               │
└──────────────┬───────────────────────┬───────────────────────┬──────────────┘
               │                       │                       │
               ▼                       ▼                       ▼
┌───────────────────────────┐ ┌─────────────────────┐ ┌───────────────────────┐
│ Phase 2: Orbital Views    │ │ Phase 3: Radial     │ │ Phase 4: Switcher     │
│ - AstrolabeFullPlayer     │ │   Settings & Search │ │ - Top Bar Orbit Icon  │
│   - Star Visualizer Core  │ │ - RadialSettingsDial│ │ - Full Player Switcher│
│   - Track Lathe Ring      │ │ - RadialEqualizer   │ │ - Sidebar Mode Toggle │
│   - Progress Scrub Arc    │ │ - RadialSearchRing  │ │ - Adaptive Tablet     │
│ - AstrolabeLibraryView    │ │   (A-Z Scrubber)    │ │   Dual-Pane Layout    │
│   - 3-Ring Planetary Dial │ └─────────────────────┘ └───────────────────────┘
└───────────────────────────┘
```

### Component & State Mapping

1. **`RadialDialEngine.kt`**:
   - `PolarPoint(radius: Float, angleDegrees: Float)`: Holds calculated coordinates.
   - `RadialDialState`: Holds current rotation angle, target snap angle, angular velocity, and dragging state.
   - `Modifier.radialRotatable()`: Attaches to circular composables; computes delta angle relative to canvas center.
   - `NaviromHaptics`: Triggers sensory feedback on traversing detent thresholds.

2. **`AstrolabeFullPlayer.kt`**:
   - Star visualizer drawn on `Canvas` using `DrawScope`.
   - Track lathe draws items positioned along circular path $r \cos(\theta), r \sin(\theta)$ with radial text rotation.
   - Binds directly to `NaviromViewModel.currentTrack` and `playbackState`.

3. **`AstrolabeLibraryView.kt`**:
   - Three concentric rings for Artists, Albums, and Tracks.
   - Touch routing dispatches rotation events to the ring under the touch point.

4. **`AstrolabeSettingsScreen.kt` & `AstrolabeSearchScreen.kt`**:
   - Radial EQ: 5 frequency spokes radiating from center; node dragging updates audio balance.
   - A–Z Scrubber: 26 discrete letter detents; snapping jumps to the corresponding index in the library.

5. **`AstrolabeModeSwitcher`**:
   - Managed via `NaviromViewModel.astrolabeModeEnabled`.
   - Top app bar icon button with rotational animation to toggle view modes seamlessly.
