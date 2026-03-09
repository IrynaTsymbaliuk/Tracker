# ============================================================================
# Public API - Keep all public classes and methods
# ============================================================================

# Main Tracker API
-keep public class com.tracker.core.Tracker {
    public *;
}
-keep public class com.tracker.core.Tracker$Builder {
    public *;
}

# ============================================================================
# Result Classes - Keep all fields and methods (used in public API)
# ============================================================================

-keep public class com.tracker.core.result.** {
    public *;
}

# Keep data class metadata for reflection
-keepclassmembers class com.tracker.core.result.** {
    public <init>(...);
}

# Preserve data class component methods (for destructuring)
-keepclassmembers class com.tracker.core.result.** {
    public ** component1();
    public ** component2();
    public ** component3();
    public ** component4();
    public ** component5();
    public ** component6();
    public ** component7();
}

# ============================================================================
# Enums - Keep all enum classes
# ============================================================================

-keepclassmembers enum com.tracker.core.types.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# Permissions - Keep permission-related classes
# ============================================================================

-keep public class com.tracker.core.permission.Permission {
    public *;
}

# ============================================================================
# Model Classes - Keep Evidence and other models
# ============================================================================

-keep class com.tracker.core.model.** {
    public *;
}

-keepclassmembers class com.tracker.core.model.** {
    public <init>(...);
}

# ============================================================================
# Kotlin Coroutines Support
# ============================================================================

# Keep Continuation for suspend functions
-keepclassmembers class * {
    public ** queryAsync(...);
}

# ============================================================================
# Kotlin Metadata - Required for Kotlin libraries
# ============================================================================

-keep class kotlin.Metadata { *; }

# Keep generic signature for Kotlin
-keepattributes Signature

# Keep annotations
-keepattributes *Annotation*

# ============================================================================
# Optimization Settings
# ============================================================================

# Don't warn about missing classes that might not be on all API levels
-dontwarn android.app.usage.**

# ============================================================================
# Debugging
# ============================================================================

# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable
