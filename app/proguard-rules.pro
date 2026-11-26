# HighSpin Proguard/R8 optimization rules
-repackageclasses 'o'
-allowaccessmodification

# Keep manifest entry points and custom Room DB subclasses
-keep class * extends androidx.room.RoomDatabase
-keep class com.lowspin.app.data.db.*_Impl { *; }
-dontwarn androidx.room.paging.**

# Keep reflection targets used by MediaSession framework bridge
-keepclassmembers class androidx.media3.session.MediaSession { *; }
-keepclassmembers class androidx.media3.session.MediaSessionImpl { *; }

# Keep annotation metadata only where required by Room/Kotlin
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip verbose/debug logging from release bytecode
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
