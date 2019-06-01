/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.app.WallpaperManager.OnColorsChangedListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;

import java.util.Objects;

public class FallbackHome extends Activity {
    private static final String TAG = "FallbackHome";
    private static final int PROGRESS_TIMEOUT = 2000;

    private boolean mProvisioned;
    private WallpaperManager mWallManager;

    private final Runnable mProgressTimeoutRunnable = () -> {
        View v = getLayoutInflater().inflate(
                R.layout.fallback_home_finishing_boot, null /* root */);
        setContentView(v);
        v.setAlpha(0f);
        v.animate()
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(AnimationUtils.loadInterpolator(
                        this, android.R.interpolator.fast_out_slow_in))
                .start();
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
    };

    private final OnColorsChangedListener mColorsChangedListener = new OnColorsChangedListener() {
        @Override
        public void onColorsChanged(WallpaperColors colors, int which) {
            if (colors != null) {
                final View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        updateVisibilityFlagsFromColors(colors, decorView.getSystemUiVisibility()));
                mWallManager.removeOnColorsChangedListener(this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ourselves totally black before the device is provisioned so that
        // we don't flash the wallpaper before SUW
        mProvisioned = Settings.Global.getInt(getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) != 0;
        final int flags;
        if (!mProvisioned) {
            setTheme(R.style.FallbackHome_SetupWizard);
            flags = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        } else {
            flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }

        mWallManager = getSystemService(WallpaperManager.class);
        if (mWallManager == null) {
            Log.w(TAG, "Wallpaper manager isn't ready, can't listen to color changes!");
        } else {
            loadWallpaperColors(flags);
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);

        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        maybeFinish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mProvisioned) {
            mHandler.postDelayed(mProgressTimeoutRunnable, PROGRESS_TIMEOUT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mProgressTimeoutRunnable);
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (mWallManager != null) {
            mWallManager.removeOnColorsChangedListener(mColorsChangedListener);
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            maybeFinish();
        }
    };

    private void loadWallpaperColors(int flags) {
        final AsyncTask loadWallpaperColorsTask = new AsyncTask<Object, Void, Integer>() {
            @Override
            protected Integer doInBackground(Object... params) {
                final WallpaperColors colors =
                        mWallManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);

                // Use a listener to wait for colors if not ready yet.
                if (colors == null) {
                    mWallManager.addOnColorsChangedListener(mColorsChangedListener,
                            null /* handler */);
                    return null;
                }
                return updateVisibilityFlagsFromColors(colors, flags);
            }

            @Override
            protected void onPostExecute(Integer flagsToUpdate) {
                if (flagsToUpdate == null) {
                    return;
                }
                getWindow().getDecorView().setSystemUiVisibility(flagsToUpdate);
            }
        };
        loadWallpaperColorsTask.execute();
    }

    private void maybeFinish() {
        if (getSystemService(UserManager.class).isUserUnlocked()) {
            final Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME);
            final ResolveInfo homeInfo = getPackageManager().resolveActivity(homeIntent, 0);
            if (Objects.equals(getPackageName(), homeInfo.activityInfo.packageName)) {
                if (UserManager.isSplitSystemUser()
                        && UserHandle.myUserId() == UserHandle.USER_SYSTEM) {
                    // This avoids the situation where the system user has no home activity after
                    // SUW and this activity continues to throw out warnings. See b/28870689.
                    return;
                }
                Log.d(TAG, "User unlocked but no home; let's hope someone enables one soon?");
                mHandler.sendEmptyMessageDelayed(0, 500);
            } else {
                Log.d(TAG, "User unlocked and real home found; let's go!");
                getSystemService(PowerManager.class).userActivity(
                        SystemClock.uptimeMillis(), false);
                finish();
            }
        }
    }

    // Set the system ui flags to light status bar if the wallpaper supports dark text to match
    // current system ui color tints.
    private int updateVisibilityFlagsFromColors(WallpaperColors colors, int flags) {
        if ((colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0) {
            return flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        return flags & ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                & ~(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            maybeFinish();
        }
    };
}
