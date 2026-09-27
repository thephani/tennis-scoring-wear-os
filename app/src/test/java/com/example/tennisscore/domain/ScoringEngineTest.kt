package com.example.tennisscore.domain

import org.junit.Assert.*
import org.junit.Test

class ScoringEngineTest {
    private fun points(vararg winners: Player): MatchScore =
        winners.fold(MatchScore()) { score, winner -> ScoringEngine.scorePoint(score, winner) }

    private fun game(score: MatchScore, winner: Player): MatchScore =
        (1..4).fold(score) { current, _ -> ScoringEngine.scorePoint(current, winner) }

    private fun games(score: MatchScore, winner: Player, count: Int): MatchScore =
        (1..count).fold(score) { current, _ -> game(current, winner) }

    private fun tieStart(): MatchScore {
        var score = MatchScore()
        repeat(6) {
            score = game(score, Player.YOU)
            score = game(score, Player.OPPONENT)
        }
        return score
    }

    @Test fun serverChangesAfterGamesRegardlessOfWhoWonThem() {
        var score = MatchScore()
        assertEquals(Player.YOU, ServeOrder.currentServer(score, Player.YOU))
        repeat(3) { score = ScoringEngine.scorePoint(score, Player.OPPONENT) }
        assertEquals(Player.YOU, ServeOrder.currentServer(score, Player.YOU))
        score = ScoringEngine.scorePoint(score, Player.OPPONENT)
        assertEquals(Player.OPPONENT, ServeOrder.currentServer(score, Player.YOU))
        score = game(score, Player.YOU)
        assertEquals(Player.YOU, ServeOrder.currentServer(score, Player.YOU))
        assertEquals(Player.OPPONENT, ServeOrder.currentServer(score, Player.OPPONENT))
    }

    @Test fun serverContinuesAcrossSetBoundary() {
        var score = games(MatchScore(), Player.YOU, 6)
        assertEquals(Player.YOU, ServeOrder.currentServer(score, Player.YOU))
        score = games(score, Player.OPPONENT, 6)
        assertEquals(Player.YOU, ServeOrder.currentServer(score, Player.YOU))
    }

    @Test fun tieBreakServiceRunsOneThenPairsAndNextSetReceives() {
        val expectations = mapOf(
            Player.YOU to listOf(Player.YOU, Player.OPPONENT, Player.OPPONENT,
                Player.YOU, Player.YOU, Player.OPPONENT, Player.OPPONENT),
            Player.OPPONENT to listOf(Player.OPPONENT, Player.YOU, Player.YOU,
                Player.OPPONENT, Player.OPPONENT, Player.YOU, Player.YOU),
        )
        expectations.forEach { (firstServer, expected) ->
            var score = tieStart()
            expected.forEachIndexed { index, server ->
                assertEquals("$firstServer point $index", server,
                    ServeOrder.currentServer(score, firstServer))
                if (index < expected.lastIndex) score = ScoringEngine.scorePoint(score, Player.YOU)
            }
            score = ScoringEngine.scorePoint(score, Player.YOU)
            assertEquals(firstServer.other(), ServeOrder.currentServer(score, firstServer))
        }
    }

    private fun Player.other(): Player = if (this == Player.YOU) Player.OPPONENT else Player.YOU

    @Test fun undoAndReplayRestorePriorServer() {
        val before = "Y".repeat(24)
        assertEquals(Player.YOU, ServeOrder.currentServer(ScoringEngine.replay(before), Player.YOU))
        assertEquals(Player.OPPONENT, ServeOrder.currentServer(ScoringEngine.replay(before + "O".repeat(4)), Player.YOU))
        assertEquals(Player.YOU, ServeOrder.currentServer(ScoringEngine.replay(before + "O".repeat(3)), Player.YOU))
    }

    @Test fun normalGamesForBothPlayers() {
        var score = MatchScore()
        for (label in listOf("15-0", "30-0", "40-0")) {
            score = ScoringEngine.scorePoint(score, Player.YOU)
            assertEquals(label, score.pointLabel)
        }
        score = ScoringEngine.scorePoint(score, Player.YOU)
        assertEquals(1, score.currentSet.you)
        assertEquals("0-0", score.pointLabel)
        score = game(score, Player.OPPONENT)
        assertEquals(1, score.currentSet.opponent)
    }

    @Test fun deuceAndAdvantageCycles() {
        var score = points(Player.YOU, Player.YOU, Player.YOU,
            Player.OPPONENT, Player.OPPONENT, Player.OPPONENT)
        assertEquals("Deuce", score.pointLabel)
        score = ScoringEngine.scorePoint(score, Player.YOU)
        assertEquals("Adv YOU", score.pointLabel)
        score = ScoringEngine.scorePoint(score, Player.OPPONENT)
        assertEquals("Deuce", score.pointLabel)
        score = ScoringEngine.scorePoint(score, Player.OPPONENT)
        assertEquals("Adv OPP", score.pointLabel)
        score = ScoringEngine.scorePoint(score, Player.YOU)
        assertEquals("Deuce", score.pointLabel)
        score = ScoringEngine.scorePoint(score, Player.YOU)
        score = ScoringEngine.scorePoint(score, Player.YOU)
        assertEquals(1, score.currentSet.you)
    }

