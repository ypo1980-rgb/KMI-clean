package il.kmi.app.subscription

object SubscriptionProducts {

    const val REGULAR_MONTHLY = "regular_monthly"
    const val REGULAR_YEARLY = "regular_yearly"

    const val MEMBER_MONTHLY = "member_monthly"
    const val MEMBER_YEARLY = "member_yearly"

    val ALL = setOf(
        REGULAR_MONTHLY,
        REGULAR_YEARLY,
        MEMBER_MONTHLY,
        MEMBER_YEARLY
    )
}