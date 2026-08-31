## Context

Rewarded ads currently exist in Tafsir and quiz flows, while ad suppression and permanent purchases are handled in shared ad/billing code. New placements span several modules and must preserve religious-content access, normal form submissions, navigation, subscription behavior, and Google rewarded-ad disclosure requirements.

## Goals / Non-Goals

**Goals:**

- Centralize reward disclosure, loading, earned-reward handling, analytics, and failure behavior.
- Keep a useful free allowance in every affected feature and let subscribers bypass rewarded gates.
- Suppress all non-reward ad formats for exactly one hour after an earned reward.
- Provide polished, multilingual, RTL-safe owned entry points outside ad containers.
- Derive progress insights only from recorded activity with deterministic copy.

**Non-Goals:**

- Locking Quran reading, streaming, basic Tafsir, quiz answers/explanations, or plan submission.
- Automatically launching rewarded ads or granting rewards before `onUserEarnedReward`.
- Generating Tafsir, fatwa, Quran interpretation, or religious advice with an AI model.
- Changing Google Play subscription products or replacing the existing permanent ad-free purchase.

## Decisions

1. **One app-level rewarded-value coordinator.** Feature screens provide a logical placement, reward description, and earned callback. The coordinator owns the pre-ad confirmation, AD label, loading/error state, analytics, and cancellation. This avoids inconsistent disclosures. Feature-specific ad units and duplicated dialogs were rejected because they create drift and make policy review harder.

2. **Entitlements are additive.** Existing free behavior remains available: one translation suited to the user, one authoritative Tafsir per language, all audio streaming, the first offline Surah download, basic quiz review, and basic recorded-progress summaries. Rewarded access adds one specific extra entitlement; subscription/permanent ad removal retains existing semantics. Locking all content was rejected because it harms trust and retention in a worship product.

3. **Persist earned entitlements locally with narrow keys.** Download unlocks are keyed to content identifiers; temporary ad-free stores a bounded grant timestamp and expiry. Wall-clock anomalies cannot extend a grant beyond one hour from the recorded grant. A server dependency was rejected because offline download access must work without login or network.

4. **Central ad suppression.** `SubscriptionChecker.shouldHideAds` incorporates an active temporary grant, so app-open, interstitial, banner, native, and full-screen native call sites share one decision. Rewarded loading continues to use subscription state rather than temporary suppression so users can deliberately earn another supported reward.

5. **Owned promo UI is separate from ad UI.** The homepage entry and post-interstitial prompt sit outside native-ad containers, have at least 48dp touch targets, use app icons rather than ad creative, and clearly distinguish permanent subscription/purchase from the optional one-hour reward.

6. **Post-interstitial promotion is rate-limited.** A prompt can appear only after an interstitial fully closes, at most once per local day, and never during submission, purchase, reading transition, or another modal. It offers choices but does not launch an ad automatically.

7. **Deterministic progress insights.** Insights use stored goals, completed verses/pages, streak, and quiz outcomes with fixed thresholds and localized templates. Missing data is shown as unavailable; values and religious conclusions are never fabricated.

## Risks / Trade-offs

- [Local preferences can be cleared] → Treat restored free allowances conservatively and never remove already downloaded files solely because entitlement metadata is absent.
- [Reward no-fill could frustrate users] → Keep the original screen usable, show a dismissible localized message, and never perform or block unrelated actions.
- [Promo density could feel intrusive] → Keep the homepage entry compact and owned; cap the post-interstitial prompt to once per day.
- [Shared suppression may miss legacy callers] → Audit every ad format and add focused tests for the central decision and expiry boundaries.
- [Remote ad-unit reuse can collide in cache] → Load only on demand per active screen and record the logical placement independently from the physical ad unit.
- [Translations may overflow or break RTL] → Use resource strings, start/end constraints, flexible wrapping, and test Arabic plus English layouts.

## Migration Plan

1. Add shared entitlement and reward coordination without changing existing feature behavior.
2. Enable each placement independently and run its regression checklist before proceeding.
3. Add central temporary suppression and verify every non-reward format.
4. Add homepage and post-interstitial owned entries last so no entry is exposed before its entitlement works.
5. Roll back individual placements through their logical configuration while retaining free access and the existing billing path.

## Open Questions

- Remote Config can later assign distinct physical rewarded ad units per placement; the initial implementation can retain the current rewarded unit while keeping logical analytics separate.
