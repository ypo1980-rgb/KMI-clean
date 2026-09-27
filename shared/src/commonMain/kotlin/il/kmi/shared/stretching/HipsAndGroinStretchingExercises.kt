package il.kmi.shared.stretching

internal object HipsAndGroinStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "hips_groin_butterfly",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "מתיחת פרפר בישיבה",
                titleEn = "Seated butterfly stretch",
                instructionsHe =
                    "שבו זקוף והצמידו את כפות הרגליים זו לזו. אחזו בכפות הרגליים ואפשרו לברכיים לרדת בעדינות לצדדים. הישארו בגב ארוך והטו מעט את הגוף לפנים לפי הנוחות.",
                instructionsEn =
                    "Sit upright and bring the soles of your feet together. Hold your feet and allow your knees to lower gently to the sides. Keep your back long and lean slightly forward as comfortable.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "hips_groin_butterfly_start",
                safetyNoteHe =
                    "אין ללחוץ על הברכיים בכוח ואין להמשיך אם מופיע כאב במפשעה או בברכיים.",
                safetyNoteEn =
                    "Do not force the knees downward, and stop if you feel pain in the groin or knees.",
                sortOrder = 1,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_butterfly_start",
                            instructionHe = "שבו זקוף.",
                            instructionEn = "Sit upright.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_butterfly_stretch",
                            instructionHe = "הישענו מעט לפנים.",
                            instructionEn = "Lean slightly forward.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_supine_figure_four",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "מתיחת ישבן בשכיבה",
                titleEn = "Supine figure-four stretch",
                instructionsHe =
                    "שכבו על הגב כשהברכיים כפופות. הניחו קרסול אחד מעל הירך הנגדית. אחזו מאחורי הירך התחתונה ומשכו אותה בעדינות לכיוון הגוף. החליפו צד.",
                instructionsEn =
                    "Lie on your back with your knees bent. Place one ankle over the opposite thigh. Hold behind the lower thigh and gently draw it toward your body. Change sides.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "hips_groin_supine_figure_four_start",
                safetyNoteHe =
                    "שמרו את הראש והכתפיים על המשטח ואל תלחצו ישירות על הברך.",
                safetyNoteEn =
                    "Keep your head and shoulders supported, and do not press directly on the knee.",
                sortOrder = 2,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_supine_figure_four_start",
                            instructionHe = "מוכנים.",
                            instructionEn = "Get ready.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_supine_figure_four_left",
                            instructionHe = "קרבו את הרגל בעדינות.",
                            instructionEn = "Gently bring the leg closer.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_supine_figure_four_right",
                            instructionHe = "החליפו צד.",
                            instructionEn = "Change sides.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_kneeling_hip_flexor",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "מתיחת מכופפי הירך בכריעה",
                titleEn = "Kneeling hip-flexor stretch",
                instructionsHe =
                    "כרעו על ברך אחת והניחו את כף הרגל השנייה לפנים. שמרו את הגב זקוף והעבירו את האגן מעט לפנים עד שמורגשת מתיחה בקדמת הירך של הרגל האחורית. החליפו צד.",
                instructionsEn =
                    "Kneel on one knee with the opposite foot forward. Keep your torso upright and move your pelvis slightly forward until you feel a stretch at the front of the rear hip. Change sides.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "hips_groin_kneeling_hip_flexor_start",
                safetyNoteHe =
                    "השתמשו בריפוד מתחת לברך ואל תקשתו את הגב התחתון.",
                safetyNoteEn =
                    "Use cushioning under the knee and avoid arching your lower back.",
                sortOrder = 3,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_kneeling_hip_flexor_start",
                            instructionHe = "עברו למנח כריעה.",
                            instructionEn = "Move into a kneeling position.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_kneeling_hip_flexor_left",
                            instructionHe = "העבירו את האגן מעט לפנים.",
                            instructionEn = "Move your pelvis slightly forward.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_kneeling_hip_flexor_right",
                            instructionHe = "החליפו צד.",
                            instructionEn = "Change sides.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_adductor_side_lunge",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "מתיחת מפשעה במכרע צדי",
                titleEn = "Side-lunge adductor stretch",
                instructionsHe =
                    "עמדו בפישוק רחב והעבירו את משקל הגוף באיטיות לצד אחד תוך כפיפת הברך. השאירו את הרגל השנייה ישרה ואת כף הרגל על הרצפה. חזרו למרכז והחליפו צד.",
                instructionsEn =
                    "Stand with your feet wide apart and slowly shift your weight to one side while bending that knee. Keep the opposite leg straight and its foot on the floor. Return to the center and change sides.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "hips_groin_adductor_side_lunge_start",
                safetyNoteHe =
                    "שמרו את הברך הכפופה בכיוון כף הרגל ואל תרדו לטווח שמכאיב.",
                safetyNoteEn =
                    "Keep the bent knee aligned with the foot and do not lower into a painful range.",
                sortOrder = 4,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_adductor_side_lunge_start",
                            instructionHe = "עמדו בפישוק רחב.",
                            instructionEn = "Stand with your feet wide apart.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_adductor_side_lunge_left",
                            instructionHe = "העבירו משקל לצד.",
                            instructionEn = "Shift your weight to one side.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_adductor_side_lunge_right",
                            instructionHe = "עברו לצד השני.",
                            instructionEn = "Move to the other side.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_seated_wide_fold",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "כפיפה קדימה בפישוק ישיבה",
                titleEn = "Seated wide-leg forward stretch",
                instructionsHe =
                    "שבו בפישוק נוח כשהברכיים פונות כלפי מעלה. שמרו על גב ארוך והחליקו את הידיים לפנים תוך הטיית הגוף מהאגן. עצרו בטווח נוח וחזרו באיטיות.",
                instructionsEn =
                    "Sit with your legs comfortably apart and knees facing upward. Keep your back long and slide your hands forward while hinging from the hips. Stop within a comfortable range and return slowly.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "hips_groin_seated_wide_fold_start",
                safetyNoteHe =
                    "אין לעגל את הגב בכוח או לנסות להגיע לרצפה.",
                safetyNoteEn =
                    "Do not force your back to round or try to reach the floor.",
                sortOrder = 5,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_seated_wide_fold_start",
                            instructionHe = "שבו זקוף בפישוק נוח.",
                            instructionEn = "Sit upright with your legs comfortably apart.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_seated_wide_fold_stretch",
                            instructionHe = "הטו את הגוף מעט לפנים.",
                            instructionEn = "Lean slightly forward.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_frog_rock_back",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "העברת אגן לאחור בפישוק ברכיים",
                titleEn = "Wide-knee rock-back stretch",
                instructionsHe =
                    "עברו לעמידת שש והרחיקו מעט את הברכיים זו מזו. שמרו את השוקיים על המשטח והעבירו את האגן באיטיות לאחור. חזרו קדימה בטווח נוח.",
                instructionsEn =
                    "Begin on your hands and knees and move your knees slightly apart. Keep your lower legs supported and slowly move your hips backward. Return forward within a comfortable range.",
                durationSeconds = 35,
                repetitions = 6,
                performBothSides = false,
                imageKey = "hips_groin_frog_rock_back_start",
                safetyNoteHe =
                    "השתמשו בריפוד והקטינו את הפישוק אם מורגש לחץ בברכיים או במפשעה.",
                safetyNoteEn =
                    "Use cushioning and reduce the width if you feel pressure in the knees or groin.",
                sortOrder = 6,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_frog_rock_back_start",
                            instructionHe = "עברו לעמידת שש.",
                            instructionEn = "Move onto your hands and knees.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_frog_rock_back_stretch",
                            instructionHe = "העבירו את האגן לאחור.",
                            instructionEn = "Move your hips backward.",
                            durationSeconds = 4,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_seated_figure_four",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "מתיחת ישבן בישיבה",
                titleEn = "Seated figure-four stretch",
                instructionsHe =
                    "שבו על כיסא יציב והניחו קרסול אחד מעל הירך הנגדית. שמרו את הגב ישר והטו את הגוף מעט לפנים מהאגן. חזרו למרכז והחליפו צד.",
                instructionsEn =
                    "Sit on a stable chair and place one ankle over the opposite thigh. Keep your back straight and lean slightly forward from the hips. Return upright and change sides.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "hips_groin_seated_figure_four_start",
                safetyNoteHe =
                    "אין ללחוץ על הברך העליונה או להמשיך אם מופיע כאב במפרק הירך.",
                safetyNoteEn =
                    "Do not press on the upper knee or continue if you feel pain in the hip joint.",
                sortOrder = 7,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_seated_figure_four_start",
                            instructionHe = "שבו זקוף.",
                            instructionEn = "Sit upright.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_seated_figure_four_left",
                            instructionHe = "הישענו מעט לפנים.",
                            instructionEn = "Lean slightly forward.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_seated_figure_four_right",
                            instructionHe = "החליפו צד.",
                            instructionEn = "Change sides.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_hip_rotations_90_90",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "העברת ברכיים מצד לצד בישיבה",
                titleEn = "Seated hip rotations",
                instructionsHe =
                    "שבו כשהברכיים כפופות וכפות הרגליים על הרצפה. הניחו את הידיים מאחור לתמיכה והעבירו את שתי הברכיים יחד מצד לצד באיטיות.",
                instructionsEn =
                    "Sit with your knees bent and feet on the floor. Place your hands behind you for support and slowly move both knees together from side to side.",
                durationSeconds = 35,
                repetitions = 8,
                performBothSides = true,
                imageKey = "hips_groin_hip_rotations_start",
                safetyNoteHe =
                    "עבדו בטווח נוח ואל תכריחו את הברכיים להגיע לרצפה.",
                safetyNoteEn =
                    "Move within a comfortable range and do not force the knees toward the floor.",
                sortOrder = 8,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_hip_rotations_start",
                            instructionHe = "מוכנים.",
                            instructionEn = "Get ready.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_hip_rotations_left",
                            instructionHe = "העבירו את הברכיים לצד.",
                            instructionEn = "Move your knees to one side.",
                            durationSeconds = 3,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_hip_rotations_right",
                            instructionHe = "עברו לצד השני.",
                            instructionEn = "Move to the other side.",
                            durationSeconds = 3,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_supine_adductor",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "פתיחת ברכיים בשכיבה",
                titleEn = "Supine groin stretch",
                instructionsHe =
                    "שכבו על הגב, כופפו את הברכיים והצמידו את כפות הרגליים. אפשרו לברכיים להיפתח בעדינות לצדדים תוך שמירה על נשימה רגועה.",
                instructionsEn =
                    "Lie on your back, bend your knees, and bring the soles of your feet together. Allow the knees to open gently to the sides while breathing comfortably.",
                durationSeconds = 35,
                repetitions = 2,
                performBothSides = false,
                imageKey = "hips_groin_supine_adductor_start",
                safetyNoteHe =
                    "אין ללחוץ על הברכיים. תמכו בירכיים בעזרת כריות אם המתיחה חזקה מדי.",
                safetyNoteEn =
                    "Do not press on the knees. Support the thighs with cushions if the stretch feels too strong.",
                sortOrder = 9,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_supine_adductor_start",
                            instructionHe = "שכבו בנוחות.",
                            instructionEn = "Lie down comfortably.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_supine_adductor_stretch",
                            instructionHe = "פתחו את הברכיים בעדינות.",
                            instructionEn = "Gently open your knees.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "hips_groin_standing_hip_circles",
                category = StretchingCategory.HIPS_AND_GROIN,
                titleHe = "מעגלי אגן בעמידה",
                titleEn = "Standing hip circles",
                instructionsHe =
                    "עמדו בפישוק ברוחב האגן והניחו את הידיים על האגן. בצעו מעגלים קטנים ואיטיים בכיוון אחד ולאחר מכן החליפו כיוון.",
                instructionsEn =
                    "Stand with your feet hip-width apart and place your hands on your hips. Make small, slow circles in one direction, then reverse direction.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = false,
                imageKey = "hips_groin_standing_hip_circles_start",
                safetyNoteHe =
                    "שמרו על מעגלים קטנים והיעזרו במשטח יציב אם יש קושי בשיווי המשקל.",
                safetyNoteEn =
                    "Keep the circles small and use stable support if balance is difficult.",
                sortOrder = 10,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "hips_groin_standing_hip_circles_start",
                            instructionHe = "עמדו זקוף.",
                            instructionEn = "Stand upright.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_standing_hip_circles_left",
                            instructionHe = "הניעו את האגן במעגל.",
                            instructionEn = "Move your hips in a circle.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "hips_groin_standing_hip_circles_right",
                            instructionHe = "המשיכו לצד השני.",
                            instructionEn = "Continue to the other side.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        )
                    )
            )
        )
}
