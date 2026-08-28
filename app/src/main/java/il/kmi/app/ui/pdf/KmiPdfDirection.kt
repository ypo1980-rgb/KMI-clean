package il.kmi.app.ui.pdf

import android.graphics.Paint

/**
 * כלי כיוון ויישור משותפים לכל קובצי ה־PDF.
 *
 * אין תלות ב־Compose ולכן ניתן להשתמש במחלקה
 * בכל פונקציית יצירת PDF באפליקציה.
 */
object KmiPdfDirection {

    fun textAlign(
        isEnglish: Boolean
    ): Paint.Align =
        if (isEnglish) {
            Paint.Align.LEFT
        } else {
            Paint.Align.RIGHT
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
}