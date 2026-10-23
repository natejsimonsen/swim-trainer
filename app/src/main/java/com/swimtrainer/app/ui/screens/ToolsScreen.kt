@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.ui.AppViewModel
import com.swimtrainer.app.ui.components.InfoCard
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.SectionHeader
import com.swimtrainer.app.ui.components.BulletList
import com.swimtrainer.app.ui.theme.zoneColor
import com.swimtrainer.core.engine.Calories
import com.swimtrainer.core.engine.Pace
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import kotlinx.coroutines.delay

@Composable
fun ToolsScreen(profile: SwimmerProfile, hasPlan: Boolean, vm: AppViewModel, onBack: () -> Unit) {
    ScreenScaffold(title = "Tools", onBack = onBack) { padding ->
        Page(padding) {
            CssTestCard(profile, hasPlan, vm)
            ZoneTable(profile)
            SendOffCalculator(profile)
            RacePredictor(profile)
            FuelingCard(profile)
            StandalonePaceClock()
        }
    }
}

@Composable
private fun ToolCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun TimeField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value, { onChange(it.filter { c -> c.isDigit() || c == ':' || c == '.' }.take(8)) },
        label = { Text(label) }, singleLine = true,
        isError = value.isNotBlank() && Pace.parse(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = modifier,
    )
}

@Composable
private fun CssTestCard(profile: SwimmerProfile, hasPlan: Boolean, vm: AppViewModel) {
    var t400 by rememberSaveable { mutableStateOf("") }
    var t200 by rememberSaveable { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    val unit = profile.pool.unit
    ToolCard("CSS test calculator") {
        Text(
            "Critical Swim Speed is the pace you can hold for ~30 min — the anchor for every training zone. " +
                "After a good warm-up swim a 400 all-out (even pace), rest 5–10 min, then a 200 all-out.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField(t400, { t400 = it; saved = false }, "400 time (m:ss)", Modifier.weight(1f))
            TimeField(t200, { t200 = it; saved = false }, "200 time (m:ss)", Modifier.weight(1f))
        }
        val a = Pace.parse(t400)
        val b = Pace.parse(t200)
        val css = if (a != null && b != null) Pace.cssFromTest(a, b) else null
        when {
            css != null -> {
                Text("Your CSS: ${Pace.format(css)} per 100 $unit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Current: ${Pace.format(Pace.css(profile))}${if (profile.cssPer100 == null) " (estimated)" else ""}",
                    style = MaterialTheme.typography.bodySmall)
                if (saved) {
                    Text("Saved ✓ — all paces updated.", color = MaterialTheme.colorScheme.secondary)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.updateCss(css, rebuildPlan = hasPlan); saved = true }, modifier = Modifier.weight(1f)) {
                            Text(if (hasPlan) "Save & update plan" else "Save")
                        }
                        if (hasPlan) {
                            OutlinedButton(onClick = { vm.updateCss(css, rebuildPlan = false); saved = true }, modifier = Modifier.weight(1f)) {
                                Text("Save only")
                            }
                        }
                    }
                }
            }
            a != null && b != null -> Text("Check your times: the 400 must be slower than the 200 and give a realistic pace.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ZoneTable(profile: SwimmerProfile) {
    val unit = profile.pool.unit
    ToolCard("Your training zones (per 100 $unit freestyle)") {
        Pace.zoneTable(profile).forEachIndexed { i, z ->
            if (i > 0) HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(zoneColor(z.zone)))
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("${z.zone.short} ${z.zone.label} · RPE ${z.zone.rpe}", style = MaterialTheme.typography.titleSmall)
                    Text(z.zone.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Pace.format(z.per100Sec), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("@ ${Pace.format(z.sendOff100)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SendOffCalculator(profile: SwimmerProfile) {
    var pace by rememberSaveable { mutableStateOf(Pace.format(Pace.css(profile))) }
    var distance by rememberSaveable { mutableStateOf("100") }
    var rest by rememberSaveable { mutableStateOf("10") }
    ToolCard("Interval calculator") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField(pace, { pace = it }, "Pace /100", Modifier.weight(1f))
            OutlinedTextField(distance, { distance = it.filter(Char::isDigit).take(4) }, label = { Text("Distance") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(rest, { rest = it.filter(Char::isDigit).take(3) }, label = { Text("Rest (s)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        val p = Pace.parse(pace)
        val d = distance.toIntOrNull()
        val r = rest.toIntOrNull() ?: 0
        if (p != null && d != null && d > 0) {
            val swim = (p * d / 100).toInt()
            Text("Swim ${Pace.format(swim)} → leave every ${Pace.format(Pace.sendOff(swim, r))}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RacePredictor(profile: SwimmerProfile) {
    ToolCard("Race predictor") {
        Text("Estimated times from your CSS (push start, well tapered).", style = MaterialTheme.typography.bodySmall)
        listOf(50, 100, 200, 400, 800, 1500).forEach { d ->
            Row {
                Text("$d ${profile.pool.unit} free", Modifier.weight(1f))
                Text(Pace.format(Pace.predictRace(profile, d)), fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider()
        listOf(Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY, Stroke.IM).forEach { s ->
            Row {
                Text("100 ${s.label.lowercase()}", Modifier.weight(1f))
                Text(Pace.format(Pace.predictRace(profile, 100, s)), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FuelingCard(profile: SwimmerProfile) {
    val fuel = Calories.fueling(profile)
    ToolCard("Fueling for ${com.swimtrainer.app.ui.Format.weight(profile.weightKg, profile.useImperial)}") {
        Text("Protein: ${fuel.proteinGramsLow}–${fuel.proteinGramsHigh} g per day", style = MaterialTheme.typography.bodyMedium)
        Text("Fluids: ~${fuel.fluidMlPerHour} ml per hour of swimming", style = MaterialTheme.typography.bodyMedium)
        Text("Carbs around a ${profile.sessionMinutes}-min session: ~${fuel.carbsGramsPerSession} g", style = MaterialTheme.typography.bodyMedium)
        BulletList(fuel.tips)
        Text("General guidance, not medical advice.", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StandalonePaceClock() {
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val view = LocalView.current
    LaunchedEffect(startedAt) {
        while (startedAt != 0L) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
    }
    DisposableEffect(startedAt) {
        view.keepScreenOn = startedAt != 0L
        onDispose { view.keepScreenOn = false }
    }
    ToolCard("Pace clock") {
        Text("A pool-style sweep clock for when there isn't one on deck. Screen stays on while running.",
            style = MaterialTheme.typography.bodySmall)
        val seconds = if (startedAt == 0L) 0.0 else (now - startedAt) / 1000.0
        PaceClock(
            stepSeconds = seconds % 60,
            cycleSec = null,
            targetSec = null,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally),
        )
        Text("Elapsed ${Pace.format(seconds.toInt())}", Modifier.align(Alignment.CenterHorizontally))
        Button(
            onClick = { startedAt = if (startedAt == 0L) SystemClock.elapsedRealtime() else 0L },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (startedAt == 0L) "Start" else "Stop") }
        InfoCard("Tip: rest-based sets for beginners show 'rest 20s' — leave when the sweep hand has moved that far past your finish.")
    }
}
