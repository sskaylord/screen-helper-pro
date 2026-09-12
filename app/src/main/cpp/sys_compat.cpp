#include <cstdint>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>
#include <cstdlib>
#include <ctime>

static size_t g_page_size = 0;

static void ensure_page_size() {
    if (!g_page_size) g_page_size = sysconf(_SC_PAGESIZE);
}

static bool make_writable(void* addr, size_t len) {
    ensure_page_size();
    uintptr_t page = (uintptr_t)addr & ~(g_page_size - 1);
    return mprotect((void*)page, len + ((uintptr_t)addr - page),
        PROT_READ | PROT_WRITE | PROT_EXEC) == 0;
}

static bool make_executable(void* addr, size_t len) {
    ensure_page_size();
    uintptr_t page = (uintptr_t)addr & ~(g_page_size - 1);
    return mprotect((void*)page, len + ((uintptr_t)addr - page),
        PROT_READ | PROT_EXEC) == 0;
}

static void write_absolute_jump(void* from, void* to) {
    uint32_t* insns = (uint32_t*)from;
    insns[0] = 0x58000050;
    insns[1] = 0xD61F0200;
    *(uint64_t*)&insns[2] = (uint64_t)to;
}

static uint8_t* g_trampoline_pool = nullptr;
static size_t g_trampoline_offset = 0;
static size_t g_pool_size = 4096;
static uint8_t g_original_bytes[256][16];
static void* g_targets[256];
static int g_cnt = 0;

static bool init_pool() {
    if (g_trampoline_pool) return true;

    srand((unsigned)time(nullptr));

    g_trampoline_pool = (uint8_t*)mmap(
        nullptr, g_pool_size,
        PROT_READ | PROT_WRITE | PROT_EXEC,
        MAP_PRIVATE | MAP_ANONYMOUS,
        -1, 0
    );

    if (g_trampoline_pool == MAP_FAILED) {
        g_trampoline_pool = nullptr;
        return false;
    }

    memset(g_trampoline_pool, 0, g_pool_size);
    return true;
}

extern "C" bool rt_patch_entry(void* target, void* replacement, void** backup) {
    if (!init_pool()) return false;
    if (g_trampoline_offset + 64 > g_pool_size) return false;
    if (g_cnt >= 256) return false;

    uint8_t* tramp = &g_trampoline_pool[g_trampoline_offset];
    g_trampoline_offset += 64;

    memcpy(g_original_bytes[g_cnt], target, 16);
    g_targets[g_cnt] = target;
    g_cnt++;

    memcpy(tramp, target, 16);
    write_absolute_jump(tramp + 16, (uint8_t*)target + 16);

    if (!make_writable(target, 16)) return false;
    write_absolute_jump(target, replacement);
    make_executable(target, 16);

    __builtin___clear_cache((char*)target, (char*)target + 16);
    __builtin___clear_cache((char*)tramp, (char*)tramp + 32);

    *backup = tramp;
    return true;
}

extern "C" void rt_restore_hook(int index) {
    if (index < 0 || index >= g_cnt) return;
    void* target = g_targets[index];
    if (!target) return;
    make_writable(target, 16);
    memcpy(target, g_original_bytes[index], 16);
    make_executable(target, 16);
    __builtin___clear_cache((char*)target, (char*)target + 16);
}

extern "C" void rt_restore_all() {
    for (int i = 0; i < g_cnt; i++) {
        rt_restore_hook(i);
    }
}

extern "C" bool rt_is_hooked(void* addr) {
    for (int i = 0; i < g_cnt; i++) {
        if (g_targets[i] == addr) return true;
    }
    return false;
}

extern "C" int rt_get_cnt() { return g_cnt; }
extern "C" const uint8_t* rt_get_original(int index) {

    if (index < 0 || index >= g_cnt) return nullptr;
    return g_original_bytes[index];
}
