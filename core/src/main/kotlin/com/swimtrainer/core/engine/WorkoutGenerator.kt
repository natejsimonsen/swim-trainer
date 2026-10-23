package com.swimtrainer.core.engine

import com.swimtrainer.core.content.Drill
import com.swimtrainer.core.content.DrillFocus
import com.swimtrainer.core.content.Drills
import com.swimtrainer.core.model.AgeGroup
import com.swimtrainer.core.model.BlockType
import com.swimtrainer.core.model.EffortPattern
import com.swimtrainer.core.model.Equipment
import com.swimtrainer.core.model.Level
import com.swimtrainer.core.model.Phase
import com.swimtrainer.core.model.SessionType
import com.swimtrainer.core.model.SetItem
import com.swimtrainer.core.model.SetKind
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.Workout
import com.swimtrainer.core.model.WorkoutBlock
import com.swimtrainer.core.model.Zone
import kotlin.math.roundToInt
import kotlin.random.Random

enum class Energy(val label: String) {
    LOW("Tired"),
    NORMAL("Normal"),
    HIGH("Fired up"),
}

data class WorkoutRequest(
    val type: SessionType,
    val minutes: Int,
    val focusStroke: Stroke = Stroke.FREESTYLE,
    val phase: Phase? = null,
    val energy: Energy = Energy.NORMAL,
    val seed: Long = Random.nextLong(),
    val title: String? = null,
)

/**
 * Builds complete, personalised swim workouts: warm-up, drills, pre-set, main set and cool-down,
 * each with distances, strokes, zones, target times and send-off intervals.
 */
class WorkoutGenerator(private val profile: SwimmerProfile) {

    private val pool = profile.pool
    private val level = profile.level
    private val css = Pace.css(profile)

    fun generate(request: WorkoutRequest): Workout {
        if (request.type == SessionType.CSS_TEST) return assemble(request, 1.0)
        // Rep counts round down, so re-fit the time budget a couple of times to land close to the request.
        val targetMin = request.minutes.coerceIn(10, 180)
        var scale = 1.0
        var lo = 0.0
        var hi = 0.0
        var best = assemble(request, scale)
        var current = best
        repeat(5) {
            val est = current.estimatedMinutes.coerceAtLeast(1)
            if (kotlin.math.abs(est - targetMin) <= maxOf(2, targetMin / 15)) return current
            if (est < targetMin) lo = scale else hi = scale
            scale = if (lo > 0 && hi > 0) (lo + hi) / 2 else (scale * targetMin / est).coerceIn(0.5, 2.0)
            current = assemble(request, scale)
            if (kotlin.math.abs(current.estimatedMinutes - targetMin) < kotlin.math.abs(best.estimatedMinutes - targetMin)) best = current
        }
        return best
    }

    private fun assemble(request: WorkoutRequest, scale: Double): Workout {
        val ctx = Ctx(Random(request.seed), request)
        val totalSec = (request.minutes.coerceIn(10, 180) * 60)

        val blocks = if (request.type == SessionType.CSS_TEST) {
            cssTest(ctx)
        } else {
            val split = timeSplit(request.type, request.minutes)
            val transitions = 30 * (split.count { it > 0 } - 1)
            val usable = ((totalSec - transitions).coerceAtLeast(totalSec / 2) * scale).toInt()
            val stroke = mainStroke(request)
            buildList {
                add(warmup(ctx, (usable * split[0]).toInt(), request.type))
                if (split[1] > 0) add(skills(ctx, (usable * split[1]).toInt(), request.type, stroke))
                if (split[2] > 0) add(preset(ctx, (usable * split[2]).toInt(), request.type, stroke))
                add(main(ctx, (usable * split[3]).toInt(), request.type, stroke))
                add(cooldown(ctx, (usable * split[4]).toInt()))
            }.filter { it.items.isNotEmpty() }
        }

        val mainBlock = blocks.firstOrNull { it.type == BlockType.MAIN }
        val title = request.title ?: "${request.type.label}: ${mainBlock?.title ?: "Session"}"
        val draft = Workout(
            id = "w${request.seed.toULong().toString(36)}",
            title = title,
            type = request.type,
            focusStroke = mainStroke(request),
            pool = pool,
            blocks = blocks,
            summary = "",
            cues = ctx.cues.distinct().take(6),
        )
        return draft.copy(
            summary = summary(draft, mainBlock),
            calories = Calories.forWorkout(draft, profile.weightKg),
        )
    }

    /** Generates just one main set — handy when you're already poolside. */
    fun generateSet(type: SessionType, minutes: Int, stroke: Stroke, seed: Long = Random.nextLong()): WorkoutBlock {
        val ctx = Ctx(Random(seed), WorkoutRequest(type, minutes, stroke, seed = seed))
        return if (type == SessionType.TECHNIQUE) {
            skills(ctx, minutes * 60, type, stroke).copy(type = BlockType.MAIN)
        } else {
            main(ctx, minutes * 60, type, stroke)
        }
    }

    // ------------------------------------------------------------------ helpers

    private inner class Ctx(val rnd: Random, val request: WorkoutRequest) {
        val cues = mutableListOf<String>()

        fun shift(zone: Zone): Zone = when (request.energy) {
            Energy.LOW -> if (zone.ordinal >= Zone.TEMPO.ordinal && zone != Zone.SPRINT) Zone.entries[zone.ordinal - 1] else zone
            else -> zone
        }

        fun <T> pick(list: List<T>): T = list[rnd.nextInt(list.size)]
    }

