# Soundlogic LED Strip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add local Tuya BLE pairing and audio-reactive low/high control for the Soundlogic LED strip inside Zana.

**Architecture:** A pure Kotlin Tuya protocol layer handles advertisements, key derivation, packet framing, and datapoints. A frame link wraps Android GATT. A controller owns the pairing/session state machine and command queue. A manager coordinates scanning, credentials, settings, and a Visualizer-driven reactor. Compose exposes it through a Settings row and a bottom sheet.

**Tech Stack:** Kotlin, Android BLE (`BluetoothLeScanner`, `BluetoothGatt`), AES-CBC/MD5 from `javax.crypto`, `android.media.audiofx.Visualizer`, Compose Material 3, JUnit 4, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-11-03-soundlogic-led-strip-design.md`

## Global Constraints

- `minSdk 24`, `targetSdk 36`.
- Do not add third-party BLE, Tuya, or audio-analysis dependencies. Use Android APIs and `javax.crypto`.
- The strip is protocol `3.3`; support protocol major `3` only and show a clear unsupported message otherwise.
- Tuya enums are **one byte**. Values are four-byte big-endian signed. Strings are UTF-8.
- Send the pair request on **every** connection, after device info and before datapoints.
- Write brightness to both DP3 and DP5's JSON `v`, always with the same value.
- Send at most one LED update per 100 ms while reacting.
- Store credentials only in private app preferences. Never log the local key or virtual ID.
- No cloud calls, no Tuya account, no microphone capture, no `MEDIA_PROJECTION`.
- The strip is whole-colour only. Do not build per-LED or chase effects.
- Use `gradle`, not `./gradlew`; this repository has no wrapper script.

## Review Focus

These are the conditions most likely to break the feature for a real user. Each is pinned by a test in the task that owns the code.

1. **Strip already bound to another key** — pairing must fail clearly, not loop or crash. (Task 5)
2. **Bluetooth off or permission denied** — scanning/connecting must show a message and leave playback untouched. (Tasks 4, 12)
3. **Dual deck or crossfade changes the audio session** — the reactor must reattach and keep reacting. (Tasks 8, 9)
4. **Silence or pause** — no flicker, no write flood, idle fade, then disconnect. (Tasks 7, 9)
5. **BLE disconnects mid-command** — reconnect with backoff and do not block playback. (Tasks 3, 9)

---

### Task 1: Protocol primitives and datapoint codec

**Files:**
- Create: `app/src/main/java/com/labix/navirom/led/TuyaBleModels.kt`
- Create: `app/src/main/java/com/labix/navirom/led/TuyaBleProtocol.kt`
- Test: `app/src/test/java/com/labix/TuyaBleProtocolTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `enum class TuyaDpType(val code: Int)`: `RAW(0)`, `BOOL(1)`, `VALUE(2)`, `STRING(3)`, `ENUM(4)`, `BITMAP(5)`.
  - `sealed interface TuyaDpValue`: `Bool(Boolean)`, `IntValue(Int)`, `StringValue(String)`, `Raw(ByteArray)`.
  - `data class TuyaDatapoint(val id: Int, val type: TuyaDpType, val value: TuyaDpValue)`.
  - `data class TuyaAdvertisementIdentity(val uuid: String, val productId: String, val bound: Boolean, val protocolMajor: Int)`.
  - `object TuyaDp`: `POWER = 1`, `MODE = 2`, `BRIGHTNESS = 3`, `COLOR = 5`.
  - `enum class TuyaWorkMode(val value: Int)`: `WHITE(0)`, `COLOUR(1)`, `SCENE(2)`, `MUSIC(3)`.
  - `object TuyaBleProtocol` with:
    - `fun crc16(data: ByteArray): Int`
    - `fun encodeVarint(value: Int): ByteArray`
    - `data class VarintResult(val value: Int, val nextOffset: Int)`
    - `fun decodeVarint(data: ByteArray, offset: Int = 0): VarintResult?`
    - `fun encodeDatapoint(dp: TuyaDatapoint): ByteArray`
    - `fun parseDatapoints(data: ByteArray): List<TuyaDatapoint>`
    - `fun decodeAdvertisement(manufacturerData: ByteArray?, serviceData: ByteArray?): TuyaAdvertisementIdentity?`
    - constants for service/characteristic UUID strings.

- [ ] **Step 1: Write the failing protocol tests**

