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
import android.support.annotation.VisibleForTesting;
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

/**
 * Detector whether to show summary tip. This detector should be executed as the last
 * {@link BatteryTipDetector} since it need the most up-to-date {@code visibleTips}
 */
public class HighUsageDetector implements BatteryTipDetector {
    private BatteryTipPolicy mPolicy;
    private BatteryStatsHelper mBatteryStatsHelper;
    private List<AppInfo> mHighUsageAppList;
    private Context mContext;
    @VisibleForTesting
    HighUsageDataParser mDataParser;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    public HighUsageDetector(Context context, BatteryTipPolicy policy,
            BatteryStatsHelper batteryStatsHelper) {
        mContext = context;
        mPolicy = policy;
        mBatteryStatsHelper = batteryStatsHelper;
        mHighUsageAppList = new ArrayList<>();
        mBatteryUtils = BatteryUtils.getInstance(context);
        mDataParser = new HighUsageDataParser(mPolicy.highUsagePeriodMs,
                mPolicy.highUsageBatteryDraining);
    }

    @Override
    public BatteryTip detect() {
        final long screenUsageTimeMs = mBatteryUtils.calculateScreenUsageTime(mBatteryStatsHelper);
        if (mPolicy.highUsageEnabled) {
            parseBatteryData();
            if (mDataParser.isDeviceHeavilyUsed()) {
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

                Collections.sort(mHighUsageAppList, Collections.reverseOrder());
                mHighUsageAppList = mHighUsageAppList.subList(0,
                        Math.min(mPolicy.highUsageAppCount, mHighUsageAppList.size()));
            }
        }

        return new HighUsageTip(screenUsageTimeMs, mHighUsageAppList);
    }

    @VisibleForTesting
    void parseBatteryData() {
        BatteryInfo.parse(mBatteryStatsHelper.getStats(), mDataParser);
    }
}
