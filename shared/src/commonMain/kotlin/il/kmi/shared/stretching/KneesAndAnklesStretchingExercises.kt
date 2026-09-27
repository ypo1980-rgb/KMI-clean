package il.kmi.shared.stretching

object KneesAndAnklesStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "knees_ankles_seated_knee_extension",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "יישור ברך בישיבה",
                titleEn = "Seated knee extension",
                instructionsHe =
                    "שבו זקוף על כיסא כששתי כפות הרגליים מונחות על הרצפה. " +
                            "יישרו ברך אחת באיטיות עד לטווח נוח. " +
                            "עצרו לזמן קצר והורידו את הרגל בשליטה. " +
                            "החליפו רגל.",
                instructionsEn =
                    "Sit upright on a chair with both feet on the floor. " +
                            "Slowly straighten one knee to a comfortable range. " +
                            "Pause briefly, then lower the leg with control. " +
                            "Change legs.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = true,
                imageKey = "knees_ankles_knee_extension_start",
                safetyNoteHe =
                    "אין לנעול את הברך או להרים את הירך מהמושב.",
                safetyNoteEn =
                    "Do not lock the knee or lift the thigh from the seat.",
                sortOrder = 0,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_knee_extension_start",
                            instructionHe = "שבו זקוף והניחו את כפות הרגליים על הרצפה.",
                            instructionEn = "Sit upright with both feet on the floor.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_knee_extension_left",
                            instructionHe = "יישרו את ברך שמאל באיטיות והורידו בשליטה.",
                            instructionEn = "Slowly straighten your left knee and lower it with control.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_knee_extension_right",
                            instructionHe = "בצעו את התנועה ברגל ימין.",
                            instructionEn = "Repeat the movement with your right leg.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_seated_knee_flexion",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "כפיפת ברך בישיבה",
                titleEn = "Seated knee flexion",
                instructionsHe =
                    "שבו זקוף על קצה כיסא יציב. " +
                            "החליקו כף רגל אחת לאחור מתחת לכיסא תוך כפיפה הדרגתית של הברך. " +
                            "עצרו בטווח נוח והחזירו את הרגל לפנים. " +
                            "החליפו צד.",
                instructionsEn =
                    "Sit upright near the edge of a stable chair. " +
                            "Slide one foot backward beneath the chair while gradually bending the knee. " +
                            "Pause at a comfortable range, then slide the foot forward. " +
                            "Change sides.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = true,
                imageKey = "knees_ankles_knee_flexion_start",
                safetyNoteHe =
                    "אין למשוך את כף הרגל בכוח או להמשיך אם מופיע כאב חד בברך.",
                safetyNoteEn =
                    "Do not force the foot backward or continue if sharp knee pain appears.",
                sortOrder = 1,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_knee_flexion_start",
                            instructionHe = "שבו בקצה הכיסא כשהרגליים לפנים.",
                            instructionEn = "Sit near the edge of the chair with your feet forward.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_knee_flexion_left",
                            instructionHe = "החליקו את רגל שמאל לאחור והחזירו אותה לפנים.",
                            instructionEn = "Slide your left foot backward, then return it forward.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_knee_flexion_right",
                            instructionHe = "בצעו את התנועה ברגל ימין.",
                            instructionEn = "Repeat the movement with your right leg.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_heel_slide",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "החלקת עקב בשכיבה",
                titleEn = "Supine heel slide",
                instructionsHe =
                    "שכבו על הגב כשהרגליים ישרות. " +
                            "החליקו עקב אחד לכיוון הישבן תוך כפיפה הדרגתית של הברך. " +
                            "החליקו את העקב בחזרה עד ליישור נוח. " +
                            "החליפו רגל.",
                instructionsEn =
                    "Lie on your back with both legs straight. " +
                            "Slide one heel toward your buttock while gradually bending the knee. " +
                            "Slide the heel away until the leg is comfortably straight. " +
                            "Change legs.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = true,
                imageKey = "knees_ankles_heel_slide_start",
                safetyNoteHe =
                    "בצעו את ההחלקה באיטיות ואל תכפו כפיפה או יישור של הברך.",
                safetyNoteEn =
                    "Move slowly and do not force the knee into flexion or extension.",
                sortOrder = 2,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_heel_slide_start",
                            instructionHe = "שכבו על הגב כשהרגליים ישרות.",
                            instructionEn = "Lie on your back with both legs straight.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_heel_slide_left",
                            instructionHe = "החליקו את עקב שמאל פנימה והחזירו אותו.",
                            instructionEn = "Slide your left heel inward, then return it.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_heel_slide_right",
                            instructionHe = "בצעו את התנועה ברגל ימין.",
                            instructionEn = "Repeat the movement with your right leg.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_standing_knee_flexion",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "כפיפת ברך בעמידה",
                titleEn = "Standing knee flexion",
                instructionsHe =
                    "עמדו זקוף והחזיקו בכיסא או בקיר לתמיכה. " +
                            "כופפו ברך אחת והרימו את העקב לכיוון הישבן בטווח נוח. " +
                            "הורידו את הרגל באיטיות והחליפו צד.",
                instructionsEn =
                    "Stand upright and hold a chair or wall for support. " +
                            "Bend one knee and lift the heel toward your buttock through a comfortable range. " +
                            "Lower the leg slowly and change sides.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = true,
                imageKey = "knees_ankles_standing_knee_flexion_start",
                safetyNoteHe =
                    "שמרו את הברכיים קרובות ואל תניפו את הרגל במהירות.",
                safetyNoteEn =
                    "Keep your knees close together and do not swing the leg quickly.",
                sortOrder = 3,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_standing_knee_flexion_start",
                            instructionHe = "עמדו זקוף והחזיקו במשטח יציב.",
                            instructionEn = "Stand upright and hold a stable surface.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_standing_knee_flexion_left",
                            instructionHe = "כופפו את ברך שמאל והורידו את הרגל בשליטה.",
                            instructionEn = "Bend your left knee and lower the leg with control.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_standing_knee_flexion_right",
                            instructionHe = "בצעו את התנועה ברגל ימין.",
                            instructionEn = "Repeat the movement with your right leg.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_supported_mini_squat",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "כפיפת ברכיים קטנה בתמיכה",
                titleEn = "Supported mini squat",
                instructionsHe =
                    "עמדו מול כיסא יציב והחזיקו במשענת. " +
                            "שלחו מעט את האגן לאחור וכופפו את הברכיים בטווח קטן. " +
                            "שמרו את הברכיים בקו כפות הרגליים וחזרו לעמידה.",
                instructionsEn =
                    "Stand facing a stable chair and hold the backrest. " +
                            "Move your hips slightly backward and bend your knees through a small range. " +
                            "Keep your knees aligned with your feet, then return to standing.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = false,
                imageKey = "knees_ankles_mini_squat_start",
                safetyNoteHe =
                    "אין לרדת עמוק. עצרו אם מופיע כאב בברך או אובדן שיווי משקל.",
                safetyNoteEn =
                    "Do not squat deeply. Stop if knee pain or loss of balance occurs.",
                sortOrder = 4,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_mini_squat_start",
                            instructionHe = "עמדו מול כיסא והחזיקו במשענת.",
                            instructionEn = "Stand facing a chair and hold the backrest.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_mini_squat_down",
                            instructionHe = "כופפו מעט את הברכיים ושלחו את האגן לאחור.",
                            instructionEn = "Slightly bend your knees and move your hips backward.",
                            durationSeconds = 3,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_mini_squat_up",
                            instructionHe = "חזרו לעמידה באיטיות.",
                            instructionEn = "Slowly return to standing.",
                            durationSeconds = 3,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_ankle_circles",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "סיבובי קרסול",
                titleEn = "Ankle circles",
                instructionsHe =
                    "שבו זקוף על כיסא והרימו מעט רגל אחת. " +
                            "סובבו את הקרסול במעגלים קטנים ואיטיים. " +
                            "החליפו כיוון ולאחר מכן החליפו רגל.",
                instructionsEn =
                    "Sit upright on a chair and slightly lift one foot. " +
                            "Rotate the ankle in small, slow circles. " +
                            "Reverse direction, then change legs.",
                durationSeconds = 30,
                repetitions = 10,
                performBothSides = true,
                imageKey = "knees_ankles_ankle_circles_start",
                safetyNoteHe =
                    "הניעו רק את הקרסול ושמרו על תנועה קטנה ונוחה.",
                safetyNoteEn =
                    "Move only the ankle and keep the circles small and comfortable.",
                sortOrder = 5,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_ankle_circles_start",
                            instructionHe = "שבו זקוף והרימו מעט את רגל שמאל.",
                            instructionEn = "Sit upright and slightly lift your left foot.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_ankle_circles_left",
                            instructionHe = "סובבו את קרסול שמאל לשני הכיוונים.",
                            instructionEn = "Rotate your left ankle in both directions.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_ankle_circles_right",
                            instructionHe = "החליפו רגל וסובבו את קרסול ימין.",
                            instructionEn = "Change legs and rotate your right ankle.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_ankle_pumps",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "כפיפה ופשיטה של הקרסול",
                titleEn = "Ankle pumps",
                instructionsHe =
                    "שבו זקוף כשהרגליים מעט לפנים. " +
                            "משכו את אצבעות כפות הרגליים לכיוון הגוף. " +
                            "לאחר מכן הפנו אותן בעדינות קדימה. " +
                            "המשיכו בתנועה איטית ורציפה.",
                instructionsEn =
                    "Sit upright with your legs slightly forward. " +
                            "Draw your toes toward your body. " +
                            "Then gently point them forward. " +
                            "Continue with a slow, controlled movement.",
                durationSeconds = 30,
                repetitions = 12,
                performBothSides = false,
                imageKey = "knees_ankles_ankle_pumps_start",
                safetyNoteHe =
                    "אין לדחוף את האצבעות בכוח או לבצע תנועה מהירה.",
                safetyNoteEn =
                    "Do not force the toes or perform the movement quickly.",
                sortOrder = 6,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_ankle_pumps_start",
                            instructionHe = "שבו זקוף והושיטו מעט את הרגליים.",
                            instructionEn = "Sit upright and extend your legs slightly.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_ankle_pumps_flex",
                            instructionHe = "משכו את אצבעות כפות הרגליים לכיוון הגוף.",
                            instructionEn = "Draw your toes toward your body.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_ankle_pumps_point",
                            instructionHe = "הפנו את אצבעות כפות הרגליים קדימה.",
                            instructionEn = "Gently point your toes forward.",
                            durationSeconds = 2,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_wall_dorsiflexion",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "קידום ברך לכיוון הקיר",
                titleEn = "Wall ankle dorsiflexion",
                instructionsHe =
                    "עמדו מול קיר והניחו רגל אחת קרוב אליו. " +
                            "קדמו את הברך באיטיות לכיוון הקיר מבלי להרים את העקב. " +
                            "חזרו לעמדת ההתחלה והחליפו רגל.",
                instructionsEn =
                    "Stand facing a wall and place one foot close to it. " +
                            "Slowly move the knee toward the wall without lifting the heel. " +
                            "Return to the starting position and change legs.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = true,
                imageKey = "knees_ankles_wall_dorsiflexion_start",
                safetyNoteHe =
                    "הברך צריכה לנוע בקו אצבעות כף הרגל והעקב חייב להישאר על הרצפה.",
                safetyNoteEn =
                    "Move the knee in line with the toes and keep the heel on the floor.",
                sortOrder = 7,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_wall_dorsiflexion_start",
                            instructionHe = "עמדו מול הקיר והניחו רגל אחת קרוב אליו.",
                            instructionEn = "Stand facing the wall with one foot close to it.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_wall_dorsiflexion_left",
                            instructionHe = "קדמו את ברך שמאל לכיוון הקיר והחזירו אותה.",
                            instructionEn = "Move your left knee toward the wall, then return.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_wall_dorsiflexion_right",
                            instructionHe = "בצעו את התנועה ברגל ימין.",
                            instructionEn = "Repeat the movement with your right leg.",
                            durationSeconds = 15,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_supported_heel_raises",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "הרמות עקבים בתמיכה",
                titleEn = "Supported heel raises",
                instructionsHe =
                    "עמדו זקוף מול כיסא או קיר והחזיקו לתמיכה. " +
                            "הרימו את שני העקבים באיטיות ועלו על כריות כפות הרגליים. " +
                            "עצרו לזמן קצר והורידו את העקבים בשליטה.",
                instructionsEn =
                    "Stand upright facing a chair or wall and hold it for support. " +
                            "Slowly lift both heels and rise onto the balls of your feet. " +
                            "Pause briefly, then lower your heels with control.",
                durationSeconds = 30,
                repetitions = 10,
                performBothSides = false,
                imageKey = "knees_ankles_heel_raises_start",
                safetyNoteHe =
                    "שמרו על גוף זקוף ואל תגלגלו את הקרסוליים פנימה או החוצה.",
                safetyNoteEn =
                    "Keep your body upright and do not roll the ankles inward or outward.",
                sortOrder = 8,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_heel_raises_start",
                            instructionHe = "עמדו זקוף והחזיקו במשטח יציב.",
                            instructionEn = "Stand upright and hold a stable surface.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_heel_raises_up",
                            instructionHe = "הרימו את העקבים ועלו באיטיות.",
                            instructionEn = "Lift your heels and rise slowly.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_heel_raises_down",
                            instructionHe = "הורידו את העקבים בשליטה.",
                            instructionEn = "Lower your heels with control.",
                            durationSeconds = 2,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "knees_ankles_supported_toe_raises",
                category = StretchingCategory.KNEES_AND_ANKLES,
                titleHe = "הרמות אצבעות כף הרגל",
                titleEn = "Supported toe raises",
                instructionsHe =
                    "עמדו זקוף מול כיסא או קיר והחזיקו לתמיכה. " +
                            "השאירו את העקבים על הרצפה והרימו את אצבעות כפות הרגליים. " +
                            "עצרו לזמן קצר והורידו אותן באיטיות.",
                instructionsEn =
                    "Stand upright facing a chair or wall and hold it for support. " +
                            "Keep your heels on the floor and lift your toes. " +
                            "Pause briefly, then lower them slowly.",
                durationSeconds = 30,
                repetitions = 10,
                performBothSides = false,
                imageKey = "knees_ankles_toe_raises_start",
                safetyNoteHe =
                    "שמרו על ברכיים משוחררות ועל תנועה קטנה ומבוקרת.",
                safetyNoteEn =
                    "Keep your knees relaxed and use a small, controlled movement.",
                sortOrder = 9,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "knees_ankles_toe_raises_start",
                            instructionHe = "עמדו זקוף והחזיקו במשטח יציב.",
                            instructionEn = "Stand upright and hold a stable surface.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_toe_raises_up",
                            instructionHe = "הרימו את אצבעות כפות הרגליים והשאירו את העקבים למטה.",
                            instructionEn = "Lift your toes while keeping your heels down.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "knees_ankles_toe_raises_down",
                            instructionHe = "הורידו את כפות הרגליים באיטיות.",
                            instructionEn = "Slowly lower your feet.",
                            durationSeconds = 2,
                            type = StretchingStepType.RELEASE
                        )
                    )
            )
        )
}