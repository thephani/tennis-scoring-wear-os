package com.example.tennisscore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.tennisscore.data.MatchResult
import com.example.tennisscore.data.SavedMatch
import com.example.tennisscore.R
import com.example.tennisscore.domain.MatchScore
import com.example.tennisscore.domain.MatchFormat
import com.example.tennisscore.domain.MatchType
import com.example.tennisscore.domain.MatchLength
import com.example.tennisscore.domain.DecidingSet
import com.example.tennisscore.domain.Player
import com.example.tennisscore.domain.ScoringEngine
import com.example.tennisscore.domain.ServeOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val background = Color(0xFF0B1012)
private val youColor = Color(0xFF9CE9A9)
private val opponentColor = Color(0xFFF7B986)
private val accent = Color(0xFFA1EFAF)
private val ink = Color(0xFF10221C)
private val muted = Color(0xFFB5C7C3)

@Composable
fun TennisApp(model: MatchViewModel) {
    val saved by model.saved.collectAsState()
    TennisScreen(
        page = model.page,
        saved = saved,
        onStart = model::startMatch,
        onSetFirstServer = model::setFirstServer,
        onScore = model::scorePoint,
        onUndo = model::undo,
        onShow = model::show,
        onSaveSettings = model::saveSettings,
        onExitSettings = model::exitSettings,
        onExitPrivacy = model::exitPrivacy,
    )
}

@Composable
fun TennisScreen(
    page: Page,
    saved: SavedMatch?,
    onStart: (Player) -> Unit,
    onSetFirstServer: (Player) -> Unit,
    onScore: (Player) -> Unit,
    onUndo: () -> Unit,
    onShow: (Page) -> Unit,
    onSaveSettings: (MatchFormat) -> Unit,
    onExitSettings: () -> Unit,
    onExitPrivacy: () -> Unit,
) {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.Center) {
            when (page) {
                Page.LOADING -> Label("Loading…", 16)
                Page.START -> MenuScreen(
                    title = "TENNIS SCORE",
                    subtitle = formatSummary(saved?.defaultFormat ?: MatchFormat()),
                    actions = listOf(
                        Action("START MATCH", "START") { onShow(Page.SELECT_SERVER_NEW) },
                        Action("HISTORY") { onShow(Page.HISTORY) },
                        Action("SETTINGS") { onShow(Page.SETTINGS) },
                        Action("PRIVACY") { onShow(Page.PRIVACY) },
                    ),
                )
                Page.RESUME -> MenuScreen(
                    title = "RESUME MATCH?",
                    subtitle = saved?.score?.let(::setSummary) ?: formatSummary(saved?.format ?: MatchFormat()),
                    actions = listOf(
                        Action("RESUME") {
                            onShow(if (saved?.firstServer == null) Page.SELECT_SERVER_RESUME else Page.SCORE)
                        },
                        Action("NEW MATCH") { onShow(Page.CONFIRM_NEW) },
                        Action("SETTINGS") { onShow(Page.SETTINGS) },
                        Action("PRIVACY") { onShow(Page.PRIVACY) },
                    ),
                )
                Page.SETTINGS -> SettingsScreen(saved?.defaultFormat ?: MatchFormat(),
                    onSaveSettings, onExitSettings)
                Page.PRIVACY -> PrivacyScreen(onExitPrivacy)
                Page.CONFIRM_NEW -> MenuScreen(
                    title = "NEW MATCH?",
                    subtitle = "Current match will be replaced",
                    actions = listOf(
                        Action("REPLACE MATCH") { onShow(Page.SELECT_SERVER_NEW) },
                        Action("CANCEL") { onShow(Page.RESUME) },
                    ),
                )
                Page.SELECT_SERVER_NEW -> FirstServerMenu("Who serves first?", onStart) {
                    onShow(when {
                        saved?.score?.winner != null -> Page.COMPLETE
                        saved?.events != null -> Page.RESUME
                        else -> Page.START
                    })
                }
                Page.SELECT_SERVER_RESUME -> FirstServerMenu("For this match", onSetFirstServer) {
                    onShow(Page.RESUME)
                }
                Page.SCORE -> saved?.score?.let { score ->
                    if (saved.firstServer == null) {
                        FirstServerMenu("For this match", onSetFirstServer) { onShow(Page.RESUME) }
                    } else {
                        ScoringScreen(score, saved.firstServer, saved.events.orEmpty(), onScore, onUndo)
                    }
                } ?: Label("Loading…", 16)
                Page.COMPLETE -> saved?.score?.let { score ->
                    CompleteScreen(score, onUndo) { onShow(Page.START) }
                } ?: Label("Loading…", 16)
                Page.HISTORY -> HistoryScreen(saved?.history.orEmpty()) { onShow(Page.START) }
            }
        }
    }
}

