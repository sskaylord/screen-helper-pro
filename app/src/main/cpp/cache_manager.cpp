#include <jni.h>
#include <cstdint>
#include <cstring>
#include <sys/uio.h>
#include <unistd.h>

ssize_t safe_read_mem(pid_t pid, uintptr_t addr, void* buf, size_t len) {
    struct iovec local  = { buf, len };
    struct iovec remote = { (void*)addr, len };
    return process_vm_readv(pid, &local, 1, &remote, 1, 0);
}

struct PlayerCache {
    float health;
    int team;
    float posX, posY, posZ;
    uint64_t lastUpdate;
    bool valid;
};

static PlayerCache g_cache[64];
static uint64_t g_frameCount = 0;

bool should_update_cache() {
    return (g_frameCount++ % 3) == 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_renderkit_support_AssetHelper_updateCache(JNIEnv*, jclass, jint idx,
    jfloat hp, jint team, jfloat x, jfloat y, jfloat z) {
    if (idx < 0 || idx >= 64) return;
    g_cache[idx].health   = hp;
    g_cache[idx].team     = team;
    g_cache[idx].posX     = x;
    g_cache[idx].posY     = y;
    g_cache[idx].posZ     = z;
    g_cache[idx].valid    = true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_renderkit_support_AssetHelper_shouldUpdate(JNIEnv*, jclass) {
    return should_update_cache() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_renderkit_support_AssetHelper_getCachedHealth(JNIEnv*, jclass, jint idx) {
    if (idx < 0 || idx >= 64 || !g_cache[idx].valid) return -1.f;
    return g_cache[idx].health;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_renderkit_support_AssetHelper_getCachedTeam(JNIEnv*, jclass, jint idx) {
    if (idx < 0 || idx >= 64 || !g_cache[idx].valid) return -1;
    return g_cache[idx].team;
}
