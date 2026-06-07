package il.kmi.app.progress

data class UserProgressSummary(
    val uid: String = "",
    val beltId: String = "",
    val knownPercent: Int = 0,
    val knownCount: Int = 0,
    val totalCount: Int = 0,
    val bucket: Int = 0
)

data class BeltStatsSummary(
    val beltId: String = "",
    val usersCount: Int = 0,
    val averageKnownPercent: Int = 0,
    val bucket_0_10: Int = 0,
    val bucket_10_20: Int = 0,
    val bucket_20_30: Int = 0,
    val bucket_30_40: Int = 0,
    val bucket_40_50: Int = 0,
    val bucket_50_60: Int = 0,
    val bucket_60_70: Int = 0,
    val bucket_70_80: Int = 0,
    val bucket_80_90: Int = 0,
    val bucket_90_100: Int = 0
)

data class UserProgressComparison(
    val beltId: String = "",
    val usersCount: Int = 0,
    val userKnownPercent: Int = 0,
    val averageKnownPercent: Int = 0,
    val percentileAbove: Int = 0,
    val hasEnoughData: Boolean = false
)