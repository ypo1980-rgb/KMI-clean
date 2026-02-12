package il.kmi.shared.prefs

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Android actual provider.
 * Call init(context) once in your Application.
 */
actual object SharedSettingsFactoryProvider {

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun createFactory(): Settings.Factory {
        val ctx = appContext
            ?: error("SharedSettingsFactoryProvider.init(context) was not called")
        return SharedPreferencesSettings.Factory(ctx)
    }
}
