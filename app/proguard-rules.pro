# HighSpin Proguard/R8 optimization rules
-keep class com.lowspin.app.data.db.** { *; }
-keep class com.lowspin.app.media.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room generated migrations and schemas
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
