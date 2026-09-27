package com.example.tennisscore.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tennisscore.domain.MatchScore
import com.example.tennisscore.domain.MatchFormat
import com.example.tennisscore.domain.MatchLength
import com.example.tennisscore.domain.DecidingSet
import com.example.tennisscore.domain.Player
import com.example.tennisscore.domain.ScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.matchDataStore by preferencesDataStore(name = "tennis_match")

data class MatchResult(val id: Long, val winner: Player, val sets: String) {
    fun encode(): String = "$id|${winner.code}|$sets"

    companion object {
        fun decode(raw: String): MatchResult? {
            val fields = raw.split('|', limit = 3)
            if (fields.size != 3) return null
            val id = fields[0].toLongOrNull() ?: return null
            val winner = runCatching { Player.fromCode(fields[1].single()) }.getOrNull() ?: return null
            return MatchResult(id, winner, fields[2])
        }

        fun fromScore(id: Long, score: MatchScore): MatchResult = MatchResult(
            id = id,
            winner = requireNotNull(score.winner),
            sets = score.sets.joinToString(" ") { "${it.you}-${it.opponent}" } +
                if (score.isMatchTieBreak) " [${score.youPoints}-${score.opponentPoints}]" else "",
        )
    }
}

data class SavedMatch(
    val events: String?,
    val startedAt: Long,
    val history: List<MatchResult>,
    val firstServer: Player? = null,
    val format: MatchFormat = MatchFormat(),
    val defaultFormat: MatchFormat = MatchFormat(),
) {
    val score: MatchScore? get() = events?.let { ScoringEngine.replay(it, format) }
}

/** One DataStore transaction per action keeps the point log and history in sync. */
class MatchRepository(context: Context) {
    private val store = context.applicationContext.matchDataStore
    private val activeKey = stringPreferencesKey("active_events")
    private val startedKey = longPreferencesKey("started_at")
    private val historyKey = stringSetPreferencesKey("history")
    private val firstServerKey = stringPreferencesKey("first_server")
    private val settingsLengthKey = stringPreferencesKey("settings_match_length")
    private val settingsDecidingKey = stringPreferencesKey("settings_deciding_set")
    private val activeLengthKey = stringPreferencesKey("active_match_length")
    private val activeDecidingKey = stringPreferencesKey("active_deciding_set")

    private fun formatOf(preferences: Preferences, lengthKey: Preferences.Key<String>,
        decidingKey: Preferences.Key<String>): MatchFormat = MatchFormat(
        length = MatchLength.entries.firstOrNull { it.name == preferences[lengthKey] }
            ?: MatchLength.BEST_OF_3,
        decidingSet = DecidingSet.entries.firstOrNull { it.name == preferences[decidingKey] }
            ?: DecidingSet.FULL_SET,
    )

    val state: Flow<SavedMatch> = store.data.map { preferences ->
        SavedMatch(
            events = preferences[activeKey],
            startedAt = preferences[startedKey] ?: 0L,
            history = (preferences[historyKey] ?: emptySet()).mapNotNull(MatchResult::decode)
                .sortedByDescending { it.id },
            firstServer = preferences[firstServerKey]?.singleOrNull()
                ?.let { runCatching { Player.fromCode(it) }.getOrNull() },
            format = formatOf(preferences, activeLengthKey, activeDecidingKey),
            defaultFormat = formatOf(preferences, settingsLengthKey, settingsDecidingKey),
        )
    }

    suspend fun saveSettings(format: MatchFormat) {
        store.edit { preferences ->
            preferences[settingsLengthKey] = format.length.name
            preferences[settingsDecidingKey] = format.decidingSet.name
        }
    }

    suspend fun newMatch(firstServer: Player): Long {
        var startedAt = 0L
        store.edit { preferences ->
            preferences[activeKey] = ""
            preferences[firstServerKey] = firstServer.code.toString()
            val format = formatOf(preferences, settingsLengthKey, settingsDecidingKey)
            preferences[activeLengthKey] = format.length.name
            preferences[activeDecidingKey] = format.decidingSet.name
            startedAt = maxOf(System.currentTimeMillis(), (preferences[startedKey] ?: 0L) + 1L)
            preferences[startedKey] = startedAt
        }
        return startedAt
    }

    suspend fun setFirstServer(firstServer: Player) {
        store.edit { preferences ->
            val savedServer = preferences[firstServerKey]?.singleOrNull()
                ?.let { runCatching { Player.fromCode(it) }.getOrNull() }
            if (preferences[activeKey] != null && savedServer == null) {
                preferences[firstServerKey] = firstServer.code.toString()
            }
        }
    }

    suspend fun scorePoint(winner: Player): Boolean {
        var completed = false
        store.edit { preferences ->
            val events = preferences[activeKey] ?: return@edit
            val before = ScoringEngine.replay(events,
                formatOf(preferences, activeLengthKey, activeDecidingKey))
            if (before.winner != null) return@edit
            val after = ScoringEngine.scorePoint(before, winner)
            preferences[activeKey] = events + winner.code
            if (after.winner != null) {
                completed = true
                val result = MatchResult.fromScore(preferences[startedKey] ?: 0L, after)
                preferences[historyKey] = (preferences[historyKey] ?: emptySet()) + result.encode()
            }
        }
        return completed
    }

    suspend fun undo() {
        store.edit { preferences ->
            val events = preferences[activeKey] ?: return@edit
            if (events.isEmpty()) return@edit
            if (ScoringEngine.replay(events,
                formatOf(preferences, activeLengthKey, activeDecidingKey)).winner != null) {
                val id = preferences[startedKey]
                preferences[historyKey] = (preferences[historyKey] ?: emptySet())
                    .filterNot { MatchResult.decode(it)?.id == id }.toSet()
            }
            preferences[activeKey] = events.dropLast(1)
        }
    }
}
