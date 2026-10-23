package com.swimtrainer.core.engine

import com.swimtrainer.core.model.AgeGroup
import com.swimtrainer.core.model.Goal
import com.swimtrainer.core.model.Level
import com.swimtrainer.core.model.Phase
import com.swimtrainer.core.model.PlanDay
import com.swimtrainer.core.model.PlanWeek
import com.swimtrainer.core.model.SessionType
import com.swimtrainer.core.model.SessionType.CSS_TEST
import com.swimtrainer.core.model.SessionType.ENDURANCE
import com.swimtrainer.core.model.SessionType.IM
import com.swimtrainer.core.model.SessionType.OPEN_WATER
import com.swimtrainer.core.model.SessionType.RACE_PACE
import com.swimtrainer.core.model.SessionType.RECOVERY
import com.swimtrainer.core.model.SessionType.SPEED
import com.swimtrainer.core.model.SessionType.TECHNIQUE
import com.swimtrainer.core.model.SessionType.THRESHOLD
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.TrainingPlan
import com.swimtrainer.core.model.Weekday
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Periodised multi-week plans: base → build → peak → taper (for race goals) with a recovery
 * and re-test week every fourth week, hard days spread out, and volume progressing gradually.
 */
object PlanGenerator {

    val raceGoals = setOf(Goal.COMPETITION, Goal.TRIATHLON, Goal.OPEN_WATER, Goal.SPEED)

    fun generate(
        profile: SwimmerProfile,
        weeks: Int = 8,
        startEpochDay: Long,
        seed: Long = Random.nextLong(),
    ): TrainingPlan {
        val n = weeks.coerceIn(2, 24)
        val phases = phases(n, profile.goal, profile.level)
        val generator = WorkoutGenerator(profile)
        val days = profile.trainingDays.sortedBy { it.ordinal }.ifEmpty { listOf(Weekday.MON, Weekday.WED, Weekday.FRI) }
        val strokeRotation = strokeRotation(profile)
        var strokeIndex = 0

        val planWeeks = phases.mapIndexed { index, phase ->
            val weekNo = index + 1
            val scale = volumeScale(phases, index)
            val types = weekTypes(days.size, profile, phase, weekNo)
            val ordered = spreadHardDays(types)
            val planDays = days.zip(ordered).mapIndexed { dayIndex, (day, type) ->
                val stroke = when (type) {
                    TECHNIQUE, SPEED, THRESHOLD, RACE_PACE ->
                        if (profile.goal == Goal.COMPETITION || type == TECHNIQUE) strokeRotation[strokeIndex++ % strokeRotation.size] else Stroke.FREESTYLE
                    else -> Stroke.FREESTYLE
                }
                val minutes = (profile.sessionMinutes * scale * dayFactor(type)).roundToInt().coerceAtLeast(15)
                val workout = generator.generate(
                    WorkoutRequest(
                        type = type,
                        minutes = minutes,
                        focusStroke = stroke,
                        phase = phase,
                        seed = seed + weekNo * 100L + dayIndex,
                    )
                )
                PlanDay(day, workout)
            }
            PlanWeek(weekNo, phase, weekFocus(phase, profile.goal, weekNo), planDays)
        }

        return TrainingPlan(
            id = "p${seed.toULong().toString(36)}",
            name = "${n}-week ${profile.goal.label} plan",
            createdEpochDay = startEpochDay,
            goal = profile.goal,
            level = profile.level,
            weeks = planWeeks,
            overview = overview(profile, phases),
        )
    }

    fun phases(weeks: Int, goal: Goal, level: Level): List<Phase> {
        val race = goal in raceGoals && weeks >= 4
        val result = MutableList(weeks) { Phase.BASE }
        if (race) {
            val taper = if (weeks >= 10) 2 else 1
            val peak = (weeks * 0.15).roundToInt().coerceAtLeast(1)
            val build = ((weeks - taper - peak) * 0.5).roundToInt()
            for (i in 0 until weeks) {
                result[i] = when {
                    i >= weeks - taper -> Phase.TAPER
                    i >= weeks - taper - peak -> Phase.PEAK
                    i >= weeks - taper - peak - build -> Phase.BUILD
                    else -> Phase.BASE
                }
            }
        } else {
            val baseWeeks = (weeks * if (level <= Level.NOVICE) 0.6 else 0.45).roundToInt()
            for (i in 0 until weeks) result[i] = if (i < baseWeeks) Phase.BASE else Phase.BUILD
        }
        // Every 4th week is a recovery/re-test week (not during peak/taper, not the first weeks).
        if (weeks >= 6) {
            for (i in 3 until weeks step 4) {
                if (result[i] == Phase.BASE || result[i] == Phase.BUILD) result[i] = Phase.RECOVERY
            }
        }
        return result
    }

