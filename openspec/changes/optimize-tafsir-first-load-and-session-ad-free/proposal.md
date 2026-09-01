## Why

The first Tafsir open can perform a manifest network refresh before requesting the selected verse, creating two sequential waits despite a bundled manifest and existing caches. The Tafsir native-ad area also lacks the requested subscription-first, voluntary rewarded alternative for users who want an uninterrupted annotation session.

## What Changes

- Serve the bundled or persisted Tafsir manifest before any optional refresh and avoid treating an empty process cache as a forced network refresh.
- Coalesce duplicate verse requests, prefetch the selected verse before navigation, and warm adjacent verses after a successful load.
- Keep loading, failure, retry, content-lock, reading, and navigation paths usable while recording actionable load-stage timings.
- Add an owned action beside the Tafsir native-ad area that first opens Premium subscription.
- After a non-subscribing user returns, replace that action with an explicit `AD` rewarded choice that hides Tafsir native ads for the remainder of the current app process only after the earned-reward callback.
- Hide both the native ad and owned action for subscribers, permanent/temporary ad-free users, and users who earned the Tafsir process-session reward.
- Keep the action text size unchanged while reducing its visible background height, and attach the same subscription-first/rewarded-second pattern to the bottom home native ad.
- Route every rewarded entry through one cache-first flow: show an available rewarded ad immediately, otherwise poll/load for at most eight seconds, then use a policy-compliant rewarded-interstitial fallback, or expose a retryable no-reward-video state.

## Capabilities

### New Capabilities

- `tafsir-fast-first-content`: Local-first manifest readiness, single-flight verse fetching, targeted prefetching, lifecycle-safe rendering, and measurable first-content stages.
- `tafsir-session-ad-free`: Subscription-first owned entry and voluntary rewarded removal of Tafsir native ads for the current app process.
- `home-session-ad-free`: Subscription-first owned entry and voluntary rewarded removal of the bottom home native ad for the current app process.
- `rewarded-ad-fallback`: Shared rewarded loading, rewarded-interstitial fallback, retry UI, earned-only delivery, and preload/cache governance for all rewarded placements.

### Modified Capabilities


## Impact

- Affects `ActivityTafsir`, `TafsirManager`, `TafsirCacheManager`, `TafsirPreloader`, and Tafsir navigation entry points.
- Adds a process-memory Tafsir ad-free entitlement and a distinct logical rewarded placement while reusing the existing ad-provider configuration.
- Extends the native-ad helper with display-state reporting without changing existing call signatures.
- Adds localized English and Arabic UI copy and modifies the Tafsir footer layout without changing subscription products or permanent/one-hour ad-free entitlements.
- Adds one compact action above the actual `FragMain` bottom native ad and a distinct home process-session rewarded placement without adding another large promo card.
- Adds an adlib-owned cancelable loading/retry surface and one Remote Config key, `rewarded_interstitial_fallback_admob`; production fallback stays disabled until that rewarded-interstitial ad unit is configured.
