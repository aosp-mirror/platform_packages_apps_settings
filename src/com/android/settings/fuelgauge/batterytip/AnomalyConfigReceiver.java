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
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Receive broadcast when {@link StatsManager} restart, then check the anomaly config and
 * prepare info for {@link StatsManager}
 */
public class AnomalyConfigReceiver extends BroadcastReceiver {
    private static final String TAG = "AnomalyConfigReceiver";
    private static final int REQUEST_CODE = 0;
    private static final String PREF_DB = "anomaly_pref";
    private static final String KEY_ANOMALY_CONFIG_VERSION = "anomaly_config_version";
    private static final int DEFAULT_VERSION = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (StatsManager.ACTION_STATSD_STARTED.equals(intent.getAction())
                || Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final StatsManager statsManager = context.getSystemService(StatsManager.class);

            // Check whether to update the config
            checkAnomalyConfig(context, statsManager);

            // Upload PendingIntent to StatsManager
            final Intent extraIntent = new Intent(context, AnomalyDetectionReceiver.class);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE,
                    extraIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            uploadPendingIntent(statsManager, pendingIntent);

            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                AnomalyCleanupJobService.scheduleCleanUp(context);
            }
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

    private void checkAnomalyConfig(Context context, StatsManager statsManager) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_DB,
                Context.MODE_PRIVATE);
        final int currentVersion = sharedPreferences.getInt(KEY_ANOMALY_CONFIG_VERSION,
                DEFAULT_VERSION);
        final int newVersion = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.ANOMALY_CONFIG_VERSION, DEFAULT_VERSION);
        Log.i(TAG, "CurrentVersion: " + currentVersion + " new version: " + newVersion);

        if (newVersion > currentVersion) {
            final byte[] config = Base64.decode(
                    Settings.Global.getString(context.getContentResolver(),
                            Settings.Global.ANOMALY_CONFIG), Base64.DEFAULT);
            if (statsManager.addConfiguration(StatsManagerConfig.ANOMALY_CONFIG_KEY, config)) {
                Log.i(TAG, "Upload the anomaly config. configKey: "
                        + StatsManagerConfig.ANOMALY_CONFIG_KEY);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(KEY_ANOMALY_CONFIG_VERSION, newVersion);
                editor.apply();
            } else {
                Log.i(TAG, "Upload the anomaly config failed. configKey: "
                        + StatsManagerConfig.ANOMALY_CONFIG_KEY);
            }
        }
    }
}
