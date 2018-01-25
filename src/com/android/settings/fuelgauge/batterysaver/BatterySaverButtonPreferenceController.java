/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;

import com.android.settings.fuelgauge.BatteryBroadcastReceiver;
import com.android.settings.widget.TwoStateButtonPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller to update the battery saver button
 */
//TODO(b/72228477): disable the button if device is charging.
public class BatterySaverButtonPreferenceController extends
        TwoStateButtonPreferenceController implements
        LifecycleObserver, OnStart, OnStop, BatteryBroadcastReceiver.OnBatteryChangedListener {
    private static final String KEY = "battery_saver_button_container";
    private BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    @VisibleForTesting
    PowerManager mPowerManager;

    public BatterySaverButtonPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(context);
        mBatteryBroadcastReceiver.setBatteryChangedListener(this);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        mBatteryBroadcastReceiver.register();
    }

    @Override
    public void onStop() {
        mBatteryBroadcastReceiver.unRegister();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean lowPowerModeOn = mPowerManager.isPowerSaveMode();
        updateButton(!lowPowerModeOn);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onBatteryChanged() {
        final boolean lowPowerModeOn = mPowerManager.isPowerSaveMode();
        updateButton(!lowPowerModeOn);
    }

    @Override
    public void onButtonClicked(boolean stateOn) {
        mPowerManager.setPowerSaveMode(stateOn);
    }
}