    /** Fractions of the session for warm-up, skills, pre-set, main and cool-down. */
    private fun timeSplit(type: SessionType, minutes: Int): DoubleArray {
        val s = when (type) {
            SessionType.TECHNIQUE -> doubleArrayOf(0.18, 0.40, 0.10, 0.25, 0.07)
            SessionType.RECOVERY -> doubleArrayOf(0.25, 0.30, 0.0, 0.35, 0.10)
            SessionType.SPEED, SessionType.RACE_PACE -> doubleArrayOf(0.22, 0.10, 0.15, 0.43, 0.10)
            SessionType.OPEN_WATER -> doubleArrayOf(0.18, 0.15, 0.07, 0.52, 0.08)
            SessionType.ENDURANCE -> doubleArrayOf(0.16, 0.12, 0.12, 0.52, 0.08)
            else -> doubleArrayOf(0.18, 0.14, 0.12, 0.48, 0.08)
        }
        if (minutes < 25) {
            s[3] += s[2]; s[2] = 0.0
        }
        when (profile.ageGroup) {
            AgeGroup.MASTERS, AgeGroup.SENIOR, AgeGroup.SENIOR_PLUS -> {
                val extra = s[0] * if (profile.ageGroup == AgeGroup.MASTERS) 0.15 else 0.3
                s[0] += extra; s[3] -= extra
            }
            AgeGroup.YOUTH -> {
                val extra = s[3] * 0.2
                if (s[1] > 0) {
                    s[1] += extra; s[3] -= extra
                }
            }
            else -> Unit
        }
        if (level == Level.BEGINNER && s[1] > 0) {
            val extra = s[3] * 0.15
            s[1] += extra; s[3] -= extra
        }
        return s
    }

    private fun mainStroke(request: WorkoutRequest): Stroke = when (request.type) {
        SessionType.IM -> Stroke.IM
        SessionType.OPEN_WATER, SessionType.CSS_TEST -> Stroke.FREESTYLE
        else -> if (request.focusStroke == Stroke.IM && request.type != SessionType.TECHNIQUE) Stroke.IM else request.focusStroke
    }

    /** Round a distance to whole pool lengths. */
    private fun d(distance: Int): Int {
        val len = pool.length
        return ((distance.toDouble() / len).roundToInt() * len).coerceAtLeast(len)
    }

    /** Longest sensible single rep for this swimmer and stroke. */
    private fun maxRep(stroke: Stroke): Int {
        val free = when (level) {
            Level.BEGINNER -> 50
            Level.NOVICE -> 200
            Level.INTERMEDIATE -> 400
            Level.ADVANCED -> 800
            Level.ELITE -> 1000
        }
        return when (stroke) {
            Stroke.BUTTERFLY -> when (level) {
                Level.BEGINNER -> 25
                Level.NOVICE -> 25
                Level.INTERMEDIATE -> 50
                Level.ADVANCED -> 100
                Level.ELITE -> 200
            }
            Stroke.BREASTSTROKE, Stroke.BACKSTROKE -> (free / 2).coerceAtLeast(25)
            Stroke.IM -> when (level) {
                Level.BEGINNER -> 100
                Level.NOVICE -> 100
                Level.INTERMEDIATE -> 200
                else -> 400
            }
            else -> free
        }.let { d(it) }
    }

    private fun usesRestNotSendOff() = level == Level.BEGINNER || profile.ageGroup == AgeGroup.YOUTH

    private fun has(e: Equipment) = e in profile.equipment

    private fun item(
        ctx: Ctx,
        reps: Int,
        distance: Int,
        stroke: Stroke,
        kind: SetKind = SetKind.SWIM,
        zone: Zone = Zone.AEROBIC,
        pattern: EffortPattern = EffortPattern.STEADY,
        equipment: Set<Equipment> = emptySet(),
        drillId: String? = null,
        note: String? = null,
        restSec: Int? = null,
        forceRest: Boolean = false,
    ): SetItem {
        val dist = d(distance)
        val z = ctx.shift(zone)
        var k = kind
        var n = note
        var eq = equipment.filter { has(it) }.toSet()
        if (k == SetKind.PULL && !has(Equipment.PULL_BUOY)) {
            k = SetKind.SWIM
            n = listOfNotNull(n, "No pull buoy: focus on the arms with a light 2-beat kick.").joinToString(" ")
        }
        if (k == SetKind.KICK && has(Equipment.KICKBOARD) && stroke != Stroke.BACKSTROKE) {
            eq = eq + Equipment.KICKBOARD
        }
        val effStroke = if (k == SetKind.KICK && stroke == Stroke.IM) Stroke.CHOICE else stroke
        var target = Pace.targetSeconds(profile, dist, effStroke, k, z, css)
        if (Equipment.FINS in eq) target = (target * 0.88).roundToInt()
        if (Equipment.PADDLES in eq) target = (target * 0.97).roundToInt()
        val rest = restSec ?: Pace.restSeconds(profile, dist, z)
        val restOnly = forceRest || usesRestNotSendOff()
        return SetItem(
            reps = reps.coerceAtLeast(1),
            distance = dist,
            stroke = effStroke,
            kind = k,
            zone = z,
            pattern = pattern,
            equipment = eq,
            drillId = drillId,
            targetSec = target,
            sendOffSec = if (restOnly) null else Pace.sendOff(target, rest),
            restSec = if (restOnly) rest else null,
            note = n,
        )
    }

    /** Reps that fit [budgetSec], clamped. */
    private fun fit(budgetSec: Int, unit: SetItem, min: Int = 1, max: Int = 40): Int {
        val perRep = unit.estimatedSec / unit.reps.coerceAtLeast(1)
        return count(budgetSec, perRep, min, max)
    }

