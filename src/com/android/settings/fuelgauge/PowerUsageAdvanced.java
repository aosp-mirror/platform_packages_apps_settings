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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.fuelgauge.PowerUsageAdvanced.PowerUsageData.UsageType;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerUsageAdvanced extends PowerUsageBase {
    private static final String TAG = "AdvancedBatteryUsage";
    private static final String KEY_BATTERY_GRAPH = "battery_graph";
    private static final String KEY_BATTERY_USAGE_LIST = "battery_usage_list";
    private static final int STATUS_TYPE = BatteryStats.STATS_SINCE_CHARGED;

    @VisibleForTesting
    final int[] mUsageTypes = {
            UsageType.WIFI,
            UsageType.CELL,
            UsageType.SYSTEM,
            UsageType.BLUETOOTH,
            UsageType.USER,
            UsageType.IDLE,
            UsageType.APP,
            UsageType.UNACCOUNTED,
            UsageType.OVERCOUNTED};

    @VisibleForTesting BatteryHistoryPreference mHistPref;
    @VisibleForTesting PreferenceGroup mUsageListGroup;
    private BatteryUtils mBatteryUtils;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private PackageManager mPackageManager;
    private UserManager mUserManager;
    private Map<Integer, PowerUsageData> mBatteryDataMap;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    final int dischargeAmount = mStatsHelper.getStats().getDischargeAmount(
                            STATUS_TYPE);
                    final double totalPower = mStatsHelper.getTotalPower();
                    final BatteryEntry entry = (BatteryEntry) msg.obj;
                    final int usageType = extractUsageType(entry.sipper);

                    PowerUsageData usageData = mBatteryDataMap.get(usageType);
                    Preference pref = findPreference(String.valueOf(usageType));
                    if (pref != null && usageData != null) {
                        updateUsageDataSummary(usageData, totalPower, dischargeAmount);
                        pref.setSummary(usageData.summary);
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHistPref = (BatteryHistoryPreference) findPreference(KEY_BATTERY_GRAPH);
        mUsageListGroup = (PreferenceGroup) findPreference(KEY_BATTERY_USAGE_LIST);

        final Context context = getContext();
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
        mPackageManager = context.getPackageManager();
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mBatteryUtils = BatteryUtils.getInstance(context);

        // init the summary so other preferences won't have unnecessary move
        updateHistPrefSummary(context);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
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
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return null;
    }

    @Override
    protected void refreshUi() {
        final long startTime = System.currentTimeMillis();
        final Context context = getContext();
        if (context == null) {
            return;
        }
        updatePreference(mHistPref);
        refreshPowerUsageDataList(mStatsHelper, mUsageListGroup);
        updateHistPrefSummary(context);

        BatteryEntry.startRequestQueue();
        BatteryUtils.logRuntime(TAG, "refreshUI", startTime);
    }

    private void updateHistPrefSummary(Context context) {
        Intent batteryIntent =
                context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final boolean plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;

        if (mPowerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(context) && !plugged) {
            mHistPref.setBottomSummary(
                    mPowerUsageFeatureProvider.getAdvancedUsageScreenInfoString());
        } else {
            mHistPref.hideBottomSummary();
        }
    }

    @VisibleForTesting
    void refreshPowerUsageDataList(BatteryStatsHelper statsHelper,
            PreferenceGroup preferenceGroup) {
        List<PowerUsageData> dataList = parsePowerUsageData(statsHelper);
        preferenceGroup.removeAll();
        for (int i = 0, size = dataList.size(); i < size; i++) {
            final PowerUsageData batteryData = dataList.get(i);
            if (shouldHideCategory(batteryData)) {
                continue;
            }
            final PowerGaugePreference pref = new PowerGaugePreference(getPrefContext());

            pref.setKey(String.valueOf(batteryData.usageType));
            pref.setTitle(batteryData.titleResId);
            pref.setSummary(batteryData.summary);
            pref.setPercent(batteryData.percentage);
            pref.setSelectable(false);
            preferenceGroup.addPreference(pref);
        }
    }

    @VisibleForTesting
    @UsageType
    int extractUsageType(BatterySipper sipper) {
        final DrainType drainType = sipper.drainType;
        final int uid = sipper.getUid();

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
        } else if (drainType == DrainType.UNACCOUNTED) {
            return UsageType.UNACCOUNTED;
        } else if (drainType == DrainType.OVERCOUNTED) {
            return UsageType.OVERCOUNTED;
        } else if (mPowerUsageFeatureProvider.isTypeSystem(sipper)
                || mPowerUsageFeatureProvider.isTypeService(sipper)) {
            return UsageType.SYSTEM;
        } else {
            return UsageType.APP;
        }
    }

    @VisibleForTesting
    boolean shouldHideCategory(PowerUsageData powerUsageData) {
        return powerUsageData.usageType == UsageType.UNACCOUNTED
                || powerUsageData.usageType == UsageType.OVERCOUNTED
                || (powerUsageData.usageType == UsageType.USER && mUserManager.getUserCount() == 1)
                || (powerUsageData.usageType == UsageType.CELL
                && !DataUsageUtils.hasMobileData(getContext()));
    }

    @VisibleForTesting
    boolean shouldShowBatterySipper(BatterySipper batterySipper) {
        return batterySipper.drainType != DrainType.SCREEN;
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
            if (sipper.drainType == DrainType.APP && sipper.usageTimeMs != 0) {
                sipper.usageTimeMs = mBatteryUtils.getProcessTimeMs(
                        BatteryUtils.StatusType.FOREGROUND, sipper.uidObj, STATUS_TYPE);
            }
            usageData.totalUsageTimeMs += sipper.usageTimeMs;
            if (shouldShowBatterySipper(sipper)) {
                usageData.usageList.add(sipper);
            }
        }

        final List<PowerUsageData> batteryDataList = new ArrayList<>(batteryDataMap.values());
        final int dischargeAmount = statusHelper.getStats().getDischargeAmount(STATUS_TYPE);
        final double totalPower = statusHelper.getTotalPower();
        final double hiddenPower = calculateHiddenPower(batteryDataList);
        for (final PowerUsageData usageData : batteryDataList) {
            usageData.percentage = mBatteryUtils.calculateBatteryPercent(usageData.totalPowerMah,
                    totalPower, hiddenPower, dischargeAmount);
            updateUsageDataSummary(usageData, totalPower, dischargeAmount);
        }

        Collections.sort(batteryDataList);

        mBatteryDataMap = batteryDataMap;
        return batteryDataList;
    }

    @VisibleForTesting
    double calculateHiddenPower(List<PowerUsageData> batteryDataList) {
        for (final PowerUsageData usageData : batteryDataList) {
            if (usageData.usageType == UsageType.UNACCOUNTED) {
                return usageData.totalPowerMah;
            }
        }

        return 0;
    }

    @VisibleForTesting
    void updateUsageDataSummary(PowerUsageData usageData, double totalPower, int dischargeAmount) {
        if (shouldHideSummary(usageData)) {
            return;
        }
        if (usageData.usageList.size() <= 1) {
            CharSequence timeSequence = Utils.formatElapsedTime(getContext(),
                    usageData.totalUsageTimeMs, false);
            usageData.summary = usageData.usageType == UsageType.IDLE ? timeSequence
                    : TextUtils.expandTemplate(getText(R.string.battery_used_for), timeSequence);
        } else {
            BatterySipper sipper = findBatterySipperWithMaxBatteryUsage(usageData.usageList);
            BatteryEntry batteryEntry = new BatteryEntry(getContext(), mHandler, mUserManager,
                    sipper);
            final double percentage = (sipper.totalPowerMah / totalPower) * dischargeAmount;
            usageData.summary = getString(R.string.battery_used_by,
                    Utils.formatPercentage(percentage, true), batteryEntry.name);
        }
    }

    @VisibleForTesting
    boolean shouldHideSummary(PowerUsageData powerUsageData) {
        @UsageType final int usageType = powerUsageData.usageType;

        return usageType == UsageType.CELL
                || usageType == UsageType.BLUETOOTH
                || usageType == UsageType.WIFI
                || usageType == UsageType.APP
                || usageType == UsageType.SYSTEM;
    }

    @VisibleForTesting
    BatterySipper findBatterySipperWithMaxBatteryUsage(List<BatterySipper> usageList) {
        BatterySipper sipper = usageList.get(0);
        for (int i = 1, size = usageList.size(); i < size; i++) {
            final BatterySipper comparedSipper = usageList.get(i);
            if (comparedSipper.totalPowerMah > sipper.totalPowerMah) {
                sipper = comparedSipper;
            }
        }

        return sipper;
    }

    @VisibleForTesting
    void setPackageManager(PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    @VisibleForTesting
    void setPowerUsageFeatureProvider(PowerUsageFeatureProvider provider) {
        mPowerUsageFeatureProvider = provider;
    }
    @VisibleForTesting
    void setUserManager(UserManager userManager) {
        mUserManager = userManager;
    }
    @VisibleForTesting
    void setBatteryUtils(BatteryUtils batteryUtils) {
        mBatteryUtils = batteryUtils;
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
                UsageType.SYSTEM,
                UsageType.BLUETOOTH,
                UsageType.USER,
                UsageType.IDLE,
                UsageType.UNACCOUNTED,
                UsageType.OVERCOUNTED})
        public @interface UsageType {
            int APP = 0;
            int WIFI = 1;
            int CELL = 2;
            int SYSTEM = 3;
            int BLUETOOTH = 4;
            int USER = 5;
            int IDLE = 6;
            int UNACCOUNTED = 7;
            int OVERCOUNTED = 8;
        }

        @StringRes
        public int titleResId;
        public CharSequence summary;
        public double percentage;
        public double totalPowerMah;
        public long totalUsageTimeMs;
        @ColorInt
        public int iconColor;
        @UsageType
        public int usageType;
        public List<BatterySipper> usageList;

        public PowerUsageData(@UsageType int usageType) {
            this(usageType, 0);
        }

        public PowerUsageData(@UsageType int usageType, double totalPower) {
            this.usageType = usageType;
            totalPowerMah = 0;
            totalUsageTimeMs = 0;
            titleResId = getTitleResId(usageType);
            totalPowerMah = totalPower;
            usageList = new ArrayList<>();
        }

        private int getTitleResId(@UsageType int usageType) {
            switch (usageType) {
                case UsageType.WIFI:
                    return R.string.power_wifi;
                case UsageType.CELL:
                    return R.string.power_cell;
                case UsageType.SYSTEM:
                    return R.string.power_system;
                case UsageType.BLUETOOTH:
                    return R.string.power_bluetooth;
                case UsageType.USER:
                    return R.string.power_user;
                case UsageType.IDLE:
                    return R.string.power_idle;
                case UsageType.UNACCOUNTED:
                    return R.string.power_unaccounted;
                case UsageType.OVERCOUNTED:
                    return R.string.power_overcounted;
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
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_advanced;
                    return Arrays.asList(sir);
                }
            };

}
