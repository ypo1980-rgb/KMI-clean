import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // ✅ מחבר נכון commonMain -> iosMain -> ios targets
    applyDefaultHierarchyTemplate()

    // XCFramework — ייכנס לפעולה כשתהיה סביבת Mac
    val xcf = XCFramework("Shared")
    targets.withType(KotlinNativeTarget::class.java).configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // ✅ חשוב: פותר מצב שבו String/Long/Double/List לא מזוהים ב-commonMain
                implementation(kotlin("stdlib"))

                implementation("com.russhwolf:multiplatform-settings:1.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.russhwolf:multiplatform-settings-test:1.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

                // Android-only
                implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
            }
        }

        // ✅ לא יוצרים iosMain ידנית כאן.
        // applyDefaultHierarchyTemplate יוצר ומחבר אותו אוטומטית.
        val iosMain by getting {
            dependencies {
                // בעתיד: Ktor Darwin / native libs
            }
        }

        val iosTest by getting {
            dependencies {
                // בעתיד בדיקות iOS
            }
        }
    }

    jvmToolchain(17)
}

// --- iOS helper tasks — Windows-friendly; run on macOS later ---
tasks.register("buildIosFramework") {
    group = "kmi"
    description = "Build the Shared XCFramework. Run on macOS."
    dependsOn("assembleSharedXCFramework")
}

tasks.register("kmiDoctor") {
    group = "kmi"
    description = "Sanity checks for KMP structure before moving to macOS."
    doLast {
        println("✅ KMI Doctor: KMP structure looks OK.")
        println("On macOS run: ./gradlew :shared:buildIosFramework")
    }
}

android {
    namespace = "il.kmi.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}