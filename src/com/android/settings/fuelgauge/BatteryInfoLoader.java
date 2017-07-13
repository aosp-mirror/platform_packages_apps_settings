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
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.SystemClock;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AsyncLoader;

/**
 * Loader that can be used by classes to load BatteryInfo in a background thread. This loader will
 * automatically grab enhanced battery estimates if available or fall back to the system estimate
 * when not available.
 */
public class BatteryInfoLoader extends AsyncLoader<BatteryInfo>{

    BatteryStatsHelper mStatsHelper;
    private static final String LOG_TAG = "BatteryInfoLoader";

    public BatteryInfoLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        super(context);
        mStatsHelper = batteryStatsHelper;
    }

    @Override
    protected void onDiscardResult(BatteryInfo result) {

    }

    @Override
    public BatteryInfo loadInBackground() {
        final long startTime = System.currentTimeMillis();
        Context context = getContext();
        PowerUsageFeatureProvider powerUsageFeatureProvider =
                FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);

        // Stuff we always need to get BatteryInfo
        Intent batteryBroadcast = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final long elapsedRealtimeUs = BatteryUtils.convertMsToUs(SystemClock.elapsedRealtime());
        BatteryInfo batteryInfo;

        // 0 means we are discharging, anything else means charging
        boolean discharging = batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0;
        // Get enhanced prediction if available and discharging, otherwise use the old code
        Cursor cursor = null;
        if (discharging && powerUsageFeatureProvider != null &&
                powerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(context)) {
            final Uri queryUri = powerUsageFeatureProvider.getEnhancedBatteryPredictionUri();
            cursor = context.getContentResolver().query(queryUri, null, null, null, null);
        }
        BatteryStats stats = mStatsHelper.getStats();
        BatteryUtils.logRuntime(LOG_TAG, "BatteryInfoLoader post query", startTime);
        if (cursor != null && cursor.moveToFirst()) {
            long enhancedEstimate = powerUsageFeatureProvider.getTimeRemainingEstimate(cursor);
            batteryInfo = BatteryInfo.getBatteryInfo(context, batteryBroadcast, stats,
                    elapsedRealtimeUs, false /* shortString */,
                    BatteryUtils.convertMsToUs(enhancedEstimate), true /* basedOnUsage */);
        } else {
            batteryInfo = BatteryInfo.getBatteryInfo(context, batteryBroadcast, stats,
                    elapsedRealtimeUs, false /* shortString */,
                    discharging ? stats.computeBatteryTimeRemaining(elapsedRealtimeUs) : 0,
                    false /* basedOnUsage */);
        }
        BatteryUtils.logRuntime(LOG_TAG, "BatteryInfoLoader.loadInBackground", startTime);
        return batteryInfo;
    }
}
