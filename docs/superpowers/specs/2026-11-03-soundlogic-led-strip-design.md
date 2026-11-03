# Soundlogic LED Strip — Design

Date: 2026-11-03
Status: Draft for approval

## Intent

Add a Bluetooth LED-strip connection to Zana’s Settings so the 5 m Soundlogic
strip can react to the music Zana is playing. The strip is a Tuya BLE device.
The app should pair and control it locally, without the Tuya/Smart Life app, a
Tuya account, or the Tuya cloud.

The feature has two halves:

1. **Connection and manual control** in Settings: scan, pair, connect, test
   colours, disconnect, forget.
2. **Music reaction**: derive low and high frequency energy from Zana’s own
   playback and translate it into whole-strip colour and brightness updates.

## What we verified on the real strip

The strip advertises as `TY` with:

- Advertising service: `A201`
- Data service: `1910`
- Notify characteristic: `2B10`
- Write characteristic: `2B11`
- Tuya BLE protocol: `3.3`
- Product ID: `r8cogx6r`
- Initially unbound, locally pairable

Advertisement identity:

- Service data `A201`: `0x00` followed by the 8-character product ID.
- Manufacturer data `0xD007` (`0x07D0`): 22 bytes. The first byte's high bit is
  the bound flag, the second byte is the protocol major. Bytes 6..22 decrypt
  with `AES-CBC(key = MD5(productId), iv = MD5(productId))` to a 16-character
  device UUID used in the pair request.

Local pairing works with a generated six-byte login key and a generated
22-character virtual device ID. The pair request payload is:

```
device UUID (16 ASCII) || login key (6 raw bytes) || virtual ID (22 ASCII)
```

Pairing is idempotent but **must be sent on every new connection** after the
device-info exchange. Without it the device answers DP writes but does not
apply them.

Working datapoints:

| DP | Purpose | Type | Value |
|----|---------|------|-------|
| 1 | Power | bool | true/false |
| 2 | Work mode | enum | 0 white, 1 colour, 2 scene, 3 music. **Must be encoded as one byte.** |
| 3 | Brightness | value | 10..1000 |
| 5 | Colour | string | JSON `{"h":0-360,"s":0-1000,"v":0-1000}` |

The strip also echoes datapoints 20/21/22/24, but those are not needed and are
not the real controls. Reading state requires a status query (`0x0003`); the
strip reports its active datapoints as `0x8001` messages.

**Hardware limitation:** the 12472/12473 is a normal RGB strip with one colour
for the whole strip. It cannot address individual LEDs, so a one-LED-at-a-time
chase is impossible. Whole-strip effects are possible: fades, breathing,
colour sweeps, and band-driven colour/brightness changes.

**Colour calibration:** this unit renders hue `0` as blue. The app must not
assume standard HSV. Warm and cool hues will be configurable; the settings
sheet will include a short calibration/test control.

## Goals

- Find nearby Tuya BLE strips from Zana’s Settings.
- Pair locally and store the generated credentials on the device.
- Connect, disconnect, forget, and recover from connection loss.
- Manual test controls: power, brightness, and a few colours.
- A “React to music” mode that maps low and high bands to the strip.
- A sensitivity control and a visible low/high level meter.
- Keep working while Zana is playing in the background.
- No Tuya app, account, cloud, or network dependency.

## Non-goals

- Per-LED or addressable effects.
- Multiple strips at once. One active strip.
- Tuya Wi-Fi devices, Tuya mesh devices, or non-Tuya LED controllers.
- Firmware updates or OTA.
- Controlling the strip from outside Zana.
- Storing or syncing strip credentials to a server.
- Audio visualisation of other apps’ audio.

## Anti-goals

- A feature that needs the official Tuya app installed.
- A feature that requires a Tuya developer account.
- Streaming audio, microphone recordings, or any audio data leaving the phone.
- Making playback depend on the LED strip being connected.
- Blocking playback or the UI while BLE is connecting.

