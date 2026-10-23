package com.swimtrainer.core

import com.swimtrainer.core.content.Drills
import com.swimtrainer.core.content.StrokeGuide
import com.swimtrainer.core.engine.Energy
import com.swimtrainer.core.engine.Pace
import com.swimtrainer.core.engine.PlanGenerator
import com.swimtrainer.core.engine.WorkoutGenerator
import com.swimtrainer.core.engine.WorkoutRequest
import com.swimtrainer.core.engine.WorkoutSteps
import com.swimtrainer.core.model.Goal
import com.swimtrainer.core.model.Level
import com.swimtrainer.core.model.Phase
import com.swimtrainer.core.model.PoolLength
import com.swimtrainer.core.model.SessionType
import com.swimtrainer.core.model.SetKind
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.TrainingPlan
import com.swimtrainer.core.model.Weekday
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {

    private val profile = SwimmerProfile(age = 34, weightKg = 72.0, level = Level.INTERMEDIATE)

    @Test
    fun cssFromTest() {
        assertEquals(90.0, Pace.cssFromTest(6 * 60 + 30.0, 3 * 60 + 30.0)!!, 0.001)
        assertNull(Pace.cssFromTest(100.0, 200.0))
    }

    @Test
    fun parseAndFormat() {
        assertEquals(95.0, Pace.parse("1:35")!!, 0.001)
        assertEquals(95.4, Pace.parse("1:35.4")!!, 0.001)
        assertEquals(42.0, Pace.parse("42")!!, 0.001)
        assertNull(Pace.parse("abc"))
        assertEquals("1:35", Pace.format(95))
        assertEquals("1:02:05", Pace.format(3725))
    }

    @Test
    fun slowerZonesAreSlower() {
        val table = Pace.zoneTable(profile)
        for (i in 1 until table.size) assertTrue(table[i].per100Sec <= table[i - 1].per100Sec)
    }

    @Test
    fun olderSwimmersGetSlowerEstimates() {
        assertTrue(Pace.css(profile.copy(age = 65)) > Pace.css(profile))
    }

    @Test
    fun everyCombinationProducesValidWorkout() {
        for (level in Level.entries) for (pool in PoolLength.entries) for (type in SessionType.entries) {
            for (minutes in listOf(20, 45, 90)) for (seed in 0L..4L) {
                val p = profile.copy(level = level, pool = pool)
                val w = WorkoutGenerator(p).generate(WorkoutRequest(type, minutes, Stroke.FREESTYLE, seed = seed))
                val label = "$level $pool $type ${minutes}min seed=$seed"
                assertTrue("$label has blocks", w.blocks.isNotEmpty())
                assertTrue("$label distance > 0", w.totalDistance > 0)
                for (block in w.blocks) for (item in block.items) {
                    assertTrue("$label ${item.distance} multiple of pool", item.distance % pool.length == 0)
                    assertTrue("$label reps>0", item.reps > 0)
                    item.drillId?.let { assertNotNull("$label drill $it exists", Drills.byId(it)) }
                    if (item.stroke == Stroke.BUTTERFLY && item.kind == SetKind.SWIM && level <= Level.NOVICE) {
                        assertTrue("$label no long fly for $level", item.distance <= 25 || pool == PoolLength.LCM_50)
                    }
                }
                if (type != SessionType.CSS_TEST) {
                    // Time-fitted sessions should land within a reasonable window of the request.
                    val est = w.estimatedMinutes
                    assertTrue("$label est $est min", est in (minutes * 0.7).toInt() - 3..(minutes * 1.3).toInt() + 5)
                }
                assertTrue("$label calories", w.calories > 0)
            }
        }
    }

    @Test
    fun beginnerRepsAreShort() {
        val p = profile.copy(level = Level.BEGINNER, pool = PoolLength.SCM_25)
        for (seed in 0L..20L) {
            val w = WorkoutGenerator(p).generate(WorkoutRequest(SessionType.ENDURANCE, 45, seed = seed))
            val main = w.blocks.first { it.type == com.swimtrainer.core.model.BlockType.MAIN }
            assertTrue(main.items.all { it.distance <= 50 })
        }
    }

    @Test
    fun lowEnergyIsEasier() {
        val normal = WorkoutGenerator(profile).generate(WorkoutRequest(SessionType.THRESHOLD, 45, seed = 1))
        val tired = WorkoutGenerator(profile).generate(WorkoutRequest(SessionType.THRESHOLD, 45, energy = Energy.LOW, seed = 1))
        val maxZone = { w: com.swimtrainer.core.model.Workout -> w.blocks.flatMap { it.items }.maxOf { it.zone.ordinal } }
        assertTrue(maxZone(tired) < maxZone(normal))
    }

    @Test
    fun singleSet() {
        for (type in SessionType.entries) {
            val set = WorkoutGenerator(profile).generateSet(type, 15, Stroke.BACKSTROKE, seed = 3)
            assertTrue(set.items.isNotEmpty())
        }
    }

    @Test
    fun plansForAllGoalsAndDays() {
        for (goal in Goal.entries) for (level in Level.entries) for (days in 1..7) {
            val p = profile.copy(goal = goal, level = level, trainingDays = Weekday.entries.take(days).toSet())
            val plan = PlanGenerator.generate(p, weeks = 8, startEpochDay = 0, seed = 7)
            assertEquals(8, plan.weeks.size)
            plan.weeks.forEach { week ->
                assertEquals("$goal $level $days", days, week.days.size)
                if (level == Level.BEGINNER) assertTrue(week.days.none { it.workout.type == SessionType.RACE_PACE })
            }
        }
    }

    @Test
    fun raceGoalsTaper() {
        val p = profile.copy(goal = Goal.COMPETITION)
        val phases = PlanGenerator.phases(12, p.goal, p.level)
        assertEquals(Phase.TAPER, phases.last())
        assertEquals(Phase.BASE, phases.first())
        assertTrue(Phase.RECOVERY in phases)
    }

    @Test
    fun taperReducesVolume() {
        val p = profile.copy(goal = Goal.SPEED, trainingDays = setOf(Weekday.MON, Weekday.WED, Weekday.FRI, Weekday.SAT))
        val plan = PlanGenerator.generate(p, weeks = 10, startEpochDay = 0, seed = 11)
        val peakWeek = plan.weeks.filter { it.phase == Phase.BUILD }.maxOf { it.totalDistance }
        assertTrue(plan.weeks.last().totalDistance < peakWeek)
    }

    @Test
    fun hardDaysSpread() {
        val ordered = PlanGenerator.spreadHardDays(
            listOf(SessionType.THRESHOLD, SessionType.SPEED, SessionType.ENDURANCE, SessionType.TECHNIQUE)
        )
        for (i in 1 until ordered.size) assertTrue(!(ordered[i].hard && ordered[i - 1].hard))
    }

    @Test
    fun firstWeekHasCssTestWhenPaceUnknown() {
        val plan = PlanGenerator.generate(profile, weeks = 4, startEpochDay = 0, seed = 1)
        assertTrue(plan.weeks.first().days.any { it.workout.type == SessionType.CSS_TEST })
        val known = PlanGenerator.generate(profile.copy(cssPer100 = 95.0), weeks = 4, startEpochDay = 0, seed = 1)
        assertTrue(known.weeks.first().days.none { it.workout.type == SessionType.CSS_TEST })
    }

    @Test
    fun planSerializesRoundTrip() {
        val plan = PlanGenerator.generate(profile, weeks = 4, startEpochDay = 20000, seed = 5)
        val json = Json.encodeToString(TrainingPlan.serializer(), plan)
        assertEquals(plan, Json.decodeFromString(TrainingPlan.serializer(), json))
    }

    @Test
    fun stepsCoverWholeWorkout() {
        val w = WorkoutGenerator(profile).generate(WorkoutRequest(SessionType.ENDURANCE, 45, seed = 2))
        val steps = WorkoutSteps.flatten(w)
        assertEquals(w.totalDistance, steps.sumOf { it.item.distance })
    }

    @Test
    fun guideFaultsReferenceRealDrills() {
        StrokeGuide.entries.flatMap { it.faults }.flatMap { it.drillIds }.forEach {
            assertNotNull("drill $it", Drills.byId(it))
        }
        assertEquals(Drills.all.size, Drills.all.map { it.id }.toSet().size)
    }
}
