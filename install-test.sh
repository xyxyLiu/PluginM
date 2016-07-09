#!/bin/sh
pkg=cn.opda.a.phonoalbumshoushou

./gradlew testplugin:assembleDebug && cp testplugin/build/outputs/apk/testplugin-debug.apk testhost/src/main/assets/ && ./gradlew testhost:assembleDebug  && adb push testhost/build/outputs/apk/testhost-debug.apk /data/local/tmp/com.example.testhost && adb shell pm install -r "/data/local/tmp/com.example.testhost" && adb shell am start -n "com.example.testhost/com.example.testhost.HostMainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER