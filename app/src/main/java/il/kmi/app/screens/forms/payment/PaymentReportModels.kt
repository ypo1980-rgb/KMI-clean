package il.kmi.app.screens.admin

enum class PaymentStatus {
    PAID,
    UNPAID,
    PARTIAL
}

enum class PaymentMethod {
    CREDIT_CARD,
    WEBSITE,
    CASH,
    BANK_TRANSFER,
    MANUAL
}

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
    val notes: String = ""
)