package com.display.utils;

import android.util.Log;

/**
 * Java-side offset cache and player data helper.
 * Bridges native cache_manager and draw_utils data to Java layer.
 *
 * Native method count: 7 (must match cpp JNI signatures exactly)
 * Java-side caching provides fast access without JNI overhead.
 *
 * Called from native layer via JNI when auto-offset scan needs
 * Java-side assistance (e.g., reading SharedPreferences configs,
 * validating offsets against known game versions).
 */
public class AssetHelper {
    private static final String TAG = "DisplayUtils";

    // ========================================================================
    // NATIVE METHOD DECLARATIONS
    // These MUST match cpp JNI function names exactly.
    // Grouped by source file for maintainability.
    // ========================================================================

    // --- asset_meta_resolve.cpp (1 method) ---

    /** Get resolved offset by type index from native cache */
    public static native long getOffset(int type);

    // --- cache_manager.cpp (4 methods) ---

    /** Trigger native player cache update cycle */
    public static native void updateCache();

    /** Check if cache needs refresh based on frame timing */
    public static native boolean shouldUpdate();

    /** Get cached health value for player at index */
    public static native float getCachedHealth(int idx);

    /** Get cached team ID for player at index */
    public static native int getCachedTeam(int idx);

    // --- draw_utils.cpp (2 methods) ---

    /** Push player data array from native to Java cache */
    public static native void setPlayerData(float[] data);

    /** Push view matrix array from native to Java cache */
    public static native void setViewMatrix(float[] matrix);

    // ========================================================================
    // JAVA-SIDE OFFSET CACHE
    // Fast access without JNI overhead for frequently read values.
    // ========================================================================

    // Cached offsets - populated after successful metadata parse
    private static long sGameManagerOffset = 0x0;
    private static long sPlayerListOffset = 0x0;
    private static long sHealthOffset = 0x0;
    private static long sPositionOffset = 0x0;
    private static long sTeamIdOffset = 0x0;
    private static long sWeaponOffset = 0x0;
    private static long sBoneArrayOffset = 0x0;
    private static long sViewMatrixOffset = 0x0;

    // Game version tracking for offset validation
    private static String sGameVersion = "";
    private static boolean sOffsetsValid = false;

    /**
     * Set resolved offsets from native metadata parser.
     * Called via JNI after successful IL2CPP parse.
     *
     * @param gmOff GameManager pointer offset
     * @param plOff PlayerList/container offset
     * @param hpOff Health field offset
     * @param posOff Position vector offset
     * @param teamOff Team ID field offset
     * @param wpnOff Weapon ID field offset
     * @param boneOff Bone array base offset
     * @param viewOff View matrix offset
     */
    public static void setOffsets(long gmOff, long plOff, long hpOff, long posOff,
                                   long teamOff, long wpnOff, long boneOff, long viewOff) {
        sGameManagerOffset = gmOff;
        sPlayerListOffset = plOff;
        sHealthOffset = hpOff;
        sPositionOffset = posOff;
        sTeamIdOffset = teamOff;
        sWeaponOffset = wpnOff;
        sBoneArrayOffset = boneOff;
        sViewMatrixOffset = viewOff;
        sOffsetsValid = true;
        Log.i(TAG, "Offsets set: GM=0x" + Long.toHexString(gmOff)
                + " PL=0x" + Long.toHexString(plOff));
    }

    /**
     * Get individual offset from Java cache by index.
     * Index mapping: 0=GM, 1=PL, 2=HP, 3=POS, 4=TEAM, 5=WPN, 6=BONE, 7=VIEW
     * Note: This reads Java cache, NOT native. Use native getOffset() for live values.
     *
     * @param index Offset index
     * @return Offset value or 0 if invalid index
     */
    public static long getJavaOffset(int index) {
        switch (index) {
            case 0: return sGameManagerOffset;
            case 1: return sPlayerListOffset;
            case 2: return sHealthOffset;
            case 3: return sPositionOffset;
            case 4: return sTeamIdOffset;
            case 5: return sWeaponOffset;
            case 6: return sBoneArrayOffset;
            case 7: return sViewMatrixOffset;
            default: return 0x0;
        }
    }

    /**
     * Validate current offsets against expected ranges.
     * IL2CPP offsets should be within reasonable bounds.
     *
     * @return true if all offsets appear valid
     */
    public static boolean validateOffsets() {
        if (!sOffsetsValid) return false;

        long[] offsets = {
            sGameManagerOffset, sPlayerListOffset, sHealthOffset,
            sPositionOffset, sTeamIdOffset, sWeaponOffset,
            sBoneArrayOffset, sViewMatrixOffset
        };

        for (long off : offsets) {
            if (off < 0 || off > 0x10000000L) {
                Log.w(TAG, "Suspicious offset detected: 0x" + Long.toHexString(off));
                return false;
            }
        }
        return true;
    }

    /**
     * Set game version string for offset compatibility tracking.
     *
     * @param version Game version code/name
     */
    public static void setGameVersion(String version) {
        sGameVersion = version != null ? version : "";
        Log.d(TAG, "Game version set: " + sGameVersion);
    }

    /** Get stored game version */
    public static String getGameVersion() { return sGameVersion; }

    /** Check if offsets have been successfully resolved */
    public static boolean areOffsetsValid() { return sOffsetsValid; }

    /** Reset all cached offsets. Called on cleanup or version mismatch. */
    public static void reset() {
        sGameManagerOffset = 0x0;
        sPlayerListOffset = 0x0;
        sHealthOffset = 0x0;
        sPositionOffset = 0x0;
        sTeamIdOffset = 0x0;
        sWeaponOffset = 0x0;
        sBoneArrayOffset = 0x0;
        sViewMatrixOffset = 0x0;
        sOffsetsValid = false;
        sGameVersion = "";
        Log.i(TAG, "AssetHelper reset");
    }

    /** Dump current offset state to log for debugging. */
    public static void dumpOffsets() {
        Log.d(TAG, "=== Offset Dump ===");
        Log.d(TAG, "GameManager: 0x" + Long.toHexString(sGameManagerOffset));
        Log.d(TAG, "PlayerList:  0x" + Long.toHexString(sPlayerListOffset));
        Log.d(TAG, "Health:      0x" + Long.toHexString(sHealthOffset));
        Log.d(TAG, "Position:    0x" + Long.toHexString(sPositionOffset));
        Log.d(TAG, "TeamID:      0x" + Long.toHexString(sTeamIdOffset));
        Log.d(TAG, "Weapon:      0x" + Long.toHexString(sWeaponOffset));
        Log.d(TAG, "BoneArray:   0x" + Long.toHexString(sBoneArrayOffset));
        Log.d(TAG, "ViewMatrix:  0x" + Long.toHexString(sViewMatrixOffset));
        Log.d(TAG, "Version:     " + sGameVersion);
        Log.d(TAG, "Valid:       " + sOffsetsValid);
        Log.d(TAG, "===================");
    }
}
