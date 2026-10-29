# ProGuard / R8 rules for Pocket AI
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}

