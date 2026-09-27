package com.example.tennisscore.domain

import com.example.tennisscore.data.MatchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchResultTest {
    @Test fun completedScoreRoundTripsIntoHistory() {
        val score = ScoringEngine.replay("Y".repeat(48))
        val result = MatchResult.fromScore(123L, score)
        assertEquals(Player.YOU, result.winner)
        assertEquals("6-0 6-0", result.sets)
        assertEquals(result, MatchResult.decode(result.encode()))
    }

    @Test fun malformedHistoryEntryIsIgnored() {
        assertNull(MatchResult.decode("bad"))
        assertNull(MatchResult.decode("123|?|6-0"))
    }

    @Test fun decidingMatchTieBreakIsRecordedAsPoints() {
        val format = MatchFormat(MatchLength.BEST_OF_3, DecidingSet.MATCH_TIEBREAK_10)
        val score = ScoringEngine.replay(
            "Y".repeat(24) + "O".repeat(24) + "O".repeat(8) + "Y".repeat(10), format)
        val result = MatchResult.fromScore(456L, score)
        assertEquals("6-0 0-6 [10-8]", result.sets)
        assertEquals(result, MatchResult.decode(result.encode()))
    }
}
