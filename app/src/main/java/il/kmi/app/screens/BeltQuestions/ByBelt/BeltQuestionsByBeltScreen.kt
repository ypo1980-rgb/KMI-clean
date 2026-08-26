package il.kmi.app.screens.BeltQuestions.ByBelt

import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import il.kmi.app.KmiViewModel
import il.kmi.shared.domain.Belt
import il.kmi.shared.domain.ContentRepo as SharedContentRepo
import il.kmi.app.screens.PracticeByTopicsSelection
import il.kmi.app.screens.PracticeMenuDialog
import il.kmi.app.ui.ext.color
import il.kmi.app.ui.rememberClickSound
import il.kmi.app.ui.rememberHapticsGlobal
import il.kmi.shared.prefs.KmiPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import il.kmi.app.ui.FloatingQuickMenu
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import il.kmi.app.R
import androidx.compose.ui.graphics.drawscope.Stroke
import il.kmi.shared.domain.TopicsEngine
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter
import il.kmi.shared.domain.content.ExerciseTitlesEn
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.ui.graphics.graphicsLayer
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.ui.QuickMenuTriggerMode
import il.kmi.app.ui.KmiTypography
import il.kmi.app.subscription.AccessMode
import il.kmi.app.subscription.AccessModeResolver
import il.kmi.app.subscription.LockedContentPolicy
import il.kmi.app.subscription.KmiAccess
import il.kmi.app.domain.ExerciseExplanationResolver
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import il.kmi.app.screens.BeltQuestions.ByTopic.TopicDetails
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.LocalAppIconScale
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/* ------------------------------ Helpers מקומיים למסך ------------------------------ */

internal fun topicDetailsFor(belt: Belt, topicTitle: String): TopicDetails {
    val details = TopicsEngine.topicDetailsFor(belt, topicTitle.trim())

    val topicTrim = topicTitle.trim()
    val cleanSubs = details.subTitles
        .map { it.trim() }
        .filter { it.isNotBlank() && it != topicTrim }
        .distinct()

    return TopicDetails(
        // מספרי תרגילים כבר מגיעים רק מ-ExerciseCountProvider.
        // השדה הזה נשאר 0 כדי למנוע שימוש בטעות בספירה הישנה.
        itemCount = 0,
        subTitles = cleanSubs
    )
}

internal fun KmiPrefs.getStringCompat(key: String): String? = try {
    val c = this::class.java
    val m1 = c.methods.firstOrNull { it.name == "getString" && it.parameterTypes.size == 1 }
    val m2 = c.methods.firstOrNull { it.name == "getString" && it.parameterTypes.size == 2 }
    when {
        m1 != null -> m1.invoke(this, key) as? String
        m2 != null -> m2.invoke(this, key, null) as? String
        else -> null
    }
} catch (_: Exception) {
    null
}

internal fun findExplanationForHit(
    belt: Belt,
    rawItem: String,
    topic: String,
    lang: AppLanguage
): String {
    val isEnglish = lang == AppLanguage.ENGLISH

    val display = ExerciseTitleFormatter
        .displayName(rawItem)
        .ifBlank { rawItem }
        .trim()

    val resolved = ExerciseExplanationResolver.get(
        belt = belt,
        topic = topic,
        item = display,
        isEnglish = isEnglish
    ).trim()

    val cleaned = if ("::" in resolved) {
        resolved
            .split("::")
            .map { it.trim() }
            .lastOrNull { it.isNotBlank() }
            ?: resolved
    } else {
        resolved
    }.trim()

    val isFallback = if (isEnglish) {
        cleaned.isBlank() ||
                cleaned.startsWith("Detailed explanation for:") ||
                cleaned.startsWith("There is currently no explanation")
    } else {
        cleaned.isBlank() ||
                cleaned.startsWith("הסבר מפורט על") ||
                cleaned.startsWith("אין כרגע")
    }

    if (!isFallback) {
        return cleaned
    }

    return if (isEnglish) {
        "There is currently no explanation for this exercise."
    } else {
        "אין כרגע הסבר לתרגיל הזה."
    }
}

internal fun formatCount(n: Int, lang: AppLanguage): String = when {
    lang == AppLanguage.ENGLISH -> {
        when {
            n <= 0 -> "0 exercises"
            n == 1 -> "1 exercise"
            else -> "$n exercises"
        }
    }

    else -> {
        when {
            n <= 0 -> "0 תרגילים"
            n == 1 -> "תרגיל 1"
            else -> "$n תרגילים"
        }
    }
}

private fun SharedContentRepo.SubTopic.totalExercisesCountDeep(): Int {
    val directCount = items.size
    val nestedCount = subTopics.sumOf { it.totalExercisesCountDeep() }
    return directCount + nestedCount
}

private fun topicExercisesCountForCurrentBelt(
    belt: Belt,
    topicTitle: String
): Int {
    val cleanTopicTitle = topicTitle.trim()

    return SharedContentRepo
        .getSubTopicsFor(
            belt = belt,
            topicTitle = cleanTopicTitle
        )
        .sumOf { subTopic ->
            subTopic.totalExercisesCountDeep()
        }
}

private fun subTopicStatsLineForUi(
    subTopic: SharedContentRepo.SubTopic,
    lang: AppLanguage
): String {
    val exercisesCount =
        subTopic.totalExercisesCountDeep()

    return if (lang == AppLanguage.ENGLISH) {
        if (exercisesCount == 1) {
            "1 exercise"
        } else {
            "$exercisesCount exercises"
        }
    } else {
        if (exercisesCount == 1) {
            "תרגיל 1"
        } else {
            "$exercisesCount תרגילים"
        }
    }
}

private data class BeltTopicsPdfSubTopic(
    val title: String,
    val exercisesCount: Int,
    val depth: Int,
    val exercises: List<String>
)

private data class BeltTopicsPdfTopic(
    val title: String,
    val exercisesCount: Int,
    val exercises: List<String>,
    val subTopics: List<BeltTopicsPdfSubTopic>
)

private fun exerciseTitleForPdf(
    rawItem: String,
    lang: AppLanguage
): String {
    val displayName =
        ExerciseTitleFormatter
            .displayName(rawItem)
            .ifBlank { rawItem }
            .trim()

    return if (lang == AppLanguage.ENGLISH) {
        ExerciseTitlesEn.getOrSame(displayName)
    } else {
        displayName
    }
}

private fun SharedContentRepo.SubTopic.toPdfSubTopicRows(
    lang: AppLanguage,
    depth: Int = 0
): List<BeltTopicsPdfSubTopic> {
    val currentRow = BeltTopicsPdfSubTopic(
        title = topicTitleForUi(
            title.trim(),
            lang
        ),
        exercisesCount = totalExercisesCountDeep(),
        depth = depth,
        exercises =
            items
                .asSequence()
                .map { rawItem ->
                    exerciseTitleForPdf(
                        rawItem = rawItem,
                        lang = lang
                    )
                }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
    )

    return buildList {
        add(currentRow)

        subTopics.forEach { nestedSubTopic ->
            addAll(
                nestedSubTopic.toPdfSubTopicRows(
                    lang = lang,
                    depth = depth + 1
                )
            )
        }
    }
}

private fun buildBeltTopicsPdfData(
    belt: Belt,
    lang: AppLanguage
): List<BeltTopicsPdfTopic> {
    return TopicsEngine.topicTitlesFor(belt)
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .map { topicTitle ->

            val cleanTopicTitle =
                topicTitle.trim()

            val repositorySubTopics =
                SharedContentRepo.getSubTopicsFor(
                    belt = belt,
                    topicTitle = cleanTopicTitle
                )

            /*
             * תרגילים ששייכים ישירות לנושא הראשי.
             *
             * בחלק מהנושאים ContentRepo מחזיק node
             * ששמו זהה לשם הנושא עצמו.
             *
             * בעבר node כזה סונן מה־PDF ולכן שמות
             * התרגילים נעלמו למרות שהספירה נשארה נכונה.
             */
            val directTopicExercises =
                repositorySubTopics
                    .asSequence()
                    .filter { subTopic ->
                        subTopic.title.trim() == cleanTopicTitle
                    }
                    .flatMap { subTopic ->
                        subTopic.items.asSequence()
                    }
                    .map { rawItem ->
                        exerciseTitleForPdf(
                            rawItem = rawItem,
                            lang = lang
                        )
                    }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .toList()

            /*
             * תתי־נושאים אמיתיים בלבד.
             *
             * ה־node ששמו זהה לנושא הראשי לא מוצג
             * כתת־נושא נוסף, משום שהתרגילים הישירים
             * שלו כבר נמצאים ב־directTopicExercises.
             */
            val subTopicRows =
                repositorySubTopics
                    .asSequence()
                    .filter { subTopic ->
                        subTopic.title.trim().isNotBlank()
                    }
                    .filter { subTopic ->
                        subTopic.title.trim() != cleanTopicTitle
                    }
                    .flatMap { subTopic ->
                        subTopic
                            .toPdfSubTopicRows(lang)
                            .asSequence()
                    }
                    .distinctBy { row ->
                        "${row.depth}:${row.title.trim()}"
                    }
                    .toList()

            BeltTopicsPdfTopic(
                title =
                    topicTitleForUi(
                        cleanTopicTitle,
                        lang
                    ),
                exercisesCount =
                    repositorySubTopics.sumOf {
                        it.totalExercisesCountDeep()
                    },
                exercises = directTopicExercises,
                subTopics = subTopicRows
            )
        }
        .toList()
}

