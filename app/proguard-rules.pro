# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.swimtrainer.**$$serializer { *; }
-keepclassmembers class com.swimtrainer.** { *** Companion; }
-keepclasseswithmembers class com.swimtrainer.** { kotlinx.serialization.KSerializer serializer(...); }
