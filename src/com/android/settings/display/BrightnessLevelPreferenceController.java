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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.System;
import android.service.vr.IVrManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.text.NumberFormat;

public class BrightnessLevelPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "BrightnessPrefCtrl";
    private static final String KEY_BRIGHTNESS = "brightness";
    private static final Uri BRIGHTNESS_MODE_URI;
    private static final Uri BRIGHTNESS_URI;
    private static final Uri BRIGHTNESS_FOR_VR_URI;
    private static final Uri BRIGHTNESS_ADJ_URI;

    private final int mMinBrightness;
    private final int mMaxBrightness;
    private final int mMinVrBrightness;
    private final int mMaxVrBrightness;
    private final ContentResolver mContentResolver;

    private Preference mPreference;

    static {
        BRIGHTNESS_MODE_URI = System.getUriFor(System.SCREEN_BRIGHTNESS_MODE);
        BRIGHTNESS_URI = System.getUriFor(System.SCREEN_BRIGHTNESS);
        BRIGHTNESS_FOR_VR_URI = System.getUriFor(System.SCREEN_BRIGHTNESS_FOR_VR);
        BRIGHTNESS_ADJ_URI = System.getUriFor(System.SCREEN_AUTO_BRIGHTNESS_ADJ);
    }

    private ContentObserver mBrightnessObserver =
        new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updatedSummary(mPreference);
            }
        };

    public BrightnessLevelPreferenceController(Context context, Lifecycle lifecycle) {
        this(context, lifecycle, new PowerManagerWrapper(
                (PowerManager) context.getSystemService(Context.POWER_SERVICE)));
    }

    @VisibleForTesting
    public BrightnessLevelPreferenceController(Context context, Lifecycle lifecycle,
            PowerManagerWrapper powerManagerWrapper) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mMinBrightness = powerManagerWrapper.getMinimumScreenBrightnessSetting();
        mMaxBrightness = powerManagerWrapper.getMaximumScreenBrightnessSetting();
        mMinVrBrightness = powerManagerWrapper.getMinimumScreenBrightnessForVrSetting();
        mMaxVrBrightness = powerManagerWrapper.getMaximumScreenBrightnessForVrSetting();
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
        mContentResolver.registerContentObserver(BRIGHTNESS_MODE_URI, false, mBrightnessObserver);
        mContentResolver.registerContentObserver(BRIGHTNESS_URI, false, mBrightnessObserver);
        mContentResolver.registerContentObserver(BRIGHTNESS_FOR_VR_URI, false, mBrightnessObserver);
        mContentResolver.registerContentObserver(BRIGHTNESS_ADJ_URI, false, mBrightnessObserver);
    }

    @Override
    public void onStop() {
        mContentResolver.unregisterContentObserver(mBrightnessObserver);
    }

    private void updatedSummary(Preference preference) {
        if (preference != null) {
            preference.setSummary(NumberFormat.getPercentInstance().format(getCurrentBrightness()));
        }
    }

    private double getCurrentBrightness() {
        if (isInVrMode()) {
            final double value = System.getInt(mContentResolver, System.SCREEN_BRIGHTNESS_FOR_VR,
                    mMaxBrightness);
            return getPercentage(value, mMinVrBrightness, mMaxVrBrightness);
        }
        final int brightnessMode = Settings.System.getInt(mContentResolver,
                System.SCREEN_BRIGHTNESS_MODE, System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (brightnessMode == System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            final float value = Settings.System.getFloat(mContentResolver,
                    System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0);
            // auto brightness is between -1 and 1
            return getPercentage(value, -1, 1);
        }
        final double value = Settings.System.getInt(mContentResolver, System.SCREEN_BRIGHTNESS,
                mMinBrightness);
        return getPercentage(value, mMinBrightness, mMaxBrightness);
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

    @VisibleForTesting
    boolean isInVrMode() {
        try {
            return IVrManager.Stub.asInterface(ServiceManager.getService(Context.VR_SERVICE))
                    .getVrModeState();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to check vr mode!", e);
        }
        return false;
    }
}
