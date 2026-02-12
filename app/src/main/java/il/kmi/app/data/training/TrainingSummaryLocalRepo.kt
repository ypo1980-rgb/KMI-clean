package il.kmi.app.data.training

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-first repository לסיכומי אימון (ללא Kotlinx Serialization)
 * נשמר ב-SharedPreferences כ-JSON.
 *
 * כל אחד בצד שלו:
 * key = training_summary_{ownerUid}_{role}_{summaryId}
 */
class TrainingSummaryLocalRepo(
    private val sp: SharedPreferences
) {
    private fun keyFor(ownerUid: String, role: SummaryAuthorRole, summaryId: String): String =
        "training_summary_${ownerUid.trim()}_${role.name}_${summaryId.trim()}"

    fun loadForOwner(
        ownerUid: String,
        role: SummaryAuthorRole,
        summaryId: String
    ): TrainingSummaryEntity? {
        val raw = sp.getString(keyFor(ownerUid, role, summaryId), null) ?: return null
        return runCatching { decode(raw) }.getOrNull()
    }

    fun saveForOwner(
        ownerUid: String,
        role: SummaryAuthorRole,
        summary: TrainingSummaryEntity
    ) {
        val now = System.currentTimeMillis()

        // נוודא שבאמת נשמר "בצד של המשתמש" לפי הפרמטרים
        val toSave = summary.copy(
            ownerUid = ownerUid.trim(),
            ownerRole = role,
            createdAtMs = summary.createdAtMs.takeIf { it > 0L } ?: now,
            updatedAtMs = now
        )

        val encoded = encode(toSave)

        sp.edit()
            .putString(keyFor(ownerUid, role, toSave.id), encoded)
            .apply()
    }

    fun clearForOwner(
        ownerUid: String,
        role: SummaryAuthorRole,
        summaryId: String
    ) {
        sp.edit()
            .remove(keyFor(ownerUid, role, summaryId))
            .apply()
    }

    // -----------------------------
    // JSON encode/decode
    // -----------------------------

    private fun encode(s: TrainingSummaryEntity): String {
        val o = JSONObject()

        o.put("id", s.id)

        o.put("ownerUid", s.ownerUid)
        o.put("ownerRole", s.ownerRole.name)

        o.put("dateIso", s.dateIso)
        o.put("branchId", s.branchId)
        o.put("branchName", s.branchName)

        o.put("coachUid", s.coachUid)
        o.put("coachName", s.coachName)

        o.put("groupKey", s.groupKey)

        o.put("notes", s.notes)
        o.put("createdAtMs", s.createdAtMs)
        o.put("updatedAtMs", s.updatedAtMs)

        val arr = JSONArray()
        s.exercises.forEach { e ->
            val je = JSONObject()
            je.put("exerciseId", e.exerciseId)
            je.put("name", e.name)
            je.put("topic", e.topic)

            if (e.difficulty == null) je.put("difficulty", JSONObject.NULL)
            else je.put("difficulty", e.difficulty)

            je.put("highlight", e.highlight)
            je.put("homePractice", e.homePractice)
            arr.put(je)
        }
        o.put("exercises", arr)

        return o.toString()
    }

    private fun decode(raw: String): TrainingSummaryEntity {
        val o = JSONObject(raw)

        val roleRaw = o.optString("ownerRole", SummaryAuthorRole.TRAINEE.name)
        val ownerRole = runCatching { SummaryAuthorRole.valueOf(roleRaw) }
            .getOrDefault(SummaryAuthorRole.TRAINEE)

        val exArr = o.optJSONArray("exercises") ?: JSONArray()
        val exercises = buildList {
            for (i in 0 until exArr.length()) {
                val je = exArr.optJSONObject(i) ?: continue

                val difficulty: Int? =
                    if (!je.has("difficulty") || je.isNull("difficulty")) null
                    else je.optInt("difficulty", 0)

                add(
                    TrainingSummaryExerciseEntity(
                        exerciseId = je.optString("exerciseId", ""),
                        name = je.optString("name", ""),
                        topic = je.optString("topic", ""),
                        difficulty = difficulty,
                        highlight = je.optString("highlight", ""),
                        homePractice = je.optBoolean("homePractice", false)
                    )
                )
            }
        }

        return TrainingSummaryEntity(
            id = o.optString("id", ""),

            ownerUid = o.optString("ownerUid", ""),
            ownerRole = ownerRole,

            dateIso = o.optString("dateIso", ""),
            branchId = o.optString("branchId", ""),
            branchName = o.optString("branchName", ""),

            coachUid = o.optString("coachUid", ""),
            coachName = o.optString("coachName", ""),

            groupKey = o.optString("groupKey", ""),

            exercises = exercises,
            notes = o.optString("notes", ""),

            createdAtMs = o.optLong("createdAtMs", 0L),
            updatedAtMs = o.optLong("updatedAtMs", 0L)
        )
    }
}
