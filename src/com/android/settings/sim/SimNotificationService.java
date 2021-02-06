/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.sim;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Log;

import com.android.settings.R;

/** A JobService sends SIM notifications. */
public class SimNotificationService extends JobService {

    private static final String TAG = "SimNotificationService";
    private static final String EXTRA_NOTIFICATION_TYPE = "notification_type";

    /**
     * Schedules a service to send SIM push notifications.
     *
     * @param context
     * @param notificationType indicates which SIM notification to send.
     */
    public static void scheduleSimNotification(
            Context context, @SimActivationNotifier.NotificationType int notificationType) {
        final JobScheduler jobScheduler =
                context.getApplicationContext().getSystemService(JobScheduler.class);
        final ComponentName component =
                new ComponentName(context.getApplicationContext(), SimNotificationService.class);
        PersistableBundle extra = new PersistableBundle();
        extra.putInt(EXTRA_NOTIFICATION_TYPE, notificationType);

        jobScheduler.schedule(
                new JobInfo.Builder(R.integer.sim_notification_send, component)
                        .setExtras(extra)
                        .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle extra = params.getExtras();
        if (extra == null) {
            Log.e(TAG, "Failed to get notification type.");
            return false;
        }
        int notificationType = extra.getInt(EXTRA_NOTIFICATION_TYPE);
        switch (notificationType) {
            case SimActivationNotifier.NotificationType.NETWORK_CONFIG:
                Log.i(TAG, "Sending SIM config notification.");
                SimActivationNotifier.setShowSimSettingsNotification(this, false);
                new SimActivationNotifier(this).sendNetworkConfigNotification();
                break;
            case SimActivationNotifier.NotificationType.SWITCH_TO_REMOVABLE_SLOT:
                new SimActivationNotifier(this).sendSwitchedToRemovableSlotNotification();
                break;
            case SimActivationNotifier.NotificationType.ENABLE_DSDS:
                new SimActivationNotifier(this).sendEnableDsdsNotification();
                break;
            default:
                Log.e(TAG, "Invalid notification type: " + notificationType);
                break;
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
