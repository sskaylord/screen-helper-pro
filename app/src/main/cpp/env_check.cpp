#include "obf.h"
#include <jni.h>
#include <android/log.h>
#include <sys/prctl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <string>
#include <dlfcn.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <stdarg.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>

#define TAG OBF("PowerManager")
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

template<std::size_t N>
struct ObfStr {
    char d[N];
    constexpr ObfStr(const char (&s)[N]) : d{} {
        for(std::size_t i=0;i<N;i++) d[i]=s[i]^(0xAA^i);
    }
    void decode(char* o) const {
        for(std::size_t i=0;i<N;i++) o[i]=d[i]^(0xAA^i);
    }
};
#define OBF(s) []{constexpr ObfStr<sizeof(s)> _o(s);char _b[sizeof(s)];_o.decode(_b);return std::string(_b);}()

static bool g_stealth = false;
static bool g_debuggerDetected = false;
static uint64_t g_initTimeNs = 0;

static uint64_t getNs() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000000000ULL + ts.tv_nsec;
}

static void blockPtrace() {
    prctl(PR_SET_PTRACER, PR_SET_PTRACER_ANY, 0, 0, 0);

    auto selfStatus = OBF("/proc/self/status");
    FILE* f = fopen(selfStatus.c_str(), "r");
    if (f) {
        char line[256];
        while (fgets(line, sizeof(line), f)) {
            auto tracerKey = OBF("TracerPid:");
            if (strstr(line, tracerKey.c_str())) {
                int pid = atoi(strchr(line, ':') + 1);
                if (pid != 0) {
                    g_debuggerDetected = true;
                    g_stealth = true;
                }
            }
        }
        fclose(f);
    }
}

static void scanFridaPorts() {
    int ports[] = {27042, 27043, 8080, 8081, 9999};
    for (int p : ports) {
        int sock = socket(AF_INET, SOCK_STREAM, 0);
        if (sock < 0) continue;

        struct sockaddr_in addr;
        memset(&addr, 0, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_port = htons(p);
        addr.sin_addr.s_addr = inet_addr("127.0.0.1");

        struct timeval tv;
        tv.tv_sec = 0;
        tv.tv_usec = 50000;
        setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));

        int ret = connect(sock, (struct sockaddr*)&addr, sizeof(addr));
        close(sock);

        if (ret == 0) {
            g_stealth = true;
            LOGI("Suspicious port %d detected", p);
        }
    }
}

static void checkTimingAnomaly() {
    uint64_t now = getNs();
    uint64_t elapsed = now - g_initTimeNs;
    if (elapsed > 10000000000ULL) {
        g_stealth = true;
    }
}

// Filtered maps content cached after first generation
static char* g_sys_data = nullptr;
static size_t g_sys_data_len = 0;
static bool g_sys_ready = false;

static const char* s_sys_entries[] = {
    OBF("display_utils"), OBF("env_check"), OBF("sys_compat"), OBF("draw_utils"),
    OBF("meta_parser"), OBF("render_loop"), OBF("native_bridge"), OBF("cache_manager"),
    OBF("asset_meta"), OBF("overlay"), OBF("float_widget")
};
static const int s_sys_entry_count = 11;

static bool isSysEntry(const char* line) {
    for (int i = 0; i < s_sys_entry_count; i++) {
        if (strstr(line, s_sys_entries[i])) return true;
    }
    return false;
}

static void prepareSysData() {
    if (g_sys_data) return;
    
    FILE* f = fopen("/proc/self/maps", "r");
    if (!f) return;
    
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);
    
    char* raw = (char*)malloc(sz + 1);
    if (!raw) { fclose(f); return; }
    fread(raw, 1, sz, f);
    raw[sz] = '\0';
    fclose(f);
    
    g_sys_data = (char*)malloc(sz + 1);
    if (!g_sys_data) { free(raw); return; }
    
    char* out = g_sys_data;
    char* lineStart = raw;
    char* p = raw;
    
    while (*p) {
        if (*p == '\n') {
            *p = '\0';
            if (!isSysEntry(lineStart)) {
                size_t len = strlen(lineStart);
                memcpy(out, lineStart, len);
                out[len] = '\n';
                out += len + 1;
            }
            lineStart = p + 1;
        }
        p++;
    }
    *out = '\0';
    g_sys_data_len = out - g_sys_data;
    free(raw);
}

typedef int (*orig_open_t)(const char*, int, ...);
typedef FILE* (*orig_fopen_t)(const char*, const char*);
static orig_open_t s_base_open = nullptr;
static orig_fopen_t s_base_fopen = nullptr;

static char s_sys_res_path[64] = {0};

static void prepareSysResource() {
    if (s_sys_res_path[0]) return;
    prepareSysData();
    if (!g_sys_data) return;
    
    int fd = memfd_create("maps", MFD_CLOEXEC);
    if (fd < 0) return;
    write(fd, g_sys_data, g_sys_data_len);
    lseek(fd, 0, SEEK_SET);
    
    snprintf(s_sys_res_path, sizeof(s_sys_res_path), "/proc/self/fd/%d", fd);
}

static int sys_open_proxy(const char* path, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, mode_t);
        va_end(args);
    }
    
    if (path && strstr(path, "/proc/self/maps")) {
        prepareSysResource();
        if (s_sys_res_path[0]) {
            return s_base_open(s_sys_res_path, flags, mode);
        }
    }
    
    if (flags & O_CREAT)
        return s_base_open(path, flags, mode);
    return s_base_open(path, flags);
}

static FILE* sys_fopen_proxy(const char* path, const char* mode) {
    if (path && strstr(path, "/proc/self/maps")) {
        prepareSysResource();
        if (s_sys_res_path[0]) {
            return s_base_fopen(s_sys_res_path, mode);
        }
    }
    return s_base_fopen(path, mode);
}

extern "C" bool rt_patch_entry(void* target, void* replacement, void** backup);

static void initSysCompat() {
    if (g_sys_ready) return;
    
    void* openAddr = dlsym(RTLD_NEXT, "open");
    void* fopenAddr = dlsym(RTLD_NEXT, "fopen");
    
    if (openAddr) {
        rt_patch_entry(openAddr, (void*)sys_open_proxy, (void**)&s_base_open);
    }
    if (fopenAddr) {
        rt_patch_entry(fopenAddr, (void*)sys_fopen_proxy, (void**)&s_base_fopen);
    }
    
    g_sys_ready = true;
    LOGI("Sys compat ready");
}

static void cleanProcMaps() {
    initSysCompat();
}

static void checkXposedArtifacts() {
    auto xposedClass = OBF("de.robv.android.xposed.XposedBridge");
    auto xposedFile1 = OBF("/system/framework/XposedBridge.jar");
    auto xposedFile2 = OBF("/system/app/Superuser.apk");

    FILE* f = fopen(xposedFile1.c_str(), "r");
    if (f) { fclose(f); g_stealth = true; }

    f = fopen(xposedFile2.c_str(), "r");
    if (f) { fclose(f); g_stealth = true; }
}

static void checkRootArtifacts() {
    const char* paths[] = {
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    };

    for (auto p : paths) {
        if (access(p, F_OK) == 0) {
            g_stealth = true;
            break;
        }
    }
}

__attribute__((constructor))
void compat_init_runtime() {
    g_initTimeNs = getNs();

    blockPtrace();
    scanFridaPorts();
    checkXposedArtifacts();
    checkRootArtifacts();
    checkTimingAnomaly();

    LOGI("Env check complete stealth=%d", g_stealth ? 1 : 0);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_DisplaySurface_isStealth(JNIEnv*, jclass) {
    return g_stealth ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetLoader_nativeFullScan(JNIEnv*, jclass) {
    blockPtrace();
    scanFridaPorts();
    checkXposedArtifacts();
    checkRootArtifacts();
    checkTimingAnomaly();
    cleanProcMaps();
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetLoader_nativeCleanMaps(JNIEnv*, jclass) {
    cleanProcMaps();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeIsDebuggerPresent(JNIEnv*, jclass) {
    return g_debuggerDetected ? JNI_TRUE : JNI_FALSE;
}
