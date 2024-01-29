/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.util.Log;

import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.IncompatibleChargerTip;
import com.android.settingslib.Utils;

/** Detect whether it is in the incompatible charging state */
public final class IncompatibleChargerDetector implements BatteryTipDetector {
    private static final String TAG = "IncompatibleChargerDetector";

    private final Context mContext;

    public IncompatibleChargerDetector(Context context) {
        mContext = context;
    }

    @Override
    public BatteryTip detect() {
        final boolean isIncompatibleCharging = Utils.containsIncompatibleChargers(mContext, TAG);
        final int state =
                isIncompatibleCharging ? BatteryTip.StateType.NEW : BatteryTip.StateType.INVISIBLE;
        Log.d(
                TAG,
                "detect() state= " + state + " isIncompatibleCharging: " + isIncompatibleCharging);
        return new IncompatibleChargerTip(state);
    }
}
