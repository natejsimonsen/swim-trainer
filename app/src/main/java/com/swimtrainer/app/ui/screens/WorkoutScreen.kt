@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.ui.ActiveWorkout
import com.swimtrainer.app.ui.AppViewModel
import com.swimtrainer.app.ui.components.EmptyState
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.WorkoutContent
import kotlin.math.roundToInt

@Composable
fun WorkoutScreen(
    active: ActiveWorkout?,
    completed: Boolean,
    vm: AppViewModel,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    var logOpen by remember { mutableStateOf(false) }
    ScreenScaffold(
        title = active?.workout?.title ?: "Workout",
        onBack = onBack,
        actions = {
            val ref = active?.planRef
            if (ref != null) {
                IconButton(onClick = { vm.regeneratePlanDay(ref) }) { Icon(Icons.Filled.Refresh, "Swap for a new session") }
            }
        },
        bottomBar = {
            if (active != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { logOpen = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Check, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (completed) "Log again" else "Log as done")
                        }
                        Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Start")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Page(padding) {
            if (active == null) {
                EmptyState("No workout selected", "Pick a session from your plan or generate a quick workout.")
            } else {
                if (completed) {
                    Text("✓ Completed", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleSmall)
                }
                WorkoutContent(active.workout)
            }
        }
    }
    if (logOpen && active != null) {
        val w = active.workout
        LogDialog(
            defaultDistance = w.totalDistance,
            defaultMinutes = w.estimatedMinutes,
            unit = w.pool.unit,
            onDismiss = { logOpen = false },
            onSave = { distance, minutes, rpe, notes ->
                val kcal = if (w.totalDistance > 0) (w.calories * distance / w.totalDistance.toDouble()).roundToInt() else w.calories
                vm.logWorkout(w.title, w.type, distance, minutes, kcal, rpe, notes, active.planRef)
                logOpen = false
            },
        )
    }
}

@Composable
fun LogDialog(
    defaultDistance: Int,
    defaultMinutes: Int,
    unit: String,
    onDismiss: () -> Unit,
    onSave: (distance: Int, minutes: Int, rpe: Int, notes: String) -> Unit,
) {
    var distance by remember { mutableStateOf(defaultDistance.toString()) }
    var minutes by remember { mutableStateOf(defaultMinutes.toString()) }
    var rpe by remember { mutableFloatStateOf(6f) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(distance, { distance = it.filter(Char::isDigit).take(6) }, label = { Text("Distance ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Effort (RPE): ${rpe.roundToInt()}/10 — ${rpeLabel(rpe.roundToInt())}")
                Slider(rpe, { rpe = it }, valueRange = 1f..10f, steps = 8)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (how did it feel?)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(distance.toIntOrNull() ?: defaultDistance, minutes.toIntOrNull() ?: defaultMinutes, rpe.roundToInt(), notes.trim())
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun rpeLabel(rpe: Int) = when (rpe) {
    in 1..2 -> "very easy"
    in 3..4 -> "easy"
    in 5..6 -> "moderate"
    in 7..8 -> "hard"
    9 -> "very hard"
    else -> "max effort"
}
