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

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Intent;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowJobScheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SettingsRobolectricTestRunner.class)
public class AnomalyDetectionJobServiceTest {

    @Test
    public void testScheduleCleanUp() {
        AnomalyDetectionJobService.scheduleAnomalyDetection(application, new Intent());

        ShadowJobScheduler shadowJobScheduler =
            Shadows.shadowOf(application.getSystemService(JobScheduler.class));
        List<JobInfo> pendingJobs = shadowJobScheduler.getAllPendingJobs();
        assertThat(pendingJobs).hasSize(1);
        JobInfo pendingJob = pendingJobs.get(0);
        assertThat(pendingJob.getId()).isEqualTo(R.id.job_anomaly_detection);
        assertThat(pendingJob.getMaxExecutionDelayMillis())
            .isEqualTo(TimeUnit.MINUTES.toMillis(30));
    }
}
