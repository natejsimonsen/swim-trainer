@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swimtrainer.app.ui.ActiveWorkout
import com.swimtrainer.app.ui.AppViewModel
import com.swimtrainer.app.ui.Format
import com.swimtrainer.app.ui.components.DrillSheet
import com.swimtrainer.app.ui.components.EmptyState
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.Tag
import com.swimtrainer.app.ui.components.ZoneTag
import com.swimtrainer.app.ui.theme.zoneColor
import com.swimtrainer.core.content.Drill
import com.swimtrainer.core.content.Drills
import com.swimtrainer.core.engine.Pace
import com.swimtrainer.core.engine.WorkoutSteps
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Beeps and buzzes for send-offs, so you can follow the set without looking at the phone. */
class Signals(context: Context) {
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 90) }.getOrNull()
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun tick() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    fun go() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        tone?.release()
    }
}

@Composable
fun PlayerScreen(active: ActiveWorkout?, vm: AppViewModel, onBack: () -> Unit, onFinished: () -> Unit) {
    if (active == null) {
        ScreenScaffold("Workout", onBack = onBack) { EmptyState("Nothing to play", "Open a workout first.") }
        return
    }
    val workout = active.workout
    val steps = remember(workout) { WorkoutSteps.flatten(workout) }
    val context = LocalContext.current
    val signals = remember { Signals(context) }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            signals.release()
        }
    }

    var index by rememberSaveable { mutableIntStateOf(0) }
    var running by rememberSaveable { mutableStateOf(false) }
    var autoAdvance by rememberSaveable { mutableStateOf(true) }
    var sound by rememberSaveable { mutableStateOf(true) }
    // Elapsed milliseconds in the current rep and overall, accumulated while running.
    var stepMs by rememberSaveable { mutableLongStateOf(0L) }
    var totalMs by rememberSaveable { mutableLongStateOf(0L) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var drill by remember { mutableStateOf<Drill?>(null) }
    var showLog by remember { mutableStateOf(false) }

    val step = steps.getOrNull(index)

    LaunchedEffect(running, index) {
        if (!running) return@LaunchedEffect
        var last = SystemClock.elapsedRealtime()
        var lastBeepSecond = -1
        while (running) {
            delay(100)
            val now = SystemClock.elapsedRealtime()
            val dt = now - last
            last = now
            stepMs += dt
            totalMs += dt
            val cycle = step?.cycleSec ?: continue
            val remaining = cycle - (stepMs / 1000).toInt()
            if (sound && remaining in 1..3 && remaining != lastBeepSecond) {
                lastBeepSecond = remaining
                signals.tick()
            }
            if (remaining <= 0 && autoAdvance) {
                if (sound) signals.go()
                if (index < steps.lastIndex) {
                    stepMs -= cycle * 1000L
                    index++
                } else {
                    running = false
                    finished = true
                }
                break
            }
        }
    }

    fun next() {
        if (index < steps.lastIndex) {
            index++; stepMs = 0
        } else {
            running = false; finished = true
        }
    }

    fun prev() {
        if (index > 0) index--
        stepMs = 0
    }

    ScreenScaffold(
        title = workout.title,
        onBack = onBack,
        actions = { TextButton(onClick = { running = false; showLog = true }) { Text("Finish") } },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val distanceDone = (step?.distanceBefore ?: workout.totalDistance)
            LinearProgressIndicator(
                progress = { if (workout.totalDistance == 0) 0f else distanceDone / workout.totalDistance.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                Text("${Format.distance(distanceDone, workout.pool.unit)} / ${Format.distance(workout.totalDistance, workout.pool.unit)}",
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("Total ${Pace.format((totalMs / 1000).toInt())}", style = MaterialTheme.typography.labelLarge)
            }

            if (finished || step == null) {
                EmptyState("Workout complete! 🎉", "Great job. Log it to track your progress.") {
                    androidx.compose.material3.Button(onClick = { showLog = true }) { Text("Log workout") }
                }
            } else {
                val item = step.item
                Text(step.block.type.label.uppercase(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(step.block.title, style = MaterialTheme.typography.titleMedium)

                PaceClock(
                    stepSeconds = stepMs / 1000.0,
                    cycleSec = step.cycleSec,
                    targetSec = item.targetSec,
                    color = zoneColor(item.zone),
                    modifier = Modifier.fillMaxWidth(0.85f).align(Alignment.CenterHorizontally),
                )

                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${item.distance} ${Format.headline(item.copy(reps = 1)).substringAfter(' ')}",
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            buildString {
                                append("Rep ${step.rep} of ${item.reps}")
                                if (step.block.rounds > 1) append(" · Round ${step.round}/${step.block.rounds}")
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ZoneTag(item.zone)
                            Format.target(item)?.let { Tag(it) }
                            Format.interval(item)?.let { Tag(it) }
                        }
                        Text(item.zone.description, style = MaterialTheme.typography.bodySmall)
                        item.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        if (item.pattern != com.swimtrainer.core.model.EffortPattern.STEADY) {
                            Text("${item.pattern.label}: ${item.pattern.hint}", style = MaterialTheme.typography.bodySmall)
                        }
                        item.drillId?.let { Drills.byId(it) }?.let { d ->
                            TextButton(onClick = { drill = d }) { Text("How to: ${d.name}") }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { prev() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(36.dp)) }
                    FilledIconButton(onClick = { running = !running }, modifier = Modifier.size(80.dp)) {
                        Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (running) "Pause" else "Start", Modifier.size(48.dp))
                    }
                    IconButton(onClick = { next() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Filled.SkipNext, "Next rep", Modifier.size(36.dp)) }
                }
                if (step.cycleSec == null) {
                    Text("No interval on this rep — tap Next when you're done.", style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-advance on interval", Modifier.weight(1f))
                    Switch(autoAdvance, { autoAdvance = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Beeps & vibration", Modifier.weight(1f))
                    Switch(sound, { sound = it })
                }

                steps.getOrNull(index + 1)?.let { n ->
                    Text("Up next", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Format.headline(n.item.copy(reps = 1))} · ${n.item.zone.label}" + (Format.interval(n.item)?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    drill?.let { DrillSheet(it) { drill = null } }
    if (showLog) {
        val doneDistance = if (finished) workout.totalDistance else (step?.distanceBefore ?: workout.totalDistance)
        LogDialog(
            defaultDistance = doneDistance,
            defaultMinutes = ((totalMs / 60000).toInt()).coerceAtLeast(1),
            unit = workout.pool.unit,
            onDismiss = { showLog = false },
            onSave = { distance, minutes, rpe, notes ->
                val kcal = if (workout.totalDistance > 0) (workout.calories * distance / workout.totalDistance.toDouble()).roundToInt() else 0
                vm.logWorkout(workout.title, workout.type, distance, minutes, kcal, rpe, notes, active.planRef)
                showLog = false
                onFinished()
            },
        )
    }
}

/**
 * A swimming pace clock: the sweep hand shows seconds into the current rep, the arc shows the
 * target swim time and the marker shows when the next rep leaves.
 */
@Composable
fun PaceClock(stepSeconds: Double, cycleSec: Int?, targetSec: Int?, color: Color, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            val stroke = r * 0.08f
            drawCircle(track, radius = r - stroke / 2, style = Stroke(stroke))
            // Target swim time arc (within one minute of the clock face).
            targetSec?.let { t ->
                val sweep = (t.coerceAtMost(60) / 60f) * 360f
                drawArc(color.copy(alpha = 0.45f), -90f, sweep, false,
                    topLeft = Offset(c.x - r + stroke / 2, c.y - r + stroke / 2),
                    size = androidx.compose.ui.geometry.Size(2 * r - stroke, 2 * r - stroke), style = Stroke(stroke))
            }
            // Ticks every 5 seconds.
            for (i in 0 until 60 step 5) {
                val a = (i / 60f) * 2 * PI - PI / 2
                val outer = r - stroke * 1.4f
                val inner = outer - if (i % 15 == 0) r * 0.12f else r * 0.06f
                drawLine(onSurface.copy(alpha = 0.6f),
                    Offset(c.x + (cos(a) * inner).toFloat(), c.y + (sin(a) * inner).toFloat()),
                    Offset(c.x + (cos(a) * outer).toFloat(), c.y + (sin(a) * outer).toFloat()),
                    strokeWidth = if (i % 15 == 0) 6f else 3f, cap = StrokeCap.Round)
            }
            // Send-off marker.
            cycleSec?.let { cy ->
                val a = ((cy % 60) / 60f) * 2 * PI - PI / 2
                val pr = r - stroke / 2
                drawCircle(primary, radius = stroke * 0.9f, center = Offset(c.x + (cos(a) * pr).toFloat(), c.y + (sin(a) * pr).toFloat()))
            }
            // Sweep hand.
            val a = ((stepSeconds % 60) / 60.0) * 2 * PI - PI / 2
            val len = r * 0.78f
            drawLine(color, c, Offset(c.x + (cos(a) * len).toFloat(), c.y + (sin(a) * len).toFloat()), strokeWidth = 10f, cap = StrokeCap.Round)
            drawCircle(color, radius = 14f, center = c)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val main = if (cycleSec != null) (cycleSec - stepSeconds.toInt()).coerceAtLeast(0) else stepSeconds.toInt()
            Text(Pace.format(main), fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(if (cycleSec != null) "until next send-off" else "elapsed", style = MaterialTheme.typography.labelMedium)
        }
    }
}
