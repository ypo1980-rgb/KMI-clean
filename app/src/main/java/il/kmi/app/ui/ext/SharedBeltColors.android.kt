package il.kmi.app.ui.ext

import androidx.compose.ui.graphics.Color
import il.kmi.shared.domain.Belt as SharedBelt

// Extensions ל-Belt מה-shared (כדי שתוכל להשתמש ב-belt.color / belt.lightColor במסכים)
val SharedBelt.color: Color get() = Color(this.colorArgb)
val SharedBelt.lightColor: Color get() = Color(this.lightColorArgb)