    @Test fun individualPointValuesCoverGamesAndTiebreaks() {
        var score = points(Player.YOU, Player.YOU, Player.OPPONENT)
        assertEquals("30", score.pointValue(Player.YOU))
        assertEquals("15", score.pointValue(Player.OPPONENT))

        score = points(Player.YOU, Player.YOU, Player.YOU,
            Player.OPPONENT, Player.OPPONENT, Player.OPPONENT)
        assertEquals("40", score.pointValue(Player.YOU))
        assertEquals("40", score.pointValue(Player.OPPONENT))
        score = ScoringEngine.scorePoint(score, Player.OPPONENT)
        assertEquals("40", score.pointValue(Player.YOU))
        assertEquals("AD", score.pointValue(Player.OPPONENT))

        score = tieStart()
        repeat(6) { score = ScoringEngine.scorePoint(score, Player.YOU) }
        repeat(5) { score = ScoringEngine.scorePoint(score, Player.OPPONENT) }
        assertEquals("6", score.pointValue(Player.YOU))
        assertEquals("5", score.pointValue(Player.OPPONENT))

        val matchTieBreak = MatchScore(format = MatchFormat(decidingSet = DecidingSet.MATCH_TIEBREAK_10),
            isMatchTieBreak = true, youPoints = 10, opponentPoints = 9)
        assertEquals("10", matchTieBreak.pointValue(Player.YOU))
        assertEquals("9", matchTieBreak.pointValue(Player.OPPONENT))
    }

    @Test fun setsEndAtSixWithTwoGameLead() {
        for (loserGames in 0..4) {
            val score = games(games(MatchScore(), Player.OPPONENT, loserGames), Player.YOU, 6)
            assertEquals(1, score.setsWon(Player.YOU))
            assertEquals(SetScore(6, loserGames), score.sets.first())
        }
    }

    @Test fun sixFiveContinuesAndSixSixStartsTieBreak() {
        var score = games(games(MatchScore(), Player.YOU, 5), Player.OPPONENT, 5)
        score = game(score, Player.YOU)
        assertEquals(0, score.setsWon(Player.YOU))
        assertFalse(score.isTieBreak)
        score = game(score, Player.OPPONENT)
        assertTrue(score.isTieBreak)
        assertEquals("0-0", score.pointLabel)
    }

    @Test fun sevenFiveCompletesSet() {
        var score = games(games(MatchScore(), Player.YOU, 5), Player.OPPONENT, 5)
        score = game(score, Player.YOU)
        score = game(score, Player.YOU)
        assertEquals(SetScore(7, 5), score.sets.first())
        assertEquals(1, score.setsWon(Player.YOU))
    }

    @Test fun tieBreakRequiresTwoPointLead() {
        for (loserPoints in 0..5) {
            var score = tieStart()
            repeat(loserPoints) { score = ScoringEngine.scorePoint(score, Player.OPPONENT) }
            repeat(7) { score = ScoringEngine.scorePoint(score, Player.YOU) }
            assertEquals(SetScore(7, 6), score.sets.first())
            assertEquals(1, score.setsWon(Player.YOU))
        }
        var score = tieStart()
        repeat(6) { score = ScoringEngine.scorePoint(score, Player.OPPONENT) }
        repeat(7) { score = ScoringEngine.scorePoint(score, Player.YOU) }
        assertTrue(score.isTieBreak)
        assertEquals("7-6", score.pointLabel)
        score = ScoringEngine.scorePoint(score, Player.YOU)
        assertEquals(1, score.setsWon(Player.YOU))
        assertEquals(SetScore(7, 6), score.sets.first())
    }

    @Test fun eitherPlayerCanWinBestOfThree() {
        for (winner in Player.entries) {
            val score = games(games(MatchScore(), winner, 6), winner, 6)
            assertEquals(winner, score.winner)
            assertEquals(2, score.sets.size)
            assertEquals(score, ScoringEngine.scorePoint(score, winner))
        }
    }

    @Test fun bestOfFiveNeedsThreeSets() {
        val format = MatchFormat(MatchLength.BEST_OF_5, DecidingSet.FULL_SET)
        var score = MatchScore(format = format)
        score = games(score, Player.YOU, 6)
        score = games(score, Player.YOU, 6)
        assertNull(score.winner)
        score = games(score, Player.OPPONENT, 6)
        score = games(score, Player.OPPONENT, 6)
        assertEquals(5, score.sets.size)
        assertNull(score.winner)
        score = games(score, Player.YOU, 6)
        assertEquals(Player.YOU, score.winner)
        assertEquals(5, score.sets.size)
    }

