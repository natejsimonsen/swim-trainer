package com.swimtrainer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swimtrainer.app.data.AppData
import com.swimtrainer.app.data.CssEntry
import com.swimtrainer.app.data.Repository
import com.swimtrainer.core.engine.Energy
import com.swimtrainer.core.engine.PlanGenerator
import com.swimtrainer.core.engine.WorkoutGenerator
import com.swimtrainer.core.engine.WorkoutRequest
import com.swimtrainer.core.model.BlockType
import com.swimtrainer.core.model.PlanDay
import com.swimtrainer.core.model.PlanWeek
import com.swimtrainer.core.model.PoolLength
import com.swimtrainer.core.model.SessionType
import com.swimtrainer.core.model.Stroke
import com.swimtrainer.core.model.SwimmerProfile
import com.swimtrainer.core.model.TrainingPlan
import com.swimtrainer.core.model.Weekday
import com.swimtrainer.core.model.Workout
import com.swimtrainer.core.model.WorkoutLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/** Where the workout on screen came from, so completing it can tick off the plan. */
data class PlanRef(val planId: String, val week: Int, val day: Weekday) {
    val key get() = "$planId:$week:${day.name}"
}

data class ActiveWorkout(val workout: Workout, val planRef: PlanRef? = null)

sealed interface UiState {
    data object Loading : UiState
    data class Ready(val data: AppData) : UiState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    val state: StateFlow<UiState> = repo.data
        .map<AppData, UiState> { UiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _active = MutableStateFlow<ActiveWorkout?>(null)
    val active: StateFlow<ActiveWorkout?> = _active.asStateFlow()

    private val _quick = MutableStateFlow<Workout?>(null)
    val quick: StateFlow<Workout?> = _quick.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val data get() = (state.value as? UiState.Ready)?.data

    val profile get() = data?.profile

    // ------------------------------------------------------------------ dates

    fun today(): LocalDate = LocalDate.now()

    fun todayWeekday(): Weekday = Weekday.entries[today().dayOfWeek.value - 1]

    private fun mondayOf(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** 1-based plan week that contains today, or null if outside the plan. */
    fun currentWeek(plan: TrainingPlan): Int? {
        val diff = today().toEpochDay() - plan.createdEpochDay
        if (diff < 0) return null
        val week = (diff / 7).toInt() + 1
        return week.takeIf { it <= plan.weeks.size }
    }

    fun dateOf(plan: TrainingPlan, week: Int, day: Weekday): LocalDate =
        LocalDate.ofEpochDay(plan.createdEpochDay).plusWeeks((week - 1).toLong()).plusDays(day.ordinal.toLong())

    data class NextSession(val week: PlanWeek, val day: PlanDay, val isToday: Boolean)

    fun nextSession(data: AppData): NextSession? {
        val plan = data.plan ?: return null
        val todayEpoch = today().toEpochDay()
        for (week in plan.weeks) for (day in week.days) {
            val date = dateOf(plan, week.number, day.day).toEpochDay()
            val done = PlanRef(plan.id, week.number, day.day).key in data.completed
            if (date >= todayEpoch && !done) return NextSession(week, day, date == todayEpoch)
        }
        return null
    }

    // ------------------------------------------------------------------ profile & plan

    fun saveProfile(profile: SwimmerProfile, rebuildPlan: Boolean, weeks: Int? = null) {
        viewModelScope.launch {
            val old = data?.profile
            repo.saveProfile(profile)
            if (profile.cssPer100 != null && profile.cssPer100 != old?.cssPer100) {
                addCssEntry(profile.cssPer100!!)
            }
            if (rebuildPlan) generatePlan(profile, weeks ?: data?.plan?.weeks?.size ?: 8)
        }
    }

    fun generatePlan(profile: SwimmerProfile? = this.profile, weeks: Int) {
        val p = profile ?: return
        viewModelScope.launch {
            _busy.value = true
            val plan = withContext(Dispatchers.Default) {
                PlanGenerator.generate(p, weeks, mondayOf(today()).toEpochDay(), Random.nextLong())
            }
            repo.savePlan(plan)
            _busy.value = false
        }
    }

    fun deletePlan() {
        viewModelScope.launch { repo.savePlan(null) }
    }

    /** Swap a planned session for a freshly generated one of the same type and length. */
    fun regeneratePlanDay(ref: PlanRef) {
        val d = data ?: return
        val plan = d.plan ?: return
        val profile = d.profile ?: return
        viewModelScope.launch {
            val newPlan = withContext(Dispatchers.Default) {
                plan.copy(weeks = plan.weeks.map { week ->
                    if (week.number != ref.week) week else week.copy(days = week.days.map { day ->
                        if (day.day != ref.day) day else {
                            val old = day.workout
                            day.copy(workout = WorkoutGenerator(profile).generate(
                                WorkoutRequest(
                                    type = old.type,
                                    minutes = old.estimatedMinutes.coerceAtLeast(15),
                                    focusStroke = old.focusStroke,
                                    phase = week.phase,
                                    seed = Random.nextLong(),
                                )
                            ))
                        }
                    })
                })
            }
            repo.savePlan(newPlan)
            val updated = newPlan.weeks.first { it.number == ref.week }.days.first { it.day == ref.day }.workout
            _active.value = ActiveWorkout(updated, ref)
        }
    }

    fun openPlanWorkout(plan: TrainingPlan, week: PlanWeek, day: PlanDay) {
        _active.value = ActiveWorkout(day.workout, PlanRef(plan.id, week.number, day.day))
    }

    fun openWorkout(workout: Workout) {
        _active.value = ActiveWorkout(workout)
    }

    // ------------------------------------------------------------------ quick sets

    fun generateQuick(type: SessionType?, minutes: Int, stroke: Stroke, energy: Energy, singleSet: Boolean, pool: PoolLength?) {
        val base = profile ?: return
        val p = if (pool != null) base.copy(pool = pool) else base
        val chosen = type ?: listOf(
            SessionType.ENDURANCE, SessionType.THRESHOLD, SessionType.SPEED, SessionType.TECHNIQUE, SessionType.IM,
        ).let { options -> if (energy == Energy.LOW) listOf(SessionType.TECHNIQUE, SessionType.RECOVERY, SessionType.ENDURANCE) else options }.random()
        viewModelScope.launch {
            val workout = withContext(Dispatchers.Default) {
                val gen = WorkoutGenerator(p)
                if (singleSet) {
                    val seed = Random.nextLong()
                    val block = gen.generateSet(chosen, minutes, stroke, seed)
                    val w = Workout(
                        id = "s${seed.toULong().toString(36)}",
                        title = "Set: ${block.title}",
                        type = chosen,
                        focusStroke = stroke,
                        pool = p.pool,
                        blocks = listOf(block.copy(type = BlockType.MAIN)),
                        summary = "${block.totalDistance} ${p.pool.unit} · ~${block.estimatedSec / 60} min · ${chosen.description}",
                    )
                    w.copy(calories = com.swimtrainer.core.engine.Calories.forWorkout(w, p.weightKg))
                } else {
                    gen.generate(WorkoutRequest(chosen, minutes, stroke, energy = energy))
                }
            }
            _quick.value = workout
        }
    }

    // ------------------------------------------------------------------ logging

    fun logWorkout(title: String, type: SessionType, distance: Int, minutes: Int, calories: Int, rpe: Int, notes: String, ref: PlanRef?) {
        val d = data ?: return
        viewModelScope.launch {
            val log = WorkoutLog(
                id = "l${System.currentTimeMillis()}",
                epochDay = today().toEpochDay(),
                title = title,
                type = type,
                distance = distance,
                minutes = minutes,
                calories = calories,
                rpe = rpe,
                notes = notes,
                planWeek = ref?.week,
                planDay = ref?.day,
            )
            repo.saveLogs(listOf(log) + d.logs)
            if (ref != null) repo.setCompleted(d.completed + ref.key)
        }
    }

    fun deleteLog(log: WorkoutLog) {
        val d = data ?: return
        viewModelScope.launch {
            repo.saveLogs(d.logs.filterNot { it.id == log.id })
            val plan = d.plan
            if (plan != null && log.planWeek != null && log.planDay != null) {
                repo.setCompleted(d.completed - PlanRef(plan.id, log.planWeek!!, log.planDay!!).key)
            }
        }
    }

    fun toggleCompleted(ref: PlanRef) {
        val d = data ?: return
        viewModelScope.launch {
            repo.setCompleted(if (ref.key in d.completed) d.completed - ref.key else d.completed + ref.key)
        }
    }

    // ------------------------------------------------------------------ CSS

    fun updateCss(css: Double, rebuildPlan: Boolean) {
        val p = profile ?: return
        saveProfile(p.copy(cssPer100 = css), rebuildPlan)
    }

    private suspend fun addCssEntry(css: Double) {
        val history = data?.cssHistory ?: emptyList()
        repo.saveCssHistory(history + CssEntry(today().toEpochDay(), css))
    }
}
