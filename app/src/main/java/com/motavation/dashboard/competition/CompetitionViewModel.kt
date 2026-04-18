package com.motavation.dashboard.competition

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.motavation.dashboard.timer.MatchTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ---------------------------------------------------------------------------
// Scoring model
// ---------------------------------------------------------------------------

enum class ScoringAction(val label: String, val shortLabel: String, val points: Int) {
    TAKEDOWN("Takedown", "Takedown", 2),
    SWEEP("Sweep", "Sweep", 2),
    GUARD_PASS("Guard Pass", "Pass", 3),
    MOUNT("Mount", "Mount", 4),
    BACK_CONTROL("Back Control", "Back", 4),
    ADVANTAGE("Advantage", "Adv", 0),
    PENALTY("Penalty", "Pen", 0),
}

enum class Competitor { ONE, TWO }

@Immutable
data class ScoreEvent(
    val competitor: Competitor,
    val action: ScoringAction
)

@Immutable
data class CompetitorScore(
    val points: Int = 0,
    val advantages: Int = 0,
    val penalties: Int = 0
)

// ---------------------------------------------------------------------------
// Match phases
// ---------------------------------------------------------------------------

enum class MatchPhase {
    IDLE,
    COUNTDOWN,
    RUNNING,
    PAUSED,
    COMPLETE
}

enum class MatchResult { WIN_ONE, WIN_TWO, DRAW }

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

@Immutable
data class CompetitionUiState(
    val phase: MatchPhase = MatchPhase.IDLE,
    val secondsRemaining: Int = MATCH_DURATION_SEC,
    val scoreOne: CompetitorScore = CompetitorScore(),
    val scoreTwo: CompetitorScore = CompetitorScore(),
    val events: List<ScoreEvent> = emptyList(),
    val result: MatchResult? = null
) {
    companion object {
        const val MATCH_DURATION_SEC = 5 * 60
    }
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class CompetitionViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CompetitionUiState())
    val state: StateFlow<CompetitionUiState> = _state.asStateFlow()

    private val timer = MatchTimer(application, viewModelScope).apply {
        onTick = { remaining -> _state.update { it.copy(secondsRemaining = remaining) } }
    }

    private var pausedSecondsRemaining: Int = CompetitionUiState.MATCH_DURATION_SEC

    // --- Match controls ---

    fun startMatch() {
        if (_state.value.phase != MatchPhase.IDLE) return
        beginCountdown()
    }

    fun pauseMatch() {
        if (_state.value.phase != MatchPhase.RUNNING) return
        pausedSecondsRemaining = timer.pause()
        _state.update { it.copy(phase = MatchPhase.PAUSED) }
    }

    fun resumeMatch() {
        if (_state.value.phase != MatchPhase.PAUSED) return
        timer.announceEnabled = true
        _state.update { it.copy(phase = MatchPhase.RUNNING) }
        timer.start(pausedSecondsRemaining) { onMatchFinished() }
    }

    fun resetMatch() {
        timer.cancel()
        timer.stopSpeaking()
        _state.value = CompetitionUiState()
        pausedSecondsRemaining = CompetitionUiState.MATCH_DURATION_SEC
    }

    // --- Scoring ---

    fun score(competitor: Competitor, action: ScoringAction) {
        val phase = _state.value.phase
        if (phase != MatchPhase.RUNNING && phase != MatchPhase.PAUSED) return

        val event = ScoreEvent(competitor, action)
        _state.update { state ->
            val newEvents = state.events + event
            state.copy(
                events = newEvents,
                scoreOne = tally(newEvents, Competitor.ONE),
                scoreTwo = tally(newEvents, Competitor.TWO)
            )
        }
    }

    fun undoLast(competitor: Competitor) {
        _state.update { state ->
            val idx = state.events.indexOfLast { it.competitor == competitor }
            if (idx < 0) return@update state
            val newEvents = state.events.toMutableList().apply { removeAt(idx) }
            state.copy(
                events = newEvents,
                scoreOne = tally(newEvents, Competitor.ONE),
                scoreTwo = tally(newEvents, Competitor.TWO)
            )
        }
    }

    // --- Phase transitions ---

    private fun beginCountdown() {
        timer.announced1Min = false
        timer.announceEnabled = false
        _state.update { it.copy(phase = MatchPhase.COUNTDOWN, secondsRemaining = 5) }
        timer.speak("Competition match. Get ready.")
        timer.start(5) { beginMatch() }
    }

    private fun beginMatch() {
        timer.announced1Min = false
        timer.announceEnabled = true
        _state.update {
            it.copy(
                phase = MatchPhase.RUNNING,
                secondsRemaining = CompetitionUiState.MATCH_DURATION_SEC
            )
        }
        timer.speak("Fight!")
        timer.start(CompetitionUiState.MATCH_DURATION_SEC) { onMatchFinished() }
    }

    private fun onMatchFinished() {
        timer.playAlarm()
        val s = _state.value
        val result = determineResult(s.scoreOne, s.scoreTwo)
        _state.update { it.copy(phase = MatchPhase.COMPLETE, secondsRemaining = 0, result = result) }

        val announcement = when (result) {
            MatchResult.WIN_ONE -> "Match over. Competitor 1 wins."
            MatchResult.WIN_TWO -> "Match over. Competitor 2 wins."
            MatchResult.DRAW -> "Match over. It's a draw."
        }
        timer.speak(announcement)
    }

    // --- Scoring helpers ---

    private fun tally(events: List<ScoreEvent>, competitor: Competitor): CompetitorScore {
        var points = 0
        var advantages = 0
        var penalties = 0
        for (e in events) {
            if (e.competitor != competitor) continue
            when (e.action) {
                ScoringAction.ADVANTAGE -> advantages++
                ScoringAction.PENALTY -> penalties++
                else -> points += e.action.points
            }
        }
        return CompetitorScore(points, advantages, penalties)
    }

    private fun determineResult(one: CompetitorScore, two: CompetitorScore): MatchResult {
        // 1. Points
        if (one.points != two.points) {
            return if (one.points > two.points) MatchResult.WIN_ONE else MatchResult.WIN_TWO
        }
        // 2. Advantages
        if (one.advantages != two.advantages) {
            return if (one.advantages > two.advantages) MatchResult.WIN_ONE else MatchResult.WIN_TWO
        }
        // 3. Fewer penalties wins
        if (one.penalties != two.penalties) {
            return if (one.penalties < two.penalties) MatchResult.WIN_ONE else MatchResult.WIN_TWO
        }
        return MatchResult.DRAW
    }

    // --- Cleanup ---

    override fun onCleared() {
        timer.release()
        super.onCleared()
    }
}
