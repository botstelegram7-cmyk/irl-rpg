package com.irlrpg.app.ui.navigation

import com.irlrpg.app.ui.design.ClayDestination
import com.irlrpg.app.ui.design.ClayGlyphType
import com.irlrpg.app.ui.theme.ClayPalette

/** Route table for the whole app. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val QUESTS = "quests"
    const val SKILLS = "skills"
    const val ANALYTICS = "analytics"
    const val PROFILE = "profile"

    // ---- Step 2 full-screen routes ----
    const val SKILL_TREE = "skill_tree"
    const val CHALLENGES = "challenges"
    const val ACHIEVEMENTS = "achievements"
    const val HISTORY = "history"
    const val CUSTOMIZE = "customize"
    const val PROGRESSION = "progression"

    const val QUEST_EDITOR = "quest_editor?questId={questId}"
    const val QUEST_DETAIL = "quest_detail/{questId}"

    fun questEditor(questId: Long? = null): String = "quest_editor?questId=${questId ?: NEW_QUEST}"
    fun questDetail(questId: Long): String = "quest_detail/$questId"

    const val NEW_QUEST = -1L
}

val topLevelDestinations = listOf(
    ClayDestination(Routes.DASHBOARD, "Home", ClayGlyphType.Home, ClayPalette.Lavender),
    ClayDestination(Routes.QUESTS, "Quests", ClayGlyphType.Quests, ClayPalette.Mint),
    ClayDestination(Routes.SKILLS, "Skills", ClayGlyphType.Skills, ClayPalette.Butter),
    ClayDestination(Routes.ANALYTICS, "Stats", ClayGlyphType.Analytics, ClayPalette.Sky),
    ClayDestination(Routes.PROFILE, "Profile", ClayGlyphType.Profile, ClayPalette.Blush),
)
