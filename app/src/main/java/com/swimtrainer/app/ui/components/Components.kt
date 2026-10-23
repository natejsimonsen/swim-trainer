@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.swimtrainer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.ui.Format
import com.swimtrainer.app.ui.theme.zoneColor
import com.swimtrainer.core.content.Drill
import com.swimtrainer.core.content.Drills
import com.swimtrainer.core.model.SetItem
import com.swimtrainer.core.model.Workout
import com.swimtrainer.core.model.WorkoutBlock
import com.swimtrainer.core.model.Zone

@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    hasBottomNav: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        contentWindowInsets = if (hasBottomNav) WindowInsets(0, 0, 0, 0) else WindowInsets.navigationBars,
        content = content,
    )
}

/** Vertically scrolling page body with standard padding. */
@Composable
fun Page(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp),
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun StatRow(vararg stats: Pair<String, String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        stats.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f)) }
    }
}

@Composable
fun Tag(text: String, color: Color = MaterialTheme.colorScheme.secondaryContainer, textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@Composable
fun ZoneTag(zone: Zone) {
    Tag("${zone.short} ${zone.label}", zoneColor(zone).copy(alpha = 0.2f), MaterialTheme.colorScheme.onSurface)
}

@Composable
fun InfoCard(text: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun BulletList(points: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        points.forEach { p ->
            Row {
                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                Text(p, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Full workout listing: stats, cues and every block. Drill names open a how-to sheet. */
@Composable
fun WorkoutContent(workout: Workout) {
    var drill by remember { mutableStateOf<Drill?>(null) }
    val unit = workout.pool.unit
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatRow(
            "Distance" to Format.distance(workout.totalDistance, unit),
            "Time" to "~${Format.minutes(workout.estimatedMinutes)}",
            "Energy" to "${workout.calories} kcal",
        )
        Text(workout.summary, style = MaterialTheme.typography.bodyMedium)
        if (workout.cues.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Focus cues", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.height(4.dp))
                    workout.cues.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
        workout.blocks.forEach { block -> BlockCard(block, unit) { id -> drill = Drills.byId(id) } }
    }
    drill?.let { DrillSheet(it) { drill = null } }
}

@Composable
fun BlockCard(block: WorkoutBlock, unit: String, onDrill: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(block.type.label.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(block.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(Format.distance(block.totalDistance, unit), style = MaterialTheme.typography.labelLarge)
            }
            if (block.rounds > 1) {
                Tag("${block.rounds} rounds" + if (block.roundRestSec > 0) " · ${block.roundRestSec}s between" else "")
            }
            block.items.forEachIndexed { i, item ->
                if (i > 0) HorizontalDivider()
                SetItemRow(item, onDrill)
            }
            block.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun SetItemRow(item: SetItem, onDrill: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(zoneColor(item.zone)))
            Spacer(Modifier.width(8.dp))
            Text(Format.headline(item), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Format.interval(item)?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ZoneTag(item.zone)
            if (item.pattern != com.swimtrainer.core.model.EffortPattern.STEADY) Tag(item.pattern.label)
            Format.target(item)?.let { Tag(it, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) }
            item.equipment.forEach { Tag(it.label, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item.drillId?.let { id ->
            Drills.byId(id)?.let { d ->
                Text(
                    "How to: ${d.name} ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onDrill(id) }.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun DrillSheet(drill: Drill, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            DrillDetailBody(drill)
        }
    }
}

@Composable
fun DrillDetailBody(drill: Drill) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(drill.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Tag(drill.stroke.label)
            Tag("From ${drill.minLevel.label}")
            drill.focus.forEach { Tag(it.label, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) }
            drill.equipment.forEach { Tag(it.label, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant) }
            if (drill.finsHelp) Tag("Fins help", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(drill.purpose, style = MaterialTheme.typography.bodyLarge)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How to do it", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                drill.steps.forEachIndexed { i, s ->
                    Row {
                        Text("${i + 1}. ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(s, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Text("Cues", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        BulletList(drill.cues)
        Text("Common mistakes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        BulletList(drill.mistakes)
    }
}

@Composable
fun EmptyState(title: String, body: String, action: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        action()
    }
}
