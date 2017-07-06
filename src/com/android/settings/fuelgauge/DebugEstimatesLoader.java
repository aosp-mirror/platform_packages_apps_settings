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
import com.android.settings.utils.AsyncLoader;
import java.util.ArrayList;
import java.util.List;

public class DebugEstimatesLoader extends AsyncLoader<List<BatteryInfo>> {
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
        final long elapsedRealtimeUs = BatteryUtils.convertMsToUs(SystemClock.elapsedRealtime());
        Intent batteryBroadcast = getContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryStats stats = mStatsHelper.getStats();

        BatteryInfo oldinfo = BatteryInfo.getBatteryInfoOld(getContext(), batteryBroadcast,
                stats, elapsedRealtimeUs, false);

        final long timeRemainingEnhanced = BatteryUtils.convertMsToUs(
                powerUsageFeatureProvider.getEnhancedBatteryPrediction(getContext()));
        BatteryInfo newinfo = BatteryInfo.getBatteryInfo(getContext(), batteryBroadcast, stats,
                elapsedRealtimeUs, false, timeRemainingEnhanced, true);

        List<BatteryInfo> infos = new ArrayList<>();
        infos.add(oldinfo);
        infos.add(newinfo);
        return infos;
    }
}
