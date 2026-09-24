package il.kmi.shared.stretching

enum class StretchingCategory(
    val id: String,
    val titleHe: String,
    val titleEn: String,
    val sortOrder: Int
) {
    NECK_AND_HEAD(
        id = "neck_and_head",
        titleHe = "צוואר וראש",
        titleEn = "Neck and head",
        sortOrder = 0
    ),

    SHOULDERS_AND_ARMS(
        id = "shoulders_and_arms",
        titleHe = "כתפיים וידיים",
        titleEn = "Shoulders and arms",
        sortOrder = 1
    ),

    UPPER_BACK(
        id = "upper_back",
        titleHe = "גב עליון",
        titleEn = "Upper back",
        sortOrder = 2
    ),

    LOWER_BACK(
        id = "lower_back",
        titleHe = "גב תחתון",
        titleEn = "Lower back",
        sortOrder = 3
    ),

    HIPS_AND_GROIN(
        id = "hips_and_groin",
        titleHe = "אגן ומפשעה",
        titleEn = "Hips and groin",
        sortOrder = 4
    ),

    LEGS(
        id = "legs",
        titleHe = "רגליים",
        titleEn = "Legs",
        sortOrder = 5
    ),

    KNEES_AND_ANKLES(
        id = "knees_and_ankles",
        titleHe = "ברכיים וקרסוליים",
        titleEn = "Knees and ankles",
        sortOrder = 6
    ),

    FULL_BODY(
        id = "full_body",
        titleHe = "גוף מלא",
        titleEn = "Full body",
        sortOrder = 7
    );

    fun displayTitle(
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            titleEn
        } else {
            titleHe
        }
    }

    companion object {

        fun fromId(
            id: String
        ): StretchingCategory? {
            return entries.firstOrNull {
                it.id == id.trim()
            }
        }

        fun ordered(): List<StretchingCategory> {
            return entries.sortedBy {
                it.sortOrder
            }
        }
    }
}