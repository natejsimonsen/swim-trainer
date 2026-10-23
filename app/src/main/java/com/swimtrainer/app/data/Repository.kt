package com.swimtrainer.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.TrainingPlan
import com.swimtrainer.core.model.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "swim_trainer")

@Serializable
data class CssEntry(val epochDay: Long, val cssPer100: Double)

data class AppData(
    val profile: SwimmerProfile?,
    val plan: TrainingPlan?,
    val logs: List<WorkoutLog>,
    /** Keys of completed plan sessions: "planId:week:day". */
    val completed: Set<String>,
    val cssHistory: List<CssEntry>,
)

class Repository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val profile = stringPreferencesKey("profile")
        val plan = stringPreferencesKey("plan")
        val logs = stringPreferencesKey("logs")
        val completed = stringSetPreferencesKey("completed")
        val cssHistory = stringPreferencesKey("css_history")
    }

    private fun <T> decode(serializer: KSerializer<T>, text: String?): T? =
        text?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }

    val data: Flow<AppData> = context.dataStore.data.map { prefs ->
        AppData(
            profile = decode(SwimmerProfile.serializer(), prefs[Keys.profile]),
            plan = decode(TrainingPlan.serializer(), prefs[Keys.plan]),
            logs = decode(ListSerializer(WorkoutLog.serializer()), prefs[Keys.logs]) ?: emptyList(),
            completed = prefs[Keys.completed] ?: emptySet(),
            cssHistory = decode(ListSerializer(CssEntry.serializer()), prefs[Keys.cssHistory]) ?: emptyList(),
        )
    }

    suspend fun saveProfile(profile: SwimmerProfile) {
        context.dataStore.edit { it[Keys.profile] = json.encodeToString(SwimmerProfile.serializer(), profile) }
    }

    suspend fun savePlan(plan: TrainingPlan?) {
        context.dataStore.edit {
            if (plan == null) it.remove(Keys.plan) else it[Keys.plan] = json.encodeToString(TrainingPlan.serializer(), plan)
        }
    }

    suspend fun saveLogs(logs: List<WorkoutLog>) {
        context.dataStore.edit { it[Keys.logs] = json.encodeToString(ListSerializer(WorkoutLog.serializer()), logs) }
    }

    suspend fun setCompleted(keys: Set<String>) {
        context.dataStore.edit { it[Keys.completed] = keys }
    }

    suspend fun saveCssHistory(history: List<CssEntry>) {
        context.dataStore.edit { it[Keys.cssHistory] = json.encodeToString(ListSerializer(CssEntry.serializer()), history) }
    }
}
