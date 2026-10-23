@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.swimtrainer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swimtrainer.app.ui.components.BulletList
import com.swimtrainer.app.ui.components.DrillDetailBody
import com.swimtrainer.app.ui.components.EmptyState
import com.swimtrainer.app.ui.components.Page
import com.swimtrainer.app.ui.components.ScreenScaffold
import com.swimtrainer.app.ui.components.SectionHeader
import com.swimtrainer.app.ui.components.Tag
import com.swimtrainer.core.content.Drills
import com.swimtrainer.core.content.Dryland
import com.swimtrainer.core.content.StrokeGuide
import com.swimtrainer.core.model.Level
import com.swimtrainer.core.model.Stroke

@Composable
fun LibraryScreen(
    level: Level,
    onDrill: (String) -> Unit,
    onGuide: (String) -> Unit,
    onDryland: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ScreenScaffold(title = "Library", hasBottomNav = true) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf("Drills", "Technique", "Dryland").forEachIndexed { i, t ->
                    Tab(tab == i, { tab = i }, text = { Text(t) })
                }
            }
            when (tab) {
                0 -> DrillList(level, onDrill)
                1 -> GuideList(onGuide)
                else -> DrylandList(onDryland)
            }
        }
    }
}

@Composable
private fun DrillList(level: Level, onDrill: (String) -> Unit) {
    var stroke by rememberSaveable { mutableStateOf<Stroke?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var myLevel by rememberSaveable { mutableStateOf(false) }
    Page(androidx.compose.foundation.layout.PaddingValues()) {
        OutlinedTextField(
            query, { query = it }, leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Search drills, e.g. catch, breathing") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(stroke == null, { stroke = null }, label = { Text("All") })
            listOf(Stroke.FREESTYLE, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY, Stroke.CHOICE).forEach { s ->
                FilterChip(stroke == s, { stroke = s }, label = { Text(if (s == Stroke.CHOICE) "General" else s.label) })
            }
            FilterChip(myLevel, { myLevel = !myLevel }, label = { Text("My level") })
        }
        val q = query.trim().lowercase()
        val drills = Drills.all.filter { d ->
            (stroke == null || d.stroke == stroke || (stroke == Stroke.CHOICE && d.stroke == Stroke.CHOICE)) &&
                (!myLevel || d.minLevel <= level) &&
                (q.isEmpty() || d.name.lowercase().contains(q) || d.purpose.lowercase().contains(q) ||
                    d.focus.any { it.label.lowercase().contains(q) })
        }
        if (drills.isEmpty()) EmptyState("No drills found", "Try another search or filter.")
        drills.forEach { d ->
            Card(onClick = { onDrill(d.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(d.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(d.purpose, style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Tag(if (d.stroke == Stroke.CHOICE) "General" else d.stroke.label)
                        Tag(d.minLevel.label, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                        d.focus.take(2).forEach { Tag(it.label, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideList(onGuide: (String) -> Unit) {
    Page(androidx.compose.foundation.layout.PaddingValues()) {
        StrokeGuide.entries.forEach { g ->
            Card(onClick = { onGuide(g.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(g.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(g.overview, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DrylandList(onDryland: (String) -> Unit) {
    Page(androidx.compose.foundation.layout.PaddingValues()) {
        Text("Short land routines that protect your shoulders and add power. No gym needed — just a resistance band.",
            style = MaterialTheme.typography.bodyMedium)
        Dryland.routines.forEach { r ->
            Card(onClick = { onDryland(r.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${r.minutes} min · ${r.whenToDo}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DrillScreen(id: String, onBack: () -> Unit) {
    val drill = Drills.byId(id)
    ScreenScaffold(title = drill?.name ?: "Drill", onBack = onBack) { padding ->
        Page(padding) {
            if (drill == null) EmptyState("Drill not found", "") else DrillDetailBody(drill)
        }
    }
}

@Composable
fun GuideScreen(id: String, onBack: () -> Unit, onDrill: (String) -> Unit) {
    val guide = StrokeGuide.byId(id)
    ScreenScaffold(title = guide?.title ?: "Technique", onBack = onBack) { padding ->
        Page(padding) {
            if (guide == null) {
                EmptyState("Not found", "")
                return@Page
            }
            Text(guide.overview, style = MaterialTheme.typography.bodyLarge)
            guide.sections.forEach { s ->
                SectionHeader(s.title)
                BulletList(s.points)
            }
            if (guide.faults.isNotEmpty()) {
                SectionHeader("Common faults & fixes")
                guide.faults.forEach { f ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(f.problem, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(f.fix, style = MaterialTheme.typography.bodyMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                f.drillIds.mapNotNull { Drills.byId(it) }.forEach { d ->
                                    Surface(
                                        onClick = { onDrill(d.id) },
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Text(d.name, style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrylandScreen(id: String, onBack: () -> Unit) {
    val routine = Dryland.routines.firstOrNull { it.id == id }
    ScreenScaffold(title = routine?.title ?: "Dryland", onBack = onBack) { padding ->
        Page(padding) {
            if (routine == null) {
                EmptyState("Not found", "")
                return@Page
            }
            Text("${routine.minutes} min · ${routine.whenToDo}", style = MaterialTheme.typography.bodyMedium)
            routine.exercises.forEachIndexed { i, e ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${i + 1}. ${e.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Tag(e.prescription)
                        Text(e.how, style = MaterialTheme.typography.bodyMedium)
                        Text(e.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
