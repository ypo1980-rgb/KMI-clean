package il.kmi.app.subscription

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    onBack: () -> Unit,
    onPurchase: (productId: String) -> Unit,
    onOpenHome: () -> Unit,
    onOpenAssociationMembership: () -> Unit,
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
    else androidx.compose.ui.unit.LayoutDirection.Rtl

    val userSp = remember {
        ctx.getSharedPreferences("kmi_user", android.content.Context.MODE_PRIVATE)
    }
    val isAssociationMember = remember {
        userSp.getBoolean("is_association_member", false)
    }

    val monthlyProductId = remember(isAssociationMember) {
        SubscriptionResolver.resolveMonthlyProduct(isAssociationMember)
    }
    val yearlyProductId = remember(isAssociationMember) {
        SubscriptionResolver.resolveYearlyProduct(isAssociationMember)
    }

    // Billing – חיבור לשירות
    val repo = remember { BillingRepository(ctx) }
    LaunchedEffect(Unit) { repo.startConnection() }
    val state by repo.state.collectAsState()

    val monthlyPriceText = remember(state, monthlyProductId) {
        repo.getPriceForProduct(monthlyProductId)
            ?: if (isEnglish) "Price will appear soon" else "המחיר יופיע בקרוב"
    }

    val yearlyPriceText = remember(state, yearlyProductId) {
        repo.getPriceForProduct(yearlyProductId)
            ?: if (isEnglish) "Price will appear soon" else "המחיר יופיע בקרוב"
    }

    Scaffold(
        topBar = {
            // כותרת יחידה – בלי חץ חזור (החזרה תתבצע מהכפתור למטה / back של המכשיר)
            il.kmi.app.ui.KmiTopBar(
                title = if (isEnglish) "Subscription plans" else "תוכניות מנוי",
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
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isAssociationMember) {
                        if (isEnglish) "Association member pricing detected" else "זוהתה זכאות למחיר חבר עמותה"
                    } else {
                        if (isEnglish) "Choose the plan that fits you:" else "בחר/י במסלול המתאים לך:"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

// ---------- Free Trial Info ----------
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF3C7)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text =
                            if (isEnglish)
                                "⭐ 3-day free trial. Billing is managed by Google Play / App Store. You can cancel anytime."
                            else
                                "⭐ ניסיון חינם ל-3 ימים. החיוב מתבצע דרך Google Play / App Store וניתן לבטל בכל עת.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                TariffCard()
                if (!isAssociationMember) {
                    Spacer(Modifier.height(14.dp))

                    JoinAssociationCard(
                        onClick = onOpenAssociationMembership
                    )
                }

                Spacer(Modifier.height(16.dp))

                // -------- מנוי חודשי --------
                PlanCard(
                    title = if (isAssociationMember) {
                        if (isEnglish) {
                            "Monthly plan for association members\n(full access to all content)"
                        } else {
                            "מנוי חודשי לחבר עמותה\n(גישה מלאה לכל התכנים)"
                        }
                    } else {
                        if (isEnglish) {
                            "Recurring monthly subscription\n(full access to all content)"
                        } else {
                            "מנוי חודשי מתחדש\n(גישה מלאה לכל התכנים)"
                        }
                    },
                    priceLine = if (isEnglish) {
                        "$monthlyPriceText / month"
                    } else {
                        "$monthlyPriceText / חודשי"
                    },
                    points = listOf(
                        if (isEnglish) "Full access to all app content" else "גישה מלאה לכל התכנים באפליקציה",
                        if (isEnglish) "Renews automatically every month" else "מתחדש אוטומטית מדי חודש",
                        if (isAssociationMember) {
                            if (isEnglish) "Includes discounted member pricing" else "כולל מחיר מוזל לחבר עמותה"
                        } else {
                            if (isEnglish) "Can be canceled anytime under store policy" else "ניתן לבטל בכל עת בהתאם למדיניות החנות"
                        }
                    ),
                    containerColor = Color(0xFF0EA5E9),
                    showTrialBadge = true,
                    onBuy = {
                        if (activity != null && state.connected) {
                            repo.launchPurchase(activity, monthlyProductId)
                            onPurchase(monthlyProductId)
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
                    title = if (isAssociationMember) {
                        if (isEnglish) {
                            "Yearly plan for association members\n(full access to all content)"
                        } else {
                            "מנוי שנתי לחבר עמותה\n(גישה מלאה לכל התכנים)"
                        }
                    } else {
                        if (isEnglish) {
                            "Recurring yearly subscription\n(full access to all content)"
                        } else {
                            "מנוי שנתי\n(גישה מלאה לכל התכנים)"
                        }
                    },
                    priceLine = if (isEnglish) {
                        "$yearlyPriceText / year"
                    } else {
                        "$yearlyPriceText / שנתי"
                    },
                    points = listOf(
                        if (isEnglish) "One yearly payment" else "תשלום חד־שנתי אחד",
                        if (isEnglish) "No monthly renewal" else "ללא חידוש חודשי",
                        if (isAssociationMember) {
                            if (isEnglish) "Includes discounted member pricing" else "כולל מחיר מוזל לחבר עמותה"
                        } else {
                            if (isEnglish) "Access to all content for the full year" else "גישה לכל התכנים לאורך כל השנה"
                        }
                    ),
                    containerColor = Color(0xFFFFA000),
                    onBuy = {
                        if (activity != null && state.connected) {
                            repo.launchPurchase(activity, yearlyProductId)
                            onPurchase(yearlyProductId)
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
                    Text(if (isEnglish) "Back to subscription screen" else "חזרה למסך ניהול המנוי")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TariffCard() {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH
    val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
    else androidx.compose.ui.unit.LayoutDirection.Rtl
    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val yearlySaving = 250 - 200

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF111827)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = horizontalAlign
            ) {
                Text(
                    text = if (isEnglish) "App pricing" else "תעריפון האפליקציה",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isEnglish) {
                        "Price comparison between regular users and K.M.I. association members"
                    } else {
                        "השוואת מחירים בין משתמש רגיל לבין חבר עמותת ק.מ.י"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PremiumTariffRow(
                            label = if (isEnglish) "User type" else "סוג משתמש",
                            monthly = if (isEnglish) "Monthly" else "חודשי",
                            yearly = if (isEnglish) "Yearly" else "שנתי",
                            isHeader = true
                        )

                        PremiumTariffDivider()

                        PremiumTariffRow(
                            label = if (isEnglish) "Regular user" else "משתמש רגיל",
                            monthly = "₪25",
                            yearly = "₪250"
                        )

                        PremiumTariffDivider()

                        PremiumTariffRow(
                            label = if (isEnglish) "K.M.I. member" else "חבר עמותת ק.מ.י",
                            monthly = "₪20",
                            yearly = "₪200",
                            highlight = true
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF065F46).copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "K.M.I. members save ₪$yearlySaving per year"
                            } else {
                                "חבר עמותת ק.מ.י חוסך ₪$yearlySaving בשנה"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF86EFAC),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = if (isEnglish) {
                                "Member pricing will be applied after membership verification."
                            } else {
                                "הנחת חבר עמותה תינתן לאחר אימות סטטוס החברות."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumTariffRow(
    label: String,
    monthly: String,
    yearly: String,
    isHeader: Boolean = false,
    highlight: Boolean = false
) {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH
    val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
    else androidx.compose.ui.unit.LayoutDirection.Rtl
    val labelAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    val textColor = when {
        isHeader -> Color.White
        highlight -> Color(0xFF86EFAC)
        else -> Color.White.copy(alpha = 0.96f)
    }

    val fontWeight = if (isHeader) FontWeight.ExtraBold else FontWeight.SemiBold

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = textColor,
                fontWeight = fontWeight,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1.6f),
                textAlign = labelAlign
            )

            Text(
                text = monthly,
                color = textColor,
                fontWeight = fontWeight,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Text(
                text = yearly,
                color = textColor,
                fontWeight = fontWeight,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PremiumTariffDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.10f))
    )
}

@Composable
private fun JoinAssociationCard(
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH
    val layoutDirection = if (isEnglish) androidx.compose.ui.unit.LayoutDirection.Ltr
    else androidx.compose.ui.unit.LayoutDirection.Rtl
    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = horizontalAlign
            ) {
                Text(
                    text = if (isEnglish) {
                        "Join the K.M.I. association"
                    } else {
                        "הצטרפות לעמותת ק.מ.י"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign
                )

                Text(
                    text = if (isEnglish) {
                        "Join now and enjoy discounted pricing in the app."
                    } else {
                        "הצטרף עכשיו לעמותה ותוכל ליהנות ממחיר מוזל באפליקציה."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = textAlign
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFECFDF5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEnglish) {
                            "Association members get discounted pricing: ₪20 / month • ₪200 / year"
                        } else {
                            "חבר עמותה נהנה ממחיר מוזל: ₪20 לחודש / ₪200 לשנה"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF065F46),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Join the association" else "להצטרפות לעמותה",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
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
    showTrialBadge: Boolean = false,
    onBuy: () -> Unit
) {
    val ctx = LocalContext.current
    val langManager = remember { il.kmi.shared.localization.AppLanguageManager(ctx) }
    val isEnglish = langManager.getCurrentLanguage() ==
            il.kmi.shared.localization.AppLanguage.ENGLISH

    val layoutDirection = if (isEnglish) {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    } else {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    }

    val horizontalAlign = if (isEnglish) Alignment.Start else Alignment.End
    val pointTextAlign = if (isEnglish) TextAlign.Left else TextAlign.Right

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = horizontalAlign,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                if (showTrialBadge) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isEnglish)
                                "⭐ 3-DAY FREE TRIAL"
                            else
                                "⭐ ניסיון חינם ל-3 ימים",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
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
                    horizontalAlignment = horizontalAlign,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    points.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEnglish) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.95f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = pointTextAlign,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = pointTextAlign,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.95f)
                                )
                            }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Secure purchase" else "רכישה מאובטחת",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
