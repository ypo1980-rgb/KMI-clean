package il.kmi.app.ui

import androidx.compose.ui.graphics.Color
import il.kmi.shared.domain.Belt

/**
 * UI-only extensions for shared Belt (Android Compose).
 * Keep this in app (not in shared).
 */
val Belt.color: Color
    get() = Color(colorArgb)

val Belt.lightColor: Color
    get() = Color(lightColorArgb)
