## 1. Shared Reward Infrastructure

- [x] 1.1 Add logical rewarded placements and centralized disclosure/loading/earned/failure analytics flow
- [x] 1.2 Add persistent free-allowance and per-content entitlement storage with subscription bypass
- [x] 1.3 Add localized AD labels, reward copy, loading/error feedback, accessibility labels, and RTL-safe layouts
- [x] 1.4 Verify cancel/no-fill leaves underlying interactions usable and reward is granted only after the earned callback

## 2. Offline Translation and Tafsir

- [x] 2.1 Keep one suitable translation free and gate only additional offline translation packs
- [x] 2.2 Preserve one authoritative Tafsir per language and add explicit AD/reward affordances to additional Tafsir access
- [x] 2.3 Verify free access, subscriber bypass, rewarded unlock persistence, downloads, cancellation, and no-fill behavior

## 3. Recitation Audio

- [x] 3.1 Keep streaming free and make the first offline Surah download free
- [x] 3.2 Gate subsequent single-Surah offline downloads with an explicit rewarded option while subscribers bypass it
- [x] 3.3 Verify playback, downloads, service lifecycle, cancellation, no-fill, and existing files remain usable

## 4. Quiz Review

- [x] 4.1 Preserve free answer/explanation/exit behavior and mark retry/skip rewards with visible AD affordances
- [x] 4.2 Allow active subscribers to retry/skip directly without viewing a reward
- [x] 4.3 Verify score submission, review navigation, cancellation, no-fill, and earned actions

## 5. Deterministic Progress Insights

- [x] 5.1 Implement a free basic summary from recorded goals, progress, streak, and quiz data with honest missing-data states
- [x] 5.2 Add an optional weekly rewarded deep report using only fixed localized study-progress templates
- [x] 5.3 Verify learning-plan submission remains independent and no generated religious interpretation is introduced

## 6. One-Hour Ad-Free and Conversion Entry

- [x] 6.1 Add bounded one-hour temporary ad-free persistence and central suppression for all non-reward ad formats
- [x] 6.2 Add the owned homepage-bottom quiet-reading/remove-ads entry outside ad containers
- [x] 6.3 Offer clearly separated subscription/permanent removal and optional one-hour rewarded choices
- [x] 6.4 Add a safe post-interstitial promotion capped to once per local day without automatic rewarded launch
- [x] 6.5 Verify grant/expiry, app restart, clock boundaries, subscription/permanent states, and every ad format

## 7. Analytics, Localization, and Release Verification

- [x] 7.1 Add funnel events for entry, accept, cancel, no-fill, show, earn, action, and expiry using logical placements
- [x] 7.2 Verify English, Arabic RTL, and supported-locale fallback copy plus 48dp touch targets and contrast
- [x] 7.3 Run module tests, Google Play debug compilation, lint/diff checks, and end-to-end regression checklist
- [x] 7.4 Review final diff for unrelated changes, update implementation notes, commit scoped files, and push the working branch
