@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.data.AppData
import com.swimtrainer.app.ui.AppViewModel
import com.swimtrainer.app.ui.Format
import com.swimtrainer.app.ui.PlanRef
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.SectionHeader
import com.swimtrainer.app.ui.components.Tag
import com.swimtrainer.core.model.Phase
import com.swimtrainer.core.model.SwimmerProfile
import kotlin.math.roundToInt

@Composable
fun PlanScreen(
    data: AppData,
    profile: SwimmerProfile,
    vm: AppViewModel,
    busy: Boolean,
    onOpenWorkout: () -> Unit,
) {
    var confirmRebuild by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var weeks by rememberSaveable { mutableIntStateOf(data.plan?.weeks?.size ?: 8) }

    ScreenScaffold(
        title = "Training plan",
        hasBottomNav = true,
        actions = {
            if (data.plan != null) {
                IconButton(onClick = { confirmRebuild = true }) { Icon(Icons.Filled.Refresh, "Rebuild plan") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete plan") }
            }
        },
    ) { padding ->
        Page(padding) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            val plan = data.plan
            if (plan == null) {
                Text("Build your plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "A periodised plan for ${profile.daysPerWeek} days/week (${profile.trainingDays.sortedBy { it.ordinal }.joinToString { it.short }}), " +
                        "~${profile.sessionMinutes} min sessions, goal: ${profile.goal.label.lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                SectionHeader("Length: $weeks weeks")
                Slider(weeks.toFloat(), { weeks = it.roundToInt() }, valueRange = 4f..16f, steps = 11)
                Button(onClick = { vm.generatePlan(profile, weeks) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Generate plan")
                }
                return@Page
            }

            val total = plan.weeks.sumOf { it.days.size }
            val done = plan.weeks.sumOf { w -> w.days.count { PlanRef(plan.id, w.number, it.day).key in data.completed } }
            Text(plan.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(plan.overview, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done / total.toFloat() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text("$done/$total done", style = MaterialTheme.typography.labelMedium)
            }
            PhaseLegend()

            val current = vm.currentWeek(plan)
            plan.weeks.forEach { week ->
                var expanded by rememberSaveable(plan.id, week.number) { mutableStateOf(week.number == current) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (week.number == current) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Week ${week.number}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Tag(week.phase.label)
                                if (week.number == current) Tag("Now", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                            }
                            Text(
                                "${Format.distance(week.totalDistance, profile.pool.unit)} · ${week.focus}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                    }
                    AnimatedVisibility(expanded) {
                        Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 8.dp)) {
                            week.days.forEachIndexed { i, day ->
                                if (i > 0) HorizontalDivider()
                                val ref = PlanRef(plan.id, week.number, day.day)
                                val isDone = ref.key in data.completed
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.openPlanWorkout(plan, week, day); onOpenWorkout() }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = { vm.toggleCompleted(ref) }) {
                                        Icon(
                                            if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                            if (isDone) "Mark not done" else "Mark done",
                                            tint = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${day.day.label} · ${Format.date(vm.dateOf(plan, week.number, day.day))}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(day.workout.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${Format.distance(day.workout.totalDistance, day.workout.pool.unit)} · ~${day.workout.estimatedMinutes} min",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.padding(8.dp))
        }
    }

    if (confirmRebuild) {
        AlertDialog(
            onDismissRequest = { confirmRebuild = false },
            title = { Text("Rebuild plan?") },
            text = {
                Column {
                    Text("Generates a fresh plan from your current profile and paces. Completed ticks reset; your workout log is kept.")
                    Spacer(Modifier.padding(4.dp))
                    Text("Length: $weeks weeks")
                    Slider(weeks.toFloat(), { weeks = it.roundToInt() }, valueRange = 4f..16f, steps = 11)
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmRebuild = false; vm.generatePlan(profile, weeks) }) { Text("Rebuild") }
            },
            dismissButton = { TextButton(onClick = { confirmRebuild = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete plan?") },
            text = { Text("Your workout log and profile are kept.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deletePlan() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PhaseLegend() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Phase.entries.forEach {
                Text("${it.label}: ${it.description}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
