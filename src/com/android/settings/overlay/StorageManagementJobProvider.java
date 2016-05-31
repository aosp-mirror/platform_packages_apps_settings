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

package com.android.settings.overlay;

import android.app.job.JobParameters;
import android.content.Context;

/**
 * Feature provider for automatic storage management jobs.
 */
public interface StorageManagementJobProvider {
    /**
     * Starts an asynchronous deletion job to clear out storage older than
     * @param params Standard JobService parameters.
     * @param daysToRetain Number of days of information to retain on the device.
     * @return If the job needs to process the work on a separate thread.
     */
    boolean onStartJob(Context context, JobParameters params, int daysToRetain);

    /**
     * Attempt to stop the execution of the job.
     * @param params Parameters specifying info about this job.
     * @return If the job should be rescheduled.
     */
    boolean onStopJob(Context context, JobParameters params);
}