```kotlin
package com.labix

import com.labix.navirom.led.*
import org.junit.Assert.*
import org.junit.Test

class TuyaBleProtocolTest {
    @Test fun crc16_matches_modbus_vector() {
        assertEquals(0x4B37, TuyaBleProtocol.crc16("123456789".toByteArray()))
    }

    @Test fun varint_round_trips_boundaries() {
        for (value in listOf(0, 1, 127, 128, 16383, 16384, 1_000_000)) {
            val encoded = TuyaBleProtocol.encodeVarint(value)
            val decoded = TuyaBleProtocol.decodeVarint(encoded)
            assertEquals(value, decoded!!.value)
            assertEquals(encoded.size, decoded.nextOffset)
        }
    }

    @Test fun enum_datapoint_is_one_byte() {
        val encoded = TuyaBleProtocol.encodeDatapoint(
            TuyaDatapoint(TuyaDp.MODE, TuyaDpType.ENUM, TuyaDpValue.IntValue(1))
        )
        assertArrayEquals(byteArrayOf(2, 4, 1, 1), encoded)
    }

    @Test fun value_datapoint_is_four_byte_big_endian() {
        val encoded = TuyaBleProtocol.encodeDatapoint(
            TuyaDatapoint(TuyaDp.BRIGHTNESS, TuyaDpType.VALUE, TuyaDpValue.IntValue(1000))
        )
        assertArrayEquals(byteArrayOf(3, 2, 4, 0, 0, 0x03, 0xE8.toByte()), encoded)
    }

    @Test fun parse_reads_mixed_datapoints() {
        val data = byteArrayOf(1, 1, 1, 1, 2, 4, 1, 1, 5, 3, 5) +
            "hello".toByteArray()
        val parsed = TuyaBleProtocol.parseDatapoints(data)
        assertEquals(3, parsed.size)
        assertEquals(TuyaDpValue.Bool(true), parsed[0].value)
        assertEquals(TuyaDpValue.IntValue(1), parsed[1].value)
        assertEquals(TuyaDpValue.StringValue("hello"), parsed[2].value)
    }

    @Test fun advertisement_identity_matches_strip_fixture() {
        val manufacturer = "000300000100b8b8ef22f26fcf7bbbb007d36ca61ca3".hex()
        val serviceData = "007238636f67783672".hex()
        val identity = TuyaBleProtocol.decodeAdvertisement(manufacturer, serviceData)!!
        assertEquals("tuyaececbfe2aece", identity.uuid)
        assertEquals("r8cogx6r", identity.productId)
        assertFalse(identity.bound)
        assertEquals(3, identity.protocolMajor)
    }

    private fun String.hex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaBleProtocolTest"`
Expected: compilation failure because `com.labix.navirom.led` does not exist.

- [ ] **Step 3: Implement the models and protocol primitives**

Implement CRC16 Modbus, LEB128 varints, datapoint encode/parse, and the advertisement decoder exactly as specified. `decodeAdvertisement` must:

- Read service data `0x00 || productId(8 ASCII)`.
- Read manufacturer payload 22 bytes; `bound = (payload[0].toInt() and 0x80) != 0`; `protocolMajor = payload[1].toInt()`.
- Derive `key = MD5(productId)` and `uuid = AES-CBC-decrypt(payload[6..21], key, iv = key)` as 16 ASCII bytes.
- Return `null` for any malformed input rather than throwing.

- [ ] **Step 4: Run the test to verify it passes**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaBleProtocolTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/TuyaBleModels.kt \
        app/src/main/java/com/labix/navirom/led/TuyaBleProtocol.kt \
        app/src/test/java/com/labix/TuyaBleProtocolTest.kt
git commit -m "feat: add Tuya BLE protocol primitives"
```

---

### Task 2: Framing, crypto, packet assembly, and time response

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/led/TuyaBleProtocol.kt`
- Modify: `app/src/main/java/com/labix/navirom/led/TuyaBleModels.kt`
- Test: `app/src/test/java/com/labix/TuyaBleProtocolTest.kt`

**Interfaces:**
- Consumes: Task 1 types.
- Produces:
  - `data class TuyaFrame(val sequence: Int, val reply: Int, val command: Int, val data: ByteArray, val securityFlag: Int)`.
  - `fun deriveLoginKey(localKey: ByteArray): ByteArray`
  - `fun deriveSessionKey(localKey: ByteArray, srand: ByteArray): ByteArray`
  - `fun buildPairPayload(deviceUuid: String, localKey: ByteArray, virtualId: String): ByteArray`
  - `fun buildPackets(sequence: Int, command: Int, data: ByteArray = ByteArray(0), key: ByteArray? = null, securityFlag: Int = 5, responseTo: Int = 0): List<ByteArray>`
  - `fun decodeEnvelope(envelope: ByteArray, key: ByteArray? = null): TuyaFrame?`
  - `fun buildTimeResponse(nowMillis: Long, timezoneOffsetMinutes: Int): ByteArray`
  - `class TuyaPacketAssembler { fun feed(packet: ByteArray): ByteArray?; fun reset() }`

- [ ] **Step 1: Write the failing framing tests**

Add tests that assert:

