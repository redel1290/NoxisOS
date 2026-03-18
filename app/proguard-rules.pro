# Noxis OS ProGuard rules

# LuaJ — потрібно зберегти для рефлексії
-keep class org.luaj.** { *; }
-dontwarn org.luaj.**

# Gson — моделі маніфестів
-keep class com.noxis.os.system.lki.LkiManifest { *; }
-keep class com.noxis.os.system.lki.LkiManager$AppMeta { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**

# Зберегти всі Activity
-keep class com.noxis.os.ui.** extends android.app.Activity { *; }
-keep class com.noxis.os.system.** { *; }
