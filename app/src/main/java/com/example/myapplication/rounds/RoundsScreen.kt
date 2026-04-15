package com.example.myapplication.rounds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.*

private val RestGreen = Color(0xFF4CAF50)

@Composable
fun RoundsScreen(viewModel: RoundsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSession()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state.phase) {
            SessionPhase.IDLE -> ConfigScreen(
                state = state,
                onRoundsChange = viewModel::setTotalRounds,
                onRoundDurationChange = viewModel::setRoundDurationSec,
                onRestDurationChange = viewModel::setRestDurationSec,
                onStart = viewModel::startSession
            )
            SessionPhase.COMPLETE -> CompleteScreen(onReset = viewModel::stopSession)
            else -> ActiveScreen(state = state, onStop = viewModel::stopSession)
        }
    }
}

// ---------------------------------------------------------------------------
// Config screen
// ---------------------------------------------------------------------------

@Composable
private fun ConfigScreen(
    state: RoundsUiState,
    onRoundsChange: (Int) -> Unit,
    onRoundDurationChange: (Int) -> Unit,
    onRestDurationChange: (Int) -> Unit,
    onStart: () -> Unit
) {
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
            .padding(horizontal = 40.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ROUND TIMER",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AccentCyan,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        ConfigRow(
            label = "Rounds",
            value = "${state.totalRounds}",
            onDecrement = { onRoundsChange(state.totalRounds - 1) },
            onIncrement = { onRoundsChange(state.totalRounds + 1) }
        )

        ConfigDivider()

        ConfigRow(
            label = "Round",
            value = formatDuration(state.roundDurationSec),
            onDecrement = { onRoundDurationChange(state.roundDurationSec - 30) },
            onIncrement = { onRoundDurationChange(state.roundDurationSec + 30) }
        )

        ConfigDivider()

        ConfigRow(
            label = "Rest",
            value = formatDuration(state.restDurationSec),
            onDecrement = { onRestDurationChange(state.restDurationSec - 5) },
            onIncrement = { onRestDurationChange(state.restDurationSec + 5) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        TvButton(
            text = "START",
            icon = Icons.Default.PlayArrow,
            accentColor = AccentCyan,
            onClick = onStart
        )
    }
}

@Composable
private fun ConfigDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.05f))
    )
}

// ---------------------------------------------------------------------------
// Active screen — full page timer during COUNTDOWN / ROUND / REST
// ---------------------------------------------------------------------------

@Composable
private fun ActiveScreen(state: RoundsUiState, onStop: () -> Unit) {
    val timerColor = when (state.phase) {
        SessionPhase.COUNTDOWN -> AccentPurple
        SessionPhase.ROUND -> AccentCyan
        SessionPhase.REST -> RestGreen
        else -> TextTertiary
    }

    val phaseLabel = when (state.phase) {
        SessionPhase.COUNTDOWN -> "GET READY"
        SessionPhase.ROUND -> "ROUND ${state.currentRound} / ${state.totalRounds}"
        SessionPhase.REST -> "REST"
        else -> ""
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = phaseLabel,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = timerColor,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val mm = state.secondsRemaining / 60
        val ss = state.secondsRemaining % 60
        val timerText = if (state.phase == SessionPhase.COUNTDOWN) {
            "${state.secondsRemaining}"
        } else {
            String.format("%02d:%02d", mm, ss)
        }

        Text(
            text = timerText,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (state.phase == SessionPhase.COUNTDOWN) 180.sp else 120.sp,
            color = timerColor,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        RoundDots(
            total = state.totalRounds,
            current = state.currentRound,
            phase = state.phase
        )

        Spacer(modifier = Modifier.height(40.dp))

        TvButton(
            text = "STOP",
            icon = Icons.Default.Stop,
            accentColor = Color(0xFFEF5350),
            onClick = onStop
        )
    }
}

// ---------------------------------------------------------------------------
// Complete screen
// ---------------------------------------------------------------------------

@Composable
private fun CompleteScreen(onReset: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GREAT WORK",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            color = AccentCyan,
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "All rounds finished",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        TvButton(
            text = "NEW SESSION",
            icon = Icons.Default.PlayArrow,
            accentColor = AccentCyan,
            onClick = onReset
        )
    }
}

// ---------------------------------------------------------------------------
// Shared components
// ---------------------------------------------------------------------------

@Composable
private fun RoundDots(total: Int, current: Int, phase: SessionPhase) {
    val isComplete = phase == SessionPhase.COMPLETE
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 1..total) {
            val dotColor = when {
                isComplete -> AccentCyan
                i < current -> AccentCyan
                i == current -> when (phase) {
                    SessionPhase.REST -> RestGreen
                    SessionPhase.COUNTDOWN -> AccentPurple
                    else -> AccentCyan
                }
                else -> Color.White.copy(alpha = 0.1f)
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun ConfigRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = TextTertiary,
            letterSpacing = 2.sp,
            modifier = Modifier.width(110.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        StepperButton(icon = Icons.Default.Remove, onClick = onDecrement)

        Text(
            text = value,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = TextPrimary,
            modifier = Modifier.width(160.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        StepperButton(icon = Icons.Default.Add, onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(100),
        label = "stepperScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AccentCyan else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(100),
        label = "stepperBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) AccentCyan.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(100),
        label = "stepperBg"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) AccentCyan else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TvButton(
    text: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(100),
        label = "btnScale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.22f else 0.08f,
        animationSpec = tween(100),
        label = "btnBgAlpha"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.9f else 0.25f,
        animationSpec = tween(100),
        label = "btnBorderAlpha"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = bgAlpha))
            .border(1.5.dp, accentColor.copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 36.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) accentColor else TextPrimary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (isFocused) accentColor else TextPrimary,
            letterSpacing = 3.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return if (m > 0 && s > 0) "${m}m ${s}s"
    else if (m > 0) "${m}m"
    else "${s}s"
}