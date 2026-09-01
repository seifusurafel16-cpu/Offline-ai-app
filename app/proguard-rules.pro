# --- MediaPipe / ML Kit native libs ---
# Keep the native model-inference binaries; do not strip them.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.mediapipe.tasks.** { *; }
-keep class com.google.mediapipe.framework.** { *; }
-keep class com.google.mediapipe.genai.** { *; }

# --- ML Kit text recognition ---
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }

# --- Room generated code ---
-keep class * extends androidx.room.RoomDatabase { *; }

# --- Kotlin metadata ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
