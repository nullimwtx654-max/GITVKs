#include <android/log.h>

#ifndef __ANDROID_API_L__
#define __ANDROID_API_L__ 21
#endif
#ifndef __ANDROID_API_L_MR1__
#define __ANDROID_API_L_MR1__ 22
#endif
#ifndef __ANDROID_API_M__
#define __ANDROID_API_M__ 23
#endif
#ifndef __ANDROID_API_N__
#define __ANDROID_API_N__ 24
#endif
#ifndef __ANDROID_API_N_MR1__
#define __ANDROID_API_N_MR1__ 25
#endif
#ifndef __ANDROID_API_O__
#define __ANDROID_API_O__ 26
#endif
#ifndef __ANDROID_API_O_MR1__
#define __ANDROID_API_O_MR1__ 27
#endif
#ifndef __ANDROID_API_P__
#define __ANDROID_API_P__ 28
#endif
#ifndef __ANDROID_API_Q__
#define __ANDROID_API_Q__ 29
#endif
#ifndef __ANDROID_API_R__
#define __ANDROID_API_R__ 30
#endif
#ifndef __ANDROID_API_S__
#define __ANDROID_API_S__ 31
#endif
#ifndef __ANDROID_API_T__
#define __ANDROID_API_T__ 33
#endif
#ifndef __ANDROID_API_U__
#define __ANDROID_API_U__ 34
#endif
#ifndef __ANDROID_API_V__
#define __ANDROID_API_V__ 35
#endif


#define TAG "NativeCore"

#if 1
#define log_print_error(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define log_print_debug(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#else
#define log_print_error(...)
#endif

#define ALOGE(...) log_print_error(__VA_ARGS__)
#define ALOGD(...) log_print_debug(__VA_ARGS__)

#ifndef SPEED_LOG_H
#define SPEED_LOG_H 1

#endif
