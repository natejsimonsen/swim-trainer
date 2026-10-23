package com.swimtrainer.core.content

data class DrylandExercise(
    val name: String,
    val prescription: String,
    val how: String,
    val why: String,
)

data class DrylandRoutine(
    val id: String,
    val title: String,
    val minutes: Int,
    val whenToDo: String,
    val exercises: List<DrylandExercise>,
)

/** Short land routines that complement swim training. No equipment needed except a resistance band. */
object Dryland {
    val routines = listOf(
        DrylandRoutine(
            id = "pre_swim",
            title = "Pre-swim activation",
            minutes = 6,
            whenToDo = "On the pool deck before every session.",
            exercises = listOf(
                DrylandExercise("Arm circles", "10 forward + 10 backward each arm", "Big, slow circles, one arm at a time.", "Mobilises the shoulder joint through its full range."),
                DrylandExercise("Band pull-aparts", "2 × 15", "Hold a band at shoulder height, pull it apart squeezing shoulder blades.", "Wakes up the upper back that stabilises your stroke."),
                DrylandExercise("Leg swings", "10 each leg, front-back and side-to-side", "Hold the wall, swing loosely.", "Loosens hips for a better kick."),
                DrylandExercise("Streamline squats", "10", "Arms locked in streamline overhead, squat to parallel.", "Activates legs and practises the streamline position."),
                DrylandExercise("Ankle rolls", "10 each direction", "Seated or standing, roll each ankle.", "Flexible ankles = a more propulsive kick."),
            ),
        ),
        DrylandRoutine(
            id = "shoulder_prehab",
            title = "Shoulder prehab",
            minutes = 12,
            whenToDo = "2–3× per week, after swimming or on rest days.",
            exercises = listOf(
                DrylandExercise("Band external rotation", "3 × 12 each arm", "Elbow tucked at your side at 90°, rotate the forearm outward against the band.", "Strengthens the rotator cuff — the #1 defence against swimmer's shoulder."),
                DrylandExercise("Y-T-W raises", "2 × 8 each letter", "Lying face down, lift arms into Y, T then W shapes, thumbs up.", "Builds lower trapezius and scapular control."),
                DrylandExercise("Scapular push-ups", "2 × 12", "In a plank, keep arms straight and let shoulder blades sink together then push apart.", "Trains the serratus anterior for a stable catch."),
                DrylandExercise("Sleeper stretch", "2 × 30 s each side", "Lie on your side, arm at 90°, gently press the forearm toward the floor.", "Maintains internal rotation range."),
                DrylandExercise("Doorway pec stretch", "2 × 30 s", "Forearms on a door frame, lean through gently.", "Counters the rounded-shoulder posture swimming can cause."),
            ),
        ),
        DrylandRoutine(
            id = "core",
            title = "Swimmer's core",
            minutes = 15,
            whenToDo = "2× per week on non-consecutive days.",
            exercises = listOf(
                DrylandExercise("Hollow hold", "3 × 20–40 s", "On your back, lower back pressed down, arms in streamline and legs lifted.", "The exact body position of a tight streamline."),
                DrylandExercise("Flutter kicks", "3 × 30 s", "In the hollow position, small fast kicks.", "Builds a kick that comes from the hips."),
                DrylandExercise("Side plank with reach", "2 × 8 each side", "Side plank, reach top arm under then up to the ceiling.", "Rotational stability for long-axis strokes."),
                DrylandExercise("Superman", "3 × 10", "Face down, lift arms and legs together, hold 2 s.", "Strengthens the posterior chain for a high body position."),
                DrylandExercise("Dead bug", "3 × 10 each side", "On your back, extend opposite arm and leg while keeping the lower back flat.", "Teaches limbs to move while the core stays still."),
                DrylandExercise("Russian twists", "3 × 20", "Seated, feet up, rotate side to side.", "Supports powerful body rotation."),
            ),
        ),
        DrylandRoutine(
            id = "power",
            title = "Power & legs",
            minutes = 20,
            whenToDo = "1–2× per week for intermediate swimmers and above. Skip in taper week.",
            exercises = listOf(
                DrylandExercise("Squat jumps", "3 × 8", "Squat then explode up with arms in streamline, land softly.", "Explosive power for starts and push-offs."),
                DrylandExercise("Walking lunges", "3 × 10 each leg", "Long stride, back knee near the floor.", "Single-leg strength and hip stability."),
                DrylandExercise("Push-ups", "3 × 8–15", "Body in a straight line, chest to the floor.", "Pressing strength for the pull and turns."),
                DrylandExercise("Band pulldowns (pull simulation)", "3 × 15", "Band anchored high, bend at the hips and pull from overhead to hips with a high elbow.", "Swim-specific pull strength and catch pattern."),
                DrylandExercise("Glute bridges", "3 × 12", "On your back, drive hips up, squeeze glutes.", "Powerful hip extension for the kick and push-offs."),
            ),
        ),
        DrylandRoutine(
            id = "mobility",
            title = "Recovery mobility",
            minutes = 10,
            whenToDo = "After any session or on rest days.",
            exercises = listOf(
                DrylandExercise("Child's pose with side reach", "60 s", "Kneel, sit back, walk hands to each side.", "Lats and shoulders lengthen after swimming."),
                DrylandExercise("Thoracic rotation", "8 each side", "On all fours, hand behind head, rotate elbow to the ceiling.", "Mid-back mobility for rotation and breathing."),
                DrylandExercise("Hip-flexor stretch", "45 s each side", "Half-kneeling, tuck the pelvis, lean forward gently.", "Opens hips tight from sitting."),
                DrylandExercise("Ankle plantar-flexion stretch", "45 s", "Kneel with toes pointed back, sit gently on your heels.", "Pointed toes = more efficient kick."),
                DrylandExercise("Neck rolls", "5 each direction", "Slow, controlled.", "Releases tension from breathing rotation."),
            ),
        ),
    )
}
