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

import static android.os.StatsDimensionsValue.FLOAT_VALUE_TYPE;
import static android.os.StatsDimensionsValue.INT_VALUE_TYPE;
import static android.os.StatsDimensionsValue.TUPLE_VALUE_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.JobSchedulerImpl;
import android.app.StatsManager;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.StatsDimensionsValue;
import android.os.UserManager;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowConnectivityManager.class})
public class AnomalyDetectionJobServiceTest {
    private static final int UID = 12345;
    private static final String SYSTEM_PACKAGE = "com.android.system";
    private static final String SUBSCRIBER_COOKIES_AUTO_RESTRICTION =
            "anomaly_type=6,auto_restriction=true";
    private static final String SUBSCRIBER_COOKIES_NOT_AUTO_RESTRICTION =
            "anomaly_type=6,auto_restriction=false";
    private static final int ANOMALY_TYPE = 6;
    private static final long VERSION_CODE = 15;
    @Mock private UserManager mUserManager;
    @Mock private BatteryDatabaseManager mBatteryDatabaseManager;
    @Mock private BatteryUtils mBatteryUtils;
    @Mock private PowerAllowlistBackend mPowerAllowlistBackend;
    @Mock private StatsDimensionsValue mStatsDimensionsValue;
    @Mock private JobParameters mJobParameters;
    @Mock private JobWorkItem mJobWorkItem;

    private BatteryTipPolicy mPolicy;
    private Bundle mBundle;
    private AnomalyDetectionJobService mAnomalyDetectionJobService;
    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private JobScheduler mJobScheduler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mJobScheduler =
                spy(new JobSchedulerImpl(mContext, IJobScheduler.Stub.asInterface(new Binder())));
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);

        mPolicy = new BatteryTipPolicy(mContext);
        mBundle = new Bundle();
        mBundle.putParcelable(StatsManager.EXTRA_STATS_DIMENSIONS_VALUE, mStatsDimensionsValue);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mBatteryUtils.getAppLongVersionCode(any())).thenReturn(VERSION_CODE);

        final ServiceController<AnomalyDetectionJobService> controller =
                Robolectric.buildService(AnomalyDetectionJobService.class);
        mAnomalyDetectionJobService = spy(controller.get());
        doNothing().when(mAnomalyDetectionJobService).jobFinished(any(), anyBoolean());
    }

    @Test
    public void scheduleCleanUp() {
        AnomalyDetectionJobService.scheduleAnomalyDetection(mContext, new Intent());

        JobScheduler jobScheduler = mContext.getSystemService(JobScheduler.class);
        List<JobInfo> pendingJobs = jobScheduler.getAllPendingJobs();
        assertThat(pendingJobs).hasSize(1);

        JobInfo pendingJob = pendingJobs.get(0);
        assertThat(pendingJob.getId()).isEqualTo(R.integer.job_anomaly_detection);
        assertThat(pendingJob.getMaxExecutionDelayMillis())
                .isEqualTo(TimeUnit.MINUTES.toMillis(30));
    }

    @Test
    public void saveAnomalyToDatabase_systemAllowlisted_doNotSave() {
        doReturn(UID).when(mAnomalyDetectionJobService).extractUidFromStatsDimensionsValue(any());
        doReturn(true)
                .when(mPowerAllowlistBackend)
                .isAllowlisted(any(String[].class), any(Integer.class));

        mAnomalyDetectionJobService.saveAnomalyToDatabase(
                mContext,
                mUserManager,
                mBatteryDatabaseManager,
                mBatteryUtils,
                mPolicy,
                mPowerAllowlistBackend,
                mContext.getContentResolver(),
                mFeatureFactory.powerUsageFeatureProvider,
                mFeatureFactory.metricsFeatureProvider,
                mBundle);

        verify(mBatteryDatabaseManager, never())
                .insertAnomaly(anyInt(), anyString(), anyInt(), anyInt(), anyLong());
    }

    @Test
    public void saveAnomalyToDatabase_systemApp_doNotSaveButLog() {
        final ArrayList<String> cookies = new ArrayList<>();
        cookies.add(SUBSCRIBER_COOKIES_AUTO_RESTRICTION);
        mBundle.putStringArrayList(StatsManager.EXTRA_STATS_BROADCAST_SUBSCRIBER_COOKIES, cookies);
        doReturn(SYSTEM_PACKAGE).when(mBatteryUtils).getPackageName(anyInt());
        doReturn(false).when(mPowerAllowlistBackend).isSysAllowlisted(SYSTEM_PACKAGE);
        doReturn(Process.FIRST_APPLICATION_UID)
                .when(mAnomalyDetectionJobService)
                .extractUidFromStatsDimensionsValue(any());
        doReturn(true).when(mBatteryUtils).shouldHideAnomaly(any(), anyInt(), any());

        mAnomalyDetectionJobService.saveAnomalyToDatabase(
                mContext,
                mUserManager,
                mBatteryDatabaseManager,
                mBatteryUtils,
                mPolicy,
                mPowerAllowlistBackend,
                mContext.getContentResolver(),
                mFeatureFactory.powerUsageFeatureProvider,
                mFeatureFactory.metricsFeatureProvider,
                mBundle);

        verify(mBatteryDatabaseManager, never())
                .insertAnomaly(anyInt(), anyString(), anyInt(), anyInt(), anyLong());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.PAGE_UNKNOWN,
                        MetricsProto.MetricsEvent.ACTION_ANOMALY_IGNORED,
                        SettingsEnums.PAGE_UNKNOWN,
                        SYSTEM_PACKAGE + "/" + VERSION_CODE,
                        ANOMALY_TYPE);
    }

    @Test
    public void saveAnomalyToDatabase_systemUid_doNotSave() {
        doReturn(Process.SYSTEM_UID)
                .when(mAnomalyDetectionJobService)
                .extractUidFromStatsDimensionsValue(any());

        mAnomalyDetectionJobService.saveAnomalyToDatabase(
                mContext,
                mUserManager,
                mBatteryDatabaseManager,
                mBatteryUtils,
                mPolicy,
                mPowerAllowlistBackend,
                mContext.getContentResolver(),
                mFeatureFactory.powerUsageFeatureProvider,
                mFeatureFactory.metricsFeatureProvider,
                mBundle);

        verify(mBatteryDatabaseManager, never())
                .insertAnomaly(anyInt(), anyString(), anyInt(), anyInt(), anyLong());
    }

    @Test
    public void saveAnomalyToDatabase_uidNull_doNotSave() {
        doReturn(AnomalyDetectionJobService.UID_NULL)
                .when(mAnomalyDetectionJobService)
                .extractUidFromStatsDimensionsValue(any());

        mAnomalyDetectionJobService.saveAnomalyToDatabase(
                mContext,
                mUserManager,
                mBatteryDatabaseManager,
                mBatteryUtils,
                mPolicy,
                mPowerAllowlistBackend,
                mContext.getContentResolver(),
                mFeatureFactory.powerUsageFeatureProvider,
                mFeatureFactory.metricsFeatureProvider,
                mBundle);

        verify(mBatteryDatabaseManager, never())
                .insertAnomaly(anyInt(), anyString(), anyInt(), anyInt(), anyLong());
    }

    @Test
    public void saveAnomalyToDatabase_normalAppWithAutoRestriction_save() {
        final ArrayList<String> cookies = new ArrayList<>();
        cookies.add(SUBSCRIBER_COOKIES_AUTO_RESTRICTION);
        mBundle.putStringArrayList(StatsManager.EXTRA_STATS_BROADCAST_SUBSCRIBER_COOKIES, cookies);
        doReturn(SYSTEM_PACKAGE).when(mBatteryUtils).getPackageName(anyInt());
        doReturn(false).when(mPowerAllowlistBackend).isSysAllowlisted(SYSTEM_PACKAGE);
        doReturn(Process.FIRST_APPLICATION_UID)
                .when(mAnomalyDetectionJobService)
                .extractUidFromStatsDimensionsValue(any());

        mAnomalyDetectionJobService.saveAnomalyToDatabase(
                mContext,
                mUserManager,
                mBatteryDatabaseManager,
                mBatteryUtils,
                mPolicy,
                mPowerAllowlistBackend,
                mContext.getContentResolver(),
                mFeatureFactory.powerUsageFeatureProvider,
                mFeatureFactory.metricsFeatureProvider,
                mBundle);

        verify(mBatteryDatabaseManager)
                .insertAnomaly(
                        anyInt(),
                        anyString(),
                        eq(6),
                        eq(AnomalyDatabaseHelper.State.AUTO_HANDLED),
                        anyLong());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.PAGE_UNKNOWN,
                        MetricsProto.MetricsEvent.ACTION_ANOMALY_TRIGGERED,
                        SettingsEnums.PAGE_UNKNOWN,
                        SYSTEM_PACKAGE + "/" + VERSION_CODE,
                        ANOMALY_TYPE);
    }

    @Test
    public void saveAnomalyToDatabase_normalAppWithoutAutoRestriction_save() {
        final ArrayList<String> cookies = new ArrayList<>();
        cookies.add(SUBSCRIBER_COOKIES_NOT_AUTO_RESTRICTION);
        mBundle.putStringArrayList(StatsManager.EXTRA_STATS_BROADCAST_SUBSCRIBER_COOKIES, cookies);
        doReturn(SYSTEM_PACKAGE).when(mBatteryUtils).getPackageName(anyInt());
        doReturn(false).when(mPowerAllowlistBackend).isSysAllowlisted(SYSTEM_PACKAGE);
        doReturn(Process.FIRST_APPLICATION_UID)
                .when(mAnomalyDetectionJobService)
                .extractUidFromStatsDimensionsValue(any());

        mAnomalyDetectionJobService.saveAnomalyToDatabase(
                mContext,
                mUserManager,
                mBatteryDatabaseManager,
                mBatteryUtils,
                mPolicy,
                mPowerAllowlistBackend,
                mContext.getContentResolver(),
                mFeatureFactory.powerUsageFeatureProvider,
                mFeatureFactory.metricsFeatureProvider,
                mBundle);

        verify(mBatteryDatabaseManager)
                .insertAnomaly(
                        anyInt(),
                        anyString(),
                        eq(6),
                        eq(AnomalyDatabaseHelper.State.NEW),
                        anyLong());
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.PAGE_UNKNOWN,
                        MetricsProto.MetricsEvent.ACTION_ANOMALY_TRIGGERED,
                        SettingsEnums.PAGE_UNKNOWN,
                        SYSTEM_PACKAGE + "/" + VERSION_CODE,
                        ANOMALY_TYPE);
    }

    @Test
    public void extractUidFromStatsDimensionsValue_extractCorrectUid() {
        // Build an integer dimensions value.
        final StatsDimensionsValue intValue = mock(StatsDimensionsValue.class);
        when(intValue.isValueType(INT_VALUE_TYPE)).thenReturn(true);
        when(intValue.getField()).thenReturn(AnomalyDetectionJobService.STATSD_UID_FILED);
        when(intValue.getIntValue()).thenReturn(UID);

        // Build a tuple dimensions value and put the previous integer dimensions value inside.
        final StatsDimensionsValue tupleValue = mock(StatsDimensionsValue.class);
        when(tupleValue.isValueType(TUPLE_VALUE_TYPE)).thenReturn(true);
        final List<StatsDimensionsValue> statsDimensionsValues = new ArrayList<>();
        statsDimensionsValues.add(intValue);
        when(tupleValue.getTupleValueList()).thenReturn(statsDimensionsValues);

        assertThat(mAnomalyDetectionJobService.extractUidFromStatsDimensionsValue(tupleValue))
                .isEqualTo(UID);
    }

    @Test
    public void extractUidFromStatsDimensionsValue_wrongFormat_returnNull() {
        // Build a float dimensions value
        final StatsDimensionsValue floatValue = mock(StatsDimensionsValue.class);
        when(floatValue.isValueType(FLOAT_VALUE_TYPE)).thenReturn(true);
        when(floatValue.getField()).thenReturn(AnomalyDetectionJobService.STATSD_UID_FILED);
        when(floatValue.getFloatValue()).thenReturn(0f);

        assertThat(mAnomalyDetectionJobService.extractUidFromStatsDimensionsValue(floatValue))
                .isEqualTo(AnomalyDetectionJobService.UID_NULL);
    }

    @Test
    public void stopJobWhileDequeuingWork_shouldNotCrash() {
        when(mJobParameters.dequeueWork()).thenThrow(new SecurityException());

        mAnomalyDetectionJobService.onStopJob(mJobParameters);

        // Should not crash even job is stopped
        mAnomalyDetectionJobService.dequeueWork(mJobParameters);
    }

    @Test
    public void stopJobWhileCompletingWork_shouldNotCrash() {
        doThrow(new SecurityException()).when(mJobParameters).completeWork(any());

        mAnomalyDetectionJobService.onStopJob(mJobParameters);

        // Should not crash even job is stopped
        mAnomalyDetectionJobService.completeWork(mJobParameters, mJobWorkItem);
    }

    @Test
    public void restartWorkAfterBeenStopped_jobStarted() {
        mAnomalyDetectionJobService.onStopJob(mJobParameters);
        mAnomalyDetectionJobService.onStartJob(mJobParameters);

        assertThat(mAnomalyDetectionJobService.mIsJobCanceled).isFalse();
    }
}
