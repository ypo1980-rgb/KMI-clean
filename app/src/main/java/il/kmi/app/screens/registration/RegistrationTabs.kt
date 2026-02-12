@file:OptIn(ExperimentalMaterial3Api::class)

package il.kmi.app.screens.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * קומפוננטה של הטאבים למעלה במסך ההרשמה.
 * 0 = מתאמן, 1 = מאמן
 *
 * זו העתקה 1:1 מהקוד שהיה במסך הגדול, רק הוצאנו אותו החוצה.
 */
@Composable
fun RegistrationTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lockToCoach: Boolean = false,    // ✅ חדש: אם true → רק מאמן (טאב 1)
    lockToTrainee: Boolean = false   // ✅ חדש: אם true → רק מתאמן (טאב 0)
) {
    val traineeEnabled = !lockToCoach
    val coachEnabled = !lockToTrainee

    // צבעים “מושבתים” עדינים
    val disabledAlpha = 0.28f
    val unselectedAlpha = 0.70f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.10f)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { tabPositions ->
                        val pos = tabPositions[selectedTab]
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(pos),
                            height = 3.dp,
                            color = Color.White
                        )
                    }
                ) {
                    // -------- מתאמן --------
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            if (traineeEnabled) onTabSelected(0)
                        },
                        text = { Text("מתאמן", fontWeight = FontWeight.Bold) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = when {
                            traineeEnabled -> Color.White.copy(alpha = unselectedAlpha)
                            else -> Color.White.copy(alpha = disabledAlpha)
                        }
                    )

                    // -------- מאמן --------
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            if (coachEnabled) onTabSelected(1)
                        },
                        text = { Text("מאמן", fontWeight = FontWeight.Bold) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = when {
                            coachEnabled -> Color.White.copy(alpha = unselectedAlpha)
                            else -> Color.White.copy(alpha = disabledAlpha)
                        }
                    )
                }

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .padding(bottom = 6.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.95f))
                )
            }
        }
    }
}
