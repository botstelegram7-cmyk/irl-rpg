package com.irlrpg.app.domain.model

/** Case- and separator-insensitive name match, shared by the Step 2 enums. */
internal fun Enum<*>.matches(name: String): Boolean =
    this.name.replace("_", "").lowercase() == name.replace("_", "").trim().lowercase()

/** Shape of a quest, independent of how it is tracked. */
enum class QuestKind(val label: String, val emoji: String, val helper: String) {
    Standard("Single goal", "🎯", "One target, tracked and reset by its schedule."),
    MultiStep("Multi-step", "🪜", "Break a bigger goal into ordered steps that each track progress."),
    LongTerm("Long-term challenge", "🏔️", "Accumulates over weeks with an optional deadline."),
    ;

    companion object {
        /**
         * Tolerant of both spellings that can appear in the database: the Kotlin
         * enum name ("MultiStep") and the upper-snake form used by SQL defaults
         * ("MULTI_STEP"). Rows written by either convention resolve correctly.
         */
        fun fromName(name: String): QuestKind =
            entries.firstOrNull { it.matches(name) } ?: Standard
    }
}

/** Difficulty is a labelling + suggested-reward aid; stored XP always wins. */
enum class QuestDifficulty(
    val label: String,
    val emoji: String,
    val accentId: String,
    val suggestedXp: Int,
) {
    Easy("Easy", "🌿", "mint", 40),
    Medium("Medium", "⚡", "sky", 60),
    Hard("Hard", "🔥", "peach", 110),
    Epic("Epic", "👑", "butter", 180),
    ;

    companion object {
        fun fromName(name: String): QuestDifficulty =
            entries.firstOrNull { it.matches(name) } ?: Medium
    }
}

/** How a skill node becomes available. */
enum class SkillUnlockRule(val label: String, val helper: String) {
    Root("Root node", "Always available — the start of a branch."),
    ParentMastery("Parent mastery", "Requires the parent skill to reach a mastery threshold."),
    ParentProgress("Parent progress", "Requires the parent skill to be at least half mastered."),
    TotalXp("Lifetime XP", "Requires a set amount of lifetime XP, regardless of branch."),
    ;

    companion object {
        fun fromName(name: String): SkillUnlockRule = entries.firstOrNull { it.matches(name) } ?: Root
    }
}

/** Derived state of a skill node — never stored, always computed from real XP. */
enum class SkillNodeState(val label: String) {
    Locked("Locked"),
    Available("Available"),
    InProgress("In progress"),
    Mastered("Mastered"),
}

/** An achievement definition is code, its earned state is data. */
data class AchievementDef(
    val code: String,
    val name: String,
    val description: String,
    val emoji: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val target: Int,
    /** Cosmetic unlocked by earning it, if any. */
    val unlocksCosmetic: String? = null,
)

enum class AchievementCategory(val label: String, val emoji: String) {
    Progress("Progress", "📈"),
    Consistency("Consistency", "🔥"),
    Mastery("Mastery", "🌟"),
    Challenges("Challenges", "🐉"),
    Creator("Creator", "🛠️"),
}

enum class AchievementTier(val label: String, val emoji: String, val accentId: String) {
    Bronze("Bronze", "🥉", "peach"),
    Silver("Silver", "🥈", "sky"),
    Gold("Gold", "🥇", "butter"),
}

/** Achievement with live progress computed from recorded data. */
data class AchievementView(
    val def: AchievementDef,
    val progress: Int,
    val earnedEpochDay: Long?,
) {
    val earned: Boolean get() = earnedEpochDay != null
    val fraction: Float
        get() = if (def.target <= 0) 0f else (progress.toFloat() / def.target.toFloat()).coerceIn(0f, 1f)
}

/** Cosmetic catalogue entry (frames + titles). Purely visual. */
data class CosmeticItem(
    val code: String,
    val name: String,
    val emoji: String,
    val accentId: String,
    val kind: CosmeticKind,
    val unlockHint: String,
) {
    val isTitle: Boolean get() = kind == CosmeticKind.Title
}

enum class CosmeticKind { Frame, Title }

/** A boss challenge plus its resolved objectives. */
data class BossObjectiveView(
    val objective: com.irlrpg.app.data.local.entity.BossObjectiveEntity,
    val progress: Long,
    val linkedQuestTitle: String?,
) {
    val target: Long get() = objective.target
    val fraction: Float
        get() = if (target <= 0L) 0f else (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val isComplete: Boolean get() = target > 0L && progress >= target
}

data class BossProgress(
    val boss: com.irlrpg.app.data.local.entity.BossChallengeEntity,
    val objectives: List<BossObjectiveView>,
) {
    val required: List<BossObjectiveView> get() = objectives.filter { it.objective.isRequired }
    val completedRequired: Int get() = required.count { it.isComplete }
    val fraction: Float
        get() = if (required.isEmpty()) 0f else completedRequired.toFloat() / required.size.toFloat()
    val allRequiredComplete: Boolean get() = required.isNotEmpty() && required.all { it.isComplete }
    val isCompleted: Boolean
        get() = boss.status == com.irlrpg.app.data.local.entity.BossChallengeEntity.STATUS_COMPLETED
    val daysLeft: Long?
        get() = boss.deadlineEpochDay?.let { it - java.time.LocalDate.now().toEpochDay() }
    val isOverdue: Boolean
        get() = !isCompleted && (daysLeft?.let { it < 0 } ?: false)
}

/** A preset challenge a user can start in one tap (always stored as their own row). */
data class BossTemplate(
    val code: String,
    val title: String,
    val description: String,
    val emoji: String,
    val accentId: String,
    val rewardXp: Int,
    val durationDays: Int,
    val objectiveTitles: List<Pair<String, Long>>,
)

/** One row of the global completion history. */
data class CompletionHistoryEntry(
    val logId: Long,
    val questId: Long,
    val questTitle: String,
    val emoji: String,
    val amountLabel: String,
    val xp: Int,
    val date: java.time.LocalDate,
    val levelUp: Boolean,
)
