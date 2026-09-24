package il.kmi.shared.stretching

internal object LowerBackStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "lower_back_pelvic_tilt",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "הטיית אגן בשכיבה",
                titleEn = "Supine pelvic tilt",
                instructionsHe =
                    "שכבו על הגב כשהברכיים כפופות וכפות הרגליים על המשטח. כווצו בעדינות את הבטן והטו את האגן כך שהגב התחתון יתקרב למשטח. שחררו וחזרו למנח ניטרלי.",
                instructionsEn =
                    "Lie on your back with knees bent and feet supported. Gently tighten your abdomen and tilt your pelvis so the lower back moves toward the floor. Relax and return to neutral.",
                durationSeconds = 35,
                repetitions = 10,
                performBothSides = false,
                imageKey = "stretch_lower_back_pelvic_tilt",
                safetyNoteHe =
                    "התנועה קטנה ועדינה. אין לדחוף את הגב בכוח אל המשטח.",
                safetyNoteEn =
                    "Keep the movement small and gentle. Do not force your back into the floor.",
                sortOrder = 1
            ),
            StretchingExercise(
                id = "lower_back_single_knee_chest",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "ברך אחת לכיוון החזה",
                titleEn = "Single knee-to-chest stretch",
                instructionsHe =
                    "שכבו על הגב כשהברכיים כפופות. אחזו מאחורי ירך אחת ומשכו את הברך בעדינות לכיוון החזה. הרגל השנייה נשארת כפופה או ישרה, לפי הנוחות. החליפו צד.",
                instructionsEn =
                    "Lie on your back with knees bent. Hold behind one thigh and gently draw the knee toward your chest. Keep the other leg bent or straight, whichever is more comfortable. Change sides.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = true,
                imageKey = "stretch_lower_back_single_knee_chest",
                safetyNoteHe =
                    "אין ללחוץ ישירות על פיקת הברך או למשוך דרך כאב בגב או בירך.",
                safetyNoteEn =
                    "Do not press directly on the kneecap or pull through back or hip pain.",
                sortOrder = 2
            ),
            StretchingExercise(
                id = "lower_back_double_knee_chest",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "שתי ברכיים לכיוון החזה",
                titleEn = "Double knee-to-chest stretch",
                instructionsHe =
                    "שכבו על הגב והרימו את שתי הברכיים. אחזו מאחורי הירכיים ומשכו אותן בעדינות לכיוון הגוף. שמרו את הראש והכתפיים משוחררים על המשטח.",
                instructionsEn =
                    "Lie on your back and lift both knees. Hold behind the thighs and gently draw them toward your body. Keep your head and shoulders relaxed on the floor.",
                durationSeconds = 25,
                repetitions = 2,
                performBothSides = false,
                imageKey = "stretch_lower_back_double_knee_chest",
                safetyNoteHe =
                    "אם התנוחה מגבירה כאב או גורמת לחץ בבטן, חזרו למתיחה עם רגל אחת.",
                safetyNoteEn =
                    "If this increases pain or creates abdominal pressure, return to the single-leg version.",
                sortOrder = 3
            ),
            StretchingExercise(
                id = "lower_back_knee_rolls",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "הטיית ברכיים מצד לצד",
                titleEn = "Supine knee rolls",
                instructionsHe =
                    "שכבו על הגב כשהברכיים כפופות וצמודות. השאירו את הכתפיים על המשטח והטו את הברכיים באיטיות לצד אחד. חזרו למרכז והטו לצד השני.",
                instructionsEn =
                    "Lie on your back with knees bent and together. Keep both shoulders supported and slowly lower the knees toward one side. Return to the center and move to the other side.",
                durationSeconds = 40,
                repetitions = 8,
                performBothSides = true,
                imageKey = "stretch_lower_back_knee_rolls",
                safetyNoteHe =
                    "הברכיים יורדות רק עד לטווח נוח ואין לבצע תנופה.",
                safetyNoteEn =
                    "Lower the knees only within a comfortable range and do not use momentum.",
                sortOrder = 4
            ),
            StretchingExercise(
                id = "lower_back_cat_cow",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "קימור ושחרור הגב בעמידת שש",
                titleEn = "Quadruped back mobility",
                instructionsHe =
                    "עברו לעמידת שש. כווצו בעדינות את הבטן ועגלו את הגב כלפי מעלה. לאחר מכן חזרו באיטיות למנח ניטרלי ופתחו מעט את בית החזה.",
                instructionsEn =
                    "Begin on your hands and knees. Gently tighten your abdomen and round your back upward. Slowly return to neutral and lightly open the chest.",
                durationSeconds = 40,
                repetitions = 8,
                performBothSides = false,
                imageKey = "stretch_lower_back_cat_cow",
                safetyNoteHe =
                    "אין לשקוע לקשת עמוקה בגב התחתון או להשליך את הראש לאחור.",
                safetyNoteEn =
                    "Avoid a deep lower-back arch or dropping the head backward.",
                sortOrder = 5
            ),
            StretchingExercise(
                id = "lower_back_child_pose",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "העברת אגן לאחור בעמידת שש",
                titleEn = "Backward rocking stretch",
                instructionsHe =
                    "מעמידת שש העבירו את האגן באיטיות לאחור לכיוון העקבים, כשהידיים נשארות לפנים. עצרו בטווח נוח וחזרו לעמידת שש.",
                instructionsEn =
                    "From hands and knees, slowly move your hips backward toward your heels while keeping your arms forward. Pause within a comfortable range and return to hands and knees.",
                durationSeconds = 35,
                repetitions = 6,
                performBothSides = false,
                imageKey = "stretch_lower_back_child_pose",
                safetyNoteHe =
                    "אם יש כאב בברכיים, השתמשו בריפוד או דלגו על התרגיל.",
                safetyNoteEn =
                    "Use cushioning or skip the exercise if it causes knee pain.",
                sortOrder = 6
            ),
            StretchingExercise(
                id = "lower_back_sphinx",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "הישענות עדינה על האמות",
                titleEn = "Gentle sphinx position",
                instructionsHe =
                    "שכבו על הבטן והניחו את האמות מתחת לכתפיים. הרימו בעדינות את החזה תוך שמירה על האגן והירכיים על המשטח. נשמו וחזרו מטה באיטיות.",
                instructionsEn =
                    "Lie on your stomach with your forearms under your shoulders. Gently lift your chest while keeping the pelvis and hips supported. Breathe and lower slowly.",
                durationSeconds = 20,
                repetitions = 4,
                performBothSides = false,
                imageKey = "stretch_lower_back_sphinx",
                safetyNoteHe =
                    "הפסיקו אם התנוחה מגבירה כאב גב או גורמת הקרנה לרגל.",
                safetyNoteEn =
                    "Stop if the position increases back pain or causes symptoms to travel into a leg.",
                sortOrder = 7
            ),
            StretchingExercise(
                id = "lower_back_seated_pelvic_rock",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "הנעת אגן בישיבה",
                titleEn = "Seated pelvic rocking",
                instructionsHe =
                    "שבו על קדמת כיסא יציב כשכפות הרגליים על הרצפה. הטו את האגן מעט לאחור ועגלו בעדינות את הגב התחתון. לאחר מכן הטו את האגן מעט לפנים וחזרו למנח ניטרלי.",
                instructionsEn =
                    "Sit near the front of a stable chair with both feet supported. Tilt the pelvis slightly backward and gently round the lower back. Then tilt slightly forward and return to neutral.",
                durationSeconds = 35,
                repetitions = 10,
                performBothSides = false,
                imageKey = "stretch_lower_back_seated_pelvic_rock",
                safetyNoteHe =
                    "שמרו על תנועה קטנה ואיטית ואל תגיעו לקשת קיצונית.",
                safetyNoteEn =
                    "Keep the movement small and slow, and avoid an extreme arch.",
                sortOrder = 8
            ),
            StretchingExercise(
                id = "lower_back_supported_forward",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "כפיפה קדימה עם תמיכה",
                titleEn = "Supported forward stretch",
                instructionsHe =
                    "שבו על כיסא יציב כשהרגליים מעט פשוקות. הניחו את הידיים על הירכיים והטו את הגוף קדימה מהאגן. החליקו את הידיים לכיוון הברכיים ועצרו בטווח נוח.",
                instructionsEn =
                    "Sit on a stable chair with your feet slightly apart. Place your hands on your thighs and hinge forward from the hips. Slide your hands toward the knees and stop within a comfortable range.",
                durationSeconds = 25,
                repetitions = 3,
                performBothSides = false,
                imageKey = "stretch_lower_back_supported_forward",
                safetyNoteHe =
                    "אין לבצע אם כפיפה קדימה מגבירה כאב או גורמת נימול והקרנה לרגל.",
                safetyNoteEn =
                    "Do not perform this movement if forward bending increases pain, numbness, or leg symptoms.",
                sortOrder = 9
            ),
            StretchingExercise(
                id = "lower_back_standing_extension",
                category = StretchingCategory.LOWER_BACK,
                titleHe = "יישור עדין בעמידה",
                titleEn = "Gentle standing extension",
                instructionsHe =
                    "עמדו זקוף כשהרגליים ברוחב האגן. הניחו את הידיים על האגן או הגב התחתון. הישענו מעט לאחור תוך שמירה על הברכיים רכות וחזרו למרכז.",
                instructionsEn =
                    "Stand upright with feet hip-width apart. Place your hands on your pelvis or lower back. Lean slightly backward while keeping the knees soft, then return to the center.",
                durationSeconds = 25,
                repetitions = 5,
                performBothSides = false,
                imageKey = "stretch_lower_back_standing_extension",
                safetyNoteHe =
                    "התנועה צריכה להיות קטנה. עצרו אם מופיעים כאב חד, סחרחורת או הקרנה לרגל.",
                safetyNoteEn =
                    "Keep the movement small. Stop if you feel sharp pain, dizziness, or symptoms spreading into a leg.",
                sortOrder = 10
            )
        )
}