    /**
     * How many units of [unitSec] fit in [budgetSec]. [min] is soft: short sessions may
     * go down to half of it so a 20-minute swim doesn't balloon into 40.
     */
    private fun count(budgetSec: Int, unitSec: Int, min: Int, max: Int): Int {
        val raw = budgetSec / unitSec.coerceAtLeast(1)
        return when {
            raw >= min -> raw.coerceAtMost(max)
            else -> maxOf(raw, (min + 1) / 2, 1)
        }
    }

    /** Builds an item with as many reps as fit the budget. */
    private fun fill(
        ctx: Ctx,
        budgetSec: Int,
        min: Int = 1,
        max: Int = 40,
        build: (reps: Int) -> SetItem,
    ): SetItem {
        val probe = build(1)
        return build(fit(budgetSec, probe, min, max)).let {
            // Even counts read better for alternating patterns.
            if ((it.pattern == EffortPattern.FAST_EASY || it.pattern == EffortPattern.EASY_FAST) && it.reps % 2 == 1) {
                build(it.reps + 1)
            } else it
        }
    }

    private fun distanceForSeconds(seconds: Int, stroke: Stroke, kind: SetKind, zone: Zone): Int {
        val per100 = Pace.targetSeconds(profile, 100, stroke, kind, zone, css)
        val raw = seconds * 100 / per100.coerceAtLeast(30)
        val step = if (raw >= 300) 100 else 50
        return (raw / step * step).coerceAtLeast(pool.length).let { d(it) }
    }

    private fun drillsFor(ctx: Ctx, stroke: Stroke, count: Int): List<Drill> {
        val s = when (stroke) {
            Stroke.IM -> ctx.pick(imStrokesFor())
            Stroke.CHOICE -> Stroke.FREESTYLE
            else -> stroke
        }
        // Fall back to the easiest drills for the stroke if none match the swimmer's level yet.
        val drillLevel = Level.entries.drop(level.ordinal).firstOrNull { Drills.suitable(s, it).isNotEmpty() } ?: return emptyList()
        val focus = ctx.pick(DrillFocus.entries.filter { f -> Drills.suitable(s, drillLevel).any { f in it.focus } })
        val options = Drills.suitable(s, drillLevel, focus)
        val top = options.take((count + 2).coerceAtMost(options.size)).shuffled(ctx.rnd)
        return top.take(count)
    }

    // ------------------------------------------------------------------ blocks

    private fun warmup(ctx: Ctx, budget: Int, type: SessionType): WorkoutBlock {
        val items = mutableListOf<SetItem>()
        val easyBudget = (budget * 0.45).toInt()
        val kickBudget = (budget * 0.25).toInt()
        val buildBudget = budget - easyBudget - kickBudget

        if (level == Level.BEGINNER) {
            items += fill(ctx, easyBudget, 2, 12) { item(ctx, it, 25, Stroke.FREESTYLE, zone = Zone.RECOVERY, note = "Easy, long strokes. Rest at the wall as needed.") }
        } else {
            val easyStroke = if (type == SessionType.IM) Stroke.CHOICE else Stroke.FREESTYLE
            val dist = distanceForSeconds(easyBudget, easyStroke, SetKind.SWIM, Zone.RECOVERY)
                .coerceAtMost(maxRep(Stroke.FREESTYLE) * 2)
            val note = if (type == SessionType.IM || ctx.rnd.nextBoolean()) "Mix strokes: 50 free / 50 back." else "Easy, loosen up, breathe every 3."
            items += item(ctx, 1, dist, easyStroke, zone = Zone.RECOVERY, note = note, forceRest = true, restSec = 15)
        }
        if (kickBudget >= 60) {
            val kickStroke = if (type == SessionType.IM) Stroke.CHOICE else Stroke.FREESTYLE
            items += fill(ctx, kickBudget, 2, 8) {
                item(ctx, it, if (level == Level.BEGINNER) 25 else 50, kickStroke, SetKind.KICK, Zone.AEROBIC,
                    note = "Choice kick. Long legs, kick from the hips.")
            }
        }
        if (buildBudget >= 60) {
            items += fill(ctx, buildBudget, 2, 8) {
                item(ctx, it, if (level <= Level.NOVICE) 25 else 50, Stroke.FREESTYLE, zone = Zone.TEMPO,
                    pattern = EffortPattern.BUILD, note = "Build each rep from easy to fast.")
            }
        }
        val note = when (profile.ageGroup) {
            AgeGroup.SENIOR, AgeGroup.SENIOR_PLUS, AgeGroup.MASTERS -> "Take the warm-up seriously — joints and heart need longer to get going."
            else -> null
        }
        return WorkoutBlock(BlockType.WARMUP, "Warm-up", items, note = note)
    }

    private fun skills(ctx: Ctx, budget: Int, type: SessionType, stroke: Stroke): WorkoutBlock {
        val count = when {
            budget < 240 -> 1
            budget < 600 -> 2
            else -> 3
        }
        val drills = drillsFor(ctx, stroke, count)
        val per = budget / drills.size.coerceAtLeast(1)
        val items = drills.map { drill ->
            val useFins = drill.finsHelp && level <= Level.INTERMEDIATE && has(Equipment.FINS)
            val drillOnly = level == Level.BEGINNER || ctx.rnd.nextBoolean()
            val repDist = if (drillOnly) drill.repDistance else drill.repDistance * 2
            ctx.cues += drill.cues.first()
            fill(ctx, per, 2, 10) { reps ->
                item(
                    ctx, reps, repDist, drill.stroke.takeIf { it != Stroke.CHOICE } ?: Stroke.FREESTYLE,
                    if (drillOnly) SetKind.DRILL else SetKind.DRILL_SWIM,
                    Zone.AEROBIC,
                    equipment = drill.equipment + if (useFins) setOf(Equipment.FINS) else emptySet(),
                    drillId = drill.id,
                    note = if (drillOnly) drill.name else "${drill.name} — ${drill.repDistance} drill / ${drill.repDistance} swim",
                    forceRest = true,
                    restSec = if (level == Level.BEGINNER) 20 else 10,
                )
            }
        }.toMutableList()

        // Walls are free speed: add a short turns/streamline element for swimmers who can turn.
        if (level >= Level.NOVICE && type != SessionType.RECOVERY && budget >= 360 && ctx.rnd.nextInt(3) == 0) {
            val turn = if (stroke == Stroke.BREASTSTROKE || stroke == Stroke.BUTTERFLY) "gen_open_turn" else "gen_flip_turn"
            items += item(ctx, 4, 25, Stroke.FREESTYLE, SetKind.DRILL, Zone.TEMPO, drillId = turn,
                note = "Fast into the wall, perfect turn, tight streamline + 3 kicks off every wall.", forceRest = true, restSec = 15)
            ctx.cues += "Don't breathe on your first stroke off the wall."
        }
        val title = if (stroke.isCompetitive) "${stroke.label} drills" else "Technique drills"
        return WorkoutBlock(BlockType.SKILLS, title, items, note = "Quality over speed. Read each drill's how-to before you start.")
    }

    private fun preset(ctx: Ctx, budget: Int, type: SessionType, stroke: Stroke): WorkoutBlock {
        val kickStroke = when (stroke) {
            Stroke.BUTTERFLY, Stroke.BREASTSTROKE, Stroke.BACKSTROKE -> stroke
            else -> Stroke.FREESTYLE
        }
        val kick = type == SessionType.SPEED || type == SessionType.TECHNIQUE || ctx.rnd.nextBoolean() || !has(Equipment.PULL_BUOY)
        val dist = if (level == Level.BEGINNER) 25 else ctx.pick(listOf(50, 100))
        return if (kick) {
            val (zone, pattern, note) = when (type) {
                SessionType.SPEED, SessionType.RACE_PACE ->
                    Triple(Zone.VO2, EffortPattern.FAST_EASY, "Odds fast, evens easy. Fast kicks wake up the legs for the main set.")
                SessionType.THRESHOLD ->
                    Triple(Zone.TEMPO, EffortPattern.DESCEND, "Descend 1 to ${'$'}n — last one strong.")
                else -> Triple(Zone.AEROBIC, EffortPattern.STEADY, "Steady kick. Point the toes, kick from the hips.")
            }
            val it = fill(ctx, budget, 2, 12) { r -> item(ctx, r, dist, kickStroke, SetKind.KICK, zone, pattern, note = note) }
            val fixed = it.copy(note = it.note?.replace("${'$'}n", it.reps.toString()))
            val withFins = if (type == SessionType.SPEED && has(Equipment.FINS) && level >= Level.INTERMEDIATE) {
                fixed.copy(equipment = fixed.equipment + Equipment.FINS, note = fixed.note + " Fins on for speed.")
            } else fixed
            WorkoutBlock(BlockType.PRESET, "${kickStroke.short} kick set", listOf(withFins))
        } else {
            val usePaddles = has(Equipment.PADDLES) && level >= Level.INTERMEDIATE
            val pullDist = if (level <= Level.NOVICE) 50 else ctx.pick(listOf(100, 200))
            val it = fill(ctx, budget, 2, 10) { r ->
                item(ctx, r, pullDist, Stroke.FREESTYLE, SetKind.PULL, Zone.AEROBIC, EffortPattern.NEGATIVE_SPLIT,
                    equipment = setOfNotNull(Equipment.PULL_BUOY, Equipment.PADDLES.takeIf { usePaddles }),
                    note = "Strong catch, high elbow. Breathe every 3 or 5." + if (usePaddles) " Paddles only if your shoulders feel good." else "")
            }
            WorkoutBlock(BlockType.PRESET, "Pull set", listOf(it))
        }
    }

