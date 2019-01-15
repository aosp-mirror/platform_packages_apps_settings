/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterytip.detectors;

import android.content.Context;
import android.os.BatteryStats;
import androidx.annotation.VisibleForTesting;
import android.text.format.DateUtils;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.batterytip.HighUsageDataParser;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Detector whether to show summary tip. This detector should be executed as the last
 * {@link BatteryTipDetector} since it need the most up-to-date {@code visibleTips}
 */
public class HighUsageDetector implements BatteryTipDetector {
    private BatteryTipPolicy mPolicy;
    private BatteryStatsHelper mBatteryStatsHelper;
    private List<AppInfo> mHighUsageAppList;
    @VisibleForTesting
    HighUsageDataParser mDataParser;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    boolean mDischarging;

    public HighUsageDetector(Context context, BatteryTipPolicy policy,
            BatteryStatsHelper batteryStatsHelper, boolean discharging) {
        mPolicy = policy;
        mBatteryStatsHelper = batteryStatsHelper;
        mHighUsageAppList = new ArrayList<>();
        mBatteryUtils = BatteryUtils.getInstance(context);
        mDataParser = new HighUsageDataParser(mPolicy.highUsagePeriodMs,
                mPolicy.highUsageBatteryDraining);
        mDischarging = discharging;
    }

    @Override
    public BatteryTip detect() {
        final long lastFullChargeTimeMs = mBatteryUtils.calculateLastFullChargeTime(
                mBatteryStatsHelper, System.currentTimeMillis());
        if (mPolicy.highUsageEnabled && mDischarging) {
            parseBatteryData();
            if (mDataParser.isDeviceHeavilyUsed() || mPolicy.testHighUsageTip) {
                final List<BatterySipper> batterySippers = mBatteryStatsHelper.getUsageList();
                for (int i = 0, size = batterySippers.size(); i < size; i++) {
                    final BatterySipper batterySipper = batterySippers.get(i);
                    if (!mBatteryUtils.shouldHideSipper(batterySipper)) {
                        final long foregroundTimeMs = mBatteryUtils.getProcessTimeMs(
                                BatteryUtils.StatusType.FOREGROUND, batterySipper.uidObj,
                                BatteryStats.STATS_SINCE_CHARGED);
                        if (foregroundTimeMs >= DateUtils.MINUTE_IN_MILLIS) {
                            mHighUsageAppList.add(new AppInfo.Builder()
                                    .setUid(batterySipper.getUid())
                                    .setPackageName(
                                            mBatteryUtils.getPackageName(batterySipper.getUid()))
                                    .setScreenOnTimeMs(foregroundTimeMs)
                                    .build());
                        }
                    }
                }

                // When in test mode, add an app if necessary
                if (mPolicy.testHighUsageTip && mHighUsageAppList.isEmpty()) {
                    mHighUsageAppList.add(new AppInfo.Builder()
                            .setPackageName("com.android.settings")
                            .setScreenOnTimeMs(TimeUnit.HOURS.toMillis(3))
                            .build());
                }

                Collections.sort(mHighUsageAppList, Collections.reverseOrder());
                mHighUsageAppList = mHighUsageAppList.subList(0,
                        Math.min(mPolicy.highUsageAppCount, mHighUsageAppList.size()));
            }
        }

        return new HighUsageTip(lastFullChargeTimeMs, mHighUsageAppList);
    }

    @VisibleForTesting
    void parseBatteryData() {
        BatteryInfo.parse(mBatteryStatsHelper.getStats(), mDataParser);
    }
}
