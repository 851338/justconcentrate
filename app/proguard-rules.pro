# Add project specific ProGuard rules here.
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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.mobichill.justconcentration.model.UserModel { *; }
# Keep the SubscriptionDetails data class itself and its constructors for Gson/JSON deserialization
-keepnames class com.mobichill.justconcentration.model.SubscriptionDetails # Keep the original name if needed by other reflection (good practice)
-keep class com.mobichill.justconcentration.model.SubscriptionDetails { *; } # Keep all members, including constructors and fields

# Optional: Keep names for UserModel if Room mapping has issues, though Room is usually better handled
-keepnames class com.mobichill.justconcentration.model.UserModel
# If Room mapping to/from Firestore needs specific fields kept by name:
# -keepclassmembers class com.mobichill.justconcentration.model.UserModel {
#   !synthetic <fields>; # Keep all non-synthetic fields
# }
#-assumenosideeffects class android.util.Log {
#    public static boolean isLoggable(java.lang.String, int);
#    public static int v(...);
#    public static int i(...);
#    public static int d(...);
#    public static int w(...);
#}
#todo comment log-remover for temporarily