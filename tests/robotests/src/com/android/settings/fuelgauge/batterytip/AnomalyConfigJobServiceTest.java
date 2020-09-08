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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.JobSchedulerImpl;
import android.app.StatsManager;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Binder;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class AnomalyConfigJobServiceTest {

    private static final int ANOMALY_CONFIG_VERSION = 1;
    private static final String ANOMALY_CONFIG = "X64s";
    @Mock
    private StatsManager mStatsManager;

    private Context mContext;
    private JobScheduler mJobScheduler;
    private AnomalyConfigJobService mJobService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mJobScheduler = spy(new JobSchedulerImpl(IJobScheduler.Stub.asInterface(new Binder())));
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);

        mJobService = spy(new AnomalyConfigJobService());
        doReturn(application.getSharedPreferences(AnomalyConfigJobService.PREF_DB,
                Context.MODE_PRIVATE)).when(mJobService).getSharedPreferences(anyString(),
                anyInt());
        doReturn(application.getContentResolver()).when(mJobService).getContentResolver();
    }

    @Test
    public void testScheduleConfigUpdate() {
        AnomalyConfigJobService.scheduleConfigUpdate(mContext);

        JobScheduler jobScheduler = mContext.getSystemService(JobScheduler.class);
        List<JobInfo> pendingJobs = jobScheduler.getAllPendingJobs();
        assertEquals(1, pendingJobs.size());
        JobInfo pendingJob = pendingJobs.get(0);
        assertThat(pendingJob.getId()).isEqualTo(R.integer.job_anomaly_config_update);
        assertThat(pendingJob.getIntervalMillis()).isEqualTo(TimeUnit.DAYS.toMillis(1));
        assertThat(pendingJob.isRequireDeviceIdle()).isTrue();
        assertThat(pendingJob.isRequireCharging()).isTrue();
        assertThat(pendingJob.isPersisted()).isTrue();
    }

    @Test
    public void testScheduleConfigUpdate_invokeTwice_onlyScheduleOnce() {
        AnomalyConfigJobService.scheduleConfigUpdate(mContext);
        AnomalyConfigJobService.scheduleConfigUpdate(mContext);

        verify(mJobScheduler, times(1)).schedule(any());
    }

    @Test
    public void checkAnomalyConfig_newConfigExist_removeOldConfig()
            throws StatsManager.StatsUnavailableException{
        Settings.Global.putInt(application.getContentResolver(),
                Settings.Global.ANOMALY_CONFIG_VERSION, ANOMALY_CONFIG_VERSION);
        Settings.Global.putString(application.getContentResolver(), Settings.Global.ANOMALY_CONFIG,
                ANOMALY_CONFIG);

        mJobService.checkAnomalyConfig(mStatsManager);

        verify(mStatsManager).removeConfig(StatsManagerConfig.ANOMALY_CONFIG_KEY);
    }

    @Test
    public void checkAnomalyConfig_newConfigExist_uploadNewConfig()
            throws StatsManager.StatsUnavailableException{
        Settings.Global.putInt(application.getContentResolver(),
                Settings.Global.ANOMALY_CONFIG_VERSION, ANOMALY_CONFIG_VERSION);
        Settings.Global.putString(application.getContentResolver(), Settings.Global.ANOMALY_CONFIG,
                ANOMALY_CONFIG);

        mJobService.checkAnomalyConfig(mStatsManager);

        verify(mStatsManager).addConfig(eq(StatsManagerConfig.ANOMALY_CONFIG_KEY), any());
    }
}
