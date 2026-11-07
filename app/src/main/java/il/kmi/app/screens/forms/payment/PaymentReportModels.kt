package il.kmi.app.screens.admin

enum class PaymentStatus {
    PAID,
    UNPAID,
    PARTIAL
}

enum class PaymentMethod {
    CREDIT_CARD,
    BIT,
    WEBSITE,
    CASH,
    BANK_TRANSFER,
    MANUAL
}

/**
 * תשלום דמי חבר המשויך לסניף מסוים.
 *
 * לכל סניף נתוני תשלום עצמאיים.
 * אין איחוד כספי אוטומטי בין סניפים.
 */
data class PaymentBranchItem(
    val branchName: String,
    val requiredAmount: Double,
    val paidAmount: Double,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod? = null,
    val paymentDate: String = "",
    val notes: String = "",
    val paymentId: String = ""
)

/**
 * משתמש מאוחד אחד עם אפשרות למספר סניפים.
 *
 * השדות הקיימים נשמרים לתאימות לאחור.
 * branchPayments מכיל את פירוט התשלומים
 * כאשר המשתמש משויך למספר סניפים.
 */
data class PaymentReportItem(
    val traineeId: String,
    val fullName: String,
    val branchName: String,
    val phone: String,
    val requiredAmount: Double,
    val paidAmount: Double,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod? = null,
    val paymentDate: String = "",
    val notes: String = "",
    val branchPayments: List<PaymentBranchItem> = emptyList()
)