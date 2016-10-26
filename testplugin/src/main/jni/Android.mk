LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := testjni
LOCAL_SRC_FILES := testjni.cpp
include $(BUILD_SHARED_LIBRARY)
