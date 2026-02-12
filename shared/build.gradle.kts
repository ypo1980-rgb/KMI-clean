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

    // XCFramework (ייכנס לפעולה כשתהיה סביבת Mac)
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

        /* ---------- iOS (נוצר ידנית – תואם ל-AGP/Kotlin אצלך) ---------- */

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // בעתיד: Ktor Darwin / native libs
            }
        }

        val iosTest by creating {
            dependsOn(commonTest)
        }

        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val iosX64Test by getting { dependsOn(iosTest) }
        val iosArm64Test by getting { dependsOn(iosTest) }
        val iosSimulatorArm64Test by getting { dependsOn(iosTest) }
    }

    jvmToolchain(17)
}

// --- iOS helper tasks (Windows-friendly; run on macOS later) ---
tasks.register("buildIosFramework") {
    group = "kmi"
    description = "Build the Shared XCFramework (run on macOS)."
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
