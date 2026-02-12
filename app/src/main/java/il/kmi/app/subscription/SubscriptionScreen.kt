package il.kmi.app.subscription

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

private const val DEV_ADMIN_CODE = "123456"   // 👈 תחליף למה שבא לך

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenHome: () -> Unit,
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    // דיאלוג קוד למפתח (מנהל אפליקציה)
    var showDevDialog by rememberSaveable { mutableStateOf(false) }
    var devCode by rememberSaveable { mutableStateOf("") }

    // ---------- זיהוי מנהל לפי kmi_user/is_manager ----------
    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    var isAdmin by remember {
        mutableStateOf(userSp.getBoolean("is_manager", false))
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
                        title = "ניהול מנוי",
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

            Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "מצב מנוי: מנהל מערכת",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "כמנהל מערכת כל התכנים באפליקציה פתוחים עבורך ואין צורך ברכישת מנוי.",
                            style = MaterialTheme.typography.bodyMedium
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
                        userSp.edit().putBoolean("is_manager", false).apply()
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

    // חיפוש: דיאלוג מקומי אחרי בחירה מהחיפוש (כשזמין)
    var pickedKey by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            val isAuthed by rememberAuthState(ctx)

            if (!isAuthed) {
                TopAppBar(title = { Text("ניהול מנוי") })
            } else {
                il.kmi.app.ui.KmiTopBar(
                    title = "ניהול מנוי",
                    lockSearch = false,
                    onPickSearchResult = { key -> pickedKey = key },
                    showBottomActions = true,
                    showTopHome = true,
                    centerTitle = true,
                    onHome = onOpenHome,      // 👈 גם כאן – לחיצה על הבית חוזרת למסך הבית
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

            // כרטיס סטטוס
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (state.active) "המנוי פעיל" else "אין מנוי פעיל",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("מוצר: ${state.productId ?: "-"}")
                    if (showError) {
                        Text(
                            text = "שגיאה: ${state.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // מעבר למסך המסלולים (שם מתבצעת הרכישה בפועל)
            Button(
                onClick = onOpenPlans,
                modifier = Modifier.fillMaxWidth()
            ) { Text("רכוש / הארך מנוי") }

            if (activity != null) {
                TextButton(
                    onClick = {
                        if (repo != null && state.connected) {
                            repo.launchPurchase(activity)
                        } else {
                            Toast.makeText(
                                ctx,
                                "שירות הרכישה אינו זמין במכשיר.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    enabled = (repo != null && state.connected),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("רכישה ישירה (בדיקות)") }

                OutlinedButton(
                    onClick = { repo?.refreshPurchases() },
                    enabled = (repo != null && state.connected),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("שחזור רכישות") }
            }

            // ---------- אזור נסתר: 5 הקשות לפתיחת דיאלוג קוד מנהל ----------
            var secretTapCount by remember { mutableStateOf(0) }
            var lastTapTime by remember { mutableStateOf(0L) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)          // גובה האזור שמתחת לכפתור "שחזור רכישות"
                    .padding(top = 8.dp)
                    .clickable {
                        val now = System.currentTimeMillis()

                        // אם עברו יותר מ־3 שניות – מתחילים ספירה מחדש
                        secretTapCount =
                            if (now - lastTapTime <= 3000L) secretTapCount + 1 else 1
                        lastTapTime = now

                        if (secretTapCount >= 5) {
                            secretTapCount = 0
                            showDevDialog = true      // פותח את דיאלוג הקוד
                        }
                    }
            ) {

        // ---------- דיאלוג הסבר + כוכבית (אם פתוח) ----------
        pickedKey?.let { key ->
            val (b, t, itemRaw) = parseKey(key)

            // SharedPreferences למועדפים
            val ctx2 = LocalContext.current
            val spFav = remember(ctx2) {
                ctx2.getSharedPreferences("kmi_settings", Context.MODE_PRIVATE)
            }
            val favKey = remember(b, t) { "fav_${b.id}_$t" }

            // סט מועדפים טיפוסי ומגובה ב־remember
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
                if (!s.add(itemRaw)) s.remove(itemRaw)   // toggle
                favSet = s
                spFav.edit().putStringSet(favKey, s).apply()
            }

            // טקסט ההסבר (עם פולבאק)
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
                        // ⭐ הכוכבית תמיד בצד שמאל
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
                        // כותרת מימין
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
                    TextButton(onClick = { pickedKey = null }) { Text("סגור") }
                }
            )
        }

                // ---------- דיאלוג קוד למפתח (DEV) ----------
                if (showDevDialog) {
                    AlertDialog(
                        onDismissRequest = { showDevDialog = false },
                        title = {
                            Text(
                                text = "כניסת מנהל",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "הכנס קוד מנהל להפעלת גישה מלאה ללא מנוי.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = devCode,
                                    onValueChange = { devCode = it },
                                    singleLine = true,
                                    label = { Text("קוד מנהל") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (devCode == DEV_ADMIN_CODE) {
                                        // מפעיל דגל מנהל ב-kmi_user
                                        userSp.edit().putBoolean("is_manager", true).apply()
                                        isAdmin = true
                                        showDevDialog = false
                                        devCode = ""
                                        Toast.makeText(
                                            ctx,
                                            "מצב מנהל הופעל – כל התכנים כעת פתוחים.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            ctx,
                                            "קוד שגוי.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Text("אישור")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDevDialog = false
                                devCode = ""
                            }) {
                                Text("בטל")
                            }
                        }
                    )
                }
            }
        }
    }
}
