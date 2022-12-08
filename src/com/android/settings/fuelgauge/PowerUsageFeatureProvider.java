/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.content.Intent;
import android.util.SparseIntArray;

import com.android.settingslib.fuelgauge.Estimate;

import java.util.Set;

/**
 * Feature Provider used in power usage
 */
public interface PowerUsageFeatureProvider {

    /**
     * Check whether the battery usage button is enabled in the battery page
     */
    boolean isBatteryUsageEnabled(Context context);

    /**
     * Check whether location setting is enabled
     */
    boolean isLocationSettingEnabled(String[] packages);

    /**
     * Check whether additional battery info feature is enabled.
     */
    boolean isAdditionalBatteryInfoEnabled();

    /**
     * Gets an {@link Intent} to show additional battery info.
     */
    Intent getAdditionalBatteryInfoIntent();

    /**
     * Check whether advanced ui is enabled
     */
    boolean isAdvancedUiEnabled();

    /**
     * Check whether it is type service
     */
    boolean isTypeService(int uid);

    /**
     * Check whether it is type system
     */
    boolean isTypeSystem(int uid, String[] packages);

    /**
     * Check whether the toggle for power accounting is enabled
     */
    boolean isPowerAccountingToggleEnabled();

    /**
     * Returns an improved prediction for battery time remaining.
     */
    Estimate getEnhancedBatteryPrediction(Context context);

    /**
     * Returns an improved projection curve for future battery level.
     *
     * @param zeroTime timestamps (array keys) are shifted by this amount
     */
    SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long zeroTime);

    /**
     * Checks whether the toggle for enhanced battery predictions is enabled.
     */
    boolean isEnhancedBatteryPredictionEnabled(Context context);

    /**
     * Checks whether debugging should be enabled for battery estimates.
     */
    boolean isEstimateDebugEnabled();

    /**
     * Converts the provided string containing the remaining time into a debug string for enhanced
     * estimates.
     *
     * @return A string containing the estimate and a label indicating it is an enhanced estimate
     */
    String getEnhancedEstimateDebugString(String timeRemaining);

    /**
     * Converts the provided string containing the remaining time into a debug string.
     *
     * @return A string containing the estimate and a label indicating it is a normal estimate
     */
    String getOldEstimateDebugString(String timeRemaining);

    /**
     * Returns a signal to indicate if the device will need to warn the user they may not make it
     * to their next charging time.
     *
     * @param id Optional string used to identify the caller for metrics. Usually the class name of
     *           the caller
     */
    boolean getEarlyWarningSignal(Context context, String id);

    /**
     * Checks whether smart battery feature is supported in this device
     */
    boolean isSmartBatterySupported();

    /**
     * Checks whether we should show usage information by slots or not.
     */
    boolean isChartGraphSlotsEnabled(Context context);

    /**
     * Checks whether adaptive charging feature is supported in this device
     */
    boolean isAdaptiveChargingSupported();

    /**
     * Returns {@code true} if current defender mode is extra defend
     */
    boolean isExtraDefend();

    /**
     * Gets a intent for one time bypass charge limited to resume charging.
     */
    Intent getResumeChargeIntent(boolean isDockDefender);

    /**
     * Returns {@link Set} for hidding applications background usage time.
     */
    Set<CharSequence> getHideBackgroundUsageTimeSet(Context context);

    /**
     * Returns package names for hidding application in the usage screen.
     */
    CharSequence[] getHideApplicationEntries(Context context);
}
