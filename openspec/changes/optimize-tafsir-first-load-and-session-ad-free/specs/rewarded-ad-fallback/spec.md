## ADDED Requirements

### Requirement: Rewarded requests are cache-first and bounded
The system SHALL route every rewarded placement through one coordinator, SHALL show a valid cached rewarded ad immediately, and otherwise SHALL observe or load the rewarded cache for no longer than eight seconds.

#### Scenario: Rewarded cache is ready
- **WHEN** a user taps a disclosed rewarded action and a valid `RewardedAd` is cached
- **THEN** the ad is shown immediately without opening the loading surface

#### Scenario: Rewarded ad arrives during loading
- **WHEN** no rewarded ad is initially cached but one becomes valid before the eight-second deadline
- **THEN** the loading surface closes and that rewarded ad is shown on the next polling pass

### Requirement: Fallback remains a rewarding Google ad format
The system SHALL attempt a cached `RewardedInterstitialAd` after the rewarded deadline and SHALL NOT grant rewards from a standard `InterstitialAd`.

#### Scenario: Rewarded-interstitial fallback is cached
- **WHEN** the rewarded deadline expires without a rewarded ad and a valid rewarded-interstitial fallback is cached
- **THEN** the fallback is shown and value is delivered only from its earned callback

#### Scenario: Production fallback ID is absent
- **WHEN** release configuration has no rewarded-interstitial ad unit ID
- **THEN** no incompatible request is sent and the retryable unavailable state is shown

### Requirement: Unavailable state is retryable and dismissible
The system SHALL replace loading with a clear no-reward-video message and Retry control when neither rewarding format is cached at the deadline.

#### Scenario: User retries
- **WHEN** the unavailable state is visible and the user taps Retry
- **THEN** a fresh rewarded-first eight-second attempt begins and the fallback cache is prepared again

#### Scenario: User dismisses
- **WHEN** the user taps outside the dialog or presses Back during loading or unavailable states
- **THEN** the request is canceled, no reward is granted, and the underlying feature remains usable

### Requirement: Reward caches are proactively governed
The system SHALL preload the shared rewarded physical ID and rewarded-interstitial fallback only after consent and foreground availability, SHALL avoid duplicate in-flight loads for the same physical ID, and SHALL replenish consumed caches.

#### Scenario: Consent becomes available
- **WHEN** ad consent completes while an Activity is available
- **THEN** one rewarded ad and one configured rewarded-interstitial fallback are requested for their caches

#### Scenario: Rewarding ad is consumed
- **WHEN** either rewarding format is shown, closed, or fails to show
- **THEN** its cache is cleared and a replacement load is requested without blocking the reward or business callback
