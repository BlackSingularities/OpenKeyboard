# Serializacja ustawień (kotlinx.serialization).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.blacksingularities.openkeyboard.** {
    *** Companion;
}
-keepclasseswithmembers class com.blacksingularities.openkeyboard.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.blacksingularities.openkeyboard.**$$serializer { *; }
