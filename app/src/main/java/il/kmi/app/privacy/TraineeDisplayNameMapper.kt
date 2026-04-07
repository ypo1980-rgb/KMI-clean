package il.kmi.app.privacy

object TraineeDisplayNameMapper {

    private val fixedNames = listOf(
        "מתאמן 1",
        "מתאמן 2",
        "מתאמן 3",
        "מתאמן 4",
        "מתאמן 5",
        "מתאמן 6",
        "מתאמן 7",
        "מתאמן 8"
    )

    fun displayName(
        realName: String?,
        stableKey: String?
    ): String {
        if (!DemoPrivacy.ENABLED) {
            return realName?.trim().orEmpty()
        }

        val key = stableKey?.trim().orEmpty().ifBlank {
            realName?.trim().orEmpty()
        }

        if (key.isBlank()) return "מתאמן"

        val index = (key.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % fixedNames.size
        return fixedNames[index]
    }
}