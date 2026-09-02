## 1. Tafsir manifest readiness

- [x] 1.1 Make ordinary Tafsir preparation load persisted/bundled manifest on IO without forced network
- [x] 1.2 Preserve explicit force-refresh with local fallback and coalesce concurrent prepare callbacks
- [x] 1.3 Remove the activity's process-cache-to-force-refresh misclassification

## 2. Tafsir content pipeline

- [x] 2.1 Add single-flight get-or-load behavior with cache-source diagnostics
- [x] 2.2 Connect selected-verse prefetch before navigation and adjacent-verse warm-up after success
- [x] 2.3 Tie ActivityTafsir content jobs to lifecycle, cancel stale requests, and record ready timings

## 3. Tafsir process-session ad removal

- [x] 3.1 Add a non-persistent process entitlement and distinct rewarded placement
- [x] 3.2 Add backward-compatible native-ad display-state callback
- [x] 3.3 Build 48dp RTL-safe owned footer action outside the native ad with English and Arabic copy
- [x] 3.4 Implement subscription-first state, non-subscriber return state, and earned-only session grant
- [x] 3.5 Recheck subscription/permanent/temporary/session suppression on resume and late ad callbacks

## 4. Verification

- [x] 4.1 Add unit tests for process entitlement and request-coalescing/cache behavior where practical
- [x] 4.2 Compile adlib and app Kotlin/Java and run relevant unit tests
- [x] 4.3 Build Google Play debug APK and run strict OpenSpec and diff validation
- [x] 4.4 Review small-screen, landscape, RTL, large text, dark theme, failure/cancel, and reading/navigation behavior

## 5. Compact and extend the native ad-removal entry

- [x] 5.1 Reduce the Tafsir action's visible background height while preserving 14sp text and a 48dp touch target
- [x] 5.2 Attach a compact action to the actual `FragMain` bottom native ad without adding a duplicate large promo card
- [x] 5.3 Add a distinct home process state, rewarded placement, English/Arabic copy, and subscription-return state machine
- [x] 5.4 Apply centralized and home-session suppression before requests, on resume, and in late native callbacks
- [x] 5.5 Add unit coverage and run compile, APK, strict OpenSpec, diff, and responsive/RTL/theme interaction checks

## 6. Unified rewarded loading and fallback

- [x] 6.1 Add rewarded-interstitial cache/load/show support with a debug test ID and blank-by-default production Remote Config key
- [x] 6.2 Build the shared eight-second rewarded-first coordinator with immediate cache show, main-thread polling, lifecycle cancellation, and earned-only callbacks
- [x] 6.3 Add the dimmed loading and no-reward-video retry UI with outside/back dismissal, 48dp controls, and English/Arabic resources
- [x] 6.4 Migrate app, Tafsir unlock, Quiz gems/review, download, audio, insight, and ad-removal rewarded entry points to the shared flow
- [x] 6.5 Preload after consent and replenish after consumption while preserving one physical request per cache ID
- [x] 6.6 Add decision/cache tests and run app/quiz/adlib compile, unit tests, APK, strict OpenSpec, diff, and device interaction checks

## 7. New-user first Tafsir reliability

- [x] 7.1 Gate navigation Tafsir prefetch on local manifest readiness before resolving the saved/default key
- [x] 7.2 Evict single-flight entries from their completion callback so a completed failed prefetch cannot be reused
- [x] 7.3 Add one bounded retry for retryable cold-network failures and fall back to a valid language Tafsir when a saved selection is stale
- [x] 7.4 Add regression tests and run Tafsir/app compile, unit tests, APK, strict OpenSpec, diff, and first-open self-checks

## 8. Reward loading polish and canonical Quiz entry

- [x] 8.1 Replace the loading card and visible copy with one centered spinner while retaining cancel and unavailable/retry behavior
- [x] 8.2 Route the Quran reader action through a one-shot MainActivity intent to the existing `QuranQuestionFragment`
- [x] 8.3 Add regression coverage for canonical Quiz intent target, flags, and one-shot consumption
- [x] 8.4 Run app/quiz/adlib tests, APK, strict OpenSpec, diff, and loading/Quiz device interaction checks
