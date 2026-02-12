package il.kmi.app.domain

import androidx.compose.ui.graphics.Color
import il.kmi.shared.domain.Belt

val Belt.color: Color
    get() = Color(this.colorArgb)

val Belt.lightColor: Color
    get() = Color(this.lightColorArgb)
