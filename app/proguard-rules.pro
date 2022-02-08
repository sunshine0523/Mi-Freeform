# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 混淆会导致序列化失败
-keep class com.sunshine.freeform.bean.MotionEventBean {*;}
-keep class com.sunshine.freeform.bean.KeyEventBean {*;}
# 不混淆需要hook的类
-keep class com.sunshine.freeform.hook.HookFramework {*;}
-keep class com.sunshine.freeform.hook.HookSystemUI {*;}
-keep class com.sunshine.freeform.utils.HookStartActivity {*;}
-keep class com.sunshine.freeform.utils.HookTest {*;}
-keep class com.sunshine.freeform.utils.HookFun {*;}
-keep class com.sunshine.freeform.hook.HookLauncher {*;}
-keep class com.sunshine.freeform.hook.**{*;}

#避免对AIDL混淆
-keep class * implements android.os.IInterface {*;}