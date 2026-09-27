# Tennis Score for Wear OS

Offline singles scoring for best-of-three or best-of-five tennis. On **Home → Settings**, choose a full deciding set or a 10-point match tiebreak in place of that set. The default format applies to new matches; an active match keeps its format. Choose the first server, then the tennis ball tracks service automatically, including tiebreaks. Tap the **YOU** half of the large point score when you win a point, or the **OPP** half when your opponent wins it. The score screen shows completed set scores above the current game score, with the winning set total emphasized. The score panel pulses after a point; the scorer's current game score briefly grows when a game is recorded. **UNDO** removes the latest point.

## Implementation

- `domain/ScoringEngine.kt` is a pure state transition: point winner → next match score. It handles games, deuce, advantage, 7-point tiebreaks at 6–6, best-of-three/five sets, and a 10-point deciding match tiebreak (win by two).
- `data/MatchRepository.kt` saves an ordered point log in Preferences DataStore. Each point and a completed history result are saved in one transaction. Undo removes the final event, so replay restores the entire previous score. Undoing a match-winning point also removes that result from history.
- Settings are saved separately from the active match; a new match snapshots them. Matches created before the settings change retain best-of-three/full-set rules.
- The first server is saved with the active match. Service is derived from completed games and tiebreak points, so it remains correct after Undo and Resume. Older unfinished matches ask for their first server once without resetting the score.
- `ui/MatchViewModel.kt` owns start, resume, score, undo, and navigation state. `ui/TennisApp.kt` draws the watch screens; no scoring rules live in Compose.
- No network permission, backend, account, phone app, or automatic cloud backup.
- Home and Resume include a scrollable Privacy screen. The public policy draft and Play release steps are in [`release/README.md`](release/README.md).

The Gradle configuration targets API 35 and uses Wear Compose Material 3 1.6.2.

## Build and test

Open this folder in Android Studio with JDK 17 or newer and install Android SDK Platform 35. Then run:

```bash
./gradlew build
```

The scoring and history unit tests run on the JVM. Watch interaction and persistence tests require an Android device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

## Wear OS emulator

1. In Android Studio's Device Manager, create a **Wear OS** virtual device. Choose a round watch and an API 35 or newer Wear OS system image.
2. Start the watch emulator, select the `app` run configuration, and press **Run**. Alternatively, run `./gradlew installDebug` while the emulator is connected.
3. Open **Tennis Score**, optionally set a format in **SETTINGS**, tap **START MATCH**, choose who serves first, then tap the **YOU** or **OPP** half of the large score panel to award a point. Completed set scores stay visible above the current game's score. Tap **UNDO** to reverse the last point. At 6–6, the screen shows **TIEBREAK** and **TB POINTS**. In a deciding match tiebreak, it shows **MATCH TIEBREAK** and **MTB POINTS**.
4. Close and reopen the app to check **RESUME MATCH?**. Complete the required number of sets to see the result, then use **HOME → HISTORY**.

## Project handoff

Read [AGENTS.md](AGENTS.md) for project-specific development rules and [HANDOFF.md](HANDOFF.md) for the project snapshot and test evidence.

## Physical watch

Enable Developer options and Wireless debugging on the watch. Pair and connect it using the IP addresses and ports shown on the watch:

```bash
adb pair WATCH_IP:PAIRING_PORT
adb connect WATCH_IP:DEBUG_PORT
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.thephani.tennisscore/com.example.tennisscore.MainActivity
```

On watches with USB debugging, connect by USB and use the same `adb install` and `adb shell am start` commands. `adb devices` should show only the intended target, or pass its serial with `adb -s SERIAL`.

## MVP limits

Singles only. The app tracks whose turn it is to serve, but not first/second serves, faults, or serve statistics. No names, doubles, phone companion, sharing, or cloud backup. History stores date, winner, set scores, and any deciding match tiebreak score; it does not retain a point-by-point archive after a new match starts. Swipe left is reserved by Wear OS navigation, so Undo is an explicit on-screen action.
