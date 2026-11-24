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

# Navigation resolves enum route arguments (PoetSource in PoemRoute) with Class.forName,
# so enum names must survive minification.
-keep enum net.ganjoorak.app.** { *; }

# WorkManager: its Room database implementation is instantiated reflectively at
# startup (androidx.startup), and Worker subclasses are instantiated by name by
# the default WorkerFactory. R8 must not strip or rename any of them — the same
# class of release-only crash as the Glance/WorkManager Room strip in Madar.
-keep class androidx.work.impl.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
# The daily-poem worker and widget provider are also referenced by name
# (WorkManager / the app-widget host), so keep the whole Android-only daily package.
-keep class net.ganjoorak.app.daily.** { *; }