## Constraints

- Android `minSdk 24`, `targetSdk 36`.
- The app currently uses `MediaPlayer` (two instances for dual audio and
  crossfade), not ExoPlayer.
- BLE GATT writes are 20 bytes per packet unless MTU is negotiated. The Tuya
  framing already works at 20 bytes; do not rely on a larger MTU.
- Visualizer requires `RECORD_AUDIO`.
- BLE scanning on Android 12+ requires `BLUETOOTH_SCAN`; older versions use
  location permission (already present).
- BLE updates are coalesced to about 8–10 per second. Higher rates are wasted
  and can overflow the GATT queue.
- The strip is a shared household device. Pairing changes its bound state.

## Approaches considered

### Connection

1. **Native Tuya BLE implementation (chosen).** Port the small protocol into
   Kotlin: advertisement identity, local pairing, AES/MD5 framing, DP writes,
   status reads. No cloud or external app. More code, but self-contained and
   matches what was verified on the hardware.
2. **Official Tuya SDK.** Faster to integrate, but requires a Tuya developer
   account, an app key/secret, cloud services, and a larger dependency. It is
   not a local-only solution and adds vendor lock-in.
3. **Delegate to the official Tuya app.** Not viable for real-time reaction and
   defeats the purpose.

### Audio analysis

1. **Android `Visualizer` attached to Zana’s audio session (chosen).** Real-time
   waveform/FFT, one runtime permission, no capture consent dialog, no audio
   recording. Works with `MediaPlayer`.
2. **AudioPlaybackCapture + MediaProjection.** Captures other apps’ audio and
   needs a consent flow. More complex, more permissions, unnecessary for our own
   playback.
3. **Use the strip’s built-in music mode.** No audio analysis and no control
   over low/high mapping.

## Architecture

New package: `com.labix.navirom.led`

```
LedStripManager          app-scoped coordinator and public StateFlow
├── TuyaBleScanner       BLE scan + advertisement identity parsing
├── TuyaLedController    connect/pair/session/command queue/reconnect
│   ├── TuyaBleProtocol  framing, CRC, AES, MD5, varints, DP codec
│   └── LedCredentialStore
└── LedAudioReactor      Visualizer + FFT + band mapping + throttling
```

`LedStripManager` is owned by `AudioPlayerController` (the existing
application-scoped singleton), alongside `WlanSpeakerManager`. This keeps the
reaction alive while playback continues in the background. The ViewModel
exposes its state and actions to Compose, the same way it exposes the WLAN
speaker manager.

`AudioPlayerController` gains:

- `val activeAudioSessionIds: StateFlow<Set<Int>>` updated whenever a primary
  or secondary `MediaPlayer` is created, replaced, paused, or released. It
  contains every session that is currently audible.
- `val ledStripManager: LedStripManager by lazy { ... }`.

Dual-deck behaviour:

- The reactor attaches one Visualizer per audible session id.
- Per band, it takes the **maximum** level across sessions, not the sum, so two
  decks do not double the reported energy.
- Reaction runs while **either** `playbackState.isPlaying` or
  `secondaryPlaybackState.isPlaying` is true.
- A crossfade briefly has two primary sessions; the set handles that naturally.
- If Visualizer cannot attach to one session, the others still work.

## Protocol

Reference implementations used during discovery (read-only references, not
dependencies to add to the APK):

- `Wheemer/local-tuya-ble` — local protocol-3 pairing and the Tuya BLE client.
- `PlusPlus-ua/ha_tuya_ble` — session handling, datapoint parsing, and device
  mappings.
- `cuda-convnet/tuya_ble_sdk_sync` — the device-side Tuya BLE SDK framing.

The implementation should be a Kotlin port with JVM fixtures, not a wrapper
around those projects.

### Endianness and primitives

- All multi-byte integers in the packet header are **big-endian**.
- CRC16 is Modbus: init `0xFFFF`, polynomial `0xA001`, reflected, appended
  big-endian.
- Varints are unsigned LEB128 with a maximum of 4 bytes.
- The first GATT packet carries `(protocolVersion << 4)` as its final header
  byte. For protocol `3.3`, send `0x30`.
- Every encrypted frame uses a fresh random 16-byte IV.
- AES is `AES/CBC/NoPadding`; the plaintext is zero-padded to a multiple of 16
  before encryption.
- Passwords/keys are raw bytes, not hex strings, when used in MD5/AES.

### Pairing bootstrap

The key exchange is not circular. The first pairing uses unencrypted frames to
deliver the new key, then all later connections use that key:

1. **First connection (unbound device)**
   - Send an unencrypted `0x0000` device-info request.
   - Wait for the unencrypted device-info response.
   - Generate a 6-byte login key and a 22-character virtual device ID
     (11 random bytes, hex-encoded).
   - Send an unencrypted `0x0001` pair request with
     `deviceUuid(16 ASCII) || loginKey(6 raw) || virtualId(22 ASCII)`.
   - The device stores the key and binds.
2. **Verification connection**
   - Send `0x0000` encrypted with `securityFlag = 4` and
     `key = MD5(loginKey)`.
   - The response contains `srand` at bytes 6..11.
   - `sessionKey = MD5(loginKey || srand)`.
   - Send `0x0001` encrypted with `securityFlag = 5` and `sessionKey`.
     Result `0` means success; `2` means already paired and is also success.
   - The device may then send `0x8011` (time request); answer it with the current
     Unix time in milliseconds as an ASCII string followed by the timezone
     offset as a signed 16-bit value, encrypted with the session key.
3. **Every later connection**
   - Device info (login key) → parse `srand` → derive session key → pair
     (session key) → status query → commands.
   - Skipping the pair step makes the device acknowledge DP writes without
     applying them. This was observed on the real strip.

The virtual device ID only needs to be stable for this app/device pair; it is
generated once and stored with the credentials.

### Packet framing

Plaintext frame:

```
seq:u32 | reply:u32 | command:u16 | length:u16 | data | crc16:u16
```

- CRC is Modbus CRC16 (poly `0xA001`, init `0xFFFF`).
- Zero-pad the plaintext to a multiple of 16 bytes.
- Unencrypted envelope: `0x00 || plaintext`.
- Encrypted envelope: `securityFlag || iv[16] || AES-128-CBC(key, iv, plaintext)`.
  - Device info uses `securityFlag = 4` and `key = MD5(loginKey[6])`.
  - Everything after the device-info exchange uses `securityFlag = 5` and
    `key = MD5(loginKey[6] || srand[6])`.
- Split the envelope into 20-byte GATT packets:
  - packet 0: `varint(0) || varint(envelopeLength) || (protocolVersion << 4) || bytes`
  - packet n: `varint(n) || bytes`
- Write to `2B11` without response, with a small delay between packets.
- Reassemble notifications from `2B10` by packet index and total length.

### Commands

| Command | Meaning |
|---------|---------|
| `0x0000` | Device info |
| `0x0001` | Pair |
| `0x0002` | Write datapoints |
| `0x0003` | Query status |
| `0x0005` | Unbind |
| `0x8011` | Device requests time sync |
| `0x8001` | Datapoint report |

Device info response data:

- `[0..1]` device version
- `[2..3]` protocol version (`3.3` on this strip)
- `[4]` flags
- `[5]` bound flag
- `[6..11]` `srand`
- `[12..13]` hardware version
- `[14..45]` auth key (not used by this path)

### DP encoding

```
id:u8 | type:u8 | length:u8 | value
```

- bool: one byte.
- value: four bytes, big-endian signed.
- enum: **one byte**.
- string: UTF-8 bytes.

The device-info request needs the device to be primed. Verified sequence:

