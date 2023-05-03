LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

#opencv library
#OPENCVROOT:= D:\opencvSeries\opencv-4.1.0-android-sdk\OpenCV-android-sdk\sdk
#OPENCVROOT:= D:\opencvSeries\opencv4_1_0_contrib
#OPENCVROOT:= D:\opencvSeries\opencv-android-sdk-with-contrib-master\opencv-android-sdk-with-contrib-master
#OPENCVROOT:= D:\opencvSeries\opencv3_4_4_contrib
##USeThis
#OPENCVROOT:= D:\iSPDWorkSpace\opencvBuild\opencv-3.4.1\opencv-3.4.1\Build\install_v7\sdk
#OPENCVROOT:= D:\iSPDWorkSpace\opencvBuild\opencv-3.4.1\opencv-3.4.1\Build3\install\sdk

#OPENCVROOT:= D:\iSPDWorkSpace\opencvBuild\opencv-4.0.0\opencv-4.0.0\platforms\android\install\sdk

#OPENCVROOT:= D:\iSPDWorkSpace\opencvBuild\opencv-3.4.15\opencv-3.4.15\platforms\android\install\sdk
#use this
OPENCVROOT:= D:\iSPDWorkSpace\opencvBuild\opencv3.4.15_64\opencv-3.4.15\opencv-3.4.15\platforms\android\install\sdk
#OPENCVROOT:= D:\opencvSeries\opencv-4.1.0-android-sdk\OpenCV-android-sdk\sdk

#OPENCVROOT:= D:\iSPDWorkSpace\opencvBuild\opencv-3.4.15-android-sdk\OpenCV-android-sdk\sdk

include ${OPENCVROOT}\native\jni\OpenCV.mk

#LOCAL_MODULE := opencv_java3
#LOCAL_SRC_FILES := libopencv_java3.so
#LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
#include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)

LOCAL_MODULE    := native-lib
#LOCAL_SHARED_LIBRARIES := opencv_java3_contrib
LOCAL_SRC_FILES := imageMapperManager.cpp \
				   imageEdgeDetect.cpp \
				   imagePDAlignment.cpp
LOCAL_LDLIBS += -llog

include $(BUILD_SHARED_LIBRARY)