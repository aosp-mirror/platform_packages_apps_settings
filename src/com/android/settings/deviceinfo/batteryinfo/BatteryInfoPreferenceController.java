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

package com.android.settings.deviceinfo.batteryinfo;

import android.content.Context;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

/** Controller to enter battery information page. */
public class BatteryInfoPreferenceController extends BasePreferenceController {

    private final BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;

    public BatteryInfoPreferenceController(Context context, String key) {
        super(context, key);
        mBatterySettingsFeatureProvider = FeatureFactory.getFeatureFactory()
                .getBatterySettingsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mBatterySettingsFeatureProvider.isBatteryInfoEnabled(mContext) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}
