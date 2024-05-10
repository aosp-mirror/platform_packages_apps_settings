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
import android.os.Bundle;
import android.util.Log;

/** Receive the anomaly info from {@link StatsManager} */
public class AnomalyDetectionReceiver extends BroadcastReceiver {
    private static final String TAG = "SettingsAnomalyReceiver";

    public static final String KEY_ANOMALY_TIMESTAMP = "key_anomaly_timestamp";

    @Override
    public void onReceive(Context context, Intent intent) {
        final long configUid = intent.getLongExtra(StatsManager.EXTRA_STATS_CONFIG_UID, -1);
        final long configKey = intent.getLongExtra(StatsManager.EXTRA_STATS_CONFIG_KEY, -1);
        final long subscriptionId =
                intent.getLongExtra(StatsManager.EXTRA_STATS_SUBSCRIPTION_ID, -1);
        Log.i(
                TAG,
                "Anomaly intent received.  configUid = "
                        + configUid
                        + " configKey = "
                        + configKey
                        + " subscriptionId = "
                        + subscriptionId);

        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        bundle.putLong(KEY_ANOMALY_TIMESTAMP, System.currentTimeMillis());

        AnomalyDetectionJobService.scheduleAnomalyDetection(context, intent);
    }
}
