LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := su-Superuser
LOCAL_MODULE_STEM := su
LOCAL_SRC_FILES := su.c activity.cpp

LOCAL_MODULE_PATH := $(TARGET_OUT_EXECUTABLES)
LOCAL_MODULE_TAGS := user

LOCAL_C_INCLUDES += external/sqlite/dist
LOCAL_SHARED_LIBRARIES := liblog libsqlite libandroid_runtime

ifneq ($(UNSIGNED_PACKAGE),)
LOCAL_CFLAGS += -DUNSIGNED_PACKAGE=1
endif


LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_OVERRIDES_PACKAGES := su
PACKAGES.$(LOCAL_MODULE).OVERRIDES := su

include $(BUILD_EXECUTABLE)

# Make #!/system/bin/toolbox launchers for each tool.
#
SYMLINK := $(TARGET_OUT_EXECUTABLES)/$(LOCAL_MODULE_STEM)
$(SYMLINK): SU_BINARY := ../xbin/$(LOCAL_MODULE_STEM)
$(SYMLINK): $(LOCAL_INSTALLED_MODULE) $(LOCAL_PATH)/Android.mk
	@echo "Symlink: $@ -> $(SU_BINARY)"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf $(SU_BINARY) $@

ALL_DEFAULT_INSTALLED_MODULES += $(SYMLINK)

# We need this so that the installed files could be picked up based on the
# local module name
ALL_MODULES.$(LOCAL_MODULE).INSTALLED := \
    $(ALL_MODULES.$(LOCAL_MODULE).INSTALLED) $(SYMLINK)


## 

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := Superuser
LOCAL_SRC_FILES := $(call all-java-files-under,src)

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

##