private fun shareBeltTopicsPdf(
    context: Context,
    belt: Belt,
    lang: AppLanguage
) {
    val isEnglish = lang == AppLanguage.ENGLISH

    try {
        val pdfFile = createBeltTopicsPdf(
            context = context,
            belt = belt,
            lang = lang
        )

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "${beltTitleForUi(belt, lang)} topics"
                } else {
                    "נושאים ותתי־נושאים – ${beltTitleForUi(belt, lang)}"
                }
            )

            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                if (isEnglish) {
                    "Share belt topics PDF"
                } else {
                    "שיתוף נושאי החגורה"
                }
            )
        )
    } catch (_: Exception) {
        Toast.makeText(
            context,
            if (isEnglish) {
                "The PDF could not be created"
            } else {
                "לא ניתן היה ליצור את קובץ ה־PDF"
            },
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun createBeltTopicsPdf(
    context: Context,
    belt: Belt,
    lang: AppLanguage
): File {
    val isEnglish = lang == AppLanguage.ENGLISH
    val topics = buildBeltTopicsPdfData(belt, lang)

    val pageWidth = 595
    val pageHeight = 842
    val horizontalMargin = 30f
    val contentBottom = 790f

    val navy = android.graphics.Color.rgb(6, 43, 74)
    val blue = android.graphics.Color.rgb(31, 120, 180)
    val lightBlue = android.graphics.Color.rgb(234, 244, 255)
    val softBlue = android.graphics.Color.rgb(247, 251, 255)
    val borderBlue = android.graphics.Color.rgb(190, 215, 235)
    val textDark = android.graphics.Color.rgb(31, 41, 55)
    val textMuted = android.graphics.Color.rgb(92, 110, 128)
    val white = android.graphics.Color.WHITE

    val beltAccent = belt.color.toArgb()

    val regularTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    val boldTypeface =
        Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun translated(hebrew: String, english: String): String =
        if (isEnglish) english else hebrew

    fun textPaint(
        size: Float,
        color: Int = textDark,
        bold: Boolean = false,
        alignment: Paint.Align =
            if (isEnglish) Paint.Align.LEFT else Paint.Align.RIGHT
    ): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface =
                if (bold) {
                    boldTypeface
                } else {
                    regularTypeface
                }
            textAlign = alignment
        }
    }

    fun drawKmiLogo(
        targetCanvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        val outer =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                style = Paint.Style.FILL
            }

        val inner =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = white
                style = Paint.Style.FILL
            }

        val logoText =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                typeface = boldTypeface
                textSize = radius * 0.62f
                textAlign = Paint.Align.CENTER
            }

        targetCanvas.drawCircle(
            cx,
            cy,
            radius,
            outer
        )

        targetCanvas.drawCircle(
            cx,
            cy,
            radius - 4f,
            inner
        )

        targetCanvas.drawText(
            "KAMI",
            cx,
            cy + radius * 0.22f,
            logoText
        )
    }

    val document = PdfDocument()

    var currentPage: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var pageNumber = 0
    var currentY = 0f

    fun finishCurrentPage() {
        currentPage?.let { page ->
            val currentCanvas = page.canvas

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                strokeWidth = 1.5f
            }

            currentCanvas.drawLine(
                horizontalMargin,
                805f,
                pageWidth - horizontalMargin,
                805f,
                footerPaint
            )

            currentCanvas.drawText(
                "K.A.M.I",
                horizontalMargin,
                827f,
                textPaint(
                    size = 10f,
                    color = navy,
                    bold = true,
                    alignment = Paint.Align.LEFT
                )
            )

            currentCanvas.drawText(
                translated(
                    "עמוד $pageNumber",
                    "Page $pageNumber"
                ),
                pageWidth / 2f,
                827f,
                textPaint(
                    size = 9f,
                    color = textMuted,
                    alignment = Paint.Align.CENTER
                )
            )

            currentCanvas.drawText(
                "Krav Maga Israel",
                pageWidth - horizontalMargin,
                827f,
                textPaint(
                    size = 9f,
                    color = textMuted,
                    alignment = Paint.Align.RIGHT
                )
            )

            document.finishPage(page)
        }

        currentPage = null
        canvas = null
    }

    fun startPage() {
        finishCurrentPage()

        pageNumber++

        val page = document.startPage(
            PdfDocument.PageInfo
                .Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                )
                .create()
        )

        currentPage = page
        canvas = page.canvas

        val currentCanvas = page.canvas
        currentCanvas.drawColor(white)

        /*
      * כותרת PDF בסגנון האחיד של דוח מסך הבית:
      * לוגו KAMI + אזור כחול אלכסוני + פסי אקסנט.
      */
        val diagonal =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                style = Paint.Style.FILL
            }

        val accent1 =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(
                    36,
                    103,
                    158
                )
                style = Paint.Style.FILL
            }

        val accent2 =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(
                    128,
                    183,
                    220
                )
                style = Paint.Style.FILL
            }

        /*
         * השטח הכחול העליון.
         */
        val diagonalPath =
            android.graphics.Path().apply {
                moveTo(
                    pageWidth.toFloat(),
                    0f
                )
                lineTo(
                    pageWidth.toFloat(),
                    122f
                )
                lineTo(
                    178f,
                    122f
                )
                lineTo(
                    238f,
                    0f
                )
                close()
            }

        currentCanvas.drawPath(
            diagonalPath,
            diagonal
        )

        /*
         * פס כחול בינוני.
         */
        currentCanvas.drawPath(
            android.graphics.Path().apply {
                moveTo(208f, 122f)
                lineTo(224f, 122f)
                lineTo(284f, 0f)
                lineTo(268f, 0f)
                close()
            },
            accent1
        )

        /*
         * פס כחול בהיר.
         */
        currentCanvas.drawPath(
            android.graphics.Path().apply {
                moveTo(230f, 122f)
                lineTo(238f, 122f)
                lineTo(298f, 0f)
                lineTo(290f, 0f)
                close()
            },
            accent2
        )

        /*
         * לוגו KAMI משמאל.
         */
        drawKmiLogo(
            targetCanvas = currentCanvas,
            cx = 78f,
            cy = 58f,
            radius = 42f
        )

        /*
         * כותרות מימין בעברית ומשמאל באנגלית.
         */
        val headerAlignment =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }

        val headerX =
            if (isEnglish) {
                34f
            } else {
                pageWidth - 34f
            }

        currentCanvas.drawText(
            translated(
                "תרגילים לפי חגורה",
                "Exercises by Belt"
            ),
            headerX,
            50f,
            textPaint(
                size = 24f,
                color = white,
                bold = true,
                alignment = headerAlignment
            )
        )

        currentCanvas.drawText(
            beltTitleForUi(
                belt,
                lang
            ),
            headerX,
            76f,
            textPaint(
                size = 13f,
                color = android.graphics.Color.rgb(
                    220,
                    235,
                    247
                ),
                bold = true,
                alignment = headerAlignment
            )
        )

        val generatedDate =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date())

        currentCanvas.drawText(
            translated(
                "הופק בתאריך $generatedDate",
                "Generated on $generatedDate"
            ),
            headerX,
            96f,
            textPaint(
                size = 9f,
                color = android.graphics.Color.rgb(
                    210,
                    230,
                    245
                ),
                alignment = headerAlignment
            )
        )

        /*
         * בעמוד הראשון מציגים גם כרטיס סיכום,
         * בדומה לכרטיס "אימונים לשבוע הקרוב"
         * בדוח מסך הבית.
         */
        if (pageNumber == 1) {

            val summaryTop = 138f
            val summaryBottom = 198f

            val summaryBackground =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = lightBlue
                    style = Paint.Style.FILL
                }

            val summaryBorder =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = borderBlue
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f
                }

            currentCanvas.drawRoundRect(
                horizontalMargin,
                summaryTop,
                pageWidth - horizontalMargin,
                summaryBottom,
                12f,
                12f,
                summaryBackground
            )

            currentCanvas.drawRoundRect(
                horizontalMargin,
                summaryTop,
                pageWidth - horizontalMargin,
                summaryBottom,
                12f,
                12f,
                summaryBorder
            )

            val totalExercises =
                topics.sumOf {
                    it.exercisesCount
                }

            val summaryX =
                if (isEnglish) {
                    horizontalMargin + 20f
                } else {
                    pageWidth - horizontalMargin - 20f
                }

            val summaryAlignment =
                if (isEnglish) {
                    Paint.Align.LEFT
                } else {
                    Paint.Align.RIGHT
                }

            currentCanvas.drawText(
                translated(
                    "תוכן החגורה",
                    "Belt content"
                ),
                summaryX,
                summaryTop + 24f,
                textPaint(
                    size = 13f,
                    color = blue,
                    bold = true,
                    alignment = summaryAlignment
                )
            )

            currentCanvas.drawText(
                translated(
                    "${topics.size} נושאים · $totalExercises תרגילים",
                    "${topics.size} topics · $totalExercises exercises"
                ),
                summaryX,
                summaryTop + 45f,
                textPaint(
                    size = 11f,
                    color = textDark,
                    bold = true,
                    alignment = summaryAlignment
                )
            )

            currentY = summaryBottom + 18f

        } else {

            /*
             * בעמודי ההמשך אין צורך לחזור על כרטיס הסיכום,
             * וכך נשאר יותר מקום לרשימת התרגילים.
             */
            currentY = 142f
        }
    }

    fun ensureSpace(requiredHeight: Float) {
        if (
            currentPage == null ||
            currentY + requiredHeight > contentBottom
        ) {
            startPage()
        }
    }

    fun drawTopicHeader(
        topic: BeltTopicsPdfTopic,
        continued: Boolean
    ) {
        val currentCanvas = canvas ?: return

        val cardTop = currentY
        val cardBottom = currentY + 54f

        val backgroundPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lightBlue
                style = Paint.Style.FILL
            }

        val borderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderBlue
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }

        currentCanvas.drawRoundRect(
            horizontalMargin,
            cardTop,
            pageWidth - horizontalMargin,
            cardBottom,
            13f,
            13f,
            backgroundPaint
        )

        currentCanvas.drawRoundRect(
            horizontalMargin,
            cardTop,
            pageWidth - horizontalMargin,
            cardBottom,
            13f,
            13f,
            borderPaint
        )

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = beltAccent
            style = Paint.Style.FILL
        }

        val accentLeft =
            if (isEnglish) {
                horizontalMargin
            } else {
                pageWidth - horizontalMargin - 6f
            }

        currentCanvas.drawRoundRect(
            accentLeft,
            cardTop,
            accentLeft + 6f,
            cardBottom,
            6f,
            6f,
            accentPaint
        )

        val textX =
            if (isEnglish) {
                horizontalMargin + 20f
            } else {
                pageWidth - horizontalMargin - 20f
            }

        val alignment =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }

        val displayedTitle =
            if (continued) {
                translated(
                    "${topic.title} – המשך",
                    "${topic.title} – continued"
                )
            } else {
                topic.title
            }

        currentCanvas.drawText(
            displayedTitle,
            textX,
            cardTop + 23f,
            textPaint(
                size = 16f,
                color = navy,
                bold = true,
                alignment = alignment
            )
        )

        currentCanvas.drawText(
            formatCount(topic.exercisesCount, lang),
            textX,
            cardTop + 43f,
            textPaint(
                size = 10.5f,
                color = beltAccent,
                bold = true,
                alignment = alignment
            )
        )

        currentY = cardBottom + 5f
    }

    fun drawSubTopicRow(
        subTopic: BeltTopicsPdfSubTopic,
        rowIndex: Int
    ) {
        val currentCanvas = canvas ?: return

        val rowTop = currentY
        val rowBottom = currentY + 26f

        if (rowIndex % 2 == 0) {
            currentCanvas.drawRoundRect(
                horizontalMargin + 9f,
                rowTop,
                pageWidth - horizontalMargin - 9f,
                rowBottom,
                7f,
                7f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = softBlue
                }
            )
        }

        val depthOffset = subTopic.depth * 13f

        val titleX =
            if (isEnglish) {
                horizontalMargin + 22f + depthOffset
            } else {
                pageWidth - horizontalMargin - 22f - depthOffset
            }

        val countX =
            if (isEnglish) {
                pageWidth - horizontalMargin - 22f
            } else {
                horizontalMargin + 22f
            }

        currentCanvas.drawText(
            subTopic.title,
            titleX,
            rowTop + 17f,
            textPaint(
                size = 11.5f,
                color = textDark,
                bold = subTopic.depth == 0,
                alignment =
                    if (isEnglish) {
                        Paint.Align.LEFT
                    } else {
                        Paint.Align.RIGHT
                    }
            )
        )

        currentCanvas.drawText(
            formatCount(
                subTopic.exercisesCount,
                lang
            ),
            countX,
            rowTop + 17f,
            textPaint(
                size = 9.5f,
                color = textMuted,
                bold = true,
                alignment =
                    if (isEnglish) {
                        Paint.Align.RIGHT
                    } else {
                        Paint.Align.LEFT
                    }
            )
        )

        currentY = rowBottom
    }

    fun drawExerciseRow(
        exerciseTitle: String,
        depth: Int,
        exerciseIndex: Int
    ) {
        val currentCanvas = canvas ?: return

        val rowHeight = 22f
        val rowTop = currentY
        val rowBottom = currentY + rowHeight

        if (exerciseIndex % 2 == 0) {
            currentCanvas.drawRoundRect(
                horizontalMargin + 20f,
                rowTop,
                pageWidth - horizontalMargin - 20f,
                rowBottom,
                5f,
                5f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(
                        250,
                        252,
                        255
                    )
                }
            )
        }

        val depthOffset =
            depth * 13f

        val bulletX =
            if (isEnglish) {
                horizontalMargin + 32f + depthOffset
            } else {
                pageWidth - horizontalMargin - 32f - depthOffset
            }

        val textX =
            if (isEnglish) {
                bulletX + 12f
            } else {
                bulletX - 12f
            }

        val bulletPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = beltAccent
                style = Paint.Style.FILL
            }

        currentCanvas.drawCircle(
            bulletX,
            rowTop + 11f,
            2.2f,
            bulletPaint
        )

        currentCanvas.drawText(
            exerciseTitle,
            textX,
            rowTop + 15f,
            textPaint(
                size = 10.2f,
                color = textDark,
                bold = false,
                alignment =
                    if (isEnglish) {
                        Paint.Align.LEFT
                    } else {
                        Paint.Align.RIGHT
                    }
            )
        )

        currentY = rowBottom
    }

    startPage()

    if (topics.isEmpty()) {
        ensureSpace(100f)

        canvas?.drawText(
            translated(
                "לא נמצאו נושאים להצגה",
                "No topics were found"
            ),
            pageWidth / 2f,
            currentY + 42f,
            textPaint(
                size = 17f,
                color = textMuted,
                bold = true,
                alignment = Paint.Align.CENTER
            )
        )
    } else {
        topics.forEachIndexed { topicIndex, topic ->

            ensureSpace(65f)

            drawTopicHeader(
                topic = topic,
                continued = false
            )

            /*
             * תרגילים ששייכים ישירות לנושא הראשי.
             */
            topic.exercises.forEachIndexed {
                    exerciseIndex,
                    exerciseTitle ->

                if (currentY + 25f > contentBottom) {
                    startPage()

                    drawTopicHeader(
                        topic = topic,
                        continued = true
                    )
                }

                drawExerciseRow(
                    exerciseTitle = exerciseTitle,
                    depth = 0,
                    exerciseIndex = exerciseIndex
                )
            }

            /*
             * רווח קטן בין התרגילים הישירים
             * לבין תתי־הנושאים.
             */
            if (
                topic.exercises.isNotEmpty() &&
                topic.subTopics.isNotEmpty()
            ) {
                currentY += 5f
            }

            if (topic.subTopics.isEmpty()) {

                if (topic.exercises.isEmpty()) {
                    currentY += 8f
                }

            } else {
                topic.subTopics.forEachIndexed {
                        subIndex,
                        subTopic ->

                    /*
                     * מקום לשורת תת־הנושא.
                     */
                    if (currentY + 31f > contentBottom) {
                        startPage()

                        drawTopicHeader(
                            topic = topic,
                            continued = true
                        )
                    }

                    drawSubTopicRow(
                        subTopic = subTopic,
                        rowIndex = subIndex
                    )

                    /*
                     * מדפיסים עכשיו את כל התרגילים
                     * הישירים של תת־הנושא.
                     */
                    subTopic.exercises.forEachIndexed {
                            exerciseIndex,
                            exerciseTitle ->

                        /*
                         * אם התרגיל הבא לא נכנס בעמוד,
                         * ממשיכים לעמוד חדש ומחזירים
                         * את כותרת הנושא ותת־הנושא.
                         */
                        if (currentY + 25f > contentBottom) {
                            startPage()

                            drawTopicHeader(
                                topic = topic,
                                continued = true
                            )

                            drawSubTopicRow(
                                subTopic = subTopic,
                                rowIndex = subIndex
                            )
                        }

                        drawExerciseRow(
                            exerciseTitle = exerciseTitle,
                            depth = subTopic.depth,
                            exerciseIndex = exerciseIndex
                        )
                    }

                    /*
                     * רווח קטן בין תתי־נושאים.
                     */
                    currentY += 3f
                }
            }

            currentY +=
                if (topicIndex == topics.lastIndex) {
                    2f
                } else {
                    13f
                }
        }
    }

    finishCurrentPage()

    val outputDirectory =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    val beltName =
        beltShortNameForUi(
            belt = belt,
            lang = lang
        ).trim()

    val fileName =
        if (isEnglish) {
            "Exercises by Belt - $beltName.pdf"
        } else {
            "תרגילים לפי חגורה - $beltName.pdf"
        }

    val outputFile =
        File(
            outputDirectory,
            fileName
        )

    if (outputFile.exists()) {
        outputFile.delete()
    }

    FileOutputStream(outputFile).use { output ->
        document.writeTo(output)
    }

    document.close()

    return outputFile
}

