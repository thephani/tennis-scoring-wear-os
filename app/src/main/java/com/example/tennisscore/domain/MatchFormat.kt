package com.example.tennisscore.domain

enum class MatchLength(val sets: Int, val setsToWin: Int) {
    BEST_OF_3(3, 2),
    BEST_OF_5(5, 3),
}

enum class DecidingSet { FULL_SET, MATCH_TIEBREAK_10 }

data class MatchFormat(
    val length: MatchLength = MatchLength.BEST_OF_3,
    val decidingSet: DecidingSet = DecidingSet.FULL_SET,
)
