## Why

The app needs sustainable rewarded and subscription conversion points without interrupting Quran reading, worship, form submission, or navigation. The current experience lacks consistent disclosures, centralized temporary ad-free entitlement handling, and balanced free access across downloadable content.

## What Changes

- Add a reusable, affirmative opt-in rewarded-value flow that clearly labels `AD`, states the exact reward before playback, supports cancellation, and never blocks the underlying action when an ad is unavailable.
- Keep core reading, streaming, Tafsir access, quiz explanations, and learning-plan submission free while offering rewarded extensions for additional offline translation, Tafsir, recitation audio, quiz retry/skip, and deterministic progress insights.
- Add a one-hour temporary ad-free reward that suppresses app-open, interstitial, banner, native, and full-screen native ads after the earned-reward callback.
- Add an owned, persistent homepage-bottom “quiet reading / remove ads” entry outside ad containers, offering subscription/lifetime removal and the optional one-hour rewarded route.
- Add a rate-limited post-interstitial entry to the one-hour ad-free option without automatically launching a rewarded ad.
- Add multilingual and RTL-safe copy, analytics events, and accessibility behavior for all new entry points.
- Do not add generative religious interpretation; progress insights use only real user activity and fixed deterministic templates.

## Capabilities

### New Capabilities

- `rewarded-value-access`: Free allowances, explicit rewarded extensions, subscription bypass, failure/cancel behavior, and entitlement persistence for translation, Tafsir, audio, quiz, and progress insights.
- `temporary-ad-free`: One-hour earned ad suppression, expiry handling, persistent homepage entry, post-interstitial promotion limits, and permanent-removal alternatives.
- `deterministic-progress-insights`: Evidence-based learning progress summaries generated from recorded user activity without AI-authored Quranic interpretation.

### Modified Capabilities

None.

## Impact

- Affects the app, ad library, Quran download/settings, Tafsir, quiz, learning-plan, home UI, localization resources, Firebase analytics, and ad lifecycle paths.
- Uses the existing Google Mobile Ads and Google Play Billing integrations; no new external SDK is required.
- Requires regression checks for normal downloads, reading, streaming, quiz submission/navigation, learning-plan save, RTL layouts, subscription ad suppression, reward delivery, and ad no-fill/cancellation.
