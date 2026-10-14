package com.irlrpg.app.domain.game

/**
 * Level progression.
 *
 * Requirement for level L -> L+1 is `90 + 60 * L` XP, so early levels feel
 * quick and later ones ask for real consistency:
 *
 * ```
 * L1 -> L2 : 150 XP          L10 -> L11 : 690 XP
 * L2 -> L3 : 210 XP          L20 -> L21 : 1290 XP
 * ```
 *
 * Everything here is pure arithmetic so it can be unit tested and reused by the
 * UI without touching the database.
 */
object LevelCurve {

    const val START_LEVEL = 1
    const val MAX_LEVEL = 60

    /** XP needed to move from [level] to the next level. */
    fun xpToAdvance(level: Int): Int {
        val safe = level.coerceIn(START_LEVEL, MAX_LEVEL)
        return 90 + 60 * safe
    }

    /** Cumulative XP required to reach [level] from zero. Level 1 == 0 XP. */
    fun totalXpForLevel(level: Int): Int {
        val target = level.coerceAtLeast(START_LEVEL)
        if (target == START_LEVEL) return 0
        // sum over l in 1..target-1 of (90 + 60l) = (target-1) * (90 + 30 * target)
        return (target - 1) * (90 + 30 * target)
    }

    /** Level achieved with [totalXp] lifetime XP. */
    fun levelForTotalXp(totalXp: Int): Int {
        var level = START_LEVEL
        while (level < MAX_LEVEL && totalXp >= totalXpForLevel(level + 1)) {
            level++
        }
        return level
    }

    /** XP earned inside the current level. */
    fun xpIntoLevel(totalXp: Int): Int = (totalXp - totalXpForLevel(levelForTotalXp(totalXp))).coerceAtLeast(0)

    /** XP span of the current level (0 when the cap is reached). */
    fun xpSpanOfLevel(totalXp: Int): Int {
        val level = levelForTotalXp(totalXp)
        return if (level >= MAX_LEVEL) 0 else xpToAdvance(level)
    }

    /** 0f..1f progress towards the next level. */
    fun progressFraction(totalXp: Int): Float {
        val span = xpSpanOfLevel(totalXp)
        if (span <= 0) return 1f
        return (xpIntoLevel(totalXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }

    /** Rank title for a level. Titles deliberately stay profession-neutral. */
    fun rankTitle(level: Int): String = when {
        level >= 50 -> "Ascendant"
        level >= 40 -> "Legend"
        level >= 30 -> "Mythic"
        level >= 25 -> "Grandmaster"
        level >= 20 -> "Master"
        level >= 16 -> "Trailblazer"
        level >= 13 -> "Pathfinder"
        level >= 10 -> "Veteran"
        level >= 7 -> "Adept"
        level >= 5 -> "Apprentice"
        level >= 3 -> "Seeker"
        else -> "Wanderer"
    }

    fun rankEmoji(level: Int): String = when {
        level >= 50 -> "🌌"
        level >= 40 -> "🐉"
        level >= 30 -> "🌟"
        level >= 25 -> "👑"
        level >= 20 -> "🏆"
        level >= 16 -> "🔥"
        level >= 13 -> "🧗"
        level >= 10 -> "⚔️"
        level >= 7 -> "🔮"
        level >= 5 -> "🪶"
        level >= 3 -> "🧭"
        else -> "🌱"
    }
}
