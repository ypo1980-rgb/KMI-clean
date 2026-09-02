package il.kmi.app.subscription

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.scaledIconSize
import il.yuval.ui.theme.kmiGraniteActionBrush
import il.yuval.ui.theme.kmiGraniteActionHighlightColor
import il.yuval.ui.theme.kmiOnSuccessContainerColor
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.yuval.ui.theme.kmiSectionHeaderBrush
import il.yuval.ui.theme.kmiSectionHeaderContentColor
import il.yuval.ui.theme.kmiSuccessColor
import il.yuval.ui.theme.kmiSuccessContainerColor
import kotlin.time.Duration.Companion.milliseconds


/* ------------------------------
   עזר: זיהוי משתמש מחובר (מרוכך)
   ------------------------------ */

private fun isUserAuthedRelaxed(ctx: Context): Boolean {
    val spKmi  = ctx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
    val spUser = ctx.getSharedPreferences("kmi_user",      Context.MODE_PRIVATE)

    val spFlag   = spKmi.getBoolean("is_registered", false)
    val userId   = spKmi.getString("user_id", null).orEmpty()
    val profName = spKmi.getString("profile_name", null).orEmpty()
    val role     = spUser.getString("user_role", null).orEmpty()

    val fbOk = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null }
        .getOrDefault(false)

    // מספיק אחד מהסימנים הבולטים
    return spFlag || userId.isNotBlank() || profName.isNotBlank() || role.equals("coach", true) || fbOk
}

@Composable
private fun rememberAuthState(ctx: Context): State<Boolean> {
    val state = remember { mutableStateOf(isUserAuthedRelaxed(ctx)) }

    DisposableEffect(ctx) {
        val spKmi  = ctx.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
        val spUser = ctx.getSharedPreferences("kmi_user",      Context.MODE_PRIVATE)

        val l1 = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            state.value = isUserAuthedRelaxed(ctx)
        }
        spKmi.registerOnSharedPreferenceChangeListener(l1)
        spUser.registerOnSharedPreferenceChangeListener(l1)

        val fbAuth = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }.getOrNull()
        val fbL = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
            state.value = isUserAuthedRelaxed(ctx)
        }
        fbAuth?.addAuthStateListener(fbL)

        onDispose {
            spKmi.unregisterOnSharedPreferenceChangeListener(l1)
            spUser.unregisterOnSharedPreferenceChangeListener(l1)
            fbAuth?.removeAuthStateListener(fbL)
        }
    }
    return state
}

/* ------------------------------
   מסך ניהול מנוי
   ------------------------------ */

@Composable
private fun PremiumSubscriptionButton(
    text: String,
    onClick: () -> Unit
) {
    val glow =
        rememberInfiniteTransition(
            label = "premium_cta_glow"
        )

    val bubbleOffset by glow.animateFloat(
        initialValue = -140f,
        targetValue = 320f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 2600
                    ),
                repeatMode = RepeatMode.Restart
            ),
        label = "premium_cta_bubble"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color =
                kmiSectionHeaderContentColor()
                    .copy(alpha = 0.68f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clip(
                    RoundedCornerShape(22.dp)
                )
                .background(
                    brush = kmiGraniteActionBrush()
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                )
        ) {

            // אנימציית האור שנעה לרוחב הכפתור
            Box(
                modifier = Modifier
                    .offset(
                        x = bubbleOffset.dp
                    )
                    .width(110.dp)
                    .fillMaxHeight()
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        kmiGraniteActionHighlightColor(),
                                        kmiGraniteActionHighlightColor()
                                            .copy(alpha = 0.16f),
                                        Color.Transparent
                                    )
                            )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style =
                        KmiTypography.sectionTitle.copy(
                            fontWeight =
                                FontWeight.ExtraBold
                        ),
                    color =
                        kmiSectionHeaderContentColor(),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(active: Boolean) {
    val containerColor =
        if (active) {
            kmiSuccessContainerColor()
        } else {
            MaterialTheme.colorScheme.errorContainer
        }

    val contentColor =
        if (active) {
            kmiOnSuccessContainerColor()
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }

    Box(
        modifier = Modifier
            .size(scaledIconSize(62.dp))
            .background(
                brush =
                    Brush.radialGradient(
                        listOf(
                            containerColor,
                            containerColor.copy(alpha = 0.72f)
                        )
                    ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (active) "✓" else "!",
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
            style = KmiTypography.sectionTitle
        )
    }
}

private fun openGooglePlaySubscriptions(
    context: Context,
    packageName: String,
    productId: String?
) {
    val safeProductId = productId?.takeIf { it.isNotBlank() }

    val deepLink =
        if (safeProductId != null) {
            (
                    "https://play.google.com/store/account/subscriptions" +
                            "?sku=$safeProductId&package=$packageName"
                    ).toUri()
        } else {
            (
                    "https://play.google.com/store/account/subscriptions" +
                            "?package=$packageName"
                    ).toUri()
        }

    val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(
            context,
            "לא ניתן לפתוח את מסך ניהול המנוי ב־Google Play",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun formatDate(ts: Long?): String {
    if (ts == null) return "-"
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenHome: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val ctx = LocalContext.current
    val activity = ctx as? Activity

    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val textAlign =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val horizontalAlign =
        if (isEnglish) Alignment.Start else Alignment.End

    val layoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    val mainCardColor =
        MaterialTheme.colorScheme.surface

    val innerCardColor =
        MaterialTheme.colorScheme.surfaceVariant

    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val subsSp = remember {
        ctx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE)
    }

    // עטיפה ב-runCatching כדי שלא יפיל את האפליקציה במקרה של שגיאה.
    // חשוב:
    // לא מפעילים כאן startConnection אוטומטית.
    // אחרת כניסה למסך מנוי משחזרת מנוי פעיל מ-Google Play
    // ופותחת נעילה בלי שהמשתמש לחץ על רכישה.
    val repo = remember {
        runCatching { BillingRepository(ctx) }
            .getOrNull()
    }

    // state תמיד מסוג SubscriptionState, עם ברירת מחדל כשאין repo
    val state: SubscriptionState =
        repo?.state?.collectAsState()?.value ?: SubscriptionState()

    val showError = state.error?.isNotBlank() == true

    val restoreScope = rememberCoroutineScope()
    var restoreInProgress by rememberSaveable { mutableStateOf(false) }

    var subscriptionUiRefreshTick by remember {
        mutableIntStateOf(0)
    }

    DisposableEffect(userSp, subsSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "has_full_access" ||
                key == "full_access" ||
                key == "subscription_active" ||
                key == "is_subscribed" ||
                key == "sub_product" ||
                key == "sub_access_until" ||
                key == "sub_purchase_time" ||
                key == "access_changed_at"
            ) {
                subscriptionUiRefreshTick++
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)
        subsSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val savedProductId = remember(subscriptionUiRefreshTick, state.productId) {
        state.productId
            ?: userSp.getString("sub_product", null)?.takeIf { it.isNotBlank() }
            ?: subsSp.getString("sub_product", null)?.takeIf { it.isNotBlank() }
    }

    val savedAccessUntil = remember(subscriptionUiRefreshTick, state.renewalDate) {
        val fromUserPrefs = userSp.getLong("sub_access_until", 0L)
        val fromSubsPrefs = subsSp.getLong("sub_access_until", 0L)

        when {
            fromUserPrefs > 0L -> fromUserPrefs
            fromSubsPrefs > 0L -> fromSubsPrefs
            state.renewalDate != null -> state.renewalDate
            else -> null
        }
    }

    val effectiveActive = remember(subscriptionUiRefreshTick, state.active, savedAccessUntil, savedProductId) {
        val now = System.currentTimeMillis()

        val userActive = KmiAccess.hasFullAccess(userSp)
        val subsActive = KmiAccess.hasFullAccess(subsSp)
        val timeActive = (savedAccessUntil ?: 0L) > now

        /*
         * חשוב:
         * SubscriptionScreen כבר לא מפעיל Billing אוטומטית בכניסה למסך.
         * לכן state.active יכול להיות false גם כש-BillingRepository אחר כבר כתב
         * מנוי פעיל ל-SharedPreferences.
         *
         * מקור האמת לתצוגה כאן הוא:
         * sub_access_until בתוקף + דגלי גישה שנשמרו.
         */
        val active =
            timeActive &&
                    (userActive || subsActive || savedProductId != null)

        active
    }

    val activePlanLabel = when (savedProductId) {

        SubscriptionProducts.REGULAR_MONTHLY ->
            if (isEnglish) "Monthly subscription" else "מנוי חודשי"

        SubscriptionProducts.REGULAR_YEARLY ->
            if (isEnglish) "Yearly subscription" else "מנוי שנתי"

        SubscriptionProducts.MEMBER_MONTHLY ->
            if (isEnglish) "Association monthly subscription" else "מנוי חודשי חבר עמותה"

        SubscriptionProducts.MEMBER_YEARLY ->
            if (isEnglish) "Association yearly subscription" else "מנוי שנתי חבר עמותה"

        else ->
            if (isEnglish) "No selected plan" else "אין מסלול נבחר"
    }

    val monthlyPriceLabel = when (savedProductId) {
        SubscriptionProducts.MEMBER_MONTHLY,
        SubscriptionProducts.MEMBER_YEARLY ->
            state.memberMonthlyPriceText

        else ->
            state.regularMonthlyPriceText
    } ?: if (isEnglish) "Not loaded yet" else "טרם נטען"

    val yearlyPriceLabel = when (savedProductId) {
        SubscriptionProducts.MEMBER_MONTHLY,
        SubscriptionProducts.MEMBER_YEARLY ->
            state.memberYearlyPriceText

        else ->
            state.regularYearlyPriceText
    } ?: if (isEnglish) "Not loaded yet" else "טרם נטען"

    val renewalLabel = formatDate(savedAccessUntil)

    Scaffold(
        topBar = {
            val isAuthed by rememberAuthState(ctx)

            il.kmi.app.ui.KmiTopBar(
                title =
                    if (isEnglish) {
                        "Subscription"
                    } else {
                        "ניהול מנוי"
                    },
                lockSearch = !isAuthed,
                showTopSearch = isAuthed,
                showBottomActions = isAuthed,
                showTopHome = false,
                centerTitle = true,
                onHome = onOpenHome,
                currentLang =
                    if (isEnglish) {
                        "en"
                    } else {
                        "he"
                    },
                onToggleLanguage = {
                    val newLanguage =
                        if (
                            langManager.getCurrentLanguage() ==
                            il.kmi.shared.localization.AppLanguage.HEBREW
                        ) {
                            il.kmi.shared.localization.AppLanguage.ENGLISH
                        } else {
                            il.kmi.shared.localization.AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLanguage)
                    (ctx as? Activity)?.recreate()
                },
                extraActions = { }
            )
        }
    ) { padding ->

        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = kmiScreenBackgroundBrush()
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = kmiSectionHeaderBrush()
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isEnglish) "KMI Subscription" else "ניהול מנוי KAMI",
                                style = KmiTypography.sectionTitle,
                                fontWeight = FontWeight.ExtraBold,
                                color = kmiSectionHeaderContentColor(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )

                            Text(
                                text = if (isEnglish) {
                                    "Here you can check your subscription status, purchase a new subscription, or restore previous purchases."
                                } else {
                                    "כאן אפשר לבדוק סטטוס מנוי, לרכוש מנוי חדש או לשחזר רכישות קיימות."
                                },
                                style = KmiTypography.caption,
                                color =
                                    kmiSectionHeaderContentColor()
                                        .copy(alpha = 0.92f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = mainCardColor,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = horizontalAlign
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isEnglish) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "סטטוס מנוי",
                                            style = KmiTypography.action,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = if (effectiveActive) "פעיל" else "לא פעיל",
                                            style = KmiTypography.screenTitle,
                                            fontWeight = FontWeight.ExtraBold,
                                            color =
                                                if (effectiveActive) {
                                                    kmiSuccessColor()
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                },
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor =
                                                    if (effectiveActive) {
                                                        kmiSuccessContainerColor()
                                                    } else {
                                                        MaterialTheme.colorScheme
                                                            .errorContainer
                                                    }
                                            )
                                        ) {
                                            Text(
                                                text =
                                                    if (effectiveActive) {
                                                        "מנוי פעיל"
                                                    } else {
                                                        "אין מנוי פעיל"
                                                    },
                                                color =
                                                    if (effectiveActive) {
                                                        kmiOnSuccessContainerColor()
                                                    } else {
                                                        MaterialTheme.colorScheme
                                                            .onErrorContainer
                                                    },
                                                style = KmiTypography.action,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    StatusIcon(effectiveActive)
                                } else {
                                    StatusIcon(effectiveActive)

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Subscription status",
                                            style = KmiTypography.action,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Left,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text =
                                                if (effectiveActive) {
                                                    "Active"
                                                } else {
                                                    "Inactive"
                                                },
                                            style = KmiTypography.screenTitle,
                                            fontWeight = FontWeight.ExtraBold,
                                            color =
                                                if (effectiveActive) {
                                                    kmiSuccessColor()
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                },
                                            textAlign = TextAlign.Left,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor =
                                                    if (effectiveActive) {
                                                        kmiSuccessContainerColor()
                                                    } else {
                                                        MaterialTheme.colorScheme
                                                            .errorContainer
                                                    }
                                            )
                                        ) {
                                            Text(
                                                text =
                                                    if (effectiveActive) {
                                                        "Subscription active"
                                                    } else {
                                                        "No active subscription"
                                                    },
                                                color =
                                                    if (effectiveActive) {
                                                        kmiOnSuccessContainerColor()
                                                    } else {
                                                        MaterialTheme.colorScheme
                                                            .onErrorContainer
                                                    },
                                                style = KmiTypography.action,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (effectiveActive) {
                                    if (isEnglish) {
                                        "All app content is currently unlocked for you."
                                    } else {
                                        "כל התכנים באפליקציה פתוחים עבורך כעת."
                                    }
                                } else {
                                    if (isEnglish) {
                                        "To unlock all content, choose an active subscription plan."
                                    } else {
                                        "כדי לפתוח את כל התכנים, יש לבחור מסלול מנוי פעיל."
                                    }
                                },
                                style = KmiTypography.body,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = textAlign
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.28f
                                        )
                                    )
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = innerCardColor,
                                    contentColor =
                                        MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = horizontalAlign
                                ) {
                                    Text(
                                        text = if (isEnglish) "Subscription details" else "פרטי המנוי",
                                        style = KmiTypography.cardTitle,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = textAlign
                                    )

                                    @Composable
                                    fun DetailsRow(label: String, value: String, valueStyle: androidx.compose.ui.text.TextStyle) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                style = KmiTypography.body.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign =
                                                    if (isEnglish) {
                                                        TextAlign.Left
                                                    } else {
                                                        TextAlign.Right
                                                    }
                                            )

                                            Text(
                                                text = value,
                                                style = valueStyle,
                                                color =
                                                    MaterialTheme.colorScheme.onSurface,
                                                textAlign = if (isEnglish) TextAlign.Right else TextAlign.Left
                                            )
                                        }
                                    }

                                    DetailsRow(
                                        label = if (isEnglish) "Renewal date:" else "תאריך חידוש:",
                                        value = renewalLabel,
                                        valueStyle = KmiTypography.body
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Plan:" else "מסלול:",
                                        value = activePlanLabel,
                                        valueStyle = KmiTypography.body
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Monthly price:" else "מחיר חודשי:",
                                        value = monthlyPriceLabel,
                                        valueStyle = KmiTypography.body
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Yearly price:" else "מחיר שנתי:",
                                        value = yearlyPriceLabel,
                                        valueStyle = KmiTypography.body
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Product ID:" else "מזהה מוצר:",
                                        value = savedProductId ?: "-",
                                        valueStyle = KmiTypography.caption
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    openGooglePlaySubscriptions(
                                        context = ctx,
                                        packageName = ctx.packageName,
                                        productId = savedProductId
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) {
                                        "Manage subscription in Google Play"
                                    } else {
                                        "ניהול המנוי ב־Google Play"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (showError) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.errorContainer,
                                        contentColor =
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "Connection error"
                                                } else {
                                                    "שגיאת חיבור"
                                                },
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onErrorContainer,
                                            style = KmiTypography.action,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text =
                                                if (isEnglish) {
                                                    "The billing service is temporarily unavailable. Please try again later."
                                                } else {
                                                    "שירות הרכישה אינו זמין כרגע. נסה שוב מאוחר יותר."
                                                },
                                            color =
                                                MaterialTheme.colorScheme
                                                    .onErrorContainer,
                                            style = KmiTypography.body,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                PremiumSubscriptionButton(
                    text = if (isEnglish) "Buy / Extend subscription" else "רכוש / הארך מנוי",
                    onClick = onOpenPlans
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = mainCardColor,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text =
                                if (isEnglish) {
                                    "More actions"
                                } else {
                                    "פעולות נוספות"
                                },
                            style = KmiTypography.sectionTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (activity != null) {

                            // כפתור הרכישה הישירה היה מיועד לבדיקות בלבד.
                            // כרגע מסתירים אותו כדי שמשתמשים ובודקים יעבדו רק דרך מסך המסלולים הרשמי.
                            /*
                            PremiumActionRow(
                                icon = "💳",
                                text = if (isEnglish) "Direct purchase (tests)" else "רכישה ישירה (בדיקות)",
                                onClick = {

                                    val isAssociationMember =
                                        userSp.getBoolean("is_association_member", false)

                                    val productId =
                                        SubscriptionResolver.resolveMonthlyProduct(isAssociationMember)

                                    if (repo != null && state.connected) {
                                        repo.launchPurchase(
                                            activity,
                                            productId
                                        )
                                    } else {
                                        Toast.makeText(
                                            ctx,
                                            if (isEnglish) "Billing service is unavailable on this device." else "שירות הרכישה אינו זמין במכשיר.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )
                            */

                            PremiumActionRow(
                                icon = if (restoreInProgress) "⏳" else "🔄",
                                text = when {
                                    restoreInProgress && isEnglish -> "Restoring purchases..."
                                    restoreInProgress -> "משחזר רכישות..."
                                    isEnglish -> "Restore purchases"
                                    else -> "שחזור רכישות"
                                },
                                onClick = {
                                    if (restoreInProgress) return@PremiumActionRow

                                    if (repo == null) {
                                        Toast.makeText(
                                            ctx,
                                            if (isEnglish) {
                                                "Billing service is unavailable on this device."
                                            } else {
                                                "שירות הרכישה אינו זמין במכשיר הזה."
                                            },
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@PremiumActionRow
                                    }

                                    restoreScope.launch {
                                        restoreInProgress = true

                                        Toast.makeText(
                                            ctx,
                                            if (isEnglish) {
                                                "Checking Google Play purchases..."
                                            } else {
                                                "בודק רכישות מול Google Play..."
                                            },
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        runCatching {
                                            repo.startConnection()
                                            delay(700.milliseconds)
                                            repo.refreshPurchases()
                                            delay(900.milliseconds)

                                            // ✅ מכריח את מסך ניהול המנוי לקרוא שוב את הנתונים שנשמרו
                                            subscriptionUiRefreshTick++
                                        }.onFailure {
                                            Toast.makeText(
                                                ctx,
                                                if (isEnglish) {
                                                    "Could not restore purchases. Please try again."
                                                } else {
                                                    "לא ניתן היה לשחזר רכישות. נסה שוב."
                                                },
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }.onSuccess {
                                            Toast.makeText(
                                                ctx,
                                                if (isEnglish) {
                                                    "Restore completed. If an active subscription exists, it will appear here."
                                                } else {
                                                    "השחזור הסתיים. אם קיים מנוי פעיל, הוא יוצג כאן."
                                                },
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        restoreInProgress = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumActionRow(
    icon: String,
    text: String,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember {
        il.kmi.shared.localization.AppLanguageManager(ctx)
    }
    val isEnglish =
        langManager.getCurrentLanguage() ==
                il.kmi.shared.localization.AppLanguage.ENGLISH

    val textAlign =
        if (isEnglish) TextAlign.Left else TextAlign.Right

    val layoutDirection =
        if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(scaledIconSize(34.dp))
                    .background(
                        color =
                            MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = KmiTypography.cardTitle
                )
            }

            Text(
                text = text,
                style = KmiTypography.body,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = textAlign
            )

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(KmiIconSize.medium)
            )
        }
    }
}