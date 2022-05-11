/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.display.SmartAutoRotateController.isRotationResolverServiceAvailable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.BannerMessagePreference;

/**
 * The controller of camera based rotate battery saver warning preference. The preference appears
 * when battery saver mode is enabled.
 */
public class SmartAutoRotateBatterySaverController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private Preference mPreference;
    private final PowerManager mPowerManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPreference == null) {
                return;
            }
            mPreference.setVisible(isAvailable());
            updateState(mPreference);
        }
    };

    public SmartAutoRotateBatterySaverController(Context context, String key) {
        super(context, key);
        mPowerManager = context.getSystemService(PowerManager.class);
    }

    @VisibleForTesting
    boolean isPowerSaveMode() {
        return mPowerManager.isPowerSaveMode();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        ((BannerMessagePreference) mPreference)
                .setPositiveButtonText(R.string.ambient_camera_battery_saver_off)
                .setPositiveButtonOnClickListener(v -> {
                    mPowerManager.setPowerSaveModeEnabled(false);
                });
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isRotationResolverServiceAvailable(mContext)
                && isPowerSaveMode() ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }
}
