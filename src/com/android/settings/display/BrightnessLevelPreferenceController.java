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

import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX;
import static com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MIN;
import static com.android.settingslib.display.BrightnessUtils.convertLinearToGammaFloat;

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
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

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
    private static final Uri BRIGHTNESS_URI;
    private static final Uri BRIGHTNESS_FOR_VR_URI;
    private static final Uri BRIGHTNESS_ADJ_URI;

    private final float mMinBrightness;
    private final float mMaxBrightness;
    private final float mMinVrBrightness;
    private final float mMaxVrBrightness;
    private final ContentResolver mContentResolver;

    private Preference mPreference;

    static {
        BRIGHTNESS_URI = System.getUriFor(System.SCREEN_BRIGHTNESS_FLOAT);
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
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mMinBrightness = powerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MINIMUM);
        mMaxBrightness = powerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MAXIMUM);
        mMinVrBrightness = powerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MINIMUM_VR);
        mMaxVrBrightness = powerManager.getBrightnessConstraint(
                PowerManager.BRIGHTNESS_CONSTRAINT_TYPE_MAXIMUM_VR);
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
        final int value;
        if (isInVrMode()) {
            value = convertLinearToGammaFloat(System.getFloat(mContentResolver,
                    System.SCREEN_BRIGHTNESS_FOR_VR_FLOAT, mMaxBrightness),
                    mMinVrBrightness, mMaxVrBrightness);
        } else {
            value = convertLinearToGammaFloat(Settings.System.getFloat(mContentResolver,
                    System.SCREEN_BRIGHTNESS_FLOAT, mMinBrightness),
                    mMinBrightness, mMaxBrightness);
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

    @VisibleForTesting
    IVrManager safeGetVrManager() {
        return IVrManager.Stub.asInterface(ServiceManager.getService(
                Context.VR_SERVICE));
    }

    @VisibleForTesting
    boolean isInVrMode() {
        IVrManager vrManager = safeGetVrManager();
        if (vrManager != null) {
            try {
                return vrManager.getVrModeState();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to check vr mode!", e);
            }
        }
        return false;
    }
}
