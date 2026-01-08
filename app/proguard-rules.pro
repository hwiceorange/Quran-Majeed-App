# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/mabaoxiang/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-optimizationpasses 5
# -dontoptimize  # Removed to enable code optimization for smaller APK size
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-ignorewarnings

-renamesourcefileattribute quran

#让混淆包 显示行号
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * implements android.os.Parcel { public *;}
-keep class com.adjust.sdk.** { *; }
-keep class org.json.** {*;}
# -keep class com.quran.** { *; }
-keep class com.quran.quranaudio.online.prayertimes.** {*;}
-keep class com.quran.quranaudio.online.quran_module.api.models.recitation.**{*;}
-keep class java.**{*;}
-keep @kotlinx.serialization.Serializable class * {*;}
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.** { *; }


-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ---------------- native -----------------
# native method.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep public class com.android.installreferrer.**{*;}

# ---------------- serializable -----------------
-keep class * implements java.io.Serializable {
    <fields>;
}

# ---------------- enumeration -----------------
# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


-keep public class com.google.android.gms.analytics.** {
    public *;
}
-keep public class com.google.firebase.FirebaseApp{*;}


#删除日志打印
-assumenosideeffects class android.util.Log {
public static *** isLoggable(java.lang.String, int);
public static *** v(...);
public static *** i(...);
public static *** w(...);
public static *** d(...);
public static *** e(...);
}
-assumenosideeffects class java.io.PrintStream {
    public *** println(...);
    public *** print(...);
}

# ============================================
# 🔥 WebView Crash Fix - Resources$NotFoundException
# ============================================
# Fix for: android.content.res.Resources$NotFoundException: Resource ID #0x90c0006
# at org.chromium.ui.base.DeviceFormFactor.isTablet()
# 
# WebView requires certain resources to be preserved and accessible
# ============================================

# Keep WebView resources
-keep class android.webkit.** { *; }
-keepclassmembers class android.webkit.** { *; }

# Keep WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView client classes
-keep class com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirWebViewClient { *; }
-keep class com.quran.quranaudio.online.quran_module.utils.chapterInfo.ChapterInfoWebViewClient { *; }

# Prevent resource obfuscation for WebView-related resources
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep resource IDs that WebView might access
-keep class **.R$integer { *; }
-keep class **.R$bool { *; }
-keep class **.R$drawable { *; }
-keep class **.R$string { *; }

# Chrome WebView specific
-dontwarn org.chromium.**
-keep class org.chromium.** { *; }

# Prevent stripping of WebView support resources
-keepresources string/**, drawable/**, layout/**