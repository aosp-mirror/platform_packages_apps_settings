/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.provider.SearchIndexableResource;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceGroup;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.fuelgauge.PowerUsageAdvanced.PowerUsageData.UsageType;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PowerUsageAdvanced extends PowerUsageBase {
    private static final String TAG = "AdvancedBatteryUsage";
    private static final String KEY_BATTERY_GRAPH = "battery_graph";
    private static final String KEY_BATTERY_USAGE_LIST = "battery_usage_list";

    @VisibleForTesting
    final int[] mUsageTypes = {
            UsageType.WIFI,
            UsageType.CELL,
            UsageType.SERVICE,
            UsageType.SYSTEM,
            UsageType.BLUETOOTH,
            UsageType.USER,
            UsageType.IDLE,
            UsageType.APP};
    private BatteryHistoryPreference mHistPref;
    private PreferenceGroup mUsageListGroup;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private PackageManager mPackageManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHistPref = (BatteryHistoryPreference) findPreference(KEY_BATTERY_GRAPH);
        mUsageListGroup = (PreferenceGroup) findPreference(KEY_BATTERY_USAGE_LIST);

        final Context context = getContext();
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
        mPackageManager = context.getPackageManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.FUELGAUGE_BATTERY_HISTORY_DETAIL;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_advanced;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        return null;
    }

    @Override
    protected void refreshStats() {
        super.refreshStats();

        updatePreference(mHistPref);

        List<PowerUsageData> dataList = parsePowerUsageData(mStatsHelper);
        mUsageListGroup.removeAll();
        for (int i = 0, size = dataList.size(); i < size; i++) {
            final PowerUsageData batteryData = dataList.get(i);
            final PowerGaugePreference pref = new PowerGaugePreference(getContext());

            pref.setTitle(batteryData.titleResId);
            pref.setSummary(batteryData.summary);
            pref.setPercent(batteryData.percentage);
            mUsageListGroup.addPreference(pref);
        }
    }

    @VisibleForTesting
    @UsageType int extractUsageType(BatterySipper sipper) {
        final DrainType drainType = sipper.drainType;
        final int uid = sipper.getUid();

        // TODO(b/34385770): add logic to extract type service
        if (drainType == DrainType.WIFI) {
            return UsageType.WIFI;
        } else if (drainType == DrainType.BLUETOOTH) {
            return UsageType.BLUETOOTH;
        } else if (drainType == DrainType.IDLE) {
            return UsageType.IDLE;
        } else if (drainType == DrainType.USER) {
            return UsageType.USER;
        } else if (drainType == DrainType.CELL) {
            return UsageType.CELL;
        } else if (mPowerUsageFeatureProvider.isTypeSystem(sipper)) {
            return UsageType.SYSTEM;
        } else if (mPowerUsageFeatureProvider.isTypeService(sipper)) {
            return UsageType.SERVICE;
        } else {
            return UsageType.APP;
        }
    }

    @VisibleForTesting
    List<PowerUsageData> parsePowerUsageData(BatteryStatsHelper statusHelper) {
        final List<BatterySipper> batterySippers = statusHelper.getUsageList();
        final Map<Integer, PowerUsageData> batteryDataMap = new HashMap<>();

        for (final @UsageType Integer type : mUsageTypes) {
            batteryDataMap.put(type, new PowerUsageData(type));
        }

        // Accumulate power usage based on usage type
        for (final BatterySipper sipper : batterySippers) {
            sipper.mPackages = mPackageManager.getPackagesForUid(sipper.getUid());
            final PowerUsageData usageData = batteryDataMap.get(extractUsageType(sipper));
            usageData.totalPowerMah += sipper.totalPowerMah;
        }

        // TODO(b/34385770): add logic to extract the summary
        final List<PowerUsageData> batteryDataList = new ArrayList<>(batteryDataMap.values());
        final double totalPower = statusHelper.getTotalPower();
        for (final PowerUsageData usageData : batteryDataList) {
            usageData.percentage = (usageData.totalPowerMah / totalPower) * 100;
        }

        Collections.sort(batteryDataList);

        return batteryDataList;
    }

    @VisibleForTesting
    void setPackageManager(PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    @VisibleForTesting
    void setPowerUsageFeatureProvider(PowerUsageFeatureProvider provider) {
        mPowerUsageFeatureProvider = provider;
    }

    /**
     * Class that contains data used in {@link PowerGaugePreference}.
     */
    @VisibleForTesting
    static class PowerUsageData implements Comparable<PowerUsageData> {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({UsageType.APP,
                UsageType.WIFI,
                UsageType.CELL,
                UsageType.SERVICE,
                UsageType.SYSTEM,
                UsageType.BLUETOOTH,
                UsageType.USER,
                UsageType.IDLE})
        public @interface UsageType {
            int APP = 0;
            int WIFI = 1;
            int CELL = 2;
            int SERVICE = 3;
            int SYSTEM = 4;
            int BLUETOOTH = 5;
            int USER = 6;
            int IDLE = 7;
        }

        @StringRes
        public int titleResId;
        public String summary;
        public double percentage;
        public double totalPowerMah;
        @ColorInt
        public int iconColor;
        @UsageType
        public int usageType;

        public PowerUsageData(@UsageType int usageType) {
            this(usageType, 0);
        }

        public PowerUsageData(@UsageType int usageType, double totalPower) {
            this.usageType = usageType;
            totalPowerMah = 0;
            titleResId = getTitleResId(usageType);
            totalPowerMah = totalPower;
        }

        private int getTitleResId(@UsageType int usageType) {
            switch (usageType) {
                case UsageType.WIFI:
                    return R.string.power_wifi;
                case UsageType.CELL:
                    return R.string.power_cell;
                case UsageType.SERVICE:
                    return R.string.power_service;
                case UsageType.SYSTEM:
                    return R.string.power_system;
                case UsageType.BLUETOOTH:
                    return R.string.power_bluetooth;
                case UsageType.USER:
                    return R.string.power_user;
                case UsageType.IDLE:
                    return R.string.power_idle;
                case UsageType.APP:
                default:
                    return R.string.power_apps;
            }
        }

        @Override
        public int compareTo(@NonNull PowerUsageData powerUsageData) {
            final int diff = Double.compare(powerUsageData.totalPowerMah, totalPowerMah);
            return diff != 0 ? diff : usageType - powerUsageData.usageType;
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_advanced;
                    return Arrays.asList(sir);
                }
            };

}
