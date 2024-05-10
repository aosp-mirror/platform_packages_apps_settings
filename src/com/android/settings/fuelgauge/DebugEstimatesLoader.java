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
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.SystemClock;
import android.util.Log;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.utils.AsyncLoaderCompat;
import com.android.settingslib.utils.PowerUtil;

import java.util.ArrayList;
import java.util.List;

public class DebugEstimatesLoader extends AsyncLoaderCompat<List<BatteryInfo>> {
    private static final String TAG = "DebugEstimatesLoader";

    public DebugEstimatesLoader(Context context) {
        super(context);
    }

    @Override
    protected void onDiscardResult(List<BatteryInfo> result) {}

    @Override
    public List<BatteryInfo> loadInBackground() {
        Context context = getContext();
        PowerUsageFeatureProvider powerUsageFeatureProvider =
                FeatureFactory.getFeatureFactory().getPowerUsageFeatureProvider();

        // get stuff we'll need for both BatteryInfo
        final long elapsedRealtimeUs = PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());
        Intent batteryBroadcast =
                getContext()
                        .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryUsageStats batteryUsageStats;
        try {
            batteryUsageStats =
                    context.getSystemService(BatteryStatsManager.class).getBatteryUsageStats();
        } catch (RuntimeException e) {
            Log.e(TAG, "getBatteryInfo() from getBatteryUsageStats()", e);
            // Use default BatteryUsageStats.
            batteryUsageStats = new BatteryUsageStats.Builder(new String[0]).build();
        }
        BatteryInfo oldinfo =
                BatteryInfo.getBatteryInfoOld(
                        getContext(),
                        batteryBroadcast,
                        batteryUsageStats,
                        elapsedRealtimeUs,
                        false);

        Estimate estimate = powerUsageFeatureProvider.getEnhancedBatteryPrediction(context);
        if (estimate == null) {
            estimate = new Estimate(0, false, EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN);
        }
        BatteryInfo newInfo =
                BatteryInfo.getBatteryInfo(
                        getContext(),
                        batteryBroadcast,
                        batteryUsageStats,
                        estimate,
                        elapsedRealtimeUs,
                        false);

        List<BatteryInfo> infos = new ArrayList<>();
        infos.add(oldinfo);
        infos.add(newInfo);

        try {
            batteryUsageStats.close();
        } catch (Exception e) {
            Log.e(TAG, "BatteryUsageStats.close() failed", e);
        }
        return infos;
    }
}
