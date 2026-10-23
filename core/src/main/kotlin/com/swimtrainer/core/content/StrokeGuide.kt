package com.swimtrainer.core.content

import com.swimtrainer.core.model.Stroke

data class GuideSection(val title: String, val points: List<String>)

data class Fault(val problem: String, val fix: String, val drillIds: List<String>)

data class StrokeGuideEntry(
    val id: String,
    val title: String,
    val stroke: Stroke?,
    val overview: String,
    val sections: List<GuideSection>,
    val faults: List<Fault>,
)

object StrokeGuide {

    val entries: List<StrokeGuideEntry> = listOf(
        StrokeGuideEntry(
            id = "freestyle",
            title = "Freestyle (front crawl)",
            stroke = Stroke.FREESTYLE,
            overview = "The fastest and most efficient stroke. Think long, balanced and rotating — " +
                "speed comes from a strong catch and minimal drag, not from spinning the arms.",
            sections = listOf(
                GuideSection(
                    "Body position", listOf(
                        "Head neutral, eyes looking at the bottom slightly ahead; waterline at the top of the head.",
                        "Press your chest gently into the water so the hips ride high.",
                        "Body rotates 35–45° to each side as one unit — hips and shoulders together.",
                    )
                ),
                GuideSection(
                    "Arms — entry & catch", listOf(
                        "Enter fingertips first, in line with your shoulder (never across the centre line).",
                        "Extend forward under the surface, then tip the fingertips down and bend the elbow: the 'early vertical forearm'.",
                        "Imagine reaching over a barrel — forearm and palm face backward as early as possible.",
                    )
                ),
                GuideSection(
                    "Arms — pull & recovery", listOf(
                        "Pull back under your body, accelerating, and push past the hip.",
                        "Exit pinky first, lift the elbow high and relax the forearm on the recovery.",
                        "Front-quadrant timing: one hand starts to pull just before the other enters.",
                    )
                ),
                GuideSection(
                    "Kick", listOf(
                        "Kick from the hips with long, relaxed legs and loose, pointed ankles.",
                        "Narrow and compact — feet stay within the body's 'shadow'.",
                        "2-beat kick for distance/triathlon, 6-beat for sprint and for keeping hips up.",
                    )
                ),
                GuideSection(
                    "Breathing", listOf(
                        "Exhale continuously under water through the nose and mouth.",
                        "Rotate the head with the body to breathe — keep one goggle in the water.",
                        "Breathe as the arm on that side starts to pull; return the face before that arm recovers.",
                        "Bilateral breathing (every 3) builds a symmetrical stroke; every 2 is fine for hard efforts.",
                    )
                ),
            ),
            faults = listOf(
                Fault("Sinking legs", "Look down, press the chest, kick from the hips.", listOf("free_side_kick", "free_6_1_6")),
                Fault("Crossing over on entry", "Enter at 'shoulder-width railway tracks'.", listOf("free_catch_up", "free_single_arm")),
                Fault("Dropped elbow / slipping catch", "Bend the elbow early, forearm vertical.", listOf("free_fist", "free_scull_front", "free_doggy")),
                Fault("Lifting head to breathe", "Rotate, don't lift — one goggle in.", listOf("free_side_kick", "free_three_pause")),
                Fault("Short, choppy stroke", "Finish past the hip, extend forward.", listOf("free_dps", "free_catch_up")),
                Fault("Flat body, no rotation", "Drive rotation from the hips.", listOf("free_6_1_6", "free_zipper")),
            ),
        ),
        StrokeGuideEntry(
            id = "backstroke",
            title = "Backstroke",
            stroke = Stroke.BACKSTROKE,
            overview = "Freestyle on your back: a still head, a high flat body, constant rotation and a continuous windmill of the arms.",
            sections = listOf(
                GuideSection(
                    "Body position", listOf(
                        "Ears in the water, eyes looking straight up, chin neutral.",
                        "Hips high — imagine your belly button reaching the surface.",
                        "Rotate shoulders and hips together ~45° each way. The head never moves.",
                    )
                ),
                GuideSection(
                    "Arms", listOf(
                        "Exit thumb first, straight arm recovery directly over the shoulder.",
                        "Rotate the hand so the little finger enters first at 11 and 1 o'clock.",
                        "Catch deep (15–30 cm), bend the elbow to ~90° and push the water toward your feet.",
                        "Finish with the palm pressing down past the thigh — it helps you rotate.",
                        "Arms stay opposite each other, like a windmill.",
                    )
                ),
                GuideSection(
                    "Kick", listOf(
                        "Continuous flutter from the hips, knees stay under the surface.",
                        "Up-kick is the propulsive phase — toes flick the surface.",
                    )
                ),
                GuideSection(
                    "Breathing & timing", listOf(
                        "Breathe in on one arm's recovery and out on the other.",
                        "6 kicks per stroke cycle keeps the rhythm steady.",
                    )
                ),
                GuideSection(
                    "Backstroke flags & turns", listOf(
                        "Count your strokes from the flags (5 m from the wall) to the wall so you never hit your head.",
                        "Turn: on the final stroke rotate to your front, take one freestyle pull and flip; push off on your back.",
                    )
                ),
            ),
            faults = listOf(
                Fault("Sitting in the water", "Head back, hips up.", listOf("back_kick_side", "back_cup")),
                Fault("Snaking / entering behind the head", "Enter at 11 and 1 o'clock.", listOf("back_single_arm", "back_double_arm")),
                Fault("Flat, no rotation", "Rotate the shoulder into each catch.", listOf("back_6_switch", "back_single_arm")),
                Fault("Bicycle kick", "Kick from the hips, knees under.", listOf("back_kick_side")),
            ),
        ),
        StrokeGuideEntry(
            id = "breaststroke",
            title = "Breaststroke",
            stroke = Stroke.BREASTSTROKE,
            overview = "A timing stroke: pull, breathe, kick, glide. The kick provides most of the propulsion; the glide is where you go fast.",
            sections = listOf(
                GuideSection(
                    "Body position", listOf(
                        "Start every cycle in a long, flat streamline with the head between the arms.",
                        "Hips stay high — the body rocks slightly but doesn't bob up and down.",
                    )
                ),
                GuideSection(
                    "Arms", listOf(
                        "Outsweep: hands slide out to just wider than the shoulders, palms angled out.",
                        "Catch & insweep: elbows stay high as the hands sweep down and in under the chin.",
                        "Shoot the hands forward quickly — the pull never goes past the shoulders.",
                    )
                ),
                GuideSection(
                    "Kick", listOf(
                        "Recovery: heels draw toward your bottom, knees hip-width.",
                        "Turn the feet out (flex the ankles) to catch water with the soles and inner shins.",
                        "Whip the feet around and back together, finishing with toes pointed.",
                    )
                ),
                GuideSection(
                    "Breathing", listOf(
                        "Breathe every stroke as the hands sweep in — the shoulders rise naturally.",
                        "Look down, not forward, as you return the head.",
                    )
                ),
                GuideSection(
                    "Timing", listOf(
                        "Pull–breathe–kick–glide. Arms extend forward just before the legs kick.",
                        "Underwater pullout off each wall: one pull to the hips, one dolphin kick, one kick to surface.",
                    )
                ),
            ),
            faults = listOf(
                Fault("Scissor kick", "Both feet turned out and symmetrical.", listOf("breast_kick_board", "breast_kick_back")),
                Fault("Pulling too far back", "Keep the hands in front of the shoulders.", listOf("breast_pull_flutter")),
                Fault("No glide / rushed timing", "Pull, breathe, kick, glide.", listOf("breast_2k1p", "breast_glide")),
                Fault("Knees drawn under the body", "Drop the heels, not the knees.", listOf("breast_kick_back")),
            ),
        ),
        StrokeGuideEntry(
            id = "butterfly",
            title = "Butterfly",
            stroke = Stroke.BUTTERFLY,
            overview = "A rhythmical, wave-like stroke powered by the chest press and two dolphin kicks per arm cycle. Rhythm beats strength.",
            sections = listOf(
                GuideSection(
                    "Body undulation", listOf(
                        "The wave starts at the chest: press down, let the hips rise, the legs follow.",
                        "Keep the undulation small and forward — not diving down deep.",
                    )
                ),
                GuideSection(
                    "Arms", listOf(
                        "Enter thumbs first at shoulder-width, hands slightly down.",
                        "Catch with high elbows, pull in a keyhole shape under the body and push past the hips.",
                        "Recover low and relaxed over the water, arms almost straight, thumbs down.",
                    )
                ),
                GuideSection(
                    "Kick", listOf(
                        "Two dolphin kicks per stroke: one as the hands enter, one as they push out.",
                        "Legs together, kick from the hips with loose ankles.",
                    )
                ),
                GuideSection(
                    "Breathing", listOf(
                        "Breathe forward with the chin skimming the water as the arms finish the push.",
                        "Face back down before the arms enter. Many swimmers breathe every second stroke.",
                    )
                ),
            ),
            faults = listOf(
                Fault("Legs sinking / no rhythm", "Chest press drives the hips up.", listOf("fly_body_dolphin", "fly_2_kick_glide")),
                Fault("Only one kick", "Kick the hands in, kick the hands out.", listOf("fly_single_arm")),
                Fault("Arms dragging on recovery", "Finish the push, then low and fast recovery.", listOf("fly_free_kick")),
                Fault("Lifting too high to breathe", "Chin forward, not up.", listOf("fly_single_arm", "fly_2_kick_glide")),
            ),
        ),
        StrokeGuideEntry(
            id = "starts_turns",
            title = "Starts, turns & underwaters",
            stroke = null,
            overview = "Walls are free speed. In a 25 pool up to a third of your race is off the walls.",
            sections = listOf(
                GuideSection(
                    "Streamline", listOf(
                        "Hands stacked, thumb locked, arms squeezing behind the ears.",
                        "Push off at 30–50 cm depth, hold the streamline, then kick.",
                    )
                ),
                GuideSection(
                    "Flip turn (free/back)", listOf(
                        "Don't slow down. Last stroke at the T, chin to chest, tight tuck.",
                        "Feet plant shoulder-width, push on your back/side, rotate during the glide.",
                    )
                ),
                GuideSection(
                    "Open turn (breast/fly)", listOf(
                        "Two-hand simultaneous touch, elbow back, knees tuck, breathe, 'answer the phone', push on side.",
                    )
                ),
                GuideSection(
                    "Underwater dolphin kicks", listOf(
                        "Small, fast kicks from the core. Start with 2–3 per wall and build toward 5–6.",
                        "Break out with a strong pull — no breath on the first stroke.",
                    )
                ),
                GuideSection(
                    "Dive start (only where depth is ≥ 1.8 m and permitted)", listOf(
                        "Track start: one foot forward, weight slightly back, eyes down.",
                        "Drive forward, hands enter first through one 'hole', then body follows.",
                    )
                ),
            ),
            faults = listOf(
                Fault("Slow turns", "Practise somersaults mid-pool, then at the wall.", listOf("gen_flip_turn", "gen_open_turn")),
                Fault("Loose streamline", "Squeeze the ears, lock the thumbs.", listOf("gen_streamline")),
            ),
        ),
        StrokeGuideEntry(
            id = "open_water",
            title = "Open water",
            stroke = null,
            overview = "No walls, no lane lines, no black line. Efficiency, navigation and confidence matter as much as fitness.",
            sections = listOf(
                GuideSection(
                    "Sighting", listOf(
                        "'Crocodile eyes': lift only the eyes, then roll to breathe. Every 6–10 strokes.",
                        "Pick large, high landmarks — buildings, trees — rather than buoys alone.",
                    )
                ),
                GuideSection(
                    "Stroke adjustments", listOf(
                        "Slightly higher turnover and straighter recovery handle chop better.",
                        "Bilateral breathing lets you breathe away from waves or sun.",
                        "A wetsuit raises the hips: you can kick less and save the legs for the bike/run.",
                    )
                ),
                GuideSection(
                    "Drafting & buoys", listOf(
                        "Swim just behind someone's feet or beside their hip to save up to 20% energy.",
                        "Corkscrew around buoys: one stroke on your back to change direction quickly.",
                    )
                ),
                GuideSection(
                    "Safety", listOf(
                        "Never swim alone. Wear a bright cap and use a tow float.",
                        "Acclimatise to cold water gradually; exit if you start shivering uncontrollably.",
                        "Check water quality and currents. Know the exit points.",
                    )
                ),
            ),
            faults = listOf(
                Fault("Zig-zagging", "Sight more often and fix the cross-over.", listOf("ow_sighting", "free_catch_up")),
                Fault("Panic at the start", "Start wide, exhale steadily, settle the breathing.", listOf("free_bilateral")),
            ),
        ),
        StrokeGuideEntry(
            id = "safety",
            title = "Safety & healthy training",
            stroke = null,
            overview = "Train smart so you can train for years.",
            sections = listOf(
                GuideSection(
                    "Before you swim", listOf(
                        "If you're new to exercise, have a heart or lung condition, are pregnant, or over 65 and starting out, check with your doctor.",
                        "Only swim where a lifeguard is present or with a buddy.",
                    )
                ),
                GuideSection(
                    "Breath-holding", listOf(
                        "Never hyperventilate before underwater swimming and never do breath-holding contests. Shallow-water blackout can happen without warning.",
                        "Hypoxic sets in this app are limited to comfortable breathing patterns — always breathe if you need to.",
                    )
                ),
                GuideSection(
                    "Shoulders", listOf(
                        "Swimmer's shoulder comes from too much volume too soon and poor technique — progress volume ≤ 10% per week.",
                        "Use paddles sparingly and only with good technique. Stop if you feel pinching pain.",
                        "Strengthen the rotator cuff and upper back on dry land 2×/week.",
                    )
                ),
                GuideSection(
                    "Recovery", listOf(
                        "At least one full rest day per week.",
                        "Sleep 7–9 hours. Hydrate — you sweat in the pool.",
                        "Persistent fatigue, poor sleep or falling performance = take an extra easy day.",
                    )
                ),
            ),
            faults = emptyList(),
        ),
    )

    fun byId(id: String) = entries.firstOrNull { it.id == id }
}
