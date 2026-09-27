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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
                        Action("START MATCH") { onShow(Page.SELECT_SERVER_NEW) },
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
                    CompleteScreen(score, { onShow(Page.SELECT_SERVER_NEW) }, onUndo) { onShow(Page.START) }
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

private data class Action(val label: String, val onClick: () -> Unit)

@Composable
private fun FirstServerMenu(subtitle: String, onChoose: (Player) -> Unit, onCancel: () -> Unit) {
    MenuScreen(
        title = "FIRST SERVER",
        subtitle = subtitle,
        actions = listOf(
            Action("YOU SERVE FIRST") { onChoose(Player.YOU) },
            Action("OPP SERVES FIRST") { onChoose(Player.OPPONENT) },
            Action("CANCEL", onCancel),
        ),
    )
}

@Composable
private fun MenuScreen(title: String, subtitle: String, actions: List<Action>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Label(title, if (title.length > 12) 16 else 19, bold = true, color = accent)
        Spacer(Modifier.height(8.dp))
        Label(subtitle, 12)
        Spacer(Modifier.height(12.dp))
        actions.forEach { action ->
            ActionButton(action.label, action.onClick, if (actions.size >= 3) 0.82f else 1f)
            Spacer(Modifier.height(6.dp))
        }
    }
}

private fun formatSummary(format: MatchFormat): String =
    "Best of ${format.length.sets} · " +
        if (format.decidingSet == DecidingSet.FULL_SET) "Full set" else "Match TB"

@Composable
private fun SettingsScreen(initial: MatchFormat, onSave: (MatchFormat) -> Unit, onCancel: () -> Unit) {
    var draft by remember(initial) { mutableStateOf(initial) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label("MATCH SETTINGS", 16, bold = true, color = accent)
        Spacer(Modifier.height(9.dp))
        Label("FORMAT", 10, bold = true, color = muted)
        Label("Singles", 13, bold = true)
        Spacer(Modifier.height(10.dp))
        Label("MATCH LENGTH", 10, bold = true, color = muted)
        Spacer(Modifier.height(5.dp))
        SettingChoice("Best of 3", draft.length == MatchLength.BEST_OF_3) {
            draft = draft.copy(length = MatchLength.BEST_OF_3)
        }
        Spacer(Modifier.height(5.dp))
        SettingChoice("Best of 5", draft.length == MatchLength.BEST_OF_5) {
            draft = draft.copy(length = MatchLength.BEST_OF_5)
        }
        Spacer(Modifier.height(12.dp))
        Label("DECIDING SET", 10, bold = true, color = muted)
        Spacer(Modifier.height(5.dp))
        SettingChoice("Full deciding set", draft.decidingSet == DecidingSet.FULL_SET) {
            draft = draft.copy(decidingSet = DecidingSet.FULL_SET)
        }
        Spacer(Modifier.height(5.dp))
        SettingChoice("10-point match tiebreak", draft.decidingSet == DecidingSet.MATCH_TIEBREAK_10) {
            draft = draft.copy(decidingSet = DecidingSet.MATCH_TIEBREAK_10)
        }
        Spacer(Modifier.height(5.dp))
        Label(if (draft.decidingSet == DecidingSet.FULL_SET) "7-point TB at 6–6"
            else "At 1–1 / 2–2 · win by 2", 10, color = muted)
        Spacer(Modifier.height(14.dp))
        ActionButton("SAVE SETTINGS", { onSave(draft) })
        Spacer(Modifier.height(6.dp))
        ActionButton("CANCEL", onCancel)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SettingChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(shape)
            .background(if (selected) youColor else Color(0xFF18272A))
            .border(1.dp, if (selected) youColor else muted, shape)
            .clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Label(if (selected) "✓  $label" else label, 11, bold = selected,
        color = if (selected) ink else Color.White) }
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

            Box(
                Modifier.fillMaxWidth(0.48f).height(48.dp)
                    .clip(RoundedCornerShape(50)).border(1.dp, muted, RoundedCornerShape(50))
                    .clickable(onClick = onUndo).semantics { contentDescription = "Undo last point" }
                    .testTag("score:undo"),
                contentAlignment = Alignment.Center,
            ) { Label("↶  UNDO", 10, bold = true) }
            Spacer(Modifier.height(screenHeight * 0.065f))
        }
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
    onStart: () -> Unit,
    onUndo: () -> Unit,
    onHome: () -> Unit,
) {
    val isYouWinner = score.winner == Player.YOU
    val finalScore = score.sets.joinToString("  ") { "${it.you}–${it.opponent}" } +
        if (score.isMatchTieBreak) "  [${score.youPoints}–${score.opponentPoints}]" else ""
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
        Label("FINAL SCORE", 9, bold = true, color = muted)
        Label(finalScore,
            when { score.sets.size >= 4 -> 10; score.sets.size == 3 || score.isMatchTieBreak -> 12; else -> 15 },
            bold = true)
        Spacer(Modifier.height(10.dp))
        CompletionAction("NEW MATCH", onStart, Modifier.fillMaxWidth(0.80f).height(48.dp),
            youColor, ink, 13)
        Spacer(Modifier.height(6.dp))
        CompletionAction("UNDO LAST POINT", onUndo, Modifier.fillMaxWidth(0.80f).height(48.dp),
            Color(0xFF283034), opponentColor, 10)
        Spacer(Modifier.height(6.dp))
        CompletionAction("HOME", onHome, Modifier.fillMaxWidth(0.55f).height(48.dp),
            background, muted, 10)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CompletionAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    fill: Color,
    textColor: Color,
    textSize: Int,
) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(fill).clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Label(label, textSize, bold = true, color = textColor) }
}

@Composable
private fun HistoryScreen(history: List<MatchResult>, onHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label("MATCH HISTORY", 16, bold = true, color = accent)
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
private fun ActionButton(label: String, onClick: () -> Unit, widthFraction: Float = 1f) {
    Box(
        Modifier.fillMaxWidth(widthFraction).height(48.dp).clip(RoundedCornerShape(24.dp))
            .background(youColor).clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Label(label, 12, bold = true, color = ink) }
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