    private fun volumeScale(phases: List<Phase>, index: Int): Double {
        val phase = phases[index]
        // Progress ~5%/week since the last recovery week within base/build.
        var streak = 0
        var i = index - 1
        while (i >= 0 && (phases[i] == Phase.BASE || phases[i] == Phase.BUILD)) {
            streak++; i--
        }
        return when (phase) {
            Phase.BASE -> 0.85 + 0.05 * streak
            Phase.BUILD -> 0.95 + 0.05 * streak
            Phase.PEAK -> 1.0
            Phase.RECOVERY -> 0.75
            Phase.TAPER -> if (index == phases.lastIndex) 0.55 else 0.7
        }.coerceAtMost(1.15)
    }

    /** Endurance sessions run a little longer, recovery sessions shorter. */
    private fun dayFactor(type: SessionType) = when (type) {
        ENDURANCE, OPEN_WATER -> 1.1
        RECOVERY -> 0.75
        SPEED, RACE_PACE -> 0.9
        else -> 1.0
    }

    private fun priorities(goal: Goal): List<SessionType> = when (goal) {
        Goal.FITNESS -> listOf(ENDURANCE, TECHNIQUE, THRESHOLD, IM, SPEED, RECOVERY, ENDURANCE)
        Goal.WEIGHT_LOSS -> listOf(ENDURANCE, TECHNIQUE, THRESHOLD, ENDURANCE, SPEED, RECOVERY, ENDURANCE)
        Goal.TECHNIQUE -> listOf(TECHNIQUE, ENDURANCE, TECHNIQUE, IM, THRESHOLD, RECOVERY, SPEED)
        Goal.ENDURANCE -> listOf(ENDURANCE, THRESHOLD, TECHNIQUE, ENDURANCE, RECOVERY, SPEED, ENDURANCE)
        Goal.SPEED -> listOf(SPEED, TECHNIQUE, THRESHOLD, RACE_PACE, ENDURANCE, RECOVERY, IM)
        Goal.TRIATHLON -> listOf(THRESHOLD, ENDURANCE, TECHNIQUE, OPEN_WATER, SPEED, RECOVERY, ENDURANCE)
        Goal.OPEN_WATER -> listOf(ENDURANCE, OPEN_WATER, THRESHOLD, TECHNIQUE, SPEED, RECOVERY, ENDURANCE)
        Goal.COMPETITION -> listOf(THRESHOLD, TECHNIQUE, SPEED, IM, RACE_PACE, ENDURANCE, RECOVERY)
    }

    fun weekTypes(days: Int, profile: SwimmerProfile, phase: Phase, weekNo: Int): List<SessionType> {
        val level = profile.level
        val race = profile.goal in raceGoals
        var types = priorities(profile.goal).take(days.coerceIn(1, 7)).toMutableList()

        // A single weekly swim should be a balanced all-rounder.
        if (days == 1) types = mutableListOf(if (level <= Level.NOVICE) TECHNIQUE else ENDURANCE)

        types = types.map { t ->
            when (phase) {
                Phase.BASE -> when (t) {
                    RACE_PACE -> ENDURANCE
                    SPEED -> if (level <= Level.NOVICE) TECHNIQUE else SPEED
                    else -> t
                }
                Phase.BUILD -> t
                Phase.PEAK -> when (t) {
                    ENDURANCE -> if (race) RACE_PACE else ENDURANCE
                    else -> t
                }
                Phase.TAPER -> when (t) {
                    THRESHOLD -> RACE_PACE
                    ENDURANCE, IM -> TECHNIQUE
                    else -> t
                }
                Phase.RECOVERY -> if (t.hard) TECHNIQUE else t
            }
        }.toMutableList()

        // Beginners: no race-pace work, threshold becomes steady endurance until they've built a base.
        if (level == Level.BEGINNER) {
            types = types.map {
                when (it) {
                    RACE_PACE -> SPEED
                    THRESHOLD -> if (phase == Phase.BASE) ENDURANCE else THRESHOLD
                    OPEN_WATER -> ENDURANCE
                    else -> it
                }
            }.toMutableList()
        }
        // Older masters: cap hard sessions at 2 per week.
        if (profile.ageGroup == AgeGroup.SENIOR || profile.ageGroup == AgeGroup.SENIOR_PLUS) {
            var hard = 0
            types = types.map { if (it.hard && ++hard > 2) ENDURANCE else it }.toMutableList()
        }

        val canTest = level >= Level.NOVICE
        val testWeek = (weekNo == 1 && profile.cssPer100 == null) || phase == Phase.RECOVERY
        if (canTest && testWeek && days >= 2) {
            val idx = types.indexOfFirst { it.hard || it == TECHNIQUE }.takeIf { it >= 0 } ?: 0
            // Keep at least one technique session in recovery weeks.
            val replace = if (phase == Phase.RECOVERY) types.indexOfLast { it == TECHNIQUE }.takeIf { it >= 0 } ?: idx else idx
            types[replace] = CSS_TEST
        }
        return types
    }

