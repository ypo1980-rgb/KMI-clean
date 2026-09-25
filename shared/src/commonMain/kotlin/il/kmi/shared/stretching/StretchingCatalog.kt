package il.kmi.shared.stretching

object StretchingCatalog {

    private val neckAndHeadExercises =
        listOf(
            StretchingExercise(
                id = "neck_chin_tuck",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "הכנסת סנטר",
                titleEn = "Chin tuck",
                instructionsHe =
                    "שב או עמוד בגב זקוף והבט ישר קדימה. " +
                            "הזז את הסנטר בעדינות לאחור, כאילו אתה יוצר סנטר כפול. " +
                            "שמור את הראש ישר ואל תטה אותו כלפי מטה. " +
                            "החזק ושחרר באיטיות.",
                instructionsEn =
                    "Sit or stand upright and look straight ahead. " +
                            "Gently move your chin backward, as if making a double chin. " +
                            "Keep your head level and do not tilt it downward. " +
                            "Hold, then release slowly.",
                durationSeconds = 5,
                repetitions = 6,
                performBothSides = false,
                imageKey = "neck_chin_tuck",
                safetyNoteHe =
                    "התנועה צריכה להיות קטנה ועדינה. אין לדחוף את הסנטר בכוח.",
                safetyNoteEn =
                    "Keep the movement small and gentle. Do not force the chin backward.",
                sortOrder = 0,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "neck_chin_tuck_start",
                            instructionHe =
                                "שב או עמוד זקוף, הרפה את הכתפיים והבט ישר קדימה.",
                            instructionEn =
                                "Sit or stand upright, relax your shoulders, and look straight ahead.",
                            durationSeconds = 3,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "neck_chin_tuck_move",
                            instructionHe =
                                "הזז את הסנטר בעדינות לאחור מבלי להטות את הראש מטה.",
                            instructionEn =
                                "Gently move your chin backward without tilting your head down.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "neck_chin_tuck_hold",
                            instructionHe =
                                "החזק את התנוחה בעדינות והמשך לנשום כרגיל.",
                            instructionEn =
                                "Hold the position gently and continue breathing normally.",
                            durationSeconds = 5,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "neck_forward_flexion",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "כפיפת צוואר לפנים",
                titleEn = "Forward neck flexion",
                instructionsHe =
                    "שב או עמוד בגב זקוף והכתפיים רפויות. " +
                            "הורד את הסנטר באיטיות לכיוון החזה עד שמורגשת מתיחה עדינה בעורף. " +
                            "החזק את התנוחה וחזור באיטיות למרכז.",
                instructionsEn =
                    "Sit or stand upright with relaxed shoulders. " +
                            "Slowly lower your chin toward your chest until you feel a gentle stretch " +
                            "at the back of your neck. Hold, then slowly return to the center.",
                durationSeconds = 5,
                repetitions = 5,
                performBothSides = false,
                imageKey = "neck_forward_flexion",
                safetyNoteHe =
                    "אין ללחוץ על הראש בעזרת הידיים ואין לבצע תנועה קופצנית.",
                safetyNoteEn =
                    "Do not press on your head with your hands and do not bounce.",
                sortOrder = 1,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey =
                                "neck_forward_flexion_start",
                            instructionHe =
                                "שבו זקוף, הרפו את הכתפיים והביטו קדימה.",
                            instructionEn =
                                "Sit upright, relax your shoulders, and look straight ahead.",
                            durationSeconds = 3,
                            type =
                                StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey =
                                "neck_forward_flexion",
                            instructionHe =
                                "הורידו את הסנטר בעדינות לכיוון החזה. החזיקו ונשמו רגיל.",
                            instructionEn =
                                "Gently lower your chin toward your chest. Hold and breathe normally.",
                            durationSeconds = 5,
                            type =
                                StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "neck_rotation",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "סיבוב צוואר לצדדים",
                titleEn = "Neck rotation",
                instructionsHe =
                    "שב או עמוד זקוף והבט קדימה. " +
                            "סובב את הראש באיטיות לצד אחד, כאילו אתה מביט מעבר לכתף. " +
                            "שמור את הסנטר באותו גובה ואת הכתפיים רפויות. " +
                            "חזור למרכז ובצע לצד השני.",
                instructionsEn =
                    "Sit or stand upright and look forward. " +
                            "Slowly turn your head to one side as if looking over your shoulder. " +
                            "Keep your chin level and your shoulders relaxed. " +
                            "Return to the center and repeat on the other side.",
                durationSeconds = 5,
                repetitions = 5,
                performBothSides = true,
                imageKey = "neck_rotation",
                safetyNoteHe =
                    "יש להסתובב רק עד הטווח הנוח, ללא כאב וללא תנועה חדה.",
                safetyNoteEn =
                    "Turn only within a comfortable range, without pain or sudden movement.",
                sortOrder = 2
            ),

            StretchingExercise(
                id = "neck_side_flexion",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "הטיית צוואר לצד",
                titleEn = "Side neck flexion",
                instructionsHe =
                    "שב או עמוד בגב זקוף. " +
                            "הטה את האוזן באיטיות לכיוון הכתף, מבלי להרים את הכתף. " +
                            "הרגש מתיחה עדינה בצד הנגדי של הצוואר. " +
                            "חזור למרכז ובצע לצד השני.",
                instructionsEn =
                    "Sit or stand upright. " +
                            "Slowly tilt one ear toward the shoulder without lifting the shoulder. " +
                            "Feel a gentle stretch on the opposite side of the neck. " +
                            "Return to the center and repeat on the other side.",
                durationSeconds = 10,
                repetitions = 3,
                performBothSides = true,
                imageKey = "neck_side_flexion",
                safetyNoteHe =
                    "אין למשוך את הראש באמצעות היד ואין לקרב את הכתף לאוזן.",
                safetyNoteEn =
                    "Do not pull your head with your hand and do not lift the shoulder toward the ear.",
                sortOrder = 3
            ),

            StretchingExercise(
                id = "upper_trapezius_stretch",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "מתיחת הטרפז העליון",
                titleEn = "Upper trapezius stretch",
                instructionsHe =
                    "שב זקוף והחזק את צד הכיסא ביד אחת כדי לשמור את הכתף נמוכה. " +
                            "הטה את הראש בעדינות לצד הנגדי. " +
                            "שמור את הפנים קדימה והרגש מתיחה בצד הצוואר והכתף. " +
                            "חזור ובצע בצד השני.",
                instructionsEn =
                    "Sit upright and hold the side of the chair with one hand to keep that shoulder down. " +
                            "Gently tilt your head toward the opposite side. " +
                            "Keep your face forward and feel the stretch along the side of the neck and shoulder. " +
                            "Repeat on the other side.",
                durationSeconds = 15,
                repetitions = 2,
                performBothSides = true,
                imageKey = "upper_trapezius_stretch",
                safetyNoteHe =
                    "אין למשוך את הראש בכוח. עצור אם מופיעים כאב חד, נימול או סחרחורת.",
                safetyNoteEn =
                    "Do not pull your head forcefully. Stop if you feel sharp pain, numbness, or dizziness.",
                sortOrder = 4
            ),

            StretchingExercise(
                id = "levator_scapulae_stretch",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "מתיחת מרים השכמה",
                titleEn = "Levator scapulae stretch",
                instructionsHe =
                    "שב זקוף והחזק את הכיסא ביד אחת. " +
                            "סובב את הראש מעט לצד הנגדי והפנה את המבט באלכסון לכיוון בית השחי. " +
                            "הורד את הסנטר בעדינות עד שמורגשת מתיחה בחלק האחורי־צדי של הצוואר. " +
                            "חזור ובצע בצד השני.",
                instructionsEn =
                    "Sit upright and hold the chair with one hand. " +
                            "Turn your head slightly toward the opposite side and look diagonally toward your armpit. " +
                            "Gently lower your chin until you feel a stretch at the back and side of your neck. " +
                            "Repeat on the other side.",
                durationSeconds = 15,
                repetitions = 2,
                performBothSides = true,
                imageKey = "levator_scapulae_stretch",
                safetyNoteHe =
                    "אין למשוך את הראש. השתמש רק במשקל הראש ובטווח תנועה נוח.",
                safetyNoteEn =
                    "Do not pull your head. Use only the weight of your head and a comfortable range.",
                sortOrder = 5
            ),

            StretchingExercise(
                id = "shoulder_rolls",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "סיבובי כתפיים",
                titleEn = "Shoulder rolls",
                instructionsHe =
                    "שב או עמוד בגב זקוף והידיים רפויות לצד הגוף. " +
                            "הרם את הכתפיים בעדינות, גלגל אותן לאחור והורד אותן בתנועה מעגלית. " +
                            "בצע את התנועה לאט ובשליטה ולאחר מכן החלף כיוון.",
                instructionsEn =
                    "Sit or stand upright with your arms relaxed by your sides. " +
                            "Gently lift your shoulders, roll them backward, and lower them in a circle. " +
                            "Move slowly and with control, then change direction.",
                durationSeconds = 20,
                repetitions = 8,
                performBothSides = false,
                imageKey = "shoulder_rolls",
                safetyNoteHe =
                    "שמור את הצוואר רפוי והימנע מתנועות מהירות או גדולות מדי.",
                safetyNoteEn =
                    "Keep your neck relaxed and avoid movements that are too fast or too large.",
                sortOrder = 6
            ),

            StretchingExercise(
                id = "scapular_retraction",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "קירוב שכמות",
                titleEn = "Scapular retraction",
                instructionsHe =
                    "שב או עמוד בגב זקוף והזרועות לצד הגוף. " +
                            "משוך בעדינות את השכמות לאחור ואחת לכיוון השנייה. " +
                            "שמור את הכתפיים נמוכות ואל תקשת את הגב. " +
                            "החזק ושחרר באיטיות.",
                instructionsEn =
                    "Sit or stand upright with your arms by your sides. " +
                            "Gently draw your shoulder blades backward and toward each other. " +
                            "Keep your shoulders down and avoid arching your back. " +
                            "Hold, then release slowly.",
                durationSeconds = 5,
                repetitions = 8,
                performBothSides = false,
                imageKey = "scapular_retraction",
                safetyNoteHe =
                    "התנועה צריכה להגיע מהשכמות ולא ממשיכת הראש או הצוואר לאחור.",
                safetyNoteEn =
                    "The movement should come from the shoulder blades, not from pulling the head or neck backward.",
                sortOrder = 7
            ),

            StretchingExercise(
                id = "seated_row_without_equipment",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "חתירה בישיבה ללא ציוד",
                titleEn = "Seated row without equipment",
                instructionsHe =
                    "שב זקוף ושלח את שתי הידיים לפנים בגובה הכתפיים. " +
                            "משוך את המרפקים לאחור תוך קירוב עדין של השכמות. " +
                            "שמור את הראש בקו ישר ואת הכתפיים רחוקות מהאוזניים. " +
                            "יישר שוב את הידיים לפנים.",
                instructionsEn =
                    "Sit upright and extend both arms forward at shoulder height. " +
                            "Pull your elbows backward while gently bringing your shoulder blades together. " +
                            "Keep your head aligned and your shoulders away from your ears. " +
                            "Extend your arms forward again.",
                durationSeconds = 20,
                repetitions = 8,
                performBothSides = false,
                imageKey = "seated_row_without_equipment",
                safetyNoteHe =
                    "אין להרים את הכתפיים ואין להקשית את הגב בזמן המשיכה.",
                safetyNoteEn =
                    "Do not lift your shoulders or arch your back while pulling.",
                sortOrder = 8
            ),

            StretchingExercise(
                id = "seated_upper_body_rotation",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "סיבוב עדין של פלג הגוף העליון",
                titleEn = "Gentle seated upper-body rotation",
                instructionsHe =
                    "שב זקוף כאשר כפות הרגליים מונחות על הרצפה. " +
                            "הנח יד אחת על הירך הנגדית וסובב באיטיות את בית החזה לצד. " +
                            "אפשר לראש לנוע יחד עם הגוף מבלי להכריח את הצוואר. " +
                            "חזור למרכז ובצע לצד השני.",
                instructionsEn =
                    "Sit upright with both feet on the floor. " +
                            "Place one hand on the opposite thigh and slowly rotate your chest to the side. " +
                            "Allow your head to move with your body without forcing your neck. " +
                            "Return to the center and repeat on the other side.",
                durationSeconds = 10,
                repetitions = 3,
                performBothSides = true,
                imageKey = "seated_upper_body_rotation",
                safetyNoteHe =
                    "בצע סיבוב קטן ונוח. אין לבצע תנועה קופצנית או להכריח את הצוואר.",
                safetyNoteEn =
                    "Use a small, comfortable rotation. Do not bounce or force your neck.",
                sortOrder = 9
            )
        )

