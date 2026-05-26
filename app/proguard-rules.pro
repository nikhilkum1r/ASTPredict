# TFLite
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.astpredict.app.data.** { *; }

# Keep detection models
-keep class com.astpredict.app.data.ml.** { *; }