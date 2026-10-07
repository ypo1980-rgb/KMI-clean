package il.kmi.app.exercises.sync

import il.kmi.app.domain.CanonicalIds
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.content.ExerciseIdentityRegistry
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter

/**
 * זהות גלובלית אחידה של תרגיל.
 *
 * כל מסך באפליקציה אמור להגיע בסופו של דבר
 * לאותה זהות עבור אותו תרגיל:
 *
 * לפי חגורה
 * לפי נושא
 * רשימות
 * סיכום
 * תרגול אקראי
 * מבחן מסכם
 * תרגול לפי נושאים
 */
data class ExerciseSyncIdentity(
    val belt: Belt,
    val topic: String,
    val subTopic: String?,
    val rawItem: String,
    val displayTitle: String,
    val exerciseId: String,
    val aliases: Set<String>
)

/**
 * מקור אמת גלובלי לזהות של תרגילים.
 *
 * בשלב הראשון הקובץ אינו שומר מידע.
 * הוא רק מבטיח שכל המסכים יחשבו
 * את אותה זהות ואת אותם aliases.
 */
object ExerciseSyncStore {

    /**
     * ניקוי בסיסי של טקסט.
     */
    private fun cleanText(
        value: String
    ): String {
        return value
            .replace(
                "\u200F",
                ""
            )
            .replace(
                "\u200E",
                ""
            )
            .replace(
                "\u00A0",
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    /**
     * נרמול לצורך השוואת שמות.
     *
     * לא מיועד להצגה למשתמש.
     */
    fun normalize(
        value: String
    ): String {
        return cleanText(
            value
        )
            .replace(
                Regex("[\u0591-\u05C7]"),
                ""
            )
            .replace(
                '־',
                '-'
            )
            .replace(
                '–',
                '-'
            )
            .replace(
                '—',
                '-'
            )
            .replace(
                Regex("\\s*-\\s*"),
                "-"
            )
            .lowercase()
    }

    /**
     * שם תצוגה נקי לתרגיל.
     *
     * קודם מסירים prefix של הנושא אם הוא קיים,
     * ורק לאחר מכן מעבירים ל־ExerciseTitleFormatter.
     */
    fun displayTitle(
        topic: String,
        rawItem: String
    ): String {
        val cleanTopic =
            cleanText(
                topic
            )

        var cleanRaw =
            cleanText(
                rawItem
            )

        if (
            cleanTopic.isNotBlank() &&
            cleanRaw.startsWith(
                "$cleanTopic::"
            )
        ) {
            cleanRaw =
                cleanRaw
                    .removePrefix(
                        "$cleanTopic::"
                    )
                    .trim()
        }

        if (
            cleanTopic.isNotBlank() &&
            cleanRaw.startsWith(
                cleanTopic
            )
        ) {
            cleanRaw =
                cleanRaw
                    .removePrefix(
                        cleanTopic
                    )
                    .trim()
                    .trimStart(
                        '-',
                        '–',
                        '—',
                        ':'
                    )
                    .trim()
        }

        val formatted =
            ExerciseTitleFormatter
                .displayName(
                    cleanRaw
                )
                .trim()

        return formatted
            .takeIf {
                it.isNotBlank() &&
                        it != "null"
            }
            ?: cleanRaw
    }

    /**
     * מפתח topic מדויק ל־Registry.
     *
     * ה־exerciseId הראשי משתמש תמיד בנושא האב בלבד.
     *
     * הסיבה:
     * אותו תרגיל צריך לקבל אותו מזהה גם אם מסך אחד
     * הגיע אליו דרך נושא ומסך אחר דרך תת־נושא.
     *
     * מזהה עם תת־נושא נכנס ל־aliases לצורך תאימות.
     */
    private fun primaryTopicKey(
        topic: String
    ): String {
        return cleanText(
            topic
        )
    }

    /**
     * topicKey מפורט יותר עבור alias נוסף.
     */
    private fun detailedTopicKey(
        topic: String,
        subTopic: String?
    ): String? {
        val cleanTopic =
            cleanText(
                topic
            )

        val cleanSubTopic =
            subTopic
                ?.let {
                    cleanText(
                        it
                    )
                }
                .orEmpty()

        if (
            cleanTopic.isBlank() ||
            cleanSubTopic.isBlank()
        ) {
            return null
        }

        return "${cleanTopic}__${cleanSubTopic}"
    }

    /**
     * מחזיר את הזהות הגלובלית של תרגיל.
     */
    fun resolveIdentity(
        belt: Belt,
        topic: String,
        subTopic: String? = null,
        rawItem: String
    ): ExerciseSyncIdentity {

        val cleanTopic =
            cleanText(
                topic
            )

        val cleanSubTopic =
            subTopic
                ?.let {
                    cleanText(
                        it
                    )
                }
                ?.takeIf {
                    it.isNotBlank()
                }

        val cleanRawItem =
            cleanText(
                rawItem
            )

        val displayTitle =
            displayTitle(
                topic = cleanTopic,
                rawItem = cleanRawItem
            )

        /*
         * זהו המזהה הראשי החדש.
         *
         * תמיד משתמשים בנושא האב,
         * כדי שכל המסכים יקבלו אותו ex_XXX.
         */
        val exerciseId =
            ExerciseIdentityRegistry.idFor(
                belt = belt,
                hebrewTitle = displayTitle,
                topicKey =
                    primaryTopicKey(
                        cleanTopic
                    )
            )

        val detailedKey =
            detailedTopicKey(
                topic = cleanTopic,
                subTopic = cleanSubTopic
            )

        /*
         * מזהה Registry לפי תת־נושא נשמר כ־alias.
         *
         * כך אנחנו עדיין מזהים נתונים שכבר נשמרו
         * במסכים שבהם statusTopicKey היה:
         *
         * נושא__תת־נושא
         */
        val detailedRegistryId =
            detailedKey
                ?.let { topicKey ->
                    ExerciseIdentityRegistry.idFor(
                        belt = belt,
                        hebrewTitle = displayTitle,
                        topicKey = topicKey
                    )
                }

        val canonicalId =
            CanonicalIds.canonicalFor(
                belt = belt,
                topicTitle = cleanTopic,
                displayItem = cleanRawItem
            )

        val explanationId =
            CanonicalIds.resolveCanonicalForExplanation(
                belt = belt,
                topicTitle = cleanTopic,
                rawItemFromRepo = cleanRawItem
            )

        val cleanedCanonicalItem =
            CanonicalIds.cleanItem(
                cleanTopic,
                cleanRawItem
            )
                .trim()

        val aliases =
            buildSet {

                add(
                    exerciseId
                )

                detailedRegistryId
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it
                        )
                    }

                canonicalId
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it
                        )
                    }

                explanationId
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it
                        )
                    }

                cleanedCanonicalItem
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it
                        )
                    }

                displayTitle
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it
                        )
                    }

                cleanRawItem
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it
                        )
                    }
            }
                .map {
                    cleanText(
                        it
                    )
                }
                .filter {
                    it.isNotBlank()
                }
                .toSet()

        return ExerciseSyncIdentity(
            belt = belt,
            topic = cleanTopic,
            subTopic = cleanSubTopic,
            rawItem = cleanRawItem,
            displayTitle = displayTitle,
            exerciseId = exerciseId,
            aliases = aliases
        )
    }

    /**
     * קיצור דרך כאשר צריך רק exerciseId.
     */
    fun exerciseId(
        belt: Belt,
        topic: String,
        subTopic: String? = null,
        rawItem: String
    ): String {
        return resolveIdentity(
            belt = belt,
            topic = topic,
            subTopic = subTopic,
            rawItem = rawItem
        ).exerciseId
    }

    /**
     * קיצור דרך כאשר צריך רק aliases.
     */
    fun aliases(
        belt: Belt,
        topic: String,
        subTopic: String? = null,
        rawItem: String
    ): Set<String> {
        return resolveIdentity(
            belt = belt,
            topic = topic,
            subTopic = subTopic,
            rawItem = rawItem
        ).aliases
    }

    /**
     * בדיקה האם מזהה שכבר נשמר שייך לתרגיל.
     */
    fun matches(
        identity: ExerciseSyncIdentity,
        storedId: String
    ): Boolean {
        val cleanStoredId =
            cleanText(
                storedId
            )

        if (cleanStoredId.isBlank()) {
            return false
        }

        if (
            cleanStoredId ==
            identity.exerciseId
        ) {
            return true
        }

        if (
            cleanStoredId in
            identity.aliases
        ) {
            return true
        }

        val normalizedStored =
            normalize(
                cleanStoredId
            )

        return identity
            .aliases
            .any { alias ->
                normalize(
                    alias
                ) ==
                        normalizedStored
            }
    }
}