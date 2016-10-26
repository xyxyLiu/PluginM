//
// Created by lxy on 16-7-13.
//
#include "com_example_testplugin_JniUtils.h"

JNIEXPORT jint JNICALL Java_com_example_testplugin_JniUtils_getsum
  (JNIEnv *env, jobject obj, jint a, jint b) {
     return a + b;
  }
