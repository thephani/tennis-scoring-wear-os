package com.example.tennisscore.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tennisscore.data.MatchRepository
import com.example.tennisscore.domain.Player
import com.example.tennisscore.domain.MatchFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Page { LOADING, START, RESUME, SETTINGS, PRIVACY, SCORE, COMPLETE, HISTORY, CONFIRM_NEW,
    SELECT_SERVER_NEW, SELECT_SERVER_RESUME }

class MatchViewModel(private val repository: MatchRepository) : ViewModel() {
    val saved = repository.state.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var page by mutableStateOf(Page.LOADING)
        private set
    private var settingsReturnPage = Page.START
    private var privacyReturnPage = Page.START

    init {
        viewModelScope.launch {
            val active = repository.state.first().score
            page = when {
                active == null -> Page.START
                active.winner != null -> Page.COMPLETE
                else -> Page.RESUME
            }
        }
    }

    fun show(page: Page) {
        if (page == Page.SETTINGS) settingsReturnPage = this.page
        if (page == Page.PRIVACY) privacyReturnPage = this.page
        this.page = page
    }
    fun saveSettings(format: MatchFormat) = viewModelScope.launch {
        repository.saveSettings(format)
        saved.first { it?.defaultFormat == format }
        page = settingsReturnPage
    }
    fun exitSettings() { page = settingsReturnPage }
    fun exitPrivacy() { page = privacyReturnPage }
    fun startMatch(firstServer: Player) = viewModelScope.launch {
        val id = repository.newMatch(firstServer)
        saved.first { it?.startedAt == id && it.events == "" && it.firstServer == firstServer }
        page = Page.SCORE
    }
    fun setFirstServer(firstServer: Player) = viewModelScope.launch {
        repository.setFirstServer(firstServer)
        saved.first { it?.events != null && it.firstServer == firstServer }
        page = Page.SCORE
    }
    fun scorePoint(winner: Player) = viewModelScope.launch {
        if (repository.scorePoint(winner)) {
            saved.first { it?.score?.winner != null }
            page = Page.COMPLETE
        }
    }
    fun undo() = viewModelScope.launch {
        repository.undo()
        val restored = saved.first { it?.score?.let { score -> score.winner == null } == true }
        page = if (restored?.firstServer == null) Page.SELECT_SERVER_RESUME else Page.SCORE
    }

    companion object {
        fun factory(repository: MatchRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MatchViewModel(repository) as T
            }
    }
}
