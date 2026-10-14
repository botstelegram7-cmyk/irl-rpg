package com.irlrpg.app.domain.model

/**
 * How an activity is tracked. The system is deliberately universal: every
 * profession and hobby maps onto one of these five methods.
 */
enum class TrackingKind(val label: String, val emoji: String, val helper: String) {
    Time("Time-based", "⏱️", "Read for 30 minutes, practise for 1 hour…"),
    Count("Count-based", "🔢", "Read 25 pages, write 500 words, 40 reps…"),
    Milestone("Milestone", "🏁", "Finish a book, ship a project, pass a level…"),
    Performance("Performance", "🎯", "Hit a target score, accuracy, weight, grade…"),
    Custom("Custom", "🧩", "Define your own measurable goal and unit."),
    ;

    companion object {
        fun fromName(name: String): TrackingKind = entries.firstOrNull { it.name == name } ?: Custom
    }
}

/** Recurrence model. "Flexible" means the quest has no schedule pressure at all. */
enum class ScheduleKind(val label: String, val emoji: String, val helper: String) {
    Daily("Every day", "☀️", "Appears in today's quest log daily."),
    Weekdays("Weekdays", "🗓️", "Monday to Friday only."),
    SelectedDays("Chosen days", "📌", "Pick the days that fit your week."),
    Weekly("Weekly target", "🔁", "One target to finish across the whole week."),
    Flexible("Any time", "🌊", "No schedule — progress whenever you like."),
    ;

    companion object {
        fun fromName(name: String): ScheduleKind = entries.firstOrNull { it.name == name } ?: Daily
    }
}

/** Units offered by the editor; [Custom] lets the user type their own. */
enum class UnitKind(val label: String, val short: String) {
    Minutes("Minutes", "min"),
    Hours("Hours", "hrs"),
    Pages("Pages", "pages"),
    Chapters("Chapters", "ch"),
    Words("Words", "words"),
    Reps("Repetitions", "reps"),
    Kilometres("Kilometres", "km"),
    Sessions("Sessions", "sessions"),
    Percent("Percent", "%"),
    Points("Points", "pts"),
    Steps("Steps", "steps"),
    Custom("Custom unit", "unit"),
    ;

    companion object {
        fun fromName(name: String): UnitKind = entries.firstOrNull { it.name == name } ?: Custom
    }
}

/**
 * Skill attributes. Six broad buckets cover every activity a person may track;
 * users can rename them and can create additional custom attributes.
 */
enum class AttributeKind(val label: String, val emoji: String, val accentId: String) {
    Strength("Strength", "💪", "peach"),
    Intellect("Intellect", "🧠", "sky"),
    Focus("Focus", "🎯", "lavender"),
    Vitality("Vitality", "🌿", "mint"),
    Creativity("Creativity", "🎨", "blush"),
    Discipline("Discipline", "🛡️", "butter"),
    ;

    companion object {
        fun fromName(name: String): AttributeKind = entries.firstOrNull { it.name == name } ?: Focus
    }
}
