package il.kmi.shared.stretching

internal object UpperBackStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "upper_back_forward_reach",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "הושטת ידיים לפנים",
                titleEn = "Forward upper-back reach",
                instructionsHe =
                    "שבו או עמדו בגב זקוף. שלבו את האצבעות והושיטו את הידיים לפנים. " +
                            "דחפו בעדינות את כפות הידיים קדימה ועגלו מעט את הגב העליון. " +
                            "שמרו את הכתפיים נמוכות והחזיקו במתיחה.",
                instructionsEn =
                    "Sit or stand upright. Interlace your fingers and extend your arms forward. " +
                            "Gently press your hands forward and slightly round your upper back. " +
                            "Keep your shoulders lowered and hold the stretch.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "upper_back_forward_reach_start",
                safetyNoteHe =
                    "אין למשוך בכוח או להרים את הכתפיים לכיוון האוזניים.",
                safetyNoteEn =
                    "Do not force the stretch or raise your shoulders toward your ears.",
                sortOrder = 1,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_forward_reach_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_forward_reach_stretch",
                            instructionHe = "הושיטו קדימה.",
                            instructionEn = "Reach forward.",
                            durationSeconds = 8,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_self_hug",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "חיבוק עצמי לגב העליון",
                titleEn = "Upper-back self-hug",
                instructionsHe =
                    "שבו או עמדו בגב זקוף. העבירו כל יד אל הכתף הנגדית כאילו אתם מחבקים את עצמכם. " +
                            "משכו בעדינות את השכמות הרחק זו מזו ועגלו מעט את הגב העליון. " +
                            "החזיקו במתיחה ונשמו כרגיל.",
                instructionsEn =
                    "Sit or stand upright. Place each hand on the opposite shoulder as if hugging yourself. " +
                            "Gently separate your shoulder blades and slightly round your upper back. " +
                            "Hold the stretch and breathe normally.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "upper_back_self_hug_start",
                safetyNoteHe =
                    "שמרו את הצוואר משוחרר ואל תלחצו את הידיים בכוח אל הגוף.",
                safetyNoteEn =
                    "Keep your neck relaxed and do not force your arms against your body.",
                sortOrder = 2,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_self_hug_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_self_hug_hold",
                            instructionHe = "חבקו והחזיקו.",
                            instructionEn = "Hug and hold.",
                            durationSeconds = 8,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_chair_extension",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "פתיחת גב עליון מעל משענת",
                titleEn = "Upper-back extension over a chair",
                instructionsHe =
                    "שבו על כיסא יציב כאשר כפות הרגליים מונחות על הרצפה. " +
                            "הניחו את הידיים מאחורי הראש והישענו בעדינות לאחור מעל המשענת. " +
                            "פתחו את בית החזה וחזרו באיטיות לתנוחת ההתחלה.",
                instructionsEn =
                    "Sit on a stable chair with both feet on the floor. " +
                            "Place your hands behind your head and gently lean backward over the backrest. " +
                            "Open your chest and slowly return to the starting position.",
                durationSeconds = 25,
                repetitions = 4,
                performBothSides = false,
                imageKey = "upper_back_chair_extension_start",
                safetyNoteHe =
                    "השתמשו בכיסא יציב ונמוך. אין להישען במהירות או להגיע לכאב בגב או בצוואר.",
                safetyNoteEn =
                    "Use a stable chair with a suitable backrest. Do not lean rapidly or move into back or neck pain.",
                sortOrder = 3,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_chair_extension_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_chair_extension_stretch",
                            instructionHe = "הישענו בעדינות.",
                            instructionEn = "Lean back gently.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_seated_rotation",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "סיבוב גב עליון בישיבה",
                titleEn = "Seated upper-back rotation",
                instructionsHe =
                    "שבו זקוף כאשר כפות הרגליים מונחות על הרצפה. שלבו את הידיים על החזה וסובבו באיטיות את פלג הגוף העליון לצד אחד. חזרו למרכז ובצעו לצד השני.",
                instructionsEn =
                    "Sit upright with both feet on the floor. Cross your arms over your chest and slowly rotate your upper body to one side. Return to the center and repeat in the other direction.",
                durationSeconds = 30,
                repetitions = 5,
                performBothSides = true,
                imageKey = "upper_back_seated_rotation_start",
                safetyNoteHe =
                    "האגן נשאר מול החזית. אין לבצע תנופה או לסובב דרך כאב.",
                safetyNoteEn =
                    "Keep the pelvis facing forward. Do not use momentum or rotate through pain.",
                sortOrder = 4,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_seated_rotation_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_seated_rotation_left",
                            instructionHe = "הסתובבו שמאלה.",
                            instructionEn = "Rotate left.",
                            durationSeconds = 5,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_seated_rotation_right",
                            instructionHe = "הסתובבו ימינה.",
                            instructionEn = "Rotate right.",
                            durationSeconds = 5,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_cat_cow",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "קימור ויישור הגב",
                titleEn = "Cat-cow movement",
                instructionsHe =
                    "עברו לעמידת שש כאשר כפות הידיים מתחת לכתפיים והברכיים מתחת לאגן. עגלו את הגב באיטיות והורידו את הראש. לאחר מכן פתחו בעדינות את בית החזה וחזרו למנח ניטרלי.",
                instructionsEn =
                    "Begin on your hands and knees with hands under shoulders and knees under hips. Slowly round your back and lower your head. Then gently open the chest and return toward a neutral spine.",
                durationSeconds = 40,
                repetitions = 8,
                performBothSides = false,
                imageKey = "upper_back_cat_cow_start",
                safetyNoteHe =
                    "בצעו תנועה קטנה ונוחה והימנעו מהשלכת הראש לאחור.",
                safetyNoteEn =
                    "Keep the movement small and comfortable, and avoid dropping the head backward.",
                sortOrder = 5,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_cat_cow_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_cat_cow_round",
                            instructionHe = "עגלו את הגב.",
                            instructionEn = "Round your back.",
                            durationSeconds = 4,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_cat_cow_open",
                            instructionHe = "פתחו בעדינות.",
                            instructionEn = "Open gently.",
                            durationSeconds = 4,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_thread_needle",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "השחלת יד מתחת לגוף",
                titleEn = "Thread the needle",
                instructionsHe =
                    "בעמידת שש החליקו יד אחת מתחת ליד השנייה כאשר כף היד פונה כלפי מעלה. אפשרו לכתף ולצד הראש להתקרב בעדינות למשטח. חזרו באיטיות והחליפו צד.",
                instructionsEn =
                    "From hands and knees, slide one arm underneath the other with the palm facing upward. Allow the shoulder and side of the head to move gently toward the floor. Return slowly and change sides.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "upper_back_thread_needle_start",
                safetyNoteHe =
                    "אין להעמיס משקל על הראש או להמשיך אם מופיע כאב בכתף.",
                safetyNoteEn =
                    "Do not place body weight on the head or continue if shoulder pain develops.",
                sortOrder = 6,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_thread_needle_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_thread_needle_left",
                            instructionHe = "השחילו יד שמאל.",
                            instructionEn = "Thread your left arm.",
                            durationSeconds = 7,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_thread_needle_right",
                            instructionHe = "השחילו יד ימין.",
                            instructionEn = "Thread your right arm.",
                            durationSeconds = 7,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_child_pose",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "ישיבת עקבים עם ידיים לפנים",
                titleEn = "Extended child’s pose",
                instructionsHe =
                    "מעמידת שש הזיזו את האגן לאחור לכיוון העקבים והושיטו את הידיים לפנים. הורידו את החזה רק עד לטווח נוח ונשמו באיטיות.",
                instructionsEn =
                    "From hands and knees, move your hips back toward your heels and reach your arms forward. Lower your chest only as far as comfortable and breathe slowly.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "upper_back_child_pose_start",
                safetyNoteHe =
                    "אם קיימת אי־נוחות בברכיים, הניחו כרית או דלגו על התרגיל.",
                safetyNoteEn =
                    "Use cushioning or skip this exercise if it causes knee discomfort.",
                sortOrder = 7,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_child_pose_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_child_pose_stretch",
                            instructionHe = "הישענו לאחור.",
                            instructionEn = "Move gently backward.",
                            durationSeconds = 8,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_wall_lat",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "מתיחת גב עליון מול קיר",
                titleEn = "Wall upper-back stretch",
                instructionsHe =
                    "עמדו מול קיר והניחו עליו את כפות הידיים. צעדו מעט לאחור והטו את הגוף מהאגן עד שהגב כמעט מקביל לרצפה. שמרו על זרועות ארוכות והורידו את החזה בעדינות.",
                instructionsEn =
                    "Stand facing a wall and place both hands against it. Step back and hinge at the hips until your back is nearly parallel to the floor. Keep the arms long and gently lower the chest.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "upper_back_wall_lat_start",
                safetyNoteHe =
                    "שמרו על ברכיים מעט כפופות ואל תדחפו את הכתפיים מעבר לטווח נוח.",
                safetyNoteEn =
                    "Keep the knees slightly bent and do not force the shoulders beyond a comfortable range.",
                sortOrder = 8,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_wall_lat_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_wall_lat_stretch",
                            instructionHe = "הורידו את החזה.",
                            instructionEn = "Lower your chest.",
                            durationSeconds = 8,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_side_reach",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "הושטת יד אלכסונית בישיבה",
                titleEn = "Seated diagonal reach",
                instructionsHe =
                    "שבו זקוף והניחו יד אחת על הירך. הושיטו את היד השנייה באלכסון לפנים ולמעלה תוך הטיה קלה של פלג הגוף. חזרו למרכז והחליפו צד.",
                instructionsEn =
                    "Sit upright with one hand resting on your thigh. Reach the other arm diagonally forward and upward while gently leaning your upper body. Return to the center and change sides.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "upper_back_side_reach_start",
                safetyNoteHe =
                    "שתי עצמות הישיבה נשארות על הכיסא ואין להטות את הגוף בכוח.",
                safetyNoteEn =
                    "Keep both sitting bones on the chair and avoid forcing the side bend.",
                sortOrder = 9,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_side_reach_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_side_reach_left",
                            instructionHe = "הושיטו שמאלה.",
                            instructionEn = "Reach to the left.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_side_reach_right",
                            instructionHe = "הושיטו ימינה.",
                            instructionEn = "Reach to the right.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "upper_back_scapular_glide",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "החלקת שכמות לפנים ולאחור",
                titleEn = "Shoulder blade glide",
                instructionsHe =
                    "שבו או עמדו זקוף כשהידיים מושטות לפנים. הרחיקו את השכמות זו מזו בלי להרים את הכתפיים. לאחר מכן משכו בעדינות את השכמות לאחור וחזרו על התנועה.",
                instructionsEn =
                    "Sit or stand upright with your arms reaching forward. Move the shoulder blades apart without shrugging. Then gently draw them backward and repeat the movement.",
                durationSeconds = 35,
                repetitions = 10,
                performBothSides = false,
                imageKey = "upper_back_scapular_glide_start",
                safetyNoteHe =
                    "שמרו על תנועה איטית ואל תכווצו את הכתפיים לכיוון האוזניים.",
                safetyNoteEn =
                    "Move slowly and avoid shrugging the shoulders toward the ears.",
                sortOrder = 10,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_scapular_glide_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_scapular_glide_forward",
                            instructionHe = "הרחיקו שכמות.",
                            instructionEn = "Spread your shoulder blades.",
                            durationSeconds = 4,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_scapular_glide_back",
                            instructionHe = "קרבו שכמות.",
                            instructionEn = "Draw your shoulder blades together.",
                            durationSeconds = 4,
                            type = StretchingStepType.RELEASE
                        )
                    )
            )
        )
}