internal fun beltTitleForUi(belt: Belt, lang: AppLanguage): String =
    if (lang == AppLanguage.ENGLISH) belt.en else belt.heb

internal fun beltShortNameForUi(belt: Belt, lang: AppLanguage): String =
    if (lang == AppLanguage.ENGLISH) {
        belt.en.removeSuffix(" Belt")
    } else {
        belt.heb.removePrefix("חגורה").trim()
    }

internal fun topicTitleForUi(title: String, lang: AppLanguage): String {
    return if (lang == AppLanguage.ENGLISH) {
        ExerciseTitlesEn.getOrSame(title.trim())
    } else {
        title
    }
}

private fun beltTopicImageFor(belt: Belt, topicTitle: String): Int? {
    val clean = topicTitle.trim()

    return when {
        // הגנות
        clean.contains("הגנות") -> R.drawable.topic_defenses

        // שחרורים / חביקות
        clean.contains("שחרורים") ||
                clean.contains("שחרור") ||
                clean.contains("חביקות") ||
                clean.contains("חביקת") -> R.drawable.topic_body_hug_releases

        // מכות מרפק — רק חגורה ירוקה
        belt == Belt.GREEN &&
                clean.contains("מכות מרפק") -> R.drawable.topic_elbow_strikes

        // מכות ידיים / עבודת ידיים
        clean.contains("מכות ידיים") ||
                clean.contains("מכות יד") ||
                clean.contains("עבודת ידיים") ||
                clean.contains("עבודת יד") -> R.drawable.topic_hand_strikes

        // בלימות וגלגולים
        clean.contains("בלימות") ||
                clean.contains("גלגולים") ||
                clean.contains("גלגול") ||
                clean.contains("בלימה") -> R.drawable.topic_forward_roll

        // מניעת התקרבות התוקף — חגורה צהובה
        belt == Belt.YELLOW &&
                (
                        clean.contains("מניעת התקרבות התוקף") ||
                                clean.contains("מניעת התקרבות") ||
                                clean.contains("התקרבות התוקף")
                        ) ->
            R.drawable.topic_prevent_attacker_approach

        // עמידת מוצא
        clean.contains("עמידת מוצא") -> R.drawable.topic_ready_stance

        // עבודת קרקע
        clean.contains("עבודת קרקע") -> R.drawable.topic_ground_fighting

        // בעיטות
        clean.contains("בעיטות") ||
                clean.contains("בעיטה") -> R.drawable.topic_kicks

        // קוואלר
        clean.contains("קוואלר") -> R.drawable.topic_kavaler

        // כללי
        clean.contains("כללי") -> R.drawable.topic_general

        // מקל / רובה — כרגע משתמשים בתמונת הגנות עם נשקים
        clean.contains("מקל") ||
                clean.contains("רובה") -> R.drawable.topic_defenses

        else -> null
    }
}

