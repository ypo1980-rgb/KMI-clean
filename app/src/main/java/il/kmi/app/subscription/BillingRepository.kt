package il.kmi.app.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update   // << חשוב! זה מה שחסר
import kotlinx.coroutines.launch

data class SubscriptionState(
    val connected: Boolean = false,
    val active: Boolean = false,
    val productId: String? = null,
    val purchaseToken: String? = null,
    val renewalDate: Long? = null,
    val monthlyPriceText: String? = null,
    val yearlyPriceText: String? = null,

    // ✅ בדיקות אמיתיות מול Google Play
    val productsLoaded: Boolean = false,
    val loadedProductIds: List<String> = emptyList(),

    val error: String? = null
)

class BillingRepository(
    private val context: Context
) : PurchasesUpdatedListener {

    private companion object {
        const val TAG = "KMI_BILLING"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    // העדפות למידע טכני על המנוי (מזהה מוצר, טוקן)
    private val sp = context.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE)

    // העדפות "משתמש" – כאן KmiAccess משתמש ב־has_full_access וכו'
    private val userSp = context.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)

    private var billingClient: BillingClient = BillingClient
        .newBuilder(context)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    // מוצרי המנוי מתוך Play Console
    private val productIds = listOf(
        SubscriptionProducts.REGULAR_MONTHLY,
        SubscriptionProducts.REGULAR_YEARLY,
        SubscriptionProducts.MEMBER_MONTHLY,
        SubscriptionProducts.MEMBER_YEARLY
    )

    private val cachedProductDetails = linkedMapOf<String, ProductDetails>()

    private fun extractFormattedPrice(details: ProductDetails): String? {
        val phases = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            .orEmpty()

        val paidPhase = phases.firstOrNull { it.priceAmountMicros > 0L }
        return (paidPhase ?: phases.firstOrNull())?.formattedPrice
    }

    fun getPriceForProduct(productId: String): String? {
        val details = cachedProductDetails[productId] ?: return null
        return extractFormattedPrice(details)
    }

    private fun refreshPriceState() {

        val monthlyPrice =
            cachedProductDetails[SubscriptionProducts.REGULAR_MONTHLY]
                ?.let(::extractFormattedPrice)

        val yearlyPrice =
            cachedProductDetails[SubscriptionProducts.REGULAR_YEARLY]
                ?.let(::extractFormattedPrice)

        _state.update {
            it.copy(
                monthlyPriceText = monthlyPrice,
                yearlyPriceText = yearlyPrice
            )
        }
    }

    fun startConnection(onReady: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            onReady?.invoke()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK

                Log.e(
                    TAG,
                    "onBillingSetupFinished ok=$ok code=${result.responseCode} msg='${result.debugMessage}'"
                )

                _state.update {
                    it.copy(
                        connected = ok,
                        error = if (ok) null else result.debugMessage
                    )
                }

                if (ok) {
                    // שחזור מנויים מיד אחרי התחברות ל-Google Play
                    refreshPurchases()

                    scope.launch {
                        queryProductDetails()
                    }

                    onReady?.invoke()
                }
            }
            override fun onBillingServiceDisconnected() {
                _state.update { it.copy(connected = false, error = "Service disconnected") }
            }
        })
    }

    private suspend fun queryProductDetails() {
        runCatching {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                })
                .build()

            val res = billingClient.queryProductDetails(params)

            cachedProductDetails.clear()
            res.productDetailsList
                ?.forEach { details ->
                    cachedProductDetails[details.productId] = details
                }

            val loadedIds = cachedProductDetails.keys.toList()

            Log.e(
                TAG,
                "queryProductDetails loaded=${loadedIds.size} ids=$loadedIds expected=$productIds"
            )

            _state.update {
                it.copy(
                    productsLoaded = loadedIds.isNotEmpty(),
                    loadedProductIds = loadedIds,
                    error = if (loadedIds.isEmpty()) {
                        "No subscription products loaded from Google Play"
                    } else {
                        null
                    }
                )
            }

            refreshPriceState()
        }.onFailure {
            Log.e(TAG, "queryProductDetails failed", it)
            _state.update { s ->
                s.copy(
                    productsLoaded = false,
                    loadedProductIds = emptyList(),
                    error = it.message ?: "queryProductDetails failed"
                )
            }
        }
    }

    fun launchPurchase(activity: Activity, productId: String): Boolean {
        Log.e(
            TAG,
            "launchPurchase requested productId=$productId connected=${billingClient.isReady} loaded=${cachedProductDetails.keys}"
        )

        if (!billingClient.isReady) {
            _state.update { it.copy(error = "Billing client is not ready") }
            Log.e(TAG, "launchPurchase blocked: billingClient not ready")
            return false
        }

        val details = cachedProductDetails[productId]
        if (details == null) {
            _state.update {
                it.copy(
                    error = "Product not loaded from Google Play: $productId"
                )
            }
            Log.e(TAG, "launchPurchase blocked: missing ProductDetails for $productId")
            return false
        }

        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken

        if (offerToken.isNullOrBlank()) {
            _state.update {
                it.copy(
                    error = "Missing offer token for product: $productId"
                )
            }
            Log.e(TAG, "launchPurchase blocked: missing offerToken for $productId")
            return false
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams
            .newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)

        Log.e(
            TAG,
            "launchBillingFlow result code=${result.responseCode} msg='${result.debugMessage}' productId=$productId"
        )

        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun refreshPurchases() {
        scope.launch {
            runCatching {
                val res = billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
                handleOwnedPurchases(res.purchasesList ?: emptyList())
            }.onFailure {
                _state.update { s -> s.copy(error = it.message) }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        Log.e(
            TAG,
            "onPurchasesUpdated code=${result.responseCode} msg='${result.debugMessage}' purchases=${purchases?.size ?: 0}"
        )

        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handleOwnedPurchases(purchases)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _state.update { it.copy(error = result.debugMessage) }
        } else {
            Log.e(TAG, "purchase canceled by user")
        }
    }

    /**
     * מצב בדיקות:
     * חודשי = 5 דקות
     * שנתי = 30 דקות
     *
     * לפני גרסה אמיתית למשתמשים:
     * לשנות את forceShortTestExpiry ל־false.
     */
    private fun calculateAccessUntilForSubscription(
        productId: String,
        purchaseTime: Long
    ): Long {
        val forceShortTestExpiry = true
        val now = System.currentTimeMillis()

        if (forceShortTestExpiry) {
            val testDurationMs = when (productId) {
                SubscriptionProducts.REGULAR_MONTHLY,
                SubscriptionProducts.MEMBER_MONTHLY -> 5L * 60L * 1000L

                SubscriptionProducts.REGULAR_YEARLY,
                SubscriptionProducts.MEMBER_YEARLY -> 30L * 60L * 1000L

                else -> 5L * 60L * 1000L
            }

            // ✅ חשוב:
            // בבדיקות Google Play יכול להחזיר purchaseTime ישן/משוחזר.
            // לכן את חלון הגישה לבדיקה מחשבים מהרגע שבו Google Play אימת שהמנוי פעיל.
            val until = now + testDurationMs

            Log.e(
                TAG,
                "TEST ACCESS UNTIL product=$productId " +
                        "purchaseTime=$purchaseTime now=$now until=$until " +
                        "minutes=${testDurationMs / 60_000L}"
            )

            return until
        }

        val productionDurationMs = when (productId) {
            SubscriptionProducts.REGULAR_MONTHLY,
            SubscriptionProducts.MEMBER_MONTHLY -> 31L * 24L * 60L * 60L * 1000L

            SubscriptionProducts.REGULAR_YEARLY,
            SubscriptionProducts.MEMBER_YEARLY -> 370L * 24L * 60L * 60L * 1000L

            else -> 31L * 24L * 60L * 60L * 1000L
        }

        // ✅ גם בפרודקשן עדיף לא להסתמך על purchaseTime בלבד,
        // כי Google Play הוא מקור האמת לשאלה האם המנוי פעיל כרגע.
        val until = now + productionDurationMs

        Log.e(
            TAG,
            "PRODUCTION ACCESS UNTIL product=$productId " +
                    "purchaseTime=$purchaseTime now=$now until=$until"
        )

        return until
    }

    private fun writeAccessEverywhere(
        enabled: Boolean,
        ownedProduct: String?,
        purchaseToken: String?,
        purchaseTime: Long?
    ) {
        val now = System.currentTimeMillis()

        val accessUntil = if (enabled) {
            calculateAccessUntilForSubscription(
                productId = ownedProduct.orEmpty(),
                purchaseTime = purchaseTime ?: now
            )
        } else {
            0L
        }

        KmiAccess.setFullAccess(sp, enabled)
        KmiAccess.setFullAccess(userSp, enabled)

        sp.edit()
            .putBoolean("full_access", enabled)
            .putBoolean("has_full_access", enabled)
            .putBoolean("subscription_active", enabled)
            .putBoolean("is_subscribed", enabled)

            // ✅ מקור אמת נוסף למסכים: המנוי אומת מול Google Play
            .putBoolean("google_subscription_verified", enabled)
            .putLong("google_subscription_checked_at", now)

            .putString("sub_product", if (enabled) ownedProduct.orEmpty() else "")
            .putString("sub_token", if (enabled) purchaseToken.orEmpty() else "")
            .putLong("sub_purchase_time", purchaseTime ?: 0L)
            .putLong("sub_access_until", accessUntil)
            .putLong("access_changed_at", now)
            .commit()

        userSp.edit()
            .putBoolean("full_access", enabled)
            .putBoolean("has_full_access", enabled)
            .putBoolean("subscription_active", enabled)
            .putBoolean("is_subscribed", enabled)

            // ✅ מקור אמת נוסף למסכים: המנוי אומת מול Google Play
            .putBoolean("google_subscription_verified", enabled)
            .putLong("google_subscription_checked_at", now)

            .putString("sub_product", if (enabled) ownedProduct.orEmpty() else "")
            .putString("sub_token", if (enabled) purchaseToken.orEmpty() else "")
            .putLong("sub_purchase_time", purchaseTime ?: 0L)
            .putLong("sub_access_until", accessUntil)
            .putLong("access_changed_at", now)
            .commit()

        Log.e(
            TAG,
            "ACCESS WRITE enabled=$enabled product=$ownedProduct " +
                    "purchaseTime=${purchaseTime ?: 0L} " +
                    "accessUntil=$accessUntil " +
                    "accessUntilInMinutesFromNow=${if (accessUntil > now) (accessUntil - now) / 60_000L else 0L} " +
                    "user_has_full_access=${userSp.getBoolean("has_full_access", false)} " +
                    "user_full_access=${userSp.getBoolean("full_access", false)} " +
                    "user_subscription_active=${userSp.getBoolean("subscription_active", false)}"
        )
    }

    private fun handleOwnedPurchases(list: List<Purchase>) {
        val sub = list.firstOrNull { purchase ->
            purchase.products.any { product ->
                product in productIds
            } &&
                    purchase.purchaseState != Purchase.PurchaseState.UNSPECIFIED_STATE
        }

        if (sub != null) {
            val ownedProduct = sub.products.firstOrNull()

            Log.e(
                TAG,
                "ACTIVE subscription detected product=$ownedProduct state=${sub.purchaseState} acknowledged=${sub.isAcknowledged}"
            )

            // יש מנוי פעיל
            acknowledgeIfNeeded(sub)

            // נשמור את פרטי המנוי הטכניים
            sp.edit()
                .putString("sub_product", ownedProduct)
                .putString("sub_token", sub.purchaseToken)
                .putLong("sub_purchase_time", sub.purchaseTime)
                .apply()

            userSp.edit()
                .putString("sub_product", ownedProduct)
                .putLong("sub_purchase_time", sub.purchaseTime)
                .apply()

            writeAccessEverywhere(
                enabled = true,
                ownedProduct = ownedProduct,
                purchaseToken = sub.purchaseToken,
                purchaseTime = sub.purchaseTime
            )

            Log.e(
                TAG,
                "ACCESS OPENED VERIFIED product=$ownedProduct"
            )

            _state.update {
                it.copy(
                    active = true,
                    productId = sub.products.firstOrNull(),
                    purchaseToken = sub.purchaseToken,
                    renewalDate = sub.purchaseTime,
                    error = null
                )
            }
            refreshPriceState()
        } else {
            Log.e(TAG, "NO active subscription found")

            // אין מנוי פעיל
            sp.edit()
                .remove("sub_product")
                .remove("sub_token")
                .remove("sub_purchase_time")
                .remove("google_subscription_verified")
                .remove("google_subscription_checked_at")
                .apply()

            userSp.edit()
                .remove("sub_product")
                .remove("sub_token")
                .remove("sub_purchase_time")
                .remove("google_subscription_verified")
                .remove("google_subscription_checked_at")
                .apply()

            writeAccessEverywhere(
                enabled = false,
                ownedProduct = null,
                purchaseToken = null,
                purchaseTime = null
            )

            Log.e(TAG, "ACCESS CLOSED VERIFIED full_access=false subscription_active=false")

            _state.update {
                it.copy(
                    active = false,
                    productId = null,
                    purchaseToken = null,
                    renewalDate = null
                )
            }
            refreshPriceState()
        }
    }

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.isAcknowledged) return
        scope.launch {
            runCatching {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(p.purchaseToken)
                    .build()
                val res = billingClient.acknowledgePurchase(params)
                if (res.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w("Billing", "Acknowledge failed: ${res.debugMessage}")
                }
            }
        }
    }
}
