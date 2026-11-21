# ChatCtrl R8 rules.

# Keep file and line numbers so crash reports point at real code (decode names with mapping.txt from the CI run).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The Rust SDK is reached through JNA and generated UniFFI bindings, which look classes and fields up by name.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.matrix.rustcomponents.sdk.** { *; }
-keep class uniffi.** { *; }
-keep class org.matrix.rustsdk.** { *; }
-keep, includedescriptorclasses class org.rustls.platformverifier.** { *; }
-dontwarn java.awt.**

# Background sync worker and notification receivers are created by the system from their class names.
-keep class com.vknxworks.chatctrl.notify.** { *; }

# osmdroid (the OpenStreetMap map for sending and viewing locations) and the zxing-cpp QR decoder (JNI).
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-keep class zxingcpp.** { *; }

-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

# Release builds drop every log entirely (they can name rooms, users, servers and errors).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}

# WorkManager keeps its job list in a Room 2.6.1 database, and Room creates the generated
# WorkDatabase_Impl by reflection through its no-argument constructor. Room 2.6.1's own rule
# (-keep class * extends androidx.room.RoomDatabase) names no members, and the R8 in Android
# Gradle Plugin 9 no longer keeps a constructor for such a rule, so without this line the app
# would crash at start (NoSuchMethodException: WorkDatabase_Impl.<init>). Same rule Room 2.7 ships.
-keep class * extends androidx.room.RoomDatabase { void <init>(); }
