plugins {
    // Android Gradle Plugin (גרסה יציבה ועדכנית)
    id("com.android.application") version "8.12.3" apply false
    id("com.android.library")     version "8.12.3" apply false

    // Kotlin 2.0.20 (התאמה מלאה ל-Compose Compiler Plugin 2.x)
    id("org.jetbrains.kotlin.android")          version "2.0.20" apply false
    id("org.jetbrains.kotlin.multiplatform")    version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose")   version "2.0.20" apply false

    // KSP חייבת להתאים במדויק לגרסת Kotlin
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    // kapt אופציונלי; אם אינך משתמש בו במודול :app אפשר להסיר
    id("org.jetbrains.kotlin.kapt") version "2.0.20" apply false

    // Compose Multiplatform (לא חובה אם אתה ב-Android בלבד)
    id("org.jetbrains.compose") version "1.7.0" apply false

    // Google / Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false
}
