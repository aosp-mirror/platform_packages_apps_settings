#############################################
# Settings Robolectric test target. #
#############################################
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Include the testing libraries (JUnit4 + Robolectric libs).
LOCAL_STATIC_JAVA_LIBRARIES := \
    platform-system-robolectric

LOCAL_JAVA_LIBRARIES := \
    junit4-target \
    platform-robolectric-prebuilt \
    sdk_v23

LOCAL_APK_LIBRARIES = Settings
LOCAL_MODULE := SettingsRoboTests

# TODO: Remove when this target builds with checkbuild
LOCAL_DONT_CHECK_MODULE := true

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

#############################################################
# Settings runner target to run the previous target. #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunSettingsRoboTests

# TODO: Remove when this target builds with checkbuild
LOCAL_DONT_CHECK_MODULE := true

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := \
    SettingsRoboTests

LOCAL_TEST_PACKAGE := Settings

include prebuilts/misc/common/robolectric/run_robotests.mk