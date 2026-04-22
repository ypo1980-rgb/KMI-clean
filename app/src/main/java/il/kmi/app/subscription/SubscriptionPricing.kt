package il.kmi.app.subscription

data class SubscriptionPricing(
    val isAssociationMember: Boolean,
    val monthlyPrice: Int,
    val yearlyPrice: Int,
    val associationPercent: Int
)

object SubscriptionPricingResolver {

    fun resolve(isAssociationMember: Boolean): SubscriptionPricing {
        return if (isAssociationMember) {
            SubscriptionPricing(
                isAssociationMember = true,
                monthlyPrice = 20,
                yearlyPrice = 200,
                associationPercent = 5
            )
        } else {
            SubscriptionPricing(
                isAssociationMember = false,
                monthlyPrice = 25,
                yearlyPrice = 250,
                associationPercent = 7
            )
        }
    }
}