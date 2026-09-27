package il.kmi.shared.stretching

internal object ShouldersAndArmsStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "shoulders_cross_body",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת כתף לרוחב הגוף",
                titleEn = "Cross-body shoulder stretch",
                instructionsHe =
                    "עמדו בגב זקוף והכתפיים משוחררות. " +
                            "העבירו יד אחת ישרה לרוחב החזה. " +
                            "תמכו בזרוע מעל המרפק בעזרת היד השנייה ומשכו בעדינות לכיוון הגוף. " +
                            "שמרו את פלג הגוף העליון פונה קדימה וחזרו בצד השני.",
                instructionsEn =
                    "Stand upright with your shoulders relaxed. " +
                            "Bring one straight arm across your chest. " +
                            "Support the upper arm above the elbow with the opposite hand and gently draw it toward your body. " +
                            "Keep your torso facing forward and repeat on the other side.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "shoulders_cross_body_start",
                safetyNoteHe =
                    "אין למשוך דרך המרפק או להפעיל לחץ חד על מפרק הכתף.",
                safetyNoteEn =
                    "Do not pull directly on the elbow or force the shoulder joint.",
                sortOrder = 1,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "shoulders_cross_body_start",
                            instructionHe = "מוכנים.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_cross_body_right",
                            instructionHe = "מתחו את כתף ימין.",
                            instructionEn = "Stretch the right shoulder.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_cross_body_left",
                            instructionHe = "מתחו את כתף שמאל.",
                            instructionEn = "Stretch the left shoulder.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "shoulders_overhead_triceps",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת זרוע מעל הראש",
                titleEn = "Overhead triceps stretch",
                instructionsHe =
                    "עמדו בגב זקוף והכתפיים משוחררות. " +
                            "הרימו יד אחת מעל הראש וכופפו את המרפק כך שכף היד תרד לכיוון הגב העליון. " +
                            "הניחו את היד השנייה על המרפק והפעילו לחץ עדין כלפי מטה. " +
                            "שמרו את הראש זקוף ואת הגב ישר וחזרו בצד השני.",
                instructionsEn =
                    "Stand upright with your shoulders relaxed. " +
                            "Raise one arm overhead and bend the elbow so the hand moves toward the upper back. " +
                            "Place the opposite hand on the elbow and apply gentle downward pressure. " +
                            "Keep your head upright and your back straight, then repeat on the other side.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "shoulders_cross_body_start",
                safetyNoteHe =
                    "אין לדחוף את המרפק בכוח או לקשת את הגב.",
                safetyNoteEn =
                    "Do not force the elbow downward or arch your back.",
                sortOrder = 2,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "shoulders_cross_body_start",
                            instructionHe = "מוכנים.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_overhead_triceps_right",
                            instructionHe = "מתחו את זרוע ימין.",
                            instructionEn = "Stretch the right arm.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_overhead_triceps_left",
                            instructionHe = "מתחו את זרוע שמאל.",
                            instructionEn = "Stretch the left arm.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "shoulders_doorway_chest",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת חזה וכתפיים במשקוף",
                titleEn = "Doorway chest and shoulder stretch",
                instructionsHe =
                    "עמדו במרכז המשקוף והניחו את שתי האמות על צדי המשקוף, " +
                            "כאשר המרפקים כפופים ובגובה הכתפיים. " +
                            "צעדו מעט קדימה והעבירו את בית החזה בעדינות דרך המשקוף. " +
                            "שמרו את הכתפיים נמוכות ואת הגב במנח ניטרלי.",
                instructionsEn =
                    "Stand in the center of the doorway and place both forearms against the frame, " +
                            "with your elbows bent at shoulder height. " +
                            "Take a small step forward and gently move your chest through the doorway. " +
                            "Keep your shoulders lowered and your spine neutral.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "shoulders_doorway_chest_start",
                safetyNoteHe =
                    "התקדמו מעט בלבד. אין להקשית את הגב או לדחוף את הכתפיים מעבר לטווח הנוח.",
                safetyNoteEn =
                    "Move forward only slightly. Do not arch your back or force your shoulders beyond a comfortable range.",
                sortOrder = 3,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "shoulders_doorway_chest_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_doorway_chest_stretch",
                            instructionHe = "התקדמו בעדינות.",
                            instructionEn = "Move forward gently.",
                            durationSeconds = 12,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "shoulders_pendulum",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "תנועת מטוטלת לכתף",
                titleEn = "Shoulder pendulum",
                instructionsHe =
                    "הישענו ביד אחת על משטח יציב והטו מעט את הגוף קדימה. " +
                            "אפשרו ליד השנייה להשתחרר כלפי מטה. " +
                            "הניעו את הגוף בעדינות כדי לאפשר ליד לנוע קדימה ואחורה כמו מטוטלת. " +
                            "החליפו צד.",
                instructionsEn =
                    "Support yourself with one hand on a stable surface and lean slightly forward. " +
                            "Allow the other arm to hang loosely. " +
                            "Gently move your body so the relaxed arm swings forward and backward like a pendulum. " +
                            "Change sides.",
                durationSeconds = 40,
                repetitions = null,
                performBothSides = true,
                imageKey = "shoulders_pendulum_start",
                safetyNoteHe =
                    "שמרו את הזרוע רפויה. התנועה צריכה להגיע מהנעה עדינה של הגוף ולא מהפעלת כוח בכתף.",
                safetyNoteEn =
                    "Keep the arm relaxed. The movement should come from gently shifting your body, not from forcing the shoulder.",
                sortOrder = 4,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "shoulders_pendulum_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_pendulum_forward",
                            instructionHe = "קדימה.",
                            instructionEn = "Forward.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_pendulum_backward",
                            instructionHe = "אחורה.",
                            instructionEn = "Backward.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "shoulders_wall_walk",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "טיפוס אצבעות על הקיר",
                titleEn = "Finger walk up the wall",
                instructionsHe =
                    "עמדו לצד הקיר והניחו עליו את קצות האצבעות של היד הקרובה. " +
                            "טפסו בהדרגה עם האצבעות כלפי מעלה עד לגובה נוח. " +
                            "עצרו לזמן קצר והורידו את היד באיטיות. " +
                            "חזרו ביד השנייה.",
                instructionsEn =
                    "Stand beside the wall and place the fingertips of the nearest hand against it. " +
                            "Slowly walk your fingers upward to a comfortable height. " +
                            "Pause briefly, then lower the arm slowly. " +
                            "Repeat with the other arm.",
                durationSeconds = 40,
                repetitions = 5,
                performBothSides = true,
                imageKey = "shoulders_wall_walk_start",
                safetyNoteHe =
                    "התקדמו רק עד לטווח נוח. שמרו את הכתף נמוכה ואל תטו את הגוף הצידה.",
                safetyNoteEn =
                    "Move only within a comfortable range. Keep your shoulder lowered and do not lean sideways.",
                sortOrder = 5,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "shoulders_wall_walk_start",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_wall_walk_middle",
                            instructionHe = "טפסו לאט.",
                            instructionEn = "Climb slowly.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_wall_walk_top",
                            instructionHe = "עצרו בגובה נוח.",
                            instructionEn = "Pause at a comfortable height.",
                            durationSeconds = 3,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),
            StretchingExercise(
                id = "shoulders_arm_circles",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מעגלי ידיים מבוקרים",
                titleEn = "Controlled arm circles",
                instructionsHe =
                    "עמדו זקוף והרימו את הידיים לצדדים עד לגובה נוח. " +
                            "בצעו מעגלים קטנים ואיטיים לפנים. " +
                            "לאחר מכן החליפו כיוון. " +
                            "שמרו את הכתפיים נמוכות ומשוחררות.",
                instructionsEn =
                    "Stand upright and raise your arms out to the sides to a comfortable height. " +
                            "Make small, slow circles forward. " +
                            "Then reverse direction. " +
                            "Keep your shoulders lowered and relaxed.",
                durationSeconds = 30,
                repetitions = null,
                performBothSides = false,
                imageKey = "shoulders_arm_circles_center",
                safetyNoteHe =
                    "התחילו במעגלים קטנים. אין לבצע תנועות מהירות או להמשיך בטווח שגורם לכאב.",
                safetyNoteEn =
                    "Begin with small circles. Avoid fast movements or any range that causes pain.",
                sortOrder = 6,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "shoulders_arm_circles_center",
                            instructionHe = "היכונו.",
                            instructionEn = "Get ready.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_arm_circles_up",
                            instructionHe = "מעגלים קטנים.",
                            instructionEn = "Small circles.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulders_arm_circles_down",
                            instructionHe = "המשיכו לאט.",
                            instructionEn = "Continue slowly.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "arms_biceps_wall",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת קדמת הזרוע ליד קיר",
                titleEn = "Wall biceps stretch",
                instructionsHe =
                    "עמדו לצד הקיר והניחו עליו את כף היד כשהזרוע ישרה ובגובה נוח. " +
                            "סובבו את הגוף באיטיות הרחק מהקיר עד שתרגישו מתיחה עדינה בקדמת הזרוע והכתף. " +
                            "שמרו את הכתף נמוכה וחזרו בצד השני.",
                instructionsEn =
                    "Stand beside a wall and place your palm against it with your arm straight at a comfortable height. " +
                            "Slowly rotate your body away from the wall until you feel a gentle stretch along the front of your arm and shoulder. " +
                            "Keep your shoulder lowered and repeat on the other side.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "arms_biceps_wall_left",
                safetyNoteHe =
                    "שמרו על מרפק משוחרר מעט. אין לסובב את הגוף מעבר לטווח הנוח.",
                safetyNoteEn =
                    "Keep the elbow slightly relaxed. Do not rotate beyond a comfortable range.",
                sortOrder = 7,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "arms_biceps_wall_left",
                            instructionHe = "מתחו בעדינות.",
                            instructionEn = "Stretch gently.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "arms_biceps_wall_right",
                            instructionHe = "החליפו צד.",
                            instructionEn = "Change sides.",
                            durationSeconds = 6,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "arms_wrist_flexor",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת פנים האמה",
                titleEn = "Wrist flexor stretch",
                instructionsHe =
                    "הושיטו יד לפנים כשהמרפק ישר אך משוחרר מעט וכף היד פונה כלפי מעלה. " +
                            "בעזרת היד השנייה משכו בעדינות את האצבעות כלפי מטה ולאחור, " +
                            "עד שתרגישו מתיחה בפנים האמה. " +
                            "החליפו צד.",
                instructionsEn =
                    "Extend one arm forward with the elbow straight but slightly relaxed and the palm facing upward. " +
                            "With the opposite hand, gently draw the fingers downward and back " +
                            "until you feel a stretch along the inner forearm. " +
                            "Change sides.",
                durationSeconds = 20,
                repetitions = 2,
                performBothSides = true,
                imageKey = "arms_wrist_flexor_right",
                safetyNoteHe =
                    "הפעילו משיכה עדינה בלבד. אין להפעיל לחץ חזק על שורש כף היד או על מפרקי האצבעות.",
                safetyNoteEn =
                    "Use only gentle pressure. Do not force the wrist or finger joints.",
                sortOrder = 8,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "arms_wrist_flexor_right",
                            instructionHe = "מתחו בעדינות.",
                            instructionEn = "Stretch gently.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "arms_wrist_flexor_left",
                            instructionHe = "החליפו צד.",
                            instructionEn = "Change sides.",
                            durationSeconds = 6,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "arms_wrist_extensor",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת גב האמה",
                titleEn = "Wrist extensor stretch",
                instructionsHe =
                    "הושיטו יד לפנים כשהמרפק ישר אך משוחרר מעט וכף היד פונה כלפי מטה. " +
                            "כופפו את שורש כף היד כך שהאצבעות יפנו מטה. " +
                            "בעזרת היד השנייה משכו בעדינות את כף היד לכיוון הגוף. " +
                            "החליפו צד.",
                instructionsEn =
                    "Extend one arm forward with the elbow straight but slightly relaxed and the palm facing downward. " +
                            "Bend the wrist so the fingers point toward the floor. " +
                            "Use the opposite hand to gently draw the hand toward your body. " +
                            "Change sides.",
                durationSeconds = 20,
                repetitions = 2,
                performBothSides = true,
                imageKey = "arms_wrist_extensor_right",
                safetyNoteHe =
                    "הפעילו משיכה עדינה בלבד. עצרו במקרה של נימול, כאב חד או הקרנה אל האצבעות.",
                safetyNoteEn =
                    "Use only gentle pressure. Stop if you feel numbness, sharp pain, or symptoms spreading into the fingers.",
                sortOrder = 9,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "arms_wrist_extensor_right",
                            instructionHe = "מתחו בעדינות.",
                            instructionEn = "Stretch gently.",
                            durationSeconds = 6,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "arms_wrist_extensor_left",
                            instructionHe = "החליפו צד.",
                            instructionEn = "Change sides.",
                            durationSeconds = 6,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),
            StretchingExercise(
                id = "arms_forearm_rotation",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "סיבובי אמות וכפות ידיים",
                titleEn = "Forearm and palm rotations",
                instructionsHe =
                    "כופפו את המרפקים לצד הגוף בזווית של כ־90 מעלות. " +
                            "סובבו באיטיות את האמות כך שכפות הידיים יפנו כלפי מעלה, פנימה ולמטה. " +
                            "שמרו את המרפקים צמודים לגוף ואת הכתפיים משוחררות.",
                instructionsEn =
                    "Bend your elbows beside your body to approximately 90 degrees. " +
                            "Slowly rotate your forearms so the palms face upward, inward, and downward. " +
                            "Keep your elbows close to your body and your shoulders relaxed.",
                durationSeconds = 30,
                repetitions = 10,
                performBothSides = false,
                imageKey = "arms_forearm_rotation_up",
                safetyNoteHe =
                    "בצעו את הסיבוב בטווח נוח בלבד. אין לכפות תנועה דרך שורש כף היד או המרפק.",
                safetyNoteEn =
                    "Rotate only within a comfortable range. Do not force the wrist or elbow.",
                sortOrder = 10,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "arms_forearm_rotation_up",
                            instructionHe = "כפות הידיים למעלה.",
                            instructionEn = "Palms up.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "arms_forearm_rotation_neutral",
                            instructionHe = "סובבו פנימה.",
                            instructionEn = "Rotate inward.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "arms_forearm_rotation_down",
                            instructionHe = "כפות הידיים למטה.",
                            instructionEn = "Palms down.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        )
                    )
            )
        )
}