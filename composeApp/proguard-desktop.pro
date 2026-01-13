# Ignorer les classes Android (non utilisées sur Desktop)
-dontwarn android.**
-dontwarn dalvik.**

# Ignorer les classes SSL optionnelles
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Ignorer les classes OkHttp Android-specific
-dontwarn okhttp3.internal.platform.Android*

# Ignorer kotlinx-datetime (optionnel pour Material3 DatePicker)
-dontwarn kotlinx.datetime.**

# Ignorer Ktor classes optionnelles
-dontwarn io.ktor.utils.io.jvm.nio.**

# Coil - ignorer les classes optionnelles
-dontwarn coil3.network.ktor.internal.**

# Garder les classes nécessaires pour la réflexion
-keepattributes Signature
-keepattributes *Annotation*

# Garder Ktor client
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }

# JNA (nécessaire pour Desktop)
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }