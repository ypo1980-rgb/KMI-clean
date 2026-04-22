package il.kmi.app.subscription

object SubscriptionResolver {

    fun resolveMonthlyProduct(isAssociationMember: Boolean): String {
        return if (isAssociationMember) {
            SubscriptionProducts.MEMBER_MONTHLY
        } else {
            SubscriptionProducts.REGULAR_MONTHLY
        }
    }

    fun resolveYearlyProduct(isAssociationMember: Boolean): String {
        return if (isAssociationMember) {
            SubscriptionProducts.MEMBER_YEARLY
        } else {
            SubscriptionProducts.REGULAR_YEARLY
        }
    }

    fun resolveProduct(
        isAssociationMember: Boolean,
        isYearly: Boolean
    ): String {
        return if (isYearly) {
            resolveYearlyProduct(isAssociationMember)
        } else {
            resolveMonthlyProduct(isAssociationMember)
        }
    }

    fun isMemberProduct(productId: String): Boolean {
        return productId == SubscriptionProducts.MEMBER_MONTHLY ||
                productId == SubscriptionProducts.MEMBER_YEARLY
    }

    fun isRegularProduct(productId: String): Boolean {
        return productId == SubscriptionProducts.REGULAR_MONTHLY ||
                productId == SubscriptionProducts.REGULAR_YEARLY
    }

    fun isYearlyProduct(productId: String): Boolean {
        return productId == SubscriptionProducts.REGULAR_YEARLY ||
                productId == SubscriptionProducts.MEMBER_YEARLY
    }

    fun isMonthlyProduct(productId: String): Boolean {
        return productId == SubscriptionProducts.REGULAR_MONTHLY ||
                productId == SubscriptionProducts.MEMBER_MONTHLY
    }
}