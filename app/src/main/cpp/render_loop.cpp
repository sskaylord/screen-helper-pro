#include "obf.h"
#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstdint>
#include <cstdlib>
#include <ctime>
#include <cstring>
#include <cmath>
#include <ctime>

#define TAG OBF("InputDispatcher")
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

template<std::size_t N>
struct ObfStr {
    char d[N];
    constexpr ObfStr(const char (&s)[N]) : d{} {
        for(std::size_t i=0;i<N;i++) d[i]=s[i]^(0xDD^i);
    }
    void decode(char* o) const {
        for(std::size_t i=0;i<N;i++) o[i]=d[i]^(0xDD^i);
    }
};
#define OBF(s) []{constexpr ObfStr<sizeof(s)> _o(s);char _b[sizeof(s)];_o.decode(_b);return std::string(_b);}()

struct Vec3 { float x, y, z; };

struct PlayerData {
    Vec3 pos;
    float health;
    int team;
    bool alive;
    bool valid;
    uint64_t lastRead;
};

static PlayerData g_data[64];
static int g_count = 0;
static float g_viewMat[16] = {};
static uintptr_t g_baseAddr = 0;
static bool g_running = false;
static uint64_t g_frameIdx = 0;

extern "C" void drawUtils_updatePlayers(const float* viewMat, const void* players, int count);

static void pushToRenderer() {
    if (!g_running || g_count <= 0) return;
    drawUtils_updatePlayers(g_viewMat, g_data, g_count);
}

static uintptr_t g_offGM = 0;
static uintptr_t g_offPL = 0;
static uintptr_t g_offLP = 0;
static uintptr_t g_offHP = 0;
static uintptr_t g_offTM = 0;
static uintptr_t g_offTR = 0;
static uintptr_t g_offPS = 0;
static uintptr_t g_offBN = 0;
static uintptr_t g_offVM = 0;
static uintptr_t g_offEL = 0;
static uintptr_t g_offEC = 0;

static struct timespec g_t0, g_t1;

static inline uint64_t nowNs() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000000000ULL + ts.tv_nsec;
}

static inline float safeReadFloat(uintptr_t addr) {
    if (addr < 0x10000) return 0.f;
    volatile float* p = (volatile float*)addr;
    return *p;
}

static inline int safeReadInt(uintptr_t addr) {
    if (addr < 0x10000) return 0;
    volatile int* p = (volatile int*)addr;
    return *p;
}

static inline uintptr_t safeReadPtr(uintptr_t addr) {
    if (addr < 0x10000) return 0;
    volatile uintptr_t* p = (volatile uintptr_t*)addr;
    return *p;
}

static inline Vec3 safeReadVec3(uintptr_t addr) {
    Vec3 v = {0, 0, 0};
    if (addr < 0x10000) return v;
    volatile float* p = (volatile float*)addr;
    v.x = p[0]; v.y = p[1]; v.z = p[2];
    return v;
}

static volatile uint32_t g_dummy_sink = 0;

static inline void dummyRead(uintptr_t base, size_t range) {
    if (!base || range == 0) return;
    uintptr_t addr = base + (rand() % range);
    if (addr > 0x10000) {
        volatile uint32_t* p = (volatile uint32_t*)addr;
        g_dummy_sink += *p;
    }
}

static int g_shuffle_indices[64];
static bool g_shuffle_init = false;

static void shuffleIndices(int count) {
    if (!g_shuffle_init) {
        srand((unsigned)time(nullptr));
        g_shuffle_init = true;
    }
    for (int i = 0; i < count; i++) g_shuffle_indices[i] = i;
    for (int i = count - 1; i > 0; i--) {
        int j = rand() % (i + 1);
        int tmp = g_shuffle_indices[i];
        g_shuffle_indices[i] = g_shuffle_indices[j];
        g_shuffle_indices[j] = tmp;
    }
}

