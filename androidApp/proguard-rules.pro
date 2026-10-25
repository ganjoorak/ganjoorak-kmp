# kotlinx.serialization: keep generated serializers for @Serializable classes (DTOs, settings, routes).
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers @kotlinx.serialization.Serializable class net.ganjoorak.app.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class net.ganjoorak.app.**$$serializer { *; }

# Ktor / OkHttp optional dependencies
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn java.lang.management.**
