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

import com.android.settings.fuelgauge.BatterySaverReceiver;
import com.android.settings.widget.TwoStateButtonPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

/**
 * Controller to update the battery saver button
 */
public class BatterySaverButtonPreferenceController extends
        TwoStateButtonPreferenceController implements
        LifecycleObserver, OnStart, OnStop, BatterySaverReceiver.BatterySaverListener {
    private static final String KEY = "battery_saver_button_container";
    private BatterySaverReceiver mBatterySaverReceiver;
    @VisibleForTesting
    PowerManager mPowerManager;

    public BatterySaverButtonPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mBatterySaverReceiver = new BatterySaverReceiver(context);
        mBatterySaverReceiver.setBatterySaverListener(this);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void onStart() {
        mBatterySaverReceiver.setListening(true);
    }

    @Override
    public void onStop() {
        mBatterySaverReceiver.setListening(false);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        setButtonVisibility(!mPowerManager.isPowerSaveMode());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void onButtonClicked(boolean stateOn) {
        // This screen already shows a warning, so we don't need another warning.
        BatterySaverUtils.setPowerSaveMode(mContext,  stateOn, /*needFirstTimeWarning*/ false);
    }

    @Override
    public void onPowerSaveModeChanged() {
        setButtonVisibility(!mPowerManager.isPowerSaveMode());
    }

    @Override
    public void onBatteryChanged(boolean pluggedIn) {
        setButtonEnabled(!pluggedIn);
    }
}
