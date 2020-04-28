LOCAL_PATH := $(call my-dir)

SETTINGS_AOSP_PATH := packages/apps/Settings

#############################################################
# Build SettingsRoboTestStub.apk which includes test-only resources.#
#############################################################
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := SettingsRoboTestStub
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS := optional
LOCAL_USE_AAPT2 := true

RELATIVE_SETTINGS_AOSP_PATH := ../../../../../$(SETTINGS_AOSP_PATH)

LOCAL_MANIFEST_FILE := $(RELATIVE_SETTINGS_AOSP_PATH)/AndroidManifest.xml

LOCAL_SRC_FILES := $(call all-java-files-under, $(RELATIVE_SETTINGS_AOSP_PATH)/src)

LOCAL_RESOURCE_DIR += \
	$(LOCAL_PATH)/res \
	$(SETTINGS_AOSP_PATH)/res

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx-constraintlayout_constraintlayout \
    androidx.slice_slice-builders \
    androidx.slice_slice-core \
    androidx.slice_slice-view \
    androidx.core_core \
    androidx.appcompat_appcompat \
    androidx.cardview_cardview \
    androidx.preference_preference \
    androidx.recyclerview_recyclerview \
    com.google.android.material_material \
    setupcompat \
    setupdesign

LOCAL_JAVA_LIBRARIES := \
    telephony-common \
    ims-common

LOCAL_STATIC_JAVA_LIBRARIES := \
    androidx-constraintlayout_constraintlayout-solver \
    androidx.lifecycle_lifecycle-runtime \
    androidx.lifecycle_lifecycle-extensions \
    guava \
    jsr305 \
    settings-contextual-card-protos-lite \
    settings-log-bridge-protos-lite \
    contextualcards \
    settings-logtags \
    zxing-core-1.7

include frameworks/base/packages/SettingsLib/common.mk
include frameworks/base/packages/SettingsLib/search/common.mk

include $(BUILD_PACKAGE)

#############################################################
# Settings Robolectric test target.                         #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := SettingsRoboTests
LOCAL_MODULE_CLASS := JAVA_LIBRARIES

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    $(call all-java-files-under, ../../../../../frameworks/base/packages/SettingsLib/tests/robotests/src/com/android/settingslib/testutils)

LOCAL_JAVA_RESOURCE_DIRS := config

LOCAL_JAVA_LIBRARIES := \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_INSTRUMENTATION_FOR := SettingsRoboTestStub

LOCAL_MODULE_TAGS := optional

# Generate test_config.properties
include external/robolectric-shadows/gen_test_config.mk

include $(BUILD_STATIC_JAVA_LIBRARY)

#############################################################
# Settings runner target to run the previous target.        #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunSettingsRoboTests

LOCAL_JAVA_LIBRARIES := \
    SettingsRoboTests \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_TEST_PACKAGE := SettingsRoboTestStub

LOCAL_INSTRUMENT_SOURCE_DIRS := $(dir $(LOCAL_PATH))../src \
    frameworks/base/packages/SettingsLib/search/src \

LOCAL_ROBOTEST_TIMEOUT := 36000

include external/robolectric-shadows/run_robotests.mk
