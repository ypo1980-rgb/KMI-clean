package il.kmi.app.screens.coach

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.firebase.firestore.SetOptions
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import il.kmi.app.privacy.DemoPrivacy
import il.kmi.app.privacy.TraineeDisplayNameMapper
import il.kmi.app.screens.registration.CoachBranchAssignmentsCodec
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.KmiTypography

//======================================================================

private fun coachBroadcastTr(isEnglish: Boolean, he: String, en: String): String {
    return if (isEnglish) en else he
}

private fun coachBroadcastTextAlign(isEnglish: Boolean): TextAlign {
    return if (isEnglish) TextAlign.Left else TextAlign.Right
}

@Composable
private fun CoachBroadcastLoadingRings(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val transition =
        rememberInfiniteTransition(
            label = "coachBroadcastLoading"
        )

    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1_250,
                        easing = LinearEasing
                    )
            ),
        label = "coachBroadcastOuterRing"
    )

    val innerRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1_750,
                        easing = LinearEasing
                    )
            ),
        label = "coachBroadcastInnerRing"
    )

    Box(
        modifier =
            modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .graphicsLayer {
                        rotationZ = outerRotation
                    }
                    .border(
                        width = 4.dp,
                        brush =
                            Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFFA855F7),
                                    Color(0xFF38BDF8),
                                    Color.Transparent
                                )
                            ),
                        shape = CircleShape
                    )
        )

        Box(
            modifier =
                Modifier
                    .size(size * 0.68f)
                    .graphicsLayer {
                        rotationZ = innerRotation
                    }
                    .border(
                        width = 3.dp,
                        brush =
                            Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFFF59E0B),
                                    Color(0xFF22C55E),
                                    Color.Transparent
                                )
                            ),
                        shape = CircleShape
                    )
        )

        Box(
            modifier =
                Modifier
                    .size(size * 0.25f)
                    .background(
                        color =
                            Color.White.copy(
                                alpha = 0.94f
                            ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color =
                            Color(0xFFA78BFA)
                                .copy(alpha = 0.55f),
                        shape = CircleShape
                    )
        )
    }
}

fun persistCoachBroadcast(
    region: String,
    branch: String,
    message: String,
    targetUids: List<String>,
    targetRecipients: List<Map<String, String>> = emptyList(),
    targetGroups: List<String> = emptyList(),
    onResult: (Boolean, Throwable?) -> Unit = { _, _ -> }
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val currentUser = auth.currentUser
    val currentUid = currentUser?.uid

    if (currentUid.isNullOrBlank()) {
        onResult(false, IllegalStateException("No logged-in user"))
        return
    }

    val cleanRegion = region.trim()
    val cleanBranch = branch.trim()
    val cleanMessage = message.trim()
    val cleanTargetUids = targetUids
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val cleanTargetGroups = targetGroups
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val cleanTargetRecipients = targetRecipients
        .mapNotNull { raw ->
            val uid = raw["uid"].orEmpty().trim()
            val name = raw["name"].orEmpty().trim()
            val phone = raw["phone"].orEmpty().trim()
            val email = raw["email"].orEmpty().trim()

            if (uid.isBlank() && phone.isBlank() && email.isBlank()) {
                null
            } else {
                mapOf(
                    "uid" to uid,
                    "name" to name,
                    "phone" to phone,
                    "email" to email
                )
            }
        }
        .distinctBy { recipient ->
            recipient["uid"]?.takeIf { it.isNotBlank() }
                ?: recipient["phone"]?.takeIf { it.isNotBlank() }
                ?: recipient["email"].orEmpty()
        }

    val cleanTargetPhones = cleanTargetRecipients
        .mapNotNull { it["phone"]?.trim()?.takeIf { phone -> phone.isNotBlank() } }
        .distinct()

    val cleanTargetNames = cleanTargetRecipients
        .mapNotNull { it["name"]?.trim()?.takeIf { name -> name.isNotBlank() } }
        .distinct()

    val cleanTargetEmails = cleanTargetRecipients
        .mapNotNull { it["email"]?.trim()?.takeIf { email -> email.isNotBlank() } }
        .distinct()

    if (cleanRegion.isBlank() || cleanBranch.isBlank()) {
        onResult(false, IllegalStateException("Missing region/branch"))
        return
    }

    if (cleanMessage.isBlank()) {
        onResult(false, IllegalStateException("Missing message"))
        return
    }

    if (cleanTargetUids.isEmpty()) {
        onResult(false, IllegalStateException("No selected recipients"))
        return
    }

    val coachName = currentUser.displayName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: currentUser.email
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: "מאמן"

    val nowMillis = System.currentTimeMillis()
    val expiresAtMillis = nowMillis + 30L * 24L * 60L * 60L * 1000L

    val docRef = db.collection("coachBroadcasts").document()
    val broadcastId = docRef.id

    val data = hashMapOf<String, Any?>(
        "broadcastId" to broadcastId,

        // סוג הודעה — חשוב ל־Cloud Function / Android Intent
        "type" to "coach_broadcast",

        "authorUid" to currentUid,
        "coachUid" to currentUid,
        "coachName" to coachName,

        "region" to cleanRegion,
        "branch" to cleanBranch,

        // קבוצות יעד — חשוב למסך הבית ולסינון הודעות מאמן
        "group" to cleanTargetGroups.joinToString(", "),
        "groupKey" to cleanTargetGroups.joinToString(", "),
        "groups" to cleanTargetGroups,
        "targetGroup" to cleanTargetGroups.joinToString(", "),
        "targetGroups" to cleanTargetGroups,
        "selectedGroups" to cleanTargetGroups,

        // תאימות למסכים קיימים
        "text" to cleanMessage,
        "message" to cleanMessage,
        "body" to cleanMessage,

        // נמענים אמיתיים
        "targetUids" to cleanTargetUids,
        "targetUidCount" to cleanTargetUids.size,
        "targetCount" to cleanTargetUids.size,

        // Snapshot קריא של הנמענים בזמן השליחה
        "targetRecipients" to cleanTargetRecipients,
        "targetPhones" to cleanTargetPhones,
        "targetNames" to cleanTargetNames,
        "targetEmails" to cleanTargetEmails,
        "targetRecipientSnapshotCount" to cleanTargetRecipients.size,

        // הכנה ל־Push
        "pushEnabled" to true,
        "pushTarget" to "targetUids",
        "pushStatus" to "pending",
        "pushCreatedBy" to "android_coach_broadcast",

        // זמנים
        "createdAt" to FieldValue.serverTimestamp(),
        "createdAtMillis" to nowMillis,
        "sentAtMillis" to nowMillis,

        // TTL: Firestore ימחק את ההודעה אוטומטית אחרי 30 יום
        "expiresAt" to Timestamp(Date(expiresAtMillis)),
        "expiresAtMillis" to expiresAtMillis,

        "source" to "android_coach_broadcast"
    )

    docRef
        .set(data.filterValues { it != null }, SetOptions.merge())
        .addOnSuccessListener {
            onResult(true, null)
        }
        .addOnFailureListener { e ->
            onResult(false, e)
        }
}

// ייצוג נמען אחד ברשימת הקבוצה
private data class CoachRecipient(
    val uid: String,
    val name: String,
    val phone: String,
    val email: String,
    val selected: Boolean,
    val canReceiveSms: Boolean = phone.isNotBlank()
)

//======================================================================
// Coach Broadcast PDF
//======================================================================

private fun shareCoachBroadcastPdf(
    context: Context,
    region: String,
    branch: String,
    groups: List<String>,
    message: String,
    recipients: List<CoachRecipient>,
    totalRecipients: Int,
    isEnglish: Boolean,
    demoPrivacyEnabled: Boolean
) {
    val file =
        createCoachBroadcastPdf(
            context = context,
            region = region,
            branch = branch,
            groups = groups,
            message = message,
            recipients = recipients,
            totalRecipients =
                totalRecipients,
            isEnglish = isEnglish,
            demoPrivacyEnabled =
                demoPrivacyEnabled
        )

    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    val intent =
        Intent(
            Intent.ACTION_SEND
        ).apply {
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_SUBJECT,
                if (isEnglish) {
                    "KAMI Broadcast Message"
                } else {
                    "שידור הודעה לקבוצה"
                }
            )

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            intent,
            if (isEnglish) {
                "Share PDF"
            } else {
                "שיתוף PDF"
            }
        )
    )
}

