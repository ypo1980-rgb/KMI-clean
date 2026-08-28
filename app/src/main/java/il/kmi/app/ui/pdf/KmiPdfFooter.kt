package il.kmi.app.ui.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * תחתית גלובלית אחידה לכל קובצי ה־PDF באפליקציה.
 *
 * כוללת:
 * - קו הפרדה עליון.
 * - לוגו KAMI קטן.
 * - הסלוגן.
 * - מספר העמוד.
 * - שם הארגון והאתר.
 * - שלושת פסי המותג בצד ימין.
 */
object KmiPdfFooter {

    const val FOOTER_HEIGHT = 38f
    const val CONTENT_BOTTOM_PADDING = 58f

    private val navy =
        Color.rgb(2, 43, 74)

    private val mediumBlue =
        Color.rgb(36, 103, 158)

    private val lightBlue =
        Color.rgb(128, 183, 220)

    private val mutedText =
        Color.rgb(80, 100, 120)

    fun draw(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int,
        totalPages: Int? = null,
        isEnglish: Boolean
    ) {
        val footerTop =
            pageHeight - FOOTER_HEIGHT

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

        val dividerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }

        canvas.drawLine(
            0f,
            footerTop,
            pageWidth.toFloat(),
            footerTop,
            dividerPaint
        )

        drawLogo(
            canvas = canvas,
            cx = 38f,
            cy = footerTop + 20f,
            radius = 12f,
            boldTypeface = boldTypeface
        )

        val footerTextPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedText
                typeface = regularTypeface
                textSize = 9f
            }

        footerTextPaint.textAlign =
            Paint.Align.LEFT

        canvas.drawText(
            "Together We Protect",
            58f,
            footerTop + 23f,
            footerTextPaint
        )

        footerTextPaint.textAlign =
            Paint.Align.CENTER

        canvas.drawText(
            if (isEnglish) {
                if (totalPages != null) {
                    "Page $pageNumber of $totalPages"
                } else {
                    "Page $pageNumber"
                }
            } else {
                if (totalPages != null) {
                    "עמוד $pageNumber מתוך $totalPages"
                } else {
                    "עמוד $pageNumber"
                }
            },
            pageWidth / 2f,
            footerTop + 23f,
            footerTextPaint
        )

        footerTextPaint.textAlign =
            Paint.Align.RIGHT

        canvas.drawText(
            "Krav Magen Israel",
            pageWidth - 66f,
            footerTop + 17f,
            footerTextPaint
        )

        canvas.drawText(
            "www.kami.org.il",
            pageWidth - 66f,
            footerTop + 29f,
            footerTextPaint
        )

        drawBrandLines(
            canvas = canvas,
            pageWidth = pageWidth,
            footerTop = footerTop
        )
    }

    private fun drawLogo(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        boldTypeface: Typeface
    ) {
        val outerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                style = Paint.Style.FILL
            }

        val innerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

        val logoTextPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                typeface = boldTypeface
                textSize = radius * 0.44f
                textAlign = Paint.Align.LEFT
            }

        canvas.drawCircle(
            cx,
            cy,
            radius,
            outerPaint
        )

        canvas.drawCircle(
            cx,
            cy,
            radius - 2.5f,
            innerPaint
        )

        val logoText = "KAMI"
        val letterSpacing = radius * 0.055f

        val lettersWidth =
            logoText.sumOf { character ->
                logoTextPaint
                    .measureText(character.toString())
                    .toDouble()
            }.toFloat()

        val completeTextWidth =
            lettersWidth +
                    letterSpacing *
                    (logoText.length - 1)

        var letterX =
            cx - completeTextWidth / 2f

        logoText.forEach { character ->
            val characterText =
                character.toString()

            canvas.drawText(
                characterText,
                letterX,
                cy + radius * 0.21f,
                logoTextPaint
            )

            letterX +=
                logoTextPaint.measureText(characterText) +
                        letterSpacing
        }
    }

    private fun drawBrandLines(
        canvas: Canvas,
        pageWidth: Int,
        footerTop: Float
    ) {
        val lineStartX =
            pageWidth - 42f

        val lineEndX =
            pageWidth - 18f

        val firstLinePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
            }

        val secondLinePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mediumBlue
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
            }

        val thirdLinePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lightBlue
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
            }

        canvas.drawLine(
            lineStartX,
            footerTop + 15f,
            lineEndX,
            footerTop + 15f,
            firstLinePaint
        )

        canvas.drawLine(
            lineStartX,
            footerTop + 21f,
            lineEndX,
            footerTop + 21f,
            secondLinePaint
        )

        canvas.drawLine(
            lineStartX,
            footerTop + 27f,
            lineEndX,
            footerTop + 27f,
            thirdLinePaint
        )
    }
}