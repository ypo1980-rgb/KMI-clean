# Firebase / Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# App + shared models
-keep class il.kmi.app.** { *; }
-keep class il.kmi.shared.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Billing
-keep class com.android.billingclient.** { *; }

# Credential Manager / Google ID
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Media3
-keep class androidx.media3.** { *; }

# Coil
-keep class coil.** { *; }
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers for clearer release crash stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name while keeping line info.
-renamesourcefileattribute SourceFile