1. Send an unencrypted device-info request.
2. Wait briefly.
3. Send a device-info request encrypted with the login key.
4. Parse `srand`, derive the session key.
5. Send the pair request encrypted with the session key.
6. Wait for pair result `0` or `2`.
7. Send the status query, then DP writes.

If the pair step is skipped, writes are acknowledged but ignored.

### Status queries, reports, and unbind

- Status query: command `0x0003`, empty data. The device answers with a
  `0x0003` response containing one status byte, followed by one or more
  `0x8001` reports.
- A `0x8001` report payload is a sequence of DP entries in the same KLV format
  as writes. Parse every complete entry and ignore trailing bytes.
- Some firmware sends `0x8003`, `0x8004`, or `0x8005` instead:
  - `0x8003` has a timestamp prefix before the KLV entries.
  - `0x8004` starts with `dpSequence:u16 || flags:u8` before the KLV entries.
  - `0x8005` combines timestamp, sequence, and flags.
  - Acknowledge `0x8004`/`0x8005` with
    `dpSequence:u16 || flags:u8 || status:0x00`, sent as the same command with
    the request sequence in `reply`.
- Acknowledge `0x8001` with an empty payload.
- Unbind: command `0x0005`, empty data, encrypted with the session key. Treat
  any response as best-effort and then delete local credentials.
- The device may send `0x8011` or `0x8012` (time requests) at any time after
  pairing. Answer them; otherwise some firmware retries and delays commands.

### Connection lifecycle

- Scan only while the settings sheet is open or the reaction needs a connection.
- On connect: GATT connect, discover `1910`, subscribe `2B10`.
- After pairing: query status and cache the current state.
- Reconnect with exponential backoff (1 s, 2 s, 5 s, 10 s, 30 s).
- Idle disconnect after 30 s when not playing and not reacting.
- Send pair on every reconnect.
- Serialize all GATT operations in a single command queue.

## Credentials and persistence

`LedCredentialStore` uses a dedicated private preferences file
(`navirom_led_prefs`). It stores:

- device address and advertised name
- product ID and device UUID
- login key (6 bytes, hex)
- virtual device ID (22 chars)
- protocol version
- last connected time

The key controls only this strip. No audio or account data is stored. A future
task can move these into the Keystore-backed store if the threat model changes.

Other settings:

- `react_to_music` (bool)
- `sensitivity` (float, 0.5–2.0)
- `warm_hue` and `cool_hue` (int, 0–360, expressed in **wanted** colours)
- `hue_offset` (int, 0–359) — device output = `(wantedHue + hue_offset) % 360`
- `idle_color` (hue/saturation/value, expressed in wanted colours)
- `brightness_min` / `brightness_max`
- `auto_connect` (bool)

This strip renders hue `0` as blue, so the default `hue_offset` is `240`.
Every hue sent to the strip — including the named test swatches — is passed
through this offset. The calibration control adjusts `hue_offset` live until
red/green/blue match the labels.

Brightness is sent in **both** places on every colour update:

- DP3 `bright_value` and DP5's JSON `v` are set to the same value.
- This makes the write robust to firmware that treats either field as
  authoritative, and avoids a colour write resetting brightness.

`auto_connect` means: reconnect to the last strip on app launch and when
playback starts. Reaction still connects on demand when the user enables it,
even if `auto_connect` is off.

## UI

### Settings row

Place it in the Playback Settings card, directly after the WLAN speaker row.
It follows the same visual pattern:

- Circular icon (lightbulb / Bluetooth)
- Title: “Soundlogic LED Strip (Bluetooth)”
- Subtitle: “Not connected”, “Connected: TY”, or “Reacting to music”
- Button: “Connect” / “Manage”
- Tapping the row opens the sheet.

### Sheet: `LedStripSheet`

States:

