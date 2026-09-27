# Handoff: Tennis Score for Wear OS

Snapshot: 2026-09-26. Workspace: `/Users/thephani/Workspace/thePhani/gitspace/tennis-scoring-wear-os`.

## Current state

The native Wear OS MVP scores offline singles matches, persists the point log, resumes and undoes points, derives service automatically, and stores completed results. Home Settings offers best of three or five and either a full deciding set or a 10-point match tiebreak. Each active match retains the format selected at its start. The scoring screen uses the selected B score-tap layout: set heading, completed-set strip, current game scores with the server ball, a large center panel split into tappable YOU and OPP point scores, and Undo. Tiebreak labels and the score pulse remain visible. The completed-set winner's game total is bold mint; the scorer's current game score briefly grows when a game is recorded. Individual point values come from `MatchScore.pointValue` so Compose has no scoring rules.

The Play listing title is **Tennis Score for Wear OS**. The permanent Android application ID is `com.thephani.tennisscore`; the Kotlin namespace remains `com.example.tennisscore`. The score screen is responsive on 192 dp and 227 dp round emulators, and its Undo, menu actions, settings choices, and match-complete actions are at least 48 dp high. Menu, settings, and completion pages scroll to reach lower actions.

## Git state

The project is on `main` in the public `thephani/tennis-scoring-wear-os` repository. The privacy policy is published from `docs/` through GitHub Pages. Inspect `git status` before editing and preserve local work.

## Where to work

| Area | Files |
| --- | --- |
| Tennis scoring and formats | `app/src/main/java/com/example/tennisscore/domain/MatchScore.kt`, `MatchFormat.kt`, `ScoringEngine.kt`, `ServeOrder.kt` |
| DataStore, settings snapshot, history | `app/src/main/java/com/example/tennisscore/data/MatchRepository.kt` |
| Navigation and watch UI | `app/src/main/java/com/example/tennisscore/ui/MatchViewModel.kt`, `TennisApp.kt` |
| JVM rule tests | `app/src/test/java/com/example/tennisscore/domain/` |
| Wear interaction and persistence tests | `app/src/androidTest/java/com/example/tennisscore/` |
| Visual options and screenshots | `design-options/` (selected score-tap concept: `tap-score-concepts.svg`, option B) |

## Key decisions to preserve

- Settings are defaults for new matches. A match's format and first server are saved with its point log.
- At 6–6, a full set uses a 7-point tiebreak. If the deciding-set option is a match tiebreak, it starts at 1–1 or 2–2 sets, targets 10 points, and requires a two-point lead.
- The current server is derived from completed games and tiebreak point order. Undo and Resume do not store a separate mutable server position.
- New Match does not replace the active match until first-server selection is completed.
- Legacy unfinished matches without a first-server value ask once and retain their score.
- The app is offline and has no account, backend, phone companion, or cloud backup.

## Build and verification

Use JDK 17 and Android SDK Platform 35. On this host, the cached offline command was:

```bash
GRADLE_USER_HOME="$PWD/.gradle-user" ./gradlew build connectedDebugAndroidTest --offline
```

Start a round Wear OS emulator before `connectedDebugAndroidTest`. Remove `--offline` if dependencies are not yet cached. On 2026-09-26, `build connectedDebugAndroidTest bundleRelease` passed with 36/36 instrumentation tests on `Tennis_Small_Round` (192 dp), and a separate `connectedDebugAndroidTest` passed 36/36 on `Wear_OS_Large_Round` (227 dp). A parallel two-emulator run crashed the small emulator's test process during `tieBreakFlow`; that test and the complete small suite passed when run alone. `jarsigner -verify` reports the release AAB as unsigned. The in-app Privacy screen, [public GitHub Pages policy](https://thephani.github.io/tennis-scoring-wear-os/privacy/), listing copy, and two authentic 454 × 454 Wear OS screenshots are prepared under `docs/privacy/` and `release/`. A physical Samsung watch and release publishing have not been verified. A secure upload key, signed AAB, and Play test release remain.

## Suggested Luna handoff prompt

> Continue the Wear OS Tennis Score app in this repository. Read `AGENTS.md`, `README.md`, and `HANDOFF.md`, then inspect `git status` and preserve unrelated files. Keep the existing scoring, settings, service, Undo, and watch UI behavior. Use the build and test commands in `HANDOFF.md` for changes I request next.
