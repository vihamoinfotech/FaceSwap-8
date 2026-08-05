# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ==========================================
# Retrofit
# ==========================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ==========================================
# OkHttp / Okio
# ==========================================
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ==========================================
# RevenueCat
# ==========================================
-dontwarn com.revenuecat.**
-keep class com.revenuecat.** { *; }

# ==========================================
# Gson
# ==========================================
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.google.gson.** { *; }
-dontwarn sun.misc.**

# IMPORTANT: Keep Gson model fields from being renamed so JSON parsing doesn't break!
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==========================================
# Firebase
# ==========================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ==========================================
# Kotlin (if used)
# ==========================================
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ==========================================
# Android Parcelable
# ==========================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ==========================================
# Remove Logs in Release
# ==========================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

##################################################
# Keep Android Components (Do NOT rename)
##################################################

# Activities
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }
-keep class * extends androidx.appcompat.app.AppCompatActivity { *; }

# Fragments
-keep class * extends androidx.fragment.app.Fragment { *; }

# Services
-keep class * extends android.app.Service { *; }

# Broadcast Receivers
-keep class * extends android.content.BroadcastReceiver { *; }

# Content Providers
-keep class * extends android.content.ContentProvider { *; }

# Applications
-keep class * extends android.app.Application { *; }

-dontwarn com.google.android.libraries.identity.googleid.GetGoogleIdOption$Builder
-dontwarn com.google.android.libraries.identity.googleid.GetGoogleIdOption
-dontwarn com.huawei.agconnect.config.AGConnectServicesConfig
-dontwarn com.huawei.hms.aaid.HmsInstanceId
-dontwarn com.huawei.hms.common.ApiException
-dontwarn com.huawei.hms.push.HmsMessageService