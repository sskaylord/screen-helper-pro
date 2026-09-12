#include <jni.h>
#include <android/log.h>
#include <GLES/gl.h>
#include <cstdint>
#include <cstring>
#include <cstdlib>
#include <cmath>

#define TAG "DispUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

struct Vec3 { float x, y, z; };
struct Vec2 { float x, y; };

struct PlayerDraw {
    Vec3 pos;
    float health;
    int team;
    bool valid;
};

static PlayerDraw g_players[64];
static int g_playerCount = 0;
static float g_viewMatrix[16] = {};
static int g_screenW = 1080, g_screenH = 1920;
static bool g_initialized = false;

static bool worldToScreen(const Vec3& world, Vec2& screen) {
    const float* m = g_viewMatrix;
    float w = m[3]*world.x + m[7]*world.y + m[11]*world.z + m[15];
    if (w < 0.01f) return false;
    float sx = (m[0]*world.x + m[4]*world.y + m[8]*world.z  + m[12]) / w;
    float sy = (m[1]*world.x + m[5]*world.y + m[9]*world.z  + m[13]) / w;
    screen.x = (sx + 1.f) * 0.5f * g_screenW;
    screen.y = (1.f - sy) * 0.5f * g_screenH;
    return true;
}

static void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b) {
    GLfloat verts[] = { x1, y1, x2, y2 };
    GLfloat cols[]  = { r,g,b,1.f, r,g,b,1.f };
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);
    glVertexPointer(2, GL_FLOAT, 0, verts);
    glColorPointer(4, GL_FLOAT, 0, cols);
    glDrawArrays(GL_LINES, 0, 2);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
}

static void drawBox(float cx, float cy, float w, float h, float r, float g, float b) {
    float l = cx - w/2, ri = cx + w/2, t = cy - h, bo = cy;
    drawLine(l, t, ri, t,  r, g, b);
    drawLine(ri, t, ri, bo, r, g, b);
    drawLine(ri, bo, l, bo, r, g, b);
    drawLine(l, bo, l, t,  r, g, b);
}

static void drawHealthBar(float cx, float cy, float w, float h, float hp) {
    float barH = h * (hp / 100.f);
    float r = hp < 30.f ? 1.f : 0.f;
    float g = hp > 60.f ? 1.f : hp / 100.f;
    drawLine(cx - w/2 - 6, cy, cx - w/2 - 6, cy - barH, r, g, 0.f);
}

static void renderFrame() {
    if (!g_initialized) return;
    glClear(GL_COLOR_BUFFER_BIT);
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glOrthof(0, g_screenW, g_screenH, 0, -1, 1);
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    glLineWidth(2.f);

    for (int i = 0; i < g_playerCount; i++) {
        if (!g_players[i].valid) continue;
        Vec2 head, feet;
        Vec3 headW = { g_players[i].pos.x, g_players[i].pos.y + 1.8f, g_players[i].pos.z };
        Vec3 feetW = g_players[i].pos;
        if (!worldToScreen(headW, head)) continue;
        if (!worldToScreen(feetW, feet)) continue;
        float boxH = feet.y - head.y;
        float boxW = boxH * 0.45f;
        float cx   = (head.x + feet.x) * 0.5f;
        bool enemy = g_players[i].team != 0;
        float cr = enemy ? 1.f  : 0.f;
        float cg = enemy ? 0.2f : 1.f;
        float cb = enemy ? 0.2f : 0.f;
        drawBox(cx, feet.y, boxW, boxH, cr, cg, cb);
        drawHealthBar(cx, feet.y, boxW, boxH, g_players[i].health);
        drawLine(g_screenW / 2.f, g_screenH / 2.f, cx, head.y, cr, cg, cb);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_DisplaySurface_nativeDrawFrame(JNIEnv*, jobject, jlong ptr) {
    renderFrame();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_display_utils_DisplaySurface_nativeInit(JNIEnv* env, jobject thiz, jobject surface) {
    g_initialized = true;
    LOGI("Native renderer initialized");
    return 1L;
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_DisplaySurface_nativeOnResize(JNIEnv*, jobject, jlong ptr, jint w, jint h) {
    g_screenW = w;
    g_screenH = h;
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_DisplaySurface_nativeDestroy(JNIEnv*, jobject, jlong ptr) {
    g_initialized = false;
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetHelper_setPlayerData(JNIEnv*, jclass,
    jint idx, jfloat x, jfloat y, jfloat z, jfloat hp, jint team) {
    if (idx < 0 || idx >= 64) return;
    g_players[idx].pos    = { x, y, z };
    g_players[idx].health = hp;
    g_players[idx].team   = team;
    g_players[idx].valid  = true;
    if (idx >= g_playerCount) g_playerCount = idx + 1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_display_utils_AssetHelper_setViewMatrix(JNIEnv* env, jclass, jfloatArray mat) {
    if (!mat) return;
    jfloat* data = env->GetFloatArrayElements(mat, nullptr);
    if (data) {
        memcpy(g_viewMatrix, data, 16 * sizeof(float));
        env->ReleaseFloatArrayElements(mat, data, JNI_ABORT);
    }
}