1. **Bluetooth off / permission missing** — explanation and a button to fix.
2. **Scanning** — progress indicator and a “Scan again” action.
3. **Device list** — name, RSSI, product ID, bound/unbound badge. One tap to
   connect or pair.
4. **Pairing** — progress, then success or a clear error.
5. **Connected** — test controls and music reaction settings.
6. **Unavailable** — reconnect / forget / reset guidance.

Connected controls, while **not** reacting:

- Power switch.
- Brightness slider (10–1000).
- Colour swatches for quick tests: red, green, blue, warm white. All swatches
  go through `hue_offset`.
- Hue calibration row: rotate `hue_offset` until red/green/blue match.
- “React to music” switch.
- “Disconnect”.
- “Unbind & forget” behind a confirmation.

Connected controls, while **reacting**:

- Reaction brightness slider (sets `brightness_max`; minimum is fixed at 200).
- Sensitivity slider.
- Low/high level meter (two thin bars) for feedback.
- Warm/cool hue preview.
- Manual power/colour swatches are disabled, with the note “Colour is driven by
  the music”. Turning the power switch off stops reaction and powers the strip
  off.
- Toggling reaction off returns manual control immediately.

The sheet must debounce writes: while dragging a slider, send at most one update
per 100 ms. Do not queue a backlog.

### Forget and unbind

There is one lifecycle action, **“Unbind & forget”**:

1. If connected, send the Tuya unbind command and wait briefly.
2. Delete local credentials and saved settings.
3. Return to the scan state.

There is no separate “forget locally” action in the normal UI; leaving the
strip bound to a key the app no longer has would strand it. If unbind fails
because the strip is out of range, show: “Move closer and retry, or power-cycle
the strip to reset it.” Do not delete credentials until unbind succeeds or the
user confirms a forced local forget in the error dialog.

## Music reaction

`LedAudioReactor`:

- Attach `Visualizer(sessionId)` with capture size 1024.
- Use FFT data, not the waveform.
- Compute `binHz = samplingRate / captureSize`.
- Low band: 30–250 Hz.
- High band: 2–8 kHz.
- Normalize each band with a running maximum that decays slowly; apply the
  sensitivity multiplier; clamp to 0..1.
- Smooth with attack 0.45 and release 0.15.
- Emit a `BandLevels(low, high, overall)` every 100 ms.

Mapping to the strip:

- Work mode: colour.
- Power: on while playing and reaction is enabled.
- `mix = high / (low + high + epsilon)`.
- `wantedHue = lerp(warmHue, coolHue, mix)`.
- `hueSentToStrip = (wantedHue + hueOffset) % 360`.
- `brightness = min + (max - min) * overall`.
- Send DP3 and DP5 `v` with the same brightness value.
- On a low-band beat (low crosses a threshold after a quiet period), add a short
  brightness pulse.
- When both bands are near zero for more than 2 s, hold the last colour at low
  brightness rather than flickering.
- When playback pauses or stops, stop sending and fade to the idle colour.
  Keep the BLE connection for 2 minutes so a quick resume is instant, then
  disconnect. On resume, reconnect and re-pair if needed.
- If the strip is disconnected when playback starts and `auto_connect` or
  reaction is enabled, reconnect in the background; do not block playback.

Because the strip is a single colour, low and high are blended into one hue
rather than shown in separate zones. The UI must say this plainly.

## Permissions

- Add `BLUETOOTH_SCAN` to the manifest (Android 12+).
- Keep existing `BLUETOOTH_CONNECT` and `ACCESS_FINE_LOCATION`.
- Request `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` before scanning.
- Request `RECORD_AUDIO` when the user enables “React to music”.
- If a permission is denied, show a short explanation and do not crash.
- No `MEDIA_PROJECTION` and no microphone capture.

## Errors and edge cases

- Bluetooth off: prompt, resume scan after it is enabled.
- Location services off on Android <12: prompt.
- Strip already bound to another key: pairing returns `2` (already paired) or
  the device does not verify. Show “Reset the strip, then scan again.” Provide
  an unbind action only when the app has valid credentials.
