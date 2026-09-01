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
