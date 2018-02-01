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

import android.app.PendingIntent;
import android.app.StatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.StatsDimensionsValue;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.fuelgauge.BatteryUtils;

import java.util.List;

/**
 * Receive broadcast when {@link StatsManager} restart, then check the anomaly config and
 * prepare info for {@link StatsManager}
 */
public class AnomalyConfigReceiver extends BroadcastReceiver {
    private static final String TAG = "AnomalyConfigReceiver";
    private static final int REQUEST_CODE = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (StatsManager.ACTION_STATSD_STARTED.equals(intent.getAction())
                || Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final StatsManager statsManager = context.getSystemService(StatsManager.class);

            //TODO(b/72385333): Check whether to update the config
            final Intent extraIntent = new Intent();
            extraIntent.setClass(context, AnomalyDetectionReceiver.class);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE,
                    extraIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            uploadPendingIntent(statsManager, pendingIntent);
        }
    }

    @VisibleForTesting
    void uploadPendingIntent(StatsManager statsManager, PendingIntent pendingIntent) {
        Log.i(TAG, "Upload PendingIntent to StatsManager. configKey: "
                + StatsManagerConfig.ANOMALY_CONFIG_KEY + " subId: "
                + StatsManagerConfig.SUBSCRIBER_ID);
        statsManager.setBroadcastSubscriber(StatsManagerConfig.ANOMALY_CONFIG_KEY,
                StatsManagerConfig.SUBSCRIBER_ID, pendingIntent);
    }
}
