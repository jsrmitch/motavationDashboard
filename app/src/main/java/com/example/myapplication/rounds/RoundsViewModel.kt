package com.example.myapplication.rounds

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class SessionPhase {
    IDLE,
    COUNTDOWN,
    ROUND,
    REST,
    COMPLETE
}

data class RoundsUiState(
    val totalRounds: Int = 4,
    val roundDurationSec: Int = 4 * 60,
    val restDurationSec: Int = 30,
    val phase: SessionPhase = SessionPhase.IDLE,
    val currentRound: Int = 1,
    val secondsRemaining: Int = 0
)

class RoundsViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(RoundsUiState())
    val state: StateFlow<RoundsUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var beepTone: ToneGenerator? = null
    private var alarmTone: ToneGenerator? = null

    // Deadline-based timer: tracks the wall-clock moment the phase ends
    private var deadlineMs: Long = 0L
    // Tracks which second we last beeped/announced on to avoid duplicates
    private var lastBeepSec: Int = -1
    private var announced1Min: Boolean = false

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

    // --- Configuration (only allowed in IDLE) ---

    fun setTotalRounds(value: Int) {
        if (_state.value.phase != SessionPhase.IDLE) return
        _state.update { it.copy(totalRounds = value.coerceIn(1, 20)) }
    }

    fun setRoundDurationSec(value: Int) {
        if (_state.value.phase != SessionPhase.IDLE) return
        _state.update { it.copy(roundDurationSec = value.coerceIn(30, 30 * 60)) }
    }

    fun setRestDurationSec(value: Int) {
        if (_state.value.phase != SessionPhase.IDLE) return
        _state.update { it.copy(restDurationSec = value.coerceIn(5, 5 * 60)) }
    }

    // --- Session controls ---

    fun startSession() {
        if (_state.value.phase != SessionPhase.IDLE) return
        _state.update { it.copy(currentRound = 1) }
        beginCountdown()
    }

    fun stopSession() {
        timerJob?.cancel()
        timerJob = null
        tts?.stop()
        _state.update {
            RoundsUiState(
                totalRounds = it.totalRounds,
                roundDurationSec = it.roundDurationSec,
                restDurationSec = it.restDurationSec
            )
        }
    }

    // --- Phase transitions ---

    private fun beginCountdown() {
        announced1Min = false
        lastBeepSec = -1
        _state.update { it.copy(phase = SessionPhase.COUNTDOWN, secondsRemaining = 5) }
        speak("Round ${_state.value.currentRound}. Get ready.")
        startTimer(5) { beginRound() }
    }

    private fun beginRound() {
        announced1Min = false
        lastBeepSec = -1
        val dur = _state.value.roundDurationSec
        _state.update { it.copy(phase = SessionPhase.ROUND, secondsRemaining = dur) }
        speak("Go!")
        startTimer(dur) { onRoundFinished() }
    }

    private fun onRoundFinished() {
        playAlarm()
        val s = _state.value
        if (s.currentRound >= s.totalRounds) {
            _state.update { it.copy(phase = SessionPhase.COMPLETE, secondsRemaining = 0) }
            speak("Workout complete. Great job!")
            return
        }
        beginRest()
    }

    private fun beginRest() {
        announced1Min = false
        lastBeepSec = -1
        val dur = _state.value.restDurationSec
        _state.update { it.copy(phase = SessionPhase.REST, secondsRemaining = dur) }
        speak("Rest.")
        startTimer(dur) {
            _state.update { it.copy(currentRound = it.currentRound + 1) }
            beginCountdown()
        }
    }

    // --- Core timer (deadline-based, drift-free) ---

    private fun startTimer(durationSec: Int, onComplete: () -> Unit) {
        timerJob?.cancel()
        deadlineMs = SystemClock.elapsedRealtime() + durationSec * 1_000L

        timerJob = viewModelScope.launch {
            while (isActive) {
                val remaining = ((deadlineMs - SystemClock.elapsedRealtime() + 999) / 1_000)
                    .toInt()
                    .coerceAtLeast(0)

                _state.update { it.copy(secondsRemaining = remaining) }

                if (remaining <= 0) break

                // Beep + announce only when the displayed second actually changes
                if (remaining != lastBeepSec) {
                    lastBeepSec = remaining
                    checkAnnouncements(remaining)
                }

                // Sleep briefly — just enough to catch the next second boundary
                val msUntilNextSec = (deadlineMs - SystemClock.elapsedRealtime()) % 1_000
                delay(msUntilNextSec.coerceIn(50, 1_000))
            }

            if (isActive) onComplete()
        }
    }

    private fun checkAnnouncements(remaining: Int) {
        if (_state.value.phase != SessionPhase.ROUND) return

        if (remaining == 60 && !announced1Min) {
            announced1Min = true
            speak("1 minute remaining.")
        }

        if (remaining <= 10) {
            playBeep()
        }
    }

    // --- Audio ---

    private fun playBeep() {
        beepTone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    private fun playAlarm() {
        alarmTone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
    }

    // --- TTS ---

    private fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    // --- Cleanup ---

    override fun onCleared() {
        timerJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
        beepTone?.release()
        beepTone = null
        alarmTone?.release()
        alarmTone = null
        super.onCleared()
    }
}
