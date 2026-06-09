# Add project specific ProGuard rules here.
# https://developer.android.com/studio/build/shrink-code

# Keep Compose runtime classes
-keep class androidx.compose.** { *; }

# 保留 Room Entity 字段（注解处理后由 ksp 生成的代码也走 R8）
-keep class com.example.todoalarm.data.** { *; }
-keep @androidx.room.Entity class * { *; }
