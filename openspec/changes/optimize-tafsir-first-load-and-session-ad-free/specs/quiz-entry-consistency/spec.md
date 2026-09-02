## ADDED Requirements

### Requirement: Quran reader opens the canonical Quiz experience
The system SHALL route the Quran reader's contextual Quiz action to the same Learn/Quiz destination used by the main bottom navigation and SHALL NOT launch a second question-screen implementation.

#### Scenario: Reader is opened above MainActivity
- **WHEN** the user taps the reader Quiz action
- **THEN** the reader closes, the existing MainActivity is reused, and `QuranQuestionFragment` is selected with its normal bottom navigation, progress, rewards, and interaction model
- **AND** the reader passes the current Surah as a one-shot context and every question in that Quiz session belongs to that Surah
- **AND** the reader action uses a localized Quran Quiz label

#### Scenario: Reader is the task root
- **WHEN** the user taps the reader Quiz action without an existing MainActivity below it
- **THEN** MainActivity is created once and opens the same canonical Learn/Quiz destination

#### Scenario: Reader itself starts in a cold process
- **WHEN** an exported reading action opens ActivityReader before any app Activity has installed a window decor
- **THEN** system-bar appearance setup is deferred until the reader layout is bound
- **AND** the reader does not crash while creating the window content container

### Requirement: Quiz navigation request is one-shot
The system SHALL consume the Quiz destination request after navigation so recreation or a later unrelated intent cannot reopen Quiz unexpectedly.

#### Scenario: Quiz navigation intent is handled
- **WHEN** MainActivity handles an intent containing the canonical Quiz request
- **THEN** it removes that request before selecting the Quiz destination

### Requirement: Reader Quiz entry reflects ready chapter content
The system SHALL show the reader Quiz entry only after the current Surah is known and the localized question bank contains at least three questions for that Surah. A first-install extraction race SHALL NOT permanently hide the entry.

#### Scenario: Quiz files are still extracting
- **WHEN** the reader checks a supported Surah before the deferred application preload has completed
- **THEN** the question repository performs one synchronized readiness extraction and recounts the Surah
- **AND** the entry becomes visible when at least three matching questions are available

#### Scenario: Reader changes Surah
- **WHEN** the reader's current Surah changes
- **THEN** the entry is recounted for the new Surah and any stale callback from the previous Surah is ignored

#### Scenario: Surah has fewer than three questions
- **WHEN** the current localized question bank has fewer than three questions for that Surah
- **THEN** the Quiz entry remains hidden rather than opening an incomplete or cross-Surah round