- `buildPackets` fragments a 44-byte pair payload into packets of at most 20 bytes; the first packet starts with `0x00`, carries the total length, and has `0x30` before the envelope.
- An encrypted frame round-trips: build with a known key/flag, capture the envelope from the packet, decode it, and assert sequence, command, data, and security flag.
- An unencrypted frame round-trips.
- `TuyaPacketAssembler` reassembles a multi-packet envelope and ignores duplicates/out-of-order packets.
- `deriveLoginKey` is `MD5(localKey)` and `deriveSessionKey` is `MD5(localKey || srand)` using a fixed six-byte test key generated inside the test (never a real device key).
- `buildPairPayload` is exactly 44 bytes.
- `buildTimeResponse` starts with the ASCII millisecond timestamp followed by a big-endian signed 16-bit timezone.

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaBleProtocolTest"`
Expected: FAIL with unresolved references for the new functions.

- [ ] **Step 3: Implement**

- `buildPackets`: 12-byte big-endian header, CRC16, zero-pad to 16, envelope `[0x00]` or `[flag, iv, ciphertext]`, then LEB128 chunking with 20-byte GATT packets.
- `decodeEnvelope`: accept flag `0x00` as plaintext, flags `4/5/14/15` as AES-CBC with the supplied key; verify length and CRC; return `null` on any mismatch.
- `TuyaPacketAssembler`: track the first packet's total length and protocol byte, append subsequent packets in order, return the envelope when complete.
- Use `MessageDigest.getInstance("MD5")`, `Cipher.getInstance("AES/CBC/NoPadding")`, and `SecureRandom` for the IV.

- [ ] **Step 4: Run the test to verify it passes**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaBleProtocolTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/TuyaBleModels.kt \
        app/src/main/java/com/labix/navirom/led/TuyaBleProtocol.kt \
        app/src/test/java/com/labix/TuyaBleProtocolTest.kt
git commit -m "feat: add Tuya BLE framing and crypto"
```

---

### Task 3: GATT transport and frame link

**Files:**
- Create: `app/src/main/java/com/labix/navirom/led/TuyaGattTransport.kt`
- Create: `app/src/main/java/com/labix/navirom/led/TuyaFrameLink.kt`
- Test: `app/src/test/java/com/labix/TuyaFrameLinkTest.kt` (fake transport only; the Android GATT class is platform glue)

**Interfaces:**
- Consumes: Task 1/2 protocol.
- Produces:
  - `interface TuyaGattTransport`:
    - `val notifications: Flow<ByteArray>`
    - `val isConnected: Boolean`
    - `suspend fun connect(address: String)`
    - `suspend fun write(data: ByteArray)`
    - `suspend fun disconnect()`
    - `fun close()`
  - `class AndroidTuyaGattTransport(context: Context) : TuyaGattTransport`
  - `interface TuyaFrameLink`:
    - `val frames: SharedFlow<TuyaFrame>`
    - `fun setSessionKey(key: ByteArray?)`
    - `suspend fun connect(address: String)`
    - `suspend fun request(sequence: Int, command: Int, data: ByteArray, key: ByteArray, securityFlag: Int, responseTo: Int = 0): TuyaFrame?`
    - `suspend fun disconnect()`
    - `fun close()`
  - `class TuyaFrameLinkImpl(transport: TuyaGattTransport, scope: CoroutineScope) : TuyaFrameLink`

- [ ] **Step 1: Write the failing frame-link test**

Use a `FakeGattTransport` that records writes and emits a prebuilt encrypted response envelope. Assert that `request` writes the expected packet sequence and returns the decoded frame. Assert that an unsolicited time request is emitted on `frames` and is not mistaken for the request response.

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaFrameLinkTest"`
Expected: FAIL because the classes do not exist.

- [ ] **Step 3: Implement the Android transport**

- Resolve `BluetoothManager` and `BluetoothAdapter`; fail with a clear exception if Bluetooth is off.
- `connect`: `connectGatt(context, false, callback, TRANSPORT_LE)`, wait for `STATE_CONNECTED`, discover services, find service `1910` and characteristics `2B10`/`2B11`, enable notifications including the `2902` descriptor.
- `write`: serialize with a `Mutex`, use `WRITE_TYPE_NO_RESPONSE`, and delay briefly between 20-byte packets.
- `disconnect`/`close`: cancel the GATT, close the callback, clear the notification flow.
- Never log packet plaintext or keys.

- [ ] **Step 4: Implement the frame link**

- Subscribe to `transport.notifications`, feed `TuyaPacketAssembler`, decode with the current session key, and:
  - complete a pending `request` when `reply == sequence`;
  - emit all other decoded frames to `frames`.
- `request`: build packets with `TuyaBleProtocol.buildPackets`, write each, wait up to 3 seconds for the matching frame, and return `null` on timeout.
- Decode device-info responses with the login key passed to that request; decode unsolicited frames with the session key set by `setSessionKey`.

- [ ] **Step 5: Run the test and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaFrameLinkTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/TuyaGattTransport.kt \
        app/src/main/java/com/labix/navirom/led/TuyaFrameLink.kt \
        app/src/test/java/com/labix/TuyaFrameLinkTest.kt
