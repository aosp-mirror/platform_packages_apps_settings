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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.StatsManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StatsDimensionsValue;
import android.os.UserManager;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowJobScheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SettingsRobolectricTestRunner.class)
public class AnomalyDetectionJobServiceTest {
    private static final int UID = 123;
    private static final String SYSTEM_PACKAGE = "com.android.system";
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private UserManager mUserManager;
    @Mock
    private BatteryDatabaseManager mBatteryDatabaseManager;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private PowerWhitelistBackend mPowerWhitelistBackend;
    @Mock
    private StatsDimensionsValue mStatsDimensionsValue;

    private BatteryTipPolicy mPolicy;
    private Bundle mBundle;
    private AnomalyDetectionJobService mAnomalyDetectionJobService;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPolicy = new BatteryTipPolicy(mContext);
        mBundle = new Bundle();
        mBundle.putParcelable(StatsManager.EXTRA_STATS_DIMENSIONS_VALUE, mStatsDimensionsValue);

        mAnomalyDetectionJobService = new AnomalyDetectionJobService();
    }

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

    @Test
    public void testSaveAnomalyToDatabase_systemWhitelisted_doNotSave() {
        doReturn(SYSTEM_PACKAGE).when(mBatteryUtils).getPackageName(anyInt());
        doReturn(true).when(mPowerWhitelistBackend).isSysWhitelisted(SYSTEM_PACKAGE);

        mAnomalyDetectionJobService.saveAnomalyToDatabase(mBatteryStatsHelper, mUserManager,
                mBatteryDatabaseManager, mBatteryUtils, mPolicy, mPowerWhitelistBackend,
                mContext.getContentResolver(), mBundle);

        verify(mBatteryDatabaseManager, never()).insertAnomaly(anyInt(), anyString(), anyInt(),
                anyInt(), anyLong());
    }

    @Test
    public void testSaveAnomalyToDatabase_normalApp_save() {
        doReturn(SYSTEM_PACKAGE).when(mBatteryUtils).getPackageName(anyInt());
        doReturn(false).when(mPowerWhitelistBackend).isSysWhitelisted(SYSTEM_PACKAGE);

        mAnomalyDetectionJobService.saveAnomalyToDatabase(mBatteryStatsHelper, mUserManager,
                mBatteryDatabaseManager, mBatteryUtils, mPolicy, mPowerWhitelistBackend,
                mContext.getContentResolver(), mBundle);

        verify(mBatteryDatabaseManager).insertAnomaly(anyInt(), anyString(), anyInt(), anyInt(),
                anyLong());
    }
}
