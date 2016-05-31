/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deletionhelper;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

/**
 * A {@link BroadcastReceiver} listening for {@link Intent#ACTION_BOOT_COMPLETED} broadcasts to
 * schedule an automatic storage management job. Automatic storage management jobs are only
 * scheduled once a day for a plugged in device.
 */
public class AutomaticStorageBroadcastReceiver extends BroadcastReceiver {
    private static final int AUTOMATIC_STORAGE_JOB_ID = 0;
    private static final long PERIOD = DateUtils.DAY_IN_MILLIS;

    @Override
    public void onReceive(Context context, Intent intent) {
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName component = new ComponentName(context,
                AutomaticStorageManagementJobService.class);
        JobInfo job = new JobInfo.Builder(AUTOMATIC_STORAGE_JOB_ID, component)
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setPeriodic(PERIOD)
                .build();
        jobScheduler.schedule(job);
    }
}
