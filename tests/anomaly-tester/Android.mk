LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_STATIC_JAVA_LIBRARIES := \
    androidx.test.rules \
    mockito-target \
    ub-uiautomator \
    truth-prebuilt \

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PACKAGE_NAME := AnomalyTester
LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_INSTRUMENTATION_FOR := Settings

LOCAL_USE_AAPT2 := true

include $(BUILD_PACKAGE)
