package il.kmi.app.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

data class OnboardingStep(
    val id: String,
    val titleHe: String,
    val titleEn: String,
    val descriptionHe: String,
    val descriptionEn: String,
    @DrawableRes val imageRes: Int? = null,
    val accentColor: Color = Color(0xFF6D4ED8)
) {
    fun title(isEnglish: Boolean): String {
        return if (isEnglish) titleEn else titleHe
    }

    fun description(isEnglish: Boolean): String {
        return if (isEnglish) descriptionEn else descriptionHe
    }
}