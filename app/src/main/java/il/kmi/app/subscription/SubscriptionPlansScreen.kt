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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.navigationBarsPadding
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.scaledIconSize
import il.yuval.ui.theme.kmiScreenBackgroundBrush

//==================================================================

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

    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }

    val subsSp = remember {
        ctx.getSharedPreferences("kmi_subs", android.content.Context.MODE_PRIVATE)
    }

    // כרגע אין ב-Google Play מסלולים נפרדים לחברי עמותה.
    // לכן מסך התוכניות מציג ומשתמש רק במסלולים הרגילים בתשלום מלא.
    val isAssociationMember =
        userSp.getBoolean("is_association_member", false)

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

    // כרגע קיימים ב-Google Play רק שני Product IDs רגילים.
    // סטטוס חבר העמותה משפיע על התצוגה בלבד, עד שיוגדרו בחנות
    // מוצרים נפרדים ומאומתים לחברי העמותה.
    val monthlyProductId =
        SubscriptionProducts.REGULAR_MONTHLY

    val yearlyProductId =
        SubscriptionProducts.REGULAR_YEARLY

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

    val monthlyProductLoaded = state.loadedProductIds.contains(monthlyProductId)
    val yearlyProductLoaded = state.loadedProductIds.contains(yearlyProductId)

    val monthlyBuyReady =
        state.connected &&
                state.error == null &&
                monthlyProductLoaded &&
                monthlyStorePrice != null

    val yearlyBuyReady =
        state.connected &&
                state.error == null &&
                yearlyProductLoaded &&
                yearlyStorePrice != null

    DisposableEffect(userSp, subsSp, purchaseStartedFromPlans) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
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

    val subscriptionBackgroundBrush =
        kmiScreenBackgroundBrush()

    Scaffold(
        containerColor = Color.Transparent,
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        subscriptionBackgroundBrush
                    )
                    .padding(padding)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                Text(
                    text =
                        if (isEnglish) {
                            "Choose the plan that fits you:"
                        } else {
                            "בחר/י במסלול המתאים לך:"
                        },
                    style = KmiTypography.sectionTitle.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                if (
                    !state.productsLoaded &&
                    state.error == null
                ) {
                    Card(
                        shape =
                            RoundedCornerShape(18.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surface
                            ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KmiLoadingRings(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            text =
                                if (isEnglish) {
                                    "Loading subscription prices from Google Play..."
                                } else {
                                    "טוען מחירי מנויים מ־Google Play..."
                                }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                } else if (state.error != null) {
                    Card(
                        shape =
                            RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .errorContainer
                            ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Purchases are temporarily unavailable. Please try again later."
                                } else {
                                    "הרכישות אינן זמינות כרגע. נסה שוב מאוחר יותר."
                                },
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onErrorContainer,
                            style =
                                KmiTypography.caption.copy(
                                    fontWeight =
                                        FontWeight.SemiBold
                                ),
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                TariffCard(
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
                    title =
                        if (isEnglish) {
                            "Recurring monthly subscription\n(full access to all content)"
                        } else {
                            "מנוי חודשי מתחדש\n(גישה מלאה לכל התכנים)"
                        },
                    priceLine =
                        if (monthlyStorePrice != null) {
                            if (isEnglish) {
                                "$monthlyStorePrice / month"
                            } else {
                                "$monthlyStorePrice / חודש"
                            }
                        } else {
                            monthlyPriceText
                        },
                    points =
                        listOf(
                            if (isEnglish) {
                                "Full access to all app content"
                            } else {
                                "גישה מלאה לכל התכנים באפליקציה"
                            },
                            if (isEnglish) {
                                "Renews automatically every month"
                            } else {
                                "מתחדש אוטומטית מדי חודש"
                            },
                            if (isEnglish) {
                                "Can be canceled anytime under Google Play policy"
                            } else {
                                "ניתן לבטל בכל עת בהתאם למדיניות Google Play"
                            }
                        ),
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary,
                    showTrialBadge = false,
                    buyEnabled = monthlyBuyReady,
                    buyText = if (monthlyBuyReady) {
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
                    title =
                        if (isEnglish) {
                            "Recurring yearly subscription\n(full access to all content)"
                        } else {
                            "מנוי שנתי\n(גישה מלאה לכל התכנים)"
                        },
                    priceLine =
                        if (yearlyStorePrice != null) {
                            if (isEnglish) {
                                "$yearlyStorePrice / year"
                            } else {
                                "$yearlyStorePrice / שנה"
                            }
                        } else {
                            yearlyPriceText
                        },
                    points =
                        listOf(
                            if (isEnglish) {
                                "One yearly payment"
                            } else {
                                "תשלום חד־שנתי אחד"
                            },
                            if (isEnglish) {
                                "No monthly renewal"
                            } else {
                                "ללא חידוש חודשי"
                            },
                            if (isEnglish) {
                                "Access to all content for the full year"
                            } else {
                                "גישה לכל התכנים לאורך כל השנה"
                            }
                        ),
                    containerColor =
                        MaterialTheme.colorScheme.tertiary,
                    contentColor =
                        MaterialTheme.colorScheme.onTertiary,
                    buyEnabled = yearlyBuyReady,
                    buyText = if (yearlyBuyReady) {
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

                Surface(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(18.dp),
                    color =
                        MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = 1.dp,
                        color =
                            MaterialTheme.colorScheme.outlineVariant
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (isEnglish) {
                                    "Back to subscription screen"
                                } else {
                                    "חזרה למסך ניהול המנוי"
                                },
                            style =
                                KmiTypography.sectionTitle.copy(
                                    fontWeight =
                                        FontWeight.ExtraBold
                                ),
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    PremiumPurchaseSuccessDialog(
                        isEnglish = isEnglish,
                        planLabel = planLabel,
                        onContinue = {
                            showPurchaseSuccessDialog = false
                            onContinueToContent()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumPurchaseSuccessDialog(
    isEnglish: Boolean,
    planLabel: String,
    onContinue: () -> Unit
) {
    val layoutDirection =
        if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
        else androidx.compose.ui.unit.LayoutDirection.Rtl

    val dialogBackground =
        MaterialTheme.colorScheme.surface

    val dialogBackgroundVariant =
        MaterialTheme.colorScheme.surfaceVariant

    val dialogContent =
        MaterialTheme.colorScheme.onSurface

    val accentColor =
        MaterialTheme.colorScheme.tertiary

    val onAccentColor =
        MaterialTheme.colorScheme.onTertiary

    val badgeColor =
        MaterialTheme.colorScheme.primary

    val onBadgeColor =
        MaterialTheme.colorScheme.onPrimary

    DisposableEffect(Unit) {
        val toneGenerator =
            runCatching {
                ToneGenerator(
                    AudioManager.STREAM_NOTIFICATION,
                    55
                ).also {
                    it.startTone(
                        ToneGenerator.TONE_PROP_ACK,
                        180
                    )
                }
            }.getOrNull()

        onDispose {
            toneGenerator?.release()
        }
    }

    val enter = rememberInfiniteTransition(label = "premium_success_motion")

    val pulse by enter.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_pulse"
    )

    val shimmer by enter.animateFloat(
        initialValue = -220f,
        targetValue = 420f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Restart
        ),
        label = "button_shimmer"
    )

    val sparkleOffset by enter.animateFloat(
        initialValue = -24f,
        targetValue = 46f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Restart
        ),
        label = "gold_sparkles"
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Card(
            shape = RoundedCornerShape(36.dp),
            border = BorderStroke(
                1.7.dp,
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.45f),
                        accentColor,
                        badgeColor.copy(alpha = 0.55f)
                    )
                )
            ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.Transparent
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                ),
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                dialogBackground,
                                dialogBackgroundVariant,
                                dialogBackground
                            )
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 26.dp)
            ) {
                Text(
                    text = "✦   ✧   ✦   ✧",
                    color =
                        accentColor.copy(alpha = 0.65f),
                    style = KmiTypography.action,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                translationY =
                                    sparkleOffset
                            }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(scaledIconSize(78.dp))
                            .graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                            }
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        accentColor,
                                        MaterialTheme.colorScheme.primaryContainer,
                                        dialogBackgroundVariant
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👑",
                            style = KmiTypography.metric
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        badgeColor,
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        "Purchase approved"
                                    } else {
                                        "רכישה אושרה"
                                    },
                                color = onBadgeColor,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                style =
                                    KmiTypography.sectionTitle
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = onBadgeColor,
                                modifier = Modifier.size(
                                    KmiIconSize.medium
                                )
                            )
                        }
                    }

                    Text(
                        text =
                            if (isEnglish) {
                                "Congratulations!"
                            } else {
                                "ברכות!"
                            },
                        color = accentColor,
                        fontWeight = FontWeight.Black,
                        style =
                            KmiTypography.screenTitle,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Text(
                        text =
                            if (isEnglish) {
                                "Your $planLabel purchase was completed successfully. You can now continue to the content."
                            } else {
                                "הרכישה של $planLabel בוצעה בהצלחה. כעת ניתן להמשיך לתוכן."
                            },
                        color =
                            dialogContent.copy(
                                alpha = 0.93f
                            ),
                        fontWeight = FontWeight.Bold,
                        style = KmiTypography.body,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = onContinue,
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccentColor
                        ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 52.dp,
                                    max = 64.dp
                                )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .width(90.dp)
                                        .heightIn(
                                            min = 52.dp,
                                            max = 64.dp
                                        )
                                        .graphicsLayer {
                                            translationX = shimmer
                                        }
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    onAccentColor.copy(alpha = 0.42f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "Continue to content"
                                    } else {
                                        "המשך לתוכן"
                                    },
                                style =
                                    KmiTypography.action,
                                fontWeight =
                                    FontWeight.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                accentColor.copy(alpha = 0.30f)
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🛡️",
                            style = KmiTypography.metric
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text =
                                if (isEnglish) {
                                    "Secure purchase • Full content access"
                                } else {
                                    "רכישה מאובטחת • גישה מלאה לתכנים"
                                },
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            style =
                                KmiTypography.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TariffCard(
    monthlyPriceText: String,
    yearlyPriceText: String
) {
    val ctx = LocalContext.current
    val langManager =
        remember {
            il.kmi.shared.localization.AppLanguageManager(ctx)
        }
    val isEnglish =
        langManager.getCurrentLanguage() ==
                il.kmi.shared.localization.AppLanguage.ENGLISH

    val layoutDirection =
        if (isEnglish) {
            androidx.compose.ui.unit.LayoutDirection.Ltr
        } else {
            androidx.compose.ui.unit.LayoutDirection.Rtl
        }

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor =
                    MaterialTheme.colorScheme.onSurfaceVariant
            ),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "App pricing"
                        } else {
                            "תעריפון האפליקציה"
                        },
                    style = KmiTypography.sectionTitle,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text =
                        if (isEnglish) {
                            "Prices are loaded directly from Google Play."
                        } else {
                            "המחירים נטענים ישירות מ־Google Play."
                        },
                    style = KmiTypography.secondary,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface,
                            contentColor =
                                MaterialTheme.colorScheme.onSurface
                        ),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 10.dp
                                ),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumTariffRow(
                            label =
                                if (isEnglish) {
                                    "Plan"
                                } else {
                                    "מסלול"
                                },
                            monthly =
                                if (isEnglish) {
                                    "Monthly"
                                } else {
                                    "חודשי"
                                },
                            yearly =
                                if (isEnglish) {
                                    "Yearly"
                                } else {
                                    "שנתי"
                                },
                            isHeader = true
                        )

                        PremiumTariffDivider()

                        PremiumTariffRow(
                            label =
                                if (isEnglish) {
                                    "Google Play"
                                } else {
                                    "Google Play"
                                },
                            monthly = monthlyPriceText,
                            yearly = yearlyPriceText,
                            highlight = true
                        )
                    }
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

    val textColor =
        when {
            isHeader ->
                MaterialTheme.colorScheme.onSurface

            highlight ->
                MaterialTheme.colorScheme.tertiary

            else ->
                MaterialTheme.colorScheme
                    .onSurface
                    .copy(alpha = 0.96f)
        }

    val fontWeight =
        if (isHeader) {
            FontWeight.ExtraBold
        } else {
            FontWeight.SemiBold
        }

    val headerStyle = KmiTypography.secondary
    val regularStyle = KmiTypography.body

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
            .background(
                MaterialTheme.colorScheme.outlineVariant
            )
    )
}

