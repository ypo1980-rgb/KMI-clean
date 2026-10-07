package il.kmi.app.notes

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import il.kmi.shared.domain.Belt

/**
 * מקור אמת גלובלי להערות אישיות על תרגילים.
 *
 * ההערה שייכת לתרגיל עצמו ולא למסך שממנו הוא נפתח.
 *
 * לכן:
 *
 * לפי חגורה
 * לפי נושא
 * חיפוש
 * תרגול אקראי
 * וכל מסך עתידי
 *
 * יכולים לקרוא ולכתוב את אותה הערה.
 *
 * ------------------------------------------------------------
 *
 * המפתח החדש:
 *
 * exercise_note_<beltId>_<exerciseId>
 *
 * ------------------------------------------------------------
 *
 * קיימת גם תאימות לאחור למפתחות הישנים:
 *
 * note_<beltId>_<topic/subTopic>_<exerciseId>
 *
 * כאשר נמצאת הערה ישנה היא מועברת אוטומטית
 * למפתח הגלובלי החדש.
 */
object ExerciseNotesStore {

    private const val PREFS_NAME =
        "kmi_settings"

    private const val GLOBAL_NOTE_PREFIX =
        "exercise_note_"

    private const val LEGACY_NOTE_PREFIX =
        "note_"

    private fun currentUserId(): String? {
        return FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    /**
     * נרמול בסיסי של מזהה תרגיל.
     *
     * אין כאן שינוי אגרסיבי של ה־ID,
     * כדי לא לפגוע ב־CanonicalIds /
     * ExerciseIdentityRegistry.
     */
    private fun normalizeExerciseId(
        value: String
    ): String {
        return value
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace("\u00A0", " ")
            .trim()
    }

    /**
     * המפתח הגלובלי החדש.
     */
    private fun globalKey(
        userId: String,
        belt: Belt,
        exerciseId: String
    ): String {
        val cleanUserId =
            userId.trim()

        val cleanExerciseId =
            normalizeExerciseId(
                exerciseId
            )

        return buildString {
            append(
                GLOBAL_NOTE_PREFIX
            )
            append(
                cleanUserId
            )
            append("_")
            append(
                belt.id
            )
            append("_")
            append(
                cleanExerciseId
            )
        }
    }

    /**
     * כל המזהים האפשריים של אותו תרגיל.
     *
     * exerciseId הוא המזהה הראשי.
     *
     * aliases מאפשרים למסכים להעביר גם:
     *
     * ExerciseIdentityRegistry id
     * CanonicalIds id
     * explanation id
     *
     * וכך נשמרת תאימות גם לתרגילים
     * שקיבלו בעבר מזהים שונים מעט.
     */
    private fun candidateIds(
        exerciseId: String,
        aliases: Collection<String>
    ): List<String> {
        return buildList {

            add(
                exerciseId
            )

            addAll(
                aliases
            )
        }
            .map { value ->
                normalizeExerciseId(
                    value
                )
            }
            .filter { value ->
                value.isNotBlank()
            }
            .distinct()
    }

    /**
     * מחפש הערה במבנה הישן:
     *
     * note_<belt>_<topic/subtopic>_<exerciseId>
     *
     * לא צריך לדעת מה היה ה־topic בזמן השמירה.
     *
     * אנחנו מחפשים לפי:
     *
     * חגורה + סוף המפתח של מזהה התרגיל.
     */
    private fun findLegacyNote(
        context: Context,
        belt: Belt,
        exerciseIds: Collection<String>
    ): Pair<String, String>? {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val legacyPrefix =
            buildString {
                append(
                    LEGACY_NOTE_PREFIX
                )
                append(
                    belt.id
                )
                append("_")
            }

        val candidates =
            exerciseIds
                .map { value ->
                    normalizeExerciseId(
                        value
                    )
                }
                .filter { value ->
                    value.isNotBlank()
                }
                .distinct()

        if (candidates.isEmpty()) {
            return null
        }

        return prefs
            .all
            .entries
            .asSequence()
            .filter { entry ->

                entry.key.startsWith(
                    legacyPrefix
                )
            }
            .mapNotNull { entry ->

                val note =
                    entry.value
                            as? String
                        ?: return@mapNotNull null

                val cleanNote =
                    note.trim()

                if (cleanNote.isBlank()) {
                    return@mapNotNull null
                }

                val matchesExercise =
                    candidates.any { exerciseId ->

                        entry.key.endsWith(
                            "_$exerciseId"
                        )
                    }

                if (!matchesExercise) {
                    return@mapNotNull null
                }

                entry.key to
                        cleanNote
            }
            .firstOrNull()
    }

    /**
     * טוען הערה גלובלית לתרגיל.
     *
     * סדר הפעולה:
     *
     * 1. מנסה את המפתח החדש.
     * 2. מנסה aliases במפתח החדש.
     * 3. מחפש הערה במבנה הישן.
     * 4. אם נמצאה הערה ישנה -
     *    מעביר אותה אוטומטית למפתח החדש.
     */
    fun loadNote(
        context: Context,
        belt: Belt,
        exerciseId: String,
        aliases: Collection<String> =
            emptyList(),
        allowLegacyMigration: Boolean = false
    ): String {

        val userId =
            currentUserId()
                ?: return ""

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val candidates =
            candidateIds(
                exerciseId = exerciseId,
                aliases = aliases
            )

        if (candidates.isEmpty()) {
            return ""
        }

        /*
         * קודם מחפשים רק הערות ששייכות
         * ל־Firebase UID של המשתמש המחובר.
         */
        candidates.forEach { candidateId ->

            val savedNote =
                prefs.getString(
                    globalKey(
                        userId = userId,
                        belt = belt,
                        exerciseId =
                            candidateId
                    ),
                    ""
                )
                    .orEmpty()
                    .trim()

            if (savedNote.isNotBlank()) {

                /*
                 * אם נמצאה ההערה תחת alias,
                 * נשמור אותה גם תחת המזהה הראשי
                 * של אותו משתמש בלבד.
                 */
                val mainKey =
                    globalKey(
                        userId = userId,
                        belt = belt,
                        exerciseId =
                            exerciseId
                    )

                val foundKey =
                    globalKey(
                        userId = userId,
                        belt = belt,
                        exerciseId =
                            candidateId
                    )

                if (
                    mainKey != foundKey
                ) {
                    prefs.edit {
                        putString(
                            mainKey,
                            savedNote
                        )
                    }
                }

                return savedNote
            }
        }

        /*
         * המפתחות הישנים אינם כוללים UID.
         *
         * לכן אסור לבצע migration אוטומטי
         * אלא אם המסך אישר במפורש שהמשתמש
         * נמצא במצב מתאמן.
         */
        if (!allowLegacyMigration) {
            return ""
        }

        val legacy =
            findLegacyNote(
                context = context,
                belt = belt,
                exerciseIds =
                    candidates
            )
                ?: return ""

        val legacyKey =
            legacy.first

        val legacyNote =
            legacy.second

        /*
         * מעבירים את ההערה הישנה ל־UID
         * של המשתמש המחובר ומסירים את
         * המפתח הישן שאינו מבודד לפי משתמש.
         *
         * כך משתמש אחר לא יוכל לקבל אותה
         * לאחר מכן.
         */
        prefs.edit {
            putString(
                globalKey(
                    userId = userId,
                    belt = belt,
                    exerciseId =
                        exerciseId
                ),
                legacyNote
            )

            remove(
                legacyKey
            )
        }

        return legacyNote
    }

    /**
     * מחזיר true אם קיימת הערה.
     */
    fun hasNote(
        context: Context,
        belt: Belt,
        exerciseId: String,
        aliases: Collection<String> =
            emptyList(),
        allowLegacyMigration: Boolean = false
    ): Boolean {
        return loadNote(
            context = context,
            belt = belt,
            exerciseId = exerciseId,
            aliases = aliases,
            allowLegacyMigration =
                allowLegacyMigration
        ).isNotBlank()
    }

    /**
     * שמירת הערה.
     *
     * אם הטקסט ריק -
     * הפעולה מתנהגת כמו מחיקה.
     */
    fun saveNote(
        context: Context,
        belt: Belt,
        exerciseId: String,
        note: String,
        aliases: Collection<String> =
            emptyList()
    ) {
        val userId =
            currentUserId()
                ?: return

        val cleanNote =
            note.trim()

        if (cleanNote.isBlank()) {
            deleteNote(
                context = context,
                belt = belt,
                exerciseId = exerciseId,
                aliases = aliases
            )

            return
        }

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val candidates =
            candidateIds(
                exerciseId = exerciseId,
                aliases = aliases
            )

        prefs.edit {
            candidates.forEach { candidateId ->
                putString(
                    globalKey(
                        userId = userId,
                        belt = belt,
                        exerciseId =
                            candidateId
                    ),
                    cleanNote
                )
            }
        }
    }

    /**
     * מחיקת הערה.
     *
     * מוחק גם:
     *
     * 1. את המפתח הגלובלי החדש.
     * 2. aliases גלובליים.
     * 3. מפתחות ישנים התואמים לאותו תרגיל.
     *
     * כך הערה שנמחקה במסך אחד
     * לא "תחזור" במסך השני ממפתח ישן.
     */
    fun deleteNote(
        context: Context,
        belt: Belt,
        exerciseId: String,
        aliases: Collection<String> =
            emptyList()
    ) {
        val userId =
            currentUserId()
                ?: return

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val candidates =
            candidateIds(
                exerciseId = exerciseId,
                aliases = aliases
            )

        /*
         * מוחקים רק הערות ששייכות ל־UID
         * של המשתמש המחובר.
         *
         * אין כאן מחיקה של legacy keys,
         * כדי שמשתמש אחד לא ימחק בטעות
         * נתון ישן שאי אפשר לזהות את בעליו.
         */
        prefs.edit {

            candidates.forEach { candidateId ->

                remove(
                    globalKey(
                        userId = userId,
                        belt = belt,
                        exerciseId =
                            candidateId
                    )
                )
            }
        }
    }
}