    private fun cooldown(ctx: Ctx, budget: Int): WorkoutBlock {
        val items = mutableListOf<SetItem>()
        if (level == Level.BEGINNER) {
            items += fill(ctx, budget, 2, 6) { item(ctx, it, 25, Stroke.CHOICE, zone = Zone.RECOVERY, note = "Very easy, any stroke. Long exhales.") }
        } else {
            val dist = distanceForSeconds((budget * 0.75).toInt(), Stroke.CHOICE, SetKind.SWIM, Zone.RECOVERY)
            items += item(ctx, 1, dist, Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy, mix of backstroke and freestyle.", forceRest = true, restSec = 10)
            if (budget >= 180 && ctx.rnd.nextBoolean()) {
                items += item(ctx, 2, 25, Stroke.CHOICE, SetKind.SCULL, Zone.RECOVERY, drillId = "free_scull_front",
                    note = "Front scull — easy feel for the water.", forceRest = true, restSec = 10)
            }
        }
        return WorkoutBlock(BlockType.COOLDOWN, "Cool-down", items, note = "Bring the heart rate down. Stretch shoulders and hips after.")
    }

    private fun cssTest(ctx: Ctx): List<WorkoutBlock> {
        val warm = WorkoutBlock(
            BlockType.WARMUP, "Thorough warm-up", listOf(
                item(ctx, 1, 400, Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy mixed strokes.", forceRest = true, restSec = 20),
                item(ctx, 4, 50, Stroke.FREESTYLE, SetKind.KICK, Zone.AEROBIC, forceRest = true, restSec = 15),
                item(ctx, 4, 50, Stroke.FREESTYLE, zone = Zone.TEMPO, pattern = EffortPattern.BUILD, forceRest = true, restSec = 20),
                item(ctx, 2, 25, Stroke.FREESTYLE, zone = Zone.SPRINT, note = "Two fast 25s to prime race speed.", forceRest = true, restSec = 45),
                item(ctx, 1, 100, Stroke.FREESTYLE, zone = Zone.RECOVERY, forceRest = true, restSec = 60),
            )
        )
        val main = WorkoutBlock(
            BlockType.MAIN, "400 + 200 time trial", listOf(
                SetItem(1, d(400), Stroke.FREESTYLE, zone = Zone.THRESHOLD, pattern = EffortPattern.STEADY,
                    restSec = 300, note = "400 all-out but EVEN pace — don't sprint the first 100. Record your time."),
                SetItem(1, d(200), Stroke.FREESTYLE, zone = Zone.RECOVERY, restSec = 30,
                    note = "Easy recovery swim (or rest 5–6 min) until fully recovered."),
                SetItem(1, d(200), Stroke.FREESTYLE, zone = Zone.VO2, restSec = 60,
                    note = "200 all-out. Record your time."),
            ),
            note = "Enter both times in Tools → CSS test. Your CSS = (400 time − 200 time) ÷ 2 per 100. All training paces update automatically.",
        )
        ctx.cues += "Pace the 400 evenly: your first 100 should be within 2–3 s of your last."
        ctx.cues += "Use a push start, not a dive, for both trials so retests are comparable."
        return listOf(warm, main, cooldown(ctx, 300))
    }

    // ------------------------------------------------------------------ main sets

    private fun main(ctx: Ctx, budget: Int, type: SessionType, stroke: Stroke): WorkoutBlock {
        val templates: List<(Ctx, Int, Stroke) -> WorkoutBlock> = when (type) {
            SessionType.ENDURANCE -> listOf(::enduranceRepeats, ::endurancePyramid, ::enduranceRounds, ::enduranceDescend)
            SessionType.THRESHOLD -> listOf(::cssHundreds, ::thresholdLong, ::thresholdBroken, ::thresholdLadder)
            SessionType.SPEED -> listOf(::speedFifties, ::sprintTwentyFives, ::speedBroken, ::speedLadder)
            SessionType.RACE_PACE -> listOf(::racePace25s, ::racePace50s, ::racePaceBroken)
            SessionType.TECHNIQUE -> listOf(::techniqueDps, ::techniqueProgression, ::techniqueTempo)
            SessionType.RECOVERY -> listOf(::recoveryEasy)
            SessionType.IM -> listOf(::imHundreds, ::imOrder, ::imSwitch)
            SessionType.OPEN_WATER -> listOf(::openWaterContinuous, ::openWaterSurges, ::openWaterRaceSim)
            SessionType.CSS_TEST -> listOf(::cssHundreds)
        }
        return ctx.pick(templates)(ctx, budget, stroke)
    }

    private fun swimStroke(stroke: Stroke) = if (stroke == Stroke.CHOICE) Stroke.FREESTYLE else stroke

    private fun enduranceRepeats(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val options = listOf(100, 200, 300, 400, 500).map { d(it) }.filter { it <= maxRep(s) }.ifEmpty { listOf(maxRep(s)) }
        val dist = ctx.pick(options.takeLast(3))
        val pattern = ctx.pick(listOf(EffortPattern.STEADY, EffortPattern.NEGATIVE_SPLIT))
        val it = fill(ctx, budget, 2, 30) { r ->
            item(ctx, r, dist, s, zone = Zone.AEROBIC, pattern = pattern, note = "Hold every rep within 2 seconds. Smooth and relaxed.")
        }
        ctx.cues += "Aerobic pace should feel like you could keep going much longer."
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} aerobic", listOf(it))
    }

    private fun endurancePyramid(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val step = d(if (level <= Level.NOVICE) 25 else if (level == Level.INTERMEDIATE) 50 else 100)
        var peak = 2
        fun build(p: Int): List<SetItem> {
            val ups = (1..p).toList() + (p - 1 downTo 1)
            return ups.map { m ->
                val zone = if (m == p) Zone.TEMPO else Zone.AEROBIC
                item(ctx, 1, step * m, s, zone = zone, note = if (m == p) "Top of the pyramid — strong!" else null)
            }
        }
        while (peak < 8 && step * (peak + 1) <= maxRep(s) && build(peak + 1).sumOf { it.estimatedSec } <= budget) peak++
        val items = build(peak)
        ctx.cues += "Settle into a rhythm on the way up, push the pace on the way down."
        val ladder = items.joinToString("-") { it.distance.toString() }
        return WorkoutBlock(BlockType.MAIN, "Pyramid $ladder", items, note = "Aerobic on the way up, try to swim faster on the way down.")
    }

    private fun enduranceRounds(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val unit = d(if (level <= Level.NOVICE) 50 else 100)
        val items = listOf(
            item(ctx, 1, (unit * 3).coerceAtMost(maxRep(s)), s, zone = Zone.AEROBIC, note = "Steady swim"),
            item(ctx, 1, (unit * 2).coerceAtMost(maxRep(Stroke.FREESTYLE)), Stroke.FREESTYLE, SetKind.PULL, Zone.AEROBIC, equipment = setOf(Equipment.PULL_BUOY), note = "Pull, long strokes"),
            item(ctx, 1, unit, s, zone = Zone.TEMPO, pattern = EffortPattern.BUILD, note = "Build to strong"),
        )
        val roundSec = items.sumOf { it.estimatedSec } + 30
        val rounds = count(budget, roundSec, 1, 6)
        ctx.cues += "Use the pull to focus on catch; use the build to practise holding form when fast."
        return WorkoutBlock(BlockType.MAIN, "$rounds rounds swim/pull/build", items, rounds = rounds, roundRestSec = 30)
    }

    private fun enduranceDescend(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val dist = d(if (level <= Level.NOVICE) 50 else 100).coerceAtMost(maxRep(s))
        val per = item(ctx, 4, dist, s, zone = Zone.TEMPO, pattern = EffortPattern.DESCEND, note = "Descend 1–4: start aerobic, finish at tempo.")
        val easy = item(ctx, 1, dist, Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy", forceRest = true, restSec = 10)
        val rounds = count(budget, (per.estimatedSec + easy.estimatedSec), 1, 6)
        return WorkoutBlock(BlockType.MAIN, "$rounds× (4×$dist descend + $dist easy)", listOf(per, easy), rounds = rounds)
    }

    private fun cssHundreds(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val dist = d(if (level == Level.BEGINNER) 50 else 100).coerceAtMost(maxRep(s).coerceAtLeast(d(50)))
        val it = fill(ctx, budget, 3, 30) { r ->
            item(ctx, r, dist, s, zone = Zone.THRESHOLD, note = "Hold CSS pace on every rep. Short rest. If you can't make the interval, add 5 s.")
        }
        ctx.cues += "Threshold should feel hard but controlled — even splits from first to last."
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} at CSS", listOf(it))
    }

    private fun thresholdLong(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val dist = d(if (level <= Level.NOVICE) 100 else 200).coerceAtMost(maxRep(s))
        val it = fill(ctx, budget, 2, 15) { r ->
            item(ctx, r, dist, s, zone = Zone.TEMPO, pattern = EffortPattern.NEGATIVE_SPLIT, note = "Just slower than CSS. Second half of each rep faster.")
        }
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} tempo-threshold", listOf(it))
    }

