package com.motavation.dashboard.rounds

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motavation.dashboard.timer.MatchTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SessionPhase {
    IDLE,
    COUNTDOWN,
    ROUND,
    REST,
    COMPLETE
}

@Immutable
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

    private val timer = MatchTimer(application, viewModelScope).apply {
        onTick = { remaining -> _state.update { it.copy(secondsRemaining = remaining) } }
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
        timer.cancel()
        timer.stopSpeaking()
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
        timer.announced1Min = false
        timer.announceEnabled = false
        _state.update { it.copy(phase = SessionPhase.COUNTDOWN, secondsRemaining = 5) }
        timer.speak("Round ${_state.value.currentRound}. Get ready.")
        timer.start(5) { beginRound() }
    }

    private fun beginRound() {
        timer.announced1Min = false
        timer.announceEnabled = true
        val dur = _state.value.roundDurationSec
        _state.update { it.copy(phase = SessionPhase.ROUND, secondsRemaining = dur) }
        timer.speak("Go!")
        timer.start(dur) { onRoundFinished() }
    }

    private fun onRoundFinished() {
        timer.playAlarm()
        val s = _state.value
        if (s.currentRound >= s.totalRounds) {
            _state.update { it.copy(phase = SessionPhase.COMPLETE, secondsRemaining = 0) }
            timer.speak("Workout complete. Great job!")
            return
        }
        beginRest()
    }

    private fun beginRest() {
        timer.announced1Min = false
        timer.announceEnabled = false
        val totalRest = _state.value.restDurationSec
        // The rest timer runs for the full configured duration, but the final
        // 5 seconds flip the phase to COUNTDOWN so the UI shows the get-ready
        // countdown during those seconds (instead of adding 5 extra seconds
        // after the rest ends). If rest is <= 5s, we stay in COUNTDOWN the
        // whole time.
        val nextRound = _state.value.currentRound + 1
        _state.update {
            it.copy(
                phase = if (totalRest > 5) SessionPhase.REST else SessionPhase.COUNTDOWN,
                secondsRemaining = totalRest,
                currentRound = nextRound
            )
        }
        if (totalRest > 5) {
            timer.speak("Rest.")
        } else {
            timer.speak("Round $nextRound. Get ready.")
        }
        var countdownAnnounced = totalRest <= 5
        timer.onTick = { remaining ->
            if (!countdownAnnounced && remaining <= 5) {
                countdownAnnounced = true
                timer.announceEnabled = true
                timer.speak("Round $nextRound. Get ready.")
                _state.update { it.copy(phase = SessionPhase.COUNTDOWN, secondsRemaining = remaining) }
            } else {
                _state.update { it.copy(secondsRemaining = remaining) }
            }
        }
        timer.start(totalRest) {
            // Restore default onTick for subsequent phases.
            timer.onTick = { r -> _state.update { it.copy(secondsRemaining = r) } }
            beginRound()
        }
    }

    // --- Cleanup ---

    override fun onCleared() {
        timer.release()
        super.onCleared()
    }
}
