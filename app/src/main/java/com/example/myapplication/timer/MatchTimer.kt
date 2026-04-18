package com.example.myapplication.timer

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Reusable deadline-based countdown timer with TTS and tone support.
 * Owned by a ViewModel — call [release] in onCleared().
 */
class MatchTimer(
    application: Application,
    private val scope: CoroutineScope
) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var beepTone: ToneGenerator? = null
    private var alarmTone: ToneGenerator? = null

    private var timerJob: Job? = null
    private var deadlineMs: Long = 0L
    private var lastBeepSec: Int = -1
    var announced1Min: Boolean = false

    /** Called every time the displayed second changes. */
    var onTick: ((secondsRemaining: Int) -> Unit)? = null

    /** Whether to run round-style announcements (1-min warning, 10-sec beeps). */
    var announceEnabled: Boolean = true

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
        beepTone = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) { null }
        alarmTone = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) { null }
    }

    fun start(durationSec: Int, onComplete: () -> Unit) {
        cancel()
        announced1Min = false
        lastBeepSec = -1
        deadlineMs = SystemClock.elapsedRealtime() + durationSec * 1_000L

        timerJob = scope.launch {
            var lastEmitted = -1
            while (isActive) {
                val remainingMs = (deadlineMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                // Ceiling division so the displayed second only decrements when we truly cross
                // a boundary (matches the 5→4→…→1→0 countdown behaviour).
                val remainingSec = ((remainingMs + 999) / 1_000).toInt()

                if (remainingSec != lastEmitted) {
                    lastEmitted = remainingSec
                    onTick?.invoke(remainingSec)
                    if (remainingSec != lastBeepSec) {
                        lastBeepSec = remainingSec
                        if (announceEnabled) checkAnnouncements(remainingSec)
                    }
                }

                if (remainingSec <= 0) break

                // Sleep exactly until the next displayed-second transition:
                // the next tick fires when remainingMs drops below (remainingSec-1)*1000.
                val nextBoundaryMs = remainingMs - (remainingSec - 1) * 1_000L
                delay(nextBoundaryMs.coerceAtLeast(1L))
            }
            if (isActive) onComplete()
        }
    }

    /** Pause and return the number of seconds that were remaining. */
    fun pause(): Int {
        val remaining = ((deadlineMs - SystemClock.elapsedRealtime() + 999) / 1_000)
            .toInt()
            .coerceAtLeast(0)
        cancel()
        return remaining
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }

    val isRunning: Boolean get() = timerJob?.isActive == true

    private fun checkAnnouncements(remaining: Int) {
        if (remaining == 60 && !announced1Min) {
            announced1Min = true
            speak("1 minute remaining.")
        }
        if (remaining <= 10) {
            playBeep()
        }
    }

    fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun playBeep() {
        beepTone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun playAlarm() {
        alarmTone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
    }

    fun release() {
        cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        beepTone?.release()
        beepTone = null
        alarmTone?.release()
        alarmTone = null
    }
}