@Composable
private fun JoinAssociationCard(
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember {
        il.kmi.shared.localization.AppLanguageManager(ctx)
    }
    val isEnglish =
        langManager.getCurrentLanguage() ==
                il.kmi.shared.localization.AppLanguage.ENGLISH

    val layoutDirection =
        if (isEnglish) {
            androidx.compose.ui.unit.LayoutDirection.Ltr
        } else {
            androidx.compose.ui.unit.LayoutDirection.Rtl
        }

    val horizontalAlign =
        if (isEnglish) Alignment.Start else Alignment.End

    val textAlign =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface,
                contentColor =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            ),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 12.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp),
                horizontalAlignment = horizontalAlign
            ) {
                Text(
                    text =
                        if (isEnglish) {
                            "Join the KAMI association"
                        } else {
                            "הצטרפות לעמותת ק.מ.י"
                        },
                    style = KmiTypography.sectionTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign
                )

                Text(
                    text =
                        if (isEnglish) {
                            "Join now and enjoy discounted pricing in the app."
                        } else {
                            "הצטרף עכשיו לעמותה ותוכל ליהנות ממחיר מוזל באפליקציה."
                        },
                    style = KmiTypography.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer,
                        contentColor =
                            MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Association members may receive discounted pricing after membership verification."
                            } else {
                                "חברי עמותה יכולים לקבל מחיר מוזל לאחר אימות סטטוס החברות."
                            },
                        style = KmiTypography.body.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color =
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 10.dp
                            ),
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text =
                            if (isEnglish) {
                                "Join the association"
                            } else {
                                "להצטרפות לעמותה"
                            },
                        style = KmiTypography.action
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
    contentColor: Color,
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
        colors =
            CardDefaults.elevatedCardColors(
                containerColor =
                    containerColor
            ),
        elevation =
            CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 12.dp
                        ),
                horizontalAlignment =
                    horizontalAlign,
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
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
                            style = KmiTypography.caption.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.Black
                        )
                    }
                }
                Text(
                    text = title,
                    style = KmiTypography.screenTitle,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = priceLine,
                    style = KmiTypography.sectionTitle,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
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
                                    tint = contentColor.copy(alpha = 0.95f),
                                    modifier = Modifier.size(
                                        KmiIconSize.medium
                                    )
                                )

                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = line,
                                    style = KmiTypography.body,
                                    color = contentColor,
                                    textAlign = pointTextAlign,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = line,
                                    style = KmiTypography.body,
                                    color = contentColor,
                                    textAlign = pointTextAlign,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.95f),
                                    modifier = Modifier.size(
                                        KmiIconSize.medium
                                    )
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
                        containerColor = contentColor,
                        contentColor = containerColor,
                        disabledContainerColor = contentColor.copy(alpha = 0.55f),
                        disabledContentColor = containerColor.copy(alpha = 0.70f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(
                                KmiIconSize.medium
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = buyText,
                            style = KmiTypography.action
                        )
                    }
                }
            }
        }
    }
}
