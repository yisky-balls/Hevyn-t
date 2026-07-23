# Changelog

All notable changes to Hevyn’t are documented here.

## [1.0.1] - 2026-07-23

### Fixed

- Made set completion atomic so rapid or duplicate completion events cannot complete the wrong set or start duplicate rest timers.
- Kept rest-timer notifications synchronized with the active workout, including `-15s`, `+15s`, skip, expiry, and completed-set removal actions.
- Added per-exercise rest durations and preserved unrelated exercises when editing a timer or deleting a set.
- Corrected next-set progression so unfinished sets in the current exercise are prioritized before moving to another exercise.
- Persisted active-workout changes safely across UI and notification actions, and refreshed the UI when background actions update workout state.
- Allowed the final set of an exercise to be removed without affecting neighboring exercises.
- Preserved routine folders when their final routine is deleted and kept routine edits in their original folder position.
- Displayed routine exercise names correctly instead of their serialized object representation.

### Tests

- Expanded regression coverage for set completion, set removal, workout progression, timer adjustment, and routine-folder operations.

### UI

- Retained the established v1.0.0 visual design; the experimental UI overhaul is not included in this release.

## [1.0.0] - 2026-07-21

- Initial public release.