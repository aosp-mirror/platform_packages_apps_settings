LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Include all makefiles in subdirectories
include $(call all-makefiles-under,$(LOCAL_PATH))
