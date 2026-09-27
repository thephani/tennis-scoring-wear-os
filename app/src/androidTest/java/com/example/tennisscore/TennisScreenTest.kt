package com.example.tennisscore

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tennisscore.data.SavedMatch
import com.example.tennisscore.domain.Player
import com.example.tennisscore.domain.MatchFormat
import com.example.tennisscore.domain.MatchLength
import com.example.tennisscore.domain.DecidingSet
import com.example.tennisscore.domain.ScoringEngine
import com.example.tennisscore.ui.Page
import com.example.tennisscore.ui.TennisScreen
import com.example.tennisscore.ui.completedSetSummaryText
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import kotlin.math.hypot

@RunWith(AndroidJUnit4::class)
class TennisScreenTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var events: MutableState<String>
    private lateinit var firstServer: MutableState<Player?>
    private lateinit var activeFormat: MutableState<MatchFormat>
    private lateinit var defaultFormat: MutableState<MatchFormat>

    private fun launch(initial: String? = null, initialPage: Page = Page.START,
        server: Player? = if (initialPage == Page.SCORE) Player.YOU else null,
        format: MatchFormat = MatchFormat()) {
        events = mutableStateOf(initial.orEmpty())
        firstServer = mutableStateOf(server)
        activeFormat = mutableStateOf(format)
        defaultFormat = mutableStateOf(format)
        val page = mutableStateOf(initialPage)
        rule.setContent {
            TennisScreen(
                page = page.value,
                saved = SavedMatch(if (initial == null && page.value == Page.START) null else events.value,
                    0L, emptyList(), firstServer.value,
                        activeFormat.value, defaultFormat.value),
                onStart = { selected ->
                    events.value = ""; firstServer.value = selected
                    activeFormat.value = defaultFormat.value; page.value = Page.SCORE
                },
                onSetFirstServer = { selected ->
                    firstServer.value = selected; page.value = Page.SCORE
                },
                onScore = { winner ->
                    events.value += winner.code
                    if (ScoringEngine.replay(events.value, activeFormat.value).winner != null) page.value = Page.COMPLETE
                },
                onUndo = { events.value = events.value.dropLast(1); page.value = Page.SCORE },
                onShow = { page.value = it },
                onSaveSettings = { format -> defaultFormat.value = format; page.value = if (initialPage == Page.RESUME) Page.RESUME else Page.START },
                onExitSettings = { page.value = if (initialPage == Page.RESUME) Page.RESUME else Page.START },
                onExitPrivacy = { page.value = if (initialPage == Page.RESUME) Page.RESUME else Page.START },
            )
        }
    }

    @Test fun settingsSaveAppliesToNextMatch() {
        launch()
        rule.onNodeWithContentDescription("SETTINGS").performClick()
        rule.onNodeWithText("Singles").assertExists()
        rule.onNodeWithContentDescription("Best of 5").performClick()
        rule.onNodeWithContentDescription("10-point match tiebreak").performScrollTo().performClick()
        rule.onNodeWithContentDescription("SAVE SETTINGS").performScrollTo().performClick()
        rule.onNodeWithText("Best of 5 · Match TB").assertExists()
        rule.onNodeWithContentDescription("START MATCH").performClick()
        rule.onNodeWithContentDescription("YOU SERVE FIRST").performClick()
        rule.onNodeWithText("SET 1 OF 5").assertExists()
    }

    @Test fun settingsCancelDiscardsChanges() {
        launch()
        rule.onNodeWithContentDescription("SETTINGS").performClick()
        rule.onNodeWithContentDescription("Best of 5").performClick()
        rule.onNodeWithContentDescription("CANCEL").performScrollTo().performClick()
        rule.onNodeWithText("Best of 3 · Full set").assertExists()
    }

    @Test fun privacyIsReachableFromHomeAndReturnsHome() {
        launch()
        rule.onNodeWithContentDescription("PRIVACY").performScrollTo().performClick()
        rule.onNodeWithText("Match scores, settings, and history stay on this watch.").assertExists()
        rule.onNodeWithText("thephani.old@gmail.com").assertExists()
        rule.onNodeWithContentDescription("BACK").performScrollTo().performClick()
        rule.onNodeWithContentDescription("START MATCH").assertExists()
    }

    @Test fun privacyIsReachableFromResumeAndReturnsResume() {
        launch(initial = "Y", initialPage = Page.RESUME, server = Player.YOU)
        rule.onNodeWithContentDescription("PRIVACY").performScrollTo().performClick()
        rule.onNodeWithContentDescription("BACK").performScrollTo().performClick()
        rule.onNodeWithContentDescription("RESUME").assertExists()
    }

    @Test fun decidingMatchTiebreakIsVisibleAtOneSetEach() {
        launch(initial = "Y".repeat(24) + "O".repeat(24), initialPage = Page.SCORE,
            format = MatchFormat(decidingSet = DecidingSet.MATCH_TIEBREAK_10))
        rule.onNodeWithText("MATCH TIEBREAK").assertExists()
        assertPoints("0", "0")
        point(Player.YOU)
        assertPoints("1", "0")
    }

    @Test fun resumeSettingsDoNotChangeActiveFormat() {
        launch(initial = "Y", initialPage = Page.RESUME, server = Player.YOU)
        rule.onNodeWithContentDescription("SETTINGS").performClick()
        rule.onNodeWithContentDescription("Best of 5").performClick()
        rule.onNodeWithContentDescription("SAVE SETTINGS").performScrollTo().performClick()
        rule.onNodeWithContentDescription("RESUME").performClick()
        rule.onNodeWithText("SET 1 OF 3").assertExists()
    }

    private fun point(player: Player) {
        val label = if (player == Player.YOU) "Score point for You" else "Score point for Opponent"
        rule.onNodeWithContentDescription(label).performClick()
    }

    private fun assertPoints(you: String, opponent: String) {
        rule.onNodeWithTag("score:you-point", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SemanticsProperties.Text,
            listOf(androidx.compose.ui.text.AnnotatedString(you))))
        rule.onNodeWithTag("score:opponent-point", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(SemanticsProperties.Text,
            listOf(androidx.compose.ui.text.AnnotatedString(opponent))))
    }

    @Test fun startMatch() {
        launch()
        rule.onNodeWithContentDescription("START MATCH").performClick()
        rule.onNodeWithContentDescription("YOU SERVE FIRST").performClick()
        rule.onNodeWithContentDescription("Score point for You").assertExists()
        rule.onNodeWithContentDescription("You serving").assertExists()
    }

    @Test fun choosingOpponentFirstPlacesBallByOpponent() {
        launch()
        rule.onNodeWithContentDescription("START MATCH").performClick()
        rule.onNodeWithContentDescription("OPP SERVES FIRST").performClick()
        rule.onNodeWithContentDescription("Opponent serving").assertExists()
    }

    @Test fun serveBallChangesAfterGameAndUndo() {
        launch(initialPage = Page.SCORE, server = Player.YOU)
        rule.onNodeWithContentDescription("You serving").assertExists()
        repeat(4) { point(Player.OPPONENT) }
        rule.onNodeWithContentDescription("Opponent serving").assertExists()
        rule.onNodeWithContentDescription("Undo last point").performClick()
        rule.onNodeWithContentDescription("You serving").assertExists()
    }

    @Test fun legacyResumeSelectsServerWithoutClearingScore() {
        launch(initial = "Y", initialPage = Page.RESUME, server = null)
        rule.onNodeWithContentDescription("RESUME").performClick()
        rule.onNodeWithContentDescription("OPP SERVES FIRST").performClick()
        assertPoints("15", "0")
        rule.onNodeWithContentDescription("Opponent serving").assertExists()
    }

    @Test fun scorePageWithoutFirstServerRequestsItBeforeScoring() {
        launch(initial = "Y", initialPage = Page.SCORE, server = null)
        rule.onNodeWithContentDescription("Score point for You").assertDoesNotExist()
        rule.onNodeWithContentDescription("YOU SERVE FIRST").performClick()
        assertPoints("15", "0")
    }

    @Test fun cancelServerChoiceKeepsCurrentMatch() {
        launch(initial = "Y", initialPage = Page.RESUME, server = Player.YOU)
        rule.onNodeWithContentDescription("NEW MATCH").performClick()
        rule.onNodeWithContentDescription("REPLACE MATCH").performClick()
        rule.onNodeWithContentDescription("CANCEL").performClick()
        rule.onNodeWithContentDescription("RESUME").performClick()
        assertPoints("15", "0")
    }

    @Test fun tieBreakIsVisibleAndServerMovesAfterFirstPoint() {
        launch(initial = ("YYYYOOOO").repeat(6), initialPage = Page.SCORE, server = Player.YOU)
        rule.onNodeWithText("TIEBREAK").assertExists()
        assertPoints("0", "0")
        rule.onNodeWithContentDescription("You serving").assertExists()
        point(Player.YOU)
        assertPoints("1", "0")
        rule.onNodeWithContentDescription("Opponent serving").assertExists()
    }

    @Test fun scorePointsForBothPlayers() {
        launch(initialPage = Page.SCORE)
        point(Player.YOU)
        point(Player.OPPONENT)
        assertPoints("15", "15")
    }

    @Test fun undoRestoresPreviousPoint() {
        launch(initialPage = Page.SCORE)
        point(Player.YOU)
        rule.onNodeWithContentDescription("Undo last point").performClick()
        assertPoints("0", "0")
    }

    @Test fun completeGame() {
        launch(initialPage = Page.SCORE)
        repeat(4) { point(Player.YOU) }
        rule.onNodeWithText("YOU 1").assertExists()
        rule.onNodeWithText("OPP 0").assertExists()
    }

    @Test fun completeSet() {
        launch(initialPage = Page.SCORE)
        repeat(24) { point(Player.YOU) }
        rule.onNodeWithText("SET 1 · 6–0").assertExists()
        rule.onNodeWithText("YOU 0").assertExists()
        rule.onNodeWithText("OPP 0").assertExists()
    }

    @Test fun finishedSetScoreIsVisuallyEmphasized() {
        launch(initial = "Y".repeat(24), initialPage = Page.SCORE)
        rule.onNodeWithText("SET 1 · 6–0").assertExists()
        val scoreText = completedSetSummaryText(ScoringEngine.replay("Y".repeat(24)))
        assertTrue("Finished set must have heavier type", scoreText.spanStyles.any { span ->
            scoreText.text.substring(span.start, span.end) == "6" &&
                span.item.fontWeight == FontWeight.ExtraBold
        })
        assertTrue("The non-winning games figure stays secondary", scoreText.spanStyles.none { span ->
            scoreText.text.substring(span.start, span.end) == "0" &&
                span.item.fontWeight == FontWeight.ExtraBold
        })
    }

    @Test fun bestOfFiveKeepsCompletedSetScoresVisible() {
        launch(initial = "Y".repeat(48), initialPage = Page.SCORE,
            format = MatchFormat(length = MatchLength.BEST_OF_5))
        rule.onNodeWithText("S1 6–0", substring = true).assertExists()
        rule.onNodeWithText("S2 6–0", substring = true).assertExists()
        rule.onNodeWithText("YOU 0").assertExists()
        rule.onNodeWithText("OPP 0").assertExists()
    }

    @Test fun recordingGamePulsesTopScoreForScorer() {
        launch(initial = "Y".repeat(3), initialPage = Page.SCORE)
        rule.mainClock.autoAdvance = false
        point(Player.YOU)
        rule.mainClock.advanceTimeBy(80)
        rule.onNodeWithText("YOU 1").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Game recorded for You")
        )
        rule.onNodeWithText("OPP 0").assertExists()
        rule.mainClock.autoAdvance = true
    }

    @Test fun tieBreakFlow() {
        launch(initialPage = Page.SCORE)
        repeat(6) {
            repeat(4) { point(Player.YOU) }
            repeat(4) { point(Player.OPPONENT) }
        }
        rule.onNodeWithText("TIEBREAK").assertExists()
        assertPoints("0", "0")
        repeat(7) { point(Player.YOU) }
        rule.onNodeWithText("SET 1 · 7–6").assertExists()
        rule.onNodeWithText("YOU 0").assertExists()
    }

    @Test fun completeMatch() {
        launch(initialPage = Page.SCORE)
        repeat(48) { point(Player.YOU) }
        rule.onNodeWithText("MATCH COMPLETE").assertExists()
        rule.onNodeWithText("YOU WIN").assertExists()
    }

    @Test fun resumePersistedMatch() {
        launch(initial = "Y", initialPage = Page.RESUME, server = Player.YOU)
        rule.onNodeWithContentDescription("RESUME").performClick()
        assertPoints("15", "0")
    }

    @Test fun undoMatchCompletionReturnsToScoring() {
        launch(initial = "Y".repeat(48), initialPage = Page.COMPLETE, server = Player.YOU)
        rule.onNodeWithContentDescription("UNDO LAST POINT").performClick()
        rule.onNodeWithContentDescription("Score point for You").assertExists()
    }

    @Test fun scoreScreenShowsCurrentSetAndGameTogether() {
        launch(initial = "Y".repeat(24) + "O".repeat(4), initialPage = Page.SCORE)
        rule.onNodeWithText("SET 2 OF 3").assertExists()
        rule.onNodeWithText("SET 1 · 6–0").assertExists()
        rule.onNodeWithText("YOU 0").assertExists()
        rule.onNodeWithText("OPP 1").assertExists()
        assertPoints("0", "0")
    }

    @Test fun secondSetShowsCompletedSetScoreAboveCurrentGameScores() {
        val firstSet = ("Y".repeat(4) + "O".repeat(4)).repeat(4) + "Y".repeat(8)
        val secondSet = ("Y".repeat(4) + "O".repeat(4)).repeat(2) + "Y".repeat(6)
        launch(initial = firstSet + secondSet, initialPage = Page.SCORE)

        rule.onNodeWithText("SET 2 OF 3").assertExists()
        rule.onNodeWithText("SET 1 · 6–4").assertExists()
        rule.onNodeWithText("YOU 3").assertExists()
        rule.onNodeWithText("OPP 2").assertExists()
        assertPoints("30", "0")
    }

    @Test fun scoringSectionsHaveBreathingRoomOnRoundWatch() {
        launch(initial = "Y".repeat(24) + "O".repeat(4), initialPage = Page.SCORE)

        val players = rule.onNodeWithTag("score:players").fetchSemanticsNode().boundsInRoot
        val pointScore = rule.onNodeWithTag("score:point-panel").fetchSemanticsNode().boundsInRoot
        val youTarget = rule.onNodeWithTag("score:you-target").fetchSemanticsNode().boundsInRoot
        val opponentTarget = rule.onNodeWithTag("score:opponent-target").fetchSemanticsNode().boundsInRoot
        val undo = rule.onNodeWithTag("score:undo").fetchSemanticsNode().boundsInRoot
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val minGap = root.height * 0.03f

        assertTrue("Player row gap ${pointScore.top - players.bottom}px must be at least ${minGap}px; root=$root players=$players panel=$pointScore undo=$undo",
            pointScore.top - players.bottom >= minGap)
        assertTrue("You score must fill the left half", youTarget.left >= pointScore.left &&
            youTarget.right <= pointScore.center.x)
        assertTrue("Opponent score must fill the right half", opponentTarget.left >= pointScore.center.x &&
            opponentTarget.right <= pointScore.right)
        assertTrue("Undo gap ${undo.top - pointScore.bottom}px must be at least ${minGap}px; root=$root panel=$pointScore undo=$undo",
            undo.top - pointScore.bottom >= minGap)
    }

    @Test fun tappingEachScoreHalfAwardsThatPlayer() {
        launch(initialPage = Page.SCORE)
        rule.onNodeWithTag("score:you-target").performClick()
        assertPoints("15", "0")
        rule.onNodeWithTag("score:opponent-target").performClick()
        assertPoints("15", "15")
        rule.onNodeWithTag("score:targets").assertDoesNotExist()
    }

    @Test fun scoreUndoFitsInsideRoundDisplay() {
        launch(initialPage = Page.SCORE)
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val undo = rule.onNodeWithTag("score:undo").fetchSemanticsNode().boundsInRoot
        val radius = minOf(root.width, root.height) / 2f
        for (x in listOf(undo.left, undo.right)) {
            for (y in listOf(undo.top, undo.bottom)) {
                assertTrue("UNDO must fit inside the round watch",
                    hypot(x - root.center.x, y - root.center.y) <= radius)
            }
        }
    }

    @Test fun scoreUndoHasWatchSizedTouchTarget() {
        launch(initialPage = Page.SCORE)
        assertWatchSizedTouchTarget("Undo last point")
    }

    @Test fun homeActionsHaveWatchSizedTouchTargets() {
        launch()
        listOf("START MATCH", "HISTORY", "SETTINGS", "PRIVACY").forEach {
            assertWatchSizedTouchTarget(it, scroll = true)
        }
    }

    @Test fun settingsChoicesHaveWatchSizedTouchTargets() {
        launch(initialPage = Page.SETTINGS)
        listOf("Best of 3", "Best of 5", "Full deciding set", "10-point match tiebreak")
            .forEach { assertWatchSizedTouchTarget(it, scroll = true) }
    }

    private fun assertWatchSizedTouchTarget(description: String, scroll: Boolean = false) {
        val node = rule.onNodeWithContentDescription(description)
        if (scroll) node.performScrollTo()
        val target = node.fetchSemanticsNode().boundsInRoot
        val minimum = 48f * InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        assertTrue("$description target is ${target.width} x ${target.height}px; minimum is ${minimum}px",
            target.width >= minimum && target.height >= minimum)
    }

    @Test fun matchCompleteHomeFitsInsideRoundDisplay() {
        launch(initial = "Y".repeat(48), initialPage = Page.COMPLETE)
        rule.onNodeWithContentDescription("HOME").performScrollTo()
        rule.onRoot().performTouchInput { swipeUp() }
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val home = rule.onNodeWithContentDescription("HOME").fetchSemanticsNode().boundsInRoot
        val centerX = root.center.x
        val centerY = root.center.y
        val radius = minOf(root.width, root.height) / 2f
        for (x in listOf(home.left, home.right)) {
            for (y in listOf(home.top, home.bottom)) {
                assertTrue("HOME must fit inside the round watch; root=$root home=$home",
                    hypot(x - centerX, y - centerY) <= radius)
            }
        }
    }

    @Test fun matchCompleteActionsHaveWatchSizedTouchTargets() {
        launch(initial = "Y".repeat(48), initialPage = Page.COMPLETE)
        listOf("NEW MATCH", "UNDO LAST POINT", "HOME").forEach {
            assertWatchSizedTouchTarget(it, scroll = true)
        }
    }
}
