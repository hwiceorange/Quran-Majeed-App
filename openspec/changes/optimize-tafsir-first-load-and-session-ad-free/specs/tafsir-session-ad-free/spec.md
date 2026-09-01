## ADDED Requirements

### Requirement: Tafsir ad-removal entry is subscription first
The system SHALL show a distinct owned Premium text action outside the native-ad view when a Tafsir native ad is displayed and the user has no ad-removal entitlement.

#### Scenario: Eligible user sees a native ad
- **WHEN** the Tafsir native ad is successfully displayed and no suppression entitlement is active
- **THEN** a 48dp, RTL-safe `Remove ads · VIP` action opens the existing compliant subscription page

### Requirement: Rewarded alternative appears only after subscription return
The system SHALL change the owned action to an explicit `AD` rewarded alternative only after the user returns from the subscription page without an ad-removal entitlement, and SHALL never automatically show the rewarded ad.

#### Scenario: User returns without subscribing
- **WHEN** the user opened Premium from the Tafsir footer, left the subscription page, and returns without entitlement
- **THEN** the footer action becomes `AD · Remove Tafsir ads` and remains user initiated

#### Scenario: User subscribes
- **WHEN** the user returns with subscription or another centralized ad-free entitlement
- **THEN** the Tafsir native ad and owned action are hidden

### Requirement: Earned reward hides Tafsir native ads for the current process
The system SHALL activate a non-persistent process entitlement only after the earned-reward callback and SHALL apply it to current and subsequently opened Tafsir pages in the same process.

#### Scenario: Reward is earned
- **WHEN** the user confirms the reward, the ad displays, and `onUserEarnedReward` fires
- **THEN** the current Tafsir native ad and owned action are removed and later Tafsir pages in the process do not request native ads

#### Scenario: Reward is not earned
- **WHEN** the user cancels, the ad has no fill, times out, fails to show, or closes before reward
- **THEN** no process entitlement is granted and Tafsir reading/navigation remain usable

#### Scenario: App process restarts
- **WHEN** the process ends after a Tafsir session reward and the app starts a new process
- **THEN** the reward state is cleared and the subscription-first Tafsir footer behavior resumes
