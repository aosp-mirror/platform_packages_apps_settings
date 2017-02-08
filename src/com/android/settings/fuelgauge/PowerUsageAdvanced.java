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
import android.os.Bundle;
import android.os.Process;
import android.provider.SearchIndexableResource;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerUsageAdvanced extends PowerUsageBase {
    private static final String TAG = "AdvancedBatteryUsage";
    private static final String KEY_BATTERY_GRAPH = "battery_graph";
    private static final String KEY_BATTERY_APPS = "battery_apps";
    private static final String KEY_BATTERY_WIFI = "battery_wifi";
    private static final String KEY_BATTERY_CELL = "battery_cell";
    private static final String KEY_BATTERY_BLUETOOTH = "battery_bluetooth";
    private static final String KEY_BATTERY_IDLE = "battery_idle";
    private static final String KEY_BATTERY_SERVICE = "battery_service";
    private static final String KEY_BATTERY_SYSTEM = "battery_system";
    private static final String KEY_BATTERY_USER = "battery_user";

    private BatteryHistoryPreference mHistPref;
    @VisibleForTesting
    SparseArray<String> mUsageTypeMap;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHistPref = (BatteryHistoryPreference) findPreference(KEY_BATTERY_GRAPH);
        init();
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
        for (int i = 0, size = dataList.size(); i < size; i++) {
            final PowerUsageData data = dataList.get(i);
            final String key = mUsageTypeMap.get(data.usageType);
            if (key != null) {
                bindData(key, data);
            }
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
        } else if (uid == Process.SYSTEM_UID || uid == Process.ROOT_UID) {
            return UsageType.SYSTEM;
        } else {
            return UsageType.APP;
        }
    }

    @VisibleForTesting
    void init() {
        // Store projection from UsageType to preference key
        mUsageTypeMap = new SparseArray<>();
        mUsageTypeMap.put(UsageType.APP, KEY_BATTERY_APPS);
        mUsageTypeMap.put(UsageType.WIFI, KEY_BATTERY_WIFI);
        mUsageTypeMap.put(UsageType.CELL, KEY_BATTERY_CELL);
        mUsageTypeMap.put(UsageType.BLUETOOTH, KEY_BATTERY_BLUETOOTH);
        mUsageTypeMap.put(UsageType.IDLE, KEY_BATTERY_IDLE);
        mUsageTypeMap.put(UsageType.SERVICE, KEY_BATTERY_SERVICE);
        mUsageTypeMap.put(UsageType.USER, KEY_BATTERY_USER);
        mUsageTypeMap.put(UsageType.SYSTEM, KEY_BATTERY_SYSTEM);
    }

    @VisibleForTesting
    List<PowerUsageData> parsePowerUsageData(BatteryStatsHelper statusHelper) {
        final List<BatterySipper> batterySippers = statusHelper.getUsageList();
        final Map<Integer, PowerUsageData> batteryDataMap = new HashMap<>();

        for (int i = 0, size = mUsageTypeMap.size(); i < size; i++) {
            @UsageType final int type = mUsageTypeMap.keyAt(i);
            batteryDataMap.put(type, PowerUsageData.createBatteryUsageData(type));
        }

        // Accumulate power usage based on usage type
        for (final BatterySipper sipper : batterySippers) {
            final PowerUsageData usageData = batteryDataMap.get(extractUsageType(sipper));
            usageData.totalPowerMah += sipper.totalPowerMah;
        }

        // TODO(b/34385770): add logic to extract the summary
        final List<PowerUsageData> batteryDataList = new ArrayList<>(batteryDataMap.values());
        final double totalPower = statusHelper.getTotalPower();
        for (final PowerUsageData usageData : batteryDataList) {
            usageData.percentage = (usageData.totalPowerMah / totalPower) * 100;
        }

        return batteryDataList;
    }

    private void bindData(String key, PowerUsageData batteryData) {
        final PowerGaugePreference pref = (PowerGaugePreference) findPreference(key);

        pref.setSummary(batteryData.summary);
        pref.setPercent(batteryData.percentage);
    }

    /**
     * Class that contains data used in {@link PowerGaugePreference}.
     */
    @VisibleForTesting
    static class PowerUsageData {
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

        public String summary;
        public double percentage;
        public double totalPowerMah;
        @ColorInt
        public int iconColor;
        @UsageType
        public int usageType;

        private PowerUsageData(@UsageType int usageType) {
            this.usageType = usageType;
            totalPowerMah = 0;
        }

        public static PowerUsageData createBatteryUsageData(@UsageType int usageType) {
            // TODO(b/34385770): add color logic in this part
            return new PowerUsageData(usageType);
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
