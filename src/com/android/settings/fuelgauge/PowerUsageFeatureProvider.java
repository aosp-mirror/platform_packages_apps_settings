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
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.SparseIntArray;

import com.android.settings.fuelgauge.batteryusage.DetectRequestSourceType;
import com.android.settings.fuelgauge.batteryusage.PowerAnomalyEventList;
import com.android.settingslib.fuelgauge.Estimate;

import java.util.List;
import java.util.Set;

/** Feature Provider used in power usage */
public interface PowerUsageFeatureProvider {

    /** Check whether the battery usage button is enabled in the battery page */
    boolean isBatteryUsageEnabled();

    /** Check whether the battery tips card is enabled in the battery usage page */
    boolean isBatteryTipsEnabled();

    /**
     * Returns a threshold (in milliseconds) for the minimal screen on time in battery usage list
     */
    double getBatteryUsageListScreenOnTimeThresholdInMs();

    /** Returns a threshold (mA) for the minimal comsume power in battery usage list */
    double getBatteryUsageListConsumePowerThreshold();

    /** Returns an allowlist of app names combined into the system-apps item */
    List<String> getSystemAppsAllowlist();

    /** Check whether location setting is enabled */
    boolean isLocationSettingEnabled(String[] packages);

    /** Gets an {@link Intent} to show additional battery info */
    Intent getAdditionalBatteryInfoIntent();

    /** Check whether it is type service */
    boolean isTypeService(int uid);

    /** Check whether it is type system */
    boolean isTypeSystem(int uid, String[] packages);

    /** Returns an improved prediction for battery time remaining */
    Estimate getEnhancedBatteryPrediction(Context context);

    /**
     * Returns an improved projection curve for future battery level
     *
     * @param zeroTime timestamps (array keys) are shifted by this amount
     */
    SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long zeroTime);

    /** Checks whether the toggle for enhanced battery predictions is enabled */
    boolean isEnhancedBatteryPredictionEnabled(Context context);

    /** Checks whether debugging should be enabled for battery estimates */
    boolean isEstimateDebugEnabled();

    /**
     * Converts the provided string containing the remaining time into a debug string for enhanced
     * estimates
     *
     * @return A string containing the estimate and a label indicating it is an enhanced estimate
     */
    String getEnhancedEstimateDebugString(String timeRemaining);

    /**
     * Converts the provided string containing the remaining time into a debug string
     *
     * @return A string containing the estimate and a label indicating it is a normal estimate
     */
    String getOldEstimateDebugString(String timeRemaining);

    /** Checks whether smart battery feature is supported in this device */
    boolean isSmartBatterySupported();

    /** Checks whether we should show usage information by slots or not */
    boolean isChartGraphSlotsEnabled(Context context);

    /** Returns {@code true} if current defender mode is extra defend */
    boolean isExtraDefend();

    /** Returns {@code true} if delay the hourly job when device is booting */
    boolean delayHourlyJobWhenBooting();

    /** Returns {@link Bundle} for settings anomaly detection result */
    PowerAnomalyEventList detectSettingsAnomaly(
            Context context, double displayDrain, DetectRequestSourceType detectRequestSourceType);

    /** Gets an intent for one time bypass charge limited to resume charging. */
    Intent getResumeChargeIntent(boolean isDockDefender);

    /** Returns the intent action used to mark as the full charge start event. */
    String getFullChargeIntentAction();

    /** Returns {@link Set} for the system component ids which are combined into others */
    Set<Integer> getOthersSystemComponentSet();

    /** Returns {@link Set} for the custom system component names which are combined into others */
    Set<String> getOthersCustomComponentNameSet();

    /** Returns {@link Set} for hiding system component ids in the usage screen */
    Set<Integer> getHideSystemComponentSet();

    /** Returns {@link Set} for hiding application package names in the usage screen */
    Set<String> getHideApplicationSet();

    /** Returns {@link Set} for hiding applications background usage time */
    Set<String> getHideBackgroundUsageTimeSet();

    /** Returns {@link Set} for ignoring task root class names for screen on time */
    Set<String> getIgnoreScreenOnTimeTaskRootSet();

    /** Returns the customized device build information for data backup */
    String getBuildMetadata1(Context context);

    /** Returns the customized device build information for data backup */
    String getBuildMetadata2(Context context);

    /** Whether the app optimization mode is valid to restore */
    boolean isValidToRestoreOptimizationMode(ArrayMap<String, String> deviceInfoMap);
}
