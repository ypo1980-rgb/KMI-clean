package il.kmi.app.subscription

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private fun formatStorePriceNoTrailingZeros(raw: String): String {
    return raw
        .replace(Regex("""(\d+)[.,]0+(?!\d)""")) { match ->
            match.groupValues[1]
        }
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun subscriptionPlanLabelForSuccess(
    productId: String?,
    isEnglish: Boolean
): String {
    return when (productId) {
        SubscriptionProducts.REGULAR_YEARLY,
        SubscriptionProducts.MEMBER_YEARLY -> {
            if (isEnglish) "yearly subscription" else "המנוי השנתי"
        }

        SubscriptionProducts.REGULAR_MONTHLY,
        SubscriptionProducts.MEMBER_MONTHLY -> {
            if (isEnglish) "monthly subscription" else "המנוי החודשי"
        }

        else -> {
            if (isEnglish) "subscription" else "המנוי"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    onBack: () -> Unit,
    onContinueToContent: () -> Unit = onBack,
    onOpenHome: () -> Unit,
    onOpenAssociationMembership: () -> Unit,
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
    else androidx.compose.ui.unit.LayoutDirection.Rtl

    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    val subsSp = remember {
        ctx.getSharedPreferences("kmi_subs", android.content.Context.MODE_PRIVATE)
    }

    val isAssociationMember = remember {
        userSp.getBoolean("is_association_member", false)
    }

    var purchaseStartedFromPlans by rememberSaveable {
        mutableStateOf(false)
    }

    var purchasedProductIdForDialog by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var showPurchaseSuccessDialog by rememberSaveable {
        mutableStateOf(false)
    }

    fun hasActiveSubscriptionInPrefs(): Boolean {
        val now = System.currentTimeMillis()

        val userUntil = userSp.getLong("sub_access_until", 0L)
        val subsUntil = subsSp.getLong("sub_access_until", 0L)

        val userHasAccess =
            userSp.getBoolean("has_full_access", false) ||
                    userSp.getBoolean("full_access", false) ||
                    userSp.getBoolean("subscription_active", false) ||
                    userSp.getBoolean("is_subscribed", false)

        val subsHasAccess =
            subsSp.getBoolean("has_full_access", false) ||
                    subsSp.getBoolean("full_access", false) ||
                    subsSp.getBoolean("subscription_active", false) ||
                    subsSp.getBoolean("is_subscribed", false)

        return (userUntil > now || subsUntil > now) && (userHasAccess || subsHasAccess)
    }

    val monthlyProductId = remember(isAssociationMember) {
        SubscriptionResolver.resolveMonthlyProduct(isAssociationMember)
    }
    val yearlyProductId = remember(isAssociationMember) {
        SubscriptionResolver.resolveYearlyProduct(isAssociationMember)
    }

    // Billing – חיבור לשירות
    val repo = remember { BillingRepository(ctx) }
    LaunchedEffect(Unit) { repo.startConnection() }
    val state by repo.state.collectAsState()

    val monthlyStorePrice = remember(state, monthlyProductId) {
        repo.getPriceForProduct(monthlyProductId)
            ?.let(::formatStorePriceNoTrailingZeros)
    }

    val yearlyStorePrice = remember(state, yearlyProductId) {
        repo.getPriceForProduct(yearlyProductId)
            ?.let(::formatStorePriceNoTrailingZeros)
    }

    val monthlyPriceText = monthlyStorePrice
        ?: if (isEnglish) "Price will appear soon" else "המחיר יופיע בקרוב"

    val yearlyPriceText = yearlyStorePrice
        ?: if (isEnglish) "Price will appear soon" else "המחיר יופיע בקרוב"

    val isBillingReady = state.connected && state.productsLoaded && state.error == null

    DisposableEffect(userSp, subsSp, purchaseStartedFromPlans) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                purchaseStartedFromPlans &&
                (
                        key == "has_full_access" ||
                                key == "full_access" ||
                                key == "subscription_active" ||
                                key == "is_subscribed" ||
                                key == "sub_product" ||
                                key == "sub_access_until" ||
                                key == "access_changed_at"
                        )
            ) {
                if (hasActiveSubscriptionInPrefs()) {
                    purchasedProductIdForDialog =
                        userSp.getString("sub_product", null)
                            ?: subsSp.getString("sub_product", null)

                    showPurchaseSuccessDialog = true
                    purchaseStartedFromPlans = false
                }
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)
        subsSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.active, state.productId, purchaseStartedFromPlans) {
        if (purchaseStartedFromPlans && state.active) {
            purchasedProductIdForDialog = state.productId
            showPurchaseSuccessDialog = true
            purchaseStartedFromPlans = false
        }
    }

    Scaffold(
        topBar = {
            // כותרת יחידה – בלי חץ חזור (החזרה תתבצע מהכפתור למטה / back של המכשיר)
            il.kmi.app.ui.KmiTopBar(
                title = if (isEnglish) "Subscription plans" else "תוכניות מנוי",
                onHome = onOpenHome,
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = true,
                centerTitle = true,
                currentLang = if (isEnglish) "en" else "he",
                onToggleLanguage = {
                    val newLang =
                        if (langManager.getCurrentLanguage() == il.kmi.shared.localization.AppLanguage.HEBREW) {
                            il.kmi.shared.localization.AppLanguage.ENGLISH
                        } else {
                            il.kmi.shared.localization.AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    (ctx as? Activity)?.recreate()
                },
                extraActions = { }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isAssociationMember) {
                        if (isEnglish) "Association member pricing detected" else "זוהתה זכאות למחיר חבר עמותה"
                    } else {
                        if (isEnglish) "Choose the plan that fits you:" else "בחר/י במסלול המתאים לך:"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (!state.productsLoaded || state.error != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF7ED)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (state.error != null) {
                                if (isEnglish) {
                                    "Purchases are temporarily unavailable. Please try again later."
                                } else {
                                    "הרכישות אינן זמינות כרגע. נסה שוב מאוחר יותר."
                                }
                            } else {
                                if (isEnglish) {
                                    "Loading subscription prices from Google Play..."
                                } else {
                                    "טוען מחירי מנויים מ־Google Play..."
                                }
                            },
                            color = Color(0xFF9A3412),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                TariffCard(
                    isAssociationMember = isAssociationMember,
                    monthlyPriceText = monthlyPriceText,
                    yearlyPriceText = yearlyPriceText
                )

                if (!isAssociationMember) {
                    Spacer(Modifier.height(14.dp))

                    JoinAssociationCard(
                        onClick = onOpenAssociationMembership
                    )
                }

                Spacer(Modifier.height(16.dp))

                // -------- מנוי חודשי --------
                PlanCard(
                    title = if (isAssociationMember) {
                        if (isEnglish) {
                            "Monthly plan for association members\n(full access to all content)"
                        } else {
                            "מנוי חודשי לחבר עמותה\n(גישה מלאה לכל התכנים)"
                        }
                    } else {
                        if (isEnglish) {
                            "Recurring monthly subscription\n(full access to all content)"
                        } else {
                            "מנוי חודשי מתחדש\n(גישה מלאה לכל התכנים)"
                        }
                    },
                    priceLine = if (monthlyStorePrice != null) {
                        if (isEnglish) {
                            "$monthlyStorePrice / month"
                        } else {
                            "$monthlyStorePrice / חודש"
                        }
                    } else {
                        monthlyPriceText
                    },
                    points = listOf(
                        if (isEnglish) "Full access to all app content" else "גישה מלאה לכל התכנים באפליקציה",
                        if (isEnglish) "Renews automatically every month" else "מתחדש אוטומטית מדי חודש",
                        if (isAssociationMember) {
                            if (isEnglish) "Includes discounted member pricing" else "כולל מחיר מוזל לחבר עמותה"
                        } else {
                            if (isEnglish) "Can be canceled anytime under Google Play policy" else "ניתן לבטל בכל עת בהתאם למדיניות Google Play"
                        }
                    ),
                    containerColor = Color(0xFF0EA5E9),
                    showTrialBadge = false,
                    buyEnabled = isBillingReady,
                    buyText = if (isBillingReady) {
                        if (isEnglish) "Secure purchase" else "רכישה מאובטחת"
                    } else {
                        if (isEnglish) "Loading price..." else "טוען מחיר..."
                    },
                    onBuy = {
                        if (activity != null && state.connected) {
                            purchaseStartedFromPlans = true
                            purchasedProductIdForDialog = monthlyProductId

                            val launched = repo.launchPurchase(activity, monthlyProductId)

                            if (!launched) {
                                purchaseStartedFromPlans = false

                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) {
                                        "The monthly subscription is not available for this tester yet."
                                    } else {
                                        "המנוי החודשי עדיין לא זמין לבודק הזה."
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                ctx,
                                if (isEnglish) {
                                    "Billing service is unavailable on this device."
                                } else {
                                    "שירות הרכישה אינו זמין במכשיר."
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )

                Spacer(Modifier.height(18.dp))

                // -------- מנוי שנתי --------
                PlanCard(
                    title = if (isAssociationMember) {
                        if (isEnglish) {
                            "Yearly plan for association members\n(full access to all content)"
                        } else {
                            "מנוי שנתי לחבר עמותה\n(גישה מלאה לכל התכנים)"
                        }
                    } else {
                        if (isEnglish) {
                            "Recurring yearly subscription\n(full access to all content)"
                        } else {
                            "מנוי שנתי\n(גישה מלאה לכל התכנים)"
                        }
                    },
                    priceLine = if (yearlyStorePrice != null) {
                        if (isEnglish) {
                            "$yearlyStorePrice / year"
                        } else {
                            "$yearlyStorePrice / שנה"
                        }
                    } else {
                        yearlyPriceText
                    },
                    points = listOf(
                        if (isEnglish) "One yearly payment" else "תשלום חד־שנתי אחד",
                        if (isEnglish) "No monthly renewal" else "ללא חידוש חודשי",
                        if (isAssociationMember) {
                            if (isEnglish) "Includes discounted member pricing" else "כולל מחיר מוזל לחבר עמותה"
                        } else {
                            if (isEnglish) "Access to all content for the full year" else "גישה לכל התכנים לאורך כל השנה"
                        }
                    ),
                    containerColor = Color(0xFFFFA000),
                    buyEnabled = isBillingReady,
                    buyText = if (isBillingReady) {
                        if (isEnglish) "Secure purchase" else "רכישה מאובטחת"
                    } else {
                        if (isEnglish) "Loading price..." else "טוען מחיר..."
                    },
                    onBuy = {
                        if (activity != null && state.connected) {
                            purchaseStartedFromPlans = true
                            purchasedProductIdForDialog = yearlyProductId

                            val launched = repo.launchPurchase(activity, yearlyProductId)

                            if (!launched) {
                                purchaseStartedFromPlans = false

                                Toast.makeText(
                                    ctx,
                                    if (isEnglish) {
                                        "The yearly subscription is not available for this tester yet."
                                    } else {
                                        "המנוי השנתי עדיין לא זמין לבודק הזה."
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                ctx,
                                if (isEnglish) {
                                    "Billing service is unavailable on this device."
                                } else {
                                    "שירות הרכישה אינו זמין במכשיר."
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(if (isEnglish) "Back to subscription screen" else "חזרה למסך ניהול המנוי")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (showPurchaseSuccessDialog) {
                val planLabel = subscriptionPlanLabelForSuccess(
                    productId = purchasedProductIdForDialog,
                    isEnglish = isEnglish
                )

                Dialog(
                    onDismissRequest = {
                        showPurchaseSuccessDialog = false
                        onContinueToContent()
                    }
                ) {
                    ElevatedCard(
                        shape = RoundedCornerShape(30.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFFF7F4FB)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF7C3AED),
                                                Color(0xFF06B6D4)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (isEnglish) "Purchase approved" else "רכישה אושרה",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Text(
                                text = if (isEnglish) {
                                    "Your $planLabel purchase was completed successfully. You can now continue to the content."
                                } else {
                                    "הרכישה של $planLabel בוצעה בהצלחה. כעת ניתן להמשיך לתוכן."
                                },
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF374151),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    showPurchaseSuccessDialog = false
                                    onContinueToContent()
                                },
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6D4DB3),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "Continue to content" else "המשך לתוכן",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TariffCard(
    isAssociationMember: Boolean,
    monthlyPriceText: String,
    yearlyPriceText: String
) {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val layoutDirection = if (isEnglish) {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    } else {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    }

    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF111827)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = horizontalAlign
            ) {
                Text(
                    text = if (isEnglish) "App pricing" else "תעריפון האפליקציה",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isEnglish) {
                        "Prices are loaded directly from Google Play according to your subscription eligibility."
                    } else {
                        "המחירים נטענים ישירות מ־Google Play בהתאם לזכאות המנוי שלך."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumTariffRow(
                            label = if (isAssociationMember) {
                                if (isEnglish) {
                                    "KAMI\nmember\nplan"
                                } else {
                                    "מסלול\nחבר עמותת\nק.מ.י"
                                }
                            } else {
                                if (isEnglish) {
                                    "Regular\nuser\nplan"
                                } else {
                                    "מסלול\nמשתמש\nרגיל"
                                }
                            },
                            monthly = if (isEnglish) "Monthly" else "חודשי",
                            yearly = if (isEnglish) "Yearly" else "שנתי",
                            isHeader = true
                        )

                        PremiumTariffDivider()

                        PremiumTariffRow(
                            label = if (isEnglish) "Current price" else "מחיר נוכחי",
                            monthly = monthlyPriceText,
                            yearly = yearlyPriceText,
                            highlight = true
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF065F46).copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAssociationMember) {
                            if (isEnglish) {
                                "Association member pricing is active for this account."
                            } else {
                                "מחיר חבר עמותה פעיל עבור החשבון הזה."
                            }
                        } else {
                            if (isEnglish) {
                                "Association members may receive discounted pricing after membership verification."
                            } else {
                                "חברי עמותה יכולים לקבל מחיר מוזל לאחר אימות סטטוס החברות."
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumTariffRow(
    label: String,
    monthly: String,
    yearly: String,
    isHeader: Boolean = false,
    highlight: Boolean = false
) {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH
    val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
    else androidx.compose.ui.unit.LayoutDirection.Rtl
    val labelAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    val textColor = when {
        isHeader -> Color.White
        highlight -> Color(0xFF86EFAC)
        else -> Color.White.copy(alpha = 0.96f)
    }

    val fontWeight = if (isHeader) FontWeight.ExtraBold else FontWeight.SemiBold
    val headerStyle = MaterialTheme.typography.bodyMedium
    val regularStyle = MaterialTheme.typography.bodyLarge

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = textColor,
                fontWeight = fontWeight,
                style = if (isHeader) headerStyle else regularStyle,
                modifier = Modifier.weight(1.55f),
                textAlign = if (isHeader) TextAlign.Center else labelAlign,
                maxLines = if (isHeader) 3 else 2,
                overflow = if (isHeader) TextOverflow.Clip else TextOverflow.Ellipsis
            )

            Text(
                text = monthly,
                color = textColor,
                fontWeight = fontWeight,
                style = if (isHeader) headerStyle else regularStyle,
                modifier = Modifier.weight(0.95f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )

            Text(
                text = yearly,
                color = textColor,
                fontWeight = fontWeight,
                style = if (isHeader) headerStyle else regularStyle,
                modifier = Modifier.weight(0.95f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun PremiumTariffDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.10f))
    )
}

@Composable
private fun JoinAssociationCard(
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH
    val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
    else androidx.compose.ui.unit.LayoutDirection.Rtl
    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = horizontalAlign
            ) {
                Text(
                    text = if (isEnglish) {
                        "Join the KAMI association"
                    } else {
                        "הצטרפות לעמותת ק.מ.י"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign
                )

                Text(
                    text = if (isEnglish) {
                        "Join now and enjoy discounted pricing in the app."
                    } else {
                        "הצטרף עכשיו לעמותה ותוכל ליהנות ממחיר מוזל באפליקציה."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFECFDF5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Association members may receive discounted pricing after membership verification."
                        } else {
                            "חברי עמותה יכולים לקבל מחיר מוזל לאחר אימות סטטוס החברות."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF065F46),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Join the association" else "להצטרפות לעמותה",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    priceLine: String,
    points: List<String>,
    containerColor: Color,
    showTrialBadge: Boolean = false,
    buyEnabled: Boolean = true,
    buyText: String,
    onBuy: () -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val layoutDirection = if (isEnglish) {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    } else {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    }

    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val pointTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = horizontalAlign,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                if (showTrialBadge) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnglish)
                                "⭐ 3-DAY FREE TRIAL"
                            else
                                "⭐ ניסיון חינם ל-3 ימים",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = priceLine,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = horizontalAlign,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    points.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEnglish) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.95f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = pointTextAlign,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = pointTextAlign,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Button(
                    onClick = onBuy,
                    enabled = buyEnabled,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = containerColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.55f),
                        disabledContentColor = containerColor.copy(alpha = 0.70f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = buyText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
