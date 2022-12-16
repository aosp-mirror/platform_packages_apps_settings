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

package com.android.settings.sim.receivers;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Phaser;

/** A JobService work on SIM slot change. */
public class SimSlotChangeService extends JobService {

    private static final String TAG = "SimSlotChangeService";

    /**
     * Schedules a service to work on SIM slot change.
     *
     * @param context is the caller context.
     */
    public static void scheduleSimSlotChange(Context context) {
        Context appContext = context.getApplicationContext();
        JobScheduler jobScheduler = appContext.getSystemService(JobScheduler.class);
        ComponentName component = new ComponentName(appContext, SimSlotChangeService.class);

        jobScheduler.schedule(
                new JobInfo.Builder(R.integer.sim_slot_changed, component).build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        final Phaser blocker = new Phaser(1);
        Handler handler = new Handler(thread.getLooper());
        handler.post(() -> {
            try {
                SimSlotChangeReceiver.runOnBackgroundThread(this);
            } catch (Throwable exception) {
                Log.e(TAG, "Exception running job", exception);
            }
            blocker.arrive();
        });
        blocker.awaitAdvance(0);
        thread.quit();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
