package com.example.tennisscore

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tennisscore.data.MatchRepository
import com.example.tennisscore.domain.Player
import com.example.tennisscore.domain.MatchFormat
import com.example.tennisscore.domain.MatchLength
import com.example.tennisscore.domain.DecidingSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchRepositoryTest {
    @Test fun settingsApplyToNewMatchesWithoutChangingActiveMatch() = runBlocking {
        val repository = MatchRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val custom = MatchFormat(MatchLength.BEST_OF_5, DecidingSet.MATCH_TIEBREAK_10)
        val defaults = MatchFormat()
        try {
            repository.saveSettings(custom)
            val firstId = repository.newMatch(Player.YOU)
            val first = repository.state.first { it.startedAt == firstId && it.events == "" }
            assertEquals(custom, first.format)
            assertEquals(custom, first.defaultFormat)

            repository.saveSettings(defaults)
            val unchanged = repository.state.first {
                it.startedAt == firstId && it.defaultFormat == defaults
            }
            assertEquals(custom, unchanged.format)
            val secondId = repository.newMatch(Player.OPPONENT)
            assertEquals(defaults, repository.state.first { it.startedAt == secondId }.format)
        } finally {
            repository.saveSettings(defaults)
        }
    }

    @Test fun firstServerPersistsWithScoreAndResetsForNewMatch() = runBlocking {
        val repository = MatchRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val firstId = repository.newMatch(Player.OPPONENT)
        assertEquals(Player.OPPONENT,
            repository.state.first { it.startedAt == firstId }.firstServer)

        repository.scorePoint(Player.YOU)
        val scored = repository.state.first { it.startedAt == firstId && it.events == "Y" }
        assertEquals(Player.OPPONENT, scored.firstServer)
        repository.setFirstServer(Player.YOU)
        assertEquals(Player.OPPONENT, repository.state.first().firstServer)

        val secondId = repository.newMatch(Player.YOU)
        assertTrue(secondId > firstId)
        val newMatch = repository.state.first { it.startedAt == secondId && it.events == "" }
        assertEquals(Player.YOU, newMatch.firstServer)
    }
}