git commit -m "feat: add Tuya BLE GATT transport"
```

---

### Task 4: Scanner, credentials, and settings store

**Files:**
- Create: `app/src/main/java/com/labix/navirom/led/TuyaBleScanner.kt`
- Create: `app/src/main/java/com/labix/navirom/led/LedCredentialStore.kt`
- Create: `app/src/main/java/com/labix/navirom/led/KeyValueStorage.kt`
- Test: `app/src/test/java/com/labix/LedCredentialStoreTest.kt`

**Interfaces:**
- Consumes: Task 1 `TuyaAdvertisementIdentity`, protocol constants.
- Produces:
  - `data class TuyaBleDevice(val address: String, val name: String, val rssi: Int, val identity: TuyaAdvertisementIdentity?)`.
  - `class TuyaBleScanner(context: Context)` with `fun start(onDevice: (TuyaBleDevice) -> Unit, onError: (String) -> Unit)` and `fun stop()`.
  - `interface KeyValueStorage { fun putString(key: String, value: String?); fun getString(key: String, default: String? = null): String?; fun putBoolean(...); fun getBoolean(...); fun putInt(...); fun getInt(...); fun remove(key: String); fun clear() }`
  - `class SharedPrefsStorage(context: Context, name: String) : KeyValueStorage`
  - `data class LedCredentials(val address: String, val name: String, val uuid: String, val productId: String, val localKeyHex: String, val virtualId: String, val protocolMajor: Int)`
  - `data class LedStripSettings(val reactToMusic: Boolean = false, val sensitivity: Float = 1f, val warmHue: Int = 30, val coolHue: Int = 210, val hueOffset: Int = 240, val idleHue: Int = 30, val idleSaturation: Int = 800, val idleValue: Int = 200, val brightnessMin: Int = 200, val brightnessMax: Int = 900, val autoConnect: Boolean = true)`
  - `class LedCredentialStore(storage: KeyValueStorage)` with `load(): LedCredentials?`, `save(LedCredentials)`, `clear()`.
  - `class LedPrefs(storage: KeyValueStorage)` with `load(): LedStripSettings`, `save(LedStripSettings)`, `clear()`.

- [ ] **Step 1: Write the failing store test**

Use an in-memory `KeyValueStorage` fake. Assert:

- `save` then `load` returns the same credentials.
- `clear` removes them.
- settings defaults match the spec.
- `hueOffset` is coerced into `0..359` and `sensitivity` into `0.5f..2.0f`.
- The local key is never written to logs (no logging call in this class).

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.LedCredentialStoreTest"`
Expected: FAIL because the classes do not exist.

- [ ] **Step 3: Implement the scanner and stores**

- Scanner: `BluetoothLeScanner.startScan` with a `ScanFilter` for service `A201`; parse each `ScanRecord` through `TuyaBleProtocol.decodeAdvertisement`; deduplicate by address; report errors from `onScanFailed`.
- Stores: use constant keys, never log values. `LedCredentialStore.clear()` removes only credential keys. `LedPrefs` coerces values on load.
- `SharedPrefsStorage` wraps `Context.getSharedPreferences(name, MODE_PRIVATE)`.

- [ ] **Step 4: Run the test and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.LedCredentialStoreTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/TuyaBleScanner.kt \
        app/src/main/java/com/labix/navirom/led/LedCredentialStore.kt \
        app/src/main/java/com/labix/navirom/led/KeyValueStorage.kt \
        app/src/test/java/com/labix/LedCredentialStoreTest.kt
git commit -m "feat: add LED scanner and credential store"
```

---

### Task 5: Controller pairing and session establishment

**Files:**
- Create: `app/src/main/java/com/labix/navirom/led/TuyaLedController.kt`
- Test: `app/src/test/java/com/labix/TuyaLedControllerTest.kt`

**Interfaces:**
- Consumes: `TuyaFrameLink`, `TuyaBleDevice`, `LedCredentials`, protocol.
- Produces:
  - `sealed interface TuyaLedConnectionState`: `Disconnected`, `Connecting`, `Pairing`, `Connected(deviceName: String)`, `Error(message: String)`.
  - `class TuyaLedController(linkFactory: (String) -> TuyaFrameLink, scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO))`
  - `val state: StateFlow<TuyaLedConnectionState>`
  - `suspend fun pair(device: TuyaBleDevice): LedCredentials`
  - `suspend fun connect(credentials: LedCredentials)`
  - `suspend fun disconnect()`
  - `fun close()`

**Interfaces for tests:** a `FakeFrameLink` records requests and returns queued `TuyaFrame`s.

- [ ] **Step 1: Write the failing pairing tests**

Assert:

- `pair` on an unbound device sends, in order: unencrypted device info, unencrypted pair, then (after reconnect) encrypted device info, encrypted pair.
- The first pair payload is 44 bytes and starts with the device UUID.
- `connect` always sends the pair request after device info, even when the credentials are already saved.
- Pair result `2` is treated as success; result `1` throws and sets `Error`.
- A device with `identity == null` or `protocolMajor != 3` fails before connecting.

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaLedControllerTest"`
Expected: FAIL because the controller does not exist.

- [ ] **Step 3: Implement pairing and connect**

- Generate the 6-byte key with `SecureRandom`, the 22-character virtual ID from 11 random bytes hex, and use the advertisement UUID.
- First bind uses `key = null` (unencrypted) and `securityFlag = 0` for device info and pair.
- Verification reconnect: device info with `key = MD5(localKey)`, `securityFlag = 4`; parse `srand` at data `6..11`; derive session; pair with session key, `securityFlag = 5`.
- `connect(credentials)` performs device info, session derivation, pair, and leaves the link connected.
- Start a collector for unsolicited frames; answer `0x8011` with `buildTimeResponse` and the session key.
- Set state transitions; always close the link on failure.

- [ ] **Step 4: Run the test and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaLedControllerTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/TuyaLedController.kt \
        app/src/test/java/com/labix/TuyaLedControllerTest.kt
git commit -m "feat: add Tuya LED pairing and session"
```

---

### Task 6: Controller commands, status, and unbind

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/led/TuyaLedController.kt`
- Test: `app/src/test/java/com/labix/TuyaLedControllerTest.kt`

**Interfaces:**
- Adds:
  - `suspend fun setPower(on: Boolean)`
  - `suspend fun setMode(mode: TuyaWorkMode)`
  - `suspend fun setBrightness(value: Int)`
  - `suspend fun setColor(hue: Int, saturation: Int = 1000, brightness: Int = 1000)`
  - `suspend fun queryStatus(): List<TuyaDatapoint>`
  - `suspend fun unbind()`

- [ ] **Step 1: Write the failing command tests**

Assert:

- `setColor` sends one DP command containing DP1 `true`, DP2 enum `1`, DP3 value `brightness`, and DP5 string `{"h":<hue>,"s":<sat>,"v":<brightness>}`.
- DP3 and DP5 `v` are always equal.
- `setPower(false)` sends DP1 `false`.
- `queryStatus` parses `0x8001` reports and returns the datapoints.
- `unbind` sends command `0x0005` with the session key and then transitions to `Disconnected`.
- Commands are serialized: two concurrent `setColor` calls never interleave sequence numbers.
- A frame with `reply` not matching the request is emitted as unsolicited, not consumed.

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaLedControllerTest"`
Expected: FAIL for the missing methods.

- [ ] **Step 3: Implement the command queue**

- Use a `Mutex` and an incrementing `Int` sequence starting at 1.
- `request(command, data, key, flag)` returns the matching frame or throws on timeout.
- `setColor` builds the four datapoints and sends one `0x0002` command.
- `queryStatus` sends `0x0003`, then collects `0x8001` frames for 1 second.
- `unbind` sends `0x0005` with the session key; ignore the response content.
- Coalesce repeated calls: if a newer colour command is waiting, drop the older one.
- Keep the session key private; never log it.

- [ ] **Step 4: Run the test and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.TuyaLedControllerTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/TuyaLedController.kt \
        app/src/test/java/com/labix/TuyaLedControllerTest.kt
git commit -m "feat: add Tuya LED commands and status"
```

---

### Task 7: Band analyzer and colour mapping

**Files:**
- Create: `app/src/main/java/com/labix/navirom/led/LedAudioMapping.kt`
- Test: `app/src/test/java/com/labix/LedBandMappingTest.kt`

**Interfaces:**
- Consumes: `LedStripSettings`.
- Produces:
  - `data class BandLevels(val low: Float, val high: Float, val overall: Float)`
  - `class BandAnalyzer(samplingRateHz: Int, captureSize: Int)` with `fun analyze(fft: ByteArray): BandLevels` and `fun reset()`.
  - `data class LedColor(val hue: Int, val saturation: Int, val value: Int)`
  - `object LedColorMapper { fun map(levels: BandLevels, settings: LedStripSettings): LedColor }`

- [ ] **Step 1: Write the failing mapping tests**

Assert:

- A synthetic FFT with energy only in 30–250 Hz yields `low > high`.
- A synthetic FFT with energy only in 2–8 kHz yields `high > low`.
- Silence yields `overall < 0.01`.
- Low-only maps to the warm hue plus `hueOffset`; high-only maps to cool plus `hueOffset`; the result is wrapped into `0..359`.
- Brightness is clamped to `brightnessMin..brightnessMax`.
- The default offset `240` turns wanted hue `0` into `240`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.LedBandMappingTest"`
Expected: FAIL because the classes do not exist.

- [ ] **Step 3: Implement**

- `BandAnalyzer`: `binHz = samplingRateHz / captureSize`; average the unsigned FFT magnitudes in the band; clamp to `0..1`.
- `LedColorMapper`: `mix = high / (low + high + 0.0001f)`; `wanted = lerp(warm, cool, mix)`; `hue = (wanted + offset) mod 360`; `value = min + (max - min) * overall`.
- Keep the math pure; no Android imports in this file.

- [ ] **Step 4: Run the test and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.LedBandMappingTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/LedAudioMapping.kt \
        app/src/test/java/com/labix/LedBandMappingTest.kt
git commit -m "feat: add LED band analysis and colour mapping"
```

---

### Task 8: Visualizer reactor and audio-session tracking

**Files:**
- Create: `app/src/main/java/com/labix/navirom/led/LedAudioReactor.kt`
- Modify: `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt`
- Test: `app/src/test/java/com/labix/AudioSessionRegistryTest.kt`

**Interfaces:**
- Consumes: `BandAnalyzer`, `BandLevels`, `LedStripSettings`.
- Produces:
  - `class AudioSessionRegistry` with `fun add(id: Int?)`, `fun remove(id: Int?)`, `fun snapshot(): Set<Int>`.
  - `class LedAudioReactor(context: Context)` with:
    - `fun setSessions(sessionIds: Set<Int>)`
    - `fun setListener(listener: (BandLevels) -> Unit)`
    - `fun start()`
    - `fun stop()`
    - `fun release()`
- `AudioPlayerController` adds:
  - `val activeAudioSessionIds: StateFlow<Set<Int>>`
  - updates the registry whenever a primary/secondary `MediaPlayer` is created, replaced, paused, or released.
  - The `ledStripManager` property is added in Task 9, after `LedStripManager` exists.

- [ ] **Step 1: Write the failing session-registry test**

Assert `add(0)` and `add(null)` are ignored; adding two ids yields both; removing one leaves the other; removing the last yields an empty set.

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.AudioSessionRegistryTest"`
Expected: FAIL because the class does not exist.

- [ ] **Step 3: Implement the Visualizer reactor and controller tracking**

- `AudioSessionRegistry` is a small synchronized set.
- `LedAudioReactor` creates one `Visualizer` per session, requests FFT capture size 1024, and merges per-band values with `max`. `Visualizer.samplingRate` is in millihertz, so pass `samplingRate / 1000` to `BandAnalyzer`.
- `setDataCaptureListener` updates the latest FFT per session; a coroutine every 100 ms runs `BandAnalyzer`, applies attack 0.45/release 0.15, and emits `BandLevels`.
- If Visualizer construction fails, stop and report an error; do not crash.
- `AudioPlayerController`: after each `MediaPlayer` creation, `registry.add(player.audioSessionId)`; on release/pause, remove it; expose the flow. Do not change playback behavior.

- [ ] **Step 4: Run the test and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.AudioSessionRegistryTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/LedAudioReactor.kt \
        app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt \
        app/src/test/java/com/labix/AudioSessionRegistryTest.kt
git commit -m "feat: add LED audio reactor and session tracking"
```

---

### Task 9: Strip manager, reaction policy, and orchestration

**Files:**
- Create: `app/src/main/java/com/labix/navirom/led/LedModels.kt`
- Create: `app/src/main/java/com/labix/navirom/led/LedStripManager.kt`
- Modify: `app/src/main/java/com/labix/navirom/player/AudioPlayerController.kt`
- Test: `app/src/test/java/com/labix/LedReactionPolicyTest.kt`

**Interfaces:**
- Consumes: scanner, controller, reactor, credentials, prefs, `AudioPlayerController`.
- Produces:
  - `data class LedStripState(...)` with fields for scanning, devices, connection, connected name, reaction, sensitivity, hue offset, band levels, and message.
  - `class LedReactionPolicy` with `fun shouldRun(react: Boolean, connected: Boolean, playing: Boolean): Boolean` and `fun disconnectAfterIdleMs(): Long = 120_000L`.
  - `class LedStripManager(context: Context, player: AudioPlayerController)` with:
    - `val state: StateFlow<LedStripState>`
    - `fun startScan()`, `fun stopScan()`
    - `fun connect(device: TuyaBleDevice)`, `fun disconnect()`, `fun unbindAndForget(force: Boolean = false)`
    - `fun setPower(on: Boolean)`, `fun setBrightness(value: Int)`, `fun testColor(wantedHue: Int)`
    - `fun setReactToMusic(enabled: Boolean)`, `fun setSensitivity(value: Float)`, `fun setHueOffset(offset: Int)`
    - `fun close()`
  - `AudioPlayerController` adds `val ledStripManager: LedStripManager by lazy { LedStripManager(context, this) }`.

- [ ] **Step 1: Write the failing policy test**

Assert `shouldRun` is true only when reaction is enabled, the strip is connected, and either deck is playing; false for each missing condition. Assert the idle disconnect is `120_000L`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.LedReactionPolicyTest"`
Expected: FAIL because the class does not exist.

