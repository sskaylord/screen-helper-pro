package com.display.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import java.lang.reflect.Method;

/**
 * Minimal Google account auth via loaded GMS Core.
 * Uses reflection to avoid compile-time GMS dependency.
 */
public class AccountAuth {
    private static final String TAG = StrObf.d("\u00c0\u00c1\u00c1\u00d9\u00d2\u00d7\u00c3"); // "AccountAuth" XOR'd
    private static final int RC_SIGN_IN = 9001;

    /**
     * Launch Google Sign-In flow using reflected GMS classes.
     * @param activity Host activity for startActivityForResult
     * @return true if sign-in intent launched successfully
     */
    public static boolean signIn(Activity activity) {
        if (!GmsLoader.isReady()) {
            Log.e(TAG, "GMS not loaded");
            return false;
        }

        try {
            ClassLoader cl = GmsLoader.getClassLoader();

            // Build GoogleSignInOptions via reflection
            Class<?> gsoBuilderClass = cl.loadClass(
                "com.google.android.gms.auth.api.signin.GoogleSignInOptions$Builder"
            );
            Object builder = gsoBuilderClass.getDeclaredConstructor().newInstance();
            
            // Add DEFAULT_GAMES_SIGN_IN scope
            Method addScopeMethod = gsoBuilderClass.getMethod("addScope", 
                cl.loadClass("com.google.android.gms.common.api.Scope"));
            Class<?> scopeClass = cl.loadClass("com.google.android.gms.common.api.Scope");
            Object gamesScope = scopeClass.getDeclaredConstructor(String.class)
                .newInstance("https://www.googleapis.com/auth/games");
            addScopeMethod.invoke(builder, gamesScope);

            // Request email + ID token
            Method requestEmail = gsoBuilderClass.getMethod("requestEmail");
            requestEmail.invoke(builder);

            // Build options
            Method buildMethod = gsoBuilderClass.getMethod("build");
            Object gso = buildMethod.invoke(builder);

            // Create GoogleSignInClient
            Class<?> gsiClass = cl.loadClass(
                "com.google.android.gms.auth.api.signin.GoogleSignIn"
            );
            Method getClient = gsiClass.getMethod("getClient", 
                android.content.Context.class,
                cl.loadClass("com.google.android.gms.auth.api.signin.GoogleSignInOptions")
            );
            Object client = getClient.invoke(null, activity, gso);

            // Get sign-in intent
            Method getSignInIntent = client.getClass().getMethod("getSignInIntent");
            Intent signInIntent = (Intent) getSignInIntent.invoke(client);

            activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            Log.i(TAG, "Sign-in flow launched");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Sign-in failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handle sign-in result in onActivityResult.
     * @return Account ID string or null on failure
     */
    public static String handleResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RC_SIGN_IN || !GmsLoader.isReady()) return null;

        try {
            ClassLoader cl = GmsLoader.getClassLoader();
            Class<?> gsiClass = cl.loadClass(
                "com.google.android.gms.auth.api.signin.GoogleSignIn"
            );
            Method getSignedInAccount = gsiClass.getMethod(
                "getSignedInAccountFromIntent", Intent.class
            );
            Object task = getSignedInAccount.invoke(null, data);

            // Get result from Task
            Class<?> tasksClass = cl.loadClass("com.google.android.gms.tasks.Tasks");
            Method await = tasksClass.getMethod("await", 
                cl.loadClass("com.google.android.gms.tasks.Task"));
            Object account = await.invoke(null, task);

            if (account != null) {
                Method getId = account.getClass().getMethod("getId");
                String id = (String) getId.invoke(account);
                Log.i(TAG, "Account authenticated: " + id.substring(0, 4) + "...");
                return id;
            }
        } catch (Exception e) {
            Log.e(TAG, "Result handling failed: " + e.getMessage());
        }
        return null;
    }
}
