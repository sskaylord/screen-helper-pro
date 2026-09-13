#include <jni.h>
#include <cstdint>
#include <cstring>
#include <sys/uio.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstdlib>
#include <ctime>

// Stealth memory read - /proc/pid/mem with scatter pattern
static int s_mem_fd = -1;
static pid_t s_cached_pid = 0;

ssize_t safe_read_mem(pid_t pid, uintptr_t addr, void* buf, size_t len) {
    // Open /proc/pid/mem lazily, cache fd
    if (s_mem_fd < 0 || s_cached_pid != pid) {
        if (s_mem_fd >= 0) close(s_mem_fd);
        char path[64];
        __builtin_snprintf(path, sizeof(path), "/proc/%d/mem", pid);
        s_mem_fd = open(path, O_RDONLY);
        s_cached_pid = pid;
        if (s_mem_fd < 0) return -1;
    }
    
    // Scatter read with small chunks + jitter to avoid pattern detection
    ssize_t total = 0;
    size_t chunk_max = 256;
    while ((size_t)total < len) {
        size_t remaining = len - total;
        size_t chunk = remaining < chunk_max ? remaining : (chunk_max - (rand() % 64));
        off_t offset = addr + total;
        ssize_t rd = pread(s_mem_fd, (char*)buf + total, chunk, offset);
        if (rd <= 0) break;
        total += rd;
        // Micro-jitter between chunks
        struct timespec ts = {0, (long)(rand() % 50000)};
        nanosleep(&ts, nullptr);
    }
    return total;
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
Java_com_display_utils_AssetHelper_updateCache(JNIEnv*, jclass, jint idx,
    jfloat hp, jint team, jfloat x, jfloat y, jfloat z) {
    if (idx < 0 || idx >= 64) return;
    g_cache[idx].health = hp;
    g_cache[idx].team = team;
    g_cache[idx].posX = x;
    g_cache[idx].posY = y;
    g_cache[idx].posZ = z;
    g_cache[idx].valid = true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetHelper_shouldUpdate(JNIEnv*, jclass) {
    return should_update_cache() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_display_utils_AssetHelper_getCachedHealth(JNIEnv*, jclass, jint idx) {
    if (idx < 0 || idx >= 64 || !g_cache[idx].valid) return -1.f;
    return g_cache[idx].health;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_display_utils_AssetHelper_getCachedTeam(JNIEnv*, jclass, jint idx) {
    if (idx < 0 || idx >= 64 || !g_cache[idx].valid) return -1;
    return g_cache[idx].team;
}
