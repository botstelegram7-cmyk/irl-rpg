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

    /** Default-rules delegate — keeps Step 1 call sites source-compatible. */
    private val default = LevelProgression(LevelRules.Default)

    fun xpToAdvance(level: Int): Int = default.xpToAdvance(level)

    fun totalXpForLevel(level: Int): Int = default.totalXpForLevel(level)

    fun levelForTotalXp(totalXp: Int): Int = default.levelForTotalXp(totalXp)

    fun xpIntoLevel(totalXp: Int): Int = default.xpIntoLevel(totalXp)

    fun xpSpanOfLevel(totalXp: Int): Int = default.xpSpanOfLevel(totalXp)

    fun progressFraction(totalXp: Int): Float = default.progressFraction(totalXp)

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
