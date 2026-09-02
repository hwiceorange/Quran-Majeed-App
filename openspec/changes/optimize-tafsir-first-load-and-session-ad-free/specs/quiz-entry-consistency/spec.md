## ADDED Requirements

### Requirement: Quran reader opens the canonical Quiz experience
The system SHALL route the Quran reader's contextual Quiz action to the same Learn/Quiz destination used by the main bottom navigation and SHALL NOT launch a second question-screen implementation.

#### Scenario: Reader is opened above MainActivity
- **WHEN** the user taps the reader Quiz action
- **THEN** the reader closes, the existing MainActivity is reused, and `QuranQuestionFragment` is selected with its normal bottom navigation, progress, rewards, and interaction model
- **AND** the reader action uses a localized generic Quran Quiz label rather than promising a separate Surah-filtered experience

#### Scenario: Reader is the task root
- **WHEN** the user taps the reader Quiz action without an existing MainActivity below it
- **THEN** MainActivity is created once and opens the same canonical Learn/Quiz destination

### Requirement: Quiz navigation request is one-shot
The system SHALL consume the Quiz destination request after navigation so recreation or a later unrelated intent cannot reopen Quiz unexpectedly.

#### Scenario: Quiz navigation intent is handled
- **WHEN** MainActivity handles an intent containing the canonical Quiz request
- **THEN** it removes that request before selecting the Quiz destination
