package il.kmi.shared.stretching

data class StretchingExercise(
    val id: String,
    val category: StretchingCategory,
    val titleHe: String,
    val titleEn: String,
    val instructionsHe: String,
    val instructionsEn: String,
    val durationSeconds: Int,
    val repetitions: Int? = null,
    val performBothSides: Boolean = false,
    val imageKey: String,
    val safetyNoteHe: String = "",
    val safetyNoteEn: String = "",
    val sortOrder: Int
) {

    init {
        require(id.isNotBlank()) {
            "Stretching exercise id must not be blank"
        }

        require(titleHe.isNotBlank()) {
            "Hebrew title must not be blank"
        }

        require(titleEn.isNotBlank()) {
            "English title must not be blank"
        }

        require(instructionsHe.isNotBlank()) {
            "Hebrew instructions must not be blank"
        }

        require(instructionsEn.isNotBlank()) {
            "English instructions must not be blank"
        }

        require(durationSeconds > 0) {
            "Duration must be greater than zero"
        }

        require(repetitions == null || repetitions > 0) {
            "Repetitions must be null or greater than zero"
        }

        require(imageKey.isNotBlank()) {
            "Image key must not be blank"
        }

        require(sortOrder >= 0) {
            "Sort order must not be negative"
        }
    }

    fun displayTitle(
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            titleEn
        } else {
            titleHe
        }
    }

    fun displayInstructions(
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            instructionsEn
        } else {
            instructionsHe
        }
    }

    fun displaySafetyNote(
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            safetyNoteEn
        } else {
            safetyNoteHe
        }
    }

    fun hasSafetyNote(
        isEnglish: Boolean
    ): Boolean {
        return displaySafetyNote(
            isEnglish = isEnglish
        ).isNotBlank()
    }

    fun durationText(
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            "$durationSeconds sec"
        } else {
            "$durationSeconds שניות"
        }
    }

    fun repetitionsText(
        isEnglish: Boolean
    ): String? {
        val count = repetitions ?: return null

        return if (isEnglish) {
            "$count repetitions"
        } else {
            "$count חזרות"
        }
    }

    fun sidesText(
        isEnglish: Boolean
    ): String? {
        if (!performBothSides) return null

        return if (isEnglish) {
            "Both sides"
        } else {
            "שני הצדדים"
        }
    }

    fun imageFileName(): String {
        return "stretching_$imageKey.webp"
    }
}