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

package com.android.settings.fuelgauge.batterytip;

import android.content.Context;
import android.os.BatteryUsageStats;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.detectors.BatteryDefenderDetector;
import com.android.settings.fuelgauge.batterytip.detectors.DockDefenderDetector;
import com.android.settings.fuelgauge.batterytip.detectors.EarlyWarningDetector;
import com.android.settings.fuelgauge.batterytip.detectors.HighUsageDetector;
import com.android.settings.fuelgauge.batterytip.detectors.LowBatteryDetector;
import com.android.settings.fuelgauge.batterytip.detectors.SmartBatteryDetector;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SummaryTip;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.utils.AsyncLoaderCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loader to compute and return a battery tip list. It will always return a full length list even
 * though some tips may have state {@code BaseBatteryTip.StateType.INVISIBLE}.
 */
public class BatteryTipLoader extends AsyncLoaderCompat<List<BatteryTip>> {
    private static final String TAG = "BatteryTipLoader";

    private static final boolean USE_FAKE_DATA = false;

    private BatteryUsageStats mBatteryUsageStats;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    public BatteryTipLoader(Context context, BatteryUsageStats batteryUsageStats) {
        super(context);
        mBatteryUsageStats = batteryUsageStats;
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public List<BatteryTip> loadInBackground() {
        if (USE_FAKE_DATA) {
            return getFakeData();
        }
        final List<BatteryTip> tips = new ArrayList<>();
        final BatteryTipPolicy policy = new BatteryTipPolicy(getContext());
        final BatteryInfo batteryInfo = mBatteryUtils.getBatteryInfo(TAG);
        final Context context = getContext();

        tips.add(new LowBatteryDetector(context, policy, batteryInfo).detect());
        tips.add(new HighUsageDetector(context, policy, mBatteryUsageStats, batteryInfo).detect());
        tips.add(new SmartBatteryDetector(
                context, policy, batteryInfo, context.getContentResolver()).detect());
        tips.add(new EarlyWarningDetector(policy, context).detect());
        tips.add(new BatteryDefenderDetector(
                batteryInfo, context.getApplicationContext()).detect());
        tips.add(new DockDefenderDetector(batteryInfo, context.getApplicationContext()).detect());
        Collections.sort(tips);
        return tips;
    }

    @Override
    protected void onDiscardResult(List<BatteryTip> result) {
    }

    private List<BatteryTip> getFakeData() {
        final List<BatteryTip> tips = new ArrayList<>();
        tips.add(new SummaryTip(BatteryTip.StateType.NEW,
                EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN));
        tips.add(new LowBatteryTip(BatteryTip.StateType.NEW, false /* powerSaveModeOn */));

        return tips;
    }

}
