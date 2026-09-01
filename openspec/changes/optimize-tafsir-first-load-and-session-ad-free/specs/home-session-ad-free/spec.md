## ADDED Requirements

### Requirement: Home native ad-removal entry mirrors the subscription-first flow
The system SHALL show one compact owned action directly above the actual `FragMain` bottom home native ad only after that ad is displayed, without introducing a second large home promo card.

#### Scenario: Bottom home native ad is displayed
- **WHEN** the native ad is successfully attached and no home or centralized ad-removal entitlement is active
- **THEN** a 48dp RTL-safe `Remove ads · VIP` action opens the existing subscription page

#### Scenario: Native ad is unavailable
- **WHEN** the bottom home native ad is suppressed, fails, or has no fill
- **THEN** the owned action and its footer are hidden without changing the rest of the home layout

### Requirement: Home rewarded alternative is explicit and earned-only
The system SHALL reveal an explicit `AD` alternative only after return from subscription without entitlement, and SHALL hide the bottom home native ad for the current process only after the earned callback.

#### Scenario: User returns without subscribing
- **WHEN** the user returns from the home footer subscription entry without an ad-removal entitlement
- **THEN** the action becomes `AD · Remove home ad` and no ad starts until the user taps and confirms

#### Scenario: Reward succeeds
- **WHEN** `onUserEarnedReward` fires for the home session placement
- **THEN** the bottom home native ad and owned action stay hidden on subsequent home fragment instances in the same process

#### Scenario: Reward does not succeed
- **WHEN** the user cancels, there is no fill, loading times out, showing fails, or the ad closes before reward
- **THEN** no home session entitlement is granted and home interaction remains usable

### Requirement: Compact visual height preserves accessibility
The system SHALL reduce the visible background height of both Tafsir and home text actions without reducing the text size or Android touch target.

#### Scenario: Compact action is rendered
- **WHEN** either owned action is visible
- **THEN** its text remains 14sp, its semantic button bounds remain at least 48dp high, and its visible background is vertically inset
