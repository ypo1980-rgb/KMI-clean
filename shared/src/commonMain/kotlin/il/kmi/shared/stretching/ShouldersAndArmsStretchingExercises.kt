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
                    "הישענו ביד אחת על משטח יציב והטו מעט את הגוף קדימה. הניחו ליד השנייה להשתחרר כלפי מטה. הניעו אותה בעדינות קדימה ואחורה, לצדדים ובמעגלים קטנים. החליפו צד.",
                instructionsEn =
                    "Support yourself with one hand on a stable surface and lean slightly forward. Allow the other arm to hang loosely. Gently move it forward and backward, side to side, and in small circles. Change sides.",
                durationSeconds = 40,
                repetitions = null,
                performBothSides = true,
                imageKey = "stretch_shoulders_pendulum",
                safetyNoteHe =
                    "התנועה צריכה להגיע מתנועת הגוף העדינה ולא ממשיכה חזקה של הזרוע.",
                safetyNoteEn =
                    "Let the gentle movement of your body guide the arm; do not swing it forcefully.",
                sortOrder = 4
            ),
            StretchingExercise(
                id = "shoulders_wall_walk",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "טיפוס אצבעות על הקיר",
                titleEn = "Finger walk up the wall",
                instructionsHe =
                    "עמדו מול קיר והניחו עליו את קצות האצבעות. טפסו בהדרגה עם האצבעות כלפי מעלה עד לטווח נוח. עצרו לזמן קצר והורידו את היד באיטיות. חזרו ביד השנייה.",
                instructionsEn =
                    "Stand facing a wall and place your fingertips against it. Slowly walk your fingers upward to a comfortable height. Pause briefly, then lower the arm slowly. Repeat with the other arm.",
                durationSeconds = 40,
                repetitions = 5,
                performBothSides = true,
                imageKey = "stretch_shoulders_wall_walk",
                safetyNoteHe =
                    "אין להרים את היד מעבר לטווח נוח ואין למשוך את הכתף לכיוון האוזן.",
                safetyNoteEn =
                    "Do not raise the arm beyond a comfortable range or shrug the shoulder toward the ear.",
                sortOrder = 5
            ),
            StretchingExercise(
                id = "shoulders_arm_circles",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מעגלי ידיים מבוקרים",
                titleEn = "Controlled arm circles",
                instructionsHe =
                    "עמדו זקוף כשהידיים לצדי הגוף. הרימו אותן לטווח נוח ובצעו מעגלים קטנים ואיטיים לפנים. לאחר מכן החליפו כיוון. שמרו על כתפיים משוחררות.",
                instructionsEn =
                    "Stand upright with your arms by your sides. Raise them to a comfortable level and make small, slow circles forward. Then reverse direction. Keep your shoulders relaxed.",
                durationSeconds = 30,
                repetitions = null,
                performBothSides = false,
                imageKey = "stretch_shoulders_arm_circles",
                safetyNoteHe =
                    "התחילו במעגלים קטנים. אין לבצע תנועות מהירות או להגיע לטווח שמכאיב.",
                safetyNoteEn =
                    "Begin with small circles. Avoid fast movements or any range that causes pain.",
                sortOrder = 6
            ),
            StretchingExercise(
                id = "arms_biceps_wall",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת קדמת הזרוע ליד קיר",
                titleEn = "Wall biceps stretch",
                instructionsHe =
                    "עמדו לצד קיר והניחו עליו את כף היד כאשר הזרוע ישרה ובגובה נוח. סובבו את הגוף באיטיות הרחק מהקיר עד שמורגשת מתיחה עדינה בקדמת הזרוע והכתף. חזרו בצד השני.",
                instructionsEn =
                    "Stand beside a wall and place your palm against it with the arm straight at a comfortable height. Slowly turn your body away until you feel a gentle stretch along the front of the arm and shoulder. Repeat on the other side.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = true,
                imageKey = "stretch_arms_biceps_wall",
                safetyNoteHe =
                    "אל תנעלו את המרפק ואל תסובבו את הגוף מעבר לטווח נוח.",
                safetyNoteEn =
                    "Do not lock the elbow or rotate beyond a comfortable range.",
                sortOrder = 7
            ),
            StretchingExercise(
                id = "arms_wrist_flexor",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת פנים האמה",
                titleEn = "Wrist flexor stretch",
                instructionsHe =
                    "הושיטו יד לפנים כשהמרפק ישר וכף היד פונה כלפי מעלה. בעזרת היד השנייה משכו בעדינות את האצבעות כלפי מטה ולאחור עד שמורגשת מתיחה בפנים האמה. החליפו צד.",
                instructionsEn =
                    "Extend one arm forward with the elbow straight and palm facing upward. With the opposite hand, gently draw the fingers downward and back until you feel a stretch along the inner forearm. Change sides.",
                durationSeconds = 20,
                repetitions = 2,
                performBothSides = true,
                imageKey = "stretch_arms_wrist_flexor",
                safetyNoteHe =
                    "המשיכה צריכה להיות עדינה ואין להפעיל לחץ על מפרקי האצבעות.",
                safetyNoteEn =
                    "Use gentle pressure and avoid forcing the finger joints.",
                sortOrder = 8
            ),
            StretchingExercise(
                id = "arms_wrist_extensor",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "מתיחת גב האמה",
                titleEn = "Wrist extensor stretch",
                instructionsHe =
                    "הושיטו יד לפנים כשהמרפק ישר וכף היד פונה כלפי מטה. כופפו את שורש כף היד כך שהאצבעות יפנו מטה. בעזרת היד השנייה משכו בעדינות את כף היד לכיוון הגוף. החליפו צד.",
                instructionsEn =
                    "Extend one arm forward with the elbow straight and palm facing downward. Bend the wrist so the fingers point down. Use the opposite hand to gently draw the hand toward your body. Change sides.",
                durationSeconds = 20,
                repetitions = 2,
                performBothSides = true,
                imageKey = "stretch_arms_wrist_extensor",
                safetyNoteHe =
                    "יש לעצור אם מופיעים נימול, כאב חד או הקרנה אל האצבעות.",
                safetyNoteEn =
                    "Stop if you feel numbness, sharp pain, or symptoms spreading into the fingers.",
                sortOrder = 9
            ),
            StretchingExercise(
                id = "arms_forearm_rotation",
                category =
                    StretchingCategory.SHOULDERS_AND_ARMS,
                titleHe = "סיבובי אמות וכפות ידיים",
                titleEn = "Forearm and palm rotations",
                instructionsHe =
                    "כופפו את המרפקים לצדי הגוף בזווית נוחה. סובבו באיטיות את האמות כך שכפות הידיים יפנו פעם כלפי מעלה ופעם כלפי מטה. שמרו את המרפקים קרובים לגוף.",
                instructionsEn =
                    "Bend your elbows comfortably at your sides. Slowly rotate your forearms so the palms turn upward and then downward. Keep the elbows close to your body.",
                durationSeconds = 30,
                repetitions = 10,
                performBothSides = false,
                imageKey = "stretch_arms_forearm_rotation",
                safetyNoteHe =
                    "בצעו את הסיבוב בטווח נוח, בלי לכפות תנועה דרך שורש כף היד או המרפק.",
                safetyNoteEn =
                    "Rotate only within a comfortable range and do not force the wrist or elbow.",
                sortOrder = 10
            )
        )
}