#include <cstring>
#include "DexFileHook.h"
#include <IO.h>
#include <BoxCore.h>
#include "UnixFileSystemHook.h"
#include "JniHook/JniHook.h"
#include <sys/stat.h>

HOOK_JNI(jobject, openDexFileNative, JNIEnv *env, jobject obj, jstring sourceName, jstring outputName, jint flags, jobject loader, jobject elements) {
    jstring effectiveSource = sourceName;
    if (sourceName != nullptr) {
        jstring redirected = IO::redirectPath(env, sourceName);
        if (redirected != nullptr) {
            effectiveSource = redirected;
        }
        const char *sourceNameC = env->GetStringUTFChars(effectiveSource, JNI_FALSE);
        if (sourceNameC != nullptr) {
            ALOGD("openDexFileNative: %s", sourceNameC);
            if (strstr(sourceNameC, "/blackbox/") != nullptr ||
                strstr(sourceNameC, "/data/") != nullptr) {
                DexFileHook::setFileReadonly(sourceNameC);
            }
            env->ReleaseStringUTFChars(effectiveSource, sourceNameC);
        }
    }
    return orig_openDexFileNative(env, obj, effectiveSource, outputName, flags, loader, elements);
}

void DexFileHook::init(JNIEnv *env) {
    if (BoxCore::getApiLevel() >= __ANDROID_API_U__) {
        const char *clazz = "dalvik/system/DexFile";
        JniHook::HookJniFun(env, clazz, "openDexFileNative",
                            "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/ClassLoader;[Ldalvik/system/DexPathList$Element;)Ljava/lang/Object;",
                            (void *) new_openDexFileNative,
                            (void **) (&orig_openDexFileNative), true);
    }
}

void DexFileHook::setFileReadonly(const char *filePath) {
    if (!filePath) return;
    struct stat fileStat;
    if (stat(filePath, &fileStat) != 0) {
        return;
    }
    if (chmod(filePath, S_IRUSR | S_IRGRP | S_IROTH) != 0) {
        chmod(filePath, S_IRUSR);
    }
}
