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

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.TimeUnit;

/** A JobService to clean up obsolete data in anomaly database */
public class AnomalyCleanupJobService extends JobService {
    private static final String TAG = "AnomalyCleanUpJobService";

    @VisibleForTesting static final long CLEAN_UP_FREQUENCY_MS = TimeUnit.DAYS.toMillis(1);

    public static void scheduleCleanUp(Context context) {
        final JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);

        final ComponentName component = new ComponentName(context, AnomalyCleanupJobService.class);
        final JobInfo.Builder jobBuilder =
                new JobInfo.Builder(R.integer.job_anomaly_clean_up, component)
                        .setPeriodic(CLEAN_UP_FREQUENCY_MS)
                        .setRequiresDeviceIdle(true)
                        .setRequiresCharging(true)
                        .setPersisted(true);
        final JobInfo pending = jobScheduler.getPendingJob(R.integer.job_anomaly_clean_up);

        // Don't schedule it if it already exists, to make sure it runs periodically even after
        // reboot
        if (pending == null
                && jobScheduler.schedule(jobBuilder.build()) != JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "Anomaly clean up job service schedule failed.");
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        final BatteryDatabaseManager batteryDatabaseManager =
                BatteryDatabaseManager.getInstance(this);
        final BatteryTipPolicy policy = new BatteryTipPolicy(this);
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    batteryDatabaseManager.deleteAllAnomaliesBeforeTimeStamp(
                            System.currentTimeMillis()
                                    - TimeUnit.DAYS.toMillis(policy.dataHistoryRetainDay));
                    jobFinished(params, false /* wantsReschedule */);
                });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
