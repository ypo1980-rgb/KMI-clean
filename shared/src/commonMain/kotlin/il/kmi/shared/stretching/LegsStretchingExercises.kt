package il.kmi.shared.stretching

object LegsStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "legs_standing_quadriceps",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת ירך קדמית בעמידה",
                titleEn = "Standing quadriceps stretch",
                instructionsHe =
                    "עמדו זקוף ליד קיר או כיסא לתמיכה. " +
                            "כופפו ברך אחת ואחזו בקרסול או בכף הרגל. " +
                            "קרבו בעדינות את העקב לכיוון הישבן תוך שמירה על הברכיים קרובות. " +
                            "החזיקו וחזרו בצד השני.",
                instructionsEn =
                    "Stand upright beside a wall or chair for support. " +
                            "Bend one knee and hold the ankle or foot. " +
                            "Gently bring the heel toward your buttock while keeping the knees close together. " +
                            "Hold, then repeat on the other side.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_standing_quadriceps_start",
                safetyNoteHe =
                    "אין למשוך את הרגל בכוח או לקשת את הגב. שמרו את הברך מופנית כלפי מטה.",
                safetyNoteEn =
                    "Do not force the leg or arch your back. Keep the bent knee pointing downward.",
                sortOrder = 0,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_standing_quadriceps_start",
                            instructionHe =
                                "עמדו זקוף והיעזרו במשטח יציב.",
                            instructionEn =
                                "Stand upright and use a stable surface for support.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_standing_quadriceps_left",
                            instructionHe =
                                "כופפו את רגל שמאל וקרבו את העקב בעדינות.",
                            instructionEn =
                                "Bend your left leg and gently bring the heel closer.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_standing_quadriceps_right",
                            instructionHe =
                                "החליפו צד ובצעו את המתיחה ברגל ימין.",
                            instructionEn =
                                "Change sides and stretch your right leg.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_seated_hamstring",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת ירך אחורית בישיבה",
                titleEn = "Seated hamstring stretch",
                instructionsHe =
                    "שבו בקצה כיסא יציב. " +
                            "יישרו רגל אחת לפנים והניחו את העקב על הרצפה כשהאצבעות פונות מעלה. " +
                            "הטו את הגוף מעט קדימה מהאגן תוך שמירה על גב ישר. " +
                            "החזיקו וחזרו ברגל השנייה.",
                instructionsEn =
                    "Sit near the edge of a stable chair. " +
                            "Extend one leg forward with the heel on the floor and toes pointing upward. " +
                            "Lean slightly forward from the hips while keeping your back straight. " +
                            "Hold, then repeat with the other leg.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_seated_hamstring_start",
                safetyNoteHe =
                    "אין לעגל את הגב או ללחוץ על הברך. עצרו בטווח שבו מורגשת מתיחה עדינה בלבד.",
                safetyNoteEn =
                    "Do not round your back or press on the knee. Stop at a gentle stretching sensation.",
                sortOrder = 1,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_seated_hamstring_start",
                            instructionHe =
                                "שבו בקצה הכיסא והניחו את שתי כפות הרגליים על הרצפה.",
                            instructionEn =
                                "Sit near the edge of the chair with both feet on the floor.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_seated_hamstring_left",
                            instructionHe =
                                "יישרו את רגל שמאל והטו את הגוף מעט קדימה.",
                            instructionEn =
                                "Extend your left leg and lean slightly forward.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_seated_hamstring_right",
                            instructionHe =
                                "חזרו למרכז ובצעו את המתיחה ברגל ימין.",
                            instructionEn =
                                "Return to the center and stretch your right leg.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_standing_hamstring",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת ירך אחורית בעמידה",
                titleEn = "Standing hamstring stretch",
                instructionsHe =
                    "עמדו זקוף והניחו רגל אחת מעט לפנים. " +
                            "השאירו את העקב על הרצפה והרימו את אצבעות כף הרגל. " +
                            "כופפו מעט את הרגל האחורית והטו את האגן לאחור עד שמורגשת מתיחה. " +
                            "החליפו צד.",
                instructionsEn =
                    "Stand upright and place one foot slightly forward. " +
                            "Keep the heel on the floor and lift the toes. " +
                            "Slightly bend the back leg and move your hips backward until you feel a stretch. " +
                            "Change sides.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_standing_hamstring_start",
                safetyNoteHe =
                    "שמרו על גב ישר ועל הברך הקדמית משוחררת. אין לדחוף את הגוף בכוח.",
                safetyNoteEn =
                    "Keep your back straight and the front knee relaxed. Do not force the movement.",
                sortOrder = 2,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_standing_hamstring_start",
                            instructionHe =
                                "עמדו זקוף כשהרגליים ברוחב האגן.",
                            instructionEn =
                                "Stand upright with your feet hip-width apart.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_standing_hamstring_left",
                            instructionHe =
                                "הניחו את רגל שמאל לפנים והעבירו את האגן לאחור.",
                            instructionEn =
                                "Place your left leg forward and move your hips backward.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_standing_hamstring_right",
                            instructionHe =
                                "חזרו למרכז ובצעו את המתיחה ברגל ימין.",
                            instructionEn =
                                "Return to the center and stretch your right leg.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_wall_calf",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת תאומים מול קיר",
                titleEn = "Wall calf stretch",
                instructionsHe =
                    "עמדו מול קיר והניחו עליו את הידיים. " +
                            "שלחו רגל אחת לאחור ושמרו את הברך ישרה ואת העקב על הרצפה. " +
                            "כופפו מעט את הברך הקדמית והעבירו את הגוף קדימה. " +
                            "החזיקו והחליפו צד.",
                instructionsEn =
                    "Stand facing a wall and place your hands against it. " +
                            "Step one leg backward, keeping the knee straight and heel on the floor. " +
                            "Slightly bend the front knee and move your body forward. " +
                            "Hold, then change sides.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_wall_calf_start",
                safetyNoteHe =
                    "כף הרגל האחורית צריכה לפנות קדימה. אין להרים את העקב מהרצפה.",
                safetyNoteEn =
                    "Keep the back foot pointing forward. Do not lift the heel from the floor.",
                sortOrder = 3,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_wall_calf_start",
                            instructionHe =
                                "עמדו מול הקיר והניחו עליו את הידיים.",
                            instructionEn =
                                "Stand facing the wall and place your hands against it.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_wall_calf_left",
                            instructionHe =
                                "שלחו את רגל שמאל לאחור והשאירו את העקב על הרצפה.",
                            instructionEn =
                                "Step your left leg back and keep the heel on the floor.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_wall_calf_right",
                            instructionHe =
                                "החליפו רגליים ובצעו את המתיחה בצד השני.",
                            instructionEn =
                                "Change legs and stretch the other side.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_wall_soleus",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת שוק עם ברך כפופה",
                titleEn = "Bent-knee calf stretch",
                instructionsHe =
                    "עמדו מול קיר והניחו עליו את הידיים. " +
                            "שלחו רגל אחת מעט לאחור והשאירו את העקב על הרצפה. " +
                            "כופפו בעדינות את שתי הברכיים והורידו מעט את הגוף. " +
                            "החזיקו והחליפו צד.",
                instructionsEn =
                    "Stand facing a wall and place your hands against it. " +
                            "Step one leg slightly backward and keep its heel on the floor. " +
                            "Gently bend both knees and lower your body slightly. " +
                            "Hold, then change sides.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_wall_soleus_start",
                safetyNoteHe =
                    "שמרו את העקב האחורי צמוד לרצפה ואת הברך בקו כף הרגל.",
                safetyNoteEn =
                    "Keep the back heel on the floor and align the knee with the foot.",
                sortOrder = 4,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_wall_soleus_start",
                            instructionHe =
                                "עמדו מול הקיר בתנוחה יציבה.",
                            instructionEn =
                                "Stand in a stable position facing the wall.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_wall_soleus_left",
                            instructionHe =
                                "שלחו את רגל שמאל לאחור וכופפו בעדינות את הברכיים.",
                            instructionEn =
                                "Step your left leg back and gently bend both knees.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_wall_soleus_right",
                            instructionHe =
                                "החליפו צד ושמרו את העקב האחורי על הרצפה.",
                            instructionEn =
                                "Change sides and keep the back heel on the floor.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_standing_inner_thigh",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת פנים הירך בעמידה",
                titleEn = "Standing inner-thigh stretch",
                instructionsHe =
                    "עמדו בפיסוק רחב כשהבהונות פונות קדימה. " +
                            "העבירו את משקל הגוף באיטיות לצד אחד וכופפו את הברך באותו צד. " +
                            "השאירו את הרגל השנייה ישרה עד שמורגשת מתיחה בפנים הירך. " +
                            "חזרו למרכז והחליפו צד.",
                instructionsEn =
                    "Stand with your feet wide apart and toes pointing forward. " +
                            "Slowly shift your weight to one side and bend that knee. " +
                            "Keep the opposite leg straight until you feel a stretch along the inner thigh. " +
                            "Return to the center and change sides.",
                durationSeconds = 20,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_standing_inner_thigh_start",
                safetyNoteHe =
                    "שמרו את הברך הכפופה בקו כף הרגל ואל תרדו מעבר לטווח נוח.",
                safetyNoteEn =
                    "Keep the bent knee aligned with the foot and do not lower beyond a comfortable range.",
                sortOrder = 5,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_standing_inner_thigh_start",
                            instructionHe =
                                "עמדו בפיסוק רחב כשהבהונות פונות קדימה.",
                            instructionEn =
                                "Stand with your feet wide apart and toes pointing forward.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_standing_inner_thigh_left",
                            instructionHe =
                                "העבירו את המשקל שמאלה וכופפו את ברך שמאל.",
                            instructionEn =
                                "Shift your weight left and bend your left knee.",
                            durationSeconds = 20,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_standing_inner_thigh_right",
                            instructionHe =
                                "חזרו למרכז והעבירו את המשקל לצד ימין.",
                            instructionEn =
                                "Return to the center and shift your weight to the right.",
                            durationSeconds = 20,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_crossed_outer_thigh",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת הצד החיצוני של הירך",
                titleEn = "Standing outer-thigh stretch",
                instructionsHe =
                    "עמדו זקוף והצליבו רגל אחת מאחורי הרגל השנייה. " +
                            "העבירו את האגן בעדינות לכיוון הרגל האחורית. " +
                            "הטו מעט את פלג הגוף לצד הנגדי עד שמורגשת מתיחה בצד הירך. " +
                            "חזרו למרכז והחליפו צד.",
                instructionsEn =
                    "Stand upright and cross one leg behind the other. " +
                            "Gently move your hips toward the back leg. " +
                            "Lean your upper body slightly to the opposite side until you feel a stretch along the outer thigh. " +
                            "Return to the center and change sides.",
                durationSeconds = 20,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_crossed_outer_thigh_start",
                safetyNoteHe =
                    "בצעו הטיה קטנה בלבד ושמרו על שיווי משקל. ניתן להיעזר בקיר.",
                safetyNoteEn =
                    "Use only a small lean and maintain your balance. Use a wall for support if needed.",
                sortOrder = 6,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_crossed_outer_thigh_start",
                            instructionHe =
                                "עמדו זקוף ליד משטח יציב.",
                            instructionEn =
                                "Stand upright beside a stable surface.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_crossed_outer_thigh_left",
                            instructionHe =
                                "הצליבו את רגל שמאל מאחור והטו את הגוף בעדינות.",
                            instructionEn =
                                "Cross your left leg behind and gently lean your body.",
                            durationSeconds = 20,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_crossed_outer_thigh_right",
                            instructionHe =
                                "חזרו למרכז ובצעו את המתיחה בצד השני.",
                            instructionEn =
                                "Return to the center and stretch the other side.",
                            durationSeconds = 20,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_supine_hamstring",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת ירך אחורית בשכיבה",
                titleEn = "Supine hamstring stretch",
                instructionsHe =
                    "שכבו על הגב כשהברכיים כפופות וכפות הרגליים על המשטח. " +
                            "הרימו רגל אחת ואחזו מאחורי הירך. " +
                            "יישרו את הברך בהדרגה עד שמורגשת מתיחה עדינה בירך האחורית. " +
                            "כופפו את הברך, הורידו את הרגל והחליפו צד.",
                instructionsEn =
                    "Lie on your back with your knees bent and feet on the floor. " +
                            "Lift one leg and hold behind the thigh. " +
                            "Gradually straighten the knee until you feel a gentle stretch along the back of the thigh. " +
                            "Bend the knee, lower the leg, and change sides.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_supine_hamstring_start",
                safetyNoteHe =
                    "אין למשוך מאחורי הברך ואין ליישר אותה בכוח. השאירו את הראש על המשטח.",
                safetyNoteEn =
                    "Do not pull behind the knee or force it straight. Keep your head resting on the floor.",
                sortOrder = 7,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_supine_hamstring_start",
                            instructionHe =
                                "שכבו על הגב וכופפו את שתי הברכיים.",
                            instructionEn =
                                "Lie on your back and bend both knees.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_supine_hamstring_left",
                            instructionHe =
                                "הרימו את רגל שמאל ויישרו את הברך בהדרגה.",
                            instructionEn =
                                "Lift your left leg and gradually straighten the knee.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_supine_hamstring_right",
                            instructionHe =
                                "הורידו את הרגל ובצעו את המתיחה ברגל ימין.",
                            instructionEn =
                                "Lower the leg and stretch your right leg.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_prone_quadriceps",
                category = StretchingCategory.LEGS,
                titleHe = "מתיחת ירך קדמית בשכיבה",
                titleEn = "Prone quadriceps stretch",
                instructionsHe =
                    "שכבו על הבטן כשהרגליים ישרות. " +
                            "כופפו ברך אחת ואחזו בקרסול או בכף הרגל. " +
                            "קרבו בעדינות את העקב לכיוון הישבן תוך שמירה על האגן צמוד למשטח. " +
                            "שחררו באיטיות והחליפו צד.",
                instructionsEn =
                    "Lie on your stomach with your legs straight. " +
                            "Bend one knee and hold the ankle or foot. " +
                            "Gently bring the heel toward your buttock while keeping your hips against the floor. " +
                            "Release slowly and change sides.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "legs_prone_quadriceps_start",
                safetyNoteHe =
                    "אין לקשת את הגב או להרים את האגן. עצרו אם מורגש לחץ בברך.",
                safetyNoteEn =
                    "Do not arch your back or lift your hips. Stop if you feel pressure in the knee.",
                sortOrder = 8,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_prone_quadriceps_start",
                            instructionHe =
                                "שכבו על הבטן והרפו את הרגליים.",
                            instructionEn =
                                "Lie on your stomach and relax your legs.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_prone_quadriceps_left",
                            instructionHe =
                                "כופפו את רגל שמאל וקרבו את העקב בעדינות.",
                            instructionEn =
                                "Bend your left leg and gently bring the heel closer.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_prone_quadriceps_right",
                            instructionHe =
                                "שחררו ובצעו את המתיחה ברגל ימין.",
                            instructionEn =
                                "Release and stretch your right leg.",
                            durationSeconds = 25,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "legs_controlled_leg_swings",
                category = StretchingCategory.LEGS,
                titleHe = "הנפות רגל מבוקרות",
                titleEn = "Controlled leg swings",
                instructionsHe =
                    "עמדו לצד קיר או כיסא והחזיקו בו לתמיכה. " +
                            "הניעו רגל אחת באיטיות לפנים ולאחור בטווח קטן ונוח. " +
                            "שמרו את פלג הגוף יציב ואת הברך משוחררת. " +
                            "האטו, עצרו והחליפו רגל.",
                instructionsEn =
                    "Stand beside a wall or chair and hold it for support. " +
                            "Slowly swing one leg forward and backward through a small, comfortable range. " +
                            "Keep your upper body stable and the knee relaxed. " +
                            "Slow down, stop, and change legs.",
                durationSeconds = 30,
                repetitions = 10,
                performBothSides = true,
                imageKey = "legs_controlled_leg_swings_start",
                safetyNoteHe =
                    "התחילו בטווח קטן. אין לבצע הנפות מהירות או לאבד שליטה בתנועה.",
                safetyNoteEn =
                    "Begin with a small range. Do not swing quickly or lose control of the movement.",
                sortOrder = 9,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "legs_controlled_leg_swings_start",
                            instructionHe =
                                "עמדו זקוף והחזיקו במשטח יציב.",
                            instructionEn =
                                "Stand upright and hold a stable surface.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_controlled_leg_swings_left",
                            instructionHe =
                                "הניעו את רגל שמאל באיטיות לפנים ולאחור.",
                            instructionEn =
                                "Slowly move your left leg forward and backward.",
                            durationSeconds = 30,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "legs_controlled_leg_swings_right",
                            instructionHe =
                                "עצרו בעדינות ובצעו את התנועה ברגל ימין.",
                            instructionEn =
                                "Stop gently and repeat with your right leg.",
                            durationSeconds = 30,
                            type = StretchingStepType.MOVE
                        )
                    )
            )
        )
}