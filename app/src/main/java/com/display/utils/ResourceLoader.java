package com.display.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class ResourceLoader {

    private static final String TAG = "DispUtils";
    private final Context hostContext;
    private final File virtualRoot;
    private final File dexDir;
    private final File optDir;
    private final File libDir;
    private ClassLoader targetLoader;

    public ResourceLoader(Context ctx) {
        this.hostContext = ctx;
        this.virtualRoot = new File(ctx.getFilesDir(), "vs");
        this.dexDir = new File(virtualRoot, "dex");
        this.optDir = new File(virtualRoot, "opt");
        this.libDir = new File(virtualRoot, "lib");
        ensureDirs();
    }

    private void ensureDirs() {
        virtualRoot.mkdirs();
        dexDir.mkdirs();
        optDir.mkdirs();
        libDir.mkdirs();
    }

    public boolean loadTarget(String apkPath) {
        try {
            File extractedDex = extractDex(apkPath);
            if (extractedDex == null) return false;

            targetLoader = new DexClassLoader(
                extractedDex.getAbsolutePath(),
                optDir.getAbsolutePath(),
                libDir.getAbsolutePath(),
                hostContext.getClassLoader()
            );

            Log.i(TAG, "Target loaded from: " + apkPath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Load failed: " + e.getMessage());
            return false;
        }
    }

    private File extractDex(String apkPath) {
        try {
            File dest = new File(dexDir, "target.apk");
            if (dest.exists()) return dest;

            InputStream in = new java.io.FileInputStream(apkPath);
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();
            return dest;
        } catch (Exception e) {
            Log.e(TAG, "Extract failed: " + e.getMessage());
            return null;
        }
    }

    public Class<?> findClass(String name) {
        if (targetLoader == null) return null;
        try {
            return targetLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public Object invokeStatic(String className, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Class<?> clazz = findClass(className);
            if (clazz == null) return null;
            Method m = clazz.getMethod(methodName, paramTypes);
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (Exception e) {
            Log.e(TAG, "Invoke failed: " + e.getMessage());
            return null;
        }
    }

    public Object createInstance(String className, Class<?>[] paramTypes, Object[] args) {
        try {
            Class<?> clazz = findClass(className);
            if (clazz == null) return null;
            return clazz.getConstructor(paramTypes).newInstance(args);
        } catch (Exception e) {
            Log.e(TAG, "Create failed: " + e.getMessage());
            return null;
        }
    }

    public ClassLoader getTargetLoader() {
        return targetLoader;
    }

    public File getVirtualRoot() {
        return virtualRoot;
    }

    public File getLibDir() {

    private static native boolean nativeLoadTarget(String libPath);
    private static native long nativeGetBase();
    private static native long nativeGetSize();
    private static native boolean nativeIsReady();
    private static native long nativeFindSymbol(String symName);
    private static native byte[] nativeReadMemory(long addr, int size);
    private static native void nativeCleanMaps();
        return libDir;
    }

    public String getNativeLibPath(String libName) {
        File f = new File(libDir, libName);
        return f.exists() ? f.getAbsolutePath() : null;
    }

    public boolean extractNativeLib(String apkPath, String libName) {
        try {
            java.util.zip.ZipFile zip = new java.util.zip.ZipFile(apkPath);
            String abi = "arm64-v8a";
            String entry = "lib/" + abi + "/" + libName;
            java.util.zip.ZipEntry ze = zip.getEntry(entry);
            if (ze == null) {
                abi = "armeabi-v7a";
                entry = "lib/" + abi + "/" + libName;
                ze = zip.getEntry(entry);
            }
            if (ze == null) {
                zip.close();
                return false;
            }
            File dest = new File(libDir, libName);
            InputStream in = zip.getInputStream(ze);
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();
            zip.close();
            dest.setReadable(true, true);
            dest.setExecutable(true, true);
            Log.i(TAG, "Native lib extracted: " + libName);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Native extract failed: " + e.getMessage());
            return false;
        }
    }
}
