package com.example.myapplication.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen() {
    val now by rememberCurrentTime()

    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    }

    val currentTime = now.format(timeFormatter)
    val currentDate = now.format(dateFormatter)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Clock + date — true centre of screen
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(AccentCyan, AccentMid, AccentPurple)
                    ),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OrbitronFamily,
                    letterSpacing = 4.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentDate,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = OrbitronFamily,
                color = TextTertiary,
                letterSpacing = 2.sp
            )
        }

        // Brand logo — positioned above the centred clock
        Image(
            painter = painterResource(id = R.drawable.darklogo),
            contentDescription = "Mota-vation Logo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .size(140.dp)
                .alpha(0.6f),
            contentScale = ContentScale.Fit
        )
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