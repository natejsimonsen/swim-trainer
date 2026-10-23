@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.ui.Format
import com.swimtrainer.app.ui.components.InfoCard
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.SectionHeader
import com.swimtrainer.core.engine.Calories
import com.swimtrainer.core.engine.Pace
import com.swimtrainer.core.model.Equipment
import com.swimtrainer.core.model.Goal
import com.swimtrainer.core.model.Level
import com.swimtrainer.core.model.PoolLength
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.Weekday
import kotlin.math.roundToInt

/**
 * Collects everything the generators need. Used both for first-run onboarding and as the
 * profile editor later.
 */
@Composable
fun ProfileScreen(
    initial: SwimmerProfile?,
    onboarding: Boolean,
    hasPlan: Boolean,
    onBack: (() -> Unit)?,
    onSave: (profile: SwimmerProfile, rebuildPlan: Boolean, weeks: Int) -> Unit,
) {
    val start = initial ?: SwimmerProfile()
    var name by rememberSaveable { mutableStateOf(start.name) }
    var ageText by rememberSaveable { mutableStateOf(start.age.toString()) }
    var imperial by rememberSaveable { mutableStateOf(start.useImperial) }
    var weightText by rememberSaveable {
        mutableStateOf((if (start.useImperial) Format.kgToLb(start.weightKg) else start.weightKg).roundToInt().toString())
    }
    var heightText by rememberSaveable {
        mutableStateOf(start.heightCm?.let { (if (start.useImperial) Format.cmToIn(it) else it).roundToInt().toString() } ?: "")
    }
    var level by rememberSaveable { mutableStateOf(start.level) }
    var goal by rememberSaveable { mutableStateOf(start.goal) }
    var days by remember { mutableStateOf(start.trainingDays) }
    var minutes by rememberSaveable { mutableFloatStateOf(start.sessionMinutes.toFloat()) }
    var pool by rememberSaveable { mutableStateOf(start.pool) }
    var equipment by remember { mutableStateOf(start.equipment) }
    var strokes by remember { mutableStateOf(start.focusStrokes) }
    var paceText by rememberSaveable { mutableStateOf(start.cssPer100?.let { Pace.format(it) } ?: "") }
    var weeks by rememberSaveable { mutableIntStateOf(8) }

    val age = ageText.toIntOrNull()
    val weightInput = weightText.toDoubleOrNull()
    val weightKg = weightInput?.let { if (imperial) Format.lbToKg(it) else it }
    val heightCm = heightText.toDoubleOrNull()?.let { if (imperial) Format.inToCm(it) else it }
    val css = Pace.parse(paceText)
    val valid = age != null && age in 5..100 && weightKg != null && weightKg in 20.0..250.0 && days.isNotEmpty() &&
        (paceText.isBlank() || css != null)

    fun build() = SwimmerProfile(
        name = name.trim(),
        age = age ?: 30,
        weightKg = weightKg ?: 70.0,
        heightCm = heightCm?.takeIf { it in 100.0..250.0 },
        useImperial = imperial,
        level = level,
        goal = goal,
        trainingDays = days,
        sessionMinutes = minutes.roundToInt(),
        pool = pool,
        equipment = equipment,
        focusStrokes = strokes.ifEmpty { setOf(Stroke.FREESTYLE) },
        cssPer100 = css?.takeIf { it in 50.0..300.0 },
    )

    ScreenScaffold(title = if (onboarding) "Set up your training" else "Profile", onBack = onBack) { padding ->
        Page(padding) {
            if (onboarding) {
                Text("Tell us about you", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Your age, weight, level and schedule shape everything: paces, rest, volume, drills and how plans progress.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionHeader("About you")
            OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false to "Metric (kg, cm)", true to "Imperial (lb, in)").forEachIndexed { i, (value, label) ->
                    SegmentedButton(
                        selected = imperial == value,
                        onClick = {
                            if (imperial != value) {
                                weightText.toDoubleOrNull()?.let {
                                    weightText = (if (value) Format.kgToLb(it) else Format.lbToKg(it)).roundToInt().toString()
                                }
                                heightText.toDoubleOrNull()?.let {
                                    heightText = (if (value) Format.cmToIn(it) else Format.inToCm(it)).roundToInt().toString()
                                }
                                imperial = value
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(label) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    ageText, { ageText = it.filter(Char::isDigit).take(3) }, label = { Text("Age") },
                    isError = age == null || age !in 5..100,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    weightText, { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text(if (imperial) "Weight (lb)" else "Weight (kg)") },
                    isError = weightKg == null || weightKg !in 20.0..250.0,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    heightText, { heightText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                    label = { Text(if (imperial) "Height (in)" else "Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionHeader("Swimming level")
            Level.entries.forEach { l ->
                ChoiceCard(l.label, l.description, selected = level == l) { level = l }
            }

            SectionHeader("Main goal")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Goal.entries.forEach { g -> FilterChip(goal == g, { goal = g }, label = { Text(g.label) }) }
            }
            Text(goal.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            SectionHeader("Days you swim (${days.size}/week)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Weekday.entries.forEach { d ->
                    FilterChip(d in days, { days = if (d in days) days - d else days + d }, label = { Text(d.short) })
                }
            }
            if (days.size >= 6 && level <= Level.NOVICE) {
                InfoCard("6–7 days is a lot for a newer swimmer. The plan will include easy recovery swims — listen to your shoulders.")
            }

            SectionHeader("Session length: ${minutes.roundToInt()} min")
            Slider(minutes, { minutes = (it / 5).roundToInt() * 5f }, valueRange = 20f..120f, steps = 19)

            SectionHeader("Pool")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PoolLength.entries.forEachIndexed { i, p ->
                    SegmentedButton(pool == p, { pool = p }, SegmentedButtonDefaults.itemShape(i, PoolLength.entries.size)) { Text(p.label) }
                }
            }

            SectionHeader("Equipment you have")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Equipment.entries.forEach { e ->
                    FilterChip(e in equipment, { equipment = if (e in equipment) equipment - e else equipment + e }, label = { Text(e.label) })
                }
            }

            SectionHeader("Strokes to work on")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Stroke.FREESTYLE, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY).forEach { s ->
                    FilterChip(s in strokes, { strokes = if (s in strokes) strokes - s else strokes + s }, label = { Text(s.label) })
                }
            }

            SectionHeader("Pace (optional)")
            OutlinedTextField(
                paceText, { paceText = it.filter { c -> c.isDigit() || c == ':' || c == '.' }.take(7) },
                label = { Text("CSS / threshold pace per 100 ${pool.unit} (e.g. 1:45)") },
                isError = paceText.isNotBlank() && css == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val preview = build()
            Text(
                if (css == null) "Leave blank and we'll estimate ${Pace.format(Pace.css(preview))}/100 ${pool.unit} from your level and age. " +
                    "Your plan's first week includes a CSS test to dial it in."
                else "Great — all training paces will be based on ${Pace.format(css)}/100 ${pool.unit}.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (valid) {
                val fuel = Calories.fueling(preview)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Your fueling targets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Protein ${fuel.proteinGramsLow}–${fuel.proteinGramsHigh} g/day · fluids ~${fuel.fluidMlPerHour} ml per hour of swimming",
                            style = MaterialTheme.typography.bodyMedium)
                        preview.bmi?.let { Text("BMI ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            if (onboarding || !hasPlan) {
                SectionHeader("Plan length: $weeks weeks")
                Slider(weeks.toFloat(), { weeks = it.roundToInt() }, valueRange = 4f..16f, steps = 11)
            }

            Spacer(Modifier.padding(4.dp))
            if (onboarding) {
                Button(onClick = { onSave(build(), true, weeks) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
                    Text("Build my plan")
                }
            } else {
                Button(onClick = { onSave(build(), true, weeks) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
                    Text(if (hasPlan) "Save & rebuild plan" else "Save & build plan")
                }
                OutlinedButton(onClick = { onSave(build(), false, weeks) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
                    Text("Save only")
                }
            }
            Spacer(Modifier.padding(8.dp))
        }
    }
}

@Composable
fun ChoiceCard(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}
