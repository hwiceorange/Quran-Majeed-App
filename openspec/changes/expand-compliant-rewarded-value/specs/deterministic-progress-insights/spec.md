## ADDED Requirements

### Requirement: Evidence-based progress summary
The app SHALL calculate progress insights only from persisted user activity and fixed localized rules.

#### Scenario: Sufficient activity data
- **WHEN** stored goals, reading progress, streak, or quiz outcomes are available
- **THEN** the app SHALL display the actual measured values and deterministic guidance tied to those values

#### Scenario: Missing activity data
- **WHEN** a required metric is unavailable
- **THEN** the app SHALL show that the insight is unavailable or omit it and SHALL NOT fabricate a value

### Requirement: No generated religious interpretation
The progress-insights capability MUST NOT generate Tafsir, fatwa, Quran interpretation, doctrinal claims, or personalized religious rulings.

#### Scenario: User opens deep insights
- **WHEN** the deep report is produced
- **THEN** it SHALL be limited to study consistency, completion, revision, and recorded quiz behavior using vetted fixed templates

### Requirement: Submission independence
Learning-plan creation and updates SHALL complete independently of progress-insight monetization.

#### Scenario: Plan submitted
- **WHEN** the user saves or submits a learning plan
- **THEN** the app SHALL complete and acknowledge the save without requiring, launching, or waiting for an ad
