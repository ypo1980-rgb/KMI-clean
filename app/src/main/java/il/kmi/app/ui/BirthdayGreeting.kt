package il.kmi.app.ui

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.sin

private const val KEY_BIRTH_DAY =
    "birth_day"

private const val KEY_BIRTH_MONTH =
    "birth_month"

private const val KEY_FULL_NAME =
    "fullName"

private const val KEY_LAST_BIRTHDAY_SHOWN =
    "last_birthday_shown"

private const val KEY_LAST_BIRTHDAY_SHOWN_YEAR =
    "last_birthday_shown_year"

private data class BirthdayConfettiParticle(
    val xFraction: Float,
    val startFraction: Float,
    val speed: Float,
    val sway: Float,
    val size: Float,
    val rotation: Float,
    val color: Color
)

@Composable
fun BirthdayGate(
    sp: SharedPreferences,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val isEnglish = remember(context) {
        AppLanguageManager(context).getCurrentLanguage() ==
                AppLanguage.ENGLISH
    }

    val fullName =
        sp.getString(
            KEY_FULL_NAME,
            ""
        )
            ?.trim()
            .orEmpty()

    val birthDay =
        sp.getString(
            KEY_BIRTH_DAY,
            null
        )
            ?.toIntOrNull()

    val birthMonth =
        sp.getString(
            KEY_BIRTH_MONTH,
            null
        )
            ?.toIntOrNull()

    var showBirthday by remember {
        mutableStateOf(false)
    }

    var birthdayYearToSave by remember {
        mutableStateOf<Int?>(null)
    }

    /*
     * הבדיקה מתבצעת בכל יצירה של BirthdayGate.
     *
     * אם יום ההולדת של השנה כבר עבר, תוצג הברכה
     * בכניסה הראשונה שלאחריו. לפני יום ההולדת נבדק
     * יום ההולדת האחרון מהשנה הקודמת.
     */
    LaunchedEffect(
        birthDay,
        birthMonth
    ) {
        val today = LocalDate.now()

        val mostRecentBirthday =
            resolveMostRecentBirthday(
                today = today,
                birthDay = birthDay,
                birthMonth = birthMonth
            )

        if (mostRecentBirthday == null) {
            showBirthday = false
            birthdayYearToSave = null
            return@LaunchedEffect
        }

        val lastShownYear =
            readLastBirthdayShownYear(sp)

        if (
            lastShownYear == null ||
            lastShownYear <
            mostRecentBirthday.year
        ) {
            birthdayYearToSave =
                mostRecentBirthday.year

            showBirthday = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }

    if (showBirthday) {
        BirthdayCelebrationDialog(
            fullName = fullName,
            isEnglish = isEnglish,
            onContinue = {
                val shownYear =
                    birthdayYearToSave
                        ?: LocalDate.now().year

                sp.edit {
                    putInt(
                        KEY_LAST_BIRTHDAY_SHOWN_YEAR,
                        shownYear
                    )
                    putString(
                        KEY_LAST_BIRTHDAY_SHOWN,
                        LocalDate.now().toString()
                    )
                }

                showBirthday = false
            }
        )
    }
}

@Composable
private fun BirthdayCelebrationDialog(
    fullName: String,
    isEnglish: Boolean,
    onContinue: () -> Unit
) {
    /*
     * לא מאפשרים לסגור את הברכה בלחיצה מחוץ למסך
     * או בלחצן החזור. הסגירה מתבצעת דרך כפתור ההמשך,
     * כדי שההצגה תירשם בצורה עקבית.
     */
    BackHandler(enabled = true) {
        // intentionally blocked
    }

    Dialog(
        onDismissRequest = {
            // intentionally blocked
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        BirthdayCelebrationContent(
            fullName = fullName,
            isEnglish = isEnglish,
            onContinue = onContinue
        )
    }
}

@Composable
private fun BirthdayCelebrationContent(
    fullName: String,
    isEnglish: Boolean,
    onContinue: () -> Unit
) {
    val celebrationTransition =
        rememberInfiniteTransition(
            label = "birthday_celebration"
        )

    val animationPhase by
    celebrationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5200
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "birthday_confetti_phase"
    )

    val trophyScale by
    celebrationTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "birthday_trophy_scale"
    )

    val trophyRotation by
    celebrationTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "birthday_trophy_rotation"
    )

    var revealContent by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        revealContent = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.88f
                            )
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            BirthdayConfetti(
                animationPhase = animationPhase,
                modifier = Modifier.fillMaxSize()
            )

            /*
             * הילה חגיגית מאחורי הכרטיס.
             */
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(330.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD166)
                                    .copy(alpha = 0.24f),
                                Color(0xFF8B5CF6)
                                    .copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            AnimatedVisibility(
                visible = revealContent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.90f)
                    .padding(horizontal = 22.dp),
                enter =
                    fadeIn(
                        animationSpec = tween(650)
                    ) +
                            scaleIn(
                                initialScale = 0.86f,
                                animationSpec = tween(650)
                            )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.97f
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 18.dp
                            ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        /*
                         * רק תוכן הברכה נגלל. הכפתור נשאר
                         * קבוע ונגיש בתחתית הכרטיס.
                         */
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(
                                    rememberScrollState()
                                ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .graphicsLayer {
                                        scaleX = trophyScale
                                        scaleY = trophyScale
                                        rotationZ = trophyRotation
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFF3B0),
                                                Color(0xFFFFD166),
                                                Color(0xFFF59E0B)
                                            )
                                        )
                                    ),
                                contentAlignment =
                                    Alignment.Center
                            ) {
                                Text(
                                    text = "🏆",
                                    style = KmiTypography.metric
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text =
                                    when {
                                        isEnglish &&
                                                fullName.isNotBlank() ->
                                            "Congratulations, $fullName!"

                                        isEnglish ->
                                            "Congratulations!"

                                        fullName.isNotBlank() ->
                                            "מזל טוב, $fullName!"

                                        else ->
                                            "מזל טוב!"
                                    },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = KmiTypography.screenTitle.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    if (isEnglish) {
                                        "Happy Birthday 🎉"
                                    } else {
                                        "יום הולדת שמח 🎉"
                                    },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                style = KmiTypography.sectionTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(
                                modifier = Modifier.height(14.dp)
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.72f
                                ),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Text(
                                    text =
                                        if (isEnglish) {
                                            "Wishing you a year of health, " +
                                                    "success, perseverance and progress.\n\n" +
                                                    "May you continue to grow stronger, " +
                                                    "learn and achieve every goal — " +
                                                    "in training and in life."
                                        } else {
                                            "מאחלים לך שנה של בריאות, " +
                                                    "הצלחה, התמדה והתקדמות.\n\n" +
                                                    "שתמשיך להתחזק, ללמוד " +
                                                    "ולהגיע לכל יעד — " +
                                                    "באימון ובחיים."
                                        },
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 15.dp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = KmiTypography.body.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )
                        }

                        Text(
                            text =
                                "🎂  🎈  ✨  🥋  ✨  🎈  🎂",
                            modifier = Modifier.fillMaxWidth(),
                            style = KmiTypography.action,
                            textAlign = TextAlign.Center
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Button(
                            onClick = onContinue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primary,
                                contentColor =
                                    MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 1.dp,
                                disabledElevation = 0.dp
                            )
                        ) {
                            Text(
                                text =
                                    if (isEnglish) {
                                        "Continue to the app"
                                    } else {
                                        "המשך לאפליקציה"
                                    },
                                style = KmiTypography.action.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BirthdayConfetti(
    animationPhase: Float,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(
            Color(0xFFFFD166),
            Color(0xFFFF5C8A),
            Color(0xFF5EEAD4),
            Color(0xFF60A5FA),
            Color(0xFFC084FC),
            Color(0xFFFFFFFF)
        )
    }

    val particles = remember {
        List(48) { index ->
            BirthdayConfettiParticle(
                xFraction =
                    ((index * 37) % 100) / 100f,
                startFraction =
                    ((index * 19) % 100) / 100f,
                speed =
                    0.55f +
                            ((index * 13) % 40) / 100f,
                sway =
                    8f +
                            ((index * 11) % 22),
                size =
                    4f +
                            ((index * 7) % 8),
                rotation =
                    ((index * 29) % 360).toFloat(),
                color =
                    colors[index % colors.size]
            )
        }
    }

    Canvas(modifier = modifier) {
        particles.forEachIndexed { index, particle ->
            val progress =
                (
                        particle.startFraction +
                                animationPhase *
                                particle.speed
                        ) % 1f

            val x =
                particle.xFraction * size.width +
                        sin(
                            (
                                    animationPhase *
                                            6.28f
                                    ) +
                                    index
                        ) *
                        particle.sway

            val y =
                progress * size.height

            rotate(
                degrees =
                    particle.rotation +
                            animationPhase * 360f,
                pivot = Offset(x, y)
            ) {
                drawCircle(
                    color = particle.color,
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun resolveMostRecentBirthday(
    today: LocalDate,
    birthDay: Int?,
    birthMonth: Int?
): LocalDate? {
    if (
        birthDay == null ||
        birthMonth == null ||
        birthMonth !in 1..12 ||
        birthDay !in 1..31
    ) {
        return null
    }

    val birthdayThisYear =
        birthdayDateForYear(
            year = today.year,
            month = birthMonth,
            day = birthDay
        )
            ?: return null

    return if (
        birthdayThisYear.isAfter(today)
    ) {
        birthdayDateForYear(
            year = today.year - 1,
            month = birthMonth,
            day = birthDay
        )
    } else {
        birthdayThisYear
    }
}

private fun birthdayDateForYear(
    year: Int,
    month: Int,
    day: Int
): LocalDate? {
    return try {
        val yearMonth =
            YearMonth.of(
                year,
                month
            )

        /*
         * משתמש שנולד ב־29 בפברואר יקבל בשנה שאינה
         * מעוברת את הברכה ביום האחרון של פברואר.
         */
        LocalDate.of(
            year,
            month,
            day.coerceAtMost(
                yearMonth.lengthOfMonth()
            )
        )
    } catch (_: DateTimeException) {
        null
    }
}

private fun readLastBirthdayShownYear(
    sp: SharedPreferences
): Int? {
    if (
        sp.contains(
            KEY_LAST_BIRTHDAY_SHOWN_YEAR
        )
    ) {
        return sp.getInt(
            KEY_LAST_BIRTHDAY_SHOWN_YEAR,
            0
        )
            .takeIf { it > 0 }
    }

    /*
     * תאימות לגרסה הישנה ששמרה תאריך מלא כמחרוזת.
     */
    return sp.getString(
        KEY_LAST_BIRTHDAY_SHOWN,
        null
    )
        ?.let { storedDate ->
            runCatching {
                LocalDate.parse(
                    storedDate
                ).year
            }
                .getOrNull()
        }
}