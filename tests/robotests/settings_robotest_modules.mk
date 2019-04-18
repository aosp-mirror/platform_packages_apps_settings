# This is included by Android.mk located in the same folder.
# It sets up all robotest sub-modules for RunSettingsRoboTests,
# where the name of each target is defined based on the package
# name under com.android.settings and order the targets
# alphabetically.
#
# Nameing pattern for the target:
#     RunSettingsRoboTests-<package_name>
#
# For example:
#     RunSettingsRoboTests-core
#
#
# TODO(b/130745039):decouple dependencies among each module

#############################################################
# Settings runner target to run applications module.        #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunSettingsRoboTests-applications
ROBOTEST_FILTER := applications

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

#############################################################
# Settings runner target to run development module.         #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunSettingsRoboTests-development
ROBOTEST_FILTER := development

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

#############################################################
# Settings runner target to run wifi module.                #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunSettingsRoboTests-wifi
ROBOTEST_FILTER := wifi

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