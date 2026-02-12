package il.kmi.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

// Local שדרכו ניגשים ל-DrawerState (אם משתמשים בו במסכים)
val LocalAppDrawerState = staticCompositionLocalOf<DrawerState?> { null }

// כפתור מוכן לשימוש בטופ-בר לפתיחת המגירה
@Composable
fun MenuIconButton(
    modifier: Modifier = Modifier,
    contentDescription: String? = "תפריט"
) {
    val drawer = LocalAppDrawerState.current
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = { scope.launch { drawer?.open() } },
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Filled.Menu, contentDescription = contentDescription)
    }
}

/* -----------------------------------------------------------
   DrawerBridge המאוחד:
   - MainApp רושם פונקציות לפתיחת המגירה/הגדרות/בית.
   - מסכים קוראים DrawerBridge.open()/openSettings()/openHome().
----------------------------------------------------------- */
object DrawerBridge {
    private var openDrawer:   (() -> Unit)? = null
    private var openSettings: (() -> Unit)? = null
    private var openHome:     (() -> Unit)? = null

    fun register(
        onOpenDrawer: () -> Unit,
        onOpenSettings: () -> Unit,
        onOpenHome: () -> Unit
    ) {
        openDrawer   = onOpenDrawer
        openSettings = onOpenSettings
        openHome     = onOpenHome
    }

    fun clear() {
        openDrawer   = null
        openSettings = null
        openHome     = null
    }

    fun open()         { openDrawer?.invoke() }
    fun openSettings() { openSettings?.invoke() }
    fun openHome()     { openHome?.invoke() }
}
