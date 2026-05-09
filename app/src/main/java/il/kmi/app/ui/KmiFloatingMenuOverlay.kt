package il.kmi.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.FitnessCenter
import il.kmi.shared.domain.Belt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance


private fun floatingMenuColorForBelt(belt: Belt): Color {
    return when (belt) {
        Belt.WHITE  -> Color(0xFFF4F4F4)
        Belt.YELLOW -> Color(0xFFF2C94C)
        Belt.ORANGE -> Color(0xFFF2994A)
        Belt.GREEN  -> Color(0xFF5DBB63)
        Belt.BLUE   -> Color(0xFF4A90E2)
        Belt.BROWN  -> Color(0xFF8D6E63)
        Belt.BLACK  -> Color(0xFF2E2E2E)
    }
}

private fun floatingMenuContentColorForBelt(belt: Belt): Color {
    return when (belt) {
        Belt.WHITE,
        Belt.YELLOW,
        Belt.ORANGE -> Color(0xFF102040)

        Belt.GREEN,
        Belt.BLUE,
        Belt.BROWN,
        Belt.BLACK -> Color.White
    }
}

@Composable
fun KmiFloatingMenuOverlay(
    effectiveBelt: Belt,
    canUseExtras: Boolean,
    onOpenWeakPoints: () -> Unit,
    onOpenLists: (Belt) -> Unit,
    onOpenPracticeMenu: () -> Unit,
    onOpenSummary: (Belt) -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenPdf: (Belt) -> Unit,
    onHaptic: () -> Unit,
    onClickSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    val menuColor = floatingMenuColorForBelt(effectiveBelt)
    val menuContentColor = floatingMenuContentColorForBelt(effectiveBelt)

    // ✅ Overlay שמאפשר align + padding אחיד בכל מסך
    Box(modifier = modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp + bottomInset)
        ) {
            // ✅ לא משתמשים ב-modifier של KmiSpeedDialFab כדי שלא תהיה תלות
            KmiSpeedDialFab(
                actions = listOf(
                    KmiFabAction(
                        text = "נקודות תורפה",
                        icon = Icons.Filled.Warning,
                        enabled = true,
                        onClick = { onOpenWeakPoints() }
                    ),
                    KmiFabAction(
                        text = "כל הרשימות",
                        icon = Icons.Filled.List,
                        enabled = (canUseExtras && effectiveBelt != Belt.WHITE),
                        onClick = { onOpenLists(effectiveBelt) }
                    ),
                    KmiFabAction(
                        text = "תרגול",
                        icon = Icons.Filled.FitnessCenter,
                        enabled = canUseExtras,
                        onClick = { onOpenPracticeMenu() }
                    ),
                    KmiFabAction(
                        text = "מסך סיכום",
                        icon = Icons.Filled.Summarize,
                        enabled = canUseExtras,
                        onClick = { onOpenSummary(effectiveBelt) }
                    ),
                    KmiFabAction(
                        text = "עוזר קולי",
                        icon = Icons.Filled.Mic,
                        enabled = canUseExtras,
                        onClick = { onOpenAssistant() }
                    )
                ),
                scrimColor = menuColor.copy(alpha = 0.18f),
                mainFabColor = menuColor,
                actionIconTint = menuContentColor,
                onHaptic = { onHaptic() },
                onClickSound = { onClickSound() }
            )
        }
    }
}
