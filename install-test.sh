#!/bin/sh

host_pkg=com.example.testhost
plugin_pkg1=com.example.testplugin
plugin_pkg2=com.example.testplugin2
buildType=assembleDebug
apk_suffix=debug
plugin_apk_dir=PluginM
only_host_build=0

# command checking
while getopts rh option
do
    case "$option" in
        r)
            buildType=assembleRelease
            apk_suffix=release
            echo "option: build release"
            ;;
        h)
            only_host_build=1
            echo "option: only build host"
            ;;
        \?)
            echo "Usage: args [-r] [-h]"
            echo "-r means make release build"
            exit 2
            ;;
    esac
done

echo "host pkg = $host_pkg"
echo "plugin_pkg1 = $plugin_pkg1"
echo "plugin_pkg2 = $plugin_pkg2"

# build pluginlib
./gradlew pluginlib:clean pluginlib:makeJar && \
# build plugin share library
./gradlew pluginsharelib:clean pluginsharelib:makeJar && \

if [ $only_host_build -eq 0 ]
then
    adb shell mkdir /sdcard/$plugin_apk_dir/
    ./gradlew testplugin2:clean testplugin2:$buildType && \
    adb push testplugin2/build/outputs/apk/testplugin2-$apk_suffix.apk /sdcard/$plugin_apk_dir/ && \
    ./gradlew testplugin:clean testplugin:$buildType && \
    adb push testplugin/build/outputs/apk/testplugin-$apk_suffix.apk /sdcard/$plugin_apk_dir/
fi && \

# build host
./gradlew testhost:clean testhost:$buildType && \
adb push testhost/build/outputs/apk/testhost-$apk_suffix.apk /data/local/tmp/com.example.testhost && \

# start host
adb shell pm install -r "/data/local/tmp/$host_pkg" && \
adb shell am start -n "$host_pkg/com.example.testhost.DemoActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER