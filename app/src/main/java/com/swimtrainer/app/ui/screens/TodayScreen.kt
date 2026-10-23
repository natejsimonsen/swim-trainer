@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.data.AppData
import com.swimtrainer.app.ui.AppViewModel
import com.swimtrainer.app.ui.Format
import com.swimtrainer.app.ui.PlanRef
import com.swimtrainer.app.ui.components.EmptyState
import com.swimtrainer.app.ui.components.InfoCard
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.SectionHeader
import com.swimtrainer.app.ui.components.StatRow
import com.swimtrainer.app.ui.components.Tag
import com.swimtrainer.core.engine.Pace
import com.swimtrainer.core.model.SwimmerProfile
import java.time.LocalTime

@Composable
fun TodayScreen(
    data: AppData,
    profile: SwimmerProfile,
    vm: AppViewModel,
    onOpenWorkout: () -> Unit,
    onQuick: () -> Unit,
    onPlan: () -> Unit,
    onTools: () -> Unit,
    onProfile: () -> Unit,
) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val name = profile.name.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
    ScreenScaffold(
        title = "$greeting$name",
        hasBottomNav = true,
        actions = {
            IconButton(onClick = onTools) { Icon(Icons.Filled.Timer, "Tools") }
            IconButton(onClick = onProfile) { Icon(Icons.Filled.Person, "Profile") }
        },
    ) { padding ->
        Page(padding) {
            val plan = data.plan
            if (plan == null) {
                EmptyState("No plan yet", "Build a personalised plan from your profile.") {
                    Button(onClick = onPlan) { Text("Create plan") }
                }
            } else {
                val next = vm.nextSession(data)
                if (next == null) {
                    InfoCard("You've completed your plan. Build a new one to keep progressing — your paces carry over.")
                    Button(onClick = onPlan) { Text("New plan") }
                } else {
                    val date = vm.dateOf(plan, next.week.number, next.day.day)
                    val w = next.day.workout
                    Card(
                        onClick = { vm.openPlanWorkout(plan, next.week, next.day); onOpenWorkout() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (next.isToday) "TODAY'S SESSION" else "NEXT SESSION · ${Format.date(date).uppercase()}",
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(w.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "${Format.distance(w.totalDistance, w.pool.unit)} · ~${Format.minutes(w.estimatedMinutes)} · ${w.calories} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag("Week ${next.week.number} · ${next.week.phase.label}")
                                Tag(w.type.label)
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(onClick = { vm.openPlanWorkout(plan, next.week, next.day); onOpenWorkout() }) {
                                Icon(Icons.Filled.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text("View & start")
                            }
                        }
                    }
                }

                // This week's strip
                val week = vm.currentWeek(plan)?.let { n -> plan.weeks.firstOrNull { it.number == n } }
                if (week != null) {
                    SectionHeader("This week · ${week.phase.label}")
                    Text(week.focus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        com.swimtrainer.core.model.Weekday.entries.forEach { wd ->
                            val pd = week.days.firstOrNull { it.day == wd }
                            val done = pd != null && PlanRef(plan.id, week.number, wd).key in data.completed
                            val isToday = wd == vm.todayWeekday()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(wd.short, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                done -> MaterialTheme.colorScheme.secondary
                                                pd != null -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        done -> Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
                                        pd != null -> Text(pd.workout.type.label.take(2), style = MaterialTheme.typography.labelSmall)
                                        else -> Text("–", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SectionHeader("In the pool without a plan?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onQuick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Bolt, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Quick workout")
                }
                FilledTonalButton(onClick = onTools, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Timer, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Pace clock")
                }
            }

            // Weekly totals
            val todayEpoch = vm.today().toEpochDay()
            val mondayEpoch = todayEpoch - (vm.today().dayOfWeek.value - 1)
            val thisWeek = data.logs.filter { it.epochDay >= mondayEpoch }
            SectionHeader("Your week")
            StatRow(
                "Distance" to Format.distance(thisWeek.sumOf { it.distance }, profile.pool.unit),
                "Sessions" to "${thisWeek.size}/${profile.daysPerWeek}",
                "Calories" to "${thisWeek.sumOf { it.calories }}",
            )
            StatRow(
                "CSS pace" to "${Pace.format(Pace.css(profile))}/100",
                "Level" to profile.level.label,
            )
            if (profile.cssPer100 == null) {
                InfoCard("Your paces are estimated from your level and age. Do the CSS test (Tools) for exact, personalised paces.")
            }
        }
    }
}
