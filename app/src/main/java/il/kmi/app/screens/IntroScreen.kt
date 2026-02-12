package il.kmi.app.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import androidx.compose.animation.core.Easing
import android.view.animation.OvershootInterpolator
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import il.kmi.app.subscription.KmiAccess
import il.kmi.shared.domain.Belt
import android.content.SharedPreferences // ✅ ADD
import android.util.Log // ✅ ADD
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private fun overshootEasing(tension: Float = 2f): Easing =
    Easing { t -> OvershootInterpolator(tension).getInterpolation(t) }

/** ✅ NEW: צבע חגורה (כדי לצייר בלי "רקע לבן" מהתמונה) */
private fun beltColor(belt: Belt): Color = when (belt) {
    Belt.WHITE  -> Color(0xFFF5F5F5)
    Belt.YELLOW -> Color(0xFFFFEB3B)
    Belt.ORANGE -> Color(0xFFFF9800)
    Belt.GREEN  -> Color(0xFF4CAF50)
    Belt.BLUE   -> Color(0xFF2196F3)
    Belt.BROWN  -> Color(0xFF6D4C41)
    Belt.BLACK  -> Color(0xFF111111)
}
// -------------------- prefs -> greeting + belt --------------------

private fun loadFirstName(sp: SharedPreferences): String? {
    val raw = listOf(
        sp.getString("fullName", null),
        sp.getString("user_name", null),
        sp.getString("name", null),
        sp.getString("displayName", null),
        sp.getString("firstName", null),
        sp.getString("first_name", null)
    ).firstOrNull { !it.isNullOrBlank() }

    val fromPrefs = raw
        ?.trim()
        ?.split(' ', limit = 2)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    if (!fromPrefs.isNullOrBlank()) return fromPrefs

    // fallback: FirebaseAuth displayName (אם יש משתמש מחובר)
    val fbName = try {
        FirebaseAuth.getInstance().currentUser?.displayName
    } catch (_: Throwable) {
        null
    }

    return fbName
        ?.trim()
        ?.split(' ', limit = 2)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

private fun loadBeltId(sp: SharedPreferences): String? {
    // ✅ לפי הלוג שלך: belt_current = yellow
    return listOf(
        sp.getString("belt_current", null), // ✅ FIRST
        sp.getString("beltId", null),
        sp.getString("belt_id", null),
        sp.getString("belt", null),
        sp.getString("belt_id_str", null)
    ).firstOrNull { !it.isNullOrBlank() }
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

@Composable
private fun rememberGreetingAndBelt(userSp: SharedPreferences): Pair<String, Belt?> {
    val firstName = remember { loadFirstName(userSp) }
    val beltId = remember { loadBeltId(userSp) }

    val belt = remember(beltId) { beltId?.let { Belt.fromId(it) } }
    val greeting = remember(firstName) {
        if (firstName.isNullOrBlank()) "שלום" else "שלום $firstName"
    }
    return greeting to belt
}

private suspend fun fetchAndPersistFullNameIfMissing(
    userSp: SharedPreferences
): String? {
    val existing = userSp.getString("fullName", null)?.trim()
    if (!existing.isNullOrBlank()) return existing

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return null

    // fallback מה-auth (לפני פיירסטור)
    val authName = auth.currentUser?.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (!authName.isNullOrBlank()) {
        userSp.edit().putString("fullName", authName).apply()
        return authName
    }

    return try {
        val doc = FirebaseFirestore.getInstance()
            .collection("users")   // ⬅️ אם אצלך זה "trainees" / "profiles" עדכן כאן
            .document(uid)
            .get()
            .await()

        val fullName = (doc.getString("fullName")
            ?: doc.getString("full_name")
            ?: doc.getString("name")
            ?: doc.getString("displayName")
            ?: doc.getString("display_name"))
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (!fullName.isNullOrBlank()) {
            userSp.edit().putString("fullName", fullName).apply()
            fullName
        } else {
            Log.w("KMI_INTRO", "Firestore user doc has no name fields. uid=$uid keys=${doc.data?.keys}")
            null
        }
    } catch (t: Throwable) {
        Log.e("KMI_INTRO", "Failed to fetch name from Firestore", t)
        null
    }
}

private fun dumpPrefs(tag: String, sp: SharedPreferences) {
    val all = sp.all
    Log.d(tag, "prefs '$sp' keys=${all.keys.sorted()}")
    for ((k, v) in all.entries.sortedBy { it.key }) {
        Log.d(tag, "  $k = $v")
    }
}

/** ✅ REPLACE: חגורה מצוירת על הרקע (בלי תמונה עם לבן) */
@Composable
private fun BeltBadge(belt: Belt, modifier: Modifier = Modifier) {
    // ✅ טקסט "חגורה XXX" בצבע החגורה הרלוונטי
    val beltTextColor = when (belt) {
        Belt.WHITE -> Color(0xFFE0E0E0) // לבן-אפרפר, נראה על רקע בהיר
        else -> beltColor(belt)
    }

    fun beltDrawableResOrNull(b: Belt): Int? = when (b) {
        Belt.WHITE  -> R.drawable.belt_white
        Belt.YELLOW -> R.drawable.belt_yellow
        Belt.ORANGE -> R.drawable.belt_orange
        Belt.GREEN  -> R.drawable.belt_green
        Belt.BLUE   -> R.drawable.belt_blue
        Belt.BROWN  -> R.drawable.belt_brown
        Belt.BLACK  -> R.drawable.belt_black
    }

    val res = beltDrawableResOrNull(belt)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ 1) מחליפים סדר: קודם "חגורה צהובה" ואז התמונה
        Text(
            text = belt.heb,
            color = beltTextColor,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(8.dp))

        if (res != null) {
            // ✅ 2) “מורידים” רקע לבן מהתמונה (BlendMode.Modulate)
            // לבן ≈ נהיה רקע המסך; צבעים נשארים אבל יכולים להיות מעט "מושפעים" מהרקע.
            Image(
                painter = painterResource(id = res),
                contentDescription = "חגורה ${belt.heb}",
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        compositingStrategy =
                            androidx.compose.ui.graphics.CompositingStrategy.Offscreen

                        // ✅ פתרון ייעודי לחגורה לבנה:
                        // מוסיף outline / צל עדין כדי שתיראה על רקע בהיר
                        if (belt == Belt.WHITE) {
                            shadowElevation = 6f
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            clip = false
                        }
                    },
                contentScale = ContentScale.Fit,
                // ❗ לא משתמשים ב-tint לחגורה לבנה
                colorFilter = if (belt == Belt.WHITE) null
                else androidx.compose.ui.graphics.ColorFilter.tint(
                    Color.White,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Modulate
                )
            )
        } else {
            // fallback: פס מצויר
            val c = beltColor(belt)
            Canvas(modifier = Modifier.size(width = 92.dp, height = 26.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = c,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.45f, h * 0.45f)
                )
            }
        }
    }
}

