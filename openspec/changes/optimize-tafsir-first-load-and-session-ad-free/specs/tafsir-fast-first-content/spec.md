## ADDED Requirements

### Requirement: Tafsir manifest is available without an ordinary-open network dependency
The system SHALL load a valid persisted Tafsir manifest or the bundled manifest before using the network during an ordinary Tafsir open, and SHALL reserve blocking network refresh for an explicit force-refresh request.

#### Scenario: First process open with no persisted manifest
- **WHEN** Tafsir is opened and the process model and persisted manifest are absent
- **THEN** the system parses the bundled manifest without waiting for a manifest network request

#### Scenario: Explicit refresh fails
- **WHEN** an explicit manifest refresh fails
- **THEN** the system falls back to the persisted or bundled manifest and keeps Tafsir selection usable

### Requirement: Verse requests are cached and coalesced
The system SHALL check memory and file caches before the network and SHALL share one in-flight network request among callers for the same Tafsir, Surah, and Ayah.

#### Scenario: Navigation prefetch overlaps page load
- **WHEN** prefetch and the Tafsir activity request the same uncached verse concurrently
- **THEN** both callers receive the result of one network request

### Requirement: Targeted prefetch reduces perceived wait
The system SHALL begin prefetching the selected verse before opening Tafsir and SHALL warm only valid adjacent verses after the selected content succeeds.

#### Scenario: User opens Tafsir from a verse action
- **WHEN** the user taps the Tafsir action for a verse with a configured Tafsir key
- **THEN** the system starts that verse request before starting the Tafsir activity

#### Scenario: Current verse finishes
- **WHEN** the selected Tafsir content loads successfully
- **THEN** the system asynchronously warms at most the valid previous and next verses without delaying render

### Requirement: Rendering is lifecycle safe and measurable
The system SHALL cancel superseded page jobs, SHALL ignore stale results, and SHALL record manifest, cache source, network, render, and total-ready timing stages.

#### Scenario: User changes verse during a slow request
- **WHEN** a previous request finishes after a newer Tafsir/verse request has started
- **THEN** the stale result is not rendered over the current selection

#### Scenario: Content is uncached
- **WHEN** the selected verse completes through the network path
- **THEN** diagnostics identify the network source and elapsed time separately from WebView render completion

### Requirement: First navigation cannot be poisoned by premature prefetch
The system SHALL prepare the local Tafsir manifest before resolving metadata for navigation prefetch, and SHALL NOT reuse a completed failed in-flight request for the foreground page.

#### Scenario: New user opens Tafsir with a saved key in a cold process
- **WHEN** navigation prefetch starts before the process Tafsir model has been populated
- **THEN** the prefetch joins local manifest preparation, resolves a valid slug, and the first foreground page shares the valid request

#### Scenario: An earlier prefetch has already failed
- **WHEN** a foreground page requests the same Tafsir/Surah/Ayah after that failed deferred completed
- **THEN** the completed failure has already been evicted and the page starts or joins a fresh request

#### Scenario: First network attempt is transiently unavailable
- **WHEN** the first uncached Tafsir request fails with a retryable connection or server error
- **THEN** the same page performs one bounded retry before showing the existing failure state