    private fun thresholdBroken(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val dist = d(if (level == Level.BEGINNER) 25 else if (level == Level.NOVICE) 50 else 100).coerceAtMost(maxRep(s))
        val hard = item(ctx, 4, dist, s, zone = Zone.THRESHOLD, note = "At CSS pace.")
        val easy = item(ctx, 1, d(100), Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy recovery", forceRest = true, restSec = 15)
        val rounds = count(budget, (hard.estimatedSec + easy.estimatedSec), 1, 8)
        ctx.cues += "Use the easy swim to reset your technique, not just to rest."
        return WorkoutBlock(BlockType.MAIN, "$rounds× (4×$dist CSS + 100 easy)", listOf(hard, easy), rounds = rounds)
    }

    private fun thresholdLadder(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val unit = d(if (level <= Level.NOVICE) 50 else 100)
        val steps = listOf(1, 2, 3, 2, 1).map { (unit * it).coerceAtMost(maxRep(s)) }
        val items = steps.map { item(ctx, 1, it, s, zone = Zone.THRESHOLD) }
        val rounds = count(budget, (items.sumOf { it.estimatedSec } + 60), 1, 6)
        ctx.cues += "Same pace per 100 on every step of the ladder."
        return WorkoutBlock(BlockType.MAIN, "CSS ladder ${steps.joinToString("-")}", items, rounds = rounds, roundRestSec = 60,
            note = "Hold the same per-100 pace on every distance.")
    }

    private fun speedFifties(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val dist = d(if (level == Level.BEGINNER) 25 else 50).coerceAtMost(maxRep(s).coerceAtLeast(d(25)))
        val it = fill(ctx, budget, 4, 20) { r ->
            item(ctx, r, dist, s, zone = Zone.VO2, pattern = EffortPattern.FAST_EASY, note = "Odds very fast, evens easy. Hold technique at speed.")
        }
        ctx.cues += "Fast but long: keep your stroke count within 2 of your easy count."
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} fast/easy", listOf(it))
    }

