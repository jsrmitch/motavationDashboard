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
        val dur = _state.value.restDurationSec
        _state.update { it.copy(phase = SessionPhase.REST, secondsRemaining = dur) }
        timer.speak("Rest.")
        timer.start(dur) {
            _state.update { it.copy(currentRound = it.currentRound + 1) }
            beginCountdown()
        }
    }

    // --- Cleanup ---

    override fun onCleared() {
        timer.release()
        super.onCleared()
    }
}