@Composable
fun IntroScreen(onContinue: () -> Unit) {
    var startAnim by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val userSp = remember { ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE) }
    var fetchedName by remember { mutableStateOf<String?>(null) }
    var didFetchName by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!didFetchName) {
            didFetchName = true
            fetchedName = fetchAndPersistFullNameIfMissing(userSp)
        }
    }

    // ✅ ADD: גם הקובץ השני כדי להבין איפה נשמר בפועל
    val legacySp = remember { ctx.getSharedPreferences("kmi_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        Log.d("KMI_INTRO", "IntroScreen ACTIVE ✅ (if you see this, you're in the right file)")
        dumpPrefs("KMI_INTRO_USER", userSp)
        dumpPrefs("KMI_INTRO_LEGACY", legacySp)
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "fadeIn"
    )

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.7f,
        animationSpec = tween(durationMillis = 2000, easing = overshootEasing(2f)),
        label = "scaleIn"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gradientAnim")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        KmiAccess.ensureTrialStarted(userSp)
    }

    // ✅ FIX: משתמשים באותו userSp שממנו אתה מתחיל Trial ושבו נשמר המשתמש
    val (dynamicGreeting0, traineeBeltOrNull) = rememberGreetingAndBelt(userSp)
    val dynamicGreeting = remember(dynamicGreeting0, fetchedName) {
        if (!fetchedName.isNullOrBlank()) "שלום ${fetchedName!!.trim().split(' ', limit = 2).first()}"
        else dynamicGreeting0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF512DA8),
                        Color(0xFF673AB7),
                        Color(0xFF2196F3),
                        Color(0xFF03A9F4)
                    ),
                    start = Offset(gradientShift, 0f),
                    end = Offset(0f, gradientShift)
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ק.מ.י",
                    fontSize = 68.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.alpha(alpha).scale(scale)
                )
                Text(
                    "קרב מגן ישראלי",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.alpha(alpha).scale(scale)
                )

                Text(
                    text = dynamicGreeting,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,      // מודגש
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .alpha(alpha)
                        .scale(scale)
                )

                if (traineeBeltOrNull != null) {
                    Spacer(Modifier.height(10.dp))
                    BeltBadge(
                        belt = traineeBeltOrNull,
                        modifier = Modifier
                            .alpha(alpha)
                            .scale(scale)
                    )
                }

                Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.25f)
                    .padding(vertical = 12.dp)
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                val imageScale = (scale * 2.42f).coerceAtMost(2.70f)
                Image(
                    painter = painterResource(id = R.drawable.fighters_blackbelt),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                            clip = false
                        },
                    contentScale = ContentScale.Fit
                )
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2196F3), Color(0xFF673AB7))
                            ),
                            shape = MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "מעבר למסך כניסה / רישום",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
}

