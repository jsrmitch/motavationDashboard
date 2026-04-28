package com.motavation.dashboard.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motavation.dashboard.navigation.LocalReportInteraction
import com.motavation.dashboard.ui.theme.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repo = remember(context) { SettingsRepository.get(context) }
    val reportInteraction = LocalReportInteraction.current

    val muted by repo.muted.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        SettingsHeader()

        Column(
            modifier = Modifier.widthIn(max = 880.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AudioSection(
                muted = muted,
                onToggle = {
                    repo.setMuted(!muted)
                    reportInteraction()
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun SettingsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "SETTINGS",
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = TextPrimary,
            letterSpacing = 8.sp
        )
        // Brand gradient underline — same cyan→purple as the home clock.
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(AccentCyan, AccentMid, AccentPurple)
                    )
                )
        )
        Text(
            text = "Dashboard configuration",
            fontSize = 13.sp,
            color = TextTertiary,
            letterSpacing = 2.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Audio / mute section
// ---------------------------------------------------------------------------

@Composable
private fun AudioSection(
    muted: Boolean,
    onToggle: () -> Unit
) {
    GlassCard {
        SectionHeader(
            title = "AUDIO",
            subtitle = "Mute or enable beeps, alarms, and voice playback."
        )

        Spacer(modifier = Modifier.height(20.dp))

        MuteToggleRow(muted = muted, onToggle = onToggle)
    }
}

@Composable
private fun MuteToggleRow(
    muted: Boolean,
    onToggle: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.015f else 1f,
        animationSpec = tween(120),
        label = "muteScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) AccentCyan else Color.White.copy(alpha = 0.10f),
        animationSpec = tween(140),
        label = "muteBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(140),
        label = "muteBorderW"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) AccentCyan.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.02f),
        animationSpec = tween(140),
        label = "muteBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon in a soft chip
        val iconTint by animateColorAsState(
            targetValue = if (muted) AccentPurple else AccentCyan,
            animationSpec = tween(180),
            label = "muteIconTint"
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f))
                .border(1.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff
                              else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mute all sounds",
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (muted) "Beeps, alarms, and voice are silenced"
                       else "Beeps, alarms, and voice will play",
                fontSize = 12.sp,
                color = TextTertiary,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        PillSwitch(checked = muted)
    }
}

// ---------------------------------------------------------------------------
// Reusable building blocks
// ---------------------------------------------------------------------------

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.025f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(horizontal = 32.dp, vertical = 28.dp),
        content = content
    )
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vertical gradient accent stripe
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(AccentCyan, AccentPurple))
                )
        )
        Text(
            text = title,
            fontFamily = OrbitronFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary,
            letterSpacing = 4.sp
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = subtitle,
        fontSize = 13.sp,
        color = TextTertiary,
        lineHeight = 19.sp,
        letterSpacing = 0.5.sp
    )
}

/**
 * iOS-style pill switch with a brand-gradient track when active.
 */
@Composable
private fun PillSwitch(checked: Boolean) {
    val trackBrush = remember(checked) {
        if (checked) Brush.horizontalGradient(listOf(AccentCyan, AccentPurple))
        else Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f))
        )
    }
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(180),
        label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .width(50.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackBrush)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset, top = 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
