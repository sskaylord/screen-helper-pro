#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cstdlib>

#define TAG "DispUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

template<std::size_t N>
struct ObfStr {
    char d[N];
    constexpr ObfStr(const char (&s)[N]) : d{} {
        for(std::size_t i=0;i<N;i++) d[i]=s[i]^(0xCC^i);
    }
    void decode(char* o) const {
        for(std::size_t i=0;i<N;i++) o[i]=d[i]^(0xCC^i);
    }
};
#define OBF(s) []{constexpr ObfStr<sizeof(s)> _o(s);char _b[sizeof(s)];_o.decode(_b);return std::string(_b);}()

#pragma pack(push, 1)
struct MetaHeader {
    uint32_t magic;
    uint32_t version;
    uint32_t strOffset;
    uint32_t strSize;
    uint32_t typeDefOffset;
    uint32_t typeDefCount;
    uint32_t fieldDefOffset;
    uint32_t fieldDefCount;
    uint32_t methodDefOffset;
    uint32_t methodDefCount;
    uint32_t paramDefOffset;
    uint32_t paramDefCount;
    uint32_t propDefOffset;
    uint32_t propDefCount;
};

struct TypeDefEntry {
    uint32_t nameIdx;
    uint32_t namespaceIdx;
    uint32_t parentIdx;
    uint32_t fieldStart;
    uint32_t fieldCount;
    uint32_t methodStart;
    uint32_t methodCount;
    uint32_t flags;
};

struct FieldDefEntry {
    uint32_t nameIdx;
    uint32_t typeIdx;
    uint32_t defaultValueIdx;
    int32_t offset;
    uint32_t flags;
};
#pragma pack(pop)

struct ResolvedOffsets {
    uintptr_t gameManager;
    uintptr_t playerList;
    uintptr_t localPlayer;
    uintptr_t health;
    uintptr_t teamId;
    uintptr_t transform;
    uintptr_t position;
    uintptr_t boneArray;
    uintptr_t viewMatrix;
    uintptr_t entityList;
    uintptr_t entityCount;
    bool ready;
};

static ResolvedOffsets g_resolved = {};
static uint8_t* g_metaData = nullptr;
static size_t g_metaSize = 0;
static MetaHeader g_header = {};

static const char* readString(uint32_t idx) {
    if (!g_metaData || idx >= g_header.strSize) return "";
    return (const char*)(g_metaData + g_header.strOffset + idx);
}

static uint32_t hashName(const char* s) {
    uint32_t h = 0x811C9DC5;
    while (*s) {
        h ^= (uint8_t)*s++;
        h *= 0x01000193;
    }
    return h;
}

static bool loadMetaFile(const char* path) {
    FILE* f = fopen(path, "rb");
    if (!f) return false;

    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);

    if (sz <= 0 || sz > 64 * 1024 * 1024) {
        fclose(f);
        return false;
    }

    g_metaData = new uint8_t[sz];
    g_metaSize = sz;
    fread(g_metaData, 1, sz, f);
    fclose(f);

    memcpy(&g_header, g_metaData, sizeof(MetaHeader));

    auto expectedMagic = OBF("\xAF\xBA\xB1\xFA");
    uint32_t magic = g_header.magic;

    if (magic != 0xFAB11BAF) {
        delete[] g_metaData;
        g_metaData = nullptr;
        return false;
    }

    LOGI("Meta loaded v%d size=%zu", g_header.version, g_metaSize);
    return true;
}

static uint32_t findTypeByName(const char* target) {
    if (!g_metaData) return UINT32_MAX;
    uint32_t targetHash = hashName(target);

    for (uint32_t i = 0; i < g_header.typeDefCount; i++) {
        TypeDefEntry* td = (TypeDefEntry*)(g_metaData + g_header.typeDefOffset + i * sizeof(TypeDefEntry));
        const char* name = readString(td->nameIdx);
        if (hashName(name) == targetHash) {
            return i;
        }
    }
    return UINT32_MAX;
}

static int32_t getFieldOffset(uint32_t typeIdx, const char* fieldName) {
    if (!g_metaData || typeIdx == UINT32_MAX) return -1;
    TypeDefEntry* td = (TypeDefEntry*)(g_metaData + g_header.typeDefOffset + typeIdx * sizeof(TypeDefEntry));
    uint32_t targetHash = hashName(fieldName);

    for (uint32_t i = 0; i < td->fieldCount; i++) {
        FieldDefEntry* fd = (FieldDefEntry*)(g_metaData + g_header.fieldDefOffset + (td->fieldStart + i) * sizeof(FieldDefEntry));
        const char* fname = readString(fd->nameIdx);
        if (hashName(fname) == targetHash) {
            return fd->offset;
        }
    }
    return -1;
}

static bool resolveAllOffsets() {
    if (!g_metaData) return false;

    auto gmName = OBF("GameManager");
    auto plName = OBF("PlayerList");
    auto lpName = OBF("LocalPlayer");
    auto hpName = OBF("Health");
    auto tmName = OBF("TeamId");
    auto trName = OBF("Transform");
    auto psName = OBF("Position");
    auto bnName = OBF("BoneArray");
    auto vmName = OBF("ViewMatrix");
    auto elName = OBF("EntityList");
    auto ecName = OBF("EntityCount");

    uint32_t gmIdx = findTypeByName(gmName.c_str());
    uint32_t plIdx = findTypeByName(plName.c_str());
    uint32_t lpIdx = findTypeByName(lpName.c_str());
    uint32_t trIdx = findTypeByName(trName.c_str());

    if (gmIdx != UINT32_MAX) {
        int32_t off = getFieldOffset(gmIdx, plName.c_str());
        if (off >= 0) g_resolved.playerList = (uintptr_t)off;
        off = getFieldOffset(gmIdx, lpName.c_str());
        if (off >= 0) g_resolved.localPlayer = (uintptr_t)off;
        off = getFieldOffset(gmIdx, elName.c_str());
        if (off >= 0) g_resolved.entityList = (uintptr_t)off;
        off = getFieldOffset(gmIdx, ecName.c_str());
        if (off >= 0) g_resolved.entityCount = (uintptr_t)off;
        g_resolved.gameManager = 0x1C0;
    }

    if (plIdx != UINT32_MAX) {
        int32_t off = getFieldOffset(plIdx, hpName.c_str());
        if (off >= 0) g_resolved.health = (uintptr_t)off;
        off = getFieldOffset(plIdx, tmName.c_str());
        if (off >= 0) g_resolved.teamId = (uintptr_t)off;
        off = getFieldOffset(plIdx, trName.c_str());
        if (off >= 0) g_resolved.transform = (uintptr_t)off;
        off = getFieldOffset(plIdx, bnName.c_str());
        if (off >= 0) g_resolved.boneArray = (uintptr_t)off;
    }

    if (trIdx != UINT32_MAX) {
        int32_t off = getFieldOffset(trIdx, psName.c_str());
        if (off >= 0) g_resolved.position = (uintptr_t)off;
    }

    g_resolved.viewMatrix = 0x2E4;
    g_resolved.ready = true;

    LOGI("Offsets resolved gm=%lx pl=%lx hp=%lx tm=%lx tr=%lx ps=%lx",
        g_resolved.gameManager, g_resolved.playerList,
        g_resolved.health, g_resolved.teamId,
        g_resolved.transform, g_resolved.position);

    return true;
}

static bool tryXorDecrypt(uint8_t* data, size_t sz);
static bool loadMetaWithFallback(const char* path) {
    FILE* f = fopen(path, "rb");
    if (!f) return false;
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);
    g_metaData = (uint8_t*)malloc(sz);
    if (!g_metaData) { fclose(f); return false; }
    size_t rd = fread(g_metaData, 1, sz, f);
    fclose(f);
    if (rd != (size_t)sz) { free(g_metaData); g_metaData = nullptr; return false; }
    g_metaSize = sz;
    memcpy(&g_header, g_metaData, sizeof(MetaHeader));
    if (tryXorDecrypt(g_metaData, g_metaSize)) {
        memcpy(&g_header, g_metaData, sizeof(MetaHeader));
    }
    return resolveAllOffsets();
}



extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeParseMeta(JNIEnv* env, jclass, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    if (!p) return JNI_FALSE;
    bool ok = loadMetaWithFallback(p);
    env->ReleaseStringUTFChars(path, p);

    if (!ok) return JNI_FALSE;

    ok = resolveAllOffsets();

    delete[] g_metaData;
    g_metaData = nullptr;
    g_metaSize = 0;

    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_display_utils_AssetLoader_nativeGetOffset(JNIEnv*, jclass, jint type) {
    if (!g_resolved.ready) return -1;
    switch (type) {
        case 0: return (jlong)g_resolved.gameManager;
        case 1: return (jlong)g_resolved.playerList;
        case 2: return (jlong)g_resolved.localPlayer;
        case 3: return (jlong)g_resolved.health;
        case 4: return (jlong)g_resolved.teamId;
        case 5: return (jlong)g_resolved.transform;
        case 6: return (jlong)g_resolved.position;
        case 7: return (jlong)g_resolved.boneArray;
        case 8: return (jlong)g_resolved.viewMatrix;
        case 9: return (jlong)g_resolved.entityList;
        case 10: return (jlong)g_resolved.entityCount;
        default: return -1;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_display_utils_AssetLoader_nativeOffsetsReady(JNIEnv*, jclass) {
    return g_resolved.ready ? JNI_TRUE : JNI_FALSE;
}

static bool tryXorDecrypt(uint8_t* data, size_t sz) {
    for (int key = 1; key < 256; key++) {
        uint32_t testMagic;
        uint8_t buf[4];
        for (int i = 0; i < 4; i++) buf[i] = data[i] ^ (uint8_t)key;
        memcpy(&testMagic, buf, 4);
        if (testMagic == 0xFAB11BAF) {
            for (size_t i = 0; i < sz; i++) data[i] ^= (uint8_t)key;
            LOGI("Meta decrypted with key=0x%02X", key);
            return true;
        }
    }
    return false;
}

