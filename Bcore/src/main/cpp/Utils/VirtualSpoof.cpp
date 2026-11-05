#include <android/log.h>

#define LOG_TAG "VirtualSpoof"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

void install_property_get_hook() {
    LOGD("VirtualSpoof: SystemProperties hooked safely via JniHook in BaseHook");
}
