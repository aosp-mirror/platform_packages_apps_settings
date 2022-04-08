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
import android.content.IntentFilter;
import android.os.BatteryStats;
import android.os.SystemClock;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.utils.AsyncLoaderCompat;
import com.android.settingslib.utils.PowerUtil;

import java.util.ArrayList;
import java.util.List;

public class DebugEstimatesLoader extends AsyncLoaderCompat<List<BatteryInfo>> {
    private BatteryStatsHelper mStatsHelper;

    public DebugEstimatesLoader(Context context, BatteryStatsHelper statsHelper) {
        super(context);
        mStatsHelper = statsHelper;
    }

    @Override
    protected void onDiscardResult(List<BatteryInfo> result) {

    }

    @Override
    public List<BatteryInfo> loadInBackground() {
        Context context = getContext();
        PowerUsageFeatureProvider powerUsageFeatureProvider =
                FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);

        // get stuff we'll need for both BatteryInfo
        final long elapsedRealtimeUs = PowerUtil.convertMsToUs(
                SystemClock.elapsedRealtime());
        Intent batteryBroadcast = getContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryStats stats = mStatsHelper.getStats();

        BatteryInfo oldinfo = BatteryInfo.getBatteryInfoOld(getContext(), batteryBroadcast,
                stats, elapsedRealtimeUs, false);

        Estimate estimate = powerUsageFeatureProvider.getEnhancedBatteryPrediction(context);
        if (estimate == null) {
            estimate = new Estimate(0, false, EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN);
        }
        BatteryInfo newInfo = BatteryInfo.getBatteryInfo(getContext(), batteryBroadcast, stats,
                estimate, elapsedRealtimeUs, false);

        List<BatteryInfo> infos = new ArrayList<>();
        infos.add(oldinfo);
        infos.add(newInfo);
        return infos;
    }
}
