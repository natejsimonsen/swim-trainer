package com.swimtrainer.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Level(val label: String, val description: String) {
    BEGINNER(
        "Beginner",
        "Comfortable in the water, can swim 25–50 continuously but needs rest between lengths."
    ),
    NOVICE(
        "Novice",
        "Can swim 200–400 freestyle without stopping. Learning other strokes and flip turns."
    ),
    INTERMEDIATE(
        "Intermediate",
        "Swims 1,500–2,500 per session, knows all four strokes, reads a pace clock."
    ),
    ADVANCED(
        "Advanced",
        "Trains 3–5×/week, 3,000–4,500 per session, solid technique in all strokes."
    ),
    ELITE(
        "Elite",
        "Competitive club/masters/college swimmer, 4,500+ per session, races regularly."
    );
}

@Serializable
enum class Goal(val label: String, val description: String) {
    FITNESS("General fitness", "Build heart health, strength and consistency."),
    WEIGHT_LOSS("Weight loss", "More steady aerobic volume to maximise calorie burn."),
    TECHNIQUE("Technique", "Smoother, more efficient strokes. Lots of drills and feedback cues."),
    ENDURANCE("Endurance", "Swim further without stopping. Long aerobic sets."),
    SPEED("Speed / sprint", "Get faster over 50–200. Race-pace and sprint work."),
    TRIATHLON("Triathlon", "Freestyle-focused aerobic power, sighting and open-water skills."),
    OPEN_WATER("Open water", "Continuous swimming, sighting, drafting and wall-free turns."),
    COMPETITION("Competition", "Periodised pool racing prep across all four strokes.");
}

@Serializable
enum class Stroke(val label: String, val short: String) {
    FREESTYLE("Freestyle", "Free"),
    BACKSTROKE("Backstroke", "Back"),
    BREASTSTROKE("Breaststroke", "Breast"),
    BUTTERFLY("Butterfly", "Fly"),
    IM("Individual medley", "IM"),
    CHOICE("Choice", "Choice");

    val isCompetitive get() = this in listOf(FREESTYLE, BACKSTROKE, BREASTSTROKE, BUTTERFLY)

    companion object {
        val four = listOf(BUTTERFLY, BACKSTROKE, BREASTSTROKE, FREESTYLE)
    }
}

@Serializable
enum class Equipment(val label: String) {
    KICKBOARD("Kickboard"),
    PULL_BUOY("Pull buoy"),
    FINS("Fins"),
    PADDLES("Paddles"),
    SNORKEL("Snorkel");
}

@Serializable
enum class PoolLength(val label: String, val length: Int, val yards: Boolean) {
    SCY_25("25 yd", 25, true),
    SCM_25("25 m", 25, false),
    LCM_50("50 m", 50, false);

    val unit get() = if (yards) "yd" else "m"
}

@Serializable
enum class Weekday(val short: String, val label: String) {
    MON("Mon", "Monday"),
    TUE("Tue", "Tuesday"),
    WED("Wed", "Wednesday"),
    THU("Thu", "Thursday"),
    FRI("Fri", "Friday"),
    SAT("Sat", "Saturday"),
    SUN("Sun", "Sunday");
}

@Serializable
data class SwimmerProfile(
    val name: String = "",
    val age: Int = 30,
    val weightKg: Double = 75.0,
    val heightCm: Double? = null,
    val useImperial: Boolean = false,
    val level: Level = Level.INTERMEDIATE,
    val goal: Goal = Goal.FITNESS,
    val trainingDays: Set<Weekday> = setOf(Weekday.MON, Weekday.WED, Weekday.FRI),
    val sessionMinutes: Int = 45,
    val pool: PoolLength = PoolLength.SCM_25,
    val equipment: Set<Equipment> = setOf(Equipment.KICKBOARD, Equipment.PULL_BUOY),
    val focusStrokes: Set<Stroke> = setOf(Stroke.FREESTYLE),
    /** Critical swim speed in seconds per 100 pool units, if known from a test. */
    val cssPer100: Double? = null,
) {
    val daysPerWeek get() = trainingDays.size.coerceAtLeast(1)

    val ageGroup: AgeGroup
        get() = when {
            age < 13 -> AgeGroup.YOUTH
            age < 18 -> AgeGroup.TEEN
            age < 40 -> AgeGroup.ADULT
            age < 55 -> AgeGroup.MASTERS
            age < 70 -> AgeGroup.SENIOR
            else -> AgeGroup.SENIOR_PLUS
        }

    val bmi: Double?
        get() = heightCm?.takeIf { it > 0 }?.let { weightKg / ((it / 100.0) * (it / 100.0)) }
}

enum class AgeGroup(val label: String) {
    YOUTH("Youth (under 13)"),
    TEEN("Teen (13–17)"),
    ADULT("Adult (18–39)"),
    MASTERS("Masters (40–54)"),
    SENIOR("Masters 55+"),
    SENIOR_PLUS("Masters 70+");
}
