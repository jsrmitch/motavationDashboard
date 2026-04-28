package com.motavation.dashboard.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for user-configurable settings that persist across
 * launches (currently just the global mute flag). State is exposed via
 * [StateFlow] so screens can observe live changes, and is backed by
 * [SharedPreferences] for simple, dependency-free persistence.
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _muted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    /** Synchronous accessor — handy for audio paths where collecting a flow would be overkill. */
    val isMuted: Boolean get() = _muted.value

    fun setMuted(muted: Boolean) {
        if (_muted.value == muted) return
        _muted.value = muted
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
    }

    companion object {
        private const val PREFS_NAME = "motavation_settings"
        private const val KEY_MUTED = "muted"

        @Volatile private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
    }
}
