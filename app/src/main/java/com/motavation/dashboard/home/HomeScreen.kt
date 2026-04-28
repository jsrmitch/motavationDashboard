package com.motavation.dashboard.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.motavation.dashboard.R
import com.motavation.dashboard.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val screenHeight = maxHeight
        val density = LocalDensity.current

        // Precompute density-based sizes once per layout pass; the clock
        // ticks every second but these values only change if the viewport
        // resizes, so we avoid recomputing them on every tick.
        val sizes = remember(screenHeight, density) {
            with(density) {
                HomeSizes(
                    logoEdgeInset = screenHeight * 0.04f,
                    timeFontSize = (screenHeight * 0.225f).toSp()
                )
            }
        }

        // Stable gradient TextStyle — avoids re-allocating Brush/TextStyle
        // every time the clock sub-composable recomposes.
        val timeStyle = remember(sizes.timeFontSize) {
            TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(AccentCyan, AccentMid, AccentPurple)
                ),
                fontSize = sizes.timeFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitronFamily,
                letterSpacing = 4.sp,
                // Tabular figures so each digit takes the same advance width
                // — without this the clock shifts horizontally as numerals
                // change (e.g. a "1" is narrower than an "8" in Orbitron).
                fontFeatureSettings = "tnum"
            )
        }

        // Giant ambient watermark — sits behind the clock as a branded
        // backdrop. matchParentSize lets ContentScale.Fit scale the logo
        // to the largest dimension that fits the full screen, then we
        // pad inwards slightly so it doesn't kiss the bezels.
        Image(
            painter = painterResource(id = R.drawable.darklogo),
            contentDescription = "Mota-vation Logo",
            modifier = Modifier
                .matchParentSize()
                .padding(sizes.logoEdgeInset)
                .alpha(0.40f),
            contentScale = ContentScale.Fit
        )

        // Isolating the ticking state inside a child composable means
        // only that subtree recomposes each second — the logo and parent
        // layout measurement above are skipped.
        Clock(
            timeStyle = timeStyle,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Immutable
private data class HomeSizes(
    val logoEdgeInset: Dp,
    val timeFontSize: TextUnit
)

@Composable
private fun Clock(
    timeStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val now by rememberCurrentTime()
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    }

    // Orbitron doesn't ship the "tnum" OpenType feature, so digits have
    // different advance widths (a "1" is narrower than an "8"). We measure
    // the widest digit once and render each digit in a fixed-width slot so
    // the clock never reflows horizontally as the seconds tick.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitWidth = remember(timeStyle, density) {
        val widest = (0..9).maxOf { d ->
            measurer.measure(d.toString(), timeStyle).size.width
        }
        with(density) { widest.toDp() }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        now.format(timeFormatter).forEach { ch ->
            if (ch.isDigit()) {
                Box(
                    modifier = Modifier.width(digitWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ch.toString(), style = timeStyle)
                }
            } else {
                Text(text = ch.toString(), style = timeStyle)
            }
        }
    }
}

@Composable
fun rememberCurrentTime(): State<LocalDateTime> {
    return produceState(initialValue = LocalDateTime.now()) {
        while (isActive) {
            val now = LocalDateTime.now()
            value = now
            // Sleep until the start of the next wall-clock second so the
            // displayed time stays in sync and we don't drift into showing
            // the same HH:mm:ss twice (or skipping a second).
            val msToNextSecond = 1_000L - (now.nano / 1_000_000L) % 1_000L
            delay(msToNextSecond)
        }
    }
}
