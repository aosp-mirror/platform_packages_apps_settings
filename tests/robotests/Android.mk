#############################################
# Settings Robolectric test target. #
#############################################
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Include the testing libraries (JUnit4 + Robolectric libs).
LOCAL_STATIC_JAVA_LIBRARIES := \
    platform-system-robolectric \
    truth-prebuilt

LOCAL_JAVA_LIBRARIES := \
    junit4-target \
    platform-robolectric-prebuilt \
    sdk_v23

LOCAL_APK_LIBRARIES = Settings
LOCAL_MODULE := SettingsRoboTests

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

#############################################################
# Settings runner target to run the previous target. #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunSettingsRoboTests

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := \
    SettingsRoboTests

LOCAL_TEST_PACKAGE := Settings

include prebuilts/misc/common/robolectric/run_robotests.mk