#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdint>
#include <cstring>
#include <string>
#include <sys/mman.h>
#include <unistd.h>

#define TAG "DispUtils"
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
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, name) && strstr(line, "r-xp")) {
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

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_ResourceLoader_nativeLoadTarget(JNIEnv* env, jclass, jstring libPath) {
    const char* path = env->GetStringUTFChars(libPath, nullptr);
    if (!path) return JNI_FALSE;

    g_target_handle = dlopen(path, RTLD_NOW);
    env->ReleaseStringUTFChars(libPath, path);

    if (!g_target_handle) {
        LOGE("dlopen failed: %s", dlerror());
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
Java_com_display_utils_ResourceLoader_nativeGetBase(JNIEnv*, jclass) {
    return (jlong)g_target_base;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_display_utils_ResourceLoader_nativeGetSize(JNIEnv*, jclass) {
    return (jlong)g_target_size;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_ResourceLoader_nativeIsReady(JNIEnv*, jclass) {
    return g_bridge_ready ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_display_utils_ResourceLoader_nativeFindSymbol(JNIEnv* env, jclass, jstring symName) {
    if (!g_target_handle) return 0;
    const char* name = env->GetStringUTFChars(symName, nullptr);
    if (!name) return 0;
    void* sym = dlsym(g_target_handle, name);
    env->ReleaseStringUTFChars(symName, name);
    return (jlong)(uintptr_t)sym;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_display_utils_ResourceLoader_nativeReadMemory(JNIEnv* env, jclass, jlong addr, jint size) {
    if (addr == 0 || size <= 0 || size > 65536) return nullptr;
    jbyteArray result = env->NewByteArray(size);
    if (!result) return nullptr;
    jbyte* buf = new jbyte[size];
    memcpy(buf, (void*)(uintptr_t)addr, size);
    env->SetByteArrayRegion(result, 0, size, buf);
    delete[] buf;
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_ResourceLoader_nativeCleanMaps(JNIEnv* env, jclass) {
    // Maps temizleme — /proc/self/maps'den izleri sil
    // Bu fonksiyon render_loop.cpp'deki hook ile birlikte çalışır
    LOGI("Maps cleanup triggered");
}