- [ ] **Step 3: Implement the manager**

- On `startScan`: clear devices, call scanner, collect until stopped.
- On `connect`: if a saved credential matches the address, `controller.connect`; otherwise `controller.pair` and save. Set `Pairing`/`Connecting` states.
- On successful connect: apply saved settings; if reaction is enabled and playing, start the reactor.
- Reaction loop: collect `BandLevels` with `conflate`, map to `LedColor`, call `controller.setColor` at most every 100 ms.
- Observe `player.activeAudioSessionIds` and both playback states; feed the reactor.
- On pause: fade to idle color, schedule disconnect after `disconnectAfterIdleMs`; cancel on resume.
- On `unbindAndForget(force)`: call `controller.unbind()`. If it fails and `force` is false, keep credentials and surface the error. If it succeeds or `force` is true, clear credentials, stop the reactor, and reset state.
- On scanner/controller errors: set `message`, keep the rest of the app running.
- Reconnect with backoff: 1 s, 2 s, 5 s, 10 s, 30 s.

- [ ] **Step 4: Run the test and build**

Run: `gradle :app:testDebugUnitTest --tests "com.labix.LedReactionPolicyTest"`
Expected: PASS.
Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/led/LedModels.kt \
        app/src/main/java/com/labix/navirom/led/LedStripManager.kt \
        app/src/test/java/com/labix/LedReactionPolicyTest.kt
git commit -m "feat: add LED strip manager"
```

---

### Task 10: ViewModel, settings row, and strings

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromApp.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/screens/ServerSettingsScreen.kt`
- Modify: `app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt`

**Interfaces:**
- Consumes: `LedStripManager`, `LedStripState`.
- Produces:
  - `val ledStripState: StateFlow<LedStripState>` in the ViewModel.
  - `val isLedStripSheetVisible: StateFlow<Boolean>`, `showLedStripSheet()`, `hideLedStripSheet()`.
  - Delegating methods: `startLedScan`, `connectLedDevice`, `disconnectLedStrip`, `unbindLedStrip`, `setLedPower`, `setLedBrightness`, `testLedColor`, `setLedReactToMusic`, `setLedSensitivity`, `setLedHueOffset`.
  - `ServerSettingsScreen` parameters: `ledStripState: LedStripState? = null`, `onOpenLedStrip: (() -> Unit)? = null`, `onSetLedReactToMusic: (Boolean) -> Unit = {}`.

- [ ] **Step 1: Wire the ViewModel**

Expose the manager state and delegate methods. Initialize the manager via `playerController.ledStripManager`. Do not block `init`.

- [ ] **Step 2: Add the Settings row**

After the WLAN speaker row in the Playback Settings card, add a row matching the WLAN style:

- icon `Icons.Filled.Lightbulb` (or `Icons.Filled.Bluetooth`)
- title from `str("settings_led_strip")`
- subtitle: `str("settings_led_strip_desc")`, or `Connected: <name>`, or `Reacting to music`
- button `Connect` / `Manage`
- `testTag("settings_led_strip_row")`

- [ ] **Step 3: Add strings**

Add these keys to the English map and the Albanian/German maps:

| Key | English |
|-----|---------|
| `settings_led_strip` | Soundlogic LED Strip (Bluetooth) |
| `settings_led_strip_desc` | Connect the strip and let it react to music |
| `led_connect` | Connect |
| `led_manage` | Manage |
| `led_scanning` | Scanning for LED strips… |
| `led_no_devices` | No LED strips found |
| `led_react_to_music` | React to music |
| `led_sensitivity` | Sensitivity |
| `led_reaction_brightness` | Reaction brightness |
| `led_disconnect` | Disconnect |
| `led_unbind_forget` | Unbind & forget |
| `led_calibration` | Colour calibration |
| `led_manual_disabled` | Colour is driven by the music |

Use natural Albanian/German translations; fallback to English is acceptable if a key is missing.

- [ ] **Step 4: Wire `NaviromApp`**

Collect `ledStripState` and `isLedStripSheetVisible`. Show `LedStripSheet` when visible, like `WlanSpeakerCastSheet`. Pass the new parameters to every `ServerSettingsScreen` call site.

- [ ] **Step 5: Build**

Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/labix/navirom/ui/NaviromViewModel.kt \
        app/src/main/java/com/labix/navirom/ui/NaviromApp.kt \
        app/src/main/java/com/labix/navirom/ui/screens/ServerSettingsScreen.kt \
        app/src/main/java/com/labix/navirom/ui/NaviromStrings.kt
git commit -m "feat: add LED strip settings row"
```

---

### Task 11: LED strip sheet and permissions

**Files:**
- Create: `app/src/main/java/com/labix/navirom/ui/components/LedStripSheet.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `NaviromViewModel.ledStripState` and the delegating methods.
- Produces: a `ModalBottomSheet` with the states and controls from the spec.

