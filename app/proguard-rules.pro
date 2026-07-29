# Project specific ProGuard/R8 rules.
#
# Most libraries used here (Retrofit, OkHttp, kotlinx.serialization, Hilt, WorkManager)
# ship their own consumer rules, so this file only contains what is specific to the app.

# Keep line numbers in release stack traces but hide the original source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---
# The compiler plugin generates a Companion.serializer() for every @Serializable class.
# The library ships rules for this, but keeping the app's models explicitly is cheap
# insurance against the reflective lookups used by the Retrofit converter.
-keepclassmembers @kotlinx.serialization.Serializable class com.quantum_prof.phantalandwaittimes.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.quantum_prof.phantalandwaittimes.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Retrofit ---
# Retrofit inspects generic signatures of suspend service methods at runtime.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --- WorkManager ---
# Workers are instantiated by name.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# Strip verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
