plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    // id("org.jetbrains.kotlin.kapt")   ← מיותר אם לא משתמשים בשום kapt
    id("com.google.devtools.ksp")
}

android {
    namespace = "il.kmi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "il.kmi.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        vectorDrawables.useSupportLibrary = true
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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // -------------------------------
    // Firebase דרך BOM
    // -------------------------------
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    // ✅ חדש – Firebase Storage להעלאת תמונות/וידאו בפורום
    implementation("com.google.firebase:firebase-storage-ktx")
    // ✅ חדש – Firebase Cloud Messaging (FCM) להתראות Push
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Icons / Billing
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // Navigation / DataStore
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")

    // Material classic (optional)
    implementation("com.google.android.material:material:1.12.0")

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
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

// קונפיגורציית KSP אופציונלית (יעיל ל-Room schemas; לא חובה)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}
