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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import il.kmi.shared.domain.Belt
import java.net.URLDecoder
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri

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

private const val DEV_ADMIN_CODE = "040483455"   // 👈 קוד אדמין פנימי בלבד

/* ------------------------------
   Parser למפתח תרגיל מהחיפוש
   ------------------------------ */

// "belt|topic|item" / "belt::topic::item" / "belt/topic/item"
private fun parseKey(key: String): Triple<Belt, String, String> {
    fun dec(s: String): String =
        runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

    val parts0: List<String> = when {
        '|'  in key -> key.split('|',  limit = 3)
        "::" in key -> key.split("::", limit = 3)
        '/'  in key -> key.split('/',  limit = 3)
        else        -> listOf("", "", "")
    }
    val parts: List<String> = (parts0 + listOf("", "", "")).take(3)

    val belt: Belt  = Belt.fromId(parts[0]) ?: Belt.WHITE
    val topic: String = dec(parts[1])
    val item: String  = dec(parts[2])

    return Triple(belt, topic, item)
}

/* ------------------------------
   מסך ניהול מנוי
   ------------------------------ */

@Composable
private fun PremiumSubscriptionButton(
    text: String,
    onClick: () -> Unit
) {
    val glow = rememberInfiniteTransition(label = "premium_cta_glow")
    val bubbleOffset = glow.animateFloat(
        initialValue = -140f,
        targetValue = 320f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Restart
        ),
        label = "premium_cta_bubble"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB794F6).copy(alpha = 0.38f),
                            Color(0xFF8B5CF6).copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
        )

        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(34.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(34.dp),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color(0xFFD8B4FE).copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.35f)
                    )
                )
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFF6D28D9),
                                Color(0xFF5B21B6)
                            )
                        )
                    )
                    .clip(RoundedCornerShape(34.dp))
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.Transparent
                                ),
                                radius = 520f
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .offset(x = bubbleOffset.value.dp)
                        .padding(vertical = 6.dp)
                        .width(120.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.32f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                Color.White.copy(alpha = 0.14f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👑",
                            fontSize = 18.sp
                        )
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(active: Boolean) {

    Box(
        modifier = Modifier
            .size(62.dp)
            .background(
                if (active) {
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFD1FAE5),
                            Color(0xFFECFDF5)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFEE2E2),
                            Color(0xFFFFF1F2)
                        )
                    )
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (active) "✓" else "!",
            fontWeight = FontWeight.ExtraBold,
            color = if (active) Color(0xFF166534) else Color(0xFFDC2626),
            style = MaterialTheme.typography.titleLarge
        )
    }
}

private fun openGooglePlaySubscriptions(context: Context, packageName: String) {
    val deepLink = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
    val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
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
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val layoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    // דיאלוג קוד למפתח (מנהל אפליקציה)
    var showDevDialog by rememberSaveable { mutableStateOf(false) }
    var devCode by rememberSaveable { mutableStateOf("") }

    // ---------- זיהוי מנהל לפי kmi_user/is_manager ----------
    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    var isAdmin by remember {
        mutableStateOf(KmiAccess.isAdmin(userSp))
    }

    val isAuthed by rememberAuthState(ctx)

    // ---------- אם זה אתה (מנהל) – בלי Billing, בלי רכישה ----------
    if (isAdmin) {
        Scaffold(
            topBar = {
                if (!isAuthed) {
                    TopAppBar(title = { Text("ניהול מנוי") })
                } else {
                    il.kmi.app.ui.KmiTopBar(
                        title = if (isEnglish) "Subscription" else "ניהול מנוי",
                        lockSearch = true,
                        showBottomActions = true,
                        showTopHome = true,
                        centerTitle = true,
                        onHome = onOpenHome,      // 👈 כאן החיבור למסך הבית
                        extraActions = { }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF6D4CC2)
                    )
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "מצב מנוי: מנהל מערכת",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Text(
                            "כמנהל מערכת כל התכנים באפליקציה פתוחים עבורך ואין צורך ברכישת מנוי.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    }
                }

                Button(
                    onClick = onOpenHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("חזרה למסך הבית")
                }

                OutlinedButton(
                    onClick = {
                        // יציאה ממצב מנהל
                        KmiAccess.setAdmin(userSp, false)
                        isAdmin = false
                        Toast.makeText(
                            ctx,
                            "יצאת ממצב מנהל.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("יציאה ממצב מנהל")
                }
            }
        }

        // 👈 אין המשך – לא מריצים Billing בכלל למנהל
        return
    }

    // ---------- מכאן והלאה – התנהגות רגילה למשתמשים רגילים ----------

    // עטיפה ב-runCatching כדי שלא יפיל את האפליקציה במקרה של שגיאה
    val repo = remember {
        runCatching { BillingRepository(ctx) }
            .getOrNull()
    }

    LaunchedEffect(repo) {
        repo?.startConnection()
    }

    // state תמיד מסוג SubscriptionState, עם ברירת מחדל כשאין repo
    val state: SubscriptionState =
        repo?.state?.collectAsState()?.value ?: SubscriptionState()

    val showError = state.error?.isNotBlank() == true

    val activePlanLabel = when (state.productId) {

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

    val monthlyPriceLabel = state.monthlyPriceText
        ?: if (isEnglish) "Not loaded yet" else "טרם נטען"

    val yearlyPriceLabel = state.yearlyPriceText
        ?: if (isEnglish) "Not loaded yet" else "טרם נטען"

    val renewalLabel = formatDate(state.renewalDate)

// חיפוש: דיאלוג מקומי אחרי בחירה מהחיפוש (כשזמין)
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            val isAuthed by rememberAuthState(ctx)

            if (!isAuthed) {
                TopAppBar(title = { Text("ניהול מנוי") })
            } else {
                il.kmi.app.ui.KmiTopBar(
                    title = if (isEnglish) "Subscription" else "ניהול מנוי",
                    lockSearch = false,
                    onPickSearchResult = { key -> pickedKey = key },
                    showBottomActions = true,
                    showTopHome = true,
                    centerTitle = true,
                    onHome = onOpenHome,
                    extraActions = { }
                )
            }
        }
    ) { padding ->

        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF7F2FF),
                            Color(0xFFF2F7FF),
                            Color(0xFFFFFBFE)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF7C3AED),
                                        Color(0xFF6D28D9),
                                        Color(0xFF5B21B6)
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (isEnglish) "KMI Subscription" else "ניהול מנוי KMI",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (isEnglish) {
                                    "Here you can check your subscription status, purchase a new subscription, or restore previous purchases."
                                } else {
                                    "כאן אפשר לבדוק סטטוס מנוי, לרכוש מנוי חדש או לשחזר רכישות קיימות."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(26.dp)
                        ),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
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
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFF6B7280),
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = if (state.active) "פעיל" else "לא פעיל",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (state.active) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (state.active) {
                                                    Color(0xFFDCFCE7)
                                                } else {
                                                    Color(0xFFFEE2E2)
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = if (state.active) "מנוי פעיל" else "אין מנוי פעיל",
                                                color = if (state.active) Color(0xFF166534) else Color(0xFFB91C1C),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    StatusIcon(state.active)
                                } else {
                                    StatusIcon(state.active)

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Subscription status",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFF6B7280),
                                            textAlign = TextAlign.Left,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = if (state.active) "Active" else "Inactive",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (state.active) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            textAlign = TextAlign.Left,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (state.active) {
                                                    Color(0xFFDCFCE7)
                                                } else {
                                                    Color(0xFFFEE2E2)
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = if (state.active) "Subscription active" else "No active subscription",
                                                color = if (state.active) Color(0xFF166534) else Color(0xFFB91C1C),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (state.active) {
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = textAlign
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFE5E7EB))
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF8FAFC)
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
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155),
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF64748B),
                                                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                                            )

                                            Text(
                                                text = value,
                                                style = valueStyle,
                                                color = Color(0xFF0F172A),
                                                textAlign = if (isEnglish) TextAlign.Right else TextAlign.Left
                                            )
                                        }
                                    }

                                    DetailsRow(
                                        label = if (isEnglish) "Renewal date:" else "תאריך חידוש:",
                                        value = renewalLabel,
                                        valueStyle = MaterialTheme.typography.bodyMedium
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Plan:" else "מסלול:",
                                        value = activePlanLabel,
                                        valueStyle = MaterialTheme.typography.bodyMedium
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Monthly price:" else "מחיר חודשי:",
                                        value = monthlyPriceLabel,
                                        valueStyle = MaterialTheme.typography.bodyMedium
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Yearly price:" else "מחיר שנתי:",
                                        value = yearlyPriceLabel,
                                        valueStyle = MaterialTheme.typography.bodyMedium
                                    )

                                    DetailsRow(
                                        label = if (isEnglish) "Product ID:" else "מזהה מוצר:",
                                        value = state.productId ?: "-",
                                        valueStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { openGooglePlaySubscriptions(ctx, ctx.packageName) },
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
                                        containerColor = Color(0xFFFFF1F2)
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
                                            text = if (isEnglish) "Connection error" else "שגיאת חיבור",
                                            color = Color(0xFFB91C1C),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = state.error ?: "",
                                            color = Color(0xFFDC2626),
                                            style = MaterialTheme.typography.bodyMedium,
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
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = if (isEnglish) "More actions" else "פעולות נוספות",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF374151)
                        )

                        if (activity != null) {

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

                            PremiumActionRow(
                                icon = "🔄",
                                text = if (isEnglish) "Restore purchases" else "שחזור רכישות",
                                onClick = { repo?.refreshPurchases() }
                            )
                        }
                    }
                }

                // ---------- אזור נסתר: 5 הקשות לפתיחת דיאלוג קוד מנהל ----------
                var secretTapCount by remember { mutableStateOf(0) }
                var lastTapTime by remember { mutableStateOf(0L) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable {
                            val now = System.currentTimeMillis()

                            secretTapCount =
                                if (now - lastTapTime <= 3000L) secretTapCount + 1 else 1
                            lastTapTime = now

                            if (secretTapCount >= 5) {
                                secretTapCount = 0
                                showDevDialog = true
                            }
                        }
                )

                // ---------- דיאלוג הסבר + כוכבית (אם פתוח) ----------
                pickedKey?.let { key ->
                    val (b, t, itemRaw) = parseKey(key)

                    val ctx2 = LocalContext.current
                    val spFav = remember(ctx2) {
                        ctx2.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
                    }
                    val favKey = remember(b, t) { "fav_${b.id}_$t" }

                    var favSet by remember(favKey) {
                        mutableStateOf(
                            spFav.getStringSet(favKey, emptySet())
                                ?.toMutableSet<String>()
                                ?: mutableSetOf<String>()
                        )
                    }
                    val isFav2 = favSet.contains(itemRaw)

                    fun toggleFavorite() {
                        val s: MutableSet<String> = favSet.toMutableSet()
                        if (!s.add(itemRaw)) s.remove(itemRaw)
                        favSet = s
                        spFav.edit().putStringSet(favKey, s).apply()
                    }

                    val explanation = remember(b, itemRaw) {
                        il.kmi.app.domain.Explanations.get(b, itemRaw).ifBlank {
                            val alt = itemRaw.substringAfter(":", itemRaw).trim()
                            il.kmi.app.domain.Explanations.get(b, alt)
                        }
                    }.ifBlank { "לא נמצא הסבר עבור \"$itemRaw\"." }

                    AlertDialog(
                        onDismissRequest = { pickedKey = null },
                        title = {
                            Box(Modifier.fillMaxWidth()) {
                                IconButton(
                                    onClick = { toggleFavorite() },
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    if (isFav2) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "הסר ממועדפים",
                                            tint = Color(0xFFFFC107)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.StarBorder,
                                            contentDescription = "הוסף למועדפים"
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterEnd),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = itemRaw,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        text = "(${b.heb}${if (t.isNotBlank()) " · $t" else ""})",
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        },
                        text = {
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Right
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { pickedKey = null }) {
                                Text("סגור")
                            }
                        }
                    )
                }

                // ---------- דיאלוג קוד למפתח (DEV) ----------
                if (showDevDialog) {
                    AlertDialog(
                        onDismissRequest = { showDevDialog = false },
                        title = {
                            Text(
                                text = "קוד גישה",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ניתן להזין קוד אדמין או קוד גישה לבודקים כדי לפתוח את האפליקציה ללא מנוי.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = devCode,
                                    onValueChange = { devCode = it },
                                    singleLine = true,
                                    label = { Text("קוד גישה") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val code = devCode.trim()

                                    when {
                                        code == DEV_ADMIN_CODE -> {
                                            KmiAccess.setAdmin(userSp, true)
                                            isAdmin = true
                                            showDevDialog = false
                                            devCode = ""
                                            Toast.makeText(
                                                ctx,
                                                "מצב מנהל הופעל – כל התכנים כעת פתוחים.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        KmiAccess.tryDevUnlock(userSp, code) -> {
                                            showDevDialog = false
                                            devCode = ""
                                            Toast.makeText(
                                                ctx,
                                                "גישה מלאה לבודק הופעלה במכשיר זה.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        else -> {
                                            Toast.makeText(
                                                ctx,
                                                "קוד שגוי.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Text("אישור")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showDevDialog = false
                                    devCode = ""
                                }
                            ) {
                                Text("בטל")
                            }
                        }
                    )
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
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val layoutDirection = if (isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .background(
                    Color(0xFFF8FAFC),
                    RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        Color(0xFFE0E7FF),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(icon)
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = textAlign
            )

            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = Color(0xFF64748B)
            )
        }
    }
}