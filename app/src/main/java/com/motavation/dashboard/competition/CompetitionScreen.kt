package com.motavation.dashboard.competition

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motavation.dashboard.ui.components.TvButton
import com.motavation.dashboard.ui.theme.*

private val PlayerOneColor = AccentCyan
private val PlayerTwoColor = AccentPurple
private val StopRed = Color(0xFFEF5350)
private val WarningAmber = Color(0xFFFFB300)

private val PointActions = listOf(
    ScoringAction.TAKEDOWN,
    ScoringAction.SWEEP,
    ScoringAction.GUARD_PASS,
    ScoringAction.MOUNT,
    ScoringAction.BACK_CONTROL
)

/** Cheap MM:SS formatter — avoids a Formatter/Locale allocation on every tick. */
private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val mTens = m / 10
    val mOnes = m % 10
    val sTens = s / 10
    val sOnes = s % 10
    return StringBuilder(5)
        .append('0' + mTens).append('0' + mOnes)
        .append(':')
        .append('0' + sTens).append('0' + sOnes)
        .toString()
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

@Composable
fun CompetitionScreen(viewModel: CompetitionViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.resetMatch() }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state.phase) {
            MatchPhase.IDLE -> IdleScreen(onStart = viewModel::startMatch)
            MatchPhase.COMPLETE -> CompleteScreen(state = state, onReset = viewModel::resetMatch)
            else -> ActiveMatchScreen(
                state = state,
                onPause = viewModel::pauseMatch,
                onResume = viewModel::resumeMatch,
                onReset = viewModel::resetMatch,
                onScore = viewModel::score,
                onUndo = viewModel::undoLast
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Idle — pre-match start screen
// ---------------------------------------------------------------------------

@Composable
private fun IdleScreen(onStart: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .padding(horizontal = 48.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "COMPETITION",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AccentCyan,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "5 : 0 0",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = TextTertiary,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "IBJJF Rules",
            fontSize = 14.sp,
            color = TextTertiary,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        TvButton(
            text = "START MATCH",
            icon = Icons.Default.PlayArrow,
            accentColor = AccentCyan,
            onClick = onStart,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

// ---------------------------------------------------------------------------
// Active match — three-column layout
// ---------------------------------------------------------------------------

@Composable
private fun ActiveMatchScreen(
    state: CompetitionUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onScore: (Competitor, ScoringAction) -> Unit,
    onUndo: (Competitor) -> Unit
) {
    val startFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { startFocus.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left — Competitor 1
        CompetitorPanel(
            modifier = Modifier.weight(1f),
            label = "PLAYER 1",
            score = state.scoreOne,
            accent = PlayerOneColor,
            competitor = Competitor.ONE,
            enabled = state.phase == MatchPhase.RUNNING || state.phase == MatchPhase.PAUSED,
            onScore = onScore,
            onUndo = onUndo,
            initialFocusRequester = if (state.phase == MatchPhase.RUNNING) startFocus else null
        )

        // Center — Timer & controls
        CenterPanel(
            modifier = Modifier.width(300.dp),
            state = state,
            onPause = onPause,
            onResume = onResume,
            onReset = onReset,
            initialFocusRequester = if (state.phase == MatchPhase.COUNTDOWN) startFocus else null
        )

        // Right — Competitor 2
        CompetitorPanel(
            modifier = Modifier.weight(1f),
            label = "PLAYER 2",
            score = state.scoreTwo,
            accent = PlayerTwoColor,
            competitor = Competitor.TWO,
            enabled = state.phase == MatchPhase.RUNNING || state.phase == MatchPhase.PAUSED,
            onScore = onScore,
            onUndo = onUndo
        )
    }
}

// ---------------------------------------------------------------------------
// Competitor scoring panel
// ---------------------------------------------------------------------------

@Composable
private fun CompetitorPanel(
    modifier: Modifier,
    label: String,
    score: CompetitorScore,
    accent: Color,
    competitor: Competitor,
    enabled: Boolean,
    onScore: (Competitor, ScoringAction) -> Unit,
    onUndo: (Competitor) -> Unit,
    initialFocusRequester: FocusRequester? = null
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Top section: label + score + chips ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = accent,
                letterSpacing = 3.sp
            )

            Text(
                text = "${score.points}",
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatChip(label = "ADV", value = score.advantages, color = accent)
                StatChip(label = "PEN", value = score.penalties, color = WarningAmber)
            }
        }

        // --- Middle section: scoring buttons ---
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in PointActions.indices step 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isFirst = i == 0
                    ScoreActionButton(
                        action = PointActions[i],
                        accent = accent,
                        enabled = enabled,
                        onClick = { onScore(competitor, PointActions[i]) },
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (isFirst && initialFocusRequester != null)
                                    Modifier.focusRequester(initialFocusRequester)
                                else Modifier
                            )
                    )
                    if (i + 1 < PointActions.size) {
                        ScoreActionButton(
                            action = PointActions[i + 1],
                            accent = accent,
                            enabled = enabled,
                            onClick = { onScore(competitor, PointActions[i + 1]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // UNDO sits next to BACK
                        UndoButton(
                            accent = accent,
                            enabled = enabled,
                            onClick = { onUndo(competitor) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScoreActionButton(
                    action = ScoringAction.ADVANTAGE,
                    accent = accent,
                    enabled = enabled,
                    onClick = { onScore(competitor, ScoringAction.ADVANTAGE) },
                    modifier = Modifier.weight(1f)
                )
                ScoreActionButton(
                    action = ScoringAction.PENALTY,
                    accent = WarningAmber,
                    enabled = enabled,
                    onClick = { onScore(competitor, ScoringAction.PENALTY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$value",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TextPrimary
        )
    }
}

// ---------------------------------------------------------------------------
// Center panel — timer + match controls
// ---------------------------------------------------------------------------

@Composable
private fun CenterPanel(
    modifier: Modifier,
    state: CompetitionUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    initialFocusRequester: FocusRequester? = null
) {
    val isCountdown = state.phase == MatchPhase.COUNTDOWN
    val isPaused = state.phase == MatchPhase.PAUSED

    val timerColor = when (state.phase) {
        MatchPhase.COUNTDOWN -> AccentPurple
        MatchPhase.PAUSED -> WarningAmber
        MatchPhase.RUNNING -> {
            if (state.secondsRemaining <= 30) StopRed else AccentCyan
        }
        else -> TextTertiary
    }

    val phaseLabel = when (state.phase) {
        MatchPhase.COUNTDOWN -> "GET READY"
        MatchPhase.RUNNING -> "MATCH"
        MatchPhase.PAUSED -> "PAUSED"
        else -> ""
    }

    val timerText = if (isCountdown) {
        state.secondsRemaining.toString()
    } else {
        formatMmSs(state.secondsRemaining)
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = phaseLabel,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = timerColor,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = timerText,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (isCountdown) 100.sp else 56.sp,
            color = timerColor,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.weight(1f))

        // Match controls
        if (!isCountdown) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isPaused) {
                    TvButton(
                        text = "RESUME",
                        icon = Icons.Default.PlayArrow,
                        accentColor = AccentCyan,
                        onClick = onResume,
                        modifier = if (initialFocusRequester != null)
                            Modifier.focusRequester(initialFocusRequester) else Modifier
                    )
                } else {
                    TvButton(
                        text = "PAUSE",
                        icon = Icons.Default.Pause,
                        accentColor = WarningAmber,
                        onClick = onPause
                    )
                }

                TvButton(
                    text = "RESET",
                    icon = Icons.Default.Refresh,
                    accentColor = StopRed,
                    onClick = onReset
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Score action button — TV-optimized focusable button
// ---------------------------------------------------------------------------

@Composable
private fun ScoreActionButton(
    action: ScoringAction,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(100),
        label = "scoreScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(100),
        label = "scoreBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accent.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(100),
        label = "scoreBorder"
    )
    val textColor = if (isFocused) accent else TextSecondary

    val pointsSuffix = when (action) {
        ScoringAction.ADVANTAGE, ScoringAction.PENALTY -> ""
        else -> " +${action.points}"
    }

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = action.shortLabel.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (pointsSuffix.isNotEmpty()) {
            Text(
                text = pointsSuffix,
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isFocused) accent else TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Undo button
// ---------------------------------------------------------------------------

@Composable
private fun UndoButton(
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(100),
        label = "undoScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) StopRed.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(100),
        label = "undoBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) StopRed.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(100),
        label = "undoBg"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Undo,
            contentDescription = "Undo last",
            tint = if (isFocused) StopRed else TextTertiary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "UNDO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) StopRed else TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Complete screen — results
// ---------------------------------------------------------------------------

@Composable
private fun CompleteScreen(state: CompetitionUiState, onReset: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val resultColor = when (state.result) {
        MatchResult.WIN_ONE -> PlayerOneColor
        MatchResult.WIN_TWO -> PlayerTwoColor
        MatchResult.DRAW -> WarningAmber
        null -> TextTertiary
    }

    val resultText = when (state.result) {
        MatchResult.WIN_ONE -> "PLAYER 1 WINS"
        MatchResult.WIN_TWO -> "PLAYER 2 WINS"
        MatchResult.DRAW -> "DRAW"
        null -> ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "MATCH OVER",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TextTertiary,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Final scores side by side
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            FinalScoreColumn(
                label = "PLAYER 1",
                score = state.scoreOne,
                accent = PlayerOneColor,
                isWinner = state.result == MatchResult.WIN_ONE
            )

            Text(
                text = "—",
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = TextTertiary
            )

            FinalScoreColumn(
                label = "PLAYER 2",
                score = state.scoreTwo,
                accent = PlayerTwoColor,
                isWinner = state.result == MatchResult.WIN_TWO
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = resultText,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = resultColor,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        TvButton(
            text = "NEW MATCH",
            icon = Icons.Default.PlayArrow,
            accentColor = AccentCyan,
            onClick = onReset,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Composable
private fun FinalScoreColumn(
    label: String,
    score: CompetitorScore,
    accent: Color,
    isWinner: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isWinner) accent else TextTertiary,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${score.points}",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 80.sp,
            color = if (isWinner) accent else TextPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip(label = "ADV", value = score.advantages, color = accent)
            StatChip(label = "PEN", value = score.penalties, color = WarningAmber)
        }
    }
}