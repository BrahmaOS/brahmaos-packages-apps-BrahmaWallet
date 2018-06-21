#
# Android.mk
# Winton.Liu, 2018-06-21 10:28
#
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := BrahmaWallet
LOCAL_SRC_FILES := $(call all-subdir-named-files,$(LOCAL_MODULE)*.apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_CERTIFICATE := PRESIGNED
include $(BUILD_PREBUILT)
