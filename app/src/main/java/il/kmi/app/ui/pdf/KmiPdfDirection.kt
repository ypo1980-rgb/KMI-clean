package il.kmi.app.ui.pdf

import android.graphics.Paint
import android.text.Layout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics

/**
 * כלי כיוון ויישור משותפים לכל קובצי ה־PDF.
 *
 * אין תלות ב־Compose ולכן ניתן להשתמש במחלקה
 * בכל פונקציית יצירת PDF באפליקציה.
 */
object KmiPdfDirection {

    /**
     * יישור לתחילת השורה:
     * שמאל באנגלית וימין בעברית.
     */
    fun textAlign(
        isEnglish: Boolean
    ): Paint.Align =
        if (isEnglish) {
            Paint.Align.LEFT
        } else {
            Paint.Align.RIGHT
        }

    /**
     * יישור לסוף השורה:
     * ימין באנגלית ושמאל בעברית.
     */
    fun endTextAlign(
        isEnglish: Boolean
    ): Paint.Align =
        if (isEnglish) {
            Paint.Align.RIGHT
        } else {
            Paint.Align.LEFT
        }

    fun startX(
        isEnglish: Boolean,
        left: Float,
        right: Float
    ): Float =
        if (isEnglish) {
            left
        } else {
            right
        }

    fun endX(
        isEnglish: Boolean,
        left: Float,
        right: Float
    ): Float =
        if (isEnglish) {
            right
        } else {
            left
        }

    fun startPaddingX(
        isEnglish: Boolean,
        left: Float,
        right: Float,
        padding: Float
    ): Float =
        if (isEnglish) {
            left + padding
        } else {
            right - padding
        }

    fun endPaddingX(
        isEnglish: Boolean,
        left: Float,
        right: Float,
        padding: Float
    ): Float =
        if (isEnglish) {
            right - padding
        } else {
            left + padding
        }

    /**
     * יישור לתחילת השורה בהתאם לכיוון הפסקה.
     *
     * כאשר TextDirection הוא RTL, תחילת השורה נמצאת
     * בצד ימין. כאשר הוא LTR, היא נמצאת בצד שמאל.
     */
    @Suppress("UNUSED_PARAMETER")
    fun layoutAlignment(
        isEnglish: Boolean
    ): Layout.Alignment =
        Layout.Alignment.ALIGN_NORMAL

    /**
     * כיוון הטקסט עבור StaticLayout.
     */
    fun textDirection(
        isEnglish: Boolean
    ): TextDirectionHeuristic =
        if (isEnglish) {
            TextDirectionHeuristics.LTR
        } else {
            TextDirectionHeuristics.RTL
        }
}