LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
include /home/rohit/android/OpenCV-2.4.3.2-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := vision
LOCAL_SRC_FILES := vision.cpp
LOCAL_LDLIBS +=  -llog -ldl
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
 
include $(BUILD_SHARED_LIBRARY)