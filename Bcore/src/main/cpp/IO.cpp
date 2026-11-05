#include "IO.h"
#include "Log.h"
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <pthread.h>

jmethodID getAbsolutePathMethodId = nullptr;

list<IO::RelocateInfo> relocate_rule;
static pthread_mutex_t relocate_mutex = PTHREAD_MUTEX_INITIALIZER;

char *replace(const char *str, const char *src, const char *dst) {
    if (!str || !src || !dst) {
        return str ? strdup(str) : nullptr;
    }
    size_t src_len = strlen(src);
    size_t dst_len = strlen(dst);
    if (src_len == 0) {
        return strdup(str);
    }
    const char *pos = str;
    int count = 0;
    while ((pos = strstr(pos, src))) {
        count++;
        pos += src_len;
    }
    ssize_t diff = (ssize_t)dst_len - (ssize_t)src_len;
    size_t result_len = (size_t)((ssize_t)strlen(str) + diff * count + 1);
    char *result = (char *) malloc(result_len);
    if (!result) {
        return strdup(str);
    }
    memset(result, 0, result_len);

    const char *left = str;
    const char *right = nullptr;

    while ((right = strstr(left, src))) {
        strncat(result, left, right - left);
        strcat(result, dst);
        right += src_len;
        left = right;
    }
    strcat(result, left);
    return result;
}

const char *IO::redirectPath(const char *__path) {
    if (!__path) {
        return __path;
    }
    if (strstr(__path, "resource-cache")) {
        ALOGD("Blocking resource-cache path: %s", __path);
        return "/dev/null";
    }
    if (strstr(__path, "@idmap")) {
        ALOGD("Blocking idmap path: %s", __path);
        return "/dev/null";
    }
    if (strstr(__path, "systemui") && (strstr(__path, ".frro") || strstr(__path, "-accent-") || strstr(__path, "-dynamic-") || strstr(__path, "-neutral-"))) {
        ALOGD("Blocking systemui problematic path: %s", __path);
        return "/dev/null";
    }
    if (strstr(__path, "data@resource-cache@")) {
        ALOGD("Blocking data@resource-cache@ pattern: %s", __path);
        return "/dev/null";
    }
    if (strstr(__path, ".frro")) {
        ALOGD("Blocking .frro file: %s", __path);
        return "/dev/null";
    }
    if (strstr(__path, "systemui")) {
        ALOGD("Blocking systemui path: %s", __path);
        return "/dev/null";
    }

    pthread_mutex_lock(&relocate_mutex);
    list<IO::RelocateInfo>::iterator iterator;
    for (iterator = relocate_rule.begin(); iterator != relocate_rule.end(); ++iterator) {
        IO::RelocateInfo info = *iterator;
        if (info.targetPath && info.relocatePath && strstr(__path, info.targetPath) && !strstr(__path, "/blackbox/")) {
            char *ret = replace(__path, info.targetPath, info.relocatePath);
            pthread_mutex_unlock(&relocate_mutex);
            return ret;
        }
    }
    pthread_mutex_unlock(&relocate_mutex);
    return __path;
}

jstring IO::redirectPath(JNIEnv *env, jstring path) {
    if (!path) {
        return nullptr;
    }
    return BoxCore::redirectPathString(env, path);
}

jobject IO::redirectPath(JNIEnv *env, jobject path) {
    if (!path) {
        return nullptr;
    }
    return BoxCore::redirectPathFile(env, path);
}

void IO::addRule(const char *targetPath, const char *relocatePath) {
    if (!targetPath || !relocatePath) {
        return;
    }
    pthread_mutex_lock(&relocate_mutex);
    IO::RelocateInfo info{};
    info.targetPath = strdup(targetPath);
    info.relocatePath = strdup(relocatePath);
    relocate_rule.push_back(info);
    pthread_mutex_unlock(&relocate_mutex);
}

void IO::init(JNIEnv *env) {
    jclass tmpFile = env->FindClass("java/io/File");
    if (tmpFile) {
        getAbsolutePathMethodId = env->GetMethodID(tmpFile, "getAbsolutePath", "()Ljava/lang/String;");
    }
}
