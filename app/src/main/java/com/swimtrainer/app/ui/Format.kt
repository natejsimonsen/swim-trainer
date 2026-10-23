package com.swimtrainer.app.ui

import com.swimtrainer.core.content.Drills
import com.swimtrainer.core.engine.Pace
import com.swimtrainer.core.model.EffortPattern
import com.swimtrainer.core.model.SetItem
import com.swimtrainer.core.model.SetKind
import com.swimtrainer.core.model.Stroke
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object Format {

    fun headline(item: SetItem): String {
        val reps = if (item.reps > 1) "${item.reps} × " else ""
        val what = when (item.kind) {
            SetKind.SWIM -> item.stroke.short
            SetKind.KICK -> "${if (item.stroke == Stroke.CHOICE) "Choice" else item.stroke.short} kick"
            SetKind.PULL -> "${item.stroke.short} pull"
            SetKind.DRILL -> drillName(item) ?: "${item.stroke.short} drill"
            SetKind.DRILL_SWIM -> "${item.stroke.short} drill/swim"
            SetKind.SCULL -> "Scull"
        }
        return "$reps${item.distance} $what"
    }

    fun drillName(item: SetItem) = item.drillId?.let { Drills.byId(it)?.name }

    /** "@ 1:50" for intervals or "rest 15s". */
    fun interval(item: SetItem): String? = when {
        item.sendOffSec != null -> "@ ${Pace.format(item.sendOffSec!!)}"
        item.restSec != null && item.restSec!! > 0 -> "rest ${item.restSec}s"
        else -> null
    }

    fun target(item: SetItem): String? {
        val t = item.targetSec ?: return null
        val base = when (item.pattern) {
            EffortPattern.DESCEND -> "finish ≈ ${Pace.format(t)}"
            EffortPattern.BUILD -> "≈ ${Pace.format(t)}"
            EffortPattern.FAST_EASY, EffortPattern.EASY_FAST -> "fast ≈ ${Pace.format((t * 0.96).roundToInt())}"
            else -> "hold ${Pace.format(t)}"
        }
        return base
    }

    fun distance(meters: Int, unit: String): String = "%,d %s".format(Locale.US, meters, unit)

    fun minutes(min: Int): String = if (min >= 60) "${min / 60}h ${min % 60}m" else "$min min"

    fun kgToLb(kg: Double) = kg * 2.20462
    fun lbToKg(lb: Double) = lb / 2.20462
    fun cmToIn(cm: Double) = cm / 2.54
    fun inToCm(inches: Double) = inches * 2.54

    fun weight(kg: Double, imperial: Boolean): String =
        if (imperial) "${kgToLb(kg).roundToInt()} lb" else "${kg.roundToInt()} kg"

    private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

    fun date(date: LocalDate): String = date.format(dayFormatter)

    fun date(epochDay: Long): String = date(LocalDate.ofEpochDay(epochDay))
}
