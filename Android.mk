LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := su-Superuser
LOCAL_MODULE_STEM := su
LOCAL_SRC_FILES := su.c activity.cpp

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := user

LOCAL_C_INCLUDES += external/sqlite/dist
LOCAL_SHARED_LIBRARIES := liblog libsqlite libandroid_runtime

ifneq ($(UNSIGNED_PACKAGE),)
LOCAL_CFLAGS += -DUNSIGNED_PACKAGE=1
endif

ifneq ($(DONUT),)
LOCAL_CFLAGS += -DDONUT=1
endif

LOCAL_OVERRIDES_PACKAGES := su
PACKAGES.$(LOCAL_MODULE).OVERRIDES := su

include $(BUILD_EXECUTABLE)

## 

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := Superuser
LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_MODULE_TAGS := user

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

##

