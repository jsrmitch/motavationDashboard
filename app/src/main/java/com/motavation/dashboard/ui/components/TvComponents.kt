package com.motavation.dashboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motavation.dashboard.ui.theme.OrbitronFamily
import com.motavation.dashboard.ui.theme.TextPrimary

@Composable
fun TvButton(
    text: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
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
