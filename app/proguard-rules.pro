# Project specific ProGuard rules

# Keep our core packages from being aggressively shrunk/obfuscated
-keep class com.awd.r2cloud.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, Signature
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.json.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# AWS SDK for Kotlin (S3/R2)
-keep class aws.sdk.kotlin.services.s3.** { *; }
-keep class aws.smithy.kotlin.runtime.** { *; }
-keep interface aws.smithy.kotlin.runtime.** { *; }
-keep class * implements aws.smithy.kotlin.runtime.http.engine.HttpClientEngineFactory { *; }
-keep class * implements aws.smithy.kotlin.runtime.auth.credentials.CredentialsProvider { *; }
-dontwarn aws.sdk.kotlin.**
-dontwarn aws.smithy.kotlin.**

# Hilt / Dagger
-keepattributes *Annotation*
-keep class dagger.hilt.android.internal.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class com.awd.r2cloud.R2DriveApp { *; }
-keep class com.awd.r2cloud.MainActivity { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# WorkManager (Maximum protection)
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-dontwarn androidx.work.impl.**

# Room (Maximum protection)
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Entity { *; }
-keep class * extends androidx.room.Dao { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Hilt Workers
-keep class * extends androidx.work.ListenableWorker {
    @dagger.assisted.AssistedInject <init>(...);
    @javax.inject.Inject <init>(...);
}

# AndroidX Lifecycle & ViewModel
-keep class androidx.lifecycle.SavedStateHandle { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Kotlin Coroutines
-keep class kotlinx.coroutines.android.HandlerContext { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-dontwarn kotlinx.coroutines.**

# Retrofit / OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Tink / Google API (Transitive optional deps)
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**

# General
-keepattributes SourceFile, LineNumberTable
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
