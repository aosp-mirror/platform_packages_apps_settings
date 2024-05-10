/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.util.Log;

import com.android.settingslib.utils.AsyncLoaderCompat;

/** Loader to get new {@link BatteryUsageStats} in the background */
public class BatteryUsageStatsLoader extends AsyncLoaderCompat<BatteryUsageStats> {
    private static final String TAG = "BatteryUsageStatsLoader";
    private final BatteryStatsManager mBatteryStatsManager;
    private final boolean mIncludeBatteryHistory;

    public BatteryUsageStatsLoader(Context context, boolean includeBatteryHistory) {
        super(context);
        mBatteryStatsManager = context.getSystemService(BatteryStatsManager.class);
        mIncludeBatteryHistory = includeBatteryHistory;
    }

    @Override
    public BatteryUsageStats loadInBackground() {
        final BatteryUsageStatsQuery.Builder builder = new BatteryUsageStatsQuery.Builder();
        if (mIncludeBatteryHistory) {
            builder.includeBatteryHistory();
        }
        try {
            return mBatteryStatsManager.getBatteryUsageStats(
                    builder.includeProcessStateData().build());
        } catch (RuntimeException e) {
            Log.e(TAG, "loadInBackground() for getBatteryUsageStats()", e);
            // Use default BatteryUsageStats.
            return new BatteryUsageStats.Builder(new String[0]).build();
        }
    }

    @Override
    protected void onDiscardResult(BatteryUsageStats result) {}
}
