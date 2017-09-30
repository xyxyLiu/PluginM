# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /android/sdk/tools/proguard/proguard-android.txt
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
-dontwarn com.reginald.pluginm.**

-keep class android.os.* {
    <methods>;
    <fields>;
}
-keep class android.util.* {
    <methods>;
    <fields>;
}
-keep class android.content.** {
    <methods>;
    <fields>;
}

### Disable known warnings ###
-dontwarn android.os.**
-dontwarn android.util.**
-dontwarn android.content.**
-dontwarn com.android.internal.**
-dontwarn android.app.**


-keep class com.reginald.pluginm.HostInstrumentation {
<methods>;
}

-keep class com.reginald.pluginm.stub.PluginContentResolver {
<methods>;
}

-keep class com.reginald.pluginm.stub.Stubs$**{
    *;
}

# pluginm api start
-keep public class com.reginald.pluginm.pluginapi.PluginHelper {
    private static <methods>;
    public static <methods>;
}
# pluginm api end