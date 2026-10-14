package com.irlrpg.app.domain.game

import com.irlrpg.app.domain.model.TrackingKind
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Every XP number the app shows is produced here, from recorded log rows only.
 *
 * Anti-abuse rules (Step 1 requirement: never reward merely opening the app or
 * tapping "complete" repeatedly):
 *  1. XP is granted per log entry, and only for the portion that moves the quest
 *     from its previous progress towards the target.
 *  2. Once a quest is complete for its period, further logs add progress history
 *     but **zero** XP.
 *  3. The completion bonus is granted exactly once per period.
 *  4. There is no XP for launching the app, for navigating, or for creating
 *     quests.
 */
object XpEngine {

    /** Progress unit credited per log — used for time/count/performance styles. */
    fun xpForProgress(
        kind: TrackingKind,
        baseXp: Int,
        target: Long,
        progressBefore: Long,
        amount: Long,
    ): Int {
        if (amount <= 0L || baseXp <= 0) return 0
        if (kind == TrackingKind.Milestone) {
            return if (progressBefore <= 0L) baseXp else 0
        }
        if (target <= 0L) return baseXp
        val credited = amount.coerceAtMost((target - progressBefore).coerceAtLeast(0L))
        if (credited <= 0L) return 0
        val raw = baseXp.toDouble() * (credited.toDouble() / target.toDouble())
        return max(1, raw.roundToInt())
    }

    /** One-off bonus for reaching the target within the period. */
    fun completionBonus(baseXp: Int): Int = (baseXp / 2).coerceAtLeast(10)

    /** True when this log entry is the one that completes the period. */
    fun completesTarget(kind: TrackingKind, target: Long, progressBefore: Long, amount: Long): Boolean {
        if (kind == TrackingKind.Milestone) return progressBefore <= 0L && amount > 0L
        if (target <= 0L) return false
        return progressBefore < target && progressBefore + amount >= target
    }

    /**
     * XP a check-in button should preview: the progress credit the user would
     * earn for a single tap of "log one step".
     */
    fun perStepXp(kind: TrackingKind, baseXp: Int, target: Long): Int = when {
        kind == TrackingKind.Milestone -> baseXp
        target <= 1L -> baseXp
        else -> max(1, (baseXp.toDouble() / target.toDouble()).roundToInt())
    }
}
