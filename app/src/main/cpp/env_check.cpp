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
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>

#define TAG "DispUtils"
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

static void cleanProcMaps() {
    auto mapsPath = OBF("/proc/self/maps");
    auto tempPath = OBF("/data/local/tmp/.dc");

    FILE* in = fopen(mapsPath.c_str(), "r");
    if (!in) return;

    FILE* out = fopen(tempPath.c_str(), "w");
    if (!out) { fclose(in); return; }

    char line[512];
    auto filterLib = OBF("display_utils");
    auto filterGuard = OBF("env_check");
    auto filterCompat = OBF("sys_compat");
    auto filterDraw = OBF("draw_utils");
    auto filterMeta = OBF("meta_parser");
    auto filterRender = OBF("render_loop");
    auto filterBridge = OBF("native_bridge");
    auto filterCache = OBF("cache_manager");
    auto filterAsset = OBF("asset_meta");

    while (fgets(line, sizeof(line), in)) {
        if (strstr(line, filterLib.c_str())) continue;
        if (strstr(line, filterGuard.c_str())) continue;
        if (strstr(line, filterCompat.c_str())) continue;
        if (strstr(line, filterDraw.c_str())) continue;
        if (strstr(line, filterMeta.c_str())) continue;
        if (strstr(line, filterRender.c_str())) continue;
        if (strstr(line, filterBridge.c_str())) continue;
        if (strstr(line, filterCache.c_str())) continue;
        if (strstr(line, filterAsset.c_str())) continue;
        fputs(line, out);
    }

    fclose(in);
    fclose(out);

    rename(tempPath.c_str(), mapsPath.c_str());
    unlink(tempPath.c_str());
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
    auto paths[] = {
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
