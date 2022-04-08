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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.content.Context;
import android.os.BatteryStats;

import androidx.annotation.VisibleForTesting;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
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
                final BatteryStats batteryStats = mBatteryStatsHelper.getStats();
                final List<BatterySipper> batterySippers
                        = new ArrayList<>(mBatteryStatsHelper.getUsageList());
                final double totalPower = mBatteryStatsHelper.getTotalPower();
                final int dischargeAmount = batteryStats != null
                        ? batteryStats.getDischargeAmount(BatteryStats.STATS_SINCE_CHARGED)
                        : 0;

                Collections.sort(batterySippers,
                        (sipper1, sipper2) -> Double.compare(sipper2.totalSmearedPowerMah,
                                sipper1.totalSmearedPowerMah));
                for (BatterySipper batterySipper : batterySippers) {
                    final double percent = mBatteryUtils.calculateBatteryPercent(
                            batterySipper.totalSmearedPowerMah, totalPower, 0, dischargeAmount);
                    if ((percent + 0.5f < 1f) || mBatteryUtils.shouldHideSipper(batterySipper)) {
                        // Don't show it if we should hide or usage percentage is lower than 1%
                        continue;
                    }
                    mHighUsageAppList.add(new AppInfo.Builder()
                            .setUid(batterySipper.getUid())
                            .setPackageName(
                                    mBatteryUtils.getPackageName(batterySipper.getUid()))
                            .build());
                    if (mHighUsageAppList.size() >= mPolicy.highUsageAppCount) {
                        break;
                    }

                }

                // When in test mode, add an app if necessary
                if (mPolicy.testHighUsageTip && mHighUsageAppList.isEmpty()) {
                    mHighUsageAppList.add(new AppInfo.Builder()
                            .setPackageName(SETTINGS_PACKAGE_NAME)
                            .setScreenOnTimeMs(TimeUnit.HOURS.toMillis(3))
                            .build());
                }
            }
        }

        return new HighUsageTip(lastFullChargeTimeMs, mHighUsageAppList);
    }

    @VisibleForTesting
    void parseBatteryData() {
        BatteryInfo.parse(mBatteryStatsHelper.getStats(), mDataParser);
    }
}
