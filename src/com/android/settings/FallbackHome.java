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
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;

import java.util.Objects;

public class FallbackHome extends Activity {
    private static final String TAG = "FallbackHome";
    private static final int PROGRESS_TIMEOUT = 2000;

    private boolean mProvisioned;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ourselves totally black before the device is provisioned so that
        // we don't flash the wallpaper before SUW
        mProvisioned = Settings.Global.getInt(getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 0) != 0;
        if (!mProvisioned) {
            setTheme(R.style.FallbackHome_SetupWizard);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }

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
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            maybeFinish();
        }
    };

    private void maybeFinish() {
        if (getSystemService(UserManager.class).isUserUnlocked()) {
            final Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME);
            final ResolveInfo homeInfo = getPackageManager().resolveActivity(homeIntent, 0);
            if (Objects.equals(getPackageName(), homeInfo.activityInfo.packageName)) {
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

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            maybeFinish();
        }
    };
}
