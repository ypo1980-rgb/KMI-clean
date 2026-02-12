package il.kmi.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Locale
import il.kmi.shared.domain.Belt
import androidx.datastore.preferences.preferencesDataStore // ← השארתי

// ← שינוי שם ה-extension כדי להימנע מכפילויות בסביבה שיש בה כבר dataStore אחר
internal val Context.kmiDataStore by preferencesDataStore(name = "kmi_datastore")

class DataStoreManager(private val context: Context) {

    private val KEY_SELECTED_BELT = stringPreferencesKey("selected_belt")
    private val KEY_SMS_CONSENT   = booleanPreferencesKey("sms_consent")
    private val KEY_PHONE_NUMBER  = stringPreferencesKey("phone_number")

    private val KEY_NOTIF_ENABLED  = booleanPreferencesKey("notif_enabled")
    private val KEY_NOTIF_LEAD_MIN = intPreferencesKey("notif_lead_min")

    // ---------- Progress per day ----------
    private fun todayKey() = LocalDate.now().toString()
    private fun keyPracticeMin(d: String) = intPreferencesKey("pr_min_$d")
    private fun keyPracticeCnt(d: String) = intPreferencesKey("pr_cnt_$d")
    private fun keyMasteredCnt(d: String) = intPreferencesKey("ms_cnt_$d")

    suspend fun addPracticeSeconds(seconds: Int) {
        val mins = (seconds / 60).coerceAtLeast(1)
        val d = todayKey()
        context.kmiDataStore.edit { p ->
            p[keyPracticeMin(d)] = (p[keyPracticeMin(d)] ?: 0) + mins
            p[keyPracticeCnt(d)] = (p[keyPracticeCnt(d)] ?: 0) + 1
        }
    }

    suspend fun addMasteredTick() {
        val d = todayKey()
        context.kmiDataStore.edit { p ->
            p[keyMasteredCnt(d)] = (p[keyMasteredCnt(d)] ?: 0) + 1
        }
    }

    suspend fun readProgressRange(daysBack: Int): Triple<List<Int>, List<Int>, List<Int>> {
        val end = LocalDate.now()
        val start = end.minusDays(daysBack.toLong() - 1)
        val prefs = context.kmiDataStore.data.first()

        val mins  = mutableListOf<Int>()
        val sets  = mutableListOf<Int>()
        val marks = mutableListOf<Int>()

        var d = start
        while (!d.isAfter(end)) {
            val k = d.toString()
            mins  += prefs[intPreferencesKey("pr_min_$k")] ?: 0
            sets  += prefs[intPreferencesKey("pr_cnt_$k")] ?: 0
            marks += prefs[intPreferencesKey("ms_cnt_$k")] ?: 0
            d = d.plusDays(1)
        }
        return Triple(mins, sets, marks)
    }

    // ---------- item key ----------
    private fun normalizeForKey(s: String): String =
        s.lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^a-z0-9_.-]"), "_")

    private fun keyForItem(belt: String, topic: String, item: String) =
        booleanPreferencesKey(
            "itm_${normalizeForKey(belt)}_${normalizeForKey(topic)}_${normalizeForKey(item)}"
        )

    // ---------- selected belt ----------
    suspend fun saveSelectedBelt(belt: Belt) {
        context.kmiDataStore.edit { it[KEY_SELECTED_BELT] = belt.id }
    }
    suspend fun clearSelectedBelt() {
        context.kmiDataStore.edit { it.remove(KEY_SELECTED_BELT) }
    }
    suspend fun readSelectedBelt(): Belt? {
        val v = context.kmiDataStore.data.first()[KEY_SELECTED_BELT] ?: return null
        return Belt.fromId(v)
    }

    // ---------- item mastery API ----------
    suspend fun setItemMastered(belt: Belt, topic: String, item: String, mastered: Boolean) {
        context.kmiDataStore.edit { it[keyForItem(belt.id, topic, item)] = mastered }
    }

    suspend fun isItemMastered(belt: Belt, topic: String, item: String): Boolean {
        return context.kmiDataStore.data.first()[keyForItem(belt.id, topic, item)] ?: false
    }

    suspend fun readItemStatus(belt: Belt, topic: String, item: String): Boolean? {
        return context.kmiDataStore.data.first()[keyForItem(belt.id, topic, item)]
    }

    suspend fun clearItemStatus(belt: Belt, topic: String, item: String) {
        context.kmiDataStore.edit { it.remove(keyForItem(belt.id, topic, item)) }
    }

    // ---------- SMS ----------
    suspend fun saveSmsConsent(consent: Boolean) {
        context.kmiDataStore.edit { it[KEY_SMS_CONSENT] = consent }
    }
    suspend fun clearSmsConsent() {
        context.kmiDataStore.edit { it.remove(KEY_SMS_CONSENT) }
    }
    suspend fun readSmsConsent(): Boolean? =
        context.kmiDataStore.data.first()[KEY_SMS_CONSENT]

    suspend fun savePhoneNumber(number: String) {
        context.kmiDataStore.edit { it[KEY_PHONE_NUMBER] = number }
    }
    suspend fun readPhoneNumber(): String? =
        context.kmiDataStore.data.first()[KEY_PHONE_NUMBER]

    // ---------- notifications ----------
    suspend fun readNotificationsEnabled(): Boolean =
        context.kmiDataStore.data.first()[KEY_NOTIF_ENABLED] ?: true

    suspend fun setNotificationsEnabled(v: Boolean) {
        context.kmiDataStore.edit { it[KEY_NOTIF_ENABLED] = v }
    }

    suspend fun readNotifLeadMinutes(): Int =
        context.kmiDataStore.data.first()[KEY_NOTIF_LEAD_MIN] ?: 60

    suspend fun setNotifLeadMinutes(m: Int) {
        context.kmiDataStore.edit { it[KEY_NOTIF_LEAD_MIN] = m }
    }
}
