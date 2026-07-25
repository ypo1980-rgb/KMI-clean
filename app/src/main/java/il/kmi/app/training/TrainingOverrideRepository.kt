package il.kmi.app.training

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.MessageDigest
import java.util.Locale

/**
 * סוג השינוי שבוצע במופע אימון מסוים.
 *
 * CANCELLED:
 * האימון בוטל לחלוטין.
 *
 * TIME_CHANGED:
 * האימון מתקיים, אך בשעה שונה.
 */
enum class TrainingOverrideType(
    val firestoreValue: String
) {
    CANCELLED("cancelled"),
    TIME_CHANGED("time_changed");

    companion object {
        fun fromFirestoreValue(
            rawValue: String?
        ): TrainingOverrideType? {
            return when (
                rawValue
                    ?.trim()
                    ?.lowercase(Locale.US)
            ) {
                CANCELLED.firestoreValue ->
                    CANCELLED

                TIME_CHANGED.firestoreValue ->
                    TIME_CHANGED

                else ->
                    null
            }
        }
    }
}

/**
 * שינוי פעיל או היסטורי של מופע אימון יחיד.
 *
 * occurrenceKey מתייחס תמיד לאימון המקורי.
 * גם כאשר שעת האימון משתנה, המפתח אינו משתנה.
 */
data class TrainingOverride(
    val documentId: String,
    val occurrenceKey: String,

    val type: TrainingOverrideType,

    val branch: String,
    val group: String,
    val place: String,
    val address: String,
    val coachName: String,

    val originalStartMillis: Long,
    val originalEndMillis: Long,

    val newStartMillis: Long?,
    val newEndMillis: Long?,

    val reason: String,

    val changedByUid: String,
    val changedByName: String,

    val isActive: Boolean,

    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,

    val notificationRequested: Boolean,
    val notificationStatus: String
) {

    val isCancelled: Boolean
        get() =
            isActive &&
                    type ==
                    TrainingOverrideType.CANCELLED

    val hasChangedTime: Boolean
        get() =
            isActive &&
                    type ==
                    TrainingOverrideType.TIME_CHANGED &&
                    newStartMillis != null &&
                    newEndMillis != null &&
                    newEndMillis > newStartMillis

    /**
     * זמן ההתחלה שבו יש להשתמש בפועל.
     */
    val effectiveStartMillis: Long
        get() =
            if (hasChangedTime) {
                newStartMillis
                    ?: originalStartMillis
            } else {
                originalStartMillis
            }

    /**
     * זמן הסיום שבו יש להשתמש בפועל.
     */
    val effectiveEndMillis: Long
        get() =
            if (hasChangedTime) {
                newEndMillis
                    ?: originalEndMillis
            } else {
                originalEndMillis
            }
}

/**
 * ידית שמרכזת מספר מאזיני Firestore.
 *
 * במסך הבית יהיה מאזין נפרד לכל מופע אימון,
 * אך כל המאזינים ייסגרו יחד כאשר המסך נסגר.
 */
class TrainingOverrideListenerHandle internal constructor(
    private val registrations: List<ListenerRegistration>
) {
    fun remove() {
        registrations.forEach { registration ->
            runCatching {
                registration.remove()
            }
        }
    }
}

/**
 * שכבת הגישה המרכזית לשינויים באימונים.
 *
 * אין להכניס לוגיקת Firestore ישירות לתוך HomeScreen.
 * מסך הבית יעבוד רק מול המחלקה הזאת.
 */
object TrainingOverrideRepository {

    private const val COLLECTION_NAME =
        "trainingOverrides"

    private const val NOTIFICATION_STATUS_PENDING =
        "pending"

    private const val SOURCE_ANDROID =
        "android_training_override"

    private val auth: FirebaseAuth
        get() =
            FirebaseAuth.getInstance()

    private val firestore: FirebaseFirestore
        get() =
            FirebaseFirestore.getInstance()

    /**
     * יצירת מפתח קבוע למופע אימון.
     *
     * המפתח כולל את זמן ההתחלה המקורי.
     * שינוי שעה עתידי אינו משנה את זהות האימון.
     */
    fun buildOccurrenceKey(
        training: TrainingData,
        branch: String,
        group: String
    ): String {
        return buildOccurrenceKey(
            branch = branch,
            group = group,
            place = training.place.orEmpty(),
            address = training.address.orEmpty(),
            coachName = training.coach.orEmpty(),
            originalStartMillis =
                training.startMillis,
            originalEndMillis =
                training.endMillis
                    ?: training.startMillis
        )
    }

    fun buildOccurrenceKey(
        branch: String,
        group: String,
        place: String,
        address: String,
        coachName: String,
        originalStartMillis: Long,
        originalEndMillis: Long
    ): String {
        return listOf(
            normalizeIdentityPart(branch),
            normalizeIdentityPart(group),
            normalizeIdentityPart(place),
            normalizeIdentityPart(address),
            normalizeIdentityPart(coachName),
            originalStartMillis.toString(),
            originalEndMillis.toString()
        ).joinToString("|")
    }

    /**
     * מזהה מסמך בטוח לשימוש ב־Firestore.
     *
     * לא משתמשים ישירות ב־occurrenceKey כמזהה מסמך,
     * משום שכתובות או שמות עלולים להכיל תווים בעייתיים.
     */
    fun documentIdForOccurrenceKey(
        occurrenceKey: String
    ): String {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    occurrenceKey
                        .toByteArray(
                            Charsets.UTF_8
                        )
                )

        return digest.joinToString("") { byte ->
            "%02x".format(
                byte.toInt() and 0xFF
            )
        }
    }

    /**
     * ביטול מופע אימון יחיד.
     */
    fun cancelTraining(
        training: TrainingData,
        branch: String,
        group: String,
        reason: String,
        changedByName: String,
        onResult: (
            success: Boolean,
            error: Throwable?
        ) -> Unit
    ) {
        val cleanReason =
            reason.trim()

        if (cleanReason.length < 3) {
            onResult(
                false,
                IllegalArgumentException(
                    "Cancellation reason is too short"
                )
            )
            return
        }

        saveOverride(
            training = training,
            branch = branch,
            group = group,
            type =
                TrainingOverrideType.CANCELLED,
            reason = cleanReason,
            changedByName = changedByName,
            newStartMillis = null,
            newEndMillis = null,
            onResult = onResult
        )
    }

    /**
     * שינוי שעת מופע אימון יחיד.
     */
    fun changeTrainingTime(
        training: TrainingData,
        branch: String,
        group: String,
        newStartMillis: Long,
        newEndMillis: Long,
        reason: String,
        changedByName: String,
        onResult: (
            success: Boolean,
            error: Throwable?
        ) -> Unit
    ) {
        if (newStartMillis <= 0L) {
            onResult(
                false,
                IllegalArgumentException(
                    "Invalid new training start time"
                )
            )
            return
        }

        if (newEndMillis <= newStartMillis) {
            onResult(
                false,
                IllegalArgumentException(
                    "New training end time must be after start time"
                )
            )
            return
        }

        val cleanReason =
            reason
                .trim()
                .ifBlank {
                    "Training time changed"
                }

        saveOverride(
            training = training,
            branch = branch,
            group = group,
            type =
                TrainingOverrideType.TIME_CHANGED,
            reason = cleanReason,
            changedByName = changedByName,
            newStartMillis =
                newStartMillis,
            newEndMillis =
                newEndMillis,
            onResult = onResult
        )
    }

    /**
     * ביטול השינוי והחזרת האימון להגדרה המקורית.
     *
     * המסמך אינו נמחק כדי לשמור היסטוריה.
     */
    fun restoreOriginalTraining(
        training: TrainingData,
        branch: String,
        group: String,
        changedByName: String,
        onResult: (
            success: Boolean,
            error: Throwable?
        ) -> Unit
    ) {
        val currentUid =
            auth.currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        if (currentUid.isBlank()) {
            onResult(
                false,
                IllegalStateException(
                    "No signed-in user"
                )
            )
            return
        }

        val occurrenceKey =
            buildOccurrenceKey(
                training = training,
                branch = branch,
                group = group
            )

        val documentId =
            documentIdForOccurrenceKey(
                occurrenceKey
            )

        val documentReference =
            firestore
                .collection(
                    COLLECTION_NAME
                )
                .document(
                    documentId
                )

        val updateData =
            hashMapOf<String, Any>(
                "isActive" to false,
                "restoredByUid" to currentUid,
                "restoredByName" to
                        changedByName
                            .trim()
                            .ifBlank {
                                auth.currentUser
                                    ?.displayName
                                    ?.trim()
                                    .orEmpty()
                            },
                "restoredAt" to
                        FieldValue.serverTimestamp(),
                "updatedAt" to
                        FieldValue.serverTimestamp(),
                "notificationRequested" to true,
                "notificationStatus" to
                        NOTIFICATION_STATUS_PENDING,
                "source" to SOURCE_ANDROID
            )

        documentReference
            .update(updateData)
            .addOnSuccessListener {
                onResult(
                    true,
                    null
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error
                )
            }
    }

    /**
     * האזנה לשינויים של קבוצת אימונים מסוימת.
     *
     * לכל occurrenceKey נפתח Listener למסמך המדויק שלו.
     * כך אין צורך להוריד את כל אוסף השינויים.
     */
    fun listenForOccurrenceKeys(
        occurrenceKeys: Set<String>,
        onChanged: (
            Map<String, TrainingOverride>
        ) -> Unit,
        onError: (
            Throwable
        ) -> Unit = {}
    ): TrainingOverrideListenerHandle {

        val cleanOccurrenceKeys =
            occurrenceKeys
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        if (cleanOccurrenceKeys.isEmpty()) {
            onChanged(emptyMap())

            return TrainingOverrideListenerHandle(
                emptyList()
            )
        }

        val overridesByOccurrenceKey =
            mutableMapOf<String, TrainingOverride>()

        val lock =
            Any()

        val registrations =
            cleanOccurrenceKeys.map { occurrenceKey ->

                val documentId =
                    documentIdForOccurrenceKey(
                        occurrenceKey
                    )

                firestore
                    .collection(
                        COLLECTION_NAME
                    )
                    .document(
                        documentId
                    )
                    .addSnapshotListener {
                            snapshot,
                            error ->

                        if (error != null) {
                            onError(error)
                            return@addSnapshotListener
                        }

                        synchronized(lock) {
                            val parsed =
                                snapshot
                                    ?.takeIf {
                                        it.exists()
                                    }
                                    ?.toTrainingOverride()

                            if (
                                parsed != null &&
                                parsed.isActive
                            ) {
                                overridesByOccurrenceKey[
                                    occurrenceKey
                                ] = parsed
                            } else {
                                overridesByOccurrenceKey
                                    .remove(
                                        occurrenceKey
                                    )
                            }

                            onChanged(
                                overridesByOccurrenceKey
                                    .toMap()
                            )
                        }
                    }
            }

        return TrainingOverrideListenerHandle(
            registrations
        )
    }

    /**
     * האזנה לכל שינויי האימונים בטווח תאריכים מסוים.
     *
     * מיועד בעיקר למסך ארכיון האימונים, שבו עשויים להיות
     * עשרות או מאות מופעי אימון. לכן משתמשים במאזין Firestore
     * יחיד לטווח כולו ולא במאזין נפרד לכל occurrenceKey.
     *
     * הסינון לפי isActive נעשה בצד האפליקציה כדי להימנע
     * מדרישה לאינדקס Firestore מורכב נוסף.
     */
    fun listenForOverridesInRange(
        fromOriginalStartMillis: Long,
        toOriginalStartMillis: Long,
        onChanged: (
            Map<String, TrainingOverride>
        ) -> Unit,
        onError: (
            Throwable
        ) -> Unit = {}
    ): TrainingOverrideListenerHandle {

        if (
            fromOriginalStartMillis <= 0L ||
            toOriginalStartMillis <= 0L ||
            toOriginalStartMillis < fromOriginalStartMillis
        ) {
            onChanged(emptyMap())

            onError(
                IllegalArgumentException(
                    "Invalid training override date range"
                )
            )

            return TrainingOverrideListenerHandle(
                emptyList()
            )
        }

        val registration =
            firestore
                .collection(
                    COLLECTION_NAME
                )
                .whereGreaterThanOrEqualTo(
                    "originalStartMillis",
                    fromOriginalStartMillis
                )
                .whereLessThanOrEqualTo(
                    "originalStartMillis",
                    toOriginalStartMillis
                )
                .addSnapshotListener {
                        snapshot,
                        error ->

                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }

                    val overrides =
                        snapshot
                            ?.documents
                            .orEmpty()
                            .mapNotNull { document ->
                                document
                                    .toTrainingOverride()
                                    ?.takeIf {
                                        it.isActive
                                    }
                            }
                            .associateBy { override ->
                                override.occurrenceKey
                            }

                    onChanged(overrides)
                }

        return TrainingOverrideListenerHandle(
            listOf(registration)
        )
    }

    /**
     * קריאה חד־פעמית של שינוי לאימון מסוים.
     *
     * ישמש בהמשך את מנגנון ההתראות המקומיות.
     */
    fun getOverride(
        occurrenceKey: String,
        onResult: (
            override: TrainingOverride?,
            error: Throwable?
        ) -> Unit
    ) {
        val cleanOccurrenceKey =
            occurrenceKey.trim()

        if (cleanOccurrenceKey.isBlank()) {
            onResult(
                null,
                IllegalArgumentException(
                    "Missing occurrence key"
                )
            )
            return
        }

        val documentId =
            documentIdForOccurrenceKey(
                cleanOccurrenceKey
            )

        firestore
            .collection(
                COLLECTION_NAME
            )
            .document(
                documentId
            )
            .get()
            .addOnSuccessListener { snapshot ->
                val parsed =
                    snapshot
                        .takeIf {
                            it.exists()
                        }
                        ?.toTrainingOverride()
                        ?.takeIf {
                            it.isActive
                        }

                onResult(
                    parsed,
                    null
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    null,
                    error
                )
            }
    }

    private fun saveOverride(
        training: TrainingData,
        branch: String,
        group: String,
        type: TrainingOverrideType,
        reason: String,
        changedByName: String,
        newStartMillis: Long?,
        newEndMillis: Long?,
        onResult: (
            success: Boolean,
            error: Throwable?
        ) -> Unit
    ) {
        val currentUser =
            auth.currentUser

        val currentUid =
            currentUser
                ?.uid
                ?.trim()
                .orEmpty()

        if (currentUid.isBlank()) {
            onResult(
                false,
                IllegalStateException(
                    "No signed-in user"
                )
            )
            return
        }

        val cleanBranch =
            branch.trim()

        val cleanGroup =
            group.trim()

        if (cleanBranch.isBlank()) {
            onResult(
                false,
                IllegalArgumentException(
                    "Missing training branch"
                )
            )
            return
        }

        if (cleanGroup.isBlank()) {
            onResult(
                false,
                IllegalArgumentException(
                    "Missing training group"
                )
            )
            return
        }

        val originalStartMillis =
            training.startMillis

        val originalEndMillis =
            training.endMillis
                ?.takeIf {
                    it > originalStartMillis
                }
                ?: originalStartMillis +
                DEFAULT_TRAINING_DURATION_MILLIS

        val occurrenceKey =
            buildOccurrenceKey(
                branch = cleanBranch,
                group = cleanGroup,
                place =
                    training.place.orEmpty(),
                address =
                    training.address.orEmpty(),
                coachName =
                    training.coach.orEmpty(),
                originalStartMillis =
                    originalStartMillis,
                originalEndMillis =
                    originalEndMillis
            )

        val documentId =
            documentIdForOccurrenceKey(
                occurrenceKey
            )

        val cleanChangedByName =
            changedByName
                .trim()
                .ifBlank {
                    currentUser
                        ?.displayName
                        ?.trim()
                        .orEmpty()
                }
                .ifBlank {
                    currentUser
                        ?.email
                        ?.trim()
                        .orEmpty()
                }
                .ifBlank {
                    "מאמן"
                }

        val documentReference =
            firestore
                .collection(
                    COLLECTION_NAME
                )
                .document(
                    documentId
                )

        firestore.runTransaction { transaction ->
            val existingSnapshot =
                transaction.get(
                    documentReference
                )

            val data =
                hashMapOf<String, Any?>(
                    "overrideId" to documentId,
                    "occurrenceKey" to
                            occurrenceKey,

                    "type" to
                            type.firestoreValue,

                    "branch" to
                            cleanBranch,
                    "group" to
                            cleanGroup,
                    "place" to
                            training.place
                                .orEmpty()
                                .trim(),
                    "address" to
                            training.address
                                .orEmpty()
                                .trim(),
                    "coachName" to
                            training.coach
                                .orEmpty()
                                .trim(),

                    "originalStartMillis" to
                            originalStartMillis,
                    "originalEndMillis" to
                            originalEndMillis,

                    "newStartMillis" to
                            newStartMillis,
                    "newEndMillis" to
                            newEndMillis,

                    "reason" to
                            reason.trim(),

                    "changedByUid" to
                            currentUid,
                    "changedByName" to
                            cleanChangedByName,

                    "isActive" to true,

                    "updatedAt" to
                            FieldValue.serverTimestamp(),

                    "notificationRequested" to true,
                    "notificationStatus" to
                            NOTIFICATION_STATUS_PENDING,

                    "source" to SOURCE_ANDROID
                )

            if (!existingSnapshot.exists()) {
                data["createdAt"] =
                    FieldValue.serverTimestamp()
            }

            transaction.set(
                documentReference,
                data.filterValues {
                    it != null
                },
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
            .addOnSuccessListener {
                onResult(
                    true,
                    null
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error
                )
            }
    }

    private fun DocumentSnapshot.toTrainingOverride():
            TrainingOverride? {

        val occurrenceKey =
            getString(
                "occurrenceKey"
            )
                ?.trim()
                .orEmpty()

        if (occurrenceKey.isBlank()) {
            return null
        }

        val type =
            TrainingOverrideType
                .fromFirestoreValue(
                    getString("type")
                )
                ?: return null

        val originalStartMillis =
            getLong(
                "originalStartMillis"
            )
                ?: return null

        val originalEndMillis =
            getLong(
                "originalEndMillis"
            )
                ?: return null

        return TrainingOverride(
            documentId = id,
            occurrenceKey =
                occurrenceKey,

            type = type,

            branch =
                getString("branch")
                    .orEmpty(),
            group =
                getString("group")
                    .orEmpty(),
            place =
                getString("place")
                    .orEmpty(),
            address =
                getString("address")
                    .orEmpty(),
            coachName =
                getString("coachName")
                    .orEmpty(),

            originalStartMillis =
                originalStartMillis,
            originalEndMillis =
                originalEndMillis,

            newStartMillis =
                getLong(
                    "newStartMillis"
                ),
            newEndMillis =
                getLong(
                    "newEndMillis"
                ),

            reason =
                getString("reason")
                    .orEmpty(),

            changedByUid =
                getString(
                    "changedByUid"
                )
                    .orEmpty(),
            changedByName =
                getString(
                    "changedByName"
                )
                    .orEmpty(),

            isActive =
                getBoolean(
                    "isActive"
                )
                    ?: false,

            createdAt =
                getTimestamp(
                    "createdAt"
                ),
            updatedAt =
                getTimestamp(
                    "updatedAt"
                ),

            notificationRequested =
                getBoolean(
                    "notificationRequested"
                )
                    ?: false,

            notificationStatus =
                getString(
                    "notificationStatus"
                )
                    .orEmpty()
        )
    }

    private fun normalizeIdentityPart(
        rawValue: String
    ): String {
        return rawValue
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(
                Regex("\\s+"),
                " "
            )
            .lowercase(
                Locale("he", "IL")
            )
    }

    private const val DEFAULT_TRAINING_DURATION_MILLIS =
        90L * 60L * 1000L
}