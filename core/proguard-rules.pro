# ============================================================================
# Keep Public API
# ============================================================================

# The consumer-rules.pro file contains the rules that apps using this library
# will automatically inherit. This file contains additional rules for building
# the library itself.

# Keep all public API classes
-keep public class com.tracker.core.** {
    public protected *;
}

# ============================================================================
# Internal Classes - Can be obfuscated
# ============================================================================

# Internal implementation classes can be shrunk/obfuscated
# (only public API needs to be preserved)

# ============================================================================
# Debugging
# ============================================================================

# Keep source file names and line numbers for better stack traces in crashes
-keepattributes SourceFile,LineNumberTable

# Keep parameter names for better debugging
-keepattributes MethodParameters

# ============================================================================
# Kotlin
# ============================================================================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep annotations
-keepattributes *Annotation*

# Keep generic signatures
-keepattributes Signature

# ============================================================================
# Remove Logging (Optional - uncomment to strip logs in release)
# ============================================================================

# Uncomment to remove all Log calls in release builds
# -assumenosideeffects class android.util.Log {
#     public static int v(...);
#     public static int d(...);
#     public static int i(...);
#     public static int w(...);
#     public static int e(...);
# }