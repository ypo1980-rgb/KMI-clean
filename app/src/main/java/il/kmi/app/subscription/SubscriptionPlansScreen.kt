package il.kmi.app.subscription

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    onBack: () -> Unit,
    onPurchase: (productId: String) -> Unit,
    onOpenHome: () -> Unit,
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    // Billing – חיבור לשירות
    val repo = remember { BillingRepository(ctx) }
    LaunchedEffect(Unit) { repo.startConnection() }
    val state by repo.state.collectAsState()

    Scaffold(
        topBar = {
            // כותרת יחידה – בלי חץ חזור (החזרה תתבצע מהכפתור למטה / back של המכשיר)
            il.kmi.app.ui.KmiTopBar(
                title = "תוכניות מנוי",
                onHome = onOpenHome,
                showTopHome = false,
                showTopSearch = false,
                showBottomActions = true,
                lockSearch = true,
                centerTitle = true,
                extraActions = { }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "בחר/י במסלול המתאים לך:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // -------- מנוי חודשי --------
                PlanCard(
                    title = "מנוי חודשי מתחדש\n(גישה מלאה לכל התכנים)",
                    priceLine = "₪25 / חודשי",
                    points = listOf(
                        "גישה מלאה לכל התכנים באפליקציה",
                        "מתחדש אוטומטית מדי חודש",
                        "ניתן לבטל בכל עת בהתאם למדיניות החנות"
                    ),
                    containerColor = Color(0xFF0EA5E9),
                    onBuy = {
                        if (activity != null && state.connected) {
                            repo.launchPurchase(activity)
                            onPurchase("monthly_full")
                        } else {
                            Toast.makeText(
                                ctx,
                                "שירות הרכישה אינו זמין במכשיר (Billing not connected).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )

                // -------- מנוי שנתי --------
                PlanCard(
                    title = "גישה מלאה לכל התכנים",
                    priceLine = "₪200 / שנתי",
                    points = listOf(
                        "תשלום חד־שנתי אחד",
                        "ללא חידוש חודשי",
                        "גישה לכל התכנים לאורך כל השנה"
                    ),
                    containerColor = Color(0xFFFFA000),
                    onBuy = {
                        if (activity != null && state.connected) {
                            repo.launchPurchase(activity)
                            onPurchase("yearly_full")
                        } else {
                            Toast.makeText(
                                ctx,
                                "שירות הרכישה אינו זמין במכשיר (Billing not connected).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )

                // -------- מנוי לפי חגורה (אם תרצה להשתמש במוצר כזה) --------
                PlanCard(
                    title = "מנוי לפי חגורה",
                    priceLine = "מחיר לפי רמת החגורה",
                    points = listOf(
                        "גישה לתכנים של חגורה אחת לבחירה",
                        "אפשרות שדרוג למנוי מלא בהמשך"
                    ),
                    containerColor = Color(0xFF8E24AA),
                    onBuy = {
                        if (activity != null && state.connected) {
                            repo.launchPurchase(activity)
                            onPurchase("per_belt")
                        } else {
                            Toast.makeText(
                                ctx,
                                "שירות הרכישה אינו זמין במכשיר (Billing not connected).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("חזרה למסך ניהול המנוי")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    priceLine: String,
    points: List<String>,
    containerColor: Color,
    onBuy: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = priceLine,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                points.forEach { line ->
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.95f)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = onBuy,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = containerColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "רכישה מאובטחת",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
