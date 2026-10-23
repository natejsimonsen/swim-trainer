package com.swimtrainer.core.engine

import com.swimtrainer.core.model.AgeGroup
import com.swimtrainer.core.model.Level
import com.swimtrainer.core.model.SetKind
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.Zone
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

object Pace {

    /** Typical freestyle CSS (s/100m) for an adult at each level. */
    private fun baseCssMeters(level: Level) = when (level) {
        Level.BEGINNER -> 165.0
        Level.NOVICE -> 130.0
        Level.INTERMEDIATE -> 108.0
        Level.ADVANCED -> 90.0
        Level.ELITE -> 75.0
    }

    /** Age adjustment: speed peaks in the 20s and declines ~0.6–1%/year after 35. */
    fun ageFactor(age: Int): Double = when {
        age < 10 -> 1.25
        age < 13 -> 1.12
        age < 16 -> 1.04
        age <= 35 -> 1.0
        age <= 60 -> 1.0 + (age - 35) * 0.006
        else -> 1.15 + (age - 60) * 0.01
    }

    /** CSS in seconds per 100 pool units: measured if available, otherwise estimated. */
    fun css(profile: SwimmerProfile): Double {
        profile.cssPer100?.let { return it }
        val meters = baseCssMeters(profile.level) * ageFactor(profile.age)
        return if (profile.pool.yards) meters * 0.9144 else meters
    }

    /**
     * Critical swim speed from a 400 and 200 time trial (seconds).
     * CSS (s/100) = (T400 - T200) / 2.
     */
    fun cssFromTest(t400Sec: Double, t200Sec: Double): Double? {
        if (t400Sec <= 0 || t200Sec <= 0 || t400Sec <= t200Sec) return null
        val css = (t400Sec - t200Sec) / 2.0
        // Sanity: elite ~60s/100, very slow ~240s/100.
        return css.takeIf { it in 50.0..300.0 }
    }

    fun strokeFactor(stroke: Stroke, level: Level): Double {
        val base = when (stroke) {
            Stroke.FREESTYLE, Stroke.CHOICE -> 1.0
            Stroke.BACKSTROKE -> 1.11
            Stroke.BREASTSTROKE -> 1.24
            Stroke.BUTTERFLY -> 1.10
            Stroke.IM -> 1.12
        }
        // Less experienced swimmers lose more speed in the non-free strokes.
        val penalty = when (level) {
            Level.BEGINNER -> 1.10
            Level.NOVICE -> 1.06
            Level.INTERMEDIATE -> 1.02
            else -> 1.0
        }
        return if (stroke == Stroke.FREESTYLE || stroke == Stroke.CHOICE) base else base * penalty
    }

    fun kindFactor(kind: SetKind, level: Level): Double = when (kind) {
        SetKind.SWIM -> 1.0
        SetKind.PULL -> 1.03
        SetKind.KICK -> when (level) {
            Level.BEGINNER -> 1.75
            Level.NOVICE -> 1.6
            Level.INTERMEDIATE -> 1.45
            Level.ADVANCED -> 1.35
            Level.ELITE -> 1.3
        }
        SetKind.DRILL -> 1.3
        SetKind.DRILL_SWIM -> 1.15
        SetKind.SCULL -> 2.0
    }

    /** Short reps can be swum faster than CSS at the same effort. */
    private fun distanceFactor(distance: Int, zone: Zone): Double {
        if (zone.ordinal < Zone.THRESHOLD.ordinal) return 1.0
        return when {
            distance <= 25 -> 0.94
            distance <= 50 -> 0.97
            distance <= 100 -> 1.0
            distance <= 200 -> 1.015
            else -> 1.03
        }
    }

    /** Target time in seconds for one rep. */
    fun targetSeconds(
        profile: SwimmerProfile,
        distance: Int,
        stroke: Stroke,
        kind: SetKind,
        zone: Zone,
        cssOverride: Double? = null,
    ): Int {
        val css = cssOverride ?: css(profile)
        val per100 = css * zone.paceFactor * strokeFactor(stroke, profile.level) *
            kindFactor(kind, profile.level) * distanceFactor(distance, zone)
        return (per100 * distance / 100.0).roundToInt()
    }

    /** Rest after a rep, based on zone, rep length, level and age. */
    fun restSeconds(profile: SwimmerProfile, distance: Int, zone: Zone): Int {
        val perHundred = when (zone) {
            Zone.RECOVERY -> 10.0
            Zone.AEROBIC -> 10.0
            Zone.TEMPO -> 12.0
            Zone.THRESHOLD -> 12.0
            Zone.VO2 -> 25.0
            Zone.SPRINT -> 60.0
        }
        val levelFactor = when (profile.level) {
            Level.BEGINNER -> 2.0
            Level.NOVICE -> 1.5
            Level.INTERMEDIATE -> 1.15
            Level.ADVANCED -> 1.0
            Level.ELITE -> 0.9
        }
        val ageFactor = when (profile.ageGroup) {
            AgeGroup.SENIOR -> 1.2
            AgeGroup.SENIOR_PLUS -> 1.4
            AgeGroup.MASTERS -> 1.1
            else -> 1.0
        }
        val minimum = when (zone) {
            Zone.SPRINT -> 40
            Zone.VO2 -> 15
            else -> 5
        }
        val raw = perHundred * (distance / 100.0).coerceAtLeast(0.5) * levelFactor * ageFactor
        return roundUp(raw.coerceAtLeast(minimum.toDouble()).roundToInt(), 5)
    }

    /** Send-off (interval) rounded up to the nearest 5 seconds, like a pool pace clock. */
    fun sendOff(targetSec: Int, restSec: Int): Int = roundUp(targetSec + restSec, 5)

    fun roundUp(value: Int, step: Int): Int = (ceil(value / step.toDouble()) * step).toInt()

    fun format(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
    }

    fun format(seconds: Double) = format(seconds.roundToInt())

    /** Parses "1:35", "95", "1:35.4" or "0:01:35" to seconds. */
    fun parse(text: String): Double? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val parts = t.split(":")
        return try {
            when (parts.size) {
                1 -> parts[0].toDouble()
                2 -> parts[0].toInt() * 60 + parts[1].toDouble()
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toDouble()
                else -> null
            }?.takeIf { it > 0 }
        } catch (e: NumberFormatException) {
            null
        }
    }

    data class ZonePace(val zone: Zone, val per100Sec: Int, val sendOff100: Int)

    /** Pace table for each zone, per 100 freestyle. */
    fun zoneTable(profile: SwimmerProfile): List<ZonePace> = Zone.entries.map { zone ->
        val target = targetSeconds(profile, 100, Stroke.FREESTYLE, SetKind.SWIM, zone)
        ZonePace(zone, target, sendOff(target, restSeconds(profile, 100, zone)))
    }

    /** Predicted race time using Riegel's formula, anchored on CSS ≈ 1500m pace. */
    fun predictRace(profile: SwimmerProfile, distance: Int, stroke: Stroke = Stroke.FREESTYLE): Int {
        val css = css(profile) * strokeFactor(stroke, profile.level)
        val t1500 = css * 15 * 1.02
        return (t1500 * Math.pow(distance / 1500.0, 1.06)).roundToInt()
    }
}
