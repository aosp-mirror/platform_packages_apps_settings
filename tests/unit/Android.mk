LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-test \
    espresso-core \
    legacy-android-test \
    mockito-target-minus-junit4 \
    truth-prebuilt \
    ub-uiautomator \


# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SettingsUnitTests
LOCAL_COMPATIBILITY_SUITE := device-tests

LOCAL_INSTRUMENTATION_FOR := Settings

include $(BUILD_PACKAGE)
