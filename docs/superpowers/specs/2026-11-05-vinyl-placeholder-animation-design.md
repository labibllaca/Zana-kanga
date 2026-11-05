# Full-player lyric status localization

## Intent
Make lyric lookup progress and missing-lyrics feedback clear in the app's selected language.

## Scope
- Show a loading indicator and the existing localized `lyrics_loading` text while lyrics are being fetched.
- Show the existing localized `lyrics_not_found` message and description when lyrics are unavailable.

## Approach
Use existing `NaviromStrings` translations and keep the change within the full-player lyric view. No dependencies or other UI changes.

## Non-goals
- Do not change the existing vinyl rotation animation.
- Do not add the proposed letter-scattering effect; the user chose to drop it.
