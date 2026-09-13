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
import il.yuval.ui.theme.kmiSectionHeaderBackground

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
            .height(56.dp)
            .kmiSectionHeaderBackground()
    ) {

        /*
         * קו מפריד במרכז —
         * כמו בשאר סרגלי הטאבים באפליקציה.
         */
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(1.dp)
                .height(28.dp)
                .background(
                    Color.White.copy(alpha = 0.65f)
                )
        )

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = Color.White,
            divider = {},
            indicator = { tabPositions ->

                val pos =
                    tabPositions[selectedTab]

                /*
                 * קו הבחירה הקצר —
                 * כמו במסך רשימת המתאמנים.
                 */
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(pos)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(3.dp)
                            .background(Color.White)
                    )
                }
            }
        ) {

            // -------- מתאמן --------
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    if (traineeEnabled) {
                        onTabSelected(0)
                    }
                },
                text = {
                    Text(
                        text = "מתאמן",
                        fontWeight =
                            if (selectedTab == 0) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.Bold
                            }
                    )
                },
                selectedContentColor =
                    Color.White,
                unselectedContentColor =
                    when {
                        traineeEnabled ->
                            Color.White.copy(
                                alpha = 0.90f
                            )

                        else ->
                            Color.White.copy(
                                alpha = disabledAlpha
                            )
                    }
            )

            // -------- מאמן --------
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    if (coachEnabled) {
                        onTabSelected(1)
                    }
                },
                text = {
                    Text(
                        text = "מאמן",
                        fontWeight =
                            if (selectedTab == 1) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.Bold
                            }
                    )
                },
                selectedContentColor =
                    Color.White,
                unselectedContentColor =
                    when {
                        coachEnabled ->
                            Color.White.copy(
                                alpha = 0.90f
                            )

                        else ->
                            Color.White.copy(
                                alpha = disabledAlpha
                            )
                    }
            )
        }
    }
}