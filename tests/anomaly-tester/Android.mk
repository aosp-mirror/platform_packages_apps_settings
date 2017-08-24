LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := AnomalyTester

LOCAL_CERTIFICATE := platform

LOCAL_USE_AAPT2 := true

include $(BUILD_PACKAGE)