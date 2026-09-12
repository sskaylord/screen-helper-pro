#include <jni.h>
#include <cstdint>
#include <cstring>
#include <dlfcn.h>
#include <cstdio>
#include <android/log.h>

#define TAG "RKSupport"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

struct Offsets {
    uintptr_t GameManager;
    uintptr_t PlayerList;
    uintptr_t LocalPlayer;
    uintptr_t Health;
    uintptr_t TeamId;
    uintptr_t Transform;
    uintptr_t Position;
    uintptr_t BoneArray;
    uintptr_t ViewMatrix;
    bool resolved;
};

static Offsets g_offsets = {};

uintptr_t find_pattern(uintptr_t base, size_t size, const uint8_t* pattern, const char* mask, size_t patLen) {
    for (size_t i = 0; i <= size - patLen; i++) {
        bool found = true;
        for (size_t j = 0; j < patLen; j++) {
            if (mask[j] != '?' && *(uint8_t*)(base + i + j) != pattern[j]) { found = false; break; }
        }
        if (found) return base + i;
    }
    return 0;
}

bool resolve_offsets() {
    void* handle = dlopen("libil2cpp.so", RTLD_NOLOAD);
    if (!handle) return false;
    uintptr_t base = 0, end = 0;
    FILE* f = fopen("/proc/self/maps", "r");
    if (!f) return false;
    char line[512];
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, "libil2cpp.so") && strstr(line, "r-xp")) {
            sscanf(line, "%lx-%lx", &base, &end);
            break;
        }
    }
    fclose(f);
    if (!base) return false;
    g_offsets.GameManager = 0x1C0;
    g_offsets.PlayerList = 0x28;
    g_offsets.LocalPlayer = 0x10;
    g_offsets.Health = 0x1A4;
    g_offsets.TeamId = 0x1B0;
    g_offsets.Transform = 0x30;
    g_offsets.Position = 0x38;
    g_offsets.BoneArray = 0x48;
    g_offsets.ViewMatrix = 0x2E4;
    g_offsets.resolved = true;
    LOGI("Offsets resolved base: %lx", base);
    return true;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_renderkit_support_AssetHelper_getOffset(JNIEnv*, jclass, jint type) {
    if (!g_offsets.resolved) resolve_offsets();
    if (!g_offsets.resolved) return -1;
    switch (type) {
        case 0: return g_offsets.GameManager;
        case 1: return g_offsets.PlayerList;
        case 2: return g_offsets.LocalPlayer;
        case 3: return g_offsets.Health;
        case 4: return g_offsets.TeamId;
        case 5: return g_offsets.Transform;
        case 6: return g_offsets.Position;
        case 7: return g_offsets.BoneArray;
        case 8: return g_offsets.ViewMatrix;
        default: return -1;
    }
}
