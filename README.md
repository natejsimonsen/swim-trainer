# Swim Trainer (Android)

A personal swim coach for Android. Tell it your **age, weight, level and the days you swim**, and it builds a complete, periodised training plan. It can also generate a full workout or a single set on the spot. Every session includes warm-up, drills, pre-set, main set and cool-down, with stroke, distance, intensity zone, target time and send-off interval for each rep.

## Features

**Personal profile**
- Age, weight (kg/lb), optional height, level (Beginner → Elite), goal, training days, session length, pool (25 yd / 25 m / 50 m), available equipment, and strokes to work on.
- Age shapes the training: masters swimmers get longer warm-ups and more rest, swimmers 55+ get at most two hard sessions a week, and young swimmers get more technique and variety.
- Weight drives calorie estimates and fueling targets (protein, fluids, carbs).

**Pacing built on CSS (Critical Swim Speed)**
- Six intensity zones, from recovery to sprint, each with an RPE and a description.
- If you haven't tested, your pace is estimated from level and age. Week 1 of the plan includes a 400 + 200 CSS test, and every recovery week re-tests.
- Target times and send-offs adjust for stroke, kick/pull/drill, rep length, level and age.

**Training plans**
- 4–16 week plans: base → build → peak → taper for race goals, with a recovery and re-test week every fourth week.
- Volume rises about 5% per week, and hard days are spread out so they don't fall back to back.
- Session mix depends on goal: fitness, weight loss, technique, endurance, speed, triathlon, open water or competition.
- You can swap any session for a freshly generated one, and tick sessions off as you complete them.

**Quick set: generate on the fly**
- A full workout or just one set, sized to the minutes you have.
- Pick a focus (or "Surprise me"), a stroke, how you feel (tired swimmers get easier zones), and the pool you're in today.

**In-pool workout player**
- A pool-style sweep pace clock with a target-time arc and a send-off marker.
- Auto-advances on the interval, with 3-2-1 beeps and vibration. The screen stays on.
- Shows rep, round, zone, target, notes, drill how-tos and what's up next.

**Library**
- 40+ drills for all four strokes, turns and open water. Each has steps, cues, common mistakes, level and equipment.
- A technique guide for freestyle, backstroke, breaststroke, butterfly, starts and turns, open water, and safety, with faults linked to the drills that fix them.
- Dryland routines: pre-swim activation, shoulder prehab, core, power and mobility.

**Tools & progress**
- CSS test calculator, zone table, interval calculator, race predictor, fueling card, and a standalone pace clock.
- Workout log with RPE and notes, totals, week streak, an 8-week distance chart and CSS history.

## Build

Requirements: JDK 17 and the Android SDK (API 35).

```bash
./gradlew :core:test          # training-engine unit tests
./gradlew :app:assembleDebug  # app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and run the `app` configuration.

CI (GitHub Actions) runs the tests and uploads debug and release APKs as a build artifact on every push.

## Architecture

| Module | What it contains |
| --- | --- |
| `core` | Pure Kotlin/JVM, no Android dependencies. Models, `Pace` (CSS & zones), `WorkoutGenerator`, `PlanGenerator`, `Calories`, plus content: `Drills`, `StrokeGuide`, `Dryland`. Fully unit-tested. |
| `app` | Jetpack Compose + Material 3 UI, Navigation Compose, DataStore persistence (JSON via kotlinx.serialization), one `AppViewModel`. |

Min SDK 26 (Android 8.0), target SDK 35.

> Training and nutrition guidance is general information, not medical advice. Check with a doctor before starting a new exercise programme.
