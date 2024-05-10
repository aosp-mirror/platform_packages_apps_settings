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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.JobSchedulerImpl;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Binder;

import com.android.settings.R;
import com.android.settings.testutils.DatabaseTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class AnomalyCleanupJobServiceTest {
    private static final int UID = 1234;
    private static final String PACKAGE_NAME = "com.android.package";
    private static final String PACKAGE_NAME_OLD = "com.android.package.old";
    private static final int ANOMALY_TYPE = 1;
    private static final long TIMESTAMP_NOW = System.currentTimeMillis();
    private static final long TIMESTAMP_31_DAYS_BEFORE = TIMESTAMP_NOW - TimeUnit.DAYS.toMillis(31);

    private Context mContext;
    private JobScheduler mJobScheduler;
    @Mock private JobParameters mParams;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mJobScheduler =
                spy(new JobSchedulerImpl(mContext, IJobScheduler.Stub.asInterface(new Binder())));
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void scheduleCleanUp() {
        AnomalyCleanupJobService.scheduleCleanUp(mContext);

        JobScheduler jobScheduler = mContext.getSystemService(JobScheduler.class);
        List<JobInfo> pendingJobs = jobScheduler.getAllPendingJobs();
        assertEquals(1, pendingJobs.size());
        JobInfo pendingJob = pendingJobs.get(0);
        assertThat(pendingJob.getId()).isEqualTo(R.integer.job_anomaly_clean_up);
        assertThat(pendingJob.getIntervalMillis()).isEqualTo(TimeUnit.DAYS.toMillis(1));
        assertThat(pendingJob.isRequireDeviceIdle()).isTrue();
        assertThat(pendingJob.isRequireCharging()).isTrue();
        assertThat(pendingJob.isPersisted()).isTrue();
    }

    @Test
    public void scheduleCleanUp_invokeTwice_onlyScheduleOnce() {
        AnomalyCleanupJobService.scheduleCleanUp(mContext);
        AnomalyCleanupJobService.scheduleCleanUp(mContext);

        verify(mJobScheduler, times(1)).schedule(any());
    }

    @Test
    @Ignore
    public void onStartJob_cleanUpDataBefore30days() {
        final BatteryDatabaseManager databaseManager = BatteryDatabaseManager.getInstance(mContext);
        final AnomalyCleanupJobService service =
                spy(Robolectric.setupService(AnomalyCleanupJobService.class));
        doNothing().when(service).jobFinished(any(), anyBoolean());

        // Insert two records, one is current and the other one is 31 days before
        databaseManager.insertAnomaly(
                UID, PACKAGE_NAME, ANOMALY_TYPE, AnomalyDatabaseHelper.State.NEW, TIMESTAMP_NOW);
        databaseManager.insertAnomaly(
                UID,
                PACKAGE_NAME_OLD,
                ANOMALY_TYPE,
                AnomalyDatabaseHelper.State.NEW,
                TIMESTAMP_31_DAYS_BEFORE);

        service.onStartJob(mParams);

        // In database, it only contains the current record
        final List<AppInfo> appInfos =
                databaseManager.queryAllAnomalies(0, AnomalyDatabaseHelper.State.NEW);
        assertThat(appInfos)
                .containsExactly(
                        new AppInfo.Builder()
                                .setUid(UID)
                                .setPackageName(PACKAGE_NAME)
                                .addAnomalyType(ANOMALY_TYPE)
                                .build());
    }
}
