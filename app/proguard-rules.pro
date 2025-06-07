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

# Keep the data classes used with Room and Firestore automatic mapping
# Keep the class name and all its members (fields, constructors, methods)

# For Room Entities and data classes used in Type Converters:
-keep class com.mobichill.justconcentration.model.UserModel { *; }
-keep class com.mobichill.justconcentration.model.BadgeModel { *; } # If used with Firestore auto-mapping TO objects
-keep class com.mobichill.justconcentration.model.ConcentrateSessionModel { *; } # If used with Firestore auto-mapping TO objects
-keep class com.mobichill.justconcentration.model.TaskModel { *; }

# For the nested data class used in the Type Converter:
-keep class com.mobichill.justconcentration.model.SubscriptionDetails { *; }

# Keep any custom Type Converters themselves (often needed)
-keep class com.mobichill.justconcentration.base.database.Converters { *; } # Replace 'Converters' with your actual Type Converter class name

# Optional: Keep ProductItem if used with reflection elsewhere (less likely to cause these specific errors)
#-keep class com.mobichill.justconcentration.model.ProductItem { *; }

# Keep @Parcelize classes (usually handled by default rules, but explicit is safe)
-keepnames @kotlinx.parcelize.Parcelize class *
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep @Entity classes and their primary constructors for Room
-keepnames @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * {
    @androidx.room.PrimaryKey *;
    # Keep the primary constructor with its parameters for Room
    public <init>(...);
}

# Keep Firestore IgnoreExtraProperties annotation (usually handled)
-keepnames @com.google.firebase.firestore.IgnoreExtraProperties class *

# If you're using Firestore's automatic mapping FROM Firestore to objects (@DocumentId, @ServerTimestamp, etc.)
# Keep the fields annotated with Firestore annotations
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    # Add other Firestore annotations if you use them
}

# If your models have secondary constructors for Firestore mapping (like UserModel)
# Keep the secondary constructor
-keepclassmembers class com.mobichill.justconcentration.model.UserModel {
    public <init>(...); # Keep any public constructors
    # If you added a no-arg constructor for Firestore/Gson manually
    public <init>();
}
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
    public static int w(...);
}