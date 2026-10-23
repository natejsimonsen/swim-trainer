@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.ui.AppViewModel
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.SectionHeader
import com.swimtrainer.app.ui.components.WorkoutContent
import com.swimtrainer.core.engine.Energy
import com.swimtrainer.core.model.PoolLength
import com.swimtrainer.core.model.SessionType
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.Workout
import kotlin.math.roundToInt

private val quickTypes = listOf(
    SessionType.ENDURANCE, SessionType.THRESHOLD, SessionType.SPEED, SessionType.TECHNIQUE,
    SessionType.IM, SessionType.RACE_PACE, SessionType.OPEN_WATER, SessionType.RECOVERY,
)

@Composable
fun QuickSetScreen(
    profile: SwimmerProfile,
    quick: Workout?,
    vm: AppViewModel,
    onStart: () -> Unit,
) {
    var singleSet by rememberSaveable { mutableStateOf(false) }
    var minutes by rememberSaveable { mutableFloatStateOf(profile.sessionMinutes.toFloat().coerceIn(15f, 120f)) }
    var type by rememberSaveable { mutableStateOf<SessionType?>(null) }
    var stroke by rememberSaveable { mutableStateOf(Stroke.FREESTYLE) }
    var energy by rememberSaveable { mutableStateOf(Energy.NORMAL) }
    var pool by rememberSaveable { mutableStateOf(profile.pool) }

    fun generate() = vm.generateQuick(type, minutes.roundToInt(), stroke, energy, singleSet, pool)

    ScreenScaffold(title = "Quick set", hasBottomNav = true) { padding ->
        Page(padding) {
            Text(
                "Generate a session on the fly, tailored to your level, age and paces.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!singleSet, {
                    if (singleSet) minutes = profile.sessionMinutes.toFloat().coerceIn(15f, 120f)
                    singleSet = false
                }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Full workout") }
                SegmentedButton(singleSet, {
                    if (!singleSet) minutes = 15f
                    singleSet = true
                }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Just one set") }
            }

            SectionHeader(if (singleSet) "Set length: ${minutes.roundToInt()} min" else "Time available: ${minutes.roundToInt()} min")
            Slider(
                minutes, { minutes = (it / 5).roundToInt() * 5f },
                valueRange = if (singleSet) 5f..45f else 15f..120f,
                steps = if (singleSet) 7 else 20,
            )

            SectionHeader("Focus")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == null, { type = null }, label = { Text("Surprise me") })
                quickTypes.forEach { t -> FilterChip(type == t, { type = t }, label = { Text(t.label) }) }
            }
            type?.let { Text(it.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            SectionHeader("Stroke")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Stroke.FREESTYLE, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY, Stroke.IM).forEach { s ->
                    FilterChip(stroke == s, { stroke = s }, label = { Text(s.label) })
                }
            }

            if (!singleSet) {
                SectionHeader("How do you feel?")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Energy.entries.forEachIndexed { i, e ->
                        SegmentedButton(energy == e, { energy = e }, SegmentedButtonDefaults.itemShape(i, Energy.entries.size)) { Text(e.label) }
                    }
                }
            }

            SectionHeader("Pool today")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PoolLength.entries.forEachIndexed { i, p ->
                    SegmentedButton(pool == p, { pool = p }, SegmentedButtonDefaults.itemShape(i, PoolLength.entries.size)) { Text(p.label) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { generate() }, modifier = Modifier.weight(1f)) {
                    Icon(if (quick == null) Icons.Filled.Bolt else Icons.Filled.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (quick == null) "Generate" else "New one")
                }
                if (quick != null) {
                    OutlinedButton(onClick = { vm.openWorkout(quick); onStart() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Start")
                    }
                }
            }

            quick?.let {
                SectionHeader(it.title)
                WorkoutContent(it)
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