    private val exercisesByCategory:
            Map<StretchingCategory, List<StretchingExercise>> =
        mapOf(
            StretchingCategory.NECK_AND_HEAD to
                    neckAndHeadExercises,

            StretchingCategory.SHOULDERS_AND_ARMS to
                    ShouldersAndArmsStretchingExercises.exercises,

            StretchingCategory.UPPER_BACK to
                    UpperBackStretchingExercises.exercises,

            StretchingCategory.LOWER_BACK to
                    LowerBackStretchingExercises.exercises
        )

    val allExercises: List<StretchingExercise> =
        exercisesByCategory
            .values
            .flatten()
            .sortedWith(
                compareBy<StretchingExercise> {
                    it.category.sortOrder
                }.thenBy {
                    it.sortOrder
                }
            )

    fun exercisesFor(
        category: StretchingCategory
    ): List<StretchingExercise> {
        return exercisesByCategory[
            category
        ].orEmpty()
    }

    fun exerciseById(
        exerciseId: String
    ): StretchingExercise? {
        val cleanId = exerciseId.trim()

        if (cleanId.isBlank()) return null

        return allExercises.firstOrNull {
            it.id == cleanId
        }
    }

    fun categoriesWithContent(): List<StretchingCategory> {
        return exercisesByCategory
            .filterValues {
                it.isNotEmpty()
            }
            .keys
            .sortedBy {
                it.sortOrder
            }
    }

    fun exerciseCountFor(
        category: StretchingCategory
    ): Int {
        return exercisesByCategory[
            category
        ]?.size ?: 0
    }

    fun hasExercises(
        category: StretchingCategory
    ): Boolean {
        return exerciseCountFor(
            category = category
        ) > 0
    }
}