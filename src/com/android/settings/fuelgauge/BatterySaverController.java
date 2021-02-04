/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

/**
 * Controller to update the battery saver entry preference.
 */
public class BatterySaverController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop, BatterySaverReceiver.BatterySaverListener {
    private static final String KEY_BATTERY_SAVER = "battery_saver_summary";
    private final BatterySaverReceiver mBatteryStateChangeReceiver;
    private final PowerManager mPowerManager;

    @VisibleForTesting
    PrimarySwitchPreference mBatterySaverPref;

    public BatterySaverController(Context context) {
        super(context, KEY_BATTERY_SAVER);

        mPowerManager = mContext.getSystemService(PowerManager.class);
        mBatteryStateChangeReceiver = new BatterySaverReceiver(context);
        mBatteryStateChangeReceiver.setBatterySaverListener(this);
        BatterySaverUtils.revertScheduleToNoneIfNeeded(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_SAVER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatterySaverPref = screen.findPreference(KEY_BATTERY_SAVER);
    }

    @Override
    public void onStart() {

        mBatteryStateChangeReceiver.setListening(true);
    }

    @Override
    public void onStop() {
        mBatteryStateChangeReceiver.setListening(false);
    }

    @Override
    public void onPowerSaveModeChanged() {
        final boolean isChecked = isChecked();
        if (mBatterySaverPref != null && mBatterySaverPref.isChecked() != isChecked) {
            mBatterySaverPref.setChecked(isChecked);
        }
    }

    @Override
    public void onBatteryChanged(boolean pluggedIn) {
        if (mBatterySaverPref != null) {
            mBatterySaverPref.setSwitchEnabled(!pluggedIn);
        }
    }

    @Override
    public boolean isChecked() {
        return mPowerManager.isPowerSaveMode();
    }

    @Override
    public boolean setChecked(boolean stateOn) {
        return BatterySaverUtils.setPowerSaveMode(mContext, stateOn,
            false /* needFirstTimeWarning */);
    }
}
