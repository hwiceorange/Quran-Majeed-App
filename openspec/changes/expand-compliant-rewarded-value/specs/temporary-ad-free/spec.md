## ADDED Requirements

### Requirement: One-hour ad-free reward
The app SHALL suppress all non-reward ad formats for no more than one hour after the user earns the disclosed ad-free reward.

#### Scenario: Reward earned
- **WHEN** `onUserEarnedReward` is received for the one-hour ad-free placement
- **THEN** app-open, interstitial, banner, native, and full-screen native ads SHALL be suppressed until the persisted expiry

#### Scenario: Reward expires
- **WHEN** the current time reaches the bounded expiry or the clock is inconsistent with the grant time
- **THEN** the temporary entitlement SHALL be cleared and normal ad eligibility SHALL resume

### Requirement: Persistent owned entry
The homepage SHALL expose a compact ad-removal entry outside advertising containers without covering content or controls.

#### Scenario: User opens ad-removal choices
- **WHEN** a non-subscriber selects the homepage entry
- **THEN** the app SHALL present clearly separated permanent subscription/purchase and optional one-hour rewarded choices

#### Scenario: Existing permanent entitlement
- **WHEN** a subscribed or permanently ad-free user opens the homepage
- **THEN** the app SHALL not advertise a redundant rewarded ad-free action

### Requirement: Rate-limited post-interstitial promotion
The app SHALL only promote the optional one-hour reward after an interstitial has closed and SHALL limit this promotion to once per local day.

#### Scenario: Eligible close
- **WHEN** an interstitial closes, the host is safe to display a dialog, and no prompt was shown that day
- **THEN** the app MAY show an owned choice surface but SHALL NOT launch a rewarded ad automatically

#### Scenario: Ineligible context
- **WHEN** a submission, purchase, reading transition, navigation, or another modal is active
- **THEN** the app SHALL skip the promotion without delaying or changing that interaction
