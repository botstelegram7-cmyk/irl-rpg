package com.irlrpg.app.data.prefs

import android.content.Context

/**
 * Tiny preferences store for one-off flags (not for game state — XP and
 * progress always live in the database).
 *
 * [prefsName] exists so tests can use an isolated preferences file and stay
 * deterministic regardless of what the real application process is doing.
 */
class SettingsStore(
    context: Context,
    prefsName: String = DEFAULT_PREFS,
) {

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    var isSeeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED, value).apply()

    companion object {
        const val DEFAULT_PREFS = "irl_rpg_settings"
        private const val KEY_SEEDED = "seeded_v1"
    }
}
