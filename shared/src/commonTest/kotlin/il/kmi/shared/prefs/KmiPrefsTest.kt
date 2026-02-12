package il.kmi.shared.prefs

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KmiPrefsTest {

    // יוצר מופע KmiPrefs אמיתי עם MapSettings (In-Memory) – בלי ירושה/Mock
    private fun newPrefs(): KmiPrefs = KmiPrefs(MapSettings())

    @Test
    fun writeAndRead_basicStrings() {
        val prefs = newPrefs()
        prefs.fullName = "Tester"
        prefs.email = "tester@kmi.app"
        prefs.region = "מרכז"
        prefs.branch = "הרצליה"
        prefs.ageGroup = "נוער"

        assertEquals("Tester", prefs.fullName)
        assertEquals("tester@kmi.app", prefs.email)
        assertEquals("מרכז", prefs.region)
        assertEquals("הרצליה", prefs.branch)
        assertEquals("נוער", prefs.ageGroup)
    }

    @Test
    fun writeAndRead_flagsAndNumbers() {
        val prefs = newPrefs()
        prefs.remindersOn = true
        prefs.leadMinutes = 90
        prefs.clickSounds = false
        prefs.hapticsOn = true
        prefs.syncCalendar = true
        prefs.fontSize = "large"
        prefs.themeMode = "dark"

        assertTrue(prefs.remindersOn)
        assertEquals(90, prefs.leadMinutes)
        assertEquals(false, prefs.clickSounds)
        assertEquals(true, prefs.hapticsOn)
        assertEquals(true, prefs.syncCalendar)
        assertEquals("large", prefs.fontSize)
        assertEquals("dark", prefs.themeMode)
    }
}
