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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.batteryusage.bugreport.BatteryUsageLogUtils;
import com.android.settingslib.fuelgauge.BatteryUtils;

/** Receives the periodic alarm {@link PendingIntent} callback. */
public final class PeriodicJobReceiver extends BroadcastReceiver {
    private static final String TAG = "PeriodicJobReceiver";
    public static final String ACTION_PERIODIC_JOB_UPDATE =
            "com.android.settings.battery.action.PERIODIC_JOB_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            loadDataAndRefreshJob(context, intent);
        } catch (Exception e) {
            BatteryUsageLogUtils.writeLog(
                    context,
                    Action.SCHEDULE_JOB,
                    String.format("loadDataAndRefreshJob() failed: %s", e));
        }
    }

    private static void loadDataAndRefreshJob(Context context, Intent intent) {
        final String action = intent == null ? "" : intent.getAction();
        if (!ACTION_PERIODIC_JOB_UPDATE.equals(action)) {
            Log.w(TAG, "receive unexpected action=" + action);
            return;
        }
        if (BatteryUtils.isWorkProfile(context)) {
            BatteryUsageLogUtils.writeLog(
                    context, Action.SCHEDULE_JOB, "do not refresh job for work profile");
            Log.w(TAG, "do not refresh job for work profile action=" + action);
            return;
        }
        BatteryUsageLogUtils.writeLog(context, Action.EXECUTE_JOB, "");
        BatteryUsageDataLoader.enqueueWork(context, /* isFullChargeStart= */ false);
        Log.d(TAG, "refresh periodic job from action=" + action);
        PeriodicJobManager.getInstance(context).refreshJob(/* fromBoot= */ false);
        DatabaseUtils.clearExpiredDataIfNeeded(context);
    }
}
