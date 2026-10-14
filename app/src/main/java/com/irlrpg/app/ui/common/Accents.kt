package com.irlrpg.app.ui.common

import com.irlrpg.app.domain.model.AttributeKind
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.ui.theme.ClayAccent
import com.irlrpg.app.ui.theme.ClayPalette

/** Category colour wins; otherwise the quest's skill attribute picks the accent. */
fun accentFor(item: QuestWithProgress): ClayAccent =
    ClayPalette.accentById(item.category?.accentId ?: attributeAccentId(item.quest.attributeKind))

fun attributeAccentId(attributeName: String): String =
    AttributeKind.entries.firstOrNull { it.name == attributeName || it.label == attributeName }?.accentId
        ?: "lavender"

fun accentForAttribute(attributeName: String): ClayAccent = ClayPalette.accentById(attributeAccentId(attributeName))
