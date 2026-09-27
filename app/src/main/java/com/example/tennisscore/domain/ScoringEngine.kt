package com.example.tennisscore.domain

import kotlin.math.abs

/** Pure tennis rules. A persisted sequence of point winners can reproduce every state. */
object ScoringEngine {
    fun replay(events: String, format: MatchFormat = MatchFormat()): MatchScore =
        events.fold(MatchScore(format = format)) { score, code -> scorePoint(score, Player.fromCode(code)) }

    fun scorePoint(score: MatchScore, winner: Player): MatchScore {
        if (score.winner != null) return score
        val you = score.youPoints + if (winner == Player.YOU) 1 else 0
        val opponent = score.opponentPoints + if (winner == Player.OPPONENT) 1 else 0
        val target = when {
            score.isMatchTieBreak -> 10
            score.isTieBreak -> 7
            else -> 4
        }
        if (maxOf(you, opponent) < target || abs(you - opponent) < 2) {
            return score.copy(youPoints = you, opponentPoints = opponent)
        }
        if (score.isMatchTieBreak) {
            return score.copy(youPoints = you, opponentPoints = opponent)
        }

        val set = score.currentSet
        val completedGame = if (winner == Player.YOU) set.copy(you = set.you + 1)
            else set.copy(opponent = set.opponent + 1)
        val updatedSets = score.sets.dropLast(1) + completedGame
        val finishedSet = completedGame.wonBy(winner)
        val finishedMatch = finishedSet &&
            updatedSets.count { it.wonBy(winner) } == score.format.length.setsToWin
        val startMatchTieBreak = finishedSet && !finishedMatch &&
            score.format.decidingSet == DecidingSet.MATCH_TIEBREAK_10 &&
            Player.entries.all { player ->
                updatedSets.count { it.wonBy(player) } == score.format.length.setsToWin - 1
            }
        return MatchScore(
            sets = if (finishedSet && !finishedMatch && !startMatchTieBreak)
                updatedSets + SetScore() else updatedSets,
            format = score.format,
            isMatchTieBreak = startMatchTieBreak,
        )
    }
}