- Strip reset/power-cycled: bound state may clear; allow re-pair.
- Strip out of range: show disconnected, retry with backoff, keep the saved
  credentials.
- GATT 133 / transient failures: retry once, then backoff.
- Visualizer init failure: disable reaction with a message; manual control still
  works.
- Android kills the app: credentials remain; reconnect on next launch if
  auto-connect is on.
- Dual audio / crossfade: reattach to the new session id.
- No music playing: do not spam BLE writes.

## Testing

Unit tests (JVM, no hardware):

- CRC16 known vectors.
- Varint encode/decode across boundaries.
- Packet fragmentation and reassembly, including multi-packet payloads.
- AES-CBC zero padding and MD5 key derivation.
- DP encode/decode for bool, value, enum (one byte), string.
- Band normalisation and mapping for synthetic FFT frames.
- Advertisement identity decode with a fixed fixture.

Manual hardware tests:

- Fresh scan, pair, connect, test colour, disconnect, reconnect.
- Pair-on-reconnect behavior.
- Brightness and power.
- Reaction with bass-heavy and treble-heavy tracks.
- Crossfade/dual audio session reattachment.
- Bluetooth off/on, strip power cycle, out of range.
- Unbind and re-pair.

Instrumentation is not useful for BLE without hardware, so the protocol tests
are pure JVM tests and the rest is a written manual checklist.

## Files to change

New:

- `app/src/main/java/com/labix/navirom/led/TuyaBleProtocol.kt`
- `app/src/main/java/com/labix/navirom/led/TuyaBleScanner.kt`
- `app/src/main/java/com/labix/navirom/led/TuyaLedController.kt`
- `app/src/main/java/com/labix/navirom/led/LedCredentialStore.kt`
- `app/src/main/java/com/labix/navirom/led/LedStripManager.kt`
- `app/src/main/java/com/labix/navirom/led/LedAudioReactor.kt`
- `app/src/main/java/com/labix/navirom/led/LedModels.kt`
- `app/src/main/java/com/labix/navirom/ui/components/LedStripSheet.kt`
- `app/src/test/java/com/labix/TuyaBleProtocolTest.kt`
- `app/src/test/java/com/labix/LedBandMappingTest.kt`

Modified:

- `app/src/main/AndroidManifest.xml` — `BLUETOOTH_SCAN`
- `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt` —
  active audio session flow, LED manager
- `app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt` — state and actions
- `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt` — sheet wiring
- `app/src/main/java/com/labix/navirom/ui/screens/ServerSettingsScreen.kt` — row
- `app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt` — en/sq/de strings
- `gradle/libs.versions.toml` — version bump when released

## Defaults and remaining tuning

These are decided; the builder may tune the numbers after listening tests but
should not change the behaviour:

- `hue_offset = 240` (this unit renders hue 0 as blue).
- Warm wanted hue = 30 (orange), cool wanted hue = 210 (cyan).
- Idle colour = wanted hue 30, saturation 800, value 200 (dim warm).
- `brightness_min = 200`, `brightness_max = 900`.
- Attack 0.45, release 0.15.
- Low-band beat pulse: always on for now; no separate switch.
- If both bands are silent, hold the idle colour rather than turning off, so
  the strip does not flash on every pause.

Visual design details of the sheet should follow the existing WLAN sheet and
theme; no new design system is needed.

## Risks

- Firmware variants may differ in pairing or DP mapping. The app should detect
  protocol major 3 and show a clear unsupported message otherwise.
- BLE write throughput varies by phone. Coalescing and a single command queue
  are mandatory.
- Visualizer support varies by device/ROM. Manual control must remain usable
  when it fails.
- The strip’s hue mapping is non-standard; calibration is required for good
  warm/cool results.
- Pairing changes device ownership; the UI must be explicit about it.
