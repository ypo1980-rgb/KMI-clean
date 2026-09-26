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
                imageKey = "neck_rotation_start",
                safetyNoteHe =
                    "יש להסתובב רק עד הטווח הנוח, ללא כאב וללא תנועה חדה.",
                safetyNoteEn =
                    "Turn only within a comfortable range, without pain or sudden movement.",
                sortOrder = 2,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey =
                                "neck_rotation_start",
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
                                "neck_rotation_right",
                            instructionHe =
                                "סובבו את הראש באיטיות ימינה, עד לטווח הנוח.",
                            instructionEn =
                                "Slowly turn your head to the right, within a comfortable range.",
                            durationSeconds = 5,
                            type =
                                StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey =
                                "neck_rotation_left",
                            instructionHe =
                                "חזרו למרכז וסובבו את הראש באיטיות שמאלה.",
                            instructionEn =
                                "Return to the center and slowly turn your head to the left.",
                            durationSeconds = 5,
                            type =
                                StretchingStepType.MOVE
                        )
                    )
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
                imageKey = "neck_side_flexion_start",
                safetyNoteHe =
                    "אין למשוך את הראש באמצעות היד ואין לקרב את הכתף לאוזן.",
                safetyNoteEn =
                    "Do not pull your head with your hand and do not lift the shoulder toward the ear.",
                sortOrder = 3,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey =
                                "neck_side_flexion_start",
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
                                "neck_side_flexion_left",
                            instructionHe =
                                "הטו את הראש בעדינות שמאלה, מבלי להרים את הכתף.",
                            instructionEn =
                                "Gently tilt your head to the left without lifting your shoulder.",
                            durationSeconds = 10,
                            type =
                                StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey =
                                "neck_side_flexion_right",
                            instructionHe =
                                "חזרו למרכז והטו את הראש בעדינות ימינה.",
                            instructionEn =
                                "Return to the center and gently tilt your head to the right.",
                            durationSeconds = 10,
                            type =
                                StretchingStepType.MOVE
                        )
                    )
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
                titleHe = "מתיחת צוואר באלכסון",
                titleEn = "Diagonal neck stretch",
                instructionsHe =
                    "שב זקוף ואחוז בצד הכיסא ביד אחת כדי להשאיר את הכתף נמוכה. " +
                            "סובב מעט את הראש לצד הנגדי והפנה את המבט באלכסון לכיוון בית השחי. " +
                            "הנח את היד השנייה בעדינות על הראש והורד את הסנטר ללא משיכה. " +
                            "חזור למרכז והחלף צד.",
                instructionsEn =
                    "Sit upright and hold the side of the chair with one hand to keep the shoulder down. " +
                            "Turn your head slightly toward the opposite side and look diagonally toward your armpit. " +
                            "Rest your other hand gently on your head and lower your chin without pulling. " +
                            "Return to the center and switch sides.",
                durationSeconds = 15,
                repetitions = 2,
                performBothSides = true,
                imageKey =
                    "neck_levator_scapulae_start_left",
                safetyNoteHe =
                    "אין למשוך את הראש. השתמש רק במשקל היד ובטווח תנועה נוח.",
                safetyNoteEn =
                    "Do not pull your head. Use only the weight of your hand and a comfortable range.",
                sortOrder = 5,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey =
                                "neck_levator_scapulae_start_left",
                            instructionHe =
                                "אחזו בצד הכיסא ביד ימין והשאירו את הכתף נמוכה.",
                            instructionEn =
                                "Hold the side of the chair with your right hand and keep the shoulder down.",
                            durationSeconds = 3,
                            type =
                                StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey =
                                "neck_levator_scapulae_left",
                            instructionHe =
                                "הביטו באלכסון שמאלה והורידו את הראש בעדינות לכיוון בית השחי.",
                            instructionEn =
                                "Look diagonally left and gently lower your head toward your armpit.",
                            durationSeconds = 15,
                            type =
                                StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey =
                                "neck_levator_scapulae_start_right",
                            instructionHe =
                                "חזרו למרכז, החליפו ידיים ואחזו בכיסא ביד שמאל.",
                            instructionEn =
                                "Return to the center, switch hands, and hold the chair with your left hand.",
                            durationSeconds = 3,
                            type =
                                StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey =
                                "neck_levator_scapulae_right",
                            instructionHe =
                                "הביטו באלכסון ימינה והורידו את הראש בעדינות לכיוון בית השחי.",
                            instructionEn =
                                "Look diagonally right and gently lower your head toward your armpit.",
                            durationSeconds = 15,
                            type =
                                StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "shoulder_rolls",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "סיבובי כתפיים",
                titleEn = "Shoulder rolls",
                instructionsHe =
                    "שב או עמוד בגב זקוף והידיים רפויות. " +
                            "הרם את הכתפיים בעדינות, גלגל אותן לאחור והורד אותן בתנועה מעגלית. " +
                            "בצע את התנועה לאט ובשליטה.",
                instructionsEn =
                    "Sit or stand upright with your arms relaxed. " +
                            "Gently lift your shoulders, roll them backward, and lower them in a circle. " +
                            "Move slowly and with control.",
                durationSeconds = 20,
                repetitions = 8,
                performBothSides = false,
                imageKey = "shoulder_rolls_start",
                safetyNoteHe =
                    "שמור על צוואר רפוי והימנע מתנועה מהירה או גדולה מדי.",
                safetyNoteEn =
                    "Keep your neck relaxed and avoid movements that are too fast or too large.",
                sortOrder = 6,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "shoulder_rolls_start",
                            instructionHe = "מוכנים.",
                            instructionEn = "Get ready.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulder_rolls_up",
                            instructionHe = "כתפיים למעלה.",
                            instructionEn = "Shoulders up.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        ),
                        StretchingVisualStep(
                            imageKey = "shoulder_rolls_back",
                            instructionHe = "סובבו לאחור והורידו.",
                            instructionEn = "Roll back and lower.",
                            durationSeconds = 2,
                            type = StretchingStepType.RELEASE
                        )
                    )
            ),

            StretchingExercise(
                id = "scapular_retraction",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "קירוב שכמות",
                titleEn = "Scapular retraction",
                instructionsHe =
                    "שב בגב זקוף כשהמרפקים כפופים לצד הגוף. " +
                            "משוך בעדינות את המרפקים לאחור וקרב את השכמות זו לזו. " +
                            "שמור את הכתפיים נמוכות ואל תקשת את הגב. " +
                            "שחרר באיטיות וחזור על התנועה.",
                instructionsEn =
                    "Sit upright with your elbows bent beside your body. " +
                            "Gently draw your elbows backward and bring your shoulder blades together. " +
                            "Keep your shoulders down and avoid arching your back. " +
                            "Release slowly and repeat.",
                durationSeconds = 24,
                repetitions = 8,
                performBothSides = false,
                imageKey = "scapular_retraction_start",
                safetyNoteHe =
                    "התנועה מגיעה מהשכמות. אין למשוך את הראש או הצוואר לאחור.",
                safetyNoteEn =
                    "The movement comes from the shoulder blades. Do not pull the head or neck backward.",
                sortOrder = 7,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "scapular_retraction_start",
                            instructionHe = "שחררו.",
                            instructionEn = "Release.",
                            durationSeconds = 1,
                            type = StretchingStepType.RELEASE
                        ),
                        StretchingVisualStep(
                            imageKey = "scapular_retraction_squeeze",
                            instructionHe = "קרבו שכמות.",
                            instructionEn = "Squeeze the shoulder blades.",
                            durationSeconds = 2,
                            type = StretchingStepType.HOLD
                        )
                    )
            ),

            StretchingExercise(
                id = "seated_row_without_equipment",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "חתירה בישיבה ללא ציוד",
                titleEn = "Seated row without equipment",
                instructionsHe =
                    "שב זקוף על כיסא או שרפרף ושלח את שתי הידיים לפנים בגובה הכתפיים. " +
                            "משוך את המרפקים לאחור וקרב בעדינות את השכמות. " +
                            "שמור את הראש בקו ישר ואת הכתפיים רחוקות מהאוזניים. " +
                            "יישר שוב את הידיים לפנים וחזור על התנועה.",
                instructionsEn =
                    "Sit upright on a chair or stool and extend both arms forward at shoulder height. " +
                            "Pull your elbows backward and gently bring your shoulder blades together. " +
                            "Keep your head aligned and your shoulders away from your ears. " +
                            "Extend your arms forward and repeat.",
                durationSeconds = 32,
                repetitions = 8,
                performBothSides = false,
                imageKey = "upper_back_seated_row_start",
                safetyNoteHe =
                    "אין להרים את הכתפיים או להקשית את הגב בזמן המשיכה.",
                safetyNoteEn =
                    "Do not lift your shoulders or arch your back while pulling.",
                sortOrder = 8,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_back_seated_row_start",
                            instructionHe = "ידיים קדימה.",
                            instructionEn = "Arms forward.",
                            durationSeconds = 2,
                            type = StretchingStepType.RELEASE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_back_seated_row_pull",
                            instructionHe = "משכו לאחור.",
                            instructionEn = "Pull back.",
                            durationSeconds = 2,
                            type = StretchingStepType.MOVE
                        )
                    )
            ),

            StretchingExercise(
                id = "seated_upper_body_rotation",
                category = StretchingCategory.NECK_AND_HEAD,
                titleHe = "סיבוב עדין של פלג הגוף העליון",
                titleEn = "Gentle seated upper-body rotation",
                instructionsHe =
                    "שב זקוף על כיסא כאשר שתי כפות הרגליים מונחות על הרצפה. " +
                            "שלב את הידיים על בית החזה וסובב באיטיות את פלג הגוף העליון לצד. " +
                            "שמור את האגן, הברכיים וכפות הרגליים פונים קדימה. " +
                            "חזור למרכז ובצע את התנועה לצד השני.",
                instructionsEn =
                    "Sit upright on a chair with both feet flat on the floor. " +
                            "Cross your arms over your chest and slowly rotate your upper body to one side. " +
                            "Keep your hips, knees, and feet facing forward. " +
                            "Return to the center and repeat on the other side.",
                durationSeconds = 30,
                repetitions = 3,
                performBothSides = true,
                imageKey = "upper_body_seated_rotation_start",
                safetyNoteHe =
                    "בצע סיבוב קטן ונוח. אין לבצע תנועה קופצנית או להכריח את הצוואר.",
                safetyNoteEn =
                    "Use a small, comfortable rotation. Do not bounce or force your neck.",
                sortOrder = 9,
                visualSteps =
                    listOf(
                        StretchingVisualStep(
                            imageKey = "upper_body_seated_rotation_start",
                            instructionHe = "חזרו למרכז.",
                            instructionEn = "Return to center.",
                            durationSeconds = 2,
                            type = StretchingStepType.PREPARE
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_body_seated_rotation_left",
                            instructionHe = "סובבו שמאלה.",
                            instructionEn = "Turn left.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        ),
                        StretchingVisualStep(
                            imageKey = "upper_body_seated_rotation_right",
                            instructionHe = "סובבו ימינה.",
                            instructionEn = "Turn right.",
                            durationSeconds = 4,
                            type = StretchingStepType.HOLD
                        )
                    )
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