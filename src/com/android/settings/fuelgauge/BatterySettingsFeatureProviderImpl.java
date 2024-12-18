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

package com.android.settings.fuelgauge;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.detectors.LowBatteryDetector;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

import java.util.List;

/** Feature provider implementation for battery settings usage. */
public class BatterySettingsFeatureProviderImpl implements BatterySettingsFeatureProvider {

    @Override
    public boolean isManufactureDateAvailable(Context context, long manufactureDateMs) {
        return false;
    }

    @Override
    public boolean isFirstUseDateAvailable(Context context, long firstUseDateMs) {
        return false;
    }

    @Override
    public boolean isBatteryInfoEnabled(Context context) {
        return false;
    }

    @Override
    public void addBatteryTipDetector(
            Context context,
            List<BatteryTip> batteryTips,
            BatteryInfo batteryInfo,
            BatteryTipPolicy batteryTipPolicy) {
        batteryTips.add(new LowBatteryDetector(context, batteryTipPolicy, batteryInfo).detect());
    }

    @Override
    @Nullable
    public CharSequence getWirelessChargingLabel(
            @NonNull Context context, @NonNull BatteryInfo info) {
        return null;
    }

    @Nullable
    @Override
    public CharSequence getWirelessChargingContentDescription(
            @NonNull Context context, @NonNull BatteryInfo info) {
        return null;
    }

    @Nullable
    @Override
    public CharSequence getWirelessChargingRemainingLabel(
            @NonNull Context context, long remainingTimeMs, long currentTimeMs) {
        return null;
    }

    @Override
    public boolean isChargingOptimizationMode(@NonNull Context context) {
        return false;
    }

    @Nullable
    @Override
    public CharSequence getChargingOptimizationRemainingLabel(
            @NonNull Context context,
            int batteryLevel,
            int pluggedStatus,
            long chargeRemainingTimeMs,
            long currentTimeMs) {
        return null;
    }

    @Nullable
    @Override
    public CharSequence getChargingOptimizationChargeLabel(
            @NonNull Context context,
            int batteryLevel,
            String batteryPercentageString,
            long chargeRemainingTimeMs,
            long currentTimeMs) {
        return null;
    }
}
