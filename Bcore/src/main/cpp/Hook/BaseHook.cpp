#include <cstring>
#include <cstdlib>
#include "BaseHook.h"
#include "JniHook/JniHook.h"
#include "Log.h"

struct SafeSpoofProp {
    const char *key;
    const char *value;
};

static const SafeSpoofProp kSafeProps[] = {
    {"ro.build.type", "user"},
    {"ro.build.tags", "release-keys"},
    {"ro.debuggable", "0"},
    {"ro.secure", "1"},
    {"ro.boot.verifiedbootstate", "green"},
    {"ro.boot.flash.locked", "1"},
    {"ro.boot.vbmeta.device_state", "locked"},
    {"ro.boot.veritymode", "enforcing"},
    {"ro.warranty_bit", "0"},
    {"ro.boot.warranty_bit", "0"},
    {"ro.kernel.qemu", "0"},
    {"ro.boot.qemu", "0"},
    {nullptr, nullptr}
};

static const char *lookupSpoofedProp(const char *key) {
    if (!key) return nullptr;
    const char *spoofSpecs = getenv("CLONEBOX_SPOOF_SPECS");
    if (spoofSpecs && spoofSpecs[0] == '1') {
        if (strcmp(key, "ro.boot.ddr_size") == 0 ||
            strcmp(key, "sys.ram.size") == 0 ||
            strcmp(key, "ro.ram.size") == 0) {
            return "6GB";
        }
        if (strcmp(key, "ro.miui.notch") == 0) {
            return "1";
        }
    }
    for (int i = 0; kSafeProps[i].key != nullptr; ++i) {
        if (strcmp(key, kSafeProps[i].key) == 0) {
            return kSafeProps[i].value;
        }
    }
    return nullptr;
}

HOOK_JNI(jstring, native_get_1, JNIEnv *env, jclass clazz, jstring keyJ) {
    if (keyJ) {
        const char *key = env->GetStringUTFChars(keyJ, JNI_FALSE);
        if (key) {
            const char *spoofed = lookupSpoofedProp(key);
            env->ReleaseStringUTFChars(keyJ, key);
            if (spoofed) {
                return env->NewStringUTF(spoofed);
            }
        }
    }
    return orig_native_get_1(env, clazz, keyJ);
}

HOOK_JNI(jstring, native_get_2, JNIEnv *env, jclass clazz, jstring keyJ, jstring defJ) {
    if (keyJ) {
        const char *key = env->GetStringUTFChars(keyJ, JNI_FALSE);
        if (key) {
            const char *spoofed = lookupSpoofedProp(key);
            env->ReleaseStringUTFChars(keyJ, key);
            if (spoofed) {
                return env->NewStringUTF(spoofed);
            }
        }
    }
    return orig_native_get_2(env, clazz, keyJ, defJ);
}

void BaseHook::init(JNIEnv *env) {
    const char *className = "android/os/SystemProperties";
    try {
        JniHook::HookJniFun(env, className, "native_get", "(Ljava/lang/String;)Ljava/lang/String;",
                            (void *) new_native_get_1, (void **) (&orig_native_get_1), true);
    } catch (...) {
    }
    try {
        JniHook::HookJniFun(env, className, "native_get", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                            (void *) new_native_get_2, (void **) (&orig_native_get_2), true);
    } catch (...) {
    }
}
