package com.example.tennisscore.domain

import kotlin.math.abs

enum class Player(val code: Char) {
    YOU('Y'), OPPONENT('O');

    companion object {
        fun fromCode(code: Char): Player = entries.firstOrNull { it.code == code }
            ?: throw IllegalArgumentException("Unknown point event")
    }
}

data class SetScore(val you: Int = 0, val opponent: Int = 0) {
    fun games(player: Player): Int = if (player == Player.YOU) you else opponent
    fun wonBy(player: Player): Boolean {
        val own = games(player)
        val other = games(if (player == Player.YOU) Player.OPPONENT else Player.YOU)
        return own >= 6 && (own - other >= 2 || own == 7 && other == 6)
    }
}

data class MatchScore(
    val sets: List<SetScore> = listOf(SetScore()),
    val youPoints: Int = 0,
    val opponentPoints: Int = 0,
    val format: MatchFormat = MatchFormat(),
    val isMatchTieBreak: Boolean = false,
) {
    val currentSet: SetScore get() = sets.last()
    fun setsWon(player: Player): Int = sets.count { it.wonBy(player) }
    val winner: Player?
        get() {
            if (isMatchTieBreak) {
                if (maxOf(youPoints, opponentPoints) < 10 || abs(youPoints - opponentPoints) < 2) {
                    return null
                }
                return if (youPoints > opponentPoints) Player.YOU else Player.OPPONENT
            }
            return Player.entries.firstOrNull { setsWon(it) == format.length.setsToWin }
        }
    val isTieBreak: Boolean
        get() = !isMatchTieBreak && winner == null && currentSet.you == 6 && currentSet.opponent == 6

    fun pointValue(player: Player): String {
        val own = if (player == Player.YOU) youPoints else opponentPoints
        val other = if (player == Player.YOU) opponentPoints else youPoints
        if (isTieBreak || isMatchTieBreak) return own.toString()
        if (own >= 3 && other >= 3) return if (own > other) "AD" else "40"
        return listOf("0", "15", "30", "40")[own.coerceAtMost(3)]
    }

    val pointLabel: String
        get() {
            if (isTieBreak || isMatchTieBreak) return "$youPoints-$opponentPoints"
            if (youPoints >= 3 && opponentPoints >= 3) {
                return when {
                    youPoints == opponentPoints -> "Deuce"
                    youPoints > opponentPoints -> "Adv YOU"
                    else -> "Adv OPP"
                }
            }
            val labels = listOf("0", "15", "30", "40")
            return "${labels[youPoints.coerceAtMost(3)]}-${labels[opponentPoints.coerceAtMost(3)]}"
        }
}
