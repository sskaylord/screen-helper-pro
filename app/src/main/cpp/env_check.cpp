#include <jni.h>
#include <string>
#include <cstring>
#include <vector>
#include <fstream>
#include <unistd.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <pthread.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <android/log.h>
#include <cstdarg>

#define TAG "AzureNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct GameOffsets { uintptr_t base=0; uint32_t health=0; bool valid=false; };
static GameOffsets g_offs;

struct Redir { std::string pkg, dataDir; };
static std::vector<Redir> g_redirs;
static pthread_mutex_t g_mtx = PTHREAD_MUTEX_INITIALIZER;

static std::string redir_path(const char* path) {
    if (!path) return {};
    std::string p(path);
    pthread_mutex_lock(&g_mtx);
    for (auto& r : g_redirs) {
        std::string orig = "/data/data/" + r.pkg;
        if (p.find(orig) == 0) {
            std::string out = r.dataDir + p.substr(orig.size());
            pthread_mutex_unlock(&g_mtx); return out;
        }
    }
    pthread_mutex_unlock(&g_mtx); return p;
}

typedef int (*open_fn)(const char*, int, ...);
static open_fn orig_open = nullptr;

static int my_open(const char* path, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) { va_list va; va_start(va, flags); mode = va_arg(va, mode_t); va_end(va); }
    std::string rp = redir_path(path);
    return orig_open ? orig_open(rp.c_str(), flags, mode) : ::open(rp.c_str(), flags, mode);
}

static bool arm64_hook(void* target, void* hook, void** orig_out) {
    uintptr_t tgt = (uintptr_t)target;
    size_t ps = sysconf(_SC_PAGESIZE);
    if (mprotect((void*)(tgt & ~(ps-1)), ps*2, PROT_READ|PROT_WRITE|PROT_EXEC) != 0) return false;
    uint8_t tramp[16] = { 0x50,0x00,0x00,0x58, 0x00,0x02,0x1F,0xD6, 0,0,0,0,0,0,0,0 };
    *(uint64_t*)(tramp+8) = (uint64_t)hook;
    if (orig_out) {
        uint8_t* os = (uint8_t*)mmap(nullptr, 32, PROT_READ|PROT_WRITE|PROT_EXEC, MAP_PRIVATE|MAP_ANONYMOUS, -1, 0);
        memcpy(os, (void*)tgt, 16);
        uint8_t jmp[16] = { 0x50,0x00,0x00,0x58, 0x00,0x02,0x1F,0xD6, 0,0,0,0,0,0,0,0 };
        uint64_t next = tgt + 16; *(uint64_t*)(jmp+8) = next;
        memcpy(os+16, jmp, 16);
        __builtin___clear_cache((char*)os, (char*)(os+32)); *orig_out = os;
    }
    memcpy((void*)tgt, tramp, 16);
    __builtin___clear_cache((char*)tgt, (char*)(tgt+16));
    return true;
}

static void install_io_hook() {
    void* libc = dlopen("libc.so", RTLD_NOW);
    if (!libc) return;
    void* sym = dlsym(libc, "open");
    if (sym && arm64_hook(sym, (void*)my_open, (void**)&orig_open)) LOGI("IO hook OK");
    dlclose(libc);
}

static volatile bool maps_run = false;
static void* maps_cleaner(void*) {
    prctl(PR_SET_NAME, "RenderThread", 0, 0, 0);
    while (maps_run) usleep(150000);
    return nullptr;
}

static uintptr_t get_base(const char* name) {
    std::ifstream maps("/proc/self/maps");
    std::string line;
    while (std::getline(maps, line)) {
        if (line.find(name) != std::string::npos) {
            uintptr_t b = 0; sscanf(line.c_str(), "%lx", &b);
            if (b) return b;
        }
    }
    return 0;
}

typedef void* (*fn_dom)(); typedef void** (*fn_asm)(void*,size_t*);
typedef void* (*fn_img)(void*); typedef void* (*fn_cfn)(void*,const char*,const char*);
typedef void* (*fn_ffn)(void*,const char*); typedef int (*fn_fo)(void*);

static void dump_il2cpp(uintptr_t base) {
    g_offs.base = base;
    void* h = dlopen("libil2cpp.so", RTLD_LAZY);
    if (!h) { LOGE("dlopen il2cpp fail"); return; }
    fn_dom dom = (fn_dom)dlsym(h,"il2cpp_domain_get");
    fn_asm asm_ = (fn_asm)dlsym(h,"il2cpp_domain_get_assemblies");
    fn_img img = (fn_img)dlsym(h,"il2cpp_assembly_get_image");
    fn_cfn cfn = (fn_cfn)dlsym(h,"il2cpp_class_from_name");
    fn_ffn ffn = (fn_ffn)dlsym(h,"il2cpp_class_get_field_from_name");
    fn_fo fo = (fn_fo)dlsym(h,"il2cpp_field_get_offset");
    if (!dom||!asm_||!img||!cfn) { LOGE("il2cpp API missing"); dlclose(h); return; }
    void* domain = dom(); if (!domain) { dlclose(h); return; }
    size_t cnt = 0; void** asms = asm_(domain, &cnt);
    LOGI("Assemblies: %zu", cnt);
    const char* hnames[] = {"_health","health","m_Health","HP",nullptr};
    const char* pcls[] = {"Player","PlayerController","Character","Soldier",nullptr};
    const char* ns[] = {"","Standoff","Game","Axle",nullptr};
    for (size_t i = 0; i < cnt && !g_offs.valid; i++) {
        void* image = img(asms[i]); if (!image) continue;
        for (int ni = 0; ns[ni] && !g_offs.valid; ni++)
            for (int ci = 0; pcls[ci] && !g_offs.valid; ci++) {
                void* cls = cfn(image, ns[ni], pcls[ci]);
                if (!cls || !ffn || !fo) continue;
                for (int fi = 0; hnames[fi] && !g_offs.valid; fi++) {
                    void* field = ffn(cls, hnames[fi]);
                    if (!field) continue;
                    int off = fo(field);
                    if (off > 0 && off < 0x10000) {
                        g_offs.health = off; g_offs.valid = true;
                        LOGI("Health: 0x%x (%s.%s)", off, ns[ni], pcls[ci]);
                    }
                }
            }
    }
    dlclose(h);
}

extern "C" {

JNIEXPORT void JNICALL Java_com_display_utils_engine_IORedirect_nativeAdd(
    JNIEnv* env, jclass, jstring jp, jstring jd, jstring jl) {
    const char* p = env->GetStringUTFChars(jp, nullptr);
    const char* d = env->GetStringUTFChars(jd, nullptr);
    pthread_mutex_lock(&g_mtx); g_redirs.push_back({p, d}); pthread_mutex_unlock(&g_mtx);
    LOGI("Redirect: %s -> %s", p, d);
    env->ReleaseStringUTFChars(jp, p); env->ReleaseStringUTFChars(jd, d);
    env->ReleaseStringUTFChars(jl, nullptr);
}

JNIEXPORT void JNICALL Java_com_display_utils_DisplayCore_nativeInit(
    JNIEnv* env, jclass, jstring jpath) {
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    LOGI("nativeInit: %s", path);
    prctl(PR_SET_NAME, "UnityMain", 0, 0, 0);
    maps_run = true;
    pthread_t t; pthread_create(&t, nullptr, maps_cleaner, nullptr); pthread_detach(t);
    install_io_hook();
    prctl(PR_SET_DUMPABLE, 0);
    uintptr_t base = get_base("libil2cpp.so");
    LOGI("il2cpp base: 0x%lx", base);
    if (base) dump_il2cpp(base);
    env->ReleaseStringUTFChars(jpath, path);
}

JNIEXPORT jlong JNICALL Java_com_display_utils_DisplayCore_dumpOffsets(JNIEnv*, jclass) {
    if (!g_offs.valid) { uintptr_t b = get_base("libil2cpp.so"); if (b) dump_il2cpp(b); }
    return (jlong)g_offs.health;
}

JNIEXPORT jboolean JNICALL Java_com_display_utils_DisplayCore_readMem(
    JNIEnv* env, jclass, jlong addr, jbyteArray buf, jint size) {
    jbyte* ptr = env->GetByteArrayElements(buf, nullptr);
    int fd = ::open("/proc/self/mem", O_RDONLY);
    bool ok = (fd >= 0) && (pread64(fd, ptr, size, (off64_t)addr) == size);
    if (fd >= 0) close(fd);
    env->ReleaseByteArrayElements(buf, ptr, 0);
    return ok ? JNI_TRUE : JNI_FALSE;
}

}
