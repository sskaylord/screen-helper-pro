#include <cstdint>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>

static size_t PAGE_SIZE = 0;
static void ensure_page_size() {
    if(!PAGE_SIZE) PAGE_SIZE = sysconf(_SC_PAGESIZE);
}

static bool make_writable(void* addr, size_t len) {
    ensure_page_size();
    uintptr_t page = (uintptr_t)addr & ~(PAGE_SIZE-1);
    return mprotect((void*)page, len + ((uintptr_t)addr - page), PROT_READ|PROT_WRITE|PROT_EXEC) == 0;
}

static bool make_executable(void* addr, size_t len) {
    ensure_page_size();
    uintptr_t page = (uintptr_t)addr & ~(PAGE_SIZE-1);
    return mprotect((void*)page, len + ((uintptr_t)addr - page), PROT_READ|PROT_EXEC) == 0;
}

static void write_absolute_jump(void* from, void* to) {
    uint32_t* insns = (uint32_t*)from;
    insns[0] = 0x58000050;
    insns[1] = 0xD61F0200;
    *(uint64_t*)&insns[2] = (uint64_t)to;
}

static uint8_t g_trampoline_pool[4096] __attribute__((aligned(4096)));
static size_t g_trampoline_offset = 0;

extern "C" bool rt_patch_entry(void* target, void* replacement, void** backup) {
    if(g_trampoline_offset + 64 > sizeof(g_trampoline_pool)) return false;
    uint8_t* tramp = &g_trampoline_pool[g_trampoline_offset];
    g_trampoline_offset += 64;
    memcpy(tramp, target, 16);
    write_absolute_jump(tramp + 16, (uint8_t*)target + 16);
    if(!make_writable(target, 16)) return false;
    write_absolute_jump(target, replacement);
    make_executable(target, 16);
    __builtin___clear_cache((char*)target, (char*)target+16);
    __builtin___clear_cache((char*)tramp, (char*)tramp+32);
    *backup = tramp;
    return true;
}
