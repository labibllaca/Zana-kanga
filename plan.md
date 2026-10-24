# Astrolabe Radial Wheel System & Celestial Player Architecture

## 1. Overview & Vision
Transform the Navirom/Zana audio experience with an **Astrolabe & Celestial Radial System** inspired by classical astronomical planispheres, astrolabes, and mechanical precision dials (as referenced in the astronomical calendar design). 

The Astrolabe mode introduces:
- **Concentric Rotary Dials**: Smooth touch-driven rotation for track scrubbing, album queue lathes, artist discovery, and settings.
- **Celestial Core**: A dark starfield center hub with real-time constellation visualizers, audio pulsation, and orbiting control satellites.
- **Physical Dial Mechanics**: Realistic angular momentum, exponential friction decay, and crisp haptic detents (`NaviromHaptics`).
- **Seamless Dual-Mode Operation**: A persistent switcher allowing instant toggling between the **Classic Modern View** and the **Astrolabe Wheel System**.

---

## 2. Architectural Blueprint

```
+-----------------------------------------------------------------------------------+
|                                Navirom / Zana App                                 |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                   Astrolabe Mode Controller (NaviromViewModel)              |  |
|  |   - astrolabeModeEnabled: StateFlow<Boolean> (Persistent)                   |  |
|  |   - activeDialFocus: OrbitalDialTarget                                      |  |
|  +-----------------------------------------------------------------------------+  |
|                                        |                                          |
|                                        v                                          |
|  +-----------------------------------------------------------------------------+  |
|  |                     RadialDialEngine (Core Math & Physics)                  |  |
|  |   - Polar/Cartesian conversions (toPolar, toCartesian, normalizeAngle)       |  |
|  |   - Multi-ring hit-testing (ringIndexAt radius bounds)                      |  |
|  |   - Rotational drag gestures & angular velocity tracker                     |  |
|  |   - Momentum decay animation & magnetic detent snapping                     |  |
|  |   - Haptic feedback coordinator (tick on detent, click on snap)             |  |
|  +-----------------------------------------------------------------------------+  |
|           |                                    |                      |           |
|           v                                    v                      v           |
|  +-------------------+              +--------------------+  +------------------+  |
|  | Phase 2: Orbital  |              | Phase 2: Radial    |  | Phase 3: Radial  |  |
|  | Full Player       |              | Library Mode       |  | Settings & Search|  |
|  | - Celestial Core  |              | - Artist Outer Ring|  | - Concentric Srv |  |
|  | - Star Visualizer |              | - Album Middle Ring|  | - Radial 5-band  |  |
|  | - Track Lathe Ring|              | - Track Inner Lathe|  |   Equalizer Rose |  |
|  | - Progress Scrub  |              | - Astrolabe Grid   |  | - A-Z Alphabet   |  |
|  | - Satellite Nodes |              |   Projection       |  |   Scrubber Ring  |  |
|  +-------------------+              +--------------------+  +------------------+  |
|                                        |                                          |
|                                        v                                          |
|  +-----------------------------------------------------------------------------+  |
|  | Phase 4: Astrolabe Mode Switcher                                            |  |
|  | - Persistent Top Header Icon with rotational morph                          |  |
|  | - Full Player & Sidebar Drawer toggles                                      |  |
|  | - Smooth Crossfade & Adaptive Tablet Dual-Pane Layout                       |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 3. Detailed Phase Breakdown

### Phase 1: The Core Radial Engine (`RadialDialEngine.kt`)
The mathematical and physical foundation for all circular and concentric interactions in the application.

1. **Polar & Cartesian Math Utilities**:
   - `toPolar(center: Offset, point: Offset): PolarPoint`
     - Computes radius $r = \sqrt{\Delta x^2 + \Delta y^2}$ and angle $\theta = \text{atan2}(\Delta y, \Delta x)$ in degrees $[0^\circ, 360^\circ)$.
   - `toCartesian(center: Offset, radius: Float, angleDegrees: Float): Offset`
     - Computes $x = x_0 + r \cos(\theta)$, $y = y_0 + r \sin(\theta)$ with support for radial text rotation and glyph alignment.
   - `normalizeDegrees(angle: Float): Float`: Wraps angles into the range $[0, 360)$.
   - `angularDistance(fromAngle: Float, toAngle: Float): Float`: Computes shortest delta $(-180^\circ \text{ to } +180^\circ)$ to avoid spinning the wrong way across the $0^\circ/360^\circ$ seam.
   - `ringIndexAt(distanceFromCenter: Float, rings: List<RadialRingSpec>): Int?`: Concentric zone hit-testing to isolate touches to the specific active dial.

2. **Rotational Drag Gesture Handling**:
   - Modifier `Modifier.radialRotatable(engineState: RadialDialState)`:
     - Tracks continuous pointer drag around a center pivot.
     - Calculates angular displacement delta $d\theta$ per frame.
     - Accommodates single-finger drag anywhere along the dial circumference.
     - Computes rotational velocity $\omega$ (degrees/sec) upon gesture release.

3. **Momentum Decay & Inertial Physics**:
   - Decelerates spinning dials using exponential friction decay:
     $$\omega(t) = \omega_0 \cdot e^{-\gamma t}$$
   - Simulates physical mass and mechanical inertia of vintage brass/iron astrolabe plates.
   - Smooth clamp and velocity capping for high-refresh-rate displays (90Hz/120Hz).

4. **Magnetic Detents & Haptic Feedback**:
   - Calculates nearest discrete snap points (e.g., track indices, letters A–Z, or 5-degree increments).
   - Magnetic snap spring animation (`Spring.StiffnessMedium`, `dampingRatio = 0.75f`) when velocity falls below snap threshold.
   - Sensory feedback integration with `NaviromHaptics`:
     - `performTick()`: Light click triggered on passing each tick mark / detent boundary.
     - `performHeavyClick()`: Satisfying tactile feedback when the dial firmly settles into a selected item slot.

---

### Phase 2: Orbital Full Player & Library Radial Mode

1. **Orbital Full Player (`AstrolabeFullPlayer.kt`)**:
   - **Celestial Star Visualizer (Center Core)**:
     - Deep space midnight disc ($r \approx 70\text{–}90\text{dp}$) framed by an engraved astrolabe bezel.
     - Procedural starfield with constellation lines, celestial coordinates, and compass axes.
     - Audio reactivity: Stars subtly pulse in size and luminosity according to playback tempo and active playback state.
     - Center album art circular medallion with integrated Play/Pause glyph.
   - **Radial Album Track Lathe (Middle Ring)**:
     - Concentric circular vinyl groove ring displaying the current album's tracks or playback queue.
     - Rotating the lathe spins the track catalogue; the track passing under the 12 o'clock indicator needle highlights and displays its track title and artist.
     - Quick tap on any visible track segment rotates it directly to the needle and starts playback.
   - **Progress Arc Dial (Inner Ring)**:
     - Circular scrubber ring with engraved minute/second ticks.
     - Sweeping planetary indicator showing current elapsed time vs. track duration.
     - Interactive rotational scrub: dragging the celestial bead jumps through the song with position readouts.
   - **Orbital Satellite Controls (Outer Orbit)**:
     - Satellite nodes arranged along the outer astrolabe rim for Shuffle, Repeat, Favorite/Heart, Lyrics, and WLAN Cast.

2. **Radial Library Mode (`AstrolabeLibraryView.kt`)**:
   - **Three Concentric Planetary Rings**:
     - *Outer Ring (Artists)*: Revolving dial of all music artists.
     - *Middle Ring (Albums)*: Discography dial dynamically populated with the selected artist's albums.
     - *Inner Ring (Tracks)*: Radiating track tracks/grooves for instant queueing.
   - **Astronomical Callouts & Typography**:
     - Curved serif text along circular arcs (inspired by the reference image's Didot/Bodoni "SUMMER", "JUNE", "JULY" styling).
     - Radial tick marks and Roman numerals corresponding to track numbers (I, II, III, IV...).

---

### Phase 3: Radial Settings & Radial Search Browser

1. **Radial Settings & Circular Equalizer (`AstrolabeSettingsScreen.kt`)**:
   - **Concentric Server Dial**:
     - Visual gauge displaying Subsonic/Navidrome connection status, ping/latency in milliseconds, and cache usage.
     - Rotary bitrate dial for selecting audio transcoding quality (128k, 192k, 256k, 320k, FLAC/Lossless) styled like a vintage radio tuner dial.
     - Cache storage fill arc with an interactive flush/purge detent.
   - **Circular Astrolabe Equalizer (EQ)**:
     - Radial multi-band frequency rose: 5 to 10 frequency spokes (60Hz, 150Hz, 400Hz, 1kHz, 2.5kHz, 6kHz, 15kHz) projecting outward from center.
     - Interactive draggable control nodes along each spoke (+12dB boost outward, -12dB cut inward).
     - Connected celestial polygon mesh dynamically rendering the equalization curve.

2. **Radial Search Browser (`AstrolabeSearchScreen.kt`)**:
   - **A–Z Astrolabe Scrubber Ring**:
     - 360-degree perimeter ring containing letters A through Z and astronomical numeral markers.
     - Fast scrubbing with instantaneous haptic detents allows rapid jumping across large music collections.
     - Active letter illuminates with an astrolabe pointer needle.
   - **Orbital Results Projection**:
     - Matching search results project into the central orbital disc with radial category filters (Tracks, Albums, Artists, Playlists).

---

### Phase 4: Astrolabe Mode Switcher

1. **Persistent State Management**:
   - `astrolabeModeEnabled: StateFlow<Boolean>` in `NaviromViewModel` saved to SharedPreferences / DataStore.
2. **Access Points & Switching Affordances**:
   - **Top App Header Bar**: Prominent Astrolabe icon button with rotational icon animation on click.
   - **Full Player Modal**: Seamless toggle in the top bar to switch between Classic Album Cover and the Orbital Lathe.
   - **Sidebar Drawer**: "Astrolabe Wheel System" toggle switch with descriptive badge.
3. **Motion & Transitions**:
   - Smooth animated transitions (crossfade + gentle scale) between Classic linear views and the Astrolabe radial views.
   - Edge-to-edge support with safe padding for status and navigation bars.
   - Responsive multi-pane adaptation for tablets and foldables (Astrolabe Wheel on left pane, queue/details on right pane).

---

## 4. Implementation Steps & Milestones

1. **Phase 1: Radial Engine**:
   - Create `com.labix.navirom.ui.radial.RadialDialEngine.kt` (math, polar transforms, drag gestures, inertia, and haptic detents).
   - Unit tests in `RadialDialEngineTest.kt` verifying polar coordinate conversions, angle normalization, and detent calculations.
2. **Phase 2: Orbital Full Player & Library**:
   - Create `com.labix.navirom.ui.radial.AstrolabeFullPlayer.kt` (celestial star visualizer, radial track lathe, progress arc).
   - Create `com.labix.navirom.ui.radial.AstrolabeLibraryView.kt` (concentric artist/album/track rings).
3. **Phase 3: Radial Settings & Search**:
   - Create `com.labix.navirom.ui.radial.AstrolabeSettingsScreen.kt` (rotary server dial and circular equalizer).
   - Create `com.labix.navirom.ui.radial.AstrolabeSearchScreen.kt` (A–Z circular scrubber and orbital results).
4. **Phase 4: Switcher & App Integration**:
   - Add Astrolabe state and preferences in `NaviromViewModel.kt`.
   - Update `NaviromApp.kt`, `FullPlayerModal.kt`, and `SidebarDrawer.kt` with persistent switcher buttons and mode routing.
5. **Compilation & Version Increment**:
   - Increment version in `gradle/libs.versions.toml` (`appVersionCode = "28"`, `appVersionName = "1.28.0"`).
   - Verify complete build with `compile_applet`.
