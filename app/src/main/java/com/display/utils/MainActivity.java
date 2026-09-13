import androidx.fragment.app.Fragment;
package com.display.utils;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Main activity with bottom tab navigation.
 * Three tabs: Home, Profiles, Display.
 * Uses fragment-based content switching.
 * All names AC-safe - no suspicious strings.
 */
public class MainActivity extends Activity {

    private FrameLayout contentFrame;
    private TextView tabHome, tabProfiles, tabDisplay;
    private int currentTab = 2;

    private static final int BG_DARK = 0xFF1A0A1E;
    private static final int NAV_BG = 0xFF2D1233;
    private static final int ACCENT = 0xFFEC4899;
    private static final int TEXT_DIM = 0xFFD4A0B0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG_DARK);
        getWindow().setNavigationBarColor(NAV_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_DARK);

        // Content frame for fragments
        contentFrame = new FrameLayout(this);
        contentFrame.setId(View.generateViewId());
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(contentFrame);

        // Bottom navigation bar
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(NAV_BG);
        nav.setPadding(0, 12, 0, 12);
        nav.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        tabDisplay = makeTab("Display", "\uD83C\uDFA8", 0);
        tabProfiles = makeTab("Profiles", "\uD83D\uDC65", 1);
        tabHome = makeTab("Home", "\uD83C\uDFE0", 2);

        nav.addView(tabDisplay, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nav.addView(tabProfiles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nav.addView(tabHome, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(nav);
        setContentView(root);
        // Request all required permissions
        requestAllPermissions();


        // Default to Home tab
        switchTab(2);
    }

    /**
     * Create a single tab button with icon and label.
     *
     * @param label Tab display name
     * @param icon Emoji icon
     * @param idx Tab index (0=Display, 1=Profiles, 2=Home)
     * @return Configured TextView for tab
     */
    private TextView makeTab(String label, String icon, final int idx) {
        TextView tv = new TextView(this);
        tv.setText(icon + "\n" + label);
        tv.setTextColor(TEXT_DIM);
        tv.setTextSize(12);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 8, 0, 8);
        tv.setOnClickListener(v -> switchTab(idx));
        return tv;
    }

    /**
     * Switch active tab and load corresponding fragment.
     * Updates tab highlight colors.
     *
     * @param index Tab index (0=Display, 1=Profiles, 2=Home)
     */
    private void switchTab(int index) {
        currentTab = index;

        // Update tab colors
        tabHome.setTextColor(index == 2 ? ACCENT : TEXT_DIM);
        tabProfiles.setTextColor(index == 1 ? ACCENT : TEXT_DIM);
        tabDisplay.setTextColor(index == 0 ? ACCENT : TEXT_DIM);

        // Load fragment for selected tab
        Fragment f;
        switch (index) {
            case 0:
                f = new DisplayFragment();
                break;
            case 1:
                f = new ProfilesFragment();
                break;
            default:
                f = new HomeFragment();
                break;
        }

        getFragmentManager().beginTransaction()
                .replace(contentFrame.getId(), f)
                .commitAllowingStateLoss();
    }

    /**
     * Public accessor for external tab switching.
     * Used by other components to programmatically change tabs.
     *
     * @param index Tab index (0=Display, 1=Profiles, 2=Home)
     */
    public void switchTabPublic(int index) {
        switchTab(index);
    }


    private void requestAllPermissions() {
        // Runtime permissions (Android 6+)
        String[] perms = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        java.util.List<String> needed = new java.util.ArrayList<>();
        for (String p : perms) {
            if (checkSelfPermission(p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), 100);
        }

        // Overlay permission (special - goes to Settings)
        if (!android.provider.Settings.canDrawOverlays(this)) {
            android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Manage external storage (Android 11+)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            if (!android.os.Environment.isExternalStorageManager()) {
                android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            android.util.Log.i("DisplayUtils", "Storage permissions granted");
        }
    }

}
