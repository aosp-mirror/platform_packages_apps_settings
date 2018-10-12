LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        $(call all-logtags-files-under, src)

LOCAL_MODULE := settings-logtags

include $(BUILD_STATIC_JAVA_LIBRARY)

# Build the Settings APK
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := Settings
LOCAL_PRIVATE_PLATFORM_APIS := true
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS := optional
LOCAL_USE_AAPT2 := true

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.slice_slice-builders \
    androidx.slice_slice-core \
    androidx.slice_slice-view \
    androidx.core_core \
    androidx.legacy_legacy-support-v4 \
    androidx.legacy_legacy-support-v13 \
    androidx.appcompat_appcompat \
    androidx.cardview_cardview \
    androidx.preference_preference \
    androidx.recyclerview_recyclerview \
    androidx.legacy_legacy-preference-v14 \

LOCAL_JAVA_LIBRARIES := \
    telephony-common \
    ims-common

LOCAL_STATIC_JAVA_LIBRARIES := \
    androidx.lifecycle_lifecycle-runtime \
    androidx.lifecycle_lifecycle-extensions \
    guava \
    jsr305 \
    settings-logtags \

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include frameworks/opt/setupwizard/library/common-gingerbread.mk
include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
