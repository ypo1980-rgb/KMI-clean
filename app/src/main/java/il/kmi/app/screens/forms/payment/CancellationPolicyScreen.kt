package il.kmi.app.screens.forms.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CancellationPolicyScreen(
    isEnglish: Boolean = false,
    onBack: () -> Unit = {}
) {

    val title = if (isEnglish)
        "Cancellation & Refund Policy"
    else
        "מדיניות ביטולים והחזרים"

    val policyText = if (isEnglish) {

        """
Cancellation and Refund Policy

Below is the cancellation and refund policy of K.M.I – Israeli Krav Magen Association, founded by Eli Avikzer.

Since the payment you are about to make is for association membership fees, once the payment is approved the transaction cannot be cancelled and no refund will be provided.

However, in cases of accidental double payment or another good-faith mistake, the association will review the case and, if confirmed as a genuine mistake, a refund may be granted.

Requests for refunds must be sent by email to:

management@kami.org.il

Requests sent through any other method will not be considered a valid cancellation notice.

The request must be sent within 14 days from the date the payment was made on the website, not from the credit card billing date.

The request must include the trainee's details, the payer’s details, and a clear explanation of the reason for the cancellation.

The association reserves the exclusive right to determine the method of refund.

By completing the payment, I confirm that I have read and agree to this cancellation and refund policy.
        """.trimIndent()

    } else {

        """
מדיניות ביטולים והחזרים

להלן תפורט מדיניות הביטולים וההחזרים של עמותת ק.מ.י – העמותה לקרב מגן ישראלי – מיסודו של אלי אביקזר.

מאחר והתשלום שאת/ה עומד/ת לבצע הינו עבור דמי חבר בעמותה, לאחר אישור הפעולה לא יתאפשר ביטול עסקה ולא יתאפשר החזר.

עם זאת, במקרה של תשלום כפול בטעות או במקרה של טעות אחרת בתום לב תבחן העמותה את המקרה וככל שמדובר בטעות בתום לב יינתן החזר כספי.

הודעה על טעות ובקשה להחזר יש לשלוח לדואר אלקטרוני:

management@kami.org.il

הודעה בכל דרך אחרת לא תחשב להודעת ביטול כדין.

את ההודעה יש לשלוח לא יאוחר מ-14 ימים מיום ביצוע התשלום באתר, להבדיל מיום החיוב על-ידי חברת האשראי.

בהודעה יש לפרט את פרטי החניך בעמותה, פרטי המשלם וסיבת הביטול בפירוט.

העמותה שומרת לעצמה את הזכות הבלעדית לקבוע את אופן ההחזר הכספי.

בביצוע תשלום לעמותה אני מסכים/מה למדיניות הביטולים וההחזרים המפורטת במסמך זה.
        """.trimIndent()

    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = policyText,
                        style = MaterialTheme.typography.bodyLarge
                    )

                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isEnglish) "Back" else "חזרה"
                )
            }

        }

    }

}