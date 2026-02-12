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
    val error: String? = null
)

class BillingRepository(
    private val context: Context
) : PurchasesUpdatedListener {

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

    // *** החלף כאן בזיהוי המוצר שלך ב-Play Console ***
    private val productIds = listOf("kmi_monthly") // TODO: change to real product id(s)

    private var cachedProductDetails: ProductDetails? = null

    fun startConnection(onReady: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            onReady?.invoke()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                _state.update { it.copy(connected = ok, error = if (ok) null else result.debugMessage) }
                if (ok) {
                    scope.launch {
                        queryProductDetails()
                        refreshPurchases()
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
                .setProductList(productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }).build()
            val res = billingClient.queryProductDetails(params)
            cachedProductDetails = res.productDetailsList?.firstOrNull()
        }.onFailure {
            Log.e("Billing", "queryProductDetails", it)
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = cachedProductDetails ?: return
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()
            ?.offerToken ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams
            .newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
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
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handleOwnedPurchases(purchases)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _state.update { it.copy(error = result.debugMessage) }
        }
    }

    private fun handleOwnedPurchases(list: List<Purchase>) {
        val sub = list.firstOrNull { p ->
            p.products.any { it in productIds } &&
                    p.purchaseState == Purchase.PurchaseState.PURCHASED
        }

        if (sub != null) {
            // יש מנוי פעיל
            acknowledgeIfNeeded(sub)

            // נשמור את פרטי המנוי הטכניים
            sp.edit()
                .putString("sub_product", sub.products.firstOrNull())
                .putString("sub_token", sub.purchaseToken)
                .apply()

            // 👇 לפתוח גישה מלאה באפליקציה
            KmiAccess.setFullAccess(userSp, true)

            _state.update {
                it.copy(
                    active = true,
                    productId = sub.products.firstOrNull(),
                    purchaseToken = sub.purchaseToken,
                    error = null
                )
            }
        } else {
            // אין מנוי פעיל
            sp.edit()
                .remove("sub_product")
                .remove("sub_token")
                .apply()

            // 👇 לבטל גישה מלאה (יישאר רק ניסיון אם עדיין בתוקף)
            KmiAccess.setFullAccess(userSp, false)

            _state.update {
                it.copy(
                    active = false,
                    productId = null,
                    purchaseToken = null
                )
            }
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
