package com.irlrpg.app.domain.game

import com.irlrpg.app.domain.model.BossTemplate

/**
 * Optional starting points for boss challenges.
 *
 * Templates are only a convenience: choosing one creates a normal, fully editable
 * challenge row owned by the user (title, objectives, deadline and reward can all
 * be changed afterwards). Nothing here is ever applied silently.
 */
object BossTemplates {

    val all: List<BossTemplate> = listOf(
        BossTemplate(
            code = "finish_a_book",
            title = "Finish a Novel",
            description = "Read a whole book within a set window — steady pages beat one heroic session.",
            emoji = "📖",
            accentId = "sky",
            rewardXp = 500,
            durationDays = 30,
            objectiveTitles = listOf(
                "Read a total of 600 pages" to 600L,
                "Log reading on 15 different days" to 15L,
                "Finish the book (milestone quest)" to 1L,
            ),
        ),
        BossTemplate(
            code = "coding_project",
            title = "Ship a Project",
            description = "Take a build from first commit to something you can show people.",
            emoji = "🚀",
            accentId = "lavender",
            rewardXp = 700,
            durationDays = 45,
            objectiveTitles = listOf(
                "Deep work sessions (hours)" to 30L,
                "Complete 5 project milestones" to 5L,
                "Write the project README" to 1L,
            ),
        ),
        BossTemplate(
            code = "reading_target",
            title = "Reading Target",
            description = "A page or session target to hit before the deadline.",
            emoji = "📚",
            accentId = "mint",
            rewardXp = 350,
            durationDays = 21,
            objectiveTitles = listOf(
                "Read 300 pages" to 300L,
                "Read on 12 days" to 12L,
            ),
        ),
        BossTemplate(
            code = "certification",
            title = "Professional Certification",
            description = "Prepare consistently and clear the milestone that matters for your career.",
            emoji = "🎓",
            accentId = "butter",
            rewardXp = 900,
            durationDays = 60,
            objectiveTitles = listOf(
                "Study sessions (hours)" to 40L,
                "Complete the practice exam" to 1L,
                "Complete the certification milestone" to 1L,
            ),
        ),
        BossTemplate(
            code = "fitness_challenge",
            title = "Multi-Week Fitness Challenge",
            description = "Build a training habit that survives busy weeks.",
            emoji = "🏋️",
            accentId = "peach",
            rewardXp = 600,
            durationDays = 42,
            objectiveTitles = listOf(
                "Complete 24 training sessions" to 24L,
                "Log 12 hours of activity" to 12L,
                "Train in 4 consecutive weeks" to 4L,
            ),
        ),
    )

    fun byCode(code: String?): BossTemplate? = all.firstOrNull { it.code == code }
}