/** איור וקטורי עם הגבלה דינמית של סקייל כדי שלא ייחתך מהמסך */
@Composable
private fun FightersIllustration() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // זה הסקייל שתרצה "באידיאל" (היה 1.30 ואז הקטנו 2×5% ≈ 1.235)
        val desiredScale = 1.235f

        // שוליים מינימליים מסביב כדי שלא ייגע בגבולות (אפשר לכוונן)
        val pad = 24.dp.toPx()

        // סקייל מקסימלי שמבטיח שהאיור נשאר בתוך הקנבס + שוליים
        val maxScaleW = (w - 2 * pad) / w
        val maxScaleH = (h - 2 * pad) / h
        val safeScale = minOf(desiredScale, maxScaleW.coerceAtMost(1f), maxScaleH.coerceAtMost(1f))

        withTransform({
            // ממרכזים ומגדילים עד הגבול הבטוח
            scale(scaleX = safeScale, scaleY = safeScale, pivot = Offset(w / 2f, h / 2f))
        }) {
            // “שמש” מאחור
            drawOval(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(w * 0.15f, h * 0.05f),
                size = Size(w * 0.7f, h * 0.7f)
            )

            // סילואטות מינימליות
            val pathLeft = Path().apply {
                moveTo(w * 0.30f, h * 0.65f)
                cubicTo(w * 0.28f, h * 0.58f, w * 0.36f, h * 0.45f, w * 0.42f, h * 0.40f)
                cubicTo(w * 0.46f, h * 0.37f, w * 0.48f, h * 0.32f, w * 0.46f, h * 0.28f)
                cubicTo(w * 0.44f, h * 0.23f, w * 0.38f, h * 0.22f, w * 0.36f, h * 0.26f)
                cubicTo(w * 0.34f, h * 0.30f, w * 0.36f, h * 0.35f, w * 0.34f, h * 0.40f)
                cubicTo(w * 0.31f, h * 0.48f, w * 0.28f, h * 0.55f, w * 0.30f, h * 0.65f)
                close()
            }
            drawPath(pathLeft, color = Color.White.copy(alpha = 0.85f))

            val pathRight = Path().apply {
                moveTo(w * 0.70f, h * 0.70f)
                cubicTo(w * 0.72f, h * 0.60f, w * 0.68f, h * 0.50f, w * 0.62f, h * 0.45f)
                cubicTo(w * 0.58f, h * 0.42f, w * 0.56f, h * 0.36f, w * 0.58f, h * 0.32f)
                cubicTo(w * 0.60f, h * 0.28f, w * 0.66f, h * 0.28f, w * 0.68f, h * 0.32f)
                cubicTo(w * 0.70f, h * 0.36f, w * 0.68f, h * 0.40f, w * 0.70f, h * 0.46f)
                cubicTo(w * 0.73f, h * 0.54f, w * 0.76f, h * 0.60f, w * 0.70f, h * 0.70f)
                close()
            }
            drawPath(pathRight, color = Color.White.copy(alpha = 0.85f))
        }
    }
}
