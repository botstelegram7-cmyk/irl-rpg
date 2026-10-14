package com.irlrpg.app.domain.model

import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import java.time.LocalDate

/** Convenience: the quest's tracking style as a typed enum. */
val com.irlrpg.app.data.local.entity.QuestEntity.trackingKindEnum: TrackingKind
    get() = TrackingKind.fromName(trackingKind)

/** A quest together with the progress recorded for the relevant period. */
data class QuestWithProgress(
    val quest: QuestEntity,
    val progress: Long,
    val category: CategoryEntity?,
    val completions: Int = 0,
    /** Multi-step quests: required steps and how many are finished this period. */
    val stepsTotal: Int = 0,
    val stepsCompleted: Int = 0,
    val periodKey: String = "",
    /** Ids of the steps already satisfied in this period. */
    val completedStepIds: Set<Long> = emptySet(),
) {
    val questKind: com.irlrpg.app.domain.model.QuestKind
        get() = com.irlrpg.app.domain.model.QuestKind.fromName(quest.questKind)

    val isMultiStep: Boolean
        get() = questKind == com.irlrpg.app.domain.model.QuestKind.MultiStep && stepsTotal > 0

    /** Target expressed in the quest's own terms (steps for multi-step quests). */
    val target: Long
        get() = if (isMultiStep) stepsTotal.toLong() else quest.targetAmount

    val fraction: Float
        get() = when {
            isMultiStep -> if (stepsTotal <= 0) 0f else stepsCompleted.toFloat() / stepsTotal.toFloat()
            quest.trackingKindEnum == TrackingKind.Milestone -> if (progress > 0L) 1f else 0f
            target <= 0L -> 0f
            else -> (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        }

    val isComplete: Boolean
        get() = when {
            isMultiStep -> stepsTotal > 0 && stepsCompleted >= stepsTotal
            quest.trackingKindEnum == TrackingKind.Milestone -> progress > 0L
            else -> target > 0L && progress >= target
        }

    val remaining: Long get() = (target - progress).coerceAtLeast(0L)

    val isLongTerm: Boolean
        get() = questKind == com.irlrpg.app.domain.model.QuestKind.LongTerm

    /** Days left until the quest's deadline, if it has one. */
    val daysLeft: Long?
        get() = quest.deadlineEpochDay?.let { it - java.time.LocalDate.now().toEpochDay() }

    val isOverdue: Boolean
        get() = !isComplete && (daysLeft?.let { it < 0 } ?: false)
}

/** Player state resolved from total XP. */
data class PlayerSnapshot(
    val stats: PlayerStatsEntity,
    val level: Int,
    val rankTitle: String,
    val rankEmoji: String,
    val xpIntoLevel: Int,
    val xpForCurrentLevel: Int,
    val nextLevelXp: Int,
    val progress: Float,
)

/** Aggregated metrics for one day. */
data class DayMetrics(
    val date: LocalDate,
    val xp: Int,
    val completions: Int,
    val minutes: Int,
    val units: Long,
) {
    val hasActivity: Boolean get() = completions > 0
}

/** One bar in the weekly summary. */
data class WeekDay(
    val date: LocalDate,
    val label: String,
    val xp: Int,
    val completions: Int,
    val minutes: Int,
)

/** Per-category rollup used by analytics and dashboards. */
data class CategoryBreakdown(
    val categoryId: Long?,
    val name: String,
    val emoji: String,
    val accentId: String,
    val minutes: Int,
    val completions: Int,
    val xp: Int,
    val units: Long,
)

/** Totals across the whole recorded history. */
data class LifetimeTotals(
    val totalXp: Int,
    val totalCompletions: Int,
    val totalMinutes: Int,
    val activeDays: Int,
    val questsCreated: Int,
    val firstDay: LocalDate?,
)

data class StreakInfo(
    val current: Int,
    val longest: Int,
    val activeToday: Boolean,
)

/** Everything the dashboard needs, assembled from real recorded rows only. */
data class DashboardState(
    val player: PlayerSnapshot,
    val today: DayMetrics,
    val week: List<WeekDay>,
    val weekXp: Int,
    val weekCompletions: Int,
    val weekMinutes: Int,
    val streak: StreakInfo,
    val todayQuests: List<QuestWithProgress>,
    val recentCompletions: List<RecentCompletion>,
    val lifetime: LifetimeTotals,
    /** Multi-step quest definitions, keyed by quest id. */
    val stepsByQuest: Map<Long, List<com.irlrpg.app.data.local.entity.QuestStepEntity>> = emptyMap(),
) {
    val plannedToday: Int get() = todayQuests.size
    val completedToday: Int get() = todayQuests.count { it.isComplete }

    fun stepsFor(questId: Long): List<com.irlrpg.app.data.local.entity.QuestStepEntity> =
        stepsByQuest[questId].orEmpty()
}

data class RecentCompletion(
    val questTitle: String,
    val emoji: String,
    val xp: Int,
    val amountLabel: String,
    val date: LocalDate,
    val levelUp: Boolean,
)

/** Analytics screen payload. All values are derived from recorded logs. */
data class AnalyticsState(
    val today: DayMetrics,
    val yesterday: DayMetrics,
    val week: List<WeekDay>,
    val weekXp: Int,
    val weekCompletions: Int,
    val weekMinutes: Int,
    val lastWeekXp: Int,
    val monthXp: Int,
    val monthCompletions: Int,
    val monthMinutes: Int,
    val yearXp: Int,
    val categories: List<CategoryBreakdown>,
    val streak: StreakInfo,
    val lifetime: LifetimeTotals,
    val readingMinutes: Int,
    val readingPages: Long,
    val completionRate: Float,
    val consistencyRate: Float,
) {
    val weekOverWeekDelta: Int get() = weekXp - lastWeekXp
}
