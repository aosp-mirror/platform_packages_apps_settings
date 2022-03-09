/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.util.SparseIntArray;

import com.android.internal.os.BatterySipper;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.fuelgauge.Estimate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PowerUsageFeatureProviderImpl implements PowerUsageFeatureProvider {

    private static final String PACKAGE_CALENDAR_PROVIDER = "com.android.providers.calendar";
    private static final String PACKAGE_MEDIA_PROVIDER = "com.android.providers.media";
    private static final String PACKAGE_SYSTEMUI = "com.android.systemui";
    private static final String[] PACKAGES_SYSTEM = {PACKAGE_MEDIA_PROVIDER,
            PACKAGE_CALENDAR_PROVIDER, PACKAGE_SYSTEMUI};

    protected PackageManager mPackageManager;
    protected Context mContext;

    public PowerUsageFeatureProviderImpl(Context context) {
        mPackageManager = context.getPackageManager();
        mContext = context.getApplicationContext();
    }

    @Override
    public boolean isTypeService(BatterySipper sipper) {
        return false;
    }

    @Override
    public boolean isTypeSystem(BatterySipper sipper) {
        final int uid = sipper.uidObj == null ? -1 : sipper.getUid();
        sipper.mPackages = mPackageManager.getPackagesForUid(uid);
        // Classify all the sippers to type system if the range of uid is 0...FIRST_APPLICATION_UID
        if (uid >= Process.ROOT_UID && uid < Process.FIRST_APPLICATION_UID) {
            return true;
        } else if (sipper.mPackages != null) {
            for (final String packageName : sipper.mPackages) {
                if (ArrayUtils.contains(PACKAGES_SYSTEM, packageName)) {
                    return true;
                }
            }
        }

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
    public boolean isLocationSettingEnabled(String[] packages) {
        return false;
    }

    @Override
    public boolean isAdditionalBatteryInfoEnabled() {
        return false;
    }

    @Override
    public Intent getAdditionalBatteryInfoIntent() {
        return null;
    }

    @Override
    public boolean isAdvancedUiEnabled() {
        return true;
    }

    @Override
    public boolean isPowerAccountingToggleEnabled() {
        return true;
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
    public String getAdvancedUsageScreenInfoString() {
        return null;
    }

    @Override
    public boolean getEarlyWarningSignal(Context context, String id) {
        return false;
    }

    @Override
    public boolean isSmartBatterySupported() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_smart_battery_available);
    }

    @Override
    public boolean isChartGraphEnabled(Context context) {
        return false;
    }

    @Override
    public boolean isChartGraphSlotsEnabled(Context context) {
        return false;
    }

    @Override
    public boolean isAdaptiveChargingSupported() {
        return false;
    }

    @Override
    public Intent getResumeChargeIntent() {
        return null;
    }

    @Override
    public Map<Long, Map<String, BatteryHistEntry>> getBatteryHistory(Context context) {
        return null;
    }

    @Override
    public Uri getBatteryHistoryUri() {
        return null;
    }

    @Override
    public Set<CharSequence> getHideBackgroundUsageTimeSet(Context context) {
        return new HashSet<>();
    }

    @Override
    public CharSequence[] getHideApplicationEntries(Context context) {
        return new CharSequence[0];
    }

    @Override
    public CharSequence[] getHideApplicationSummary(Context context) {
        return new CharSequence[0];
    }
}