@Composable
private fun PrivacyScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label("PRIVACY", 17, bold = true, color = accent)
        Spacer(Modifier.height(12.dp))
        Text("Match scores, settings, and history stay on this watch.",
            color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Saved data includes point entries, match format, first server, and results with date, winner, and set scores. It is excluded from cloud backup and device transfer.",
            color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("We do not collect or share data from this app. There is no account, advertising, or analytics.",
            color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Clear the app's storage or uninstall it to delete saved match data.",
            color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Label("thephani.old@gmail.com", 10, color = accent)
        Spacer(Modifier.height(16.dp))
        ActionButton("BACK", onBack)
        Spacer(Modifier.height(20.dp))
    }
}

private data class Action(val label: String, val displayLabel: String = label, val onClick: () -> Unit)

@Composable
private fun FirstServerMenu(subtitle: String, onChoose: (Player) -> Unit, onCancel: () -> Unit) {
    MenuScreen(
        title = "FIRST SERVER",
        subtitle = subtitle,
        actions = listOf(
            Action("YOU SERVE FIRST", "YOU") { onChoose(Player.YOU) },
            Action("OPP SERVES FIRST", "OPP") { onChoose(Player.OPPONENT) },
            Action("CANCEL") { onCancel() },
        ),
    )
}

@Composable
private fun MenuScreen(title: String, subtitle: String, actions: List<Action>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rowWidth = maxWidth * 0.62f
        val viewportHeight = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = viewportHeight)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Label(title, if (title.length >= 12) 16 else 18, bold = true, color = accent)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.White, fontSize = 12.sp,
                    textAlign = TextAlign.Center, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                when (actions.size) {
                    4 -> {
                        actions.chunked(2).forEachIndexed { rowIndex, rowActions ->
                            Row(Modifier.width(rowWidth), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowActions.forEach { action ->
                                    Box(Modifier.weight(1f)) {
                                        ActionButton(action.label, action.onClick,
                                            displayLabel = action.displayLabel, textSize = 10)
                                    }
                                }
                            }
                            if (rowIndex == 0) Spacer(Modifier.height(4.dp))
                        }
                    }
                    3 -> {
                        Row(Modifier.width(rowWidth), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            actions.take(2).forEach { action ->
                                Box(Modifier.weight(1f)) {
                                    ActionButton(action.label, action.onClick,
                                        displayLabel = action.displayLabel, textSize = 10)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.width(rowWidth * 0.58f)) {
                            ActionButton(actions[2].label, actions[2].onClick, textSize = 10)
                        }
                    }
                    else -> actions.forEachIndexed { index, action ->
                        Box(Modifier.width(rowWidth)) {
                            ActionButton(action.label, action.onClick,
                                displayLabel = action.displayLabel, textSize = 10)
                        }
                        if (index < actions.lastIndex) Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun formatSummary(format: MatchFormat): String {
    val matchType = if (format.matchType == MatchType.SINGLES) "Singles" else "Doubles"
    val decidingSet = if (format.decidingSet == DecidingSet.FULL_SET) "Full" else "MTB"
    return "$matchType · BO${format.length.sets} · $decidingSet"
}

@Composable
private fun SettingsScreen(initial: MatchFormat, onSave: (MatchFormat) -> Unit, onCancel: () -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val actionWidth = maxWidth * 0.62f
        val viewportHeight = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = viewportHeight)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Match\nSettings",
                    modifier = Modifier.wrapContentWidth(),
                    color = accent,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                Spacer(Modifier.height(9.dp))
                Label("MATCH FORMAT", 10, bold = true, color = muted)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.width(actionWidth), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f)) {
                        SettingChoice("Singles", draft.matchType == MatchType.SINGLES,
                            displayLabel = "SINGLES") {
                            draft = draft.copy(matchType = MatchType.SINGLES)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        SettingChoice("Doubles", draft.matchType == MatchType.DOUBLES,
                            displayLabel = "DOUBLES") {
                            draft = draft.copy(matchType = MatchType.DOUBLES)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Label("MATCH LENGTH", 10, bold = true, color = muted)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.width(actionWidth), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f)) {
                        SettingChoice("Best of 3", draft.length == MatchLength.BEST_OF_3,
                            displayLabel = "3 SETS") {
                            draft = draft.copy(length = MatchLength.BEST_OF_3)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        SettingChoice("Best of 5", draft.length == MatchLength.BEST_OF_5,
                            displayLabel = "5 SETS") {
                            draft = draft.copy(length = MatchLength.BEST_OF_5)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Label("DECIDING SET", 10, bold = true, color = muted)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.width(actionWidth), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f)) {
                        SettingChoice("Full deciding set", draft.decidingSet == DecidingSet.FULL_SET,
                            displayLabel = "FULL SET") {
                            draft = draft.copy(decidingSet = DecidingSet.FULL_SET)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        SettingChoice("10-point match tiebreak",
                            draft.decidingSet == DecidingSet.MATCH_TIEBREAK_10,
                            displayLabel = "10-PT TB") {
                            draft = draft.copy(decidingSet = DecidingSet.MATCH_TIEBREAK_10)
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Label(if (draft.decidingSet == DecidingSet.FULL_SET) "7-point TB at 6–6"
                    else "At 1–1 / 2–2 · win by 2", 10, color = muted)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.width(actionWidth), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f)) {
                        ActionButton("SAVE SETTINGS", { onSave(draft) },
                            displayLabel = "✓", textSize = 18)
                    }
                    Box(Modifier.weight(1f)) {
                        ActionButton("CANCEL", onCancel, displayLabel = "×",
                            textSize = 18, fillColor = opponentColor)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SettingChoice(
    label: String,
    selected: Boolean,
    displayLabel: String = label,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().height(40.dp).clip(shape)
                .background(if (selected) youColor else Color(0xFF18272A))
                .border(1.dp, if (selected) youColor else muted.copy(alpha = 0.45f), shape),
            contentAlignment = Alignment.Center,
        ) { Label(displayLabel, 10, bold = selected,
            color = if (selected) ink else Color.White) }
    }
}

@Composable
private fun ScoringScreen(score: MatchScore, firstServer: Player?, events: String,
    onScore: (Player) -> Unit, onUndo: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val server = firstServer?.let { ServeOrder.currentServer(score, it) }
    var previousEvents by remember { mutableStateOf(events) }
    var pulseColor by remember { mutableStateOf(muted) }
    val scale = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    val headerFlash = remember { Animatable(0f) }
    val gameScale = remember { Animatable(1f) }
    var gamePulsePlayer by remember { mutableStateOf<Player?>(null) }
    LaunchedEffect(events) {
        if (events != previousEvents) {
            val before = ScoringEngine.replay(previousEvents, score.format)
            val added = events.length > previousEvents.length
            val changedPlayerCode = if (added) events.lastOrNull() else previousEvents.lastOrNull()
            pulseColor = when {
                !added -> muted
                events.lastOrNull() == Player.YOU.code -> youColor
                else -> opponentColor
            }
            previousEvents = events
            gamePulsePlayer = null
            gameScale.snapTo(1f)
            coroutineScope {
                launch {
                    scale.snapTo(1f)
                    scale.animateTo(1.08f, tween(130))
                    scale.animateTo(1f, tween(220))
                }
                launch {
                    flash.snapTo(1f)
                    flash.animateTo(0f, tween(350))
                }
                if (before.sets != score.sets) launch {
                    headerFlash.snapTo(1f)
                    headerFlash.animateTo(0f, tween(350))
                }
                if (before.sets != score.sets) launch {
                    gamePulsePlayer = Player.entries.firstOrNull { it.code == changedPlayerCode }
                    gameScale.animateTo(1.13f, tween(140))
                    gameScale.animateTo(1f, tween(210))
                    gamePulsePlayer = null
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(background)) {
        val screenHeight = maxHeight
        val compact = screenHeight < 250.dp
        val smallText = screenHeight < 205.dp
        val breakMode = score.isTieBreak || score.isMatchTieBreak
        val completedSetCount = if (score.isMatchTieBreak) score.sets.size else score.sets.lastIndex
        val completedSets = score.sets.take(completedSetCount)
        val pointHint = when {
            score.isMatchTieBreak -> "MTB POINTS"
            score.isTieBreak -> "TB POINTS"
            else -> if (smallText) "TAP" else "TAP TO SCORE"
        }

        val baseBorder = if (breakMode) accent else Color(0xFF677573)
        Column(
            Modifier.fillMaxSize().padding(vertical = if (compact) 2.dp else 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Label(
                when {
                    score.isMatchTieBreak -> "MATCH TIEBREAK"
                    score.isTieBreak -> "TIEBREAK"
                    else -> "SET ${score.sets.size} OF ${score.format.length.sets}"
                },
                if (breakMode) 12 else 10,
                bold = breakMode,
                color = if (breakMode) accent else lerp(muted, accent, headerFlash.value),
                modifier = Modifier.padding(top = if (compact) 0.dp else 2.dp),
            )
            if (completedSets.isNotEmpty()) {
                Text(
                    text = completedSetSummaryText(score),
                    modifier = Modifier.padding(top = if (compact) 0.dp else 2.dp).fillMaxWidth(0.86f)
                        .semantics { contentDescription = "Completed set scores" },
                    color = muted,
                    fontSize = if (completedSets.size > 2) 8.sp else 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Row(
                Modifier.fillMaxWidth(0.76f).height(if (compact) 20.dp else 22.dp).testTag("score:players"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CurrentGameLabel(
                        score, Player.YOU, server,
                        if (gamePulsePlayer == Player.YOU) gameScale.value else 1f,
                        gamePulsePlayer == Player.YOU,
                    )
                }
                Box(Modifier.height(18.dp).width(1.dp).background(muted.copy(alpha = 0.45f)))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CurrentGameLabel(
                        score, Player.OPPONENT, server,
                        if (gamePulsePlayer == Player.OPPONENT) gameScale.value else 1f,
                        gamePulsePlayer == Player.OPPONENT,
                    )
                }
            }

            Spacer(Modifier.height(screenHeight * 0.03f))

            Box(
                Modifier.fillMaxWidth(0.80f)
                    .weight(1f)
                    .scale(scale.value).clip(RoundedCornerShape(25.dp))
                    .background(if (breakMode) Color(0xFF174638) else Color(0xFF202124))
                    .border(if (flash.value > 0f) 2.dp else 1.dp,
                        lerp(baseBorder, pulseColor, flash.value), RoundedCornerShape(25.dp))
                    .testTag("score:point-panel"),
                contentAlignment = Alignment.Center,
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    ScoreHalf(Player.YOU, score.pointValue(Player.YOU), pointHint, youColor, smallText,
                        Modifier.weight(1f).fillMaxHeight().testTag("score:you-target")) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onScore(Player.YOU)
                    }
                    Box(Modifier.width(1.dp).fillMaxHeight().background(baseBorder))
                    ScoreHalf(Player.OPPONENT, score.pointValue(Player.OPPONENT), pointHint, opponentColor, smallText,
                        Modifier.weight(1f).fillMaxHeight().testTag("score:opponent-target")) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onScore(Player.OPPONENT)
                    }
                }
            }

            Spacer(Modifier.height(screenHeight * 0.03f))

            UndoButton(onUndo)
            Spacer(Modifier.height(screenHeight * 0.065f))
        }
    }
}

@Composable
private fun UndoButton(onUndo: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(0.38f)) {
    Box(
        modifier.height(48.dp).clickable(onClick = onUndo)
            .semantics { contentDescription = "Undo last point" }
            .testTag("score:undo"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF151B1E))
                .border(1.dp, muted.copy(alpha = 0.45f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("score:undo-visual"),
            contentAlignment = Alignment.Center,
        ) { Label("↶  UNDO", 9, bold = true, color = muted) }
    }
}

@Composable
private fun CurrentGameLabel(score: MatchScore, player: Player, server: Player?, scoreScale: Float,
    recordingGame: Boolean) {
    val label = if (player == Player.YOU) "YOU" else "OPP"
    val gamesOrSets = if (score.isMatchTieBreak) score.setsWon(player) else score.currentSet.games(player)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (player == server) {
            Image(
                painter = painterResource(R.drawable.ic_tennis_ball),
                contentDescription = if (player == Player.YOU) "You serving" else "Opponent serving",
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(3.dp))
        }
        Text(
            text = "$label $gamesOrSets",
            modifier = Modifier.scale(scoreScale).semantics {
                if (recordingGame) stateDescription = "Game recorded for ${if (player == Player.YOU) "You" else "Opponent"}"
            },
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

internal fun completedSetSummaryText(score: MatchScore): AnnotatedString = buildAnnotatedString {
    val completedCount = if (score.isMatchTieBreak) score.sets.size else score.sets.lastIndex
    for (index in 0 until completedCount) {
        val set = score.sets[index]
        if (index > 0) append("  ·  ")
        if (completedCount == 1) append("SET ${index + 1} · ") else append("S${index + 1} ")
        val youWon = set.you > set.opponent
        val opponentWon = set.opponent > set.you
        withStyle(SpanStyle(
            fontWeight = if (youWon) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (youWon) accent else Color.White,
        )) { append(set.you.toString()) }
        append("–")
        withStyle(SpanStyle(
            fontWeight = if (opponentWon) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (opponentWon) accent else Color.White,
        )) { append(set.opponent.toString()) }
    }
}

@Composable
private fun ScoreHalf(player: Player, value: String, hint: String, color: Color,
    compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val label = if (player == Player.YOU) "YOU" else "OPP"
    Box(
        modifier.clickable(onClick = onClick).semantics {
            contentDescription = "Score point for ${if (player == Player.YOU) "You" else "Opponent"}"
            stateDescription = "$value points"
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Label("$label +", 12, bold = true, color = color)
            Label(value, if (compact) 26 else if (value.length > 2) 31 else 37, bold = true,
                modifier = Modifier.testTag(if (player == Player.YOU) "score:you-point" else "score:opponent-point"))
            Label(hint, if (compact) 10 else 8, bold = true, color = color)
        }
    }
}

@Composable
private fun CompleteScreen(
    score: MatchScore,
    onUndo: () -> Unit,
    onHome: () -> Unit,
) {
    val isYouWinner = score.winner == Player.YOU
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val actionWidth = maxWidth * 0.60f
        val undoWidth = maxWidth * 0.38f
        Column(
            Modifier.fillMaxSize().background(background).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(0.25f).height(2.dp).background(accent))
            Spacer(Modifier.height(6.dp))
            Label("MATCH COMPLETE", 11, bold = true, color = muted)
            Spacer(Modifier.height(8.dp))
            Label(if (isYouWinner) "YOU WIN" else "OPPONENT WINS",
                if (isYouWinner) 25 else 19, bold = true, color = accent)
            Spacer(Modifier.height(8.dp))
            FinalScoreTable(score)
            Spacer(Modifier.height(10.dp))
            UndoButton(onUndo, Modifier.width(undoWidth))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(actionWidth)) {
                ActionButton("HOME", onHome, textSize = 10)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FinalScoreTable(score: MatchScore) {
    val columnCount = score.sets.size + if (score.isMatchTieBreak) 1 else 0
    Column(
        Modifier.fillMaxWidth(if (columnCount >= 4) 0.90f else 0.76f)
            .clip(RoundedCornerShape(14.dp)).background(Color(0xFF24322F))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (score.isMatchTieBreak) {
            Row(Modifier.fillMaxWidth().height(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(28.dp))
                score.sets.forEach { _ -> Spacer(Modifier.weight(1f)) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Label("MTB", 8, bold = true, color = muted)
                }
            }
        }
        listOf(Player.YOU, Player.OPPONENT).forEach { player ->
            val setValues = score.sets.map { it.games(player) }
            val matchTieBreakPoints = if (player == Player.YOU) score.youPoints else score.opponentPoints
            val description = "${if (player == Player.YOU) "You" else "Opponent"} final scores: " +
                setValues.joinToString(", ") +
                if (score.isMatchTieBreak) ", MTB $matchTieBreakPoints" else ""
            val rowColor = if (score.winner == player) accent else Color.White
            Row(
                Modifier.fillMaxWidth().height(19.dp)
                    .semantics(mergeDescendants = true) { contentDescription = description },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
                    Label(if (player == Player.YOU) "YOU" else "OPP", 9,
                        bold = true, color = rowColor)
                }
                setValues.forEach { value ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Label(value.toString(), 11, bold = true, color = rowColor)
                    }
                }
                if (score.isMatchTieBreak) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Label(matchTieBreakPoints.toString(), 11, bold = true, color = rowColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(history: List<MatchResult>, onHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Match\nHistory",
            modifier = Modifier.wrapContentWidth(),
            color = accent,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.height(5.dp))
        if (history.isEmpty()) Label("No completed matches", 12)
        history.forEach { result ->
            val date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(result.id))
            Label("$date · ${if (result.winner == Player.YOU) "YOU WIN" else "OPP WINS"}", 12)
            Label(result.sets, 11)
            Spacer(Modifier.height(5.dp))
        }
        ActionButton("HOME", onHome)
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, widthFraction: Float = 1f,
    displayLabel: String = label, textSize: Int = 12, fillColor: Color = youColor) {
    Box(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth(widthFraction).height(40.dp)
                .clip(RoundedCornerShape(20.dp)).background(fillColor),
            contentAlignment = Alignment.Center,
        ) { Label(displayLabel, textSize, bold = true, color = ink) }
    }
}

@Composable
private fun Label(
    text: String,
    size: Int,
    bold: Boolean = false,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

private fun setSummary(score: MatchScore): String =
    score.sets.joinToString("  ") { "${it.you}-${it.opponent}" }
