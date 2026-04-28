package com.motavation.dashboard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lets screens report whether they are currently "busy" (e.g. a timer is
 * running). When no screen is busy and the user is idle, the shell can return
 * to the home screensaver.
 */
class IdleController {
    var busy: Boolean by mutableStateOf(false)
        private set

    private var busyCount: Int = 0

    fun acquire() {
        busyCount++
        busy = busyCount > 0
    }

    fun release() {
        busyCount = (busyCount - 1).coerceAtLeast(0)
        busy = busyCount > 0
    }
}

val LocalIdleController = compositionLocalOf { IdleController() }

/**
 * Reports that the user just interacted with the current screen (e.g. typed
 * into a TextField, toggled a switch). Shell-level listeners use this to
 * reset the auto-hide menu and auto-return-to-home timers — same signal that
 * hardware key events produce, just triggered from non-key sources.
 */
val LocalReportInteraction = compositionLocalOf<() -> Unit> { {} }

/**
 * Marks the current screen as busy while [isBusy] is true. Automatically
 * releases the claim when the composable leaves the composition.
 */
@Composable
fun ReportScreenBusy(isBusy: Boolean) {
    val controller = LocalIdleController.current
    DisposableEffect(controller, isBusy) {
        if (isBusy) {
            controller.acquire()
            onDispose { controller.release() }
        } else {
            onDispose { }
        }
    }
}
