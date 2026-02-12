package il.kmi.shared.prefs

@Suppress("unused") // חשיפה עתידית ל-iOS/Android
class KmiPrefsFacade private constructor(
    private val prefs: KmiPrefs
) {
    companion object {
        // ✔️ נדרש קונטקסט – מעבירים אותו ל־Factory
        fun shared(context: Any): KmiPrefsFacade =
            KmiPrefsFacade(prefs = KmiPrefsFactory.create(context))
    }

    // --- GET ---
    fun themeMode(): String = prefs.themeMode
    fun fontSize(): String = prefs.fontSize
    fun fontScale(): String = prefs.fontScaleString

    fun fullName(): String = prefs.fullName.orEmpty()
    fun phone(): String = prefs.phone.orEmpty()
    fun email(): String = prefs.email.orEmpty()
    fun region(): String = prefs.region.orEmpty()
    fun branch(): String = prefs.branch.orEmpty()
    fun ageGroup(): String = prefs.ageGroup.orEmpty()
    fun username(): String = prefs.username.orEmpty()
    fun password(): String = prefs.password.orEmpty()
    fun branchId(): String = prefs.branchId.orEmpty()

    fun remindersOn(): Boolean = prefs.remindersOn
    fun leadMinutes(): Int = prefs.leadMinutes
    fun syncCalendar(): Boolean = prefs.syncCalendar
}
