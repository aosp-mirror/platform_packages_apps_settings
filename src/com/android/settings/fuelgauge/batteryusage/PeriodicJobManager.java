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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry.Action;
import com.android.settings.fuelgauge.batteryusage.bugreport.BatteryUsageLogUtils;
import com.android.settings.overlay.FeatureFactory;

import java.time.Duration;

/** Manages the periodic job to schedule or cancel the next job. */
public final class PeriodicJobManager {
    private static final String TAG = "PeriodicJobManager";
    private static final int ALARM_MANAGER_REQUEST_CODE = TAG.hashCode();

    private static PeriodicJobManager sSingleton;

    private final Context mContext;
    private final AlarmManager mAlarmManager;

    @VisibleForTesting static long sBroadcastDelayFromBoot = Duration.ofMinutes(40).toMillis();

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void reset() {
        sSingleton = null; // for testing only
    }

    /** Gets or creates the new {@link PeriodicJobManager} instance. */
    public static synchronized PeriodicJobManager getInstance(Context context) {
        if (sSingleton == null || sSingleton.mAlarmManager == null) {
            sSingleton = new PeriodicJobManager(context);
        }
        return sSingleton;
    }

    private PeriodicJobManager(Context context) {
        this.mContext = context.getApplicationContext();
        this.mAlarmManager = context.getSystemService(AlarmManager.class);
    }

    /** Schedules the next alarm job if it is available. */
    public void refreshJob(final boolean fromBoot) {
        if (mAlarmManager == null) {
            BatteryUsageLogUtils.writeLog(
                    mContext,
                    Action.SCHEDULE_JOB,
                    "cannot schedule next alarm job due to AlarmManager is null");
            Log.e(TAG, "cannot schedule next alarm job");
            return;
        }
        // Cancels the previous alert job and schedules the next one.
        final PendingIntent pendingIntent = getPendingIntent();
        cancelJob(pendingIntent);
        // Uses the timestamp of next full hour in local timezone.
        long currentTimeMillis = System.currentTimeMillis();
        final long triggerAtMillis = getTriggerAtMillis(currentTimeMillis, fromBoot);
        mAlarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);

        final String timeForLogging = ConvertUtils.utcToLocalTimeForLogging(triggerAtMillis);
        BatteryUsageLogUtils.writeLog(
                mContext,
                Action.SCHEDULE_JOB,
                String.format("triggerTime=%s, fromBoot=%b", timeForLogging, fromBoot));
        Log.d(TAG, "schedule next alarm job at " + timeForLogging);
    }

    private void cancelJob(PendingIntent pendingIntent) {
        if (mAlarmManager != null) {
            mAlarmManager.cancel(pendingIntent);
        } else {
            Log.e(TAG, "cannot cancel the alarm job");
        }
    }

    /** Gets the next alarm trigger time in milliseconds. */
    @VisibleForTesting
    static long getTriggerAtMillis(final long currentTimeMillis, final boolean fromBoot) {
        final boolean delayHourlyJobWhenBooting =
                FeatureFactory.getFeatureFactory()
                        .getPowerUsageFeatureProvider()
                        .delayHourlyJobWhenBooting();
        long targetTimeMillis = TimestampUtils.getNextHourTimestamp(currentTimeMillis);
        if (delayHourlyJobWhenBooting
                && fromBoot
                && (targetTimeMillis - currentTimeMillis) <= sBroadcastDelayFromBoot) {
            // Skips this time broadcast, schedule in the next alarm trigger.
            targetTimeMillis = TimestampUtils.getNextHourTimestamp(targetTimeMillis);
        }
        return targetTimeMillis;
    }

    private PendingIntent getPendingIntent() {
        final Intent broadcastIntent =
                new Intent(mContext, PeriodicJobReceiver.class)
                        .setAction(PeriodicJobReceiver.ACTION_PERIODIC_JOB_UPDATE);
        return PendingIntent.getBroadcast(
                mContext.getApplicationContext(),
                ALARM_MANAGER_REQUEST_CODE,
                broadcastIntent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
