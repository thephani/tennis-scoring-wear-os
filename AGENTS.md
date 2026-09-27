# Project instructions: Tennis Score for Wear OS

Read `README.md` for setup and `HANDOFF.md` for the current project snapshot before editing. Inspect `git status` first and preserve staged, unstaged, and untracked work.

## Product and architecture

- This is a native, offline Wear OS singles scoring app in Kotlin and Jetpack Compose. Keep the scoring targets usable on a round watch.
- Keep tennis rules in `app/src/main/java/com/example/tennisscore/domain/`. `ScoringEngine` replays an ordered `Y`/`O` point log; `ServeOrder` derives the server from the first server and score. Do not make the UI a second source of truth for scoring or service.
- `MatchRepository` owns DataStore persistence. Saved settings are defaults for future matches; each active match keeps the format captured when it began. Undo must restore score and service, and undoing a winning point must remove that result from history.
- Preserve compatibility with older point logs that have no format or first-server value. They default to best of three with a full deciding set and ask for the first server once when resumed.
- Starting or replacing a match takes effect only after the user chooses the first server. Cancel must leave the existing match intact.
- Normal sets use a 7-point tiebreak at 6–6. The optional 10-point match tiebreak replaces the deciding set at 1–1 or 2–2; both tiebreaks require a two-point lead.

## UI behavior

- Use the selected B score-tap layout: set header, completed-set score strip, current game scores with the server ball, a large center point panel split into tappable YOU / OPP halves, and Undo below.
- The center point panel pulses when a point is recorded. The scorer's current game score briefly grows when a game is recorded. Completed set scores remain visible in the strip, with the winner's game total in heavier mint type.
- A normal 6–6 tiebreak shows `TIEBREAK` and `TB` points. A deciding match tiebreak shows `MATCH TIEBREAK` and `MTB` points.
- Settings live on Home and Resume. Saving changes future-match defaults only; Cancel discards edits. Keep the page scrollable and its controls reachable on a round display.

## Working practice

- Make focused changes and check existing tests and callers before editing. Preserve unrelated files and avoid new dependencies unless necessary.
- Run targeted JVM tests for scoring changes, UI/emulator tests for interaction changes, then `./gradlew build` and `./gradlew connectedDebugAndroidTest` before claiming completion. Report exactly what ran and any emulator or hardware limitation.
- Do not push, deploy, publish, or erase match data without explicit user authorization.
