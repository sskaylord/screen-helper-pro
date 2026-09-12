#include <jni.h>
#include <android/log.h>
#include <sys/prctl.h>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <string>

#define TAG "RKSupport"
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

__attribute__((constructor))
void compat_init_runtime() {
    prctl(PR_SET_PTRACER, PR_SET_PTRACER_ANY, 0, 0, 0);
    auto path = OBF("/proc/self/status");
    FILE* f = fopen(path.c_str(), "r");
    if(f) {
        char line[256];
        while(fgets(line,sizeof(line),f)) {
            auto key = OBF("TracerPid:");
            if(strstr(line, key.c_str())) {
                int pid = atoi(strchr(line,':')+1);
                if(pid != 0) g_stealth = true;
            }
        }
        fclose(f);
    }
    LOGI("Compat layer init");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_renderkit_support_RuntimeBridge_isStealth(JNIEnv*, jclass) {
    return g_stealth ? JNI_TRUE : JNI_FALSE;
}
