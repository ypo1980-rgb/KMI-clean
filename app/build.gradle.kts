import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    // id("org.jetbrains.kotlin.kapt")   ← מיותר אם לא משתמשים בשום kapt
    id("com.google.devtools.ksp")
}

val keystoreProperties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { load(it) }
    }
}

val releaseStoreFile = keystoreProperties.getProperty("RELEASE_STORE_FILE")
val releaseStorePassword = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")

val hasReleaseSigningConfig =
    !releaseStoreFile.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "il.kmi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "il.kmi.training"
        minSdk = 24
        targetSdk = 35
        versionCode = 159
        versionName = "1.0.18"
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // ⬇️ מאפשר שימוש ב-java.time על API<26
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.ui.unit.ExperimentalUnitApi",
            "-XXLanguage:+UnitConversionsOnArbitraryExpressions"
        )
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin { jvmToolchain(17) }

// -------------------------------
// בלוק התלויות המקורי (השארנו כהערות; אל תמחק)
// -------------------------------
// dependencies {
//     implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
//     implementation("com.google.firebase:firebase-auth")
// }

dependencies {
    // ✅ מודול משותף
    implementation(project(":shared"))
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

    // Compose BOM (תואם Kotlin 2.x / Compose 1.7+)
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Lifecycle (שודרג ובלי כפילויות)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-process:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("com.kosherjava:zmanim:2.5.0")

    // ✅ Coil לתמונות (כולל AsyncImage בפורום)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Core / Activity / Runtime
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.runtime:runtime-saveable")

    // ✅ Settings KMP
    implementation("com.russhwolf:multiplatform-settings:1.1.1")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-graphics")
    // 👇 תוספת קטנה כדי לעבוד בנוחות עם AndroidView / ViewBinding
    implementation("androidx.compose.ui:ui-viewbinding")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Browser (Custom Tabs)
    implementation("androidx.browser:browser:1.8.0")

// Splash Screen (Android 12+)
    implementation("androidx.core:core-splashscreen:1.0.1")
    // -------------------------------
    // Firebase דרך BOM
    // -------------------------------
    // ✅ נשארים כרגע על 33.3.0 כי הפרויקט עדיין משתמש במודולי firebase-*-ktx
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    // ✅ Firebase Storage להעלאת תמונות/וידאו בפורום
    implementation("com.google.firebase:firebase-storage-ktx")
    // ✅ Firebase Cloud Messaging (FCM) להתראות Push
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // -------------------------------
    // Google Login - Credential Manager + Classic fallback
    // -------------------------------
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Icons / Billing
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    // Navigation / DataStore
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    // Material classic (optional)
    implementation("com.google.android.material:material:1.12.0")

    // ✅ Google Places SDK - חיפוש מקומות / השלמה אוטומטית
    implementation("com.google.android.libraries.places:places:4.3.1")

    // Coroutines ל־Firebase/Google Play Services (בשביל tasks.await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ✅ Desugaring ל-java.time על API<26
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // -------------------------------
    // Room – דרך KSP (לא KAPT)
    // -------------------------------
    val roomVer = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVer")
    implementation("androidx.room:room-ktx:$roomVer")
    ksp("androidx.room:room-compiler:$roomVer")

    // Tests
    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// קונפיגורציית KSP אופציונלית (יעיל ל-Room schemas; לא חובה)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}
