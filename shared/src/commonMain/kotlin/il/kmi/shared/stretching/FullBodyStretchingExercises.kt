package il.kmi.shared.stretching

object FullBodyStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "full_body_standing_reach",
                category = StretchingCategory.FULL_BODY,
                titleHe = "התארכות מלאה בעמידה",
                titleEn = "Standing full-body reach",
                instructionsHe =
                    "עמדו זקוף כשהרגליים ברוחב האגן. " +
                            "שלבו את האצבעות והרימו את הידיים מעל הראש. " +
                            "התארכו בעדינות כלפי מעלה מבלי להרים את הכתפיים. " +
                            "הורידו את הידיים באיטיות.",
                instructionsEn =
                    "Stand upright with your feet hip-width apart. " +
                            "Interlace your fingers and raise your arms overhead. " +
                            "Gently lengthen upward without shrugging your shoulders. " +
                            "Slowly lower your arms.",
                durationSeconds = 20,
                repetitions = 3,
                performBothSides = false,
                imageKey = "full_body_standing_reach_start",
                safetyNoteHe =
                    "אין לקשת את הגב או להרים את הידיים מעבר לטווח נוח.",
                safetyNoteEn =
                    "Do not arch your back or raise your arms beyond a comfortable range.",
                sortOrder = 0,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_standing_reach_start",
                            instructionHe = "עמדו זקוף והרפו את הכתפיים.",
                            instructionEn = "Stand upright and relax your shoulders.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_standing_reach_up",
                            instructionHe = "הרימו את הידיים והתארכו בעדינות כלפי מעלה.",
                            instructionEn = "Raise your arms and gently lengthen upward.",
                            durationSeconds = 8,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_standing_reach_release",
                            instructionHe = "הורידו את הידיים באיטיות.",
                            instructionEn = "Slowly lower your arms.",
                            durationSeconds = 3,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_standing_side_reach",
                category = StretchingCategory.FULL_BODY,
                titleHe = "התארכות צדית בעמידה",
                titleEn = "Standing side reach",
                instructionsHe =
                    "עמדו בפיסוק נוח והרימו את הידיים מעל הראש. " +
                            "אחזו בשורש כף יד אחת והטו את הגוף בעדינות לצד הנגדי. " +
                            "חזרו למרכז ובצעו את המתיחה לצד השני.",
                instructionsEn =
                    "Stand with your feet comfortably apart and raise your arms overhead. " +
                            "Hold one wrist and gently lean toward the opposite side. " +
                            "Return to the center and repeat on the other side.",
                durationSeconds = 20,
                repetitions = 2,
                performBothSides = true,
                imageKey = "full_body_side_reach_start",
                safetyNoteHe =
                    "שמרו את הגוף פונה קדימה ואל תסובבו או תקשתו את הגב.",
                safetyNoteEn =
                    "Keep your body facing forward and avoid twisting or arching your back.",
                sortOrder = 1,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_side_reach_start",
                            instructionHe = "עמדו זקוף והרימו את הידיים.",
                            instructionEn = "Stand upright and raise your arms.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_side_reach_left",
                            instructionHe = "הטו את הגוף בעדינות שמאלה.",
                            instructionEn = "Gently lean your body to the left.",
                            durationSeconds = 10,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_side_reach_right",
                            instructionHe = "חזרו למרכז והטו את הגוף ימינה.",
                            instructionEn = "Return to the center and lean to the right.",
                            durationSeconds = 10,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_standing_rotation",
                category = StretchingCategory.FULL_BODY,
                titleHe = "סיבוב גוף מבוקר בעמידה",
                titleEn = "Controlled standing rotation",
                instructionsHe =
                    "עמדו בפיסוק נוח וכופפו מעט את הברכיים. " +
                            "הרימו את הידיים לגובה החזה. " +
                            "סובבו באיטיות את בית החזה לצד אחד וחזרו למרכז. " +
                            "בצעו את התנועה לצד השני.",
                instructionsEn =
                    "Stand with your feet comfortably apart and slightly bend your knees. " +
                            "Raise your arms to chest height. " +
                            "Slowly rotate your chest to one side and return to the center. " +
                            "Repeat on the other side.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = true,
                imageKey = "full_body_standing_rotation_start",
                safetyNoteHe =
                    "שמרו את האגן פונה קדימה ובצעו סיבוב קטן וללא תנופה.",
                safetyNoteEn =
                    "Keep your hips facing forward and use a small movement without momentum.",
                sortOrder = 2,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_standing_rotation_start",
                            instructionHe = "עמדו יציב והרימו את הידיים לגובה החזה.",
                            instructionEn = "Stand steadily and raise your arms to chest height.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_standing_rotation_left",
                            instructionHe = "סובבו את בית החזה באיטיות שמאלה.",
                            instructionEn = "Slowly rotate your chest to the left.",
                            durationSeconds = 4,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_standing_rotation_right",
                            instructionHe = "עברו דרך המרכז וסובבו ימינה.",
                            instructionEn = "Move through the center and rotate to the right.",
                            durationSeconds = 4,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_squat_reach",
                category = StretchingCategory.FULL_BODY,
                titleHe = "כפיפה והתארכות כלפי מעלה",
                titleEn = "Squat and overhead reach",
                instructionsHe =
                    "עמדו כשהרגליים מעט רחבות מרוחב האגן. " +
                            "שלחו את האגן לאחור וכופפו מעט את הברכיים. " +
                            "חזרו לעמידה והרימו את הידיים מעל הראש. " +
                            "המשיכו בתנועה איטית ומבוקרת.",
                instructionsEn =
                    "Stand with your feet slightly wider than hip-width apart. " +
                            "Move your hips backward and slightly bend your knees. " +
                            "Return to standing and raise your arms overhead. " +
                            "Continue with a slow, controlled movement.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = false,
                imageKey = "full_body_squat_reach_start",
                safetyNoteHe =
                    "אין לרדת עמוק. שמרו את הברכיים בקו כפות הרגליים ואת הגב ארוך.",
                safetyNoteEn =
                    "Do not squat deeply. Keep your knees aligned with your feet and your back long.",
                sortOrder = 3,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_squat_reach_start",
                            instructionHe = "עמדו בפיסוק נוח כשהידיים לצד הגוף.",
                            instructionEn = "Stand comfortably with your arms by your sides.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_squat_reach_down",
                            instructionHe = "שלחו את האגן לאחור וכופפו מעט את הברכיים.",
                            instructionEn = "Move your hips backward and slightly bend your knees.",
                            durationSeconds = 3,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_squat_reach_up",
                            instructionHe = "חזרו לעמידה והרימו את הידיים.",
                            instructionEn = "Return to standing and raise your arms.",
                            durationSeconds = 3,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_reverse_lunge_reach",
                category = StretchingCategory.FULL_BODY,
                titleHe = "פסיעה לאחור עם הרמת ידיים",
                titleEn = "Reverse step with overhead reach",
                instructionsHe =
                    "עמדו זקוף והחזיקו בכיסא יציב במידת הצורך. " +
                            "קחו צעד קטן לאחור ברגל אחת והרימו את הידיים לטווח נוח. " +
                            "חזרו לעמדת ההתחלה ובצעו בצד השני.",
                instructionsEn =
                    "Stand upright and hold a stable chair if needed. " +
                            "Take a small step backward with one leg and raise your arms to a comfortable height. " +
                            "Return to the starting position and repeat on the other side.",
                durationSeconds = 30,
                repetitions = 6,
                performBothSides = true,
                imageKey = "full_body_reverse_lunge_reach_start",
                safetyNoteHe =
                    "בצעו פסיעה קטנה בלבד ושמרו את הברך הקדמית בקו כף הרגל.",
                safetyNoteEn =
                    "Use only a small step and keep the front knee aligned with the foot.",
                sortOrder = 4,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_reverse_lunge_reach_start",
                            instructionHe = "עמדו זקוף בפיסוק יציב.",
                            instructionEn = "Stand upright in a stable position.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_reverse_lunge_reach_left",
                            instructionHe = "קחו צעד קטן לאחור ברגל שמאל והרימו את הידיים.",
                            instructionEn = "Step your left leg back and raise your arms.",
                            durationSeconds = 5,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_reverse_lunge_reach_right",
                            instructionHe = "חזרו למרכז ובצעו ברגל ימין.",
                            instructionEn = "Return to the center and repeat with your right leg.",
                            durationSeconds = 5,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_cat_cow",
                category = StretchingCategory.FULL_BODY,
                titleHe = "קימור ושקיעה על שש",
                titleEn = "Cat-cow stretch",
                instructionsHe =
                    "עברו לעמידת שש כשהידיים מתחת לכתפיים והברכיים מתחת לאגן. " +
                            "עגלו את הגב בעדינות והורידו את הראש. " +
                            "לאחר מכן האריכו את הגב והרימו מעט את בית החזה. " +
                            "עברו בין התנוחות באיטיות.",
                instructionsEn =
                    "Move onto all fours with your hands below your shoulders and knees below your hips. " +
                            "Gently round your back and lower your head. " +
                            "Then lengthen your spine and slightly lift your chest. " +
                            "Move slowly between the positions.",
                durationSeconds = 30,
                repetitions = 8,
                performBothSides = false,
                imageKey = "full_body_cat_cow_start",
                safetyNoteHe =
                    "אין להשליך את הראש לאחור או ליצור קשת חדה בגב התחתון.",
                safetyNoteEn =
                    "Do not throw your head backward or create a sharp arch in your lower back.",
                sortOrder = 5,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_cat_cow_start",
                            instructionHe = "עברו לעמידת שש ושמרו על גב ניטרלי.",
                            instructionEn = "Move onto all fours and keep your spine neutral.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_cat_cow_round",
                            instructionHe = "עגלו את הגב בעדינות.",
                            instructionEn = "Gently round your back.",
                            durationSeconds = 3,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_cat_cow_extend",
                            instructionHe = "האריכו את הגב והרימו מעט את בית החזה.",
                            instructionEn = "Lengthen your spine and slightly lift your chest.",
                            durationSeconds = 3,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_child_pose_reach",
                category = StretchingCategory.FULL_BODY,
                titleHe = "תנוחת ילד עם התארכות",
                titleEn = "Child's pose reach",
                instructionsHe =
                    "עברו לעמידת שש והעבירו את האגן בעדינות לאחור לכיוון העקבים. " +
                            "השאירו את הידיים ארוכות לפנים והניחו את הראש בטווח נוח. " +
                            "נשמו רגיל וחזרו באיטיות לעמידת שש.",
                instructionsEn =
                    "Begin on all fours and gently move your hips backward toward your heels. " +
                            "Keep your arms extended forward and rest your head comfortably. " +
                            "Breathe normally, then slowly return to all fours.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = false,
                imageKey = "full_body_child_pose_start",
                safetyNoteHe =
                    "אין לשבת בכוח על העקבים. עצרו אם מופיע לחץ בברכיים או בכתפיים.",
                safetyNoteEn =
                    "Do not force your hips onto your heels. Stop if you feel pressure in the knees or shoulders.",
                sortOrder = 6,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_child_pose_start",
                            instructionHe = "התחילו בעמידת שש.",
                            instructionEn = "Begin on all fours.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_child_pose_reach",
                            instructionHe = "העבירו את האגן לאחור והאריכו את הידיים.",
                            instructionEn = "Move your hips backward and extend your arms.",
                            durationSeconds = 20,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_child_pose_return",
                            instructionHe = "חזרו באיטיות לעמידת שש.",
                            instructionEn = "Slowly return to all fours.",
                            durationSeconds = 3,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_thread_the_needle",
                category = StretchingCategory.FULL_BODY,
                titleHe = "השחלת יד מתחת לגוף",
                titleEn = "Thread the needle",
                instructionsHe =
                    "עברו לעמידת שש. " +
                            "החליקו יד אחת מתחת ליד השנייה וסובבו בעדינות את בית החזה. " +
                            "חזרו למרכז ובצעו את התנועה בצד השני.",
                instructionsEn =
                    "Begin on all fours. " +
                            "Slide one arm underneath the opposite arm and gently rotate your chest. " +
                            "Return to the center and repeat on the other side.",
                durationSeconds = 20,
                repetitions = 3,
                performBothSides = true,
                imageKey = "full_body_thread_needle_start",
                safetyNoteHe =
                    "אין להעמיס את משקל הגוף על הראש או למשוך את הכתף בכוח.",
                safetyNoteEn =
                    "Do not place your body weight on your head or force the shoulder.",
                sortOrder = 7,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_thread_needle_start",
                            instructionHe = "עברו לעמידת שש יציבה.",
                            instructionEn = "Move into a stable all-fours position.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_thread_needle_left",
                            instructionHe = "החליקו את יד שמאל מתחת לגוף.",
                            instructionEn = "Slide your left arm underneath your body.",
                            durationSeconds = 10,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_thread_needle_right",
                            instructionHe = "חזרו למרכז ובצעו ביד ימין.",
                            instructionEn = "Return to the center and repeat with your right arm.",
                            durationSeconds = 10,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_supine_lengthening",
                category = StretchingCategory.FULL_BODY,
                titleHe = "התארכות מלאה בשכיבה",
                titleEn = "Supine full-body lengthening",
                instructionsHe =
                    "שכבו על הגב כשהרגליים ישרות. " +
                            "הושיטו את הידיים מעבר לראש ואת העקבים לכיוון הנגדי. " +
                            "התארכו בעדינות, נשמו רגיל ושחררו.",
                instructionsEn =
                    "Lie on your back with your legs straight. " +
                            "Reach your arms overhead and your heels in the opposite direction. " +
                            "Gently lengthen, breathe normally, and release.",
                durationSeconds = 20,
                repetitions = 3,
                performBothSides = false,
                imageKey = "full_body_supine_lengthening_start",
                safetyNoteHe =
                    "שמרו את הגב בטווח נוח ואל תכריחו את הידיים להגיע לרצפה.",
                safetyNoteEn =
                    "Keep your back comfortable and do not force your arms to reach the floor.",
                sortOrder = 8,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_supine_lengthening_start",
                            instructionHe = "שכבו על הגב והרפו את הגוף.",
                            instructionEn = "Lie on your back and relax your body.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_supine_lengthening_reach",
                            instructionHe = "הושיטו את הידיים והעקבים לכיוונים מנוגדים.",
                            instructionEn = "Reach your arms and heels in opposite directions.",
                            durationSeconds = 12,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_supine_lengthening_release",
                            instructionHe = "שחררו את הידיים והרגליים.",
                            instructionEn = "Release your arms and legs.",
                            durationSeconds = 3,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "full_body_marching_reach",
                category = StretchingCategory.FULL_BODY,
                titleHe = "צעידה במקום עם הושטת ידיים",
                titleEn = "Marching with arm reach",
                instructionsHe =
                    "עמדו זקוף בפיסוק נוח. " +
                            "הרימו ברך אחת ובמקביל הושיטו את הידיים כלפי מעלה בטווח נוח. " +
                            "הורידו את הרגל והידיים והחליפו צד. " +
                            "המשיכו בקצב איטי ומבוקר.",
                instructionsEn =
                    "Stand upright with your feet comfortably apart. " +
                            "Lift one knee while reaching your arms upward through a comfortable range. " +
                            "Lower the leg and arms, then change sides. " +
                            "Continue at a slow, controlled pace.",
                durationSeconds = 40,
                repetitions = 10,
                performBothSides = true,
                imageKey = "full_body_marching_reach_start",
                safetyNoteHe =
                    "הרימו את הברך רק לטווח נוח והיעזרו בכיסא אם שיווי המשקל אינו יציב.",
                safetyNoteEn =
                    "Lift the knee only to a comfortable height and use a chair if your balance is unsteady.",
                sortOrder = 9,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "full_body_marching_reach_start",
                            instructionHe = "עמדו זקוף והרפו את הידיים.",
                            instructionEn = "Stand upright and relax your arms.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_marching_reach_left",
                            instructionHe = "הרימו את ברך שמאל והושיטו את הידיים.",
                            instructionEn = "Lift your left knee and reach your arms upward.",
                            durationSeconds = 4,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "full_body_marching_reach_right",
                            instructionHe = "הורידו והחליפו לצד ימין.",
                            instructionEn = "Lower and change to the right side.",
                            durationSeconds = 4,
                            type = StretchingStepType.MOVE
                        )
                    )
            )
        )
}