pluginManagement {
    repositories {
        // סדר כזה עובד מצוין
        gradlePluginPortal()
        google()
        mavenCentral()
        // נדרש רק אם אתה משתמש ב־org.jetbrains.compose / Compose Multiplatform
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // כנ"ל – עבור ארטיפקטים של Compose MPP (לא מזיק גם אם לא משתמשים)
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "KMI"

// מודולים קיימים בשורש הפרויקט
include(":app")
include(":shared")
