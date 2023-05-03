#APP_STL := c++_static
#APP_STL := c++_shared
#use this
APP_STL := gnustl_static

APP_CPPFLAGS := -frtti -fexceptions

#ues this
APP_CPPFLAGS += -std=c++11
#APP_CPPFLAGS += -std=gnu++11

APP_PLATFORM := android-21

#use this
#APP_ABI := armeabi-v7a
APP_ABI := arm64-v8a