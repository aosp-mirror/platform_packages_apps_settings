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

import static com.android.settings.Utils.SYSTEMUI_PACKAGE_NAME;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Process;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseIntArray;

import com.android.internal.util.ArrayUtils;
import com.android.settings.fuelgauge.batteryusage.DetectRequestSourceType;
import com.android.settings.fuelgauge.batteryusage.PowerAnomalyEventList;
import com.android.settingslib.fuelgauge.Estimate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Implementation of {@code PowerUsageFeatureProvider} */
public class PowerUsageFeatureProviderImpl implements PowerUsageFeatureProvider {

    private static final String PACKAGE_CALENDAR_PROVIDER = "com.android.providers.calendar";
    private static final String PACKAGE_MEDIA_PROVIDER = "com.android.providers.media";
    private static final String[] PACKAGES_SYSTEM = {
        PACKAGE_MEDIA_PROVIDER, PACKAGE_CALENDAR_PROVIDER, SYSTEMUI_PACKAGE_NAME
    };

    protected PackageManager mPackageManager;
    protected Context mContext;

    public PowerUsageFeatureProviderImpl(Context context) {
        mPackageManager = context.getPackageManager();
        mContext = context.getApplicationContext();
    }

    @Override
    public boolean isTypeService(int uid) {
        return false;
    }

    @Override
    public boolean isTypeSystem(int uid, String[] packages) {
        // Classify all the sippers to type system if the range of uid is 0...FIRST_APPLICATION_UID
        if (uid >= Process.ROOT_UID && uid < Process.FIRST_APPLICATION_UID) {
            return true;
        } else if (packages != null) {
            for (final String packageName : packages) {
                if (ArrayUtils.contains(PACKAGES_SYSTEM, packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isBatteryUsageEnabled() {
        return true;
    }

    @Override
    public boolean isBatteryTipsEnabled() {
        return false;
    }

    @Override
    public double getBatteryUsageListScreenOnTimeThresholdInMs() {
        return 0;
    }

    @Override
    public double getBatteryUsageListConsumePowerThreshold() {
        return 0;
    }

    @Override
    public List<String> getSystemAppsAllowlist() {
        return new ArrayList<>();
    }

    @Override
    public boolean isLocationSettingEnabled(String[] packages) {
        return false;
    }

    @Override
    public Intent getAdditionalBatteryInfoIntent() {
        return null;
    }

    @Override
    public Estimate getEnhancedBatteryPrediction(Context context) {
        return null;
    }

    @Override
    public SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long zeroTime) {
        return null;
    }

    @Override
    public boolean isEnhancedBatteryPredictionEnabled(Context context) {
        return false;
    }

    @Override
    public String getEnhancedEstimateDebugString(String timeRemaining) {
        return null;
    }

    @Override
    public boolean isEstimateDebugEnabled() {
        return false;
    }

    @Override
    public String getOldEstimateDebugString(String timeRemaining) {
        return null;
    }

    @Override
    public boolean isSmartBatterySupported() {
        return mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_smart_battery_available);
    }

    @Override
    public boolean isChartGraphSlotsEnabled(Context context) {
        return false;
    }

    @Override
    public Intent getResumeChargeIntent(boolean isDockDefender) {
        return null;
    }

    @Override
    public String getFullChargeIntentAction() {
        return Intent.ACTION_BATTERY_LEVEL_CHANGED;
    }

    @Override
    public boolean isExtraDefend() {
        return false;
    }

    @Override
    public boolean delayHourlyJobWhenBooting() {
        return true;
    }

    @Override
    public PowerAnomalyEventList detectSettingsAnomaly(
            Context context, double displayDrain, DetectRequestSourceType detectRequestSourceType) {
        return null;
    }

    @Override
    public Set<Integer> getOthersSystemComponentSet() {
        return new ArraySet<>();
    }

    @Override
    public Set<String> getOthersCustomComponentNameSet() {
        return new ArraySet<>();
    }

    @Override
    public Set<Integer> getHideSystemComponentSet() {
        return new ArraySet<>();
    }

    @Override
    public Set<String> getHideApplicationSet() {
        return new ArraySet<>();
    }

    @Override
    public Set<String> getHideBackgroundUsageTimeSet() {
        return new ArraySet<>();
    }

    @Override
    public Set<String> getIgnoreScreenOnTimeTaskRootSet() {
        return new ArraySet<>();
    }

    @Override
    public String getBuildMetadata1(Context context) {
        return null;
    }

    @Override
    public String getBuildMetadata2(Context context) {
        return null;
    }

    @Override
    public boolean isValidToRestoreOptimizationMode(ArrayMap<String, String> deviceInfoMap) {
        return false;
    }
}