    @Test fun decidingMatchTieBreakReplacesThirdSetAndNeedsTwoPointLead() {
        val format = MatchFormat(MatchLength.BEST_OF_3, DecidingSet.MATCH_TIEBREAK_10)
        var score = games(MatchScore(format = format), Player.YOU, 6)
        score = games(score, Player.OPPONENT, 6)
        assertTrue(score.isMatchTieBreak)
        assertEquals(2, score.sets.size)
        assertEquals("0-0", score.pointLabel)
        repeat(9) { score = ScoringEngine.scorePoint(score, Player.YOU) }
        repeat(9) { score = ScoringEngine.scorePoint(score, Player.OPPONENT) }
        score = ScoringEngine.scorePoint(score, Player.YOU)
        assertNull(score.winner)
        score = ScoringEngine.scorePoint(score, Player.YOU)
        assertEquals(Player.YOU, score.winner)
        assertEquals("11-9", score.pointLabel)
        assertEquals(2, score.sets.size)
    }

    @Test fun decidingMatchTieBreakAlsoReplacesFifthSet() {
        val format = MatchFormat(MatchLength.BEST_OF_5, DecidingSet.MATCH_TIEBREAK_10)
        var score = MatchScore(format = format)
        repeat(2) { score = games(score, Player.YOU, 6) }
        repeat(2) { score = games(score, Player.OPPONENT, 6) }
        assertTrue(score.isMatchTieBreak)
        assertEquals(4, score.sets.size)
        repeat(8) { score = ScoringEngine.scorePoint(score, Player.OPPONENT) }
        repeat(10) { score = ScoringEngine.scorePoint(score, Player.YOU) }
        assertEquals(Player.YOU, score.winner)
        assertEquals("10-8", score.pointLabel)
    }

    @Test fun matchTieBreakReplaysAndUndoRestoresLiveState() {
        val format = MatchFormat(MatchLength.BEST_OF_3, DecidingSet.MATCH_TIEBREAK_10)
        val events = "Y".repeat(24) + "O".repeat(24) + "Y".repeat(10)
        assertEquals(Player.YOU, ScoringEngine.replay(events, format).winner)
        val beforeWinningPoint = ScoringEngine.replay(events.dropLast(1), format)
        assertTrue(beforeWinningPoint.isMatchTieBreak)
        assertNull(beforeWinningPoint.winner)
        assertEquals("9-0", beforeWinningPoint.pointLabel)
        assertEquals(Player.OPPONENT, ServeOrder.currentServer(
            ScoringEngine.replay("Y".repeat(24) + "O".repeat(24) + "Y", format), Player.YOU))
    }

    @Test fun opponentCanWinTieBreak() {
        var score = tieStart()
        repeat(7) { score = ScoringEngine.scorePoint(score, Player.OPPONENT) }
        assertEquals(SetScore(6, 7), score.sets.first())
        assertEquals(1, score.setsWon(Player.OPPONENT))
    }

    @Test fun replayAndUndoRestoreExactStatesAcrossBoundaries() {
        var events = ""
        fun add(winner: Player) { events += winner.code }
        repeat(5 * 4 + 3) { add(Player.YOU) }
        val beforeGame = ScoringEngine.replay(events)
        add(Player.YOU)
        assertEquals(SetScore(6, 0), ScoringEngine.replay(events).sets.first())
        assertEquals(beforeGame, ScoringEngine.replay(events.dropLast(1)))

        events = ""
        repeat(6) {
            repeat(4) { add(Player.YOU) }
            repeat(4) { add(Player.OPPONENT) }
        }
        add(Player.YOU)
        val beforeTiePoint = ScoringEngine.replay(events)
        add(Player.YOU)
        assertEquals(beforeTiePoint, ScoringEngine.replay(events.dropLast(1)))
        repeat(4) { add(Player.YOU) }
        val beforeTieEnd = ScoringEngine.replay(events)
        add(Player.YOU)
        assertEquals(SetScore(7, 6), ScoringEngine.replay(events).sets.first())
        assertEquals(beforeTieEnd, ScoringEngine.replay(events.dropLast(1)))

        events = ""
        repeat(6 * 4 + 6 * 4 - 1) { add(Player.OPPONENT) }
        val beforeMatchEnd = ScoringEngine.replay(events)
        add(Player.OPPONENT)
        assertEquals(Player.OPPONENT, ScoringEngine.replay(events).winner)
        assertEquals(beforeMatchEnd, ScoringEngine.replay(events.dropLast(1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun corruptEventLogIsRejected() { ScoringEngine.replay("Y?") }
}
