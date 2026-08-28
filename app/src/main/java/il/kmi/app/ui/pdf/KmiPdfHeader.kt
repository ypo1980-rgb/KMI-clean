package il.kmi.app.ui.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import il.kmi.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * כותרת גלובלית אחידה לכל קובצי ה־PDF באפליקציה.
 *
 * הקובץ מרכז:
 * - רקע כחול קבוע.
 * - פסי עיצוב אלכסוניים.
 * - לוגו KAMI.
 * - כותרת וכותרת משנה.
 * - תאריך הפקה.
 * - תמיכה בעברית ובאנגלית.
 *
 * שינוי בקובץ הזה ישפיע על כל דוחות ה־PDF
 * שמשתמשים ב־KmiPdfHeader.draw().
 */
object KmiPdfHeader {

    const val HEADER_BOTTOM = 122f
    const val CONTENT_TOP = 146f

    private const val RIGHT_MARGIN = 34f
    private const val ENGLISH_TEXT_LEFT = 308f

    private val navy =
        Color.rgb(2, 43, 74)

    private val mediumBlue =
        Color.rgb(36, 103, 158)

    private val lightHeaderBlue =
        Color.rgb(128, 183, 220)

    fun draw(
        context: Context,
        canvas: Canvas,
        pageWidth: Int,
        isEnglish: Boolean,
        titleHebrew: String,
        titleEnglish: String,
        subtitleHebrew: String,
        subtitleEnglish: String,
        generatedDate: String = currentDate()
    ) {
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

        canvas.drawColor(Color.WHITE)

        drawBackground(
            canvas = canvas,
            pageWidth = pageWidth
        )

        drawOfficialLogo(
            context = context,
            canvas = canvas
        )

        val textAlignment =
            KmiPdfDirection.textAlign(
                isEnglish = isEnglish
            )

        val headerTextX =
            KmiPdfDirection.startX(
                isEnglish = isEnglish,
                left = ENGLISH_TEXT_LEFT,
                right = pageWidth - RIGHT_MARGIN
            )

        val titlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = boldTypeface
                textSize = 29f
                textAlign = textAlignment
            }

        val subtitlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = regularTypeface
                textSize = 14f
                textAlign = textAlignment
            }

        val datePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    Color.WHITE
                alpha = 230
                typeface = regularTypeface
                textSize = 9f
                textAlign = textAlignment
            }

        canvas.drawText(
            if (isEnglish) {
                titleEnglish
            } else {
                titleHebrew
            },
            headerTextX,
            47f,
            titlePaint
        )

        canvas.drawText(
            if (isEnglish) {
                subtitleEnglish
            } else {
                subtitleHebrew
            },
            headerTextX,
            72f,
            subtitlePaint
        )

        canvas.drawText(
            if (isEnglish) {
                "Generated: $generatedDate"
            } else {
                "תאריך הפקה: $generatedDate"
            },
            headerTextX,
            94f,
            datePaint
        )
    }

    private fun drawBackground(
        canvas: Canvas,
        pageWidth: Int
    ) {
        canvas.drawPath(
            Path().apply {
                moveTo(pageWidth.toFloat(), 0f)
                lineTo(
                    pageWidth.toFloat(),
                    HEADER_BOTTOM
                )
                lineTo(178f, HEADER_BOTTOM)
                lineTo(238f, 0f)
                close()
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                style = Paint.Style.FILL
            }
        )

        canvas.drawPath(
            Path().apply {
                moveTo(208f, HEADER_BOTTOM)
                lineTo(224f, HEADER_BOTTOM)
                lineTo(284f, 0f)
                lineTo(268f, 0f)
                close()
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mediumBlue
                style = Paint.Style.FILL
            }
        )

        canvas.drawPath(
            Path().apply {
                moveTo(230f, HEADER_BOTTOM)
                lineTo(238f, HEADER_BOTTOM)
                lineTo(298f, 0f)
                lineTo(290f, 0f)
                close()
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lightHeaderBlue
                style = Paint.Style.FILL
            }
        )
    }

    private fun drawOfficialLogo(
        context: Context,
        canvas: Canvas
    ) {
        val logoBitmap =
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.kami_logo
            ) ?: return

        val destination =
            RectF(
                25f,
                8f,
                131f,
                114f
            )

        val bitmapPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                isDither = true
            }

        canvas.drawBitmap(
            logoBitmap,
            null,
            destination,
            bitmapPaint
        )

        logoBitmap.recycle()
    }

    private fun currentDate(): String {
        return SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())
    }
}