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
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

import java.util.List;

/** Feature provider for battery settings usage. */
public interface BatterySettingsFeatureProvider {

    /** Returns true if manufacture date should be shown */
    boolean isManufactureDateAvailable(Context context, long manufactureDateMs);

    /** Returns true if first use date should be shown */
    boolean isFirstUseDateAvailable(Context context, long firstUseDateMs);

    /** Check whether the battery information page is enabled in the About phone page */
    boolean isBatteryInfoEnabled(Context context);

    /** A way to add more battery tip detectors. */
    void addBatteryTipDetector(
            Context context,
            List<BatteryTip> batteryTips,
            BatteryInfo batteryInfo,
            BatteryTipPolicy batteryTipPolicy);

    /** Return a label for the bottom summary during wireless charging. */
    @Nullable
    CharSequence getWirelessChargingLabel(@NonNull Context context, @NonNull BatteryInfo info);

    /** Return a content description for the bottom summary during wireless charging. */
    @Nullable
    CharSequence getWirelessChargingContentDescription(
            @NonNull Context context, @NonNull BatteryInfo info);

    /** Return a charging remaining time label for wireless charging. */
    @Nullable
    CharSequence getWirelessChargingRemainingLabel(
            @NonNull Context context, long remainingTimeMs, long currentTimeMs);

    /** Return true if it's in the charging optimization mode. */
    boolean isChargingOptimizationMode(@NonNull Context context);

    /** Return a charging remaining time label for charging optimization mode. */
    @Nullable
    CharSequence getChargingOptimizationRemainingLabel(
            @NonNull Context context,
            int batteryLevel,
            int pluggedStatus,
            long chargeRemainingTimeMs,
            long currentTimeMs);

    /** Return a charge label for charging optimization mode. */
    @Nullable
    CharSequence getChargingOptimizationChargeLabel(
            @NonNull Context context,
            int batteryLevel,
            String batteryPercentageString,
            long chargeRemainingTimeMs,
            long currentTimeMs);
}