static void readPlayers() {
    if (!g_baseAddr || g_offGM == 0) return;

    uintptr_t gm = safeReadPtr(g_baseAddr + g_offGM);
    if (!gm) return;

    uintptr_t el = safeReadPtr(gm + g_offEL);
    if (!el) return;

    int count = safeReadInt(gm + g_offEC);
    if (count <= 0 || count > 64) count = 0;

    g_count = count;

    for (int i = 0; i < count; i++) {
        uintptr_t entity = safeReadPtr(el + i * 8);
        if (!entity) { g_data[i].valid = false; continue; }

        float hp = safeReadFloat(entity + g_offHP);
        int team = safeReadInt(entity + g_offTM);
        uintptr_t tr = safeReadPtr(entity + g_offTR);
        Vec3 pos = {0, 0, 0};
        if (tr) pos = safeReadVec3(tr + g_offPS);

        g_data[i].pos = pos;
        g_data[i].health = hp;
        g_data[i].team = team;
        g_data[i].alive = hp > 0.f;
        g_data[i].valid = true;
        g_data[i].lastRead = nowNs();
    }

    uintptr_t vmAddr = safeReadPtr(gm + g_offVM);
    if (vmAddr) {
        volatile float* vp = (volatile float*)vmAddr;
        for (int i = 0; i < 16; i++) g_viewMat[i] = vp[i];
    }
}

static void normalizeTiming(uint64_t durNs) {
    // Add random jitter 2-5ms to break fixed timing patterns
    uint32_t jitterUs = 2000 + (rand() % 3000);
    struct timespec ts;
    ts.tv_sec = 0;
    ts.tv_nsec = jitterUs * 1000L;
    nanosleep(&ts, nullptr);
    
    if (durNs > 2000000) {
        // Overhead minimized for AC timing evasion
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetLoader_nativeStartLoop(JNIEnv*, jclass, jlong base) {
    g_baseAddr = (uintptr_t)base;
    g_running = true;
    g_frameIdx = 0;
    memset(g_data, 0, sizeof(g_data));
    LOGI("Loop started base=%lx", g_baseAddr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetLoader_nativeStopLoop(JNIEnv*, jclass) {
    g_running = false;
    g_baseAddr = 0;
    g_count = 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetLoader_nativeTick(JNIEnv*, jclass) {
    if (!g_running) return;

    g_frameIdx++;

    if (g_frameIdx % 3 != 0) return;

    uint64_t t0 = nowNs();
    readPlayers();
    uint64_t t1 = nowNs();

    normalizeTiming(t1 - t0);
    pushToRenderer();
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetLoader_nativeSetOffsets(JNIEnv*, jclass,
    jlong gm, jlong pl, jlong lp, jlong hp, jlong tm,
    jlong tr, jlong ps, jlong bn, jlong vm, jlong el, jlong ec) {
    g_offGM = (uintptr_t)gm;
    g_offPL = (uintptr_t)pl;
    g_offLP = (uintptr_t)lp;
    g_offHP = (uintptr_t)hp;
    g_offTM = (uintptr_t)tm;
    g_offTR = (uintptr_t)tr;
    g_offPS = (uintptr_t)ps;
    g_offBN = (uintptr_t)bn;
    g_offVM = (uintptr_t)vm;
    g_offEL = (uintptr_t)el;
    g_offEC = (uintptr_t)ec;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_display_utils_AssetLoader_nativeGetPlayerData(JNIEnv* env, jclass, jint idx) {
    if (idx < 0 || idx >= 64 || !g_data[idx].valid) return nullptr;

    jfloatArray arr = env->NewFloatArray(7);
    if (!arr) return nullptr;

    jfloat buf[7] = {
        g_data[idx].pos.x,
        g_data[idx].pos.y,
        g_data[idx].pos.z,
        g_data[idx].health,
        (float)g_data[idx].team,
        g_data[idx].alive ? 1.f : 0.f,
        g_data[idx].valid ? 1.f : 0.f
    };

    env->SetFloatArrayRegion(arr, 0, 7, buf);
    return arr;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_display_utils_AssetLoader_nativeGetViewMatrix(JNIEnv* env, jclass) {
    jfloatArray arr = env->NewFloatArray(16);
    if (!arr) return nullptr;
    env->SetFloatArrayRegion(arr, 0, 16, g_viewMat);
    return arr;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_display_utils_AssetLoader_nativeGetPlayerCount(JNIEnv*, jclass) {
    return g_count;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeIsRunning(JNIEnv*, jclass) {
    return g_running ? JNI_TRUE : JNI_FALSE;
}

__attribute__((constructor))
void render_loop_init() {
    memset(g_data, 0, sizeof(g_data));
    memset(g_viewMat, 0, sizeof(g_viewMat));
}