/* ------------------------------ API ציבורי למסך ------------------------------ */
@Composable
fun BeltQuestionsByBeltScreen(
    vm: KmiViewModel,
    kmiPrefs: KmiPrefs,
    onNext: () -> Unit,
    onBackHome: () -> Unit,
    onOpenByTopic: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit = { _, _, _ -> },
    onOpenWeakPoints: (Belt) -> Unit = {},
    onOpenAllLists: (Belt) -> Unit = {},
    onOpenRandomPractice: (Belt) -> Unit = {},
    onOpenRandomPracticeByTopic: (Belt, String) -> Unit = { _, _ -> },
    onOpenFinalExam: (Belt) -> Unit = {},
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit = {},
    onOpenSummaryScreen: (Belt) -> Unit = {},
    onOpenVoiceAssistant: (Belt) -> Unit,
    onOpenPdfMaterials: (Belt) -> Unit = {}
) {
    BeltPangoLayout(
        vm = vm,
        kmiPrefs = kmiPrefs,
        onNext = onNext,
        onBackHome = onBackHome,
        onOpenByTopic = onOpenByTopic,
        onOpenSubscription = onOpenSubscription,
        onOpenTopic = onOpenTopic,
        onOpenDefenseMenu = onOpenDefenseMenu,
        onOpenSubTopic = onOpenSubTopic,
        onOpenWeakPoints = onOpenWeakPoints,
        onOpenAllLists = onOpenAllLists,
        onOpenRandomPractice = onOpenRandomPractice,
        onOpenRandomPracticeByTopic = onOpenRandomPracticeByTopic,
        onOpenFinalExam = onOpenFinalExam,
        onPracticeByTopics = onPracticeByTopics,
        onOpenSummaryScreen = onOpenSummaryScreen,
        onOpenVoiceAssistant = onOpenVoiceAssistant,
        onOpenPdfMaterials = onOpenPdfMaterials
    )
}

/* -------------------------------- Layout ראשי -------------------------------- */

@Composable
internal fun BeltPangoLayout(
    vm: KmiViewModel,
    kmiPrefs: KmiPrefs,
    onNext: () -> Unit,
    onBackHome: () -> Unit,
    onOpenByTopic: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenWeakPoints: (Belt) -> Unit,
    onOpenAllLists: (Belt) -> Unit,
    onOpenRandomPractice: (Belt) -> Unit,
    onOpenRandomPracticeByTopic: (Belt, String) -> Unit,
    onOpenFinalExam: (Belt) -> Unit,
    onPracticeByTopics: (PracticeByTopicsSelection) -> Unit,
    onOpenSummaryScreen: (Belt) -> Unit,
    onOpenVoiceAssistant: (Belt) -> Unit,
    onOpenPdfMaterials: (Belt) -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember { AppLanguageManager(ctx) }

    val haptic = rememberHapticsGlobal()
    val clickSound = rememberClickSound()
    // הערות תרגילים מנוהלות עכשיו בדיאלוג הגלובלי החדש דרך KmiTopBar
    val userSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_user", Context.MODE_PRIVATE)
    }

    val subsSp = remember(ctx) {
        ctx.getSharedPreferences("kmi_subs", Context.MODE_PRIVATE)
    }

    var accessRefreshTick by remember { mutableIntStateOf(0) }

    // מצב הגישה מתרענן דרך SharedPreferences listener.
    // אין צורך בלולאת רענון קבועה בזמן שהמסך פתוח.

    DisposableEffect(userSp, subsSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "has_full_access" ||
                key == "full_access" ||
                key == "subscription_active" ||
                key == "is_subscribed" ||
                key == "google_subscription_verified" ||
                key == "google_subscription_checked_at" ||
                key == "sub_product" ||
                key == "sub_access_until" ||
                key == "access_changed_at"
            ) {
                accessRefreshTick++
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)
        subsSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
            subsSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val hasManagerAccess = remember(accessRefreshTick) {
        KmiAccess.hasFullAccess(userSp) ||
                KmiAccess.hasFullAccess(subsSp)
    }

    val accessMode = AccessModeResolver.resolve(
        hasManagerAccess = hasManagerAccess
    )

    val hasUnlockedAccess = accessMode == AccessMode.OPEN

    // החיפוש והסברי התרגילים עוברים דרך KmiTopBar + ExercisePremiumSearchDialog

    var showPracticeMenu by rememberSaveable { mutableStateOf(false) }

    // state לתפריט הצף במסך לפי חגורה
    var quickMenuExpanded by rememberSaveable { mutableStateOf(false) }

    /*
     * הקרוסלה מציגה רק חגורות שיש בהן תרגילים.
     * חגורה לבנה אינה מוצגת משום שאין בה חומר לימוד.
     */
    val belts = remember {
        Belt.order.filterNot { belt ->
            belt == Belt.WHITE
        }
    }

    /*
     * חגורת הפתיחה של הקרוסלה נקבעת לפי חגורת המשתמש:
     *
     * חגורה קיימת -> מציגים במרכז את החגורה הבאה (+1).
     * ללא חגורה -> מציגים כתומה.
     *
     * vm.selectedBelt לא משמש לקביעת נקודת הפתיחה,
     * כדי שבחירה ישנה או ערך ברירת מחדל ב-ViewModel
     * לא יחזירו את הקרוסלה בטעות לחגורה כתומה.
     */
    val initialBelt: Belt = remember(
        belts,
        kmiPrefs,
        userSp
    ) {
        val regId =
            kmiPrefs.getStringCompat("current_belt")
                ?: kmiPrefs.getStringCompat("belt_current")
                ?: userSp.getString("current_belt", null)
                ?: userSp.getString("belt_current", null)
                ?: userSp.getString("currentBelt", null)
                ?: userSp.getString("belt", null)

        val cleanRegId = regId?.trim().orEmpty()

        val registeredBelt =
            cleanRegId
                .takeIf { it.isNotBlank() }
                ?.let { Belt.fromAny(it) }

        val nextBelt =
            when (registeredBelt) {
                // אין למשתמש חגורה בכלל -> כתומה
                null ->
                    Belt.ORANGE

                // חגורה שחורה, כולל דרגות הדאן שממופות לשחורה,
                // תמיד מתחילה בחגורה שחורה
                Belt.BLACK ->
                    Belt.BLACK

                // לבנה אינה מוצגת בקרוסלה,
                // ולכן החגורה הבאה שלה היא צהובה
                Belt.WHITE ->
                    Belt.YELLOW

                else -> {
                    val registeredIndex =
                        belts.indexOf(registeredBelt)

                    if (
                        registeredIndex >= 0 &&
                        registeredIndex < belts.lastIndex
                    ) {
                        belts[registeredIndex + 1]
                    } else {
                        // המשתמש כבר בחגורה האחרונה
                        registeredBelt
                    }
                }
            }

        nextBelt
            .takeIf { it in belts }
            ?: Belt.ORANGE
    }

    /*
     * המפתח initialBelt.id גורם לביטול מצב שמור ישן
     * כאשר מגיעה פקודה קולית עם חגורה אחרת.
     */
    var currentIndex by rememberSaveable(initialBelt.id) {
        mutableIntStateOf(
            belts.indexOf(initialBelt)
                .takeIf { it >= 0 }
                ?: belts.indexOf(Belt.ORANGE)
                    .coerceAtLeast(0)
        )
    }

    currentIndex = currentIndex.coerceIn(
        minimumValue = 0,
        maximumValue = belts.lastIndex
    )

    val currentBelt = remember(
        currentIndex,
        belts,
        initialBelt
    ) {
        belts.getOrNull(currentIndex) ?: initialBelt
    }

    LaunchedEffect(currentBelt) {
        if (vm.selectedBelt.value != currentBelt) {
            vm.setSelectedBelt(currentBelt)
        }
    }

    val backgroundBrush =
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.background
            )
        )

    if (showPracticeMenu) {
        PracticeMenuDialog(
            canUseExtras = true,
            defaultBelt = currentBelt,
            onDismiss = { showPracticeMenu = false },
            onRandomPractice = { beltArg ->
                showPracticeMenu = false
                onOpenRandomPractice(beltArg)
            },
            onFinalExam = { beltArg ->
                showPracticeMenu = false
                onOpenFinalExam(beltArg)
            },
            onPracticeByTopics = { selection ->
                showPracticeMenu = false
                onPracticeByTopics(selection)
            },
            onPracticeByTopicSelected = { beltArg, topicArg ->
                showPracticeMenu = false
                onOpenRandomPracticeByTopic(beltArg, topicArg)
            }
        )
    }

    Scaffold(
        topBar = {
            val contextLang = LocalContext.current
            val topBarLanguageManager = remember {
                AppLanguageManager(contextLang)
            }

            KmiTopBar(
                title = beltTitleForUi(
                    belt = currentBelt,
                    lang = topBarLanguageManager.getCurrentLanguage()
                ),
                onHome = onBackHome,
                lockSearch = false,
                showBottomActions = true,
                centerTitle = true,
                showTopHome = false,
                showBackNavigation = false,
                showTopBeltIcon = true,
                topBeltIconRes = null,
                currentLang =
                    if (
                        topBarLanguageManager.getCurrentLanguage() ==
                        AppLanguage.ENGLISH
                    ) {
                        "en"
                    } else {
                        "he"
                    },
                onShare = {
                    clickSound()
                    haptic(true)

                    shareBeltTopicsPdf(
                        context = contextLang,
                        belt = currentBelt,
                        lang = topBarLanguageManager.getCurrentLanguage()
                    )
                },
                onToggleLanguage = {
                    val newLang =
                        if (
                            topBarLanguageManager.getCurrentLanguage() ==
                            AppLanguage.HEBREW
                        ) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    topBarLanguageManager.setLanguage(newLang)
                    (contextLang as? Activity)?.recreate()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundBrush)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                BeltQuestionsModeSwitcher(
                    onOpenByBelt = {},
                    onOpenByTopic = {
                        clickSound()
                        haptic(true)
                        onOpenByTopic()
                    }
                )

                Spacer(Modifier.height(4.dp))

                TopicsCardForBelt(
                    belt = currentBelt,
                    lang = langManager.getCurrentLanguage(),
                    accessMode = accessMode,
                    onOpenSubscription = onOpenSubscription,
                    onOpenTopic = onOpenTopic,
                    onOpenSubTopic = onOpenSubTopic,
                    onOpenDefenseMenu = onOpenDefenseMenu,
                    haptic = haptic,
                    clickSound = clickSound
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .offset(y = 34.dp)
            ) {
                BeltArcPicker(
                    belts = belts,
                    currentIndex = currentIndex,
                    onIndexChange = { selectedIndex ->
                        currentIndex = selectedIndex
                    },
                    onCenterTap = onNext,
                    haptic = haptic,
                    clickSound = clickSound,
                    inputEnabled = true,
                    reverseSwipeDirection = true
                )
            }

            FloatingQuickMenu(
                belt = currentBelt,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(999f),
                expanded = quickMenuExpanded,
                onExpandedChange = { expanded ->
                    quickMenuExpanded = expanded
                },
                triggerMode = QuickMenuTriggerMode.SideRail,
                includePractice = true,
                hasFullAccess = hasUnlockedAccess,
                onLockedItemClick = {
                    clickSound()
                    haptic(true)
                    onOpenSubscription()
                },
                onWeakPoints = {
                    clickSound()
                    haptic(true)
                    onOpenWeakPoints(currentBelt)
                },
                onAllLists = {
                    clickSound()
                    haptic(true)
                    onOpenAllLists(currentBelt)
                },
                onPractice = {
                    clickSound()
                    haptic(true)
                    showPracticeMenu = true
                },
                onSummary = {
                    clickSound()
                    haptic(true)
                    onOpenSummaryScreen(currentBelt)
                },
                onVoice = {
                    clickSound()
                    haptic(true)
                    onOpenVoiceAssistant(currentBelt)
                },
                onPdf = {
                    clickSound()
                    haptic(true)
                    onOpenPdfMaterials(currentBelt)
                }
            )
        }
    }
}

private enum class BeltQuestionsDisplayMode {
    BY_BELT,
    BY_TOPIC
}

@Composable
private fun BeltQuestionsModeSwitcher(
    onOpenByBelt: () -> Unit,
    onOpenByTopic: () -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember(context) {
        AppLanguageManager(context)
    }

    val isEnglish =
        languageManager.getCurrentLanguage() ==
                AppLanguage.ENGLISH

    val tabs = remember(isEnglish) {
        if (isEnglish) {
            listOf(
                BeltQuestionsDisplayMode.BY_TOPIC to "By Topic",
                BeltQuestionsDisplayMode.BY_BELT to "By Belt"
            )
        } else {
            listOf(
                BeltQuestionsDisplayMode.BY_TOPIC to "לפי נושא",
                BeltQuestionsDisplayMode.BY_BELT to "לפי חגורה"
            )
        }
    }

    val selectedIndex = tabs
        .indexOfFirst { (mode, _) ->
            mode == BeltQuestionsDisplayMode.BY_BELT
        }
        .coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(bottom = 6.dp),
        color = Color(0xFF062B4A).copy(alpha = 0.78f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.34f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-4).dp)
                    .width(1.dp)
                    .height(24.dp)
                    .background(
                        Color.White.copy(alpha = 0.65f)
                    )
            )

            CompositionLocalProvider(
                LocalLayoutDirection provides
                        LayoutDirection.Ltr
            ) {
                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                positions[selectedIndex]
                            ),
                            height = 3.dp,
                            color = Color.White
                        )
                    },
                    modifier = Modifier.matchParentSize()
                ) {
                    tabs.forEach { (mode, label) ->
                        Tab(
                            selected = mode == BeltQuestionsDisplayMode.BY_BELT,
                            onClick = {
                                when (mode) {
                                    BeltQuestionsDisplayMode.BY_BELT ->
                                        onOpenByBelt()

                                    BeltQuestionsDisplayMode.BY_TOPIC ->
                                        onOpenByTopic()
                                }
                            },
                            text = {
                                Text(
                                    text = label,
                                    style = KmiTypography.action,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor =
                                Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        }
    }
}

/* ----------------------------- כרטיס “נושאים בחגורה” ---------------------------- */

@Composable
private fun PremiumPulsingLockBadge(
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "topicLockPulse")

    val scale by pulse.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "topicLockScale"
    )

    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        tint = Color(0xFFF59E0B),
        modifier = modifier
            .size(
                20.dp * LocalAppIconScale.current
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 1f
            }
    )
}

@Composable
private fun TopicsCardForBelt(
    belt: Belt,
    lang: AppLanguage,
    accessMode: AccessMode,
    onOpenSubscription: () -> Unit,
    onOpenTopic: (Belt, String) -> Unit,
    onOpenSubTopic: (Belt, String, String) -> Unit,
    onOpenDefenseMenu: (Belt, String) -> Unit,
    haptic: (Boolean) -> Unit,
    clickSound: () -> Unit
) {
    val isEnglish = lang == AppLanguage.ENGLISH
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // ✅ חשוב:
    // באנגלית משתמשים ב-Left פיזי ולא ב-Start,
    // כדי שלא יתהפך אם המסך עדיין מקבל RTL.
    val titleTextAlignByLang = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalByLang = if (isEnglish) Alignment.Start else Alignment.End
    val layoutByLang =
        if (isEnglish) LayoutDirection.Ltr
        else LayoutDirection.Rtl

    val cardBg =
        if (isDarkTheme) {
            Color(0xFF101827)
        } else {
            Color.White
        }

    val titleColor =
        if (isDarkTheme) {
            Color(0xFFF8FAFC)
        } else {
            Color(0xFF263238)
        }

    val rowTitleColor =
        if (isDarkTheme) {
            Color(0xFFF8FAFC)
        } else {
            Color(0xFF1F2937)
        }

    /*
     * צבע החגורה הלבנה נשאר לבן על הכרטיס הכהה.
     * במצב בהיר משתמשים באפור־כחלחל כדי שלא ייעלם
     * על הרקע הלבן.
     */
    /*
   * בחגורה שחורה, על גבי כרטיס כהה, משתמשים בלבן
   * עבור ספירות, חצים, מסגרות ופעולות.
   *
   * בחגורה לבנה במצב בהיר משתמשים באפור־כחלחל,
   * כדי שהצבע לא ייעלם על הרקע הבהיר.
   */
    val readableBeltAccent =
        when {
            belt == Belt.BLACK && isDarkTheme ->
                Color.White

            belt == Belt.WHITE && isDarkTheme ->
                Color.White

            belt == Belt.WHITE ->
                Color(0xFF64748B)

            belt == Belt.YELLOW && !isDarkTheme ->
                Color(0xFFC98A00)

            else ->
                belt.color
        }

    val rowSubColor =
        readableBeltAccent.copy(alpha = 0.88f)

    // ✅ כרטיס תתי־הנושאים מקבל גוון קריא של החגורה
    val subTopicsCardBg =
        if (isDarkTheme) {
            readableBeltAccent.copy(alpha = 0.12f)
        } else {
            readableBeltAccent.copy(alpha = 0.10f)
        }

    val subTopicsCardBorder =
        if (isDarkTheme) {
            readableBeltAccent.copy(alpha = 0.34f)
        } else {
            readableBeltAccent.copy(alpha = 0.38f)
        }

    val subDividerColor =
        if (isDarkTheme) {
            readableBeltAccent.copy(alpha = 0.28f)
        } else {
            readableBeltAccent.copy(alpha = 0.36f)
        }

    val rawTopicTitles: List<String> = remember(belt) {
        TopicsEngine.topicTitlesFor(belt)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    val detailsByTitle: Map<String, TopicDetails> = remember(belt, rawTopicTitles) {
        rawTopicTitles.associateWith { title -> topicDetailsFor(belt, title) }
    }

    val exerciseCountByTitle = remember(belt, rawTopicTitles) {
        rawTopicTitles.associateWith { title ->
            topicExercisesCountForCurrentBelt(
                belt = belt,
                topicTitle = title
            )
        }
    }

    // ✅ נושאים שיש להם תתי־נושאים תמיד מופיעים למעלה.
    // ✅ בחגורה צהובה נותנים סדר מיוחד:
    // עבודת ידיים -> הגנות -> שחרורים.
    // ✅ בחגורה חומה "שחרורים" נשאר נושא רגיל,
    // כי שם יש רק תרגיל אחד והוא לא אמור להיות נעול/תת־נושאים.
    val topicTitles: List<String> = remember(belt, rawTopicTitles, detailsByTitle) {

        fun hasRealSubTopics(title: String): Boolean {
            val topicTrim = title.trim()

            return detailsByTitle[title]
                ?.subTitles
                .orEmpty()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { it != topicTrim }
                .distinct()
                .any()
        }

        fun priorityRank(title: String): Int {
            val clean = title.trim()

            return when {
                // ✅ בכל החגורות:
                // הגנות תמיד ראשון
                clean.contains("הגנות") -> 0

                // ✅ שחרורים תמיד שני
                clean.contains("שחרורים") -> 1

                // ✅ בצהובה עבודת ידיים מיד אחרי הגנות ושחרורים
                belt == Belt.YELLOW &&
                        clean.contains("עבודת ידיים") -> 2

                // ✅ שאר הנושאים עם תתי־נושאים
                hasRealSubTopics(title) -> 3

                // ✅ שאר הנושאים הרגילים
                else -> 10
            }
        }

        val sorted = rawTopicTitles
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<String>> { priorityRank(it.value) }
                    .thenBy { it.index }
            )
            .map { it.value }

        sorted
    }

    var expandedTopic by rememberSaveable(belt.id) {
        mutableStateOf<String?>(null)
    }

    var generalNoteTitle by rememberSaveable(belt.id) {
        mutableStateOf<String?>(null)
    }

    var generalNoteText by rememberSaveable(belt.id) {
        mutableStateOf<String?>(null)
    }

    val rowMinHeight = 54.dp

    // ✅ מגדיל את גובה כרטיסיית הנושאים,
    // כדי שתרד נמוך יותר לכיוון העיגול המרכזי.
    val visibleRows = 6
    val listHeight = rowMinHeight * visibleRows + 10.dp

    val fabSize = 120.dp

    // ✅ במקום להשאיר חצי עיגול רווח,
    // משאירים רק בערך שליש גובה כדי שהכרטיס יירד יותר למטה.
    val fabClearance = fabSize * 0.34f

    val visibleGeneralNote = generalNoteText
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    if (visibleGeneralNote != null) {
        val noteAccent = Color(0xFF2563EB)

        val noteCardBackground =
            if (isDarkTheme) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF172033),
                        Color(0xFF101827),
                        Color(0xFF0D1422)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFDFEFF),
                        Color(0xFFF5F8FF),
                        Color(0xFFFFFFFF)
                    )
                )
            }

        val noteBorderColor =
            if (isDarkTheme) {
                Color(0xFF60A5FA).copy(alpha = 0.34f)
            } else {
                noteAccent.copy(alpha = 0.20f)
            }

        val noteTitleColor =
            if (isDarkTheme) {
                Color(0xFFF8FAFC)
            } else {
                Color(0xFF182235)
            }

        val noteBodyColor =
            if (isDarkTheme) {
                Color(0xFFD5DEEB)
            } else {
                Color(0xFF334155)
            }

        Dialog(
            onDismissRequest = {
                generalNoteTitle = null
                generalNoteText = null
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            CompositionLocalProvider(
                LocalLayoutDirection provides
                        if (isEnglish) {
                            LayoutDirection.Ltr
                        } else {
                            LayoutDirection.Rtl
                        }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .widthIn(max = 430.dp)
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        color = noteBorderColor
                    ),
                    shadowElevation =
                        if (isDarkTheme) {
                            14.dp
                        } else {
                            24.dp
                        },
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(noteCardBackground)
                    ) {
                        /*
                         * פס כחול עליון ועדין שמעניק לכרטיס
                         * זהות ברורה של הערת מידע.
                         */
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(0.32f)
                                .height(4.dp)
                                .clip(
                                    RoundedCornerShape(
                                        bottomStart = 999.dp,
                                        bottomEnd = 999.dp
                                    )
                                )
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF60A5FA),
                                            Color(0xFF2563EB),
                                            Color(0xFF7C3AED)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 22.dp,
                                    end = 22.dp,
                                    top = 14.dp,
                                    bottom = 12.dp
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            /*
                             * אייקון מובחן בצבע כחול.
                             * אינו דומה למנעול הכתום של תוכן נעול.
                             */
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color =
                                    if (isDarkTheme) {
                                        noteAccent.copy(alpha = 0.22f)
                                    } else {
                                        Color(0xFFE8F1FF)
                                    },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = noteAccent.copy(alpha = 0.28f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint =
                                            if (isDarkTheme) {
                                                Color(0xFF60A5FA)
                                            } else {
                                                noteAccent
                                            },
                                        modifier =
                                            Modifier.size(
                                                19.dp * LocalAppIconScale.current
                                            )
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text =
                                    if (isEnglish) {
                                        "PROFESSIONAL GUIDANCE"
                                    } else {
                                        "דגשים מקצועיים"
                                    },
                                style = KmiTypography.caption.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color =
                                    if (isDarkTheme) {
                                        Color(0xFF93C5FD)
                                    } else {
                                        noteAccent
                                    },
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = generalNoteTitle.orEmpty(),
                                style = KmiTypography.sectionTitle.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = noteTitleColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color =
                                    if (isDarkTheme) {
                                        Color.White.copy(alpha = 0.10f)
                                    } else {
                                        noteAccent.copy(alpha = 0.12f)
                                    }
                            )

                            Spacer(Modifier.height(8.dp))

                            /*
                             * אזור נגלל: גם הערה ארוכה לא תחרוג
                             * מגובה המסך ולא תסתיר את כפתור הסגירה.
                             */
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isDarkTheme) {
                                            Color.White.copy(alpha = 0.035f)
                                        } else {
                                            Color.White.copy(alpha = 0.72f)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color =
                                            if (isDarkTheme) {
                                                Color.White.copy(alpha = 0.07f)
                                            } else {
                                                noteAccent.copy(alpha = 0.08f)
                                            },
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .verticalScroll(
                                        rememberScrollState()
                                    )
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 14.dp
                                    )
                            ) {
                                Text(
                                    text = visibleGeneralNote,
                                    /*
                                     * secondary קטן יותר מ־body,
                                     * ועדיין מותאם להגדרת גודל הכתב הגלובלית.
                                     */
                                    style = KmiTypography.secondary.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = noteBodyColor,
                                    textAlign =
                                        if (isEnglish) {
                                            TextAlign.Left
                                        } else {
                                            TextAlign.Right
                                        },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        clickSound()
                                        generalNoteTitle = null
                                        generalNoteText = null
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = noteAccent,
                                tonalElevation = 0.dp,
                                shadowElevation =
                                    if (isDarkTheme) {
                                        0.dp
                                    } else {
                                        5.dp
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 18.dp,
                                            vertical = 12.dp
                                        ),
                                    horizontalArrangement =
                                        Arrangement.Center,
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Text(
                                        text =
                                            if (isEnglish) {
                                                "Close"
                                            } else {
                                                "סגור"
                                            },
                                        style = KmiTypography.action.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White
                                    )

                                    Spacer(Modifier.width(7.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = BorderStroke(
            1.dp,
            if (isDarkTheme) Color.White.copy(alpha = 0.12f) else belt.color.copy(alpha = 0.14f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .padding(top = 0.dp)
            .padding(bottom = fabClearance + 2.dp)
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Text(
                text =
                    if (isEnglish) {
                        "Topics in Belt"
                    } else {
                        "נושאים בחגורה"
                    },
                style = KmiTypography.sectionTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                textAlign = TextAlign.Center,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            if (topicTitles.isEmpty()) {
                Text(
                    text =
                        if (isEnglish) {
                            "No topics to display"
                        } else {
                            "אין נושאים להצגה"
                        },
                    style = KmiTypography.body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center,
                    color = rowSubColor
                )
            } else {
                val topicsScroll = rememberScrollState(0)

                // ✅ חשוב:
                // כשעוברים חגורה, ScrollState עלול להישאר באמצע הרשימה מהחגורה הקודמת.
                // לכן מאפסים לראש הרשימה, כדי שבאמת נראה את הנושאים עם תתי־נושאים למעלה.
                LaunchedEffect(belt.id, topicTitles) {
                    topicsScroll.scrollTo(0)
                }

                Column(
                    modifier = Modifier
                        .height(listHeight)
                        .verticalScroll(topicsScroll)
                ) {
                    topicTitles.forEachIndexed { index, title ->

                        val details = detailsByTitle[title]
                            ?: TopicDetails(itemCount = 0, subTitles = emptyList())

                        val displayTitle = topicTitleForUi(title, lang)

                        val topicGeneralNote = remember(
                            belt,
                            title
                        ) {
                            SharedContentRepo.getTopicGeneralNote(
                                belt = belt,
                                topicTitle = title
                            )
                        }

                        val subTitles: List<String> = details.subTitles
                            .asSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .filter { it != title.trim() }
                            .distinct()
                            .toList()

                        val itemCount = exerciseCountByTitle[title]
                            ?: topicExercisesCountForCurrentBelt(
                                belt = belt,
                                topicTitle = title
                            )

                        val subCount = subTitles.size
                        val hasSubs = subCount > 0

                        val countsLine = if (isEnglish) {
                            if (subCount > 0) {
                                "$subCount sub-topics · $itemCount exercises"
                            } else {
                                "$itemCount exercises"
                            }
                        } else {
                            if (subCount > 0) {
                                "\u200F$subCount\u00A0תתי נושאים · $itemCount\u00A0תרגילים\u200F"
                            } else {
                                "$itemCount תרגילים"
                            }
                        }

                        val isExpanded = expandedTopic == title
                        val isDefenseTopic =
                            title.trim().contains("הגנות")

                        val floatingAccent =
                            Brush.verticalGradient(
                                colors = listOf(
                                    readableBeltAccent,
                                    readableBeltAccent.copy(
                                        alpha =
                                            if (isDarkTheme) {
                                                0.90f
                                            } else {
                                                0.82f
                                            }
                                    )
                                )
                            )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = rowMinHeight)
                                .padding(horizontal = 10.dp, vertical = 1.dp)
                                .clickable {
                                    clickSound()
                                    haptic(true)

                                    val canOpen =
                                        LockedContentPolicy.canOpenTopic(accessMode, title)

                                    if (!canOpen) {
                                        onOpenSubscription()
                                    } else if (hasSubs) {
                                        expandedTopic = if (isExpanded) null else title
                                    } else {
                                        if (isDefenseTopic) {
                                            onOpenDefenseMenu(belt, title)
                                        } else {
                                            onOpenTopic(belt, title)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                horizontalAlignment = horizontalByLang
                            ) {
                                val parentLocked =
                                    LockedContentPolicy.shouldShowLock(accessMode, title)

                                val topicImageRes = beltTopicImageFor(belt, title)

                                CompositionLocalProvider(
                                    LocalLayoutDirection provides layoutByLang
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(floatingAccent)
                                        )

                                        Spacer(Modifier.width(5.dp))

                                        if (topicImageRes != null) {
                                            Box(
                                                modifier = Modifier
                                                    .width(38.dp)
                                                    .height(31.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            ) {
                                                Image(
                                                    painter = painterResource(id = topicImageRes),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Spacer(Modifier.width(6.dp))
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = horizontalByLang
                                        ) {
                                            Text(
                                                text = displayTitle,
                                                style = KmiTypography.cardTitle,
                                                color = rowTitleColor,
                                                textAlign = titleTextAlignByLang,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(Modifier.height(1.dp))

                                            Text(
                                                text = countsLine,
                                                style = KmiTypography.caption.copy(
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = rowSubColor,
                                                textAlign = titleTextAlignByLang,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        /*
                                         * שומרים אזור פעולות קבוע רק כאשר
                                         * באמת קיימת לפחות פעולה אחת.
                                         *
                                         * נושא רגיל ללא הערה, מנעול או
                                         * תתי־נושאים מקבל את מלוא רוחב השורה.
                                         */
                                        val hasTopicGeneralNote =
                                            !topicGeneralNote.isNullOrBlank()

                                        val showTopicActions =
                                            hasTopicGeneralNote ||
                                                    parentLocked ||
                                                    hasSubs

                                        if (showTopicActions) {
                                            Spacer(Modifier.width(4.dp))

                                            Box(
                                                modifier =
                                                    Modifier.size(30.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                                if (hasTopicGeneralNote) {
                                                Surface(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clickable {
                                                            clickSound()
                                                            haptic(false)

                                                            generalNoteTitle =
                                                                if (isEnglish) {
                                                                    "General note: $displayTitle"
                                                                } else {
                                                                    "הערה כללית: $displayTitle"
                                                                }

                                                            generalNoteText =
                                                                topicGeneralNote
                                                        },
                                                    shape = CircleShape,
                                                    color =
                                                        if (isDarkTheme) {
                                                            Color(0xFF2563EB)
                                                                .copy(alpha = 0.24f)
                                                        } else {
                                                            Color(0xFFE8F1FF)
                                                        },
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color =
                                                            Color(0xFF2563EB)
                                                                .copy(alpha = 0.45f)
                                                    ),
                                                    tonalElevation = 0.dp,
                                                    shadowElevation = 0.dp
                                                ) {
                                                    Box(
                                                        contentAlignment =
                                                            Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector =
                                                                Icons.Filled.Info,
                                                            contentDescription =
                                                                if (isEnglish) {
                                                                    "General note"
                                                                } else {
                                                                    "הערה כללית"
                                                                },
                                                            tint =
                                                                if (isDarkTheme) {
                                                                    Color(0xFF60A5FA)
                                                                } else {
                                                                    Color(0xFF2563EB)
                                                                },
                                                            modifier =
                                                                Modifier.size(
                                                                    17.dp * LocalAppIconScale.current
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.width(4.dp))

                                        Box(
                                            modifier = Modifier.size(20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (parentLocked) {
                                                PremiumPulsingLockBadge(
                                                    modifier =
                                                        Modifier.size(
                                                            16.dp * LocalAppIconScale.current
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(4.dp))

                                            Box(
                                                modifier =
                                                    Modifier.size(20.dp),
                                                contentAlignment =
                                                    Alignment.Center
                                            ) {
                                                if (hasSubs) {
                                                    Icon(
                                                        imageVector =
                                                            if (isExpanded) {
                                                                Icons.Filled
                                                                    .KeyboardArrowUp
                                                            } else {
                                                                Icons.Filled
                                                                    .KeyboardArrowDown
                                                            },
                                                        contentDescription =
                                                            null,
                                                        tint =
                                                            readableBeltAccent,
                                                        modifier =
                                                            Modifier.size(
                                                                20.dp * LocalAppIconScale.current
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (hasSubs && isExpanded) {
                                    Spacer(Modifier.height(4.dp))

                                    val parentLockedForSubTopics =
                                        LockedContentPolicy.shouldShowLock(accessMode, title)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 0.dp,
                                                end = 0.dp,
                                                top = 2.dp,
                                                bottom = 6.dp
                                            ),
                                        shape = RoundedCornerShape(18.dp),
                                        color = subTopicsCardBg,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = subTopicsCardBorder
                                        ),
                                        shadowElevation = 0.dp,
                                        tonalElevation = 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(0.dp),
                                            horizontalAlignment = horizontalByLang
                                        ) {
                                            subTitles.forEachIndexed { subIndex, sub ->
                                                val displaySub =
                                                    topicTitleForUi(sub, lang)

                                                val subTopicGeneralNote =
                                                    remember(
                                                        belt,
                                                        title,
                                                        sub
                                                    ) {
                                                        SharedContentRepo
                                                            .getSubTopicGeneralNote(
                                                                belt = belt,
                                                                topicTitle = title,
                                                                subTopicTitle = sub
                                                            )
                                                    }

                                                val subTopicStatsLine =
                                                    remember(belt, title, sub, lang) {
                                                        val matchedSubTopic: SharedContentRepo.SubTopic? =
                                                            SharedContentRepo.getSubTopicsFor(
                                                                belt = belt,
                                                                topicTitle = title
                                                            )
                                                                .firstOrNull { candidate ->
                                                                    candidate.title.trim() == sub.trim()
                                                                }

                                                        matchedSubTopic
                                                            ?.let { resolvedSubTopic: SharedContentRepo.SubTopic ->
                                                                subTopicStatsLineForUi(
                                                                    subTopic = resolvedSubTopic,
                                                                    lang = lang
                                                                )
                                                            }
                                                            .orEmpty()
                                                    }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            clickSound()
                                                            haptic(true)

                                                            val canOpenSubTopic =
                                                                accessMode == AccessMode.OPEN ||
                                                                        LockedContentPolicy.canOpenTopic(
                                                                            accessMode,
                                                                            title
                                                                        )

                                                            if (!canOpenSubTopic) {
                                                                onOpenSubscription()
                                                            } else {
                                                                onOpenSubTopic(belt, title, sub)
                                                            }
                                                        }
                                                        .padding(
                                                            horizontal = 8.dp,
                                                            vertical = 5.dp
                                                        )
                                                        .heightIn(min = 48.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector =
                                                            Icons.Filled.ChevronLeft,
                                                        contentDescription = null,
                                                        tint = readableBeltAccent,
                                                        modifier =
                                                            Modifier.size(
                                                                15.dp * LocalAppIconScale.current
                                                            )
                                                    )

                                                    Spacer(Modifier.width(6.dp))

                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalAlignment = horizontalByLang
                                                    ) {
                                                        Text(
                                                            text = displaySub,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = titleTextAlignByLang,
                                                            color = rowTitleColor,
                                                            style = KmiTypography.cardTitle,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        if (subTopicStatsLine.isNotBlank()) {
                                                            Spacer(Modifier.height(1.dp))

                                                            Text(
                                                                text = subTopicStatsLine,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = titleTextAlignByLang,
                                                                color = readableBeltAccent,
                                                                style =
                                                                    KmiTypography.caption.copy(
                                                                        fontWeight =
                                                                            FontWeight.Bold
                                                                    ),
                                                                maxLines = 2,
                                                                overflow =
                                                                    TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    if (
                                                        !subTopicGeneralNote
                                                            .isNullOrBlank()
                                                    ) {
                                                        Spacer(
                                                            Modifier.width(4.dp)
                                                        )

                                                        Box(
                                                            modifier =
                                                                Modifier.size(30.dp),
                                                            contentAlignment =
                                                                Alignment.Center
                                                        ) {
                                                            Surface(
                                                                modifier = Modifier
                                                                    .size(26.dp)
                                                                    .clickable {
                                                                        clickSound()
                                                                        haptic(false)

                                                                        generalNoteTitle =
                                                                            if (isEnglish) {
                                                                                "General note: $displaySub"
                                                                            } else {
                                                                                "הערה כללית: $displaySub"
                                                                            }

                                                                        generalNoteText =
                                                                            subTopicGeneralNote
                                                                    },
                                                                shape = CircleShape,
                                                                color =
                                                                    if (isDarkTheme) {
                                                                        Color(0xFF2563EB)
                                                                            .copy(
                                                                                alpha =
                                                                                    0.24f
                                                                            )
                                                                    } else {
                                                                        Color(0xFFE8F1FF)
                                                                    },
                                                                border =
                                                                    BorderStroke(
                                                                        width = 1.dp,
                                                                        color =
                                                                            Color(
                                                                                0xFF2563EB
                                                                            )
                                                                                .copy(
                                                                                    alpha =
                                                                                        0.45f
                                                                                )
                                                                    ),
                                                                tonalElevation = 0.dp,
                                                                shadowElevation = 0.dp
                                                            ) {
                                                                Box(
                                                                    contentAlignment =
                                                                        Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector =
                                                                            Icons.Filled.Info,
                                                                        contentDescription =
                                                                            if (isEnglish) {
                                                                                "General note"
                                                                            } else {
                                                                                "הערה כללית"
                                                                            },
                                                                        tint =
                                                                            if (
                                                                                isDarkTheme
                                                                            ) {
                                                                                Color(
                                                                                    0xFF60A5FA
                                                                                )
                                                                            } else {
                                                                                Color(
                                                                                    0xFF2563EB
                                                                                )
                                                                            },
                                                                        modifier =
                                                                            Modifier.size(
                                                                                17.dp
                                                                            )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (parentLockedForSubTopics) {
                                                        Spacer(
                                                            Modifier.width(8.dp)
                                                        )

                                                        PremiumPulsingLockBadge(
                                                            modifier =
                                                                Modifier.size(
                                                                    16.dp * LocalAppIconScale.current
                                                                )
                                                        )
                                                    }
                                                }

                                                if (subIndex != subTitles.lastIndex) {
                                                    HorizontalDivider(
                                                        color = subDividerColor,
                                                        thickness = 0.8.dp,
                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "Close topic"
                                                    } else {
                                                        "סגור נושא"
                                                    },
                                                modifier = Modifier
                                                    .align(horizontalByLang)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        clickSound()
                                                        haptic(true)
                                                        expandedTopic = null
                                                    }
                                                    .padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                color =
                                                    readableBeltAccent,
                                                style = KmiTypography.action
                                            )

                                            Spacer(Modifier.height(2.dp))

                                            Text(
                                                text =
                                                    if (isEnglish) {
                                                        "Open full topic"
                                                    } else {
                                                        "פתח את כל הנושא"
                                                    },
                                                modifier = Modifier
                                                    .align(horizontalByLang)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        clickSound()
                                                        haptic(true)

                                                        if (isDefenseTopic) {
                                                            onOpenDefenseMenu(
                                                                belt,
                                                                title
                                                            )
                                                        } else {
                                                            onOpenTopic(
                                                                belt,
                                                                title
                                                            )
                                                        }
                                                    }
                                                    .padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                color =
                                                    belt.color.copy(alpha = 1f),
                                                style = KmiTypography.action
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (index != topicTitles.lastIndex) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = if (isDarkTheme) {
                                    Color.White.copy(alpha = 0.12f)
                                } else {
                                    Color(0x22000000)
                                },
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                    }

                    // ✅ רווח גלילה תחתון:
                    // מונע מצב שהסרגל הצף / הקרוסלה מסתירים את הנושא האחרון.
                    Spacer(Modifier.height(56.dp))
                }
            }
        }
    }
}

/* ------------------------------- קרוסלת חגורות ------------------------------- */

@Composable
@Suppress("UNUSED_PARAMETER")
private fun BeltArcPicker(
    belts: List<Belt>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onCenterProgress: (Float) -> Unit = {},
    haptic: (Boolean) -> Unit,
    clickSound: () -> Unit,
    inputEnabled: Boolean = true,
    reverseSwipeDirection: Boolean = false
) {
    val big = 120.dp
    val small = 68.dp
    val step = small + 44.dp
    val arcDepth = 84.dp
    val pickerHeight = small + arcDepth

    val density = LocalDensity.current
    val stepPx = with(density) { step.toPx() }

    /*
     * אובייקט האנימציה נשמר לאורך המעבר בין החגורות.
     * currentIndex מסנכרן אותו בלי ליצור אותו מחדש.
     */
    val center = remember(belts) {
        Animatable(
            currentIndex
                .coerceIn(0, belts.lastIndex)
                .toFloat()
        )
    }

    LaunchedEffect(currentIndex, belts) {
        center.snapTo(
            currentIndex
                .coerceIn(0, belts.lastIndex)
                .toFloat()
        )
    }

    LaunchedEffect(center.value) {
        onCenterProgress(center.value)
    }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pickerHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        belts.forEachIndexed { index, belt ->
            val rel = index - center.value
            val dist = abs(rel)
            val hide = dist > 2.6f
            val t = min(1f, dist / 2f)

            val drop = arcDepth * (1f - cos(t * (PI / 2)).toFloat())
            val grow = (1f - min(1f, dist)).coerceIn(0f, 1f)
            val targetSize = small + (big - small) * grow
            val size by animateDpAsState(targetValue = targetSize, label = "belt-size")

            val targetAlpha = if (hide) 0f else 0.75f + 0.25f * (1f - t)
            val alpha by animateFloatAsState(targetValue = targetAlpha, label = "belt-alpha")

            val xTarget = step * rel
            val xDp by animateDpAsState(targetValue = xTarget, label = "belt-x")

            val circleColor = belt.color.copy(alpha = 0.96f)

            val boostFactor = min(1f, dist)
            val yDrop = drop + small * boostFactor

            val isCenter = dist < 0.25f

            val base = Modifier
                // ✅ חשוב:
                // absoluteOffset לא מושפע מ־RTL/LTR.
                // כך עיגול שנמצא פיזית מימין יישאר באמת מימין,
                // ועיגול שנמצא פיזית משמאל יישאר באמת משמאל.
                .absoluteOffset(x = xDp, y = yDrop)
                .size(size)
                .alpha(alpha)
                .zIndex(if (isCenter) 2f else 1f)

            val gestures =
                if (!inputEnabled) {
                    Modifier
                } else {
                    Modifier
                        .pointerInput(
                            belts,
                            index,
                            currentIndex,
                            reverseSwipeDirection
                        ) {
                            var dragUpdateJob: Job? = null

                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    /*
                                     * מבטל עדכון ישן שטרם הסתיים,
                                     * כדי שלא יחזיר את הקרוסלה אחורה.
                                     */
                                    dragUpdateJob?.cancel()
                                    dragUpdateJob = null

                                    val selectedIndex =
                                        center.value
                                            .roundToInt()
                                            .coerceIn(
                                                0,
                                                belts.lastIndex
                                            )

                                    val selectionChanged =
                                        selectedIndex != currentIndex

                                    scope.launch {
                                        /*
                                         * קודם מצמידים את העיגול למיקום
                                         * המדויק ורק לאחר מכן מעדכנים
                                         * את הכותרת ותוכן החגורה.
                                         */
                                        center.snapTo(
                                            selectedIndex.toFloat()
                                        )

                                        onIndexChange(selectedIndex)

                                        if (selectionChanged) {
                                            clickSound()
                                            haptic(true)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    /*
                                     * אם המחווה בוטלה, חוזרים לחגורה
                                     * שמוצגת בכותרת ולא נשארים בין חגורות.
                                     */
                                    dragUpdateJob?.cancel()
                                    dragUpdateJob = null

                                    scope.launch {
                                        center.snapTo(
                                            currentIndex
                                                .coerceIn(
                                                    0,
                                                    belts.lastIndex
                                                )
                                                .toFloat()
                                        )
                                    }
                                }
                            ) { _, dragAmount ->
                                val direction =
                                    if (reverseSwipeDirection) {
                                        -1f
                                    } else {
                                        1f
                                    }

                                val delta =
                                    (dragAmount / stepPx) *
                                            direction

                                val nextPosition =
                                    (center.value + delta)
                                        .coerceIn(
                                            0f,
                                            belts.lastIndex.toFloat()
                                        )

                                /*
                                 * בכל תנועה נשמרת רק הפעולה האחרונה.
                                 */
                                dragUpdateJob?.cancel()
                                dragUpdateJob = scope.launch {
                                    center.snapTo(nextPosition)
                                }
                            }
                        }
                }

            Box(
                modifier = base.then(gestures),
                contentAlignment = Alignment.Center
            ) {
                val forceWhiteOutline =
                    belt.heb.lowercase().let { it.contains("שחור") || it.contains("חום") }
                val outlineColor = when {
                    forceWhiteOutline -> Color.White
                    belt.color.luminance() < 0.5f -> Color.White
                    else -> Color.Black
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // ✅ רק לעיגול המרכזי: טבעת צבעונית מסתובבת מסביב
                    if (isCenter) {
                        RotatingOrbitRing(
                            modifier = Modifier.fillMaxSize(),
                            base = circleColor
                        )
                    }

                    // ✅ כדי שהטבעת לא “תכוסה” ע״י העיגול, מכניסים את העיגול פנימה קצת רק במרכז
                    val ringPad = if (isCenter) 8.dp else 0.dp

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(ringPad)
                                .clip(CircleShape)
                                .border(BorderStroke(3.dp, outlineColor), CircleShape)
                                .background(circleColor),
                            contentAlignment = Alignment.Center
                        ) {
                            val ctx = LocalContext.current
                            val lang = remember { AppLanguageManager(ctx) }.getCurrentLanguage()
                            if (isCenter) {
                                val clean = remember(belt, lang) {
                                    beltShortNameForUi(belt, lang)
                                }
                                Text(
                                    text =
                                        if (lang == AppLanguage.ENGLISH) {
                                            "Belt\n$clean"
                                        } else {
                                            "חגורה\n$clean"
                                        },
                                    style = KmiTypography.action,
                                    color =
                                        if (belt.color.luminance() < 0.5f) {
                                            Color.White
                                        } else {
                                            Color.Black
                                        },
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                }
            }
        }
    }
}


@Composable
private fun RotatingOrbitRing(
    modifier: Modifier = Modifier,
    base: Color
) {
    val ringStroke = 7.dp
    val gapStroke = 7.dp

    val inf = rememberInfiniteTransition(label = "ring")
    val angle by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1600,
                easing = LinearEasing
            )
        ),
        label = "ring-angle"
    )

    /*
     * הטבעת משתמשת רק בגוונים של צבע החגורה שבמרכז.
     * השינוי בעוצמת הגוונים מאפשר לראות את תנועת הסיבוב,
     * בלי להציג צבעים שאינם קשורים לחגורה.
     */
    val ringColors = remember(base) {
        val opaqueBase = base.copy(alpha = 1f)

        fun darker(factor: Float): Color {
            return Color(
                red = opaqueBase.red * factor,
                green = opaqueBase.green * factor,
                blue = opaqueBase.blue * factor,
                alpha = 1f
            )
        }

        when {
            /*
             * בצבעים כהים מאוד אי אפשר ליצור גוון כהה נוסף
             * שעדיין יהיה נראה, ולכן משתמשים באפור־שחור.
             */
            opaqueBase.luminance() < 0.05f -> {
                listOf(
                    Color(0xFF111827),
                    Color(0xFF374151),
                    Color(0xFF1F2937),
                    Color(0xFF4B5563),
                    Color(0xFF111827)
                )
            }

            /*
             * אם חגורה לבנה תוחזר בעתיד לקרוסלה,
             * הטבעת שלה תוצג בגוונים אפורים.
             */
            opaqueBase.luminance() > 0.88f -> {
                listOf(
                    Color(0xFF9CA3AF),
                    Color(0xFF6B7280),
                    Color(0xFF4B5563),
                    Color(0xFF6B7280),
                    Color(0xFF9CA3AF)
                )
            }

            else -> {
                listOf(
                    darker(0.78f),
                    darker(0.48f),
                    darker(0.68f),
                    darker(0.38f),
                    darker(0.78f)
                )
            }
        }
    }

    val sweep = Brush.sweepGradient(
        colors = ringColors
    )

    Canvas(modifier = modifier) {
        val strokePx = ringStroke.toPx()
        val inset = strokePx / 2f

        // שכבת בסיס עדינה מתחת לטבעת המסתובבת
        drawArc(
            color = ringColors.first().copy(alpha = 0.22f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(
                size.width - inset * 2,
                size.height - inset * 2
            ),
            style = Stroke(
                width = gapStroke.toPx()
            )
        )

        // טבעת מסתובבת בגוונים של צבע החגורה בלבד
        rotate(degrees = angle) {
            drawArc(
                brush = sweep,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(
                    size.width - inset * 2,
                    size.height - inset * 2
                ),
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}