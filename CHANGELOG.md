# Changelog

All notable changes to Hevyn’t are documented here.

## [1.9.0] - 2026-08-25

### Added

- Added a shared Repeat Last Workout flow for the app and widgets that copies the latest detailed workout into a fresh active session without mutating history.
- Added compact 2×1 and main 4×2 local-first workout widgets with active-set controls and launcher-hosted rest countdowns.
- Added a Room-backed routine library with transactional legacy migration, ordered routine templates, and durable exercise identities.
- Added likely-next routine recommendations and searchable exercise suggestions informed by the routine library and completed history.

### Improved

- Made the main Compose UI available immediately after the existing Android splash while history observation, legacy import, and pending-finish recovery continue asynchronously.
- Rebuilt the shared interface around the Hevynt near-black, thin-outline, restrained-red, Doto-led design system across Home, Workout, Journal, dialogs, recap, and timer surfaces.
- Redesigned active workout logging as a dense instrument panel with a unified stats header, compact collapsed exercises, on-demand notes and rest controls, aligned set columns, one-tap previous-value copy, efficient numeric replacement, and circular completion actions.
- Refined the workout launcher with lightweight routine summaries, clearer folder hierarchy, routine metadata, and separated open, start, and overflow actions.
- Corrected exercise-name editing and notification formatting so legitimate Unicode, punctuation, accents, and emoji are preserved while malformed control text is removed safely.

### Reliability

- Made repeated-workout startup atomic so existing, finishing, or temporarily undecodable active sessions cannot be overwritten.
- Added event-driven widget refreshes backed by the existing Room, active-workout, and timer stores, with bounded receiver work and no polling jobs.
- Kept committed workout mutations authoritative when ancillary widget refreshes fail, preserving the save-before-clear completion contract.
- Moved routine and previous-performance loading away from hot composable paths, retained stable workout-list keys, isolated timer recomposition, and removed unnecessary active-workout layout animations.

### Tests

- Added repeat mapping and race coverage, Unicode notification sanitization tests, routine migration and repository instrumentation, launcher layout coverage, and a four-exercise active-workout interaction fixture.
- Validated the release changes with unit tests, Android instrumentation tests, debug and signed release assembly, lint, installed-app visual inspection, and a ten-exercise scroll profile.

## [1.6] - 2026-07-31

### Added

- Added durable completed-workout history backed by Room, including ordered exercises and sets, decimal performance values, notes, and app-owned progress photos.
- Added a Journal destination with newest-first workout summaries, full workout details, legacy-summary support, and safe photo replacement and removal.
- Added a post-workout recap for reviewing session metrics, saving a note, and attaching an image through Android’s Photo Picker.
- Added a Monday-first Home calendar with daily workout counts, month navigation, monthly training totals, and direct routing to one or multiple completed sessions.
- Added previous-set references that resolve the latest matching exercise before the active workout and preserve original set positions.

### Improved

- Expanded the bottom navigation to Home, Workout, and Journal while refining contrast, typography, icon sizing, dividers, and destructive-action styling across the app.
- Completed workouts now save only finished sets and omit exercises without completed work, keeping history metrics aligned with the performed session.
- Preserved compatibility with legacy workout summaries through a transactional one-time import while retaining the original preferences.

### Reliability

- Made workout finishing a guarded save-and-clear flow: history is committed before the active workout is cleared, failures retain the active session, and cancellation is propagated safely.
- Added a durable finish marker and startup reconciliation so process interruption cannot silently lose an active workout or duplicate a committed session.
- Blocked active-workout mutations during the finish transaction, including notification-driven set completion, to prevent edits from racing with the saved snapshot.
- Made local workout-photo handling tolerant of missing or corrupt files and restricted deletion to validated files inside the app-owned photo directory.

### Privacy

- Excluded the workout-history database and local workout photos from cloud backup while keeping the app free of Internet and broad media permissions.
- Kept workout history, notes, and photos local to the device without adding analytics, authentication, or network image dependencies.

### Tests

- Added unit coverage for completed-workout mapping, calendar boundaries and aggregates, previous-set matching, photo-path safety, and guarded completion behavior.
- Added Room and repository instrumentation coverage for graph ordering, decimal values, legacy import, transaction rollback, persistence, finish markers, and photo lifecycle operations.

## [1.5.1] - 2026-07-23

### Added

- Added persistent routine-builder sessions so interrupted routine creation and editing resumes in the correct folder and updates the intended routine after an app restart.
- Added complete routine exercise templates, preserving exercise IDs, notes, rest durations, and each set’s weight and repetition targets when routines are saved, edited, reloaded, or started.
- Added decimal weight entry and decimal workout-volume calculations, with compact formatting that removes unnecessary trailing zeros.
- Added a final workout-complete notification when the last unfinished set is completed from the rest-timer flow.

### Improved

- Replaced periodic active-workout polling with shared-preference listeners and cached session decoding, making workout and timer changes from the app or notification actions appear immediately with less background work.
- Reworked rest-timer scheduling to resolve the next set from the latest workout state, schedule completion in-app as well as through alarms, and request notification permission only when a rest notification is first needed.
- Made new workout and routine sets blank by default instead of pre-filling sample weights and repetitions.
- Sanitized workout inputs while typing: weights accept one decimal separator, commas are normalized to decimal points, repetitions remain whole numbers, invalid characters are removed, and field lengths are bounded.
- Simplified numeric set fields and replaced extended Material icons with core equivalents while preserving routine, folder, workout, and navigation actions.
- Updated adaptive launcher resources with monochrome icon support and removed obsolete generated icon, color, and starter-test resources.
- Reduced runtime dependencies by removing unused Lifecycle Runtime KTX and Material Icons Extended artifacts.
- Enabled release optimization for smaller optimized APKs.

### Fixed

- Fixed routine edits losing notes, set counts, weights, repetitions, rest durations, or exercise identities after save and reload.
- Fixed restored routine-builder sessions being treated as normal workouts, which could save history instead of updating the selected routine.
- Fixed stale rest-timer target details after sets were edited, removed, or completed through another app component.
- Preserved compatibility with workouts and routines saved by earlier releases while migrating them to the expanded session and template formats.

### Tests

- Added unit coverage for decimal workout volume, weight formatting, malformed completed-set values, and weight/repetition input sanitization.
- Added repository instrumentation coverage for active-workout session persistence, legacy workout compatibility, routine template round trips, and preference-change notifications.
- Removed the generated placeholder unit and instrumentation tests.

## [1.5.0] - 2026-07-23

### Added

- Added month navigation to the workout calendar, with completed training days and the current day clearly marked.
- Added monthly training-day totals and a compact latest-session summary to the Home screen.
- Added a bundled Doto variable display font for important headers, workout metrics, timers, routine names, and primary actions.

### Improved

- Refreshed Home, Workout, routine management, active workout logging, dialogs, bottom navigation, minimized workouts, and rest timers with a cohesive industrial red-on-black design.
- Introduced dotted dividers, compact shapes, custom workout iconography, clearer visual hierarchy, and more consistent typography.
- Redesigned routine folders as expandable groups and made populated routine rows directly startable.
- Streamlined active workout logging with clearer metrics, compact set fields, explicit completion indicators, inline exercise editing, and redesigned rest-duration controls.

### Tests

- Added formatting coverage for routine previews and workout-calendar summaries.

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