private fun createCoachBroadcastPdf(
    context: Context,
    region: String,
    branch: String,
    groups: List<String>,
    message: String,
    recipients: List<CoachRecipient>,
    totalRecipients: Int,
    isEnglish: Boolean,
    demoPrivacyEnabled: Boolean
): File {

    val pageWidth = 595
    val pageHeight = 842
    val margin = 32f

    val document =
        PdfDocument()

    val navy =
        android.graphics.Color.rgb(
            2,
            43,
            74
        )

    val mediumBlue =
        android.graphics.Color.rgb(
            36,
            103,
            158
        )

    val lightBlue =
        android.graphics.Color.rgb(
            128,
            183,
            220
        )

    val textDark =
        android.graphics.Color.rgb(
            15,
            23,
            42
        )

    val textMuted =
        android.graphics.Color.rgb(
            80,
            100,
            120
        )

    val boxBackground =
        android.graphics.Color.rgb(
            248,
            250,
            252
        )

    val boxBorder =
        android.graphics.Color.rgb(
            226,
            232,
            240
        )

    val regularTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.NORMAL
        )

    val boldTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.BOLD
        )

    fun textPaint(
        size: Float,
        color: Int = textDark,
        bold: Boolean = false,
        align: Paint.Align =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }
    ): Paint {
        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            typeface =
                if (bold) {
                    boldTypeface
                } else {
                    regularTypeface
                }

            textAlign = align
        }
    }

    var pageNumber = 0
    var page: PdfDocument.Page? = null
    lateinit var canvas: android.graphics.Canvas
    var y = 0f

    fun startPage() {
        page?.let {
            document.finishPage(it)
        }

        pageNumber++

        val info =
            PdfDocument.PageInfo
                .Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                )
                .create()

        val newPage =
            document.startPage(info)

        page = newPage
        canvas = newPage.canvas

        val pageCanvas = newPage.canvas

        pageCanvas.drawColor(
            android.graphics.Color.WHITE
        )

        //==============================================================
        // Header אחיד KAMI
        //==============================================================

        val headerBottom = 122f

        val navyPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = navy
                style = Paint.Style.FILL
            }

        val mediumStripe =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = mediumBlue
                style = Paint.Style.FILL
            }

        val lightStripe =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = lightBlue
                style = Paint.Style.FILL
            }

        pageCanvas.drawPath(
            android.graphics.Path().apply {
                moveTo(
                    pageWidth.toFloat(),
                    0f
                )
                lineTo(
                    pageWidth.toFloat(),
                    headerBottom
                )
                lineTo(
                    178f,
                    headerBottom
                )
                lineTo(
                    238f,
                    0f
                )
                close()
            },
            navyPaint
        )

        pageCanvas.drawPath(
            android.graphics.Path().apply {
                moveTo(
                    208f,
                    headerBottom
                )
                lineTo(
                    224f,
                    headerBottom
                )
                lineTo(
                    284f,
                    0f
                )
                lineTo(
                    268f,
                    0f
                )
                close()
            },
            mediumStripe
        )

        pageCanvas.drawPath(
            android.graphics.Path().apply {
                moveTo(
                    230f,
                    headerBottom
                )
                lineTo(
                    238f,
                    headerBottom
                )
                lineTo(
                    298f,
                    0f
                )
                lineTo(
                    290f,
                    0f
                )
                close()
            },
            lightStripe
        )

        // Logo
        val logoX = 78f
        val logoY = 58f
        val logoRadius = 42f

        pageCanvas.drawCircle(
            logoX,
            logoY,
            logoRadius,
            navyPaint
        )

        pageCanvas.drawCircle(
            logoX,
            logoY,
            logoRadius - 4f,
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    android.graphics.Color.WHITE
            }
        )

        pageCanvas.drawText(
            "KAMI",
            logoX,
            logoY + logoRadius * 0.22f,
            textPaint(
                size = logoRadius * 0.62f,
                color = navy,
                bold = true,
                align = Paint.Align.CENTER
            )
        )

        val headerX =
            pageWidth - 34f

        pageCanvas.drawText(
            if (isEnglish) {
                "Broadcast Message"
            } else {
                "שידור הודעה לקבוצה"
            },
            headerX,
            52f,
            textPaint(
                size = 24f,
                color =
                    android.graphics.Color.WHITE,
                bold = true,
                align = Paint.Align.RIGHT
            )
        )

        pageCanvas.drawText(
            if (isEnglish) {
                "Coach communication report"
            } else {
                "דו״ח תקשורת מאמן"
            },
            headerX,
            78f,
            textPaint(
                size = 11f,
                color =
                    android.graphics.Color.WHITE,
                align = Paint.Align.RIGHT
            )
        )

        val date =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(
                Date()
            )

        pageCanvas.drawText(
            if (isEnglish) {
                "Generated: $date"
            } else {
                "תאריך הפקה: $date"
            },
            headerX,
            142f,
            textPaint(
                size = 9f,
                color = textMuted,
                align = Paint.Align.RIGHT
            )
        )

        y = 170f
    }

    fun ensureSpace(
        required: Float
    ) {
        if (
            y + required >
            pageHeight - 42f
        ) {
            startPage()
        }
    }

    fun drawInfoRow(
        label: String,
        value: String
    ) {
        if (value.isBlank()) {
            return
        }

        ensureSpace(46f)

        val right =
            pageWidth.toFloat() - margin

        val rect =
            android.graphics.RectF(
                margin,
                y,
                right,
                y + 38f
            )

        val backgroundPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = boxBackground
                style = Paint.Style.FILL
            }

        val borderPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = boxBorder
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

        canvas.drawRoundRect(
            rect,
            8f,
            8f,
            backgroundPaint
        )

        canvas.drawRoundRect(
            rect,
            8f,
            8f,
            borderPaint
        )

        val x =
            if (isEnglish) {
                margin + 12f
            } else {
                right - 12f
            }

        canvas.drawText(
            "$label: $value",
            x,
            y + 24f,
            textPaint(
                size = 11f
            )
        )

        y += 46f
    }

    fun wrappedLines(
        text: String,
        maxChars: Int
    ): List<String> {
        val clean =
            text.trim()

        if (clean.isBlank()) {
            return emptyList()
        }

        val result =
            mutableListOf<String>()

        clean
            .split(
                Regex("\\s+")
            )
            .forEach { word ->

                val current =
                    result.lastOrNull()
                        .orEmpty()

                if (current.isBlank()) {
                    result += word
                } else {
                    val candidate =
                        "$current $word"

                    if (
                        candidate.length <=
                        maxChars
                    ) {
                        result[
                            result.lastIndex
                        ] = candidate
                    } else {
                        result += word
                    }
                }
            }

        return result
    }

    startPage()

    canvas.drawText(
        if (isEnglish) {
            "Broadcast details"
        } else {
            "פרטי השידור"
        },
        if (isEnglish) {
            margin
        } else {
            pageWidth.toFloat() - margin
        },
        y,
        textPaint(
            size = 16f,
            color = mediumBlue,
            bold = true
        )
    )

    y += 22f

    drawInfoRow(
        label =
            if (isEnglish) {
                "Region"
            } else {
                "אזור"
            },
        value = region.trim()
    )

    drawInfoRow(
        label =
            if (isEnglish) {
                "Branch"
            } else {
                "סניף"
            },
        value = branch.trim()
    )

    drawInfoRow(
        label =
            if (isEnglish) {
                "Groups"
            } else {
                "קבוצות"
            },
        value =
            groups.joinToString(", ")
    )

    drawInfoRow(
        label =
            if (isEnglish) {
                "Selected recipients"
            } else {
                "נמענים שנבחרו"
            },
        value =
            totalRecipients.toString()
    )

    ensureSpace(70f)

    canvas.drawText(
        if (isEnglish) {
            "Message"
        } else {
            "תוכן ההודעה"
        },
        if (isEnglish) {
            margin
        } else {
            pageWidth.toFloat() - margin
        },
        y,
        textPaint(
            size = 14f,
            color = mediumBlue,
            bold = true
        )
    )

    y += 20f

    wrappedLines(
        text = message,
        maxChars =
            if (isEnglish) {
                78
            } else {
                62
            }
    ).forEach { line ->
        ensureSpace(18f)

        canvas.drawText(
            line,
            if (isEnglish) {
                margin
            } else {
                pageWidth.toFloat() - margin
            },
            y,
            textPaint(
                size = 11f
            )
        )

        y += 16f
    }

    y += 14f

    if (recipients.isNotEmpty()) {

        ensureSpace(42f)

        canvas.drawText(
            if (isEnglish) {
                "Recipients"
            } else {
                "רשימת נמענים"
            },
            if (isEnglish) {
                margin
            } else {
                pageWidth.toFloat() - margin
            },
            y,
            textPaint(
                size = 14f,
                color = mediumBlue,
                bold = true
            )
        )

        y += 22f

        recipients.forEachIndexed {
                index,
                recipient ->

            ensureSpace(30f)

            val displayLine =
                buildString {
                    append(
                        "${index + 1}. "
                    )

                    append(
                        recipient.name
                            .ifBlank {
                                if (isEnglish) {
                                    "Unnamed trainee"
                                } else {
                                    "מתאמן ללא שם"
                                }
                            }
                    )

                    if (
                        !demoPrivacyEnabled &&
                        recipient.phone.isNotBlank()
                    ) {
                        append(
                            " · ${recipient.phone}"
                        )
                    }
                }

            canvas.drawText(
                displayLine,
                if (isEnglish) {
                    margin + 8f
                } else {
                    pageWidth.toFloat() -
                            margin -
                            8f
                },
                y,
                textPaint(
                    size = 10.5f
                )
            )

            y += 22f
        }
    }

    page?.let {
        document.finishPage(it)
    }

    val directory =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    /*
     * שם קבוע:
     * כל יצירה חדשה מחליפה את הקובץ הקודם.
     */
    val fileName =
        if (isEnglish) {
            "Broadcast Message.pdf"
        } else {
            "שידור הודעה.pdf"
        }

    val file =
        File(
            directory,
            fileName
        )

    FileOutputStream(
        file,
        false
    ).use { output ->
        document.writeTo(output)
    }

    document.close()

    return file
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachBroadcastScreen(
    branchesByRegion: Map<String, List<String>>,
    defaultRegion: String?,
    defaultBranch: String?,
    onBack: () -> Unit,
    onHome: () -> Unit,

    // ✅ פעולות פלטפורמה
    onOpenSms: (numbers: List<String>, message: String) -> Unit = { _, _ -> },

    @Suppress("UNUSED_PARAMETER")
    onShareText: (message: String) -> Unit = {}
) {
    val contextLang = LocalContext.current
    val langManager = remember { AppLanguageManager(contextLang) }

    var currentLanguage by remember {
        mutableStateOf(langManager.getCurrentLanguage())
    }

    val isEnglish =
        currentLanguage ==
                AppLanguage.ENGLISH

    val screenTextAlign =
        coachBroadcastTextAlign(isEnglish)

    val layoutDirection =
        if (isEnglish) {
            LayoutDirection.Ltr
        } else {
            LayoutDirection.Rtl
        }

    val isDarkMode =
        androidx.compose.material3.MaterialTheme
            .colorScheme
            .background
            .luminance() < 0.5f

    /*
     * הקריאה מתבצעת מתוך Composition ולכן
     * שינוי המתג בהגדרות מעדכן את המסך מיד.
     */
    val demoPrivacyEnabled =
        DemoPrivacy.isEnabled()

    val userSp =
        remember(contextLang) {
            contextLang.getSharedPreferences(
                "kmi_user",
                Context.MODE_PRIVATE
            )
        }

    /*
     * המבנה החדש:
     *
     * כל סניף מחזיק רק את הקבוצות שאליהן
     * המאמן משויך באותו סניף.
     */
    val coachBranchAssignments =
        remember(userSp) {
            CoachBranchAssignmentsCodec.decode(
                userSp.getString(
                    "coach_branch_assignments_json",
                    ""
                )
            )
        }

    fun normalizedAssignmentValue(
        value: String
    ): String {
        return value
            .trim()
            .replace('־', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace(
                Regex("\\s+"),
                " "
            )
            .lowercase()
    }

    val assignedBranchNames =
        remember(coachBranchAssignments) {
            CoachBranchAssignmentsCodec
                .flattenBranches(
                    coachBranchAssignments
                )
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotBlank()
                }
                .distinct()
        }

    /*
     * אם קיים המבנה החדש, מסננים את רשימת
     * הסניפים לפי השיוכים האמיתיים של המאמן.
     *
     * אם המבנה החדש עדיין ריק, נשמר fallback
     * למידע הישן כדי לא לפגוע במשתמשים קיימים.
     */
    val visibleBranchesByRegion =
        remember(
            branchesByRegion,
            assignedBranchNames
        ) {
            if (assignedBranchNames.isEmpty()) {
                branchesByRegion
            } else {
                val assignedNormalized =
                    assignedBranchNames
                        .map(::normalizedAssignmentValue)
                        .toSet()

                branchesByRegion
                    .mapValues { (_, branches) ->
                        branches.filter { candidate ->
                            normalizedAssignmentValue(
                                candidate
                            ) in assignedNormalized
                        }
                    }
                    .filterValues {
                        it.isNotEmpty()
                    }
            }
        }

    fun readCoachGroupKey(): String {
        return userSp.getString("active_group", null)
            ?: userSp.getString("activeGroup", null)
            ?: userSp.getString("primaryGroup", null)
            ?: userSp.getString("groupKey", null)
            ?: userSp.getString("group_key", null)
            ?: userSp.getString("age_group", null)
            ?: userSp.getString("group", null)
            ?: ""
    }

    var coachGroupKey by remember {
        mutableStateOf(readCoachGroupKey())
    }

    androidx.compose.runtime.DisposableEffect(userSp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "active_group" ||
                key == "activeGroup" ||
                key == "primaryGroup" ||
                key == "groupKey" ||
                key == "group_key" ||
                key == "age_group" ||
                key == "group"
            ) {
                coachGroupKey = readCoachGroupKey()
            }
        }

        userSp.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            userSp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    var sendScope by remember { mutableStateOf("groups") }

    var availableBranchGroups by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var availableBranchGroupCounts by remember {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }

    var selectedTargetGroups by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    val effectiveGroupKeys = remember(sendScope, selectedTargetGroups) {
        if (sendScope == "branch") {
            emptyList()
        } else {
            selectedTargetGroups
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }
    }

    var region by remember {
        mutableStateOf(
            defaultRegion.orEmpty()
        )
    }

    var branch by remember {
        mutableStateOf(
            defaultBranch.orEmpty()
        )
    }

    var message by remember {
        mutableStateOf("")
    }

    var isSending by remember {
        mutableStateOf(false)
    }

    /*
     * הקבוצות המותרות נקראות מהשיוך של
     * הסניף שנבחר בלבד.
     */
    val assignedGroupsForSelectedBranch =
        remember(
            coachBranchAssignments,
            branch
        ) {
            val normalizedBranch =
                normalizedAssignmentValue(
                    branch
                )

            coachBranchAssignments
                .firstOrNull { assignment ->
                    normalizedAssignmentValue(
                        assignment.branch
                    ) == normalizedBranch
                }
                ?.groups
                ?.map {
                    it.trim()
                }
                ?.filter {
                    it.isNotBlank()
                }
                ?.distinct()
                .orEmpty()
        }

    // רשימת הנמענים מהקבוצה (נשלפת מפיירסטור)
    var recipients by remember {
        mutableStateOf<List<CoachRecipient>>(emptyList())
    }

    var isRecipientsLoading by remember {
        mutableStateOf(false)
    }

    val db = remember { FirebaseFirestore.getInstance() }

    // Snackbar לפידבק
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ===== טעינת חברי הקבוצה מה- Firestore לפי אזור + סניף + קבוצה =====
    LaunchedEffect(
        region,
        branch,
        sendScope,
        selectedTargetGroups,
        coachGroupKey,
        coachBranchAssignments,
        assignedGroupsForSelectedBranch
    ) {
        fun String.norm(): String {
            return trim()
                .replace('־', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun String.pickPrimaryBranch(): String {
            return split(",", "•", "|")
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
                ?: trim()
        }

        fun splitTokensNorm(raw: String?): List<String> {
            if (raw.isNullOrBlank()) return emptyList()

            return raw
                .replace(" • ", ",")
                .replace("|", ",")
                .replace("\n", ",")
                .split(',', ';', '；')
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun normalizePhoneKey(raw: String): String {
            return raw
                .trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".", "")
        }

        fun expandGroupAliases(raw: String): List<String> {
            val n = raw.norm()

            // ✅ התאמת קבוצות חייבת להיות מדויקת.
            // לא מפרקים "נוער + בוגרים" ל-"נוער" ו-"בוגרים",
            // כדי שלא יתערבבו רשימות מתאמנים בין קבוצות שונות.
            return buildList {
                add(n)
                addAll(splitTokensNorm(n))

                when (n.lowercase()) {
                    "children", "kids" -> add("ילדים")
                    "youth" -> add("נוער")
                    "adults", "adult" -> add("בוגרים")
                }
            }
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun DocumentSnapshot.roleText(): String {
            return (
                    getString("role")
                        ?: getString("userType")
                        ?: getString("type")
                        ?: ""
                    )
                .trim()
                .lowercase()
        }

        fun DocumentSnapshot.isActiveUser(): Boolean {
            val activeBoolean = getBoolean("isActive")
            val activeText = (
                    getString("status")
                        ?: getString("active")
                        ?: ""
                    ).trim().lowercase()

            return activeBoolean != false &&
                    activeText != "inactive" &&
                    activeText != "disabled" &&
                    activeText != "blocked" &&
                    activeText != "לא פעיל"
        }

        fun DocumentSnapshot.isTraineeRole(): Boolean {
            val role = roleText()

            return role.isBlank() ||
                    role == "trainee" ||
                    role.contains("trainee") ||
                    role.contains("student") ||
                    role.contains("מתאמן") ||
                    role.contains("חניך")
        }

        fun DocumentSnapshot.branchTokensNorm(): List<String> {
            val branchesList = (get("branches") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                .orEmpty()

            return buildList {
                addAll(branchesList.map { it.norm() })
                addAll(splitTokensNorm(getString("branchesCsv")))
                addAll(splitTokensNorm(getString("branch")))
                addAll(splitTokensNorm(getString("activeBranch")))
                addAll(splitTokensNorm(getString("active_branch")))
            }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun DocumentSnapshot.groupTokensNorm(): List<String> {
            val groupsList = (get("groups") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                ?.flatMap { expandGroupAliases(it) }
                .orEmpty()

            return buildList {
                addAll(groupsList)
                addAll(splitTokensNorm(getString("primaryGroup")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("activeGroup")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("active_group")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupKey")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("group_key")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("group")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupName")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupsCsv")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("groupCsv")).flatMap { expandGroupAliases(it) })
                addAll(splitTokensNorm(getString("age_group")).flatMap { expandGroupAliases(it) })
            }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun DocumentSnapshot.groupDisplayTokens(): List<String> {
            val groupsList = (get("groups") as? List<*>)
                ?.mapNotNull { it?.toString()?.trim() }
                .orEmpty()

            return buildList {
                addAll(groupsList)
                addAll(splitTokensNorm(getString("primaryGroup")))
                addAll(splitTokensNorm(getString("activeGroup")))
                addAll(splitTokensNorm(getString("active_group")))
                addAll(splitTokensNorm(getString("groupKey")))
                addAll(splitTokensNorm(getString("group_key")))
                addAll(splitTokensNorm(getString("group")))
                addAll(splitTokensNorm(getString("groupName")))
                addAll(splitTokensNorm(getString("groupsCsv")))
                addAll(splitTokensNorm(getString("groupCsv")))
                addAll(splitTokensNorm(getString("age_group")))
            }
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        fun matchesAnyToken(tokens: List<String>, candidates: Set<String>): Boolean {
            if (tokens.isEmpty() || candidates.isEmpty()) return false

            return tokens.any { token ->
                token in candidates ||
                        candidates.any { candidate ->
                            candidate.length >= 2 &&
                                    token.length >= 2 &&
                                    (token.contains(candidate) || candidate.contains(token))
                        }
            }
        }

        fun matchesExactGroupToken(tokens: List<String>, candidates: Set<String>): Boolean {
            if (tokens.isEmpty() || candidates.isEmpty()) return false

            val cleanTokens = tokens
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .toSet()

            val cleanCandidates = candidates
                .map { it.norm() }
                .filter { it.isNotBlank() }
                .toSet()

            return cleanTokens.any { token ->
                token in cleanCandidates
            }
        }

        fun DocumentSnapshot.displayNameOrPhone(phone: String): String {
            return getString("fullName")?.takeIf { it.isNotBlank() }
                ?: getString("name")?.takeIf { it.isNotBlank() }
                ?: getString("displayName")?.takeIf { it.isNotBlank() }
                ?: getString("email")?.takeIf { it.isNotBlank() }
                ?: phone
        }

        val regionNorm = region.norm()

        val branchNames = splitTokensNorm(branch)
            .ifEmpty {
                listOf(branch.norm().pickPrimaryBranch())
            }
            .map { it.pickPrimaryBranch().norm() }
            .filter { it.isNotBlank() }
            .distinct()

        val selectedGroupNames = effectiveGroupKeys.map { it.norm() }

        if (regionNorm.isBlank() || branchNames.isEmpty()) {
            availableBranchGroups = emptyList()
            availableBranchGroupCounts = emptyMap()
            selectedTargetGroups = emptySet()
            isRecipientsLoading = false
            recipients = emptyList()
            return@LaunchedEffect
        }

        // ✅ לא מאפסים כאן selectedTargetGroups.
        // הסיבה: שינוי בחירת קבוצה מפעיל מחדש את LaunchedEffect,
        // ואם נאפס כאן — הקבוצה לא תישאר מסומנת והמתאמנים לא ייטענו.
        recipients = emptyList()
        isRecipientsLoading = true

        val branchCandidates = branchNames
            .flatMap { branchName ->
                listOf(
                    branchName,
                    branchName.replace("-", "–"),
                    branchName.replace("-", "—"),
                    branchName.replace("-", "־"),
                    branchName.replace("–", "-"),
                    branchName.replace("—", "-"),
                    branchName.replace("־", "-")
                )
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val branchCandidateSet = branchCandidates
            .map { it.norm() }
            .toSet()

        val groupCandidateSet = selectedGroupNames
            .flatMap { expandGroupAliases(it) }
            .toSet()

        fun docsToRecipients(docs: List<DocumentSnapshot>): List<CoachRecipient> {
            val currentSelectionByUid = recipients.associate { it.uid to it.selected }
            val currentSelectionByPhone = recipients.associate { it.phone to it.selected }

            return docs
                .asSequence()
                .filter { it.isActiveUser() }
                .filter { it.isTraineeRole() }
                .filter { matchesAnyToken(it.branchTokensNorm(), branchCandidateSet) }
                .filter {
                    // ✅ במצב קבוצות — התאמה מדויקת בלבד.
                    // "בוגרים" לא ימשוך "נוער + בוגרים", ולהפך.
                    if (sendScope == "branch") {
                        true
                    } else {
                        groupCandidateSet.isNotEmpty() &&
                                matchesExactGroupToken(it.groupTokensNorm(), groupCandidateSet)
                    }
                }
                .mapNotNull { doc ->
                    val phone = (
                            doc.getString("phone")
                                ?: doc.getString("phoneNumber")
                                ?: doc.getString("phone_number")
                                ?: ""
                            ).trim()

                    val uid = (
                            doc.getString("uid")
                                ?: doc.getString("authUid")
                                ?: doc.id
                            ).trim()

                    val email = doc.getString("email")
                        ?.trim()
                        .orEmpty()

                    val resolvedUid = uid.ifBlank { doc.id }
                    val name = doc.displayNameOrPhone(phone).trim()

                    if (resolvedUid.isBlank() && phone.isBlank() && email.isBlank()) {
                        return@mapNotNull null
                    }

                    CoachRecipient(
                        uid = resolvedUid,
                        name = name,
                        phone = phone,
                        email = email,
                        selected = currentSelectionByUid[resolvedUid]
                            ?: currentSelectionByPhone[phone]
                            ?: true,
                        canReceiveSms = phone.isNotBlank()
                    )
                }
                .distinctBy { recipient ->
                    val phoneKey = normalizePhoneKey(recipient.phone)
                    val emailKey = recipient.email.trim().lowercase()
                    val nameKey = recipient.name.trim().lowercase()

                    when {
                        phoneKey.isNotBlank() -> "phone:$phoneKey"
                        emailKey.isNotBlank() -> "email:$emailKey"
                        recipient.uid.isNotBlank() -> "uid:${recipient.uid}"
                        else -> "name:$nameKey"
                    }
                }
                .sortedBy { it.name }
                .toList()
        }

        suspend fun fetchCandidateDocs(): List<DocumentSnapshot> {
            val col = db.collection("users")
            val out = mutableListOf<DocumentSnapshot>()

            for (candidate in branchCandidates) {
                runCatching {
                    out.addAll(
                        col.whereArrayContains("branches", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("branchesCsv", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("branch", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("activeBranch", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }

                runCatching {
                    out.addAll(
                        col.whereEqualTo("active_branch", candidate)
                            .get()
                            .await()
                            .documents
                    )
                }
            }

            val distinct = out.distinctBy { it.id }

            if (distinct.isNotEmpty()) {
                return distinct
            }

            // fallback זהיר: רק אם אין שום תוצאה ישירה.
            // מוגבל ל־3000 כדי לא להעמיס בפרויקטים גדולים.
            val all = mutableListOf<DocumentSnapshot>()
            var last: DocumentSnapshot? = null

            while (true) {
                var q = col
                    .orderBy(FieldPath.documentId())
                    .limit(1000)

                last?.let { lastDocument ->
                    q = q.startAfter(lastDocument)
                }

                val snap = q.get().await()
                val page = snap.documents

                if (page.isEmpty()) break

                all.addAll(page)
                last = page.last()

                if (all.size >= 3000) break
            }

            return all.distinctBy { it.id }
        }

        try {
            val docs = fetchCandidateDocs()

            val branchMatchedDocs = docs
                .asSequence()
                .filter { it.isActiveUser() }
                .filter { it.isTraineeRole() }
                .filter { matchesAnyToken(it.branchTokensNorm(), branchCandidateSet) }
                .toList()

            val discoveredBranchGroups =
                branchMatchedDocs
                    .asSequence()
                    .flatMap {
                        it.groupDisplayTokens()
                            .asSequence()
                    }
                    .map {
                        it.norm()
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
                    .sorted()
                    .toList()

            /*
             * המבנה החדש הוא מקור האמת.
             *
             * אם קיימות קבוצות משויכות לסניף,
             * מציגים רק אותן — גם אם קיימים
             * במסד מתאמנים מקבוצות אחרות.
             *
             * כאשר אין עדיין מבנה חדש, משתמשים
             * זמנית בקבוצות שהתגלו מהמסמכים.
             */
            val branchGroups =
                if (
                    coachBranchAssignments
                        .isNotEmpty()
                ) {
                    assignedGroupsForSelectedBranch
                        .map {
                            it.norm()
                        }
                        .filter {
                            it.isNotBlank()
                        }
                        .distinct()
                        .sorted()
                } else {
                    discoveredBranchGroups
                }

            val groupCounts =
                branchGroups.associateWith { groupName ->
                val groupCandidates = expandGroupAliases(groupName).toSet()

                branchMatchedDocs
                    .asSequence()
                    .filter { doc ->
                        matchesExactGroupToken(
                            tokens = doc.groupTokensNorm(),
                            candidates = groupCandidates
                        )
                    }
                    .mapNotNull { doc ->
                        val phone = (
                                doc.getString("phone")
                                    ?: doc.getString("phoneNumber")
                                    ?: doc.getString("phone_number")
                                    ?: ""
                                ).trim()

                        val uid = (
                                doc.getString("uid")
                                    ?: doc.getString("authUid")
                                    ?: doc.id
                                ).trim()

                        val email = doc.getString("email")
                            ?.trim()
                            .orEmpty()

                        val name = doc.displayNameOrPhone(phone).trim()

                        val phoneKey = normalizePhoneKey(phone)
                        val emailKey = email.lowercase()
                        val nameKey = name.lowercase()

                        when {
                            phoneKey.isNotBlank() -> "phone:$phoneKey"
                            emailKey.isNotBlank() -> "email:$emailKey"
                            uid.isNotBlank() -> "uid:$uid"
                            nameKey.isNotBlank() -> "name:$nameKey"
                            else -> null
                        }
                    }
                    .distinct()
                    .count()
            }

            availableBranchGroups = branchGroups
            availableBranchGroupCounts = groupCounts

            if (sendScope != "branch" && branchGroups.isNotEmpty()) {
                val normalizedSelectedGroups = selectedTargetGroups
                    .map { it.norm() }
                    .toSet()

                val selectedStillValid = normalizedSelectedGroups
                    .filter { it in branchGroups }
                    .toSet()

                if (selectedStillValid != normalizedSelectedGroups) {
                    selectedTargetGroups = selectedStillValid
                    recipients = emptyList()
                    isRecipientsLoading = false
                    return@LaunchedEffect
                }
            }

            val finalRecipients = docsToRecipients(docs)

            recipients = finalRecipients
            isRecipientsLoading = false
        } catch (_: Exception) {
            availableBranchGroups = emptyList()
            availableBranchGroupCounts = emptyMap()
            recipients = emptyList()
            isRecipientsLoading = false
        }
    }

    /*
     * הרשימה האמיתית נשארת בזיכרון לצורך שליחה.
     * החלפת השם מתבצעת בשכבת התצוגה בלבד.
     */
    val uiRecipients =
        remember(
            recipients,
            demoPrivacyEnabled,
            isEnglish
        ) {
            recipients.mapIndexed {
                    index,
                    recipient ->

                recipient.copy(
                    name =
                        TraineeDisplayNameMapper
                            .displayName(
                                realName =
                                    recipient.name,
                                stableKey =
                                    recipient.uid
                                        .ifBlank {
                                            recipient.email
                                                .ifBlank {
                                                    recipient.phone
                                                }
                                        },
                                demoIndex = index + 1,
                                isEnglish =
                                    isEnglish
                            )
                            .ifBlank {
                                coachBroadcastTr(
                                    isEnglish,
                                    "מתאמן ללא שם",
                                    "Unnamed trainee"
                                )
                            }
                )
            }
        }

    // נמענים שנבחרו (גם טלפונים וגם UIDs)
    val selectedRecipients =
        recipients.filter {
            it.selected
        }

    /*
     * רשימת תצוגה בלבד.
     *
     * במצב הדגמה היא כוללת:
     * מתאמן 1, מתאמן 2 וכו'.
     *
     * הרשימה האמיתית נשארת ב-selectedRecipients
     * לצורך SMS / Push / Firestore בלבד.
     */
    val selectedUiRecipients =
        uiRecipients.filter {
            it.selected
        }

    val selectedNumbers =
        selectedRecipients
            .map {
                it.phone.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()

    val selectedUids =
        selectedRecipients
        .map { it.uid.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val selectedRecipientSnapshots = selectedRecipients.map { recipient ->
        mapOf(
            "uid" to recipient.uid,
            "name" to recipient.name,
            "phone" to recipient.phone,
            "email" to recipient.email
        )
    }

    val allSelected = recipients.isNotEmpty() && recipients.all { it.selected }

    val sendButtonText = when {
        selectedUids.isEmpty() -> coachBroadcastTr(
            isEnglish,
            "בחר מתאמנים לשליחה",
            "Select trainees to send"
        )

        allSelected -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה לכל המתאמנים",
            "Send message to all trainees"
        )

        selectedUids.size == 1 -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה למתאמן שנבחר",
            "Send message to selected trainee"
        )

        else -> coachBroadcastTr(
            isEnglish,
            "שליחת הודעה ל-${selectedUids.size} מתאמנים",
            "Send message to ${selectedUids.size} trainees"
        )
    }

    // שמירת השידור עם ה־UIDs שנבחרו ל-Firestore
    fun saveBroadcast() {
        val cleanRegion = region.trim()
        val cleanBranch = branch.trim()
        val cleanMessage = message.trim()

        // ✅ שמירה ודאית מתוך המסך עצמו.
        // כך גם אם הניווט מעביר onPersistBroadcast ריק/ישן, עדיין יווצר מסמך ב-coachBroadcasts.
        persistCoachBroadcast(
            region = cleanRegion,
            branch = cleanBranch,
            message = cleanMessage,
            targetUids = selectedUids,
            targetRecipients = selectedRecipientSnapshots,
            targetGroups = effectiveGroupKeys,
            onResult = { ok, error ->
                if (ok) {
                    isSending = false

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            coachBroadcastTr(
                                isEnglish,
                                "ההודעה נשמרה ונשלחה לעיבוד Push עבור ${selectedRecipients.size} נמענים",
                                "The message was saved and sent for Push processing to ${selectedRecipients.size} recipients"
                            )
                        )
                    }
                } else {
                    isSending = false

                    scope.launch {
                        snackbarHostState.showSnackbar(
                            coachBroadcastTr(
                                isEnglish,
                                "שמירת ההודעה נכשלה: ${error?.localizedMessage ?: "שגיאה לא ידועה"}",
                                "Saving the message failed: ${error?.localizedMessage ?: "Unknown error"}"
                            )
                        )
                    }
                }
            }
        )
    }

    // 🔵 רקע אחיד למצב בהיר וכהה
    val gradientBackground =
        Brush.verticalGradient(
            colors =
                if (isDarkMode) {
                    listOf(
                        androidx.compose.material3
                            .MaterialTheme
                            .colorScheme
                            .background,
                        androidx.compose.material3
                            .MaterialTheme
                            .colorScheme
                            .surface,
                        Color(0xFF10243A),
                        Color(0xFF0A3657),
                        Color(0xFF041E33)
                    )
                } else {
                    listOf(
                        Color(0xFFF8FBFF),
                        Color(0xFFEAF4FF),
                        Color(0xFFB7DDF7),
                        Color(0xFF1F78B4),
                        Color(0xFF062B4A)
                    )
                }
        )

    val fieldContainerColor =
        if (isDarkMode) {
            androidx.compose.material3
                .MaterialTheme
                .colorScheme
                .surface
        } else {
            Color.White
        }

    val fieldTextColor =
        if (isDarkMode) {
            androidx.compose.material3
                .MaterialTheme
                .colorScheme
                .onSurface
        } else {
            Color(0xFF1E2A3D)
        }

    val fieldLabelColor =
        if (isDarkMode) {
            androidx.compose.material3
                .MaterialTheme
                .colorScheme
                .onSurfaceVariant
        } else {
            Color(0xFF5E6C80)
        }

    val fieldBorderColor =
        if (isDarkMode) {
            androidx.compose.material3
                .MaterialTheme
                .colorScheme
                .outline
                .copy(alpha = 0.55f)
        } else {
            Color(0xFFD8E3F5)
        }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor =
                fieldContainerColor,
            unfocusedContainerColor =
                fieldContainerColor,
            disabledContainerColor =
                fieldContainerColor,
            focusedTextColor =
                fieldTextColor,
            unfocusedTextColor =
                fieldTextColor,
            disabledTextColor =
                fieldTextColor,
            focusedLabelColor =
                fieldLabelColor,
            unfocusedLabelColor =
                fieldLabelColor,
            disabledLabelColor =
                fieldLabelColor,
            focusedBorderColor =
                fieldBorderColor,
            unfocusedBorderColor =
                fieldBorderColor,
            disabledBorderColor =
                fieldBorderColor,
            cursorColor =
                fieldTextColor
        )

    val cardContainerColor =
        if (isDarkMode) {
            androidx.compose.material3
                .MaterialTheme
                .colorScheme
                .surface
                .copy(alpha = 0.94f)
        } else {
            Color(0xFFF4F8FF)
        }

    val cardBorderColor =
        if (isDarkMode) {
            Color(0xFF60A5FA)
                .copy(alpha = 0.42f)
        } else {
            Color(0xFFB8D5F4)
        }

    val primaryContentColor =
        if (isDarkMode) {
            androidx.compose.material3
                .MaterialTheme
                .colorScheme
                .onSurface
        } else {
            Color(0xFF1E2A3D)
        }

    val secondaryContentColor =
        if (isDarkMode) {
            androidx.compose.material3
                .MaterialTheme
                .colorScheme
                .onSurfaceVariant
        } else {
            Color(0xFF5E6C80)
        }

    val recipientListColor =
        if (isDarkMode) {
            Color(0xFF0F2033)
                .copy(alpha = 0.96f)
        } else {
            Color.White.copy(alpha = 0.95f)
        }

    val selectedRecipientColor =
        if (isDarkMode) {
            Color(0xFF123A56)
        } else {
            Color(0xFFE0F2FE)
        }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        Scaffold(
            topBar = {
                il.kmi.app.ui.KmiTopBar(
                    title = coachBroadcastTr(
                        isEnglish,
                        "שידור הודעה לקבוצה",
                        "Broadcast Message"
                    ),
                    onBack = onBack,
                    onHome = onHome,
                    onOpenDrawer = {
                        il.kmi.app.ui.DrawerBridge.open()
                    },
                    showRoleStatus = false,
                    lockSearch = false,

                    // ✅ לא נועל את הבית בסרגל האייקונים התחתון.
                    // ✅ רק מסתיר את אייקון הבית הקטן ליד הכותרת העליונה.
                    lockHome = false,
                    showTopHome = false,

                    showBottomActions = true,

                    showTopShare = true,
                    onShare = {
                        shareCoachBroadcastPdf(
                            context = contextLang,
                            region = region,
                            branch = branch,
                            groups = effectiveGroupKeys,
                            message = message,
                            recipients = selectedUiRecipients,
                            totalRecipients =
                                selectedRecipients.size,
                            isEnglish = isEnglish,
                            demoPrivacyEnabled =
                                demoPrivacyEnabled
                        )
                    },

                    currentLang =
                        if (isEnglish) {
                            "en"
                        } else {
                            "he"
                        },

                    onToggleLanguage = {
                    val newLang =
                        if (currentLanguage == AppLanguage.HEBREW) {
                            AppLanguage.ENGLISH
                        } else {
                            AppLanguage.HEBREW
                        }

                    langManager.setLanguage(newLang)
                    currentLanguage = newLang
                    (contextLang as? Activity)?.recreate()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0)
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(gradientBackground)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    color = cardContainerColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = cardBorderColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ===== אזור =====
                        KmiPremiumDropdown(
                            title =
                                coachBroadcastTr(
                                    isEnglish,
                                    "אזור",
                                    "Region"
                                ),
                            options =
                                visibleBranchesByRegion
                                    .keys
                                    .toList(),
                            selectedValue = region,
                            isEnglish = isEnglish,
                            placeholder =
                                coachBroadcastTr(
                                    isEnglish,
                                    "בחר אזור",
                                    "Select region"
                                ),
                            onSelected = { selectedRegion ->
                                region = selectedRegion

                                // שינוי אזור מחייב בחירת סניף מחדש.
                                branch = ""
                                selectedTargetGroups = emptySet()
                                availableBranchGroups = emptyList()
                                availableBranchGroupCounts = emptyMap()
                                recipients = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ===== סניף =====
                        if (region.isNotBlank()) {
                            KmiPremiumDropdown(
                                title =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "סניף",
                                        "Branch"
                                    ),
                                options =
                                    visibleBranchesByRegion[
                                        region
                                    ].orEmpty(),
                                selectedValue = branch,
                                isEnglish = isEnglish,
                                placeholder =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "בחר סניף",
                                        "Select branch"
                                    ),
                                onSelected = { selectedBranch ->
                                    branch = selectedBranch

                                    // מעבר לסניף אחר מאפס רק מידע
                                    // שתלוי בסניף הקודם.
                                    selectedTargetGroups = emptySet()
                                    availableBranchGroups = emptyList()
                                    availableBranchGroupCounts = emptyMap()
                                    recipients = emptyList()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // ===== טקסט ההודעה =====
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = {
                                Text(
                                    coachBroadcastTr(isEnglish, "טקסט ההודעה", "Message text"),
                                    textAlign = screenTextAlign
                                )
                            },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle =
                                KmiTypography.body.copy(
                                    textAlign =
                                        screenTextAlign,
                                    color =
                                        fieldTextColor
                                ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            colors = fieldColors
                        )
                    }
                }

                if (
                    branch.isNotBlank() &&
                    availableBranchGroups.isNotEmpty()
                ) {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation
                                .shape
                                .RoundedCornerShape(22.dp),
                        color = cardContainerColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = cardBorderColor
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "בחירת קבוצות לשליחה",
                                        "Select groups to include"
                                    ),
                                color =
                                    primaryContentColor,
                                style =
                                    KmiTypography.cardTitle,
                                fontWeight =
                                    androidx.compose.ui.text
                                        .font
                                        .FontWeight.ExtraBold,
                                textAlign =
                                    screenTextAlign,
                                modifier =
                                    Modifier.fillMaxWidth()
                            )

                            val areAllGroupsSelected =
                                availableBranchGroups.isNotEmpty() &&
                                        selectedTargetGroups.containsAll(availableBranchGroups)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    onClick = {
                                        sendScope = "groups"

                                        selectedTargetGroups =
                                            if (areAllGroupsSelected) {
                                                emptySet()
                                            } else {
                                                availableBranchGroups.toSet()
                                            }

                                        recipients = emptyList()
                                        isRecipientsLoading = true
                                    },
                                    modifier =
                                        Modifier
                                            .widthIn(
                                                min = 210.dp,
                                                max = 280.dp
                                            )
                                            .heightIn(
                                                min = 58.dp
                                            ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                    color = Color.Transparent,
                                    shadowElevation = 0.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        if (areAllGroupsSelected) {
                                            Color(0xFFFBBF24)
                                        } else {
                                            Color(0xFF67E8F9)
                                        }
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = if (areAllGroupsSelected) {
                                                        listOf(
                                                            Color(0xFF92400E),
                                                            Color(0xFFD97706),
                                                            Color(0xFFF59E0B)
                                                        )
                                                    } else {
                                                        listOf(
                                                            Color(0xFF155E75),
                                                            Color(0xFF0891B2),
                                                            Color(0xFF22D3EE)
                                                        )
                                                    }
                                                )
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text =
                                                    if (
                                                        areAllGroupsSelected
                                                    ) {
                                                        "↩️"
                                                    } else {
                                                        "☑️"
                                                    },
                                                style =
                                                    KmiTypography.action,
                                                textAlign =
                                                    TextAlign.Center,
                                                maxLines = 1
                                            )

                                            Spacer(Modifier.width(7.dp))

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = if (areAllGroupsSelected) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "בטל סימון לכולם",
                                                            "Unselect all"
                                                        )
                                                    } else if (availableBranchGroups.size == 1) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "בחר את קבוצת ${availableBranchGroups.first()}",
                                                            "Select ${availableBranchGroups.first()}"
                                                        )
                                                    } else {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "סמן את כולם",
                                                            "Select all"
                                                        )
                                                    },
                                                    textAlign =
                                                        TextAlign.Center,
                                                    color =
                                                        Color.White,
                                                    style =
                                                        KmiTypography.action,
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = if (areAllGroupsSelected) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "הקבוצה מסומנת לשליחה",
                                                            "Group selected for sending"
                                                        )
                                                    } else if (availableBranchGroups.size == 1) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "לחץ כאן או סמן את הריבוע למטה",
                                                            "Tap here or tick the checkbox below"
                                                        )
                                                    } else {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "${availableBranchGroups.size} קבוצות · ${
                                                                availableBranchGroups.sumOf { availableBranchGroupCounts[it] ?: 0 }
                                                            } מתאמנים",
                                                            "${availableBranchGroups.size} groups · ${
                                                                availableBranchGroups.sumOf { availableBranchGroupCounts[it] ?: 0 }
                                                            } trainees"
                                                        )
                                                    },
                                                    textAlign = TextAlign.Center,
                                                    color = if (areAllGroupsSelected) {
                                                        Color(0xFFFFF7ED)
                                                    } else {
                                                        Color(0xFFE0F2FE)
                                                    },
                                                    style =
                                                        KmiTypography.caption,
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableBranchGroups.forEach { groupName ->
                                    val isSelected = sendScope != "branch" &&
                                            selectedTargetGroups.contains(groupName)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                sendScope = "groups"

                                                selectedTargetGroups =
                                                    if (isSelected) {
                                                        selectedTargetGroups - groupName
                                                    } else {
                                                        selectedTargetGroups + groupName
                                                    }

                                                recipients = emptyList()
                                                isRecipientsLoading = true
                                            },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                        color = if (isSelected) {
                                            Color(0xFFD9F1FF)
                                        } else {
                                            Color(0xFF0B1220).copy(alpha = 0.78f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) {
                                                Color(0xFF38BDF8)
                                            } else {
                                                Color.White.copy(alpha = 0.12f)
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = if (isEnglish) {
                                                    Alignment.Start
                                                } else {
                                                    Alignment.End
                                                },
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = groupName,
                                                    color = if (isSelected) {
                                                        Color(0xFF1E2A3D)
                                                    } else {
                                                        Color.White
                                                    },
                                                    style =
                                                        KmiTypography.cardTitle,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                    textAlign = screenTextAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Text(
                                                    text = coachBroadcastTr(
                                                        isEnglish,
                                                        "${availableBranchGroupCounts[groupName] ?: 0} מתאמנים",
                                                        "${availableBranchGroupCounts[groupName] ?: 0} trainees"
                                                    ),
                                                    color = if (isSelected) {
                                                        Color(0xFF0F5E9C)
                                                    } else {
                                                        Color(0xFFBAE6FD)
                                                    },
                                                    style = KmiTypography.caption,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                    textAlign = screenTextAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isSelected) {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "נבחר",
                                                            "Selected"
                                                        )
                                                    } else {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "בחר",
                                                            "Select"
                                                        )
                                                    },
                                                    color = if (isSelected) {
                                                        Color(0xFF16A34A)
                                                    } else {
                                                        Color(0xFFE0F2FE)
                                                    },
                                                    style =
                                                        KmiTypography.secondary,
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                    maxLines = 2
                                                )

                                                Surface(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable {
                                                            sendScope = "groups"

                                                            selectedTargetGroups =
                                                                if (isSelected) {
                                                                    selectedTargetGroups - groupName
                                                                } else {
                                                                    selectedTargetGroups + groupName
                                                                }

                                                            recipients = emptyList()
                                                            isRecipientsLoading = true
                                                        },
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                                    color = if (isSelected) {
                                                        Color(0xFF16A34A)
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    border = BorderStroke(
                                                        width = 1.4.dp,
                                                        color = if (isSelected) {
                                                            Color(0xFF22C55E)
                                                        } else {
                                                            Color(0xFF64748B)
                                                        }
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Text(
                                                                text = "✓",
                                                                color =
                                                                    Color.White,
                                                                style =
                                                                    KmiTypography.action,
                                                                fontWeight =
                                                                    androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                                                textAlign =
                                                                    TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = if (selectedTargetGroups.isEmpty()) {
                                    coachBroadcastTr(
                                        isEnglish,
                                        "סמן את הריבוע ליד הקבוצה כדי להציג את המתאמנים ולשלוח הודעה.",
                                        "Tick the group checkbox to show trainees and send a message."
                                    )
                                } else {
                                    coachBroadcastTr(
                                        isEnglish,
                                        "ההודעה תישלח רק למתאמנים בקבוצות שסומנו.",
                                        "The message will be sent only to trainees in the selected groups."
                                    )
                                },
                                color =
                                    if (
                                        selectedTargetGroups
                                            .isEmpty()
                                    ) {
                                        if (isDarkMode) {
                                            Color(0xFF7DD3FC)
                                        } else {
                                            Color(0xFF0F5E9C)
                                        }
                                    } else {
                                        secondaryContentColor
                                    },
                                style =
                                    KmiTypography.caption,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                textAlign = screenTextAlign,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // ===== רשימת נמענים מהקבוצה =====
                if (isRecipientsLoading) {
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            androidx.compose.foundation
                                .shape
                                .RoundedCornerShape(18.dp),
                        color = cardContainerColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color = cardBorderColor
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 18.dp
                                    ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            CoachBroadcastLoadingRings(
                                size = 64.dp
                            )

                            Text(
                                text =
                                    coachBroadcastTr(
                                        isEnglish,
                                        if (
                                            selectedTargetGroups
                                                .isEmpty()
                                        ) {
                                            "טוען קבוצות בסניף..."
                                        } else {
                                            "טוען מתאמנים בקבוצות שנבחרו..."
                                        },
                                        if (
                                            selectedTargetGroups
                                                .isEmpty()
                                        ) {
                                            "Loading branch groups..."
                                        } else {
                                            "Loading trainees in selected groups..."
                                        }
                                    ),
                                color = primaryContentColor,
                                style =
                                    KmiTypography.secondary,
                                fontWeight =
                                    androidx.compose.ui.text
                                        .font
                                        .FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (recipients.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val newValue = !allSelected
                                recipients = recipients.map { it.copy(selected = newValue) }
                            },
                            modifier =
                                Modifier.widthIn(
                                    min = 200.dp,
                                    max = 280.dp
                                ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF67E8F9)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF0B1220),
                                contentColor = Color(0xFFE0F2FE)
                            )
                        ) {
                            Text(
                                text =
                                    if (allSelected) {
                                        coachBroadcastTr(
                                            isEnglish,
                                            "בטל סימון לכולם",
                                            "Unselect all"
                                        )
                                    } else {
                                        coachBroadcastTr(
                                            isEnglish,
                                            "סמן את כל חברי הקבוצה",
                                            "Select all group members"
                                        )
                                    },
                                style =
                                    KmiTypography.action,
                                fontWeight =
                                    androidx.compose.ui.text.font.FontWeight.Bold,
                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = recipientListColor,
                                shape =
                                    androidx.compose.foundation
                                        .shape
                                        .RoundedCornerShape(18.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = cardBorderColor,
                                shape =
                                    androidx.compose.foundation
                                        .shape
                                        .RoundedCornerShape(18.dp)
                            )
                            .padding(8.dp),
                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                    ) {
                        uiRecipients.forEach { r ->
                            val isSelected = r.selected

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                color =
                                    if (isSelected) {
                                        selectedRecipientColor
                                    } else {
                                        Color.Transparent
                                    },
                                tonalElevation = 0.dp,
                                border = if (isSelected) {
                                    BorderStroke(
                                        1.dp,
                                        Color(0xFF7DD3FC)
                                    )
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
                                    ) {
                                        Text(
                                            text = r.name,
                                            color =
                                                if (isSelected) {
                                                    if (isDarkMode) {
                                                        Color(0xFFE0F2FE)
                                                    } else {
                                                        Color(0xFF0C4A6E)
                                                    }
                                                } else {
                                                    primaryContentColor
                                                },
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text =
                                                if (
                                                    demoPrivacyEnabled
                                                ) {
                                                    coachBroadcastTr(
                                                        isEnglish,
                                                        "מספר מוסתר",
                                                        "Hidden number"
                                                    )
                                                } else {
                                                    r.phone.ifBlank {
                                                        coachBroadcastTr(
                                                            isEnglish,
                                                            "ללא מספר טלפון",
                                                            "No phone number"
                                                        )
                                                    }
                                                },
                                            style =
                                                KmiTypography.caption,
                                            color = if (isSelected) Color(0xFF0369A1) else Color.Gray,
                                            textAlign = screenTextAlign,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Checkbox(
                                        checked = r.selected,
                                        onCheckedChange = { checked ->
                                            recipients = recipients.map {
                                                if (it.uid == r.uid) it.copy(selected = checked) else it
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (region.isNotBlank() && branch.isNotBlank()) {
                    Text(
                        text = coachBroadcastTr(
                            isEnglish,
                            if (selectedTargetGroups.isEmpty()) {
                                "לא נבחרו קבוצות לשליחה."
                            } else {
                                "לא נמצאו מתאמנים פעילים לקבוצות שנבחרו בסניף הזה."
                            },
                            if (selectedTargetGroups.isEmpty()) {
                                "No groups were selected for sending."
                            } else {
                                "No active trainees were found for the selected groups in this branch."
                            }
                        ),
                        color = Color.White,
                        textAlign = screenTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text =
                            coachBroadcastTr(
                                isEnglish,
                                "מתאמנים בקבוצות שנבחרו: ${recipients.size}",
                                "Trainees in selected groups: ${recipients.size}"
                            ),
                        color = Color.White,
                        style =
                            KmiTypography.cardTitle,
                        fontWeight =
                            androidx.compose.ui.text
                                .font
                                .FontWeight.Bold,
                        textAlign = screenTextAlign,
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Text(
                        text =
                            coachBroadcastTr(
                                isEnglish,
                                "מתאמנים נבחרים: ${selectedUids.size}",
                                "Selected trainees: ${selectedUids.size}"
                            ),
                        color = Color(0xFFE0F2FE),
                        style =
                            KmiTypography.cardTitle,
                        fontWeight =
                            androidx.compose.ui.text
                                .font
                                .FontWeight.Bold,
                        textAlign = screenTextAlign,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                // שליחה דרך SMS + שמירה + פידבק
                androidx.compose.material3.Button(
                    onClick = {
                        scope.launch {
                            if (isSending) {
                                return@launch
                            }

                            when {
                                message.isBlank() -> {
                                    snackbarHostState.showSnackbar(
                                        coachBroadcastTr(
                                            isEnglish,
                                            "נא לכתוב טקסט להודעה",
                                            "Please write a message"
                                        )
                                    )
                                }

                                selectedUids.isEmpty() -> {
                                    snackbarHostState.showSnackbar(
                                        coachBroadcastTr(
                                            isEnglish,
                                            "לא נבחרו נמענים – סמן לפחות מתאמן אחד",
                                            "No recipients selected — select at least one trainee"
                                        )
                                    )
                                }
                                else -> {
                                    isSending = true

                                    val messageToSend = message

                                    saveBroadcast()

                                    if (selectedNumbers.isNotEmpty()) {
                                        runCatching {
                                            onOpenSms(selectedNumbers, messageToSend)
                                        }.onSuccess {
                                            message = ""

                                            snackbarHostState.showSnackbar(
                                                coachBroadcastTr(
                                                    isEnglish,
                                                    "נפתחה אפליקציית ההודעות עם ${selectedNumbers.size} מתאמנים",
                                                    "The messaging app opened with ${selectedNumbers.size} trainees"
                                                )
                                            )
                                        }.onFailure {
                                            snackbarHostState.showSnackbar(
                                                coachBroadcastTr(
                                                    isEnglish,
                                                    "ההודעה נשמרה לעיבוד Push, אבל פתיחת אפליקציית ההודעות נכשלה.",
                                                    "The message was saved for Push processing, but opening the messaging app failed."
                                                )
                                            )
                                        }
                                    } else {
                                        message = ""

                                        snackbarHostState.showSnackbar(
                                            coachBroadcastTr(
                                                isEnglish,
                                                "ההודעה נשמרה לעיבוד Push עבור ${selectedUids.size} נמענים",
                                                "The message was saved for Push processing for ${selectedUids.size} recipients"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSending && message.isNotBlank() && selectedUids.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    elevation =
                        androidx.compose.material3
                            .ButtonDefaults
                            .buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                disabledElevation = 0.dp
                            ),
                    border = BorderStroke(1.dp, Color(0xFF67E8F9))
                ) {
                    if (isSending) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            CoachBroadcastLoadingRings(
                                size = 30.dp
                            )

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Text(
                                text =
                                    coachBroadcastTr(
                                        isEnglish,
                                        "שולח הודעה...",
                                        "Sending message..."
                                    ),
                                color = Color.White,
                                style =
                                    KmiTypography.action,
                                fontWeight =
                                    androidx.compose.ui.text
                                        .font
                                        .FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = sendButtonText,
                            color = Color(0xFFE0F2FE),
                            style = KmiTypography.action,
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 2,
                            textAlign =
                                TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(84.dp))
                }
            }
        }
    }
}