    private fun sprintTwentyFives(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val sprint = item(ctx, 1, 25, s, zone = Zone.SPRINT, note = if (pool.length == 50) "Sprint the first 15–20 m, then easy." else "All-out sprint, no breath first 5 strokes if you can.", forceRest = true, restSec = 5)
        val easy = item(ctx, 1, 25, Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy back to recover", forceRest = true, restSec = 20)
        val rounds = count(budget, (sprint.estimatedSec + easy.estimatedSec), 4, 16)
        ctx.cues += "Sprinting = fast turnover with a firm catch. Don't spin your wheels."
        return WorkoutBlock(BlockType.MAIN, "$rounds× (25 sprint + 25 easy)", listOf(sprint, easy), rounds = rounds)
    }

    private fun speedBroken(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val fast = item(ctx, 4, 25, s, zone = Zone.VO2, note = "Broken 100: 10 s rest between 25s, race speed.", forceRest = true, restSec = 10)
        val easy = item(ctx, 1, d(100), Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy", forceRest = true, restSec = 30)
        val rounds = count(budget, (fast.estimatedSec + easy.estimatedSec), 2, 6)
        ctx.cues += "Add up the four 25 times — that's your broken 100. Try to beat it each round."
        return WorkoutBlock(BlockType.MAIN, "$rounds× broken 100", listOf(fast, easy), rounds = rounds)
    }

    private fun speedLadder(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val items = listOf(
            item(ctx, 1, d(100).coerceAtMost(maxRep(s)), s, zone = Zone.TEMPO),
            item(ctx, 1, d(75).coerceAtMost(maxRep(s)), s, zone = Zone.THRESHOLD),
            item(ctx, 1, d(50).coerceAtMost(maxRep(s)), s, zone = Zone.VO2),
            item(ctx, 1, 25, s, zone = Zone.SPRINT, forceRest = true, restSec = 30),
            item(ctx, 1, d(100), Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy", forceRest = true, restSec = 20),
        )
        val rounds = count(budget, items.sumOf { it.estimatedSec }, 1, 5)
        return WorkoutBlock(BlockType.MAIN, "$rounds× speed ladder 100-75-50-25", items, rounds = rounds,
            note = "Shorter = faster. Each step a gear up.")
    }

    private fun withTarget(item: SetItem, target: Int, rest: Int): SetItem =
        item.copy(targetSec = target, sendOffSec = item.sendOffSec?.let { Pace.sendOff(target, rest) })

    private fun racePace25s(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val dist = d(25)
        val target = (Pace.predictRace(profile, 100, s) * dist / 100.0).roundToInt()
        val it = fill(ctx, budget, 10, 30) { r ->
            withTarget(
                item(ctx, r, dist, s, zone = Zone.VO2, pattern = EffortPattern.HOLD_BEST, restSec = 15,
                    note = "Goal 100 race pace: ${Pace.format(target)} per $dist. Miss twice in a row → sit out one rep. Stop after the third miss."),
                target, 15,
            )
        }
        ctx.cues += "Race-pace training: every rep at goal speed, stroke rate and breathing pattern."
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×$dist at 100 race pace", listOf(it))
    }

    private fun racePace50s(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val dist = d(50)
        val target = (Pace.predictRace(profile, 200, s) * dist / 200.0).roundToInt()
        val it = fill(ctx, budget, 6, 24) { r ->
            withTarget(
                item(ctx, r, dist, s, zone = Zone.THRESHOLD, pattern = EffortPattern.HOLD_BEST, restSec = 20,
                    note = "Goal 200 race pace: ${Pace.format(target)} per $dist. Fail twice → stop the set."),
                target, 20,
            )
        }
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×$dist at 200 race pace", listOf(it))
    }

    private fun racePaceBroken(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke)
        val race200 = Pace.predictRace(profile, 200, s)
        val fast = item(ctx, 4, 50, s, zone = Zone.THRESHOLD, restSec = 10,
            note = "Broken 200 at goal pace (${Pace.format(race200 / 4)} per 50), 10 s rest.", forceRest = true)
            .let { if (it.distance == 50) it.copy(targetSec = race200 / 4) else it }
        val easy = item(ctx, 1, d(200), Stroke.CHOICE, zone = Zone.RECOVERY, note = "Easy", forceRest = true, restSec = 30)
        val rounds = count(budget, (fast.estimatedSec + easy.estimatedSec), 2, 5)
        return WorkoutBlock(BlockType.MAIN, "$rounds× broken 200", listOf(fast, easy), rounds = rounds,
            note = "Add the four splits (minus rest) — that's your simulated 200 time.")
    }

    private fun techniqueDps(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke).takeIf { it != Stroke.IM } ?: Stroke.FREESTYLE
        val drillId = if (level >= Level.INTERMEDIATE && ctx.rnd.nextBoolean()) "free_golf" else "free_dps"
        val it = fill(ctx, budget, 4, 16) { r ->
            item(ctx, r, if (level == Level.BEGINNER) 25 else 50, s, zone = Zone.AEROBIC, drillId = drillId,
                note = if (drillId == "free_golf") "Swim golf: score = strokes + seconds. Lower it each rep." else "Count strokes. Drop one stroke per length if you can, same effort.")
        }
        ctx.cues += "Fewer strokes at the same speed = better technique."
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} stroke count", listOf(it))
    }

    private fun techniqueProgression(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke).takeIf { it != Stroke.IM } ?: Stroke.FREESTYLE
        val drill = drillsFor(ctx, s, 1).firstOrNull()
        val unit = d(if (level == Level.BEGINNER) 25 else 50)
        val items = listOf(
            item(ctx, 1, unit, drill?.stroke?.takeIf { it != Stroke.CHOICE } ?: s, SetKind.DRILL, Zone.AEROBIC, drillId = drill?.id,
                note = drill?.name ?: "Drill", forceRest = true, restSec = 10),
            item(ctx, 1, unit, s, zone = Zone.AEROBIC, note = "Swim, applying the drill's feel", forceRest = true, restSec = 10),
            item(ctx, 1, unit, s, zone = Zone.TEMPO, pattern = EffortPattern.BUILD, note = "Build — keep the technique at speed", forceRest = true, restSec = 20),
        )
        val rounds = count(budget, items.sumOf { it.estimatedSec }, 2, 8)
        drill?.let { ctx.cues += it.cues.first() }
        return WorkoutBlock(BlockType.MAIN, "$rounds× drill → swim → build", items, rounds = rounds)
    }

    private fun techniqueTempo(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val s = swimStroke(stroke).takeIf { it != Stroke.IM } ?: Stroke.FREESTYLE
        val it = fill(ctx, budget, 4, 16) { r ->
            item(ctx, r, d(if (level == Level.BEGINNER) 50 else 100), s, zone = Zone.TEMPO, pattern = EffortPattern.FAST_EASY,
                note = "Odd reps: long and slow, minimum strokes. Even reps: quick turnover, same stroke length. Feel the difference.")
        }
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} tempo contrast", listOf(it))
    }

    private fun recoveryEasy(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val dist = d(if (level == Level.BEGINNER) 50 else 100)
        val it = fill(ctx, (budget * 0.8).toInt(), 2, 20) { r ->
            item(ctx, r, dist, Stroke.CHOICE, zone = Zone.RECOVERY, note = "Very easy. Every third rep backstroke. Focus on long exhales.", forceRest = true, restSec = 15)
        }
        val scull = item(ctx, 4, 25, Stroke.CHOICE, SetKind.SCULL, Zone.RECOVERY, drillId = "gen_scull_mid",
            equipment = setOf(Equipment.PULL_BUOY), note = "Mid scull: feel the water.", forceRest = true, restSec = 10)
        ctx.cues += "Recovery means easy. If in doubt, go slower."
        return WorkoutBlock(BlockType.MAIN, "Easy aerobic + sculling", listOf(it, scull))
    }

    private fun imStrokesFor(): List<Stroke> =
        if (level == Level.BEGINNER) listOf(Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.FREESTYLE)
        else Stroke.four

    private fun imHundreds(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        if (level <= Level.NOVICE) return imOrder(ctx, budget, stroke)
        val dist = d(if (level == Level.INTERMEDIATE) 100 else ctx.pick(listOf(100, 200)))
        val it = fill(ctx, budget, 3, 16) { r ->
            item(ctx, r, dist, Stroke.IM, zone = Zone.TEMPO, pattern = EffortPattern.DESCEND,
                note = "Fly–back–breast–free. Descend in groups of 3.")
        }
        ctx.cues += "IM transitions: back-to-breast is where races are won. Practise the crossover turns."
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} IM", listOf(it))
    }

    private fun imOrder(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val unit = d(if (level <= Level.NOVICE) 25 else 50)
        val items = imStrokesFor().map { st ->
            val isFlyHard = st == Stroke.BUTTERFLY && level <= Level.NOVICE
            item(ctx, 1, unit, st, if (isFlyHard) SetKind.DRILL else SetKind.SWIM, Zone.AEROBIC,
                drillId = if (isFlyHard) "fly_body_dolphin" else null,
                note = if (isFlyHard) "Body dolphin / single-arm fly instead of full fly." else null)
        }
        val rounds = count(budget, (items.sumOf { it.estimatedSec } + 20), 2, 8)
        ctx.cues += "Smooth technique in every stroke — no 'survival' fly."
        return WorkoutBlock(BlockType.MAIN, "$rounds× IM order by $unit", items, rounds = rounds, roundRestSec = 20,
            note = "Each round faster than the last, or alternate easy/strong rounds.")
    }

    private fun imSwitch(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val unit = d(if (level <= Level.NOVICE) 25 else 50)
        val strokes = imStrokesFor().filter { it != Stroke.FREESTYLE }
        val items = strokes.map { st ->
            val flyDrill = st == Stroke.BUTTERFLY && level <= Level.NOVICE
            item(ctx, 2, unit * 2, st, if (flyDrill) SetKind.DRILL_SWIM else SetKind.SWIM, Zone.AEROBIC,
                drillId = if (flyDrill) "fly_single_arm" else null,
                note = "First $unit ${st.short.lowercase()}${if (flyDrill) " (single-arm fly drill)" else ""}, second $unit free.")
        }
        val rounds = count(budget, items.sumOf { it.estimatedSec }, 1, 4)
        return WorkoutBlock(BlockType.MAIN, "IM switch ${unit * 2}s", items, rounds = rounds,
            note = "Each rep: half form stroke, half freestyle.")
    }

    private fun openWaterContinuous(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val dist = distanceForSeconds(budget - 30, Stroke.FREESTYLE, SetKind.SWIM, Zone.AEROBIC)
        val items = listOf(
            item(ctx, 1, dist, Stroke.FREESTYLE, zone = Zone.AEROBIC, drillId = "ow_sighting", forceRest = true, restSec = 0,
                note = "Continuous. Sight every 6–8 strokes, wall-free turns (no push-off), bilateral breathing."),
        )
        ctx.cues += "Lift the eyes, not the head, then roll to breathe."
        return WorkoutBlock(BlockType.MAIN, "${dist} continuous, open-water style", items)
    }

    private fun openWaterSurges(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val steady = item(ctx, 1, d(if (level <= Level.NOVICE) 100 else 200), Stroke.FREESTYLE, zone = Zone.AEROBIC, drillId = "ow_sighting",
            note = "Steady, sight every 6 strokes.", forceRest = true, restSec = 0)
        val surge = item(ctx, 1, d(50), Stroke.FREESTYLE, zone = Zone.VO2, drillId = "free_tarzan",
            note = "Surge! Like rounding a buoy or breaking from a pack. First 4 strokes head-up.", forceRest = true, restSec = 15)
        val rounds = count(budget, (steady.estimatedSec + surge.estimatedSec), 2, 10)
        ctx.cues += "Practise changing pace without losing your stroke."
        return WorkoutBlock(BlockType.MAIN, "$rounds× (steady + surge)", listOf(steady, surge), rounds = rounds,
            note = "No touching the wall on the surge 50s — turn just before it.")
    }

    private fun openWaterRaceSim(ctx: Ctx, budget: Int, stroke: Stroke): WorkoutBlock {
        val dist = d(if (level <= Level.NOVICE) 200 else 400).coerceAtMost(maxRep(Stroke.FREESTYLE))
        val it = fill(ctx, budget, 2, 8) { r ->
            item(ctx, r, dist, Stroke.FREESTYLE, zone = Zone.TEMPO, pattern = EffortPattern.NEGATIVE_SPLIT,
                note = "Start-sprint the first 50 (race start), then settle into tempo. Sight every 8 strokes. Wall-free turns.")
        }
        return WorkoutBlock(BlockType.MAIN, "${it.reps}×${it.distance} race simulation", listOf(it))
    }

    private fun summary(workout: Workout, main: WorkoutBlock?): String {
        val base = "${workout.totalDistance} ${pool.unit} · ~${workout.estimatedMinutes} min · ${workout.type.description}"
        return if (main != null) "$base Main set: ${main.title}." else base
    }
}
