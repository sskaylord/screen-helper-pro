#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdint>
#include <cstring>
#include <string>
#include <sys/mman.h>
#include <unistd.h>

#define TAG "SurfaceControl"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

template<std::size_t N>
struct ObfStr {
    char d[N];
    constexpr ObfStr(const char (&s)[N]) : d{} {
        for(std::size_t i=0;i<N;i++) d[i]=s[i]^(0xBB^i);
    }
    void decode(char* o) const {
        for(std::size_t i=0;i<N;i++) o[i]=d[i]^(0xBB^i);
    }
};
#define OBF(s) []{constexpr ObfStr<sizeof(s)> _o(s);char _b[sizeof(s)];_o.decode(_b);return std::string(_b);}()

static void* g_target_handle = nullptr;
static uintptr_t g_target_base = 0;
static size_t g_target_size = 0;
static bool g_bridge_ready = false;

static uintptr_t find_module_base(const char* name) {
    auto mapsPath = OBF("/proc/self/maps");
    FILE* f = fopen(mapsPath.c_str(), "r");
    if (!f) return 0;
    char line[512];
    uintptr_t base = 0;
    auto rxpFlag = OBF("r-xp");
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, name) && strstr(line, rxpFlag.c_str())) {
            sscanf(line, "%lx", &base);
            break;
        }
    }
    fclose(f);
    return base;
}

static size_t find_module_size(uintptr_t base, const char* name) {
    auto mapsPath = OBF("/proc/self/maps");
    FILE* f = fopen(mapsPath.c_str(), "r");
    if (!f) return 0;
    char line[512];
    uintptr_t end = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, name)) {
            uintptr_t s, e;
            sscanf(line, "%lx-%lx", &s, &e);
            if (e > end) end = e;
        }
    }
    fclose(f);
    return end > base ? end - base : 0;
}


// Forward declarations from meta_parser
extern bool loadMetaFromMemory(uintptr_t addr, size_t sz);
extern bool resolveAllOffsets();

static bool g_autoDumpDone = false;

static bool scanMetadataRegion(uintptr_t moduleBase, size_t moduleSize) {
    // Scan for IL2CPP metadata magic: 0xFAB11BAF
    const uint32_t MAGIC = 0xFAB11BAF;
    const uint8_t* ptr = (const uint8_t*)moduleBase;
    const uint8_t* end = ptr + moduleSize - sizeof(uint32_t);
    
    for (; ptr < end; ptr += 4) {
        if (*(uint32_t*)ptr == MAGIC) {
            // Found potential metadata header, try to parse
            // Metadata size is at offset 4 (int32) in some versions
            // Try reasonable sizes: 1MB-32MB range
            size_t trySizes[] = { 2*1024*1024, 4*1024*1024, 8*1024*1024, 16*1024*1024 };
            for (size_t sz : trySizes) {
                if ((uintptr_t)ptr + sz > moduleBase + moduleSize) continue;
                if (loadMetaFromMemory((uintptr_t)ptr, sz)) {
                    LOGI("Auto-dump: metadata found at offset %lx", (uintptr_t)ptr - moduleBase);
                    return true;
                }
            }
        }
    }
    return false;
}

static bool initAutoDumper() {
    if (g_autoDumpDone) return true;
    
    auto il2cppName = OBF("libil2cpp.so");
    uintptr_t base = find_module_base(il2cppName.c_str());
    if (!base) {
        LOGI("Auto-dump: libil2cpp.so not found yet");
        return false;
    }
    
    size_t sz = find_module_size(base, il2cppName.c_str());
    if (!sz) {
        LOGI("Auto-dump: could not determine module size");
        return false;
    }
    
    LOGI("Auto-dump: libil2cpp.so base=%lx size=%zu", base, sz);
    
    if (scanMetadataRegion(base, sz)) {
        g_autoDumpDone = true;
        LOGI("Auto-dump: SUCCESS - offsets resolved");
        return true;
    }
    
    LOGI("Auto-dump: metadata signature not found in module");
    return false;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeLoadTarget(JNIEnv* env, jclass, jstring libPath) {
    const char* path = env->GetStringUTFChars(libPath, nullptr);
    if (!path) return JNI_FALSE;
    g_target_handle = dlopen(path, RTLD_NOW);
    env->ReleaseStringUTFChars(libPath, path);
    if (!g_target_handle) {
        LOGE("Load failed: %s", dlerror());
        return JNI_FALSE;
    }
    auto libName = OBF("libil2cpp.so");
    g_target_base = find_module_base(libName.c_str());
    g_target_size = find_module_size(g_target_base, libName.c_str());
    if (g_target_base == 0) {
        LOGE("Module base not found");
        return JNI_FALSE;
    }
    g_bridge_ready = true;
    LOGI("Bridge ready base=%lx size=%zx", g_target_base, g_target_size);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_display_utils_AssetLoader_nativeGetBase(JNIEnv*, jclass) {
    return (jlong)g_target_base;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_display_utils_AssetLoader_nativeGetSize(JNIEnv*, jclass) {
    return (jlong)g_target_size;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeIsReady(JNIEnv*, jclass) {
    return g_bridge_ready ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_display_utils_AssetLoader_nativeFindSymbol(JNIEnv* env, jclass, jstring symName) {
    if (!g_target_handle) return 0;
    const char* name = env->GetStringUTFChars(symName, nullptr);
    if (!name) return 0;
    void* sym = dlsym(g_target_handle, name);
    env->ReleaseStringUTFChars(symName, name);
    return (jlong)(uintptr_t)sym;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_display_utils_AssetLoader_nativeReadMemory(JNIEnv* env, jclass, jlong addr, jint size) {
    if (addr == 0 || size <= 0 || size > 65536) return nullptr;
    jbyteArray result = env->NewByteArray(size);
    if (!result) return nullptr;
    jbyte* buf = new jbyte[size];
    memcpy(buf, (void*)(uintptr_t)addr, size);
    env->SetByteArrayRegion(result, 0, size, buf);
    delete[] buf;
    return result;
}

extern "C" int rt_get_cnt();

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetLoader_nativeEmergencyRestore(JNIEnv*, jclass) {
    extern void rt_restore_all();
    rt_restore_all();
    LOGI("Emergency restore done");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_display_utils_AssetLoader_nativeGetHookCount(JNIEnv*, jclass) {
    return rt_get_cnt();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeInitAutoDumper(JNIEnv*, jclass) {
    return initAutoDumper() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeIsDumpReady(JNIEnv*, jclass) {
    return g_autoDumpDone ? JNI_TRUE : JNI_FALSE;
}
