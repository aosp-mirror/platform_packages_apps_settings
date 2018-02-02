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

package com.android.settings.fuelgauge.batterytip;

import android.app.StatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.StatsDimensionsValue;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.settings.fuelgauge.BatteryUtils;

import java.util.List;

/**
 * Receive the anomaly info from {@link StatsManager}
 */
public class AnomalyDetectionReceiver extends BroadcastReceiver {
    private static final String TAG = "SettingsAnomalyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final BatteryDatabaseManager databaseManager = new BatteryDatabaseManager(context);
        final BatteryUtils batteryUtils = BatteryUtils.getInstance(context);
        final long configUid = intent.getLongExtra(StatsManager.EXTRA_STATS_CONFIG_UID, -1);
        final long configKey = intent.getLongExtra(StatsManager.EXTRA_STATS_CONFIG_KEY, -1);
        final long subscriptionId = intent.getLongExtra(StatsManager.EXTRA_STATS_SUBSCRIPTION_ID,
                -1);

        Log.i(TAG, "Anomaly intent received.  configUid = " + configUid + " configKey = "
                + configKey + " subscriptionId = " + subscriptionId);
        saveAnomalyToDatabase(databaseManager, batteryUtils, intent);
    }

    @VisibleForTesting
    void saveAnomalyToDatabase(BatteryDatabaseManager databaseManager, BatteryUtils batteryUtils
            , Intent intent) {
        // The Example of intentDimsValue is: 35:{1:{1:{1:10013|}|}|}
        StatsDimensionsValue intentDimsValue =
                intent.getParcelableExtra(StatsManager.EXTRA_STATS_DIMENSIONS_VALUE);
        Log.i(TAG, "Extra stats value: " + intentDimsValue.toString());
        List<StatsDimensionsValue> intentTuple = intentDimsValue.getTupleValueList();

        if (!intentTuple.isEmpty()) {
            try {
                // TODO(b/72385333): find more robust way to extract the uid.
                final StatsDimensionsValue intentTupleValue = intentTuple.get(0)
                        .getTupleValueList().get(0).getTupleValueList().get(0);
                final int uid = intentTupleValue.getIntValue();
                // TODD(b/72385333): extract anomaly type
                final int anomalyType = 0;
                final String packageName = batteryUtils.getPackageName(uid);
                final long timeMs = System.currentTimeMillis();
                databaseManager.insertAnomaly(packageName, anomalyType, timeMs);
            } catch (NullPointerException | IndexOutOfBoundsException e) {
                Log.e(TAG, "Parse stats dimensions value error.", e);
            }
        }
    }
}
