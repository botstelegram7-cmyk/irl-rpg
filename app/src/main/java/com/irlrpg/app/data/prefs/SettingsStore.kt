package com.irlrpg.app.data.prefs

import android.content.Context
import com.irlrpg.app.domain.game.LevelRules

/**
 * Tiny preferences store for flags and progression rules.
 *
 * Game state (XP, logs, achievements) always lives in the database — this store
 * only holds settings, so nothing here can corrupt progression.
 */
class SettingsStore(
    context: Context,
    prefsName: String = DEFAULT_PREFS,
) {

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    var isSeeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED, value).apply()

    /** True once achievements have been reconciled with existing history. */
    var achievementsPrimed: Boolean
        get() = prefs.getBoolean(KEY_ACHIEVEMENTS_PRIMED, false)
        set(value) = prefs.edit().putBoolean(KEY_ACHIEVEMENTS_PRIMED, value).apply()

    /** Configurable level thresholds; changing them never alters earned XP. */
    var levelBase: Int
        get() = prefs.getInt(KEY_LEVEL_BASE, LevelRules.DEFAULT_BASE)
        set(value) = prefs.edit().putInt(KEY_LEVEL_BASE, value).apply()

    var levelStep: Int
        get() = prefs.getInt(KEY_LEVEL_STEP, LevelRules.DEFAULT_STEP)
        set(value) = prefs.edit().putInt(KEY_LEVEL_STEP, value).apply()

    fun levelRules(): LevelRules = LevelRules(base = levelBase, step = levelStep)

    fun resetLevelRules() {
        prefs.edit()
            .putInt(KEY_LEVEL_BASE, LevelRules.DEFAULT_BASE)
            .putInt(KEY_LEVEL_STEP, LevelRules.DEFAULT_STEP)
            .apply()
    }

    companion object {
        const val DEFAULT_PREFS = "irl_rpg_settings"
        private const val KEY_SEEDED = "seeded_v1"
        private const val KEY_ACHIEVEMENTS_PRIMED = "achievements_primed_v2"
        private const val KEY_LEVEL_BASE = "level_base"
        private const val KEY_LEVEL_STEP = "level_step"
    }
}
