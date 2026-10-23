package com.swimtrainer.core.engine

import com.swimtrainer.core.model.SetKind
import com.swimtrainer.core.model.SetItem
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.Workout
import com.swimtrainer.core.model.Zone
import kotlin.math.roundToInt

/** Energy and fueling estimates based on the Compendium of Physical Activities MET values. */
object Calories {

    fun met(stroke: Stroke, kind: SetKind, zone: Zone): Double {
        val vigorous = zone.ordinal >= Zone.THRESHOLD.ordinal
        val moderate = zone == Zone.TEMPO || zone == Zone.AEROBIC
        return when (kind) {
            SetKind.KICK -> if (vigorous) 8.0 else 6.0
            SetKind.DRILL, SetKind.DRILL_SWIM -> 5.0
            SetKind.SCULL -> 3.5
            SetKind.PULL, SetKind.SWIM -> when (stroke) {
                Stroke.BUTTERFLY -> 13.8
                Stroke.BREASTSTROKE -> if (vigorous) 10.3 else 5.3
                Stroke.BACKSTROKE -> if (vigorous) 9.5 else 4.8
                Stroke.IM -> if (vigorous) 10.0 else 6.5
                Stroke.FREESTYLE, Stroke.CHOICE -> when {
                    zone == Zone.SPRINT -> 10.0
                    vigorous -> 9.8
                    moderate -> 7.0
                    else -> 5.8
                }
            }
        }
    }

    /** Swim-time only (excludes rest), weighted by MET, plus rest at ~2 MET. */
    fun forItem(item: SetItem, weightKg: Double): Double {
        val workSec = (item.targetSec ?: (item.distance * 2)) * item.reps
        val restSec = (item.estimatedSec - workSec).coerceAtLeast(0)
        val kcalPerSec = weightKg / 3600.0
        return met(item.stroke, item.kind, item.zone) * workSec * kcalPerSec + 2.0 * restSec * kcalPerSec
    }

    fun forWorkout(workout: Workout, weightKg: Double): Int =
        workout.blocks.sumOf { block -> block.items.sumOf { forItem(it, weightKg) } * block.rounds }.roundToInt()

    /** Generic estimate when only duration and effort are known. */
    fun forDuration(minutes: Int, rpe: Int, weightKg: Double): Int {
        val met = when {
            rpe <= 3 -> 5.0
            rpe <= 5 -> 6.5
            rpe <= 7 -> 8.0
            else -> 9.8
        }
        return (met * weightKg * minutes / 60.0).roundToInt()
    }

    data class Fueling(
        val proteinGramsLow: Int,
        val proteinGramsHigh: Int,
        val fluidMlPerHour: Int,
        val carbsGramsPerSession: Int,
        val tips: List<String>,
    )

    fun fueling(profile: SwimmerProfile): Fueling {
        val w = profile.weightKg
        val intensityFactor = when (profile.level) {
            com.swimtrainer.core.model.Level.BEGINNER, com.swimtrainer.core.model.Level.NOVICE -> 0.5
            com.swimtrainer.core.model.Level.INTERMEDIATE -> 0.75
            else -> 1.0
        }
        val tips = buildList {
            add("Eat a carb-based snack 60–90 minutes before swimming (banana, toast, oats).")
            add("You sweat in the pool even if you can't feel it — keep a bottle at the end of your lane.")
            add("Within 60 minutes after training aim for ~${(w * 0.3).roundToInt()} g protein plus carbs.")
            if (profile.sessionMinutes >= 75) add("Sessions over 75 minutes: sip a sports drink with 30–60 g carbs per hour.")
            if (profile.goal == com.swimtrainer.core.model.Goal.WEIGHT_LOSS) {
                add("For weight loss keep protein high and create a modest deficit — swimming makes you hungry, plan a post-swim snack so you don't over-eat later.")
            }
            if (profile.ageGroup.ordinal >= com.swimtrainer.core.model.AgeGroup.MASTERS.ordinal) {
                add("Masters swimmers benefit from protein spread across 4 meals (~0.4 g/kg each) to preserve muscle.")
            }
        }
        return Fueling(
            proteinGramsLow = (w * 1.2).roundToInt(),
            proteinGramsHigh = (w * if (profile.level.ordinal >= 3) 1.8 else 1.6).roundToInt(),
            fluidMlPerHour = (w * 7 * (0.7 + intensityFactor * 0.3)).roundToInt().coerceIn(300, 900),
            carbsGramsPerSession = (w * intensityFactor * profile.sessionMinutes / 60.0).roundToInt(),
            tips = tips,
        )
    }
}
