package il.kmi.app.notes

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import il.kmi.shared.domain.Belt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

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

    private const val SYNCED_NOTE_PREFIX =
        "exercise_note_synced_"

    private const val DELETED_NOTE_PREFIX =
        "exercise_note_deleted_"

    private const val DIRTY_NOTE_PREFIX =
        "exercise_note_dirty_"

    private const val REMOTE_NOTES_COLLECTION =
        "exerciseNotes"

    private const val LOG_TAG =
        "KMI_EXERCISE_NOTES_SYNC"

    private const val MAX_NOTE_LENGTH = 5000

    private const val MAX_EXERCISE_ID_LENGTH = 500

    private val auth =
        FirebaseAuth.getInstance()

    private val firestore =
        FirebaseFirestore.getInstance()

    private val syncScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    private val noteWriteMutexes =
        mutableMapOf<String, Mutex>()

    private val pendingNoteVersions =
        mutableMapOf<String, Long>()

    private var noteChangeVersion = 0L

    private fun isRetryableSyncError(
        error: Throwable
    ): Boolean {
        val firestoreError =
            error as? FirebaseFirestoreException
                ?: return false

        return firestoreError.code in setOf(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
        )
    }

    private fun retryDelayMillis(
        attempt: Long
    ): Long {
        return when {
            attempt <= 0L -> 1_000L
            attempt == 1L -> 2_000L
            attempt == 2L -> 4_000L
            attempt == 3L -> 8_000L
            else -> 15_000L
        }
    }

    private fun currentUserId(): String? {
        return auth
            .currentUser
            ?.uid
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    private fun noteWriteKey(
        uid: String,
        belt: Belt,
        exerciseId: String
    ): String =
        "$uid|${belt.id}|${normalizeExerciseId(exerciseId)}"

    private fun hasPendingNoteWrite(
        uid: String,
        belt: Belt,
        exerciseId: String
    ): Boolean =
        synchronized(noteWriteMutexes) {
            pendingNoteVersions.containsKey(
                noteWriteKey(uid, belt, exerciseId)
            )
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
    private fun remoteDocumentId(
        belt: Belt,
        exerciseId: String
    ): String {
        val rawKey =
            "${belt.id}|${normalizeExerciseId(exerciseId)}"

        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(rawKey.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private fun syncedNoteKey(
        userId: String,
        belt: Belt,
        exerciseId: String
    ): String =
        SYNCED_NOTE_PREFIX +
                userId.trim() + "_" +
                remoteDocumentId(belt, exerciseId)

    private fun deletedNoteKey(
        userId: String,
        belt: Belt,
        exerciseId: String
    ): String =
        DELETED_NOTE_PREFIX +
                userId.trim() + "_" +
                remoteDocumentId(belt, exerciseId)

    private fun dirtyNoteKey(
        userId: String,
        belt: Belt,
        exerciseId: String
    ): String =
        DIRTY_NOTE_PREFIX +
                userId.trim() + "_" +
                remoteDocumentId(belt, exerciseId)

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
    private fun remoteNoteDocument(
        uid: String,
        belt: Belt,
        exerciseId: String
    ) =
        firestore
            .collection("users")
            .document(uid)
            .collection(REMOTE_NOTES_COLLECTION)
            .document(
                remoteDocumentId(
                    belt = belt,
                    exerciseId = exerciseId
                )
            )

    suspend fun setRemoteNote(
        uid: String,
        belt: Belt,
        exerciseId: String,
        note: String
    ) {
        check(currentUserId() == uid) {
            "Exercise notes account changed"
        }

        val normalizedId =
            normalizeExerciseId(exerciseId)

        require(
            normalizedId.isNotBlank() &&
                    normalizedId.length <= MAX_EXERCISE_ID_LENGTH
        ) {
            "Invalid exercise note ID"
        }

        val cleanNote = note.trim()

        require(
            cleanNote.isNotBlank() &&
                    cleanNote.length <= MAX_NOTE_LENGTH
        ) {
            "Invalid exercise note length"
        }

        remoteNoteDocument(
            uid = uid,
            belt = belt,
            exerciseId = normalizedId
        ).set(
            mapOf(
                "beltId" to belt.id,
                "exerciseId" to normalizedId,
                "note" to cleanNote,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteRemoteNote(
        uid: String,
        belt: Belt,
        exerciseId: String
    ) {
        check(currentUserId() == uid) {
            "Exercise notes account changed"
        }

        val normalizedId =
            normalizeExerciseId(exerciseId)

        require(
            normalizedId.isNotBlank() &&
                    normalizedId.length <= MAX_EXERCISE_ID_LENGTH
        ) {
            "Invalid exercise note ID"
        }

        remoteNoteDocument(
            uid = uid,
            belt = belt,
            exerciseId = normalizedId
        ).delete().await()
    }

    private fun markNoteSynced(
        context: Context,
        uid: String,
        belt: Belt,
        exerciseId: String
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit {
                putBoolean(
                    syncedNoteKey(uid, belt, exerciseId),
                    true
                )
            }
    }

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
    fun observeRemoteNote(
        uid: String,
        belt: Belt,
        exerciseId: String
    ): Flow<String?> = callbackFlow {
        check(currentUserId() == uid) {
            "Exercise notes account changed"
        }

        val registration =
            remoteNoteDocument(
                uid = uid,
                belt = belt,
                exerciseId = exerciseId
            ).addSnapshotListener(
                MetadataChanges.INCLUDE
            ) { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (currentUserId() != uid) {
                    return@addSnapshotListener
                }

                if (
                    snapshot?.metadata?.hasPendingWrites() == true
                ) {
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                // נתון מה-Cache אינו אישור למצב השרת.
                // הערה מקומית נשארת זמינה עד לקבלת
                // Snapshot מאומת מהשרת.
                if (snapshot.metadata.isFromCache) {
                    return@addSnapshotListener
                }

                trySend(
                    snapshot.getString("note")?.trim()
                )
            }

        awaitClose {
            registration.remove()
        }
    }

    private fun queueRemoteNoteChange(
        context: Context,
        uid: String,
        belt: Belt,
        exerciseId: String,
        note: String?
    ) {
        val normalizedId =
            normalizeExerciseId(exerciseId)

        if (
            normalizedId.isBlank() ||
            normalizedId.length > MAX_EXERCISE_ID_LENGTH
        ) {
            Log.w(LOG_TAG, "Invalid exercise note ID")
            return
        }

        val writeKey =
            noteWriteKey(uid, belt, normalizedId)

        val mutex: Mutex
        val version: Long

        synchronized(noteWriteMutexes) {
            mutex = noteWriteMutexes.getOrPut(writeKey) {
                Mutex()
            }

            noteChangeVersion++
            version = noteChangeVersion

            pendingNoteVersions[writeKey] = version

            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            ).edit {
                putBoolean(
                    dirtyNoteKey(
                        userId = uid,
                        belt = belt,
                        exerciseId = normalizedId
                    ),
                    true
                )
            }
        }

        syncScope.launch {
            mutex.withLock {
                try {
                    if (currentUserId() != uid) {
                        return@withLock
                    }

                    val isLatest =
                        synchronized(noteWriteMutexes) {
                            pendingNoteVersions[writeKey] == version
                        }

                    if (!isLatest) {
                        return@withLock
                    }

                    var attempt = 0L

                    while (true) {
                        if (currentUserId() != uid) {
                            return@withLock
                        }

                        val stillLatest =
                            synchronized(noteWriteMutexes) {
                                pendingNoteVersions[writeKey] ==
                                        version
                            }

                        if (!stillLatest) {
                            return@withLock
                        }

                        try {
                            if (note == null) {
                                deleteRemoteNote(
                                    uid = uid,
                                    belt = belt,
                                    exerciseId = normalizedId
                                )
                            } else {
                                setRemoteNote(
                                    uid = uid,
                                    belt = belt,
                                    exerciseId = normalizedId,
                                    note = note
                                )
                            }

                            break
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            if (
                                !isRetryableSyncError(error) ||
                                attempt >= 4L
                            ) {
                                throw error
                            }

                            val retryDelay =
                                retryDelayMillis(attempt)

                            Log.w(
                                LOG_TAG,
                                "Note write retry: belt=${belt.id} id=$normalizedId attempt=${attempt + 1}",
                                error
                            )

                            delay(retryDelay)

                            attempt++
                        }
                    }

                    synchronized(noteWriteMutexes) {
                        if (
                            currentUserId() == uid &&
                            pendingNoteVersions[writeKey] == version
                        ) {
                            markNoteSynced(
                                context = context,
                                uid = uid,
                                belt = belt,
                                exerciseId = normalizedId
                            )

                            context.getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                            ).edit {
                                remove(
                                    dirtyNoteKey(
                                        userId = uid,
                                        belt = belt,
                                        exerciseId = normalizedId
                                    )
                                )
                            }
                        }
                    }

                    Log.d(
                        LOG_TAG,
                        "Note synced: belt=${belt.id} id=$normalizedId"
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e(
                        LOG_TAG,
                        "Note sync failed: belt=${belt.id} id=$normalizedId",
                        error
                    )
                } finally {
                    synchronized(noteWriteMutexes) {
                        if (
                            pendingNoteVersions[writeKey] == version
                        ) {
                            pendingNoteVersions.remove(writeKey)
                        }
                    }
                }
            }
        }
    }

    fun observeNote(
        context: Context,
        belt: Belt,
        exerciseId: String,
        aliases: Collection<String> = emptyList(),
        allowLegacyMigration: Boolean = false
    ): Flow<String> = flow {
        val uid = currentUserId()

        val localNote = loadNote(
            context = context,
            belt = belt,
            exerciseId = exerciseId,
            aliases = aliases,
            allowLegacyMigration = allowLegacyMigration
        )

        emit(localNote)

        if (uid == null) {
            return@flow
        }

        val normalizedId =
            normalizeExerciseId(exerciseId)

        if (normalizedId.isBlank()) {
            return@flow
        }

        val prefs =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        val candidates =
            candidateIds(
                exerciseId = normalizedId,
                aliases = aliases
            )

        observeRemoteNote(
            uid = uid,
            belt = belt,
            exerciseId = normalizedId
        ).retryWhen { error, attempt ->

            if (
                currentUserId() != uid ||
                !isRetryableSyncError(error)
            ) {
                return@retryWhen false
            }

            val retryDelay =
                retryDelayMillis(attempt)

            Log.w(
                LOG_TAG,
                "Note listener retry: belt=${belt.id} id=$normalizedId attempt=${attempt + 1}",
                error
            )

            delay(retryDelay)

            currentUserId() == uid
        }.catch { error ->
            if (error is CancellationException) {
                throw error
            }

            Log.e(
                LOG_TAG,
                "Note listener failed: belt=${belt.id} id=$normalizedId",
                error
            )
        }.collect { remoteNote ->

            if (currentUserId() != uid) {
                return@collect
            }

            if (
                hasPendingNoteWrite(
                    uid = uid,
                    belt = belt,
                    exerciseId = normalizedId
                )
            ) {
                return@collect
            }

            val cleanRemoteNote =
                remoteNote?.trim()

            val isDirty =
                prefs.getBoolean(
                    dirtyNoteKey(
                        userId = uid,
                        belt = belt,
                        exerciseId = normalizedId
                    ),
                    false
                )

            if (isDirty) {
                val wasDeletedLocally =
                    prefs.getBoolean(
                        deletedNoteKey(
                            userId = uid,
                            belt = belt,
                            exerciseId = normalizedId
                        ),
                        false
                    )

                val localText =
                    candidates
                        .asSequence()
                        .mapNotNull { candidateId ->
                            prefs.getString(
                                globalKey(
                                    userId = uid,
                                    belt = belt,
                                    exerciseId = candidateId
                                ),
                                null
                            )?.trim()?.takeIf {
                                it.isNotBlank()
                            }
                        }
                        .firstOrNull()

                val alreadySynced =
                    if (wasDeletedLocally) {
                        cleanRemoteNote.isNullOrBlank()
                    } else {
                        !localText.isNullOrBlank() &&
                                localText == cleanRemoteNote
                    }

                if (alreadySynced) {
                    synchronized(noteWriteMutexes) {
                        if (
                            !hasPendingNoteWrite(
                                uid = uid,
                                belt = belt,
                                exerciseId = normalizedId
                            )
                        ) {
                            prefs.edit {
                                remove(
                                    dirtyNoteKey(
                                        userId = uid,
                                        belt = belt,
                                        exerciseId = normalizedId
                                    )
                                )

                                putBoolean(
                                    syncedNoteKey(
                                        userId = uid,
                                        belt = belt,
                                        exerciseId = normalizedId
                                    ),
                                    true
                                )
                            }
                        }
                    }
                } else {
                    // השינוי המקומי טרם הגיע לשרת.
                    // אין לדרוס אותו בנתון הישן.
                    if (
                        wasDeletedLocally ||
                        !localText.isNullOrBlank()
                    ) {
                        queueRemoteNoteChange(
                            context = context.applicationContext,
                            uid = uid,
                            belt = belt,
                            exerciseId = normalizedId,
                            note =
                                if (wasDeletedLocally) {
                                    null
                                } else {
                                    localText
                                }
                        )
                    }

                    return@collect
                }
            }

            if (!cleanRemoteNote.isNullOrBlank()) {
                prefs.edit {
                    remove(
                        deletedNoteKey(
                            userId = uid,
                            belt = belt,
                            exerciseId = normalizedId
                        )
                    )

                    candidates.forEach { candidateId ->
                        putString(
                            globalKey(
                                userId = uid,
                                belt = belt,
                                exerciseId = candidateId
                            ),
                            cleanRemoteNote
                        )
                    }

                    putBoolean(
                        syncedNoteKey(
                            userId = uid,
                            belt = belt,
                            exerciseId = normalizedId
                        ),
                        true
                    )
                }

                emit(cleanRemoteNote)

                return@collect
            }

            val wasPreviouslySynced =
                prefs.getBoolean(
                    syncedNoteKey(
                        userId = uid,
                        belt = belt,
                        exerciseId = normalizedId
                    ),
                    false
                )

            if (!wasPreviouslySynced) {
                // אין אישור שהייתה בעבר הערה בענן.
                // נשמרת הערה מקומית שטרם סונכרנה.
                return@collect
            }

            // המסמך היה מוכר בעבר לענן וכעת חסר.
            // מסירים עותקים מקומיים כדי למנוע
            // חזרת הערה דרך alias ישן.
            prefs.edit {
                putBoolean(
                    deletedNoteKey(
                        userId = uid,
                        belt = belt,
                        exerciseId = normalizedId
                    ),
                    true
                )

                candidates.forEach { candidateId ->
                    remove(
                        globalKey(
                            userId = uid,
                            belt = belt,
                            exerciseId = candidateId
                        )
                    )
                }
            }

            Log.d(
                LOG_TAG,
                "Remote note deletion applied: belt=${belt.id} id=$normalizedId"
            )

            emit("")
        }
    }

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

        if (
            prefs.getBoolean(
                deletedNoteKey(
                    userId = userId,
                    belt = belt,
                    exerciseId = exerciseId
                ),
                false
            )
        ) {
            return ""
        }

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

                    // מיגרציה מקומית בלבד.
                    // אין לדרוס בענן הערה חדשה יותר
                    // בעקבות קריאה של alias ישן.
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

        // הערת Legacy נשמרת מקומית תחת ה-UID.
        // העלאה לענן תבוצע בעריכה או בשמירה
        // מפורשת, כדי לא לדרוס מידע מרוחק.

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

        if (cleanNote.length > MAX_NOTE_LENGTH) {
            Log.w(
                LOG_TAG,
                "Note exceeds maximum length: ${cleanNote.length}"
            )
            return
        }

        if (cleanNote.isBlank()) {
            deleteNote(
                context = context,
                belt = belt,
                exerciseId = exerciseId,
                aliases = aliases
            )

            return
        }

        val normalizedId =
            normalizeExerciseId(exerciseId)

        if (
            normalizedId.isBlank() ||
            normalizedId.length > MAX_EXERCISE_ID_LENGTH
        ) {
            Log.w(
                LOG_TAG,
                "Save skipped: invalid exercise ID"
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
            putBoolean(
                dirtyNoteKey(
                    userId = userId,
                    belt = belt,
                    exerciseId = exerciseId
                ),
                true
            )

            remove(
                deletedNoteKey(
                    userId = userId,
                    belt = belt,
                    exerciseId = exerciseId
                )
            )

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

        queueRemoteNoteChange(
            context = context.applicationContext,
            uid = userId,
            belt = belt,
            exerciseId = exerciseId,
            note = cleanNote
        )
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

        val normalizedId =
            normalizeExerciseId(exerciseId)

        if (
            normalizedId.isBlank() ||
            normalizedId.length > MAX_EXERCISE_ID_LENGTH
        ) {
            Log.w(
                LOG_TAG,
                "Delete skipped: invalid exercise ID"
            )
            return
        }

        val candidates =
            candidateIds(
                exerciseId = normalizedId,
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

            putBoolean(
                dirtyNoteKey(
                    userId = userId,
                    belt = belt,
                    exerciseId = normalizedId
                ),
                true
            )

            putBoolean(
                deletedNoteKey(
                    userId = userId,
                    belt = belt,
                    exerciseId = normalizedId
                ),
                true
            )

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

        queueRemoteNoteChange(
            context = context.applicationContext,
            uid = userId,
            belt = belt,
            exerciseId = exerciseId,
            note = null
        )
    }
}