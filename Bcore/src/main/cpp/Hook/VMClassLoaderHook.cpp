#include <cstring>
#include "VMClassLoaderHook.h"
#include "JniHook/JniHook.h"

static bool hideXposedClass = true;

HOOK_JNI(jobject, findLoadedClass, JNIEnv *env, jobject obj, jobject class_loader, jstring name) {
    if (name && hideXposedClass) {
        const char *nameC = env->GetStringUTFChars(name, JNI_FALSE);
        if (nameC) {
            if (strstr(nameC, "de/robv/android/xposed/") ||
                strstr(nameC, "de.robv.android") ||
                strstr(nameC, "me/weishu/epic") ||
                strstr(nameC, "me.weishu.epic") ||
                strstr(nameC, "me/weishu/exposed") ||
                strstr(nameC, "me.weishu.exposed") ||
                strstr(nameC, "org/lsposed") ||
                strstr(nameC, "org.lsposed") ||
                strstr(nameC, "com/elderdrivers/riru") ||
                strstr(nameC, "com.elderdrivers.riru") ||
                strstr(nameC, "com/swift/sandhook") ||
                strstr(nameC, "com.swift.sandhook") ||
                strstr(nameC, "top/canyie/pine") ||
                strstr(nameC, "top.canyie.pine") ||
                strstr(nameC, "com/saurik/substrate") ||
                strstr(nameC, "com.saurik.substrate")) {
                env->ReleaseStringUTFChars(name, nameC);
                return nullptr;
            }
            env->ReleaseStringUTFChars(name, nameC);
        }
    }
    return orig_findLoadedClass(env, obj, class_loader, name);
}

void VMClassLoaderHook::init(JNIEnv *env) {
    const char *className = "java/lang/VMClassLoader";
    JniHook::HookJniFun(env, className, "findLoadedClass", "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;",
                        (void *) new_findLoadedClass,
                        (void **) (&orig_findLoadedClass), true);
}

void VMClassLoaderHook::hideXposed() {
    hideXposedClass = true;
}
