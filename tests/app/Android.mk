LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := android.test.runner bouncycastle

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-test \
    mockito-target \
    espresso-core \
    espresso-contrib-nodep \
    espresso-intents-nodep \
    ub-uiautomator

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SettingsTests

LOCAL_INSTRUMENTATION_FOR := Settings

include $(BUILD_PACKAGE)
