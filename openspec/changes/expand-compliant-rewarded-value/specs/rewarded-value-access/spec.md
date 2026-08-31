## ADDED Requirements

### Requirement: Explicit rewarded-ad consent
The app SHALL show an owned confirmation surface before every rewarded ad with a visible AD/video label, the exact reward, a cancel action, and no misleading claim that the ad is required for core use.

#### Scenario: User accepts reward
- **WHEN** a user explicitly chooses to watch the disclosed rewarded ad
- **THEN** the app SHALL load/show the ad and grant the disclosed entitlement only from the earned-reward callback

#### Scenario: User cancels or ad is unavailable
- **WHEN** the user cancels or the rewarded ad fails to load or show
- **THEN** the app SHALL keep the underlying screen interactive and SHALL NOT block submission, navigation, streaming, reading, or already-free content

### Requirement: Balanced free and rewarded content
The app SHALL provide useful free access while reserving only additive offline or repeat benefits for rewarded ads or subscription.

#### Scenario: Free allowances
- **WHEN** a non-subscriber uses translation, Tafsir, recitation, quiz review, or progress insights
- **THEN** the app SHALL retain one suitable free translation, one authoritative free Tafsir per language, all streaming, the first offline Surah download, basic quiz answers/explanations, and a basic progress summary

#### Scenario: Additional entitlement
- **WHEN** a non-subscriber requests an additional eligible pack, download, retry/skip, or deep insight
- **THEN** the app SHALL offer a clearly described optional rewarded route and SHALL unlock only that described benefit after earning the reward

### Requirement: Subscriber bypass
The app SHALL allow an active subscriber to use rewarded-value features without viewing a rewarded ad.

#### Scenario: Active subscriber selects gated benefit
- **WHEN** an active subscriber selects a rewarded extension
- **THEN** the app SHALL perform the action directly and SHALL NOT present the rewarded confirmation or ad

### Requirement: Rewarded placement analytics
The app SHALL record logical placement and outcome without storing Quran content or sensitive free-form user data.

#### Scenario: Reward lifecycle
- **WHEN** a rewarded prompt is viewed, accepted, cancelled, fails, shows, or earns a reward
- **THEN** the app SHALL record the logical placement and outcome for funnel analysis