- [ ] **Step 1: Add permissions**

Add to `AndroidManifest.xml`:

```xml
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="s" />
<uses-feature
    android:name="android.hardware.bluetooth_le"
    android:required="false" />
```

Keep the existing `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, and location permissions.

- [ ] **Step 2: Build the sheet**

Implement the six states from the spec. Use `rememberLauncherForActivityResult(RequestMultiplePermissions)`:

- On API 31+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`.
- On older: `ACCESS_FINE_LOCATION`.
- `RECORD_AUDIO` when the reaction toggle is turned on.

Sections:

- Header with title and close.
- Scanning indicator and device list (`name`, RSSI, product ID, bound badge).
- Connected controls (power, brightness, swatches, calibration, reaction toggle, sensitivity, level meter, disconnect, unbind).
- Error/empty states with actionable text.

Debounce sliders: update state immediately, call the ViewModel at most every 100 ms.

- [ ] **Step 3: Add the level meter and calibration preview**

Use two `LinearProgressIndicator` bars for low/high. The calibration row sends a wanted red hue and a label; the `hueOffset` slider changes the persisted offset.

- [ ] **Step 4: Build**

Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/labix/navirom/ui/components/LedStripSheet.kt \
        app/src/main/AndroidManifest.xml
git commit -m "feat: add LED strip connection sheet"
```

---

### Task 12: Hardware verification and release build

**Files:**
- Modify: `gradle/libs.versions.toml` (version bump only if building a release)
- Create/update: `docs/superpowers/plans/2026-11-03-soundlogic-led-strip-checklist.md` (manual checklist)

**Interfaces:**
- Consumes: the complete feature.
- Produces: a verified debug/release build and a written test result.

- [ ] **Step 1: Unbind the spike pairing**

The physical strip is currently bound to the test key used during discovery. Before the first app pairing test, run the spike unbind tooling with the saved test credentials, or reset the strip per its manual. Do not commit the test key.

- [ ] **Step 2: Build and install**

Run: `gradle :app:assembleDebug`
Expected: BUILD SUCCESSFUL.
Install the APK on the phone with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Run the manual checklist**

- Open Settings → LED Strip; grant Bluetooth permissions.
- Scan; the strip appears as `TY` with product `r8cogx6r`.
- Pair from the app; the strip becomes bound and the app shows Connected.
- Test power off/on, brightness, red/green/blue, calibration offset.
- Play a bass-heavy track; low bar rises, colour moves warm.
- Play a treble-heavy track; high bar rises, colour moves cool.
- Pause; the strip fades to idle, then disconnects after 2 minutes.
- Resume; it reconnects and reacts.
- Enable dual audio / crossfade; reaction continues.
- Walk out of range; state shows disconnected; return; it reconnects.
- Unbind & forget; the strip can be scanned and paired again.
- Confirm the app never asks for microphone recording and no audio is stored.

- [ ] **Step 4: Run the full unit suite**

Run: `gradle :app:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit the checklist and any fixes**

```bash
git add docs/superpowers/plans/2026-11-03-soundlogic-led-strip-checklist.md \
        app/src/main/java/com/labix/navirom/led \
        app/src/main/java/com/labix/navirom/ui \
        app/src/main/AndroidManifest.xml \
        app/src/test/java/com/labix
git commit -m "test: verify Soundlogic LED strip integration"
```

---

## Self-Review

**Spec coverage:** The tasks cover advertisement identity, local pairing, session keys, pair-on-reconnect, DP control, status/unbind, credentials/settings, audio sessions, band analysis, colour mapping, reaction policy, manager orchestration, settings UI, permissions, and hardware verification. The spec's non-goals (per-LED, cloud, multiple strips, other apps' audio) are enforced by omission.

**Type consistency:** `TuyaDatapoint`/`TuyaDpValue` are defined in Task 1 and used in Tasks 2, 5, 6. `LedCredentials`/`LedStripSettings` are defined in Task 4 and used in Tasks 5, 7, 9. `BandLevels`/`LedColor` are defined in Task 7 and used in Tasks 8, 9. `LedStripState` is defined in Task 9 and used in Tasks 10, 11. `activeAudioSessionIds` is defined in Task 8 and consumed in Task 9.

**Review Focus coverage:** Bound-device failure is tested in Task 5; Bluetooth/permission failure is handled in Tasks 4 and 11; dual-deck/crossfade is handled by the session set in Task 8 and the manager in Task 9; silence/pause is tested in Tasks 7 and 9; BLE disconnect/backoff is in Tasks 3 and 9.

**Proportion:** The plan is longer than the spec because it has 12 independently testable tasks, but each task is a small unit with one test cycle. The protocol and UI details are decisions the implementer cannot infer from the spec.
