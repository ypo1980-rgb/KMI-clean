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
        private const val TAG = "KMI_BILLING"

        fun logBilling(message: String) {
            Log.d(TAG, message)
        }

        fun logBillingError(message: String, throwable: Throwable? = null) {
            Log.e(TAG, message, throwable)
        }

        fun logBillingWarning(message: String) {
            Log.w(TAG, message)
        }
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
    // כרגע ב-Google Play מוגדרים רק שני מוצרים:
    // מנוי חודשי רגיל + מנוי שנתי רגיל.
    // לכן לא טוענים כאן מוצרי חבר עמותה עד שיוגדרו בפועל ב-Play Console.
    private val productIds = listOf(
        SubscriptionProducts.REGULAR_MONTHLY,
        SubscriptionProducts.REGULAR_YEARLY
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

                logBilling(
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

            logBilling(
                "queryProductDetails loaded=${loadedIds.size} ids=$loadedIds expected=$productIds " +
                        "billingResponseCode=${res.billingResult.responseCode} " +
                        "billingMessage='${res.billingResult.debugMessage}'"
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
            logBillingError("queryProductDetails failed", it)
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
        logBilling(
            "launchPurchase requested productId=$productId connected=${billingClient.isReady} loaded=${cachedProductDetails.keys}"
        )

        if (!billingClient.isReady) {
            _state.update { it.copy(error = "Billing client is not ready") }
            logBilling("launchPurchase blocked: billingClient not ready")
            return false
        }

        val details = cachedProductDetails[productId]
        if (details == null) {
            _state.update {
                it.copy(
                    error = "Product not loaded from Google Play: $productId"
                )
            }
            logBilling("launchPurchase blocked: missing ProductDetails for $productId")
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
            logBilling("launchPurchase blocked: missing offerToken for $productId")
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

        logBilling(
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
        logBilling(
            "onPurchasesUpdated code=${result.responseCode} msg='${result.debugMessage}' purchases=${purchases?.size ?: 0}"
        )

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    handleOwnedPurchases(purchases)
                } else {
                    refreshPurchases()
                }
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                logBilling(
                    "purchase result ITEM_ALREADY_OWNED - refreshing owned subscriptions from Google Play"
                )

                refreshPurchases()
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                logBilling("purchase canceled by user")
            }

            else -> {
                _state.update { it.copy(error = result.debugMessage) }
            }
        }
    }

    /**
     * מצב בדיקות Google Play:
     *
     * חודשי = 15 דקות
     * שנתי = 45 דקות
     *
     * חשוב:
     * מחשבים את הסיום לפי purchaseTime ולא לפי now.
     * כך רענון רכישות לא מאריך את המנוי שוב ושוב.
     */
    private fun calculateAccessUntilForSubscription(
        productId: String,
        purchaseTime: Long
    ): Long {
        val now = System.currentTimeMillis()
        val safePurchaseTime = purchaseTime.takeIf { it > 0L } ?: now

        val testDurationMs = when (productId) {
            SubscriptionProducts.REGULAR_MONTHLY,
            SubscriptionProducts.MEMBER_MONTHLY -> 15L * 60L * 1000L

            SubscriptionProducts.REGULAR_YEARLY,
            SubscriptionProducts.MEMBER_YEARLY -> 45L * 60L * 1000L

            else -> 15L * 60L * 1000L
        }

        val until = safePurchaseTime + testDurationMs

        logBilling(
            "TEST ACCESS UNTIL product=$productId " +
                    "purchaseTime=$purchaseTime safePurchaseTime=$safePurchaseTime " +
                    "now=$now until=$until " +
                    "minutes=${testDurationMs / 60_000L} " +
                    "remainingMinutes=${if (until > now) (until - now) / 60_000L else 0L}"
        )

        return until
    }

    private fun closeExpiredAccessForToken(
        expiredToken: String,
        expiredUntil: Long,
        ownedProduct: String?
    ) {
        val now = System.currentTimeMillis()

        sp.edit()
            .putBoolean("full_access", false)
            .putBoolean("has_full_access", false)
            .putBoolean("subscription_active", false)
            .putBoolean("is_subscribed", false)
            .putBoolean("google_subscription_verified", false)
            .putLong("google_subscription_checked_at", now)
            .putString("expired_sub_token", expiredToken)
            .putLong("expired_sub_access_until", expiredUntil)
            .putString("expired_sub_product", ownedProduct.orEmpty())
            .putString("last_sub_token", expiredToken)
            .putString("last_sub_product", ownedProduct.orEmpty())
            .remove("sub_product")
            .remove("sub_token")
            .remove("sub_purchase_time")
            .remove("sub_access_until")
            .putLong("access_changed_at", now)
            .commit()

        userSp.edit()
            .putBoolean("full_access", false)
            .putBoolean("has_full_access", false)
            .putBoolean("subscription_active", false)
            .putBoolean("is_subscribed", false)
            .putBoolean("google_subscription_verified", false)
            .putLong("google_subscription_checked_at", now)
            .putString("expired_sub_token", expiredToken)
            .putLong("expired_sub_access_until", expiredUntil)
            .putString("expired_sub_product", ownedProduct.orEmpty())
            .putString("last_sub_token", expiredToken)
            .putString("last_sub_product", ownedProduct.orEmpty())
            .remove("sub_product")
            .remove("sub_token")
            .remove("sub_purchase_time")
            .remove("sub_access_until")
            .putLong("access_changed_at", now)
            .commit()

        KmiAccess.setFullAccess(sp, false)
        KmiAccess.setFullAccess(userSp, false)

        logBilling(
            "ACCESS CLOSED EXPIRED TOKEN token=$expiredToken product=$ownedProduct expiredUntil=$expiredUntil now=$now"
        )
    }

    private fun writeAccessEverywhere(
        enabled: Boolean,
        ownedProduct: String?,
        purchaseToken: String?,
        purchaseTime: Long?,
        forceFreshWindow: Boolean = false
    ) {
        val now = System.currentTimeMillis()

        val existingAccessUntil = maxOf(
            sp.getLong("sub_access_until", 0L),
            userSp.getLong("sub_access_until", 0L)
        )

        val existingToken =
            sp.getString("sub_token", "").orEmpty()
                .ifBlank { userSp.getString("sub_token", "").orEmpty() }

        val expiredToken =
            sp.getString("expired_sub_token", "").orEmpty()
                .ifBlank { userSp.getString("expired_sub_token", "").orEmpty() }

        val lastToken =
            sp.getString("last_sub_token", "").orEmpty()
                .ifBlank { userSp.getString("last_sub_token", "").orEmpty() }

        val currentToken = purchaseToken.orEmpty()

        val isSamePurchaseToken =
            enabled &&
                    currentToken.isNotBlank() &&
                    existingToken == currentToken

        val wasLastTokenAlreadyClosed =
            enabled &&
                    currentToken.isNotBlank() &&
                    lastToken == currentToken &&
                    existingToken.isBlank() &&
                    existingAccessUntil == 0L

        val accessUntil = if (enabled) {
            when {
                forceFreshWindow -> {
                    logBilling(
                        "ACCESS FRESH WINDOW FORCED because Google Play reports PURCHASED/ITEM_ALREADY_OWNED. " +
                                "token=$currentToken product=$ownedProduct now=$now"
                    )

                    calculateAccessUntilForSubscription(
                        productId = ownedProduct.orEmpty(),
                        purchaseTime = now
                    )
                }

                isSamePurchaseToken && existingAccessUntil > now -> {
                    logBilling(
                        "ACCESS UNTIL REUSED sameToken=true existingUntil=$existingAccessUntil now=$now"
                    )
                    existingAccessUntil
                }

                isSamePurchaseToken && existingAccessUntil > 0L && existingAccessUntil <= now -> {
                    logBilling(
                        "ACCESS SAME TOKEN WAS LOCALLY EXPIRED BUT GOOGLE STILL OWNS IT. Recalculating fresh window. " +
                                "existingUntil=$existingAccessUntil now=$now token=$currentToken"
                    )

                    calculateAccessUntilForSubscription(
                        productId = ownedProduct.orEmpty(),
                        purchaseTime = now
                    )
                }

                else -> {
                    calculateAccessUntilForSubscription(
                        productId = ownedProduct.orEmpty(),
                        purchaseTime = purchaseTime ?: now
                    )
                }
            }
        } else {
            0L
        }

        val finalEnabled = enabled && accessUntil > now

        KmiAccess.setFullAccess(sp, finalEnabled)
        KmiAccess.setFullAccess(userSp, finalEnabled)

        sp.edit()
            .putBoolean("full_access", finalEnabled)
            .putBoolean("has_full_access", finalEnabled)
            .putBoolean("subscription_active", finalEnabled)
            .putBoolean("is_subscribed", finalEnabled)
            .putBoolean("google_subscription_verified", finalEnabled)
            .putLong("google_subscription_checked_at", now)
            .putString("sub_product", if (finalEnabled) ownedProduct.orEmpty() else "")
            .putString("sub_token", if (finalEnabled) currentToken else "")
            .putLong("sub_purchase_time", if (finalEnabled) purchaseTime ?: 0L else 0L)
            .putLong("sub_access_until", if (finalEnabled) accessUntil else 0L)
            .putString("last_sub_token", if (currentToken.isNotBlank()) currentToken else sp.getString("last_sub_token", "").orEmpty())
            .putString("last_sub_product", ownedProduct.orEmpty())
            .putLong("access_changed_at", now)
            .commit()

        userSp.edit()
            .putBoolean("full_access", finalEnabled)
            .putBoolean("has_full_access", finalEnabled)
            .putBoolean("subscription_active", finalEnabled)
            .putBoolean("is_subscribed", finalEnabled)
            .putBoolean("google_subscription_verified", finalEnabled)
            .putLong("google_subscription_checked_at", now)
            .putString("sub_product", if (finalEnabled) ownedProduct.orEmpty() else "")
            .putString("sub_token", if (finalEnabled) currentToken else "")
            .putLong("sub_purchase_time", if (finalEnabled) purchaseTime ?: 0L else 0L)
            .putLong("sub_access_until", if (finalEnabled) accessUntil else 0L)
            .putString("last_sub_token", if (currentToken.isNotBlank()) currentToken else userSp.getString("last_sub_token", "").orEmpty())
            .putString("last_sub_product", ownedProduct.orEmpty())
            .putLong("access_changed_at", now)
            .commit()

        logBilling(
            "ACCESS WRITE requestedEnabled=$enabled finalEnabled=$finalEnabled product=$ownedProduct " +
                    "purchaseTime=${purchaseTime ?: 0L} " +
                    "accessUntil=$accessUntil " +
                    "accessUntilInMinutesFromNow=${if (accessUntil > now) (accessUntil - now) / 60_000L else 0L} " +
                    "existingToken=$existingToken lastToken=$lastToken expiredToken=$expiredToken currentToken=$currentToken " +
                    "user_has_full_access=${userSp.getBoolean("has_full_access", false)} " +
                    "user_full_access=${userSp.getBoolean("full_access", false)} " +
                    "user_subscription_active=${userSp.getBoolean("subscription_active", false)}"
        )
    }

    private fun handleOwnedPurchases(list: List<Purchase>) {
        logBilling(
            "handleOwnedPurchases count=${list.size} " +
                    list.joinToString(prefix = "[", postfix = "]") { purchase ->
                        "products=${purchase.products}, state=${purchase.purchaseState}, acknowledged=${purchase.isAcknowledged}"
                    }
        )

        val sub = list.firstOrNull { purchase ->
            purchase.products.any { product ->
                product in productIds
            } &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (sub != null) {
            val ownedProduct = sub.products.firstOrNull()
            val now = System.currentTimeMillis()
            val currentToken = sub.purchaseToken

            val existingToken =
                sp.getString("sub_token", "").orEmpty()
                    .ifBlank { userSp.getString("sub_token", "").orEmpty() }

            val existingAccessUntil = maxOf(
                sp.getLong("sub_access_until", 0L),
                userSp.getLong("sub_access_until", 0L)
            )

            val expiredToken =
                sp.getString("expired_sub_token", "").orEmpty()
                    .ifBlank { userSp.getString("expired_sub_token", "").orEmpty() }

            val expiredUntil = maxOf(
                sp.getLong("expired_sub_access_until", 0L),
                userSp.getLong("expired_sub_access_until", 0L)
            )

            val lastToken =
                sp.getString("last_sub_token", "").orEmpty()
                    .ifBlank { userSp.getString("last_sub_token", "").orEmpty() }

            val isSameToken = existingToken == currentToken

            val isExpiredSameToken =
                isSameToken && existingAccessUntil > 0L && existingAccessUntil <= now

            val isAlreadyMarkedExpired =
                expiredToken == currentToken

            val wasLastTokenAlreadyClosed =
                lastToken == currentToken &&
                        existingToken.isBlank() &&
                        existingAccessUntil == 0L

            logBilling(
                "ACTIVE subscription detected product=$ownedProduct state=${sub.purchaseState} " +
                        "acknowledged=${sub.isAcknowledged} " +
                        "isSameToken=$isSameToken existingUntil=$existingAccessUntil now=$now " +
                        "expiredToken=$expiredToken expiredUntil=$expiredUntil lastToken=$lastToken " +
                        "isExpiredSameToken=$isExpiredSameToken isAlreadyMarkedExpired=$isAlreadyMarkedExpired " +
                        "wasLastTokenAlreadyClosed=$wasLastTokenAlreadyClosed"
            )

            // ✅ Google Play הוא מקור האמת:
            // אם Google מחזיר מנוי PURCHASED, לא חוסמים פתיחה בגלל expired_sub_token מקומי.
            // המצב הקודם יצר תקלה: Play אומר "כבר יש מנוי", אבל KmiAccess נשאר false ולכן התוכן נשאר נעול.
            if (isExpiredSameToken || isAlreadyMarkedExpired || wasLastTokenAlreadyClosed) {
                sp.edit()
                    .remove("expired_sub_token")
                    .remove("expired_sub_access_until")
                    .remove("expired_sub_product")
                    .commit()

                userSp.edit()
                    .remove("expired_sub_token")
                    .remove("expired_sub_access_until")
                    .remove("expired_sub_product")
                    .commit()

                logBilling(
                    "ACCESS RECOVERED FROM LOCAL EXPIRED TOKEN because Google Play reports PURCHASED. " +
                            "token=$currentToken product=$ownedProduct existingUntil=$existingAccessUntil " +
                            "expiredToken=$expiredToken expiredUntil=$expiredUntil lastToken=$lastToken"
                )
            }

            val shouldForceFreshWindow =
                isExpiredSameToken || isAlreadyMarkedExpired || wasLastTokenAlreadyClosed

            // יש מנוי פעיל חדש/תקף
            acknowledgeIfNeeded(sub)

            // את פרטי המנוי שומרים רק בתוך writeAccessEverywhere,
            // כדי שלא נייצר בטעות "אותו טוקן פעיל" לפני בדיקת תוקף.
            writeAccessEverywhere(
                enabled = true,
                ownedProduct = ownedProduct,
                purchaseToken = sub.purchaseToken,
                purchaseTime = sub.purchaseTime,
                forceFreshWindow = shouldForceFreshWindow
            )

            val activeAccessUntil = maxOf(
                sp.getLong("sub_access_until", 0L),
                userSp.getLong("sub_access_until", 0L)
            )

            val activeNow = activeAccessUntil > System.currentTimeMillis()

            logBilling(
                "ACCESS OPENED VERIFIED product=$ownedProduct activeNow=$activeNow activeAccessUntil=$activeAccessUntil"
            )

            _state.update {
                it.copy(
                    active = activeNow,
                    productId = if (activeNow) sub.products.firstOrNull() else null,
                    purchaseToken = if (activeNow) sub.purchaseToken else null,
                    renewalDate = if (activeNow) activeAccessUntil else null,
                    error = null
                )
            }
            refreshPriceState()
        } else {
            logBilling("NO active subscription found")

            // אין מנוי פעיל
            sp.edit()
                .remove("sub_product")
                .remove("sub_token")
                .remove("sub_purchase_time")
                .remove("sub_access_until")
                .remove("google_subscription_verified")
                .remove("google_subscription_checked_at")
                .apply()

            userSp.edit()
                .remove("sub_product")
                .remove("sub_token")
                .remove("sub_purchase_time")
                .remove("sub_access_until")
                .remove("google_subscription_verified")
                .remove("google_subscription_checked_at")
                .apply()

            writeAccessEverywhere(
                enabled = false,
                ownedProduct = null,
                purchaseToken = null,
                purchaseTime = null
            )

            logBilling("ACCESS CLOSED VERIFIED full_access=false subscription_active=false")

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
                    logBillingWarning("Acknowledge failed: ${res.debugMessage}")
                }
            }
        }
    }
}
