package com.display.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * System configuration and AC bypass layer.
 * Stealth-renamed from ServiceBridge to avoid detection.
 *
 * Responsibilities:
 * - Hook Build fields via reflection to spoof device fingerprint
 * - Generate consistent spoofed identifiers per session
 * - Store original values for restoration on uninstall
 * - Provide spoofed values to other subsystems
 *
 * All hooks use reflection only - no native ptrace required.
 */
public class SysConfig {
    private static final String TAG = "DisplayUtils";
    private static volatile boolean sInstalled = false;
    private static Map<String, Object> sOriginalValues = new HashMap<>();
    private static String sSpoofedSignature = null;
    private static String sSpoofedFingerprint = null;
    private static String sSpoofedDeviceId = null;

    /**
     * Install all AC bypass hooks and spoofs.
     * Safe to call multiple times - idempotent after first install.
     *
     * @param ctx Application context
     */
    public static synchronized void install(Context ctx) {
        if (sInstalled) return;

        try {
            // Generate consistent spoofed values for this session
            generateSpoofedValues(ctx);

            // Hook Build fields to return spoofed fingerprint/device info
            hookBuildFields();

            sInstalled = true;
            Log.i(TAG, "SysConfig installed");
        } catch (Exception e) {
            Log.e(TAG, "SysConfig install failed", e);
        }
    }

    /**
     * Remove all hooks and restore original system state.
     */
    public static synchronized void uninstall() {
        if (!sInstalled) return;

        try {
            restoreBuildFields();
            sOriginalValues.clear();
            sInstalled = false;
            Log.i(TAG, "SysConfig uninstalled");
        } catch (Exception e) {
            Log.e(TAG, "SysConfig uninstall failed", e);
        }
    }

    /**
     * Generate spoofed device identifiers.
     * Uses real Samsung/Google/Xiaomi fingerprints for authenticity.
     * Device ID is seeded from package name for consistency across restarts.
     *
     * @param ctx Application context for seed generation
     */
    private static void generateSpoofedValues(Context ctx) {
        // Spoofed signature hash (matches common legitimate apps)
        sSpoofedSignature = "A1:B2:C3:D4:E5:F6:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB";

        // Real device fingerprints from popular devices
        String[] fingerprints = {
            "samsung/dreamltexx/dreamlte:10/QP1A.190711.020/G950FXXU9DTJ1:user/release-keys",
            "google/oriole/oriole:13/TQ3A.230901.001/10754064:user/release-keys",
            "Xiaomi/venus/venus:12/SKQ1.211006.001/V13.0.15.0.SKBMIXM:user/release-keys",
            "OPPO/CPH2219EEA/OP4F7BL1:12/SKQ1.211113.001/1657520800000:user/release-keys"
        };
        Random fpRandom = new Random(System.currentTimeMillis());
        sSpoofedFingerprint = fingerprints[fpRandom.nextInt(fingerprints.length)];

        // Deterministic Android ID seeded from package name
        // Ensures same ID across app restarts but different per package
        Random idRandom = new Random(ctx.getPackageName().hashCode());
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(Integer.toHexString(idRandom.nextInt(16)));
        }
        sSpoofedDeviceId = sb.toString();

        Log.d(TAG, "Spoofed values generated: fp=" + sSpoofedFingerprint.substring(0, 20) + "...");
    }

    /**
     * Hook Build.FINGERPRINT, Build.MODEL, Build.BRAND via reflection.
     * Stores originals for restoration.
     */
    private static void hookBuildFields() {
        try {
            // Save original values
            sOriginalValues.put("fingerprint", Build.FINGERPRINT);
            sOriginalValues.put("model", Build.MODEL);
            sOriginalValues.put("brand", Build.BRAND);
            sOriginalValues.put("device", Build.DEVICE);
            sOriginalValues.put("product", Build.PRODUCT);

            // Set spoofed values via reflection (Build fields are static final)
            setStaticFinalField(Build.class, "FINGERPRINT", sSpoofedFingerprint);
            setStaticFinalField(Build.class, "MODEL", "SM-G950F");
            setStaticFinalField(Build.class, "BRAND", "samsung");
            setStaticFinalField(Build.class, "DEVICE", "dreamlte");
            setStaticFinalField(Build.class, "PRODUCT", "dreamltexx");

            Log.d(TAG, "Build fields hooked successfully");
        } catch (Exception e) {
            Log.e(TAG, "Build field hook failed", e);
        }
    }

    /**
     * Restore original Build field values.
     */
    private static void restoreBuildFields() {
        try {
            if (sOriginalValues.containsKey("fingerprint")) {
                setStaticFinalField(Build.class, "FINGERPRINT", sOriginalValues.get("fingerprint"));
            }
            if (sOriginalValues.containsKey("model")) {
                setStaticFinalField(Build.class, "MODEL", sOriginalValues.get("model"));
            }
            if (sOriginalValues.containsKey("brand")) {
                setStaticFinalField(Build.class, "BRAND", sOriginalValues.get("brand"));
            }
            if (sOriginalValues.containsKey("device")) {
                setStaticFinalField(Build.class, "DEVICE", sOriginalValues.get("device"));
            }
            if (sOriginalValues.containsKey("product")) {
                setStaticFinalField(Build.class, "PRODUCT", sOriginalValues.get("product"));
            }

            Log.d(TAG, "Build fields restored");
        } catch (Exception e) {
            Log.e(TAG, "Build field restore failed", e);
        }
    }

    /**
     * Utility: Set static final field value via reflection.
     * Required because Build fields are declared static final.
     * Removes final modifier before setting value.
     *
     * @param clazz Target class
     * @param fieldName Field name to modify
     * @param value New value to set
     */
    private static void setStaticFinalField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        // Remove final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

        field.set(null, value);
    }

    /** Get spoofed signature hash string */
    public static String getSpoofedSignature() {
        return sSpoofedSignature;
    }

    /** Get spoofed build fingerprint */
    public static String getSpoofedFingerprint() {
        return sSpoofedFingerprint;
    }

    /** Get spoofed Android ID */
    public static String getSpoofedDeviceId() {
        return sSpoofedDeviceId;
    }

    /** Check if hooks are currently active */
    public static boolean isInstalled() {
        return sInstalled;
    }
}
