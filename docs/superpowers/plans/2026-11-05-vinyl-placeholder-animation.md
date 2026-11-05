# Full-Player Lyric Status Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show clear, localized loading and no-lyrics states in the full player.

**Architecture:** Use existing `NaviromStrings` translations from `FullPlayerModal.kt`; preserve the existing vinyl rotation. No letter-scattering animation is included.

**Tech Stack:** Kotlin, Jetpack Compose, Android instrumentation tests.

**Spec:** `docs/superpowers/specs/2026-11-05-vinyl-placeholder-animation-design.md`

## Global Constraints

- Use existing `lyrics_loading`, `lyrics_not_found`, and `lyrics_not_found_desc` translations.
- Preserve existing retry and TeksteShqip actions and existing vinyl rotation.
- Do not add dependencies.

## Review Focus

- English, Albanian, and German lyric status text must match existing translations.
- Loading must show both progress and status text.
- Empty and error lyric states must keep the existing actions.

---

### Task 1: Localized lyric loading and empty state — complete

**Files:**
- Modify: `app/src/main/java/com/labix/navirom/ui/components/FullPlayerModal.kt`
- Test: `app/src/androidTest/java/com/labix/FullPlayerLyricsLocalizationTest.kt`

- [x] Add Compose tests for loading and no-lyrics text in English, Albanian, and German.
- [x] Verify the tests fail before the UI change.
- [x] Show localized loading text alongside progress; localize empty/error message and description.
- [x] Verify focused instrumentation tests pass.

### Canceled per user request

The user clarified they want to keep the existing vinyl rotation but drop only letter scattering. The temporary scattering implementation and its tests were removed; no production vinyl animation changes remain. The previously planned animation-gating task is canceled.