    /** Orders sessions so that hard days are separated by easier ones where possible. */
    fun spreadHardDays(types: List<SessionType>): List<SessionType> {
        val hard = types.filter { it.hard }.toMutableList()
        val easy = types.filter { !it.hard }.toMutableList()
        val result = mutableListOf<SessionType>()
        var lastHard = false
        while (hard.isNotEmpty() || easy.isNotEmpty()) {
            val remaining = hard.size + easy.size
            val takeHard = when {
                hard.isEmpty() -> false
                easy.isEmpty() -> true
                lastHard -> false
                // Must place hard now if otherwise we'd run out of easy days to separate them.
                hard.size * 2 - 1 >= remaining -> true
                else -> true
            }
            if (takeHard) result += hard.removeAt(0) else result += easy.removeAt(0)
            lastHard = takeHard
        }
        return result
    }

    private fun strokeRotation(profile: SwimmerProfile): List<Stroke> {
        if (profile.goal == Goal.TRIATHLON || profile.goal == Goal.OPEN_WATER) return listOf(Stroke.FREESTYLE)
        val chosen = profile.focusStrokes.filter { it.isCompetitive }
        val list = (listOf(Stroke.FREESTYLE) + chosen).distinct()
        return if (profile.goal == Goal.COMPETITION && list.size == 1) Stroke.four.reversed() else list
    }

    private fun weekFocus(phase: Phase, goal: Goal, weekNo: Int): String = when (phase) {
        Phase.BASE -> if (weekNo == 1) "Settle in, learn the drills and calibrate your paces." else "Aerobic volume and technique."
        Phase.BUILD -> when (goal) {
            Goal.SPEED, Goal.COMPETITION -> "Threshold and speed endurance."
            Goal.TRIATHLON, Goal.OPEN_WATER -> "Sustained threshold and pacing."
            else -> "More volume, steady threshold work."
        }
        Phase.PEAK -> "Race-specific speed. Quality over quantity."
        Phase.TAPER -> "Cut volume, stay sharp, rest up."
        Phase.RECOVERY -> "Absorb the training. Re-test your CSS."
    }

    private fun overview(profile: SwimmerProfile, phases: List<Phase>): String {
        val counts = phases.groupingBy { it }.eachCount()
        val parts = Phase.entries.mapNotNull { p -> counts[p]?.let { "${it}× ${p.label.lowercase()}" } }
        val age = when (profile.ageGroup) {
            AgeGroup.YOUTH, AgeGroup.TEEN -> " Extra technique and variety for a young swimmer."
            AgeGroup.MASTERS -> " Longer warm-ups for a masters swimmer."
            AgeGroup.SENIOR, AgeGroup.SENIOR_PLUS -> " Longer warm-ups, more rest and max two hard sessions a week."
            else -> ""
        }
        return "${profile.daysPerWeek} sessions/week of ~${profile.sessionMinutes} min for ${article(profile.level.label)} ${profile.level.label.lowercase()} swimmer. " +
            "Structure: ${parts.joinToString(", ")}.$age Paces are based on CSS ${Pace.format(Pace.css(profile))}/100${profile.pool.unit}" +
            (if (profile.cssPer100 == null) " (estimated — do the CSS test to personalise)." else ".")
    }

    private fun article(word: String) = if (word.first().lowercaseChar() in "aeiou") "an" else "a"
}
