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

package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip;
import com.android.settings.overlay.FeatureFactory;

/**
 * Detector whether to early warning tip.
 */
public class EarlyWarningDetector implements BatteryTipDetector {
    private BatteryTipPolicy mPolicy;
    private PowerManager mPowerManager;
    private Context mContext;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    public EarlyWarningDetector(BatteryTipPolicy policy, Context context) {
        mPolicy = policy;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mContext = context;
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(
                context).getPowerUsageFeatureProvider(context);
    }

    @Override
    public BatteryTip detect() {
        final Intent batteryBroadcast = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final boolean discharging =
                batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0;
        final boolean powerSaveModeOn = mPowerManager.isPowerSaveMode();
        final boolean earlyWarning = mPowerUsageFeatureProvider.getEarlyWarningSignal(mContext,
                EarlyWarningDetector.class.getName()) || mPolicy.testBatterySaverTip;

        final int state = powerSaveModeOn
                ? BatteryTip.StateType.HANDLED
                : mPolicy.batterySaverTipEnabled && discharging && earlyWarning
                        ? BatteryTip.StateType.NEW
                        : BatteryTip.StateType.INVISIBLE;
        return new EarlyWarningTip(state, powerSaveModeOn);
    }
}
