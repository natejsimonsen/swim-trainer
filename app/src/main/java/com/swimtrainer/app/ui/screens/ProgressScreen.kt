package com.swimtrainer.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.data.AppData
import com.swimtrainer.app.ui.AppViewModel
import com.swimtrainer.app.ui.Format
import com.swimtrainer.app.ui.components.EmptyState
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.SectionHeader
import com.swimtrainer.app.ui.components.StatRow
import com.swimtrainer.app.ui.components.Tag
import com.swimtrainer.core.engine.Pace
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.WorkoutLog

@Composable
fun ProgressScreen(data: AppData, profile: SwimmerProfile, vm: AppViewModel) {
    var toDelete by remember { mutableStateOf<WorkoutLog?>(null) }
    val unit = profile.pool.unit
    ScreenScaffold(title = "Progress", hasBottomNav = true) { padding ->
        Page(padding) {
            val logs = data.logs
            if (logs.isEmpty()) {
                EmptyState("No workouts logged yet", "Finish or log a session and your stats will show up here.")
                return@Page
            }
            val today = vm.today().toEpochDay()
            val monday = today - (vm.today().dayOfWeek.value - 1)

            StatRow(
                "Total distance" to Format.distance(logs.sumOf { it.distance }, unit),
                "Sessions" to "${logs.size}",
            )
            StatRow(
                "Time in water" to Format.minutes(logs.sumOf { it.minutes }),
                "Calories" to "%,d".format(logs.sumOf { it.calories }),
            )
            StatRow(
                "Week streak" to "${weekStreak(logs, monday)} wk",
                "Avg effort" to "%.1f".format(logs.map { it.rpe }.average()),
            )

            SectionHeader("Weekly distance (last 8 weeks)")
            val weeks = (7 downTo 0).map { w ->
                val start = monday - w * 7
                logs.filter { it.epochDay in start until start + 7 }.sumOf { it.distance }
            }
            WeeklyBars(weeks)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("8 wks ago", style = MaterialTheme.typography.labelSmall)
                Text("This week: ${Format.distance(weeks.last(), unit)}", style = MaterialTheme.typography.labelSmall)
            }

            if (data.cssHistory.isNotEmpty()) {
                SectionHeader("CSS history")
                data.cssHistory.takeLast(8).reversed().forEachIndexed { i, e ->
                    val prev = data.cssHistory.reversed().getOrNull(i + 1)
                    val delta = prev?.let { e.cssPer100 - it.cssPer100 }
                    Row(Modifier.fillMaxWidth()) {
                        Text(Format.date(e.epochDay), Modifier.weight(1f))
                        Text("${Pace.format(e.cssPer100)}/100", fontWeight = FontWeight.SemiBold)
                        delta?.let {
                            Text(
                                "  ${if (it <= 0) "▼" else "▲"} ${"%.1f".format(kotlin.math.abs(it))}s",
                                color = if (it <= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            SectionHeader("Log")
            logs.sortedByDescending { it.epochDay }.forEach { log ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(Format.date(log.epochDay), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(log.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${Format.distance(log.distance, unit)} · ${log.minutes} min · ${log.calories} kcal",
                                style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag(log.type.label)
                                Tag("RPE ${log.rpe}")
                            }
                            if (log.notes.isNotBlank()) Text(log.notes, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { toDelete = log }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                }
            }
        }
    }
    toDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text(log.title) },
            confirmButton = { TextButton(onClick = { vm.deleteLog(log); toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

/** Consecutive weeks (ending this week or last) with at least one logged session. */
private fun weekStreak(logs: List<WorkoutLog>, monday: Long): Int {
    val weeksWithSwims = logs.map { Math.floorDiv(monday - it.epochDay + 6, 7L) }.toSet()
    var start = if (0L in weeksWithSwims) 0L else 1L
    var streak = 0
    while (start in weeksWithSwims) {
        streak++; start++
    }
    return streak
}

@Composable
private fun WeeklyBars(values: List<Int>) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val bar = MaterialTheme.colorScheme.primary
    val current = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val gap = size.width / values.size * 0.25f
        val w = size.width / values.size - gap
        values.forEachIndexed { i, v ->
            val x = i * (w + gap) + gap / 2
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(8f, 8f))
            val h = size.height * v / max
            drawRoundRect(
                if (i == values.lastIndex) current else bar,
                Offset(x, size.height - h), Size(w, h), CornerRadius(8f, 8f),
            )
        }
    }
}
