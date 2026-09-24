package il.kmi.shared.stretching

internal object UpperBackStretchingExercises {

    val exercises: List<StretchingExercise> =
        listOf(
            StretchingExercise(
                id = "upper_back_forward_reach",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "מתיחת גב עליון בהושטת ידיים",
                titleEn = "Forward reach upper-back stretch",
                instructionsHe =
                    "שבו או עמדו בגב זקוף. שלבו את האצבעות והושיטו את הידיים לפנים בגובה החזה. הרחיקו בעדינות את השכמות זו מזו והורידו מעט את הסנטר.",
                instructionsEn =
                    "Sit or stand upright. Interlace your fingers and reach your arms forward at chest height. Gently spread the shoulder blades apart and lower your chin slightly.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "stretch_upper_back_forward_reach",
                safetyNoteHe =
                    "אין למשוך את הראש בכוח או לעגל את הגב התחתון בצורה מוגזמת.",
                safetyNoteEn =
                    "Do not pull the head forward or excessively round the lower back.",
                sortOrder = 1
            ),
            StretchingExercise(
                id = "upper_back_self_hug",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "חיבוק עצמי לגב העליון",
                titleEn = "Self-hug upper-back stretch",
                instructionsHe =
                    "עטפו את הידיים סביב הגוף כאילו אתם מחבקים את עצמכם. אחזו בעדינות בשכמות או בכתפיים והרחיקו את המרפקים מעט לפנים עד שמורגשת מתיחה בין השכמות.",
                instructionsEn =
                    "Wrap your arms around your body as if giving yourself a hug. Gently hold the shoulder blades or shoulders and move the elbows slightly forward until you feel a stretch between the shoulder blades.",
                durationSeconds = 30,
                repetitions = 2,
                performBothSides = false,
                imageKey = "stretch_upper_back_self_hug",
                safetyNoteHe =
                    "שמרו על נשימה חופשית ואל תפעילו לחץ ישיר על הצוואר.",
                safetyNoteEn =
                    "Breathe normally and avoid placing direct pressure on the neck.",
                sortOrder = 2
            ),
            StretchingExercise(
                id = "upper_back_chair_extension",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "פשיטת גב עליון על כיסא",
                titleEn = "Seated thoracic extension",
                instructionsHe =
                    "שבו על כיסא יציב בעל משענת נמוכה. הניחו את הידיים מאחורי הראש ותמכו בו בעדינות. הישענו לאחור מעל קצה המשענת ופתחו את בית החזה, בלי לקשת את הגב התחתון.",
                instructionsEn =
                    "Sit on a stable chair with a low backrest. Place your hands behind your head for gentle support. Lean the upper back over the edge of the chair and open the chest without arching the lower back.",
                durationSeconds = 25,
                repetitions = 4,
                performBothSides = false,
                imageKey = "stretch_upper_back_chair_extension",
                safetyNoteHe =
                    "הכיסא חייב להיות יציב. אין למשוך את הראש או להישען לאחור במהירות.",
                safetyNoteEn =
                    "Use a stable chair. Do not pull on your head or lean backward quickly.",
                sortOrder = 3
            ),
            StretchingExercise(
                id = "upper_back_seated_rotation",
                category = StretchingCategory.UPPER_BACK,
                titleHe = "סיבוב גב עליון בישיבה",
                titleEn = "Seated upper-back rotation",
                instructionsHe =
                    "שבו זקוף כשהרגליים מונחות על הרצפה. שלבו את הידיים על החזה וסובבו באיטיות את פלג הגוף העליון לצד אחד. חזרו למרכז ובצעו לצד השני.",
                instructionsEn =
                    "Sit upright with both feet on the floor. Cross your arms over your chest and slowly rotate your upper body to one side. Return to the center and repeat in the other direction.",
                durationSeconds = 30,
                repetitions = 5,
                performBothSides = true,
                imageKey = "stretch_upper_back_seated_rotation",
                safetyNoteHe =
                    "האגן נשאר מול החזית. אין לבצע תנופה או לסובב דרך כאב.",
                safetyNoteEn =
                    "Keep the pelvis facing forward. Do not use momentum or rotate through pain.",
                sortOrder = 4
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
                imageKey = "stretch_upper_back_cat_cow",
                safetyNoteHe =
                    "בצעו תנועה קטנה ונוחה והימנעו מהשלכת הראש לאחור.",
                safetyNoteEn =
                    "Keep the movement small and comfortable, and avoid dropping the head backward.",
                sortOrder = 5
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
                imageKey = "stretch_upper_back_thread_needle",
                safetyNoteHe =
                    "אין להעמיס משקל על הראש או להמשיך אם מופיע כאב בכתף.",
                safetyNoteEn =
                    "Do not place body weight on the head or continue if shoulder pain develops.",
                sortOrder = 6
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
                imageKey = "stretch_upper_back_child_pose",
                safetyNoteHe =
                    "אם קיימת אי־נוחות בברכיים, הניחו כרית או דלגו על התרגיל.",
                safetyNoteEn =
                    "Use cushioning or skip this exercise if it causes knee discomfort.",
                sortOrder = 7
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
                imageKey = "stretch_upper_back_wall_lat",
                safetyNoteHe =
                    "שמרו על ברכיים מעט כפופות ואל תדחפו את הכתפיים מעבר לטווח נוח.",
                safetyNoteEn =
                    "Keep the knees slightly bent and do not force the shoulders beyond a comfortable range.",
                sortOrder = 8
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
                imageKey = "stretch_upper_back_side_reach",
                safetyNoteHe =
                    "שתי עצמות הישיבה נשארות על הכיסא ואין להטות את הגוף בכוח.",
                safetyNoteEn =
                    "Keep both sitting bones on the chair and avoid forcing the side bend.",
                sortOrder = 9
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
                imageKey = "stretch_upper_back_scapular_glide",
                safetyNoteHe =
                    "שמרו על תנועה איטית ואל תכווצו את הכתפיים לכיוון האוזניים.",
                safetyNoteEn =
                    "Move slowly and avoid shrugging the shoulders toward the ears.",
                sortOrder = 10
            )
        )
}