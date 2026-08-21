package il.kmi.app.screens.registration

import org.json.JSONArray
import org.json.JSONObject

/**
 * שיוך קבוצות לסניף מסוים של המאמן.
 *
 * אין כאן שמות קשיחים של סניפים או קבוצות.
 * כל הנתונים מגיעים מבחירת המשתמש ומהקטלוג.
 */
data class CoachBranchAssignment(
    val branch: String = "",
    val groups: List<String> = emptyList()
) {
    fun sanitized(): CoachBranchAssignment {
        return copy(
            branch = branch.trim(),
            groups =
                groups
                    .map { group ->
                        group.trim()
                    }
                    .filter { group ->
                        group.isNotBlank()
                    }
                    .distinct()
        )
    }

    fun toFirestoreMap(): Map<String, Any> {
        val clean = sanitized()

        return mapOf(
            "branch" to clean.branch,
            "groups" to clean.groups
        )
    }
}

/**
 * קידוד ופענוח של השיוכים לצורך SharedPreferences.
 */
object CoachBranchAssignmentsCodec {

    fun encode(
        assignments: List<CoachBranchAssignment>
    ): String {
        val jsonArray = JSONArray()

        assignments
            .map { assignment ->
                assignment.sanitized()
            }
            .filter { assignment ->
                assignment.branch.isNotBlank()
            }
            .distinctBy { assignment ->
                assignment.branch
            }
            .forEach { assignment ->
                val groupsArray = JSONArray()

                assignment.groups.forEach { group ->
                    groupsArray.put(group)
                }

                val assignmentObject =
                    JSONObject()
                        .put(
                            "branch",
                            assignment.branch
                        )
                        .put(
                            "groups",
                            groupsArray
                        )

                jsonArray.put(assignmentObject)
            }

        return jsonArray.toString()
    }

    fun decode(
        raw: String?
    ): List<CoachBranchAssignment> {
        val cleanRaw = raw
            ?.trim()
            .orEmpty()

        if (cleanRaw.isBlank()) {
            return emptyList()
        }

        return runCatching {
            val jsonArray = JSONArray(cleanRaw)

            buildList {
                for (
                index in 0 until jsonArray.length()
                ) {
                    val assignmentObject =
                        jsonArray.optJSONObject(index)
                            ?: continue

                    val branch =
                        assignmentObject
                            .optString("branch")
                            .trim()

                    if (branch.isBlank()) {
                        continue
                    }

                    val groupsArray =
                        assignmentObject
                            .optJSONArray("groups")

                    val groups =
                        buildList {
                            if (groupsArray != null) {
                                for (
                                groupIndex in
                                0 until
                                        groupsArray.length()
                                ) {
                                    groupsArray
                                        .optString(groupIndex)
                                        .trim()
                                        .takeIf { group ->
                                            group.isNotBlank()
                                        }
                                        ?.let(::add)
                                }
                            }
                        }
                            .distinct()

                    add(
                        CoachBranchAssignment(
                            branch = branch,
                            groups = groups
                        )
                    )
                }
            }
                .map { assignment ->
                    assignment.sanitized()
                }
                .distinctBy { assignment ->
                    assignment.branch
                }
        }.getOrDefault(emptyList())
    }

    fun flattenBranches(
        assignments: List<CoachBranchAssignment>
    ): List<String> {
        return assignments
            .map { assignment ->
                assignment.branch.trim()
            }
            .filter { branch ->
                branch.isNotBlank()
            }
            .distinct()
    }

    fun flattenGroups(
        assignments: List<CoachBranchAssignment>
    ): List<String> {
        return assignments
            .flatMap { assignment ->
                assignment.groups
            }
            .map { group ->
                group.trim()
            }
            .filter { group ->
                group.isNotBlank()
            }
            .distinct()
    }

    fun toFirestoreList(
        assignments: List<CoachBranchAssignment>
    ): List<Map<String, Any>> {
        return assignments
            .map { assignment ->
                assignment.sanitized()
            }
            .filter { assignment ->
                assignment.branch.isNotBlank()
            }
            .distinctBy { assignment ->
                assignment.branch
            }
            .map { assignment ->
                assignment.toFirestoreMap()
            }
    }
}