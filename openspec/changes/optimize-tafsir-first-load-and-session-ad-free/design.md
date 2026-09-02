## Context

`ActivityTafsir` currently calls `TafsirManager.prepare(force = true)` whenever the process-memory model is empty. A process restart therefore bypasses the 3 KB bundled manifest and can serialize a manifest network call with the selected-verse API call. Verse data has memory/file caches, but direct loads and the unused preloader can issue duplicate requests, and page coroutines are not tied to the activity lifecycle.

The Tafsir footer currently contains only the native-ad container. The requested owned action must remain visually and structurally outside the `NativeAdView`, prefer subscription, disclose rewarded advertising before display, and grant only a process-scoped Tafsir-native-ad benefit.

## Goals / Non-Goals

**Goals:**

- Make the bundled/persisted manifest the immediate source on ordinary opens.
- Start the current-verse request before activity navigation and merge duplicate requests.
- Cancel stale page render jobs and warm only adjacent verses after success.
- Report manifest/cache/network/render stages for post-release performance diagnosis.
- Provide a 48dp, RTL-safe owned action next to the Tafsir ad area with subscription-first and rewarded-second states.
- Hide only Tafsir native ads for the current process after a successfully earned reward.
- Keep the visible Tafsir action background compact while retaining its 48dp Android touch target and current text size.
- Attach an equivalent compact action directly to the actual `FragMain` bottom native ad without introducing a second large promo card.

- Give all rewarded placements one cache-first, eight-second request and fallback state machine with cancel/retry behavior.

**Non-Goals:**

- Bundling all Tafsir verse content, changing Tafsir sources, or guaranteeing zero network time on uncached verses.
- Removing app-open, banner, interstitial, or native ads on other pages with the Tafsir session reward.
- Changing subscription products, the one-hour global ad-free reward, permanent ad removal, or premium Tafsir content access.
- Automatically showing rewarded ads on return from subscription.
- Treating a home-only rewarded removal as global ad-free access.

- Granting a promised reward from a standard `InterstitialAd`; only `RewardedAd` or `RewardedInterstitialAd` may complete a promised reward flow.

## Decisions

1. **Local-first manifest with optional explicit refresh.** Ordinary preparation loads the persisted manifest or bundled asset on an IO dispatcher. Only explicit force refreshes wait for the network; failures fall back locally. This removes the unnecessary first network dependency while preserving settings/version refresh behavior.

2. **Single-flight content requests.** `TafsirCacheManager` owns in-flight work keyed by Tafsir/Surah/Ayah. Navigation prefetch and page loading await the same deferred request instead of doubling traffic. The alternative of merely starting the existing preloader would race the page request and increase latency/load.

3. **Targeted warm-up.** `ReaderFactory.startTafsir` starts the selected verse before navigation. After success, only valid previous/next verses are warmed. The existing broad common-chapter preloader is not automatically activated because it can consume bandwidth unrelated to the user's current reading.

4. **Lifecycle-owned rendering.** The activity keeps one content job, cancels it on a new request, and renders only if the requested key and verse are still current. This prevents stale content from winning during rapid previous/next or Tafsir switches.

5. **Process-memory entitlement.** `TafsirSessionAdFreeManager` stores only an atomic in-memory flag and whether the rewarded alternative has been unlocked after a subscription-page return. No timestamp or disk persistence is used, so process death restores the normal subscription-first offer exactly as requested.

6. **Owned footer outside ad content.** A separate Material text action sits above/beside the native container, uses the existing premium vector, a 48dp target, and start/end-aware layout. It is shown only when an ad is actually displayed. Initial action opens Premium; returning without entitlement changes it to `AD · Remove Tafsir ads` and still requires a confirmation dialog before loading the reward.

7. **Earned callback is authoritative.** The process entitlement is activated only by `onUserEarnedReward`. Cancel, no-fill, load timeout, close-before-reward, or show failure leaves the native ad and reading content unchanged.

8. **Native display callback is backward compatible.** The ad helper retains its existing three-argument method and adds a four-argument overload for display-state reporting, avoiding JVM signature changes for Java callers.

9. **Compact visual bounds do not shrink interaction bounds.** The action remains a 48dp `MaterialButton`, while 8dp Android top/bottom background insets reduce the visible background to about 32dp. Text remains 14sp and the existing ripple/semantic button behavior is retained.

10. **Home uses an independent process state.** The actual `FragMain` VOTD-bottom native ad receives the same subscription-first/rewarded-second flow, but its earned state is separate from Tafsir so the benefit copy remains exact. No second large home promo is introduced.

11. **One rewarded request coordinator.** App and Quiz entry points delegate to an adlib coordinator. A cached `RewardedAd` shows immediately; otherwise the coordinator starts or joins the physical rewarded-ID load and polls the cache on the main thread for no longer than eight seconds.

12. **Compliant fallback format only.** The fallback is `RewardedInterstitialAd`, never ordinary `InterstitialAd`, and uses its real earned callback. Debug uses Google's rewarded-interstitial test unit; release resolves `rewarded_interstitial_fallback_admob` and safely reaches the unavailable state while that key is blank.

13. **Cancelable loading and retry.** A dimmed dialog explains that a reward ad is being prepared, keeps a clear cancel path, and switches after the attempt to `No reward video is available` with a 48dp Retry button. Retry starts a new rewarded-first/fallback attempt; outside or Back cancellation never grants value.

14. **Preload around user demand.** Consent completion preloads the shared rewarded physical ID and rewarded-interstitial fallback. Successful consumption, close, and show failure replenish their respective one-item caches. `needLoadAd` remains the single-flight guard, while polling lets a second caller observe an in-progress load without issuing a duplicate request.

15. **Manifest-gated Tafsir navigation prefetch.** `TafsirPreloader` joins `TafsirManager.prepare(false)` before resolving the saved/default key and starting the selected verse. This preserves pre-navigation warming while preventing slug resolution against an empty process model on a new-user/cold-process open.

16. **Completed failures never remain reusable.** Tafsir single-flight entries own a completion hook that removes the exact deferred on success, failure, or cancellation before a later foreground caller can join it. A bounded second network attempt handles cold DNS/TLS and retryable server failures in the same page visit.

## Risks / Trade-offs

- **Uncached content still depends on network quality** → Show stable loading/retry feedback, start the request before navigation, and expose stage timings.
- **A native-ad callback can return after the session reward** → Recheck all suppression gates in the callback and immediately clear/hide late results.
- **Subscription state may update shortly after resume** → Recheck centralized ad suppression on every resume and hide the footer as soon as entitlement is visible.
- **Process-scoped reward is intentionally not durable** → Keep it in memory and use copy that says it lasts until the app is closed.
- **Adjacent prefetch uses additional data** → Limit to at most two valid adjacent verses and never block rendering on it.
- **Fallback production ID is not yet configured** → Keep the default blank, document the Remote Config key, and fail safely instead of sending a rewarded-interstitial request through an incompatible standard-interstitial unit.
- **A cold-network retry adds a short wait on genuine outages** → Retry only once with a small backoff, then expose the existing page Retry/no-internet state without blocking navigation indefinitely.
