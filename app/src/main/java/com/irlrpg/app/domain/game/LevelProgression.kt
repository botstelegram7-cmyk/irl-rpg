package com.irlrpg.app.domain.game

/**
 * Configurable level thresholds.
 *
 * The Step 1 curve is `cost(L -> L+1) = base + step * L` with `base = 90` and
 * `step = 60`. Advanced settings let the player tune both, and because levels are
 * always *derived* from lifetime XP, changing the rules re-reads existing progress
 * instead of resetting it — total XP and every log row stay exactly as recorded.
 */
data class LevelRules(
    val base: Int = DEFAULT_BASE,
    val step: Int = DEFAULT_STEP,
) {
    val isValid: Boolean get() = base >= 10 && step >= 0

    companion object {
        const val DEFAULT_BASE = 90
        const val DEFAULT_STEP = 60
        val Default = LevelRules()
        const val MIN_BASE = 20
        const val MAX_BASE = 400
        const val MIN_STEP = 0
        const val MAX_STEP = 300
    }
}

/**
 * Level maths for a given [LevelRules]. `LevelCurve` (the Step 1 object) is kept
 * as a thin delegate to the default rules so existing call sites and tests keep
 * working unchanged.
 */
class LevelProgression(private val rules: LevelRules = LevelRules.Default) {

    val startLevel: Int get() = LevelCurve.START_LEVEL
    val maxLevel: Int get() = LevelCurve.MAX_LEVEL

    /** XP needed to move from [level] to the next level. */
    fun xpToAdvance(level: Int): Int {
        val safe = level.coerceIn(startLevel, maxLevel)
        return (rules.base + rules.step * safe).coerceAtLeast(10)
    }

    /** Cumulative XP required to reach [level] from zero (level 1 == 0 XP). */
    fun totalXpForLevel(level: Int): Int {
        val target = level.coerceAtLeast(startLevel)
        if (target == startLevel) return 0
        // sum over l in 1..target-1 of (base + step * l)
        val n = target - 1
        return n * rules.base + rules.step * (n * (n + 1)) / 2
    }

    fun levelForTotalXp(totalXp: Int): Int {
        var level = startLevel
        while (level < maxLevel && totalXp >= totalXpForLevel(level + 1)) level++
        return level
    }

    fun xpIntoLevel(totalXp: Int): Int =
        (totalXp - totalXpForLevel(levelForTotalXp(totalXp))).coerceAtLeast(0)

    fun xpSpanOfLevel(totalXp: Int): Int {
        val level = levelForTotalXp(totalXp)
        return if (level >= maxLevel) 0 else xpToAdvance(level)
    }

    fun progressFraction(totalXp: Int): Float {
        val span = xpSpanOfLevel(totalXp)
        if (span <= 0) return 1f
        return (xpIntoLevel(totalXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }
}
