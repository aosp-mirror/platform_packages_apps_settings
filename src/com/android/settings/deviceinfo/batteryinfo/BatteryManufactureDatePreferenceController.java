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
import android.os.BatteryManager;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;

import java.util.concurrent.TimeUnit;

/**
 * A controller that manages the information about battery manufacture date.
 */
public class BatteryManufactureDatePreferenceController extends BasePreferenceController {

    private final BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;
    private final BatteryManager mBatteryManager;

    private long mManufactureDateInMs;

    public BatteryManufactureDatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatterySettingsFeatureProvider = FeatureFactory.getFeatureFactory()
                .getBatterySettingsFeatureProvider();
        mBatteryManager = mContext.getSystemService(BatteryManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return mBatterySettingsFeatureProvider.isManufactureDateAvailable(mContext,
                getManufactureDate())
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return isAvailable()
                ? BatteryUtils.getBatteryInfoFormattedDate(mManufactureDateInMs)
                : null;
    }

    private long getManufactureDate() {
        if (mManufactureDateInMs == 0L) {
            final long manufactureDateInSec = mBatteryManager.getLongProperty(
                    BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE);
            mManufactureDateInMs = TimeUnit.MILLISECONDS.convert(manufactureDateInSec,
                    TimeUnit.SECONDS);
        }
        return mManufactureDateInMs;
    }
}
