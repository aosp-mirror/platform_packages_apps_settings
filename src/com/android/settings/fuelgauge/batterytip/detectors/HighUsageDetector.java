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
import android.os.BatteryUsageStats;
import android.os.UidBatteryConsumer;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.HighUsageDataParser;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.HighUsageTip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Detector whether to show summary tip. This detector should be executed as the last {@link
 * BatteryTipDetector} since it need the most up-to-date {@code visibleTips}
 */
public class HighUsageDetector implements BatteryTipDetector {
    private static final String TAG = "HighUsageDetector";

    private BatteryTipPolicy mPolicy;
    private BatteryUsageStats mBatteryUsageStats;
    private final BatteryInfo mBatteryInfo;
    private List<AppInfo> mHighUsageAppList;
    @VisibleForTesting HighUsageDataParser mDataParser;
    @VisibleForTesting BatteryUtils mBatteryUtils;
    @VisibleForTesting boolean mDischarging;

    public HighUsageDetector(
            Context context,
            BatteryTipPolicy policy,
            BatteryUsageStats batteryUsageStats,
            BatteryInfo batteryInfo) {
        mPolicy = policy;
        mBatteryUsageStats = batteryUsageStats;
        mBatteryInfo = batteryInfo;
        mHighUsageAppList = new ArrayList<>();
        mBatteryUtils = BatteryUtils.getInstance(context);
        mDataParser =
                new HighUsageDataParser(
                        mPolicy.highUsagePeriodMs, mPolicy.highUsageBatteryDraining);
        mDischarging = batteryInfo.discharging;
    }

    @Override
    public BatteryTip detect() {
        final long lastFullChargeTimeMs =
                mBatteryUtils.calculateLastFullChargeTime(
                        mBatteryUsageStats, System.currentTimeMillis());
        if (mPolicy.highUsageEnabled && mDischarging) {
            parseBatteryData();
            if (mDataParser.isDeviceHeavilyUsed() || mPolicy.testHighUsageTip) {
                final double totalPower = mBatteryUsageStats.getConsumedPower();
                final int dischargeAmount = mBatteryUsageStats.getDischargePercentage();
                final List<UidBatteryConsumer> uidBatteryConsumers =
                        mBatteryUsageStats.getUidBatteryConsumers();
                // Sort by descending power
                uidBatteryConsumers.sort(
                        (consumer1, consumer2) ->
                                Double.compare(
                                        consumer2.getConsumedPower(),
                                        consumer1.getConsumedPower()));
                for (UidBatteryConsumer consumer : uidBatteryConsumers) {
                    final double percent =
                            mBatteryUtils.calculateBatteryPercent(
                                    consumer.getConsumedPower(), totalPower, dischargeAmount);
                    if ((percent + 0.5f < 1f)
                            || mBatteryUtils.shouldHideUidBatteryConsumer(consumer)) {
                        // Don't show it if we should hide or usage percentage is lower than 1%
                        continue;
                    }

                    mHighUsageAppList.add(
                            new AppInfo.Builder()
                                    .setUid(consumer.getUid())
                                    .setPackageName(mBatteryUtils.getPackageName(consumer.getUid()))
                                    .build());
                    if (mHighUsageAppList.size() >= mPolicy.highUsageAppCount) {
                        break;
                    }
                }

                // When in test mode, add an app if necessary
                if (mPolicy.testHighUsageTip && mHighUsageAppList.isEmpty()) {
                    mHighUsageAppList.add(
                            new AppInfo.Builder()
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
        try {
            mBatteryInfo.parseBatteryHistory(mDataParser);
        } catch (IllegalStateException e) {
            Log.e(TAG, "parseBatteryData() failed", e);
        }
    }
}
