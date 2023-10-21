/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import static android.content.Intent.ACTION_SHOW_BRIGHTNESS_DIALOG;
import static android.content.Intent.EXTRA_BRIGHTNESS_DIALOG_IS_FULL_WIDTH;

import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX;
import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MIN;
import static com.android.settingslib.display.BrightnessUtils.convertLinearToGammaFloat;

import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.System;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.transition.SettingsTransitionHelper;

import java.text.NumberFormat;

public class BrightnessLevelPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "BrightnessPrefCtrl";
    private static final String KEY_BRIGHTNESS = "brightness";
    private static final Uri BRIGHTNESS_ADJ_URI;
    private final ContentResolver mContentResolver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final DisplayManager mDisplayManager;

    private Preference mPreference;

    static {
        BRIGHTNESS_ADJ_URI = System.getUriFor(System.SCREEN_AUTO_BRIGHTNESS_ADJ);
    }

    private ContentObserver mBrightnessObserver =
            new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    updatedSummary(mPreference);
                }
            };

    private final DisplayListener mDisplayListener = new DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            updatedSummary(mPreference);
        }
    };


    public BrightnessLevelPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mDisplayManager = context.getSystemService(DisplayManager.class);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BRIGHTNESS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_BRIGHTNESS);
    }

    @Override
    public void updateState(Preference preference) {
        updatedSummary(preference);
    }

    @Override
    public void onStart() {
        mContentResolver.registerContentObserver(BRIGHTNESS_ADJ_URI, false, mBrightnessObserver);
        mDisplayManager.registerDisplayListener(mDisplayListener, mHandler,
                DisplayManager.EVENT_FLAG_DISPLAY_BRIGHTNESS);
        updatedSummary(mPreference);
    }

    @Override
    public void onStop() {
        mContentResolver.unregisterContentObserver(mBrightnessObserver);
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        final Intent intent = new Intent(ACTION_SHOW_BRIGHTNESS_DIALOG);
        intent.putExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                SettingsTransitionHelper.TransitionType.TRANSITION_NONE);
        intent.putExtra(EXTRA_BRIGHTNESS_DIALOG_IS_FULL_WIDTH, true);

        // Start activity in the same task and pass fade animations
        final ActivityOptions options = ActivityOptions.makeCustomAnimation(mContext,
                android.R.anim.fade_in, android.R.anim.fade_out);
        mContext.startActivityForResult(preference.getKey(), intent, 0, options.toBundle());
        return true;
    }

    private void updatedSummary(Preference preference) {
        if (preference != null) {
            preference.setSummary(NumberFormat.getPercentInstance().format(getCurrentBrightness()));
        }
    }

    private double getCurrentBrightness() {
        int value = 0;
        final BrightnessInfo info = mContext.getDisplay().getBrightnessInfo();
        if (info != null) {
            value = convertLinearToGammaFloat(info.brightness, info.brightnessMinimum,
                    info.brightnessMaximum);
        }
        return getPercentage(value, GAMMA_SPACE_MIN, GAMMA_SPACE_MAX);
    }

    private double getPercentage(double value, int min, int max) {
        if (value > max) {
            return 1.0;
        }
        if (value < min) {
            return 0.0;
        }
        return (value - min) / (max - min);
    }
}
