package com.swimtrainer.core.engine

import com.swimtrainer.core.model.SetItem
import com.swimtrainer.core.model.Workout
import com.swimtrainer.core.model.WorkoutBlock

/** One rep the swimmer performs, used by the in-pool workout player. */
data class Step(
    val index: Int,
    val block: WorkoutBlock,
    val item: SetItem,
    val round: Int,
    val rep: Int,
    /** Distance completed before this step starts. */
    val distanceBefore: Int,
) {
    /** Seconds until the next rep leaves: the send-off, or swim target + rest. */
    val cycleSec: Int?
        get() = item.sendOffSec ?: item.restSec?.let { rest -> (item.targetSec ?: 0) + rest }
}

object WorkoutSteps {
    fun flatten(workout: Workout): List<Step> {
        val steps = mutableListOf<Step>()
        var distance = 0
        for (block in workout.blocks) {
            for (round in 1..block.rounds) {
                for (item in block.items) {
                    for (rep in 1..item.reps) {
                        steps += Step(steps.size, block, item, round, rep, distance)
                        distance += item.distance
                    }
                }
            }
        }
        return steps
    }
}
