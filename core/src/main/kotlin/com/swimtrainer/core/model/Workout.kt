package com.swimtrainer.core.model

import kotlinx.serialization.Serializable

/** Training intensity zones, anchored to critical swim speed (CSS). */
@Serializable
enum class Zone(
    val label: String,
    val short: String,
    val rpe: String,
    /** Multiplier applied to CSS pace (seconds per 100). >1 = slower. */
    val paceFactor: Double,
    val description: String,
) {
    RECOVERY("Recovery", "Z1", "2–3/10", 1.18, "Very easy, loosen up, perfect technique. Could hold a conversation."),
    AEROBIC("Aerobic", "Z2", "4–5/10", 1.09, "Steady, comfortable, sustainable for a long time. Nasal-breathing easy between reps."),
    TEMPO("Tempo", "Z3", "6/10", 1.04, "Moderately hard, 'comfortably uncomfortable'. Rhythm and focus."),
    THRESHOLD("Threshold", "Z4", "7–8/10", 1.00, "CSS pace: the fastest pace you can hold for ~30 minutes. Hard but controlled."),
    VO2("VO2 max", "Z5", "8–9/10", 0.95, "Very hard. Heavy breathing, can only hold for a few minutes total per rep."),
    SPRINT("Sprint", "Z6", "10/10", 0.88, "All-out, maximum speed. Full recovery between efforts.");
}

@Serializable
enum class SetKind(val label: String) {
    SWIM("Swim"),
    KICK("Kick"),
    PULL("Pull"),
    DRILL("Drill"),
    DRILL_SWIM("Drill/Swim"),
    SCULL("Scull"),
}

/** How effort should change within a set. */
@Serializable
enum class EffortPattern(val label: String, val hint: String) {
    STEADY("Steady", "Hold the same pace every rep."),
    DESCEND("Descend", "Each rep faster than the last."),
    BUILD("Build", "Start easy, finish fast within each rep."),
    NEGATIVE_SPLIT("Negative split", "Second half of each rep faster than the first."),
    FAST_EASY("Fast/Easy", "Alternate fast and easy reps (odds fast, evens easy)."),
    EASY_FAST("Easy/Fast", "Alternate easy and fast reps (odds easy, evens fast)."),
    HOLD_BEST("Hold best", "Hold your best average — stop the set if you miss the target twice."),
}

@Serializable
data class SetItem(
    val reps: Int,
    val distance: Int,
    val stroke: Stroke,
    val kind: SetKind = SetKind.SWIM,
    val zone: Zone = Zone.AEROBIC,
    val pattern: EffortPattern = EffortPattern.STEADY,
    val equipment: Set<Equipment> = emptySet(),
    val drillId: String? = null,
    /** Target time for one rep in seconds (null when pace doesn't apply, e.g. drills for beginners). */
    val targetSec: Int? = null,
    /** Interval / send-off per rep in seconds. */
    val sendOffSec: Int? = null,
    /** Rest after each rep in seconds (used when send-off isn't given). */
    val restSec: Int? = null,
    val note: String? = null,
) {
    val totalDistance get() = reps * distance

    /** Estimated wall-clock seconds for the whole item. */
    val estimatedSec: Int
        get() {
            val perRep = sendOffSec ?: ((targetSec ?: (distance * 2)) + (restSec ?: 15))
            return perRep * reps
        }
}

@Serializable
enum class BlockType(val label: String) {
    WARMUP("Warm-up"),
    SKILLS("Technique & drills"),
    PRESET("Pre-set"),
    MAIN("Main set"),
    COOLDOWN("Cool-down"),
}

@Serializable
data class WorkoutBlock(
    val type: BlockType,
    val title: String,
    val items: List<SetItem>,
    val rounds: Int = 1,
    /** Rest between rounds in seconds. */
    val roundRestSec: Int = 0,
    val note: String? = null,
) {
    val totalDistance get() = items.sumOf { it.totalDistance } * rounds
    val estimatedSec get() = items.sumOf { it.estimatedSec } * rounds + roundRestSec * (rounds - 1)
}

@Serializable
enum class SessionType(val label: String, val hard: Boolean, val description: String) {
    ENDURANCE("Endurance", false, "Aerobic base: steady, longer repeats with short rest."),
    THRESHOLD("Threshold", true, "CSS pace repeats that raise the speed you can sustain."),
    SPEED("Speed", true, "Short, fast efforts with plenty of rest to build top-end speed."),
    TECHNIQUE("Technique", false, "Drill-heavy session to groove efficient movement."),
    RECOVERY("Recovery", false, "Easy swimming, mobility and feel for the water."),
    IM("Medley", false, "All four strokes for balance, coordination and variety."),
    RACE_PACE("Race pace", true, "Short repeats at goal race speed with tight rest."),
    OPEN_WATER("Open water skills", false, "Sighting, wall-free turns and continuous swimming."),
    CSS_TEST("CSS test", true, "400 + 200 time trial to set your training paces."),
}

@Serializable
data class Workout(
    val id: String,
    val title: String,
    val type: SessionType,
    val focusStroke: Stroke,
    val pool: PoolLength,
    val blocks: List<WorkoutBlock>,
    val summary: String,
    val cues: List<String> = emptyList(),
    val calories: Int = 0,
) {
    val totalDistance get() = blocks.sumOf { it.totalDistance }
    val estimatedMinutes get() = (blocks.sumOf { it.estimatedSec } + (blocks.size - 1) * 30) / 60
}

@Serializable
enum class Phase(val label: String, val description: String) {
    BASE("Base", "Build aerobic engine and technique. Mostly easy-to-moderate volume."),
    BUILD("Build", "Add threshold work. Volume and intensity rise together."),
    PEAK("Peak", "Race-specific speed. Quality over quantity."),
    TAPER("Taper", "Volume drops, sharpness stays. Arrive fresh and fast."),
    RECOVERY("Recovery week", "Lower volume to absorb training. Includes a CSS re-test."),
}

@Serializable
data class PlanDay(
    val day: Weekday,
    val workout: Workout,
)

@Serializable
data class PlanWeek(
    val number: Int,
    val phase: Phase,
    val focus: String,
    val days: List<PlanDay>,
) {
    val totalDistance get() = days.sumOf { it.workout.totalDistance }
}

@Serializable
data class TrainingPlan(
    val id: String,
    val name: String,
    val createdEpochDay: Long,
    val goal: Goal,
    val level: Level,
    val weeks: List<PlanWeek>,
    val overview: String,
)

@Serializable
data class WorkoutLog(
    val id: String,
    val epochDay: Long,
    val title: String,
    val type: SessionType,
    val distance: Int,
    val minutes: Int,
    val calories: Int,
    val rpe: Int,
    val notes: String = "",
    val planWeek: Int? = null,
    val planDay: Weekday? = null,
)
