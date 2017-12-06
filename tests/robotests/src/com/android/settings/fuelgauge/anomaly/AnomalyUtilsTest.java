/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge.anomaly;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.os.Build;
import android.util.Pair;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.fuelgauge.anomaly.action.StopAndBackgroundCheckAction;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.action.ForceStopAction;
import com.android.settings.fuelgauge.anomaly.checker.WakeLockAnomalyDetector;
import com.android.settings.testutils.shadow.ShadowKeyValueListParserWrapperImpl;
import com.android.settings.fuelgauge.anomaly.checker.WakeupAlarmAnomalyDetector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION, shadows = {
        ShadowKeyValueListParserWrapperImpl.class})
public class AnomalyUtilsTest {
    private static final String PACKAGE_NAME_WAKEUP = "com.android.app1";
    private static final String PACKAGE_NAME_WAKELOCK = "com.android.app2";
    private static final int CONTEXT_ID = 55;

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private AnomalyUtils mAnomalyUtils;
    private Anomaly mWakeupAnomaly;
    private Anomaly mWakeLockAnomaly;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mAnomalyUtils = new AnomalyUtils(RuntimeEnvironment.application);

        mWakeLockAnomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKE_LOCK)
                .setPackageName(PACKAGE_NAME_WAKELOCK)
                .build();
        mWakeupAnomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setPackageName(PACKAGE_NAME_WAKEUP)
                .build();
    }

    @Test
    public void testGetAnomalyAction_typeWakeLock_returnForceStop() {
        Anomaly anomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKE_LOCK)
                .build();
        assertThat(mAnomalyUtils.getAnomalyAction(anomaly)).isInstanceOf(
                ForceStopAction.class);
    }

    @Test
    public void testGetAnomalyDetector_typeWakeLock_returnWakeLockDetector() {
        assertThat(mAnomalyUtils.getAnomalyDetector(Anomaly.AnomalyType.WAKE_LOCK)).isInstanceOf(
                WakeLockAnomalyDetector.class);
    }

    @Test
    public void testGetAnomalyAction_typeWakeUpAlarmTargetO_returnForceStop() {
        Anomaly anomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setTargetSdkVersion(Build.VERSION_CODES.O)
                .build();
        assertThat(mAnomalyUtils.getAnomalyAction(anomaly)).isInstanceOf(
                ForceStopAction.class);
    }

    @Test
    public void testGetAnomalyAction_typeWakeUpAlarmTargetPriorOAndBgOff_returnStopAndBackground() {
        Anomaly anomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setTargetSdkVersion(Build.VERSION_CODES.L)
                .setBackgroundRestrictionEnabled(false)
                .build();
        assertThat(mAnomalyUtils.getAnomalyAction(anomaly)).isInstanceOf(
                StopAndBackgroundCheckAction.class);
    }

    @Test
    public void testGetAnomalyAction_typeWakeUpAlarmTargetPriorOAndBgOn_returnForceStop() {
        Anomaly anomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setTargetSdkVersion(Build.VERSION_CODES.L)
                .setBackgroundRestrictionEnabled(true)
                .build();
        assertThat(mAnomalyUtils.getAnomalyAction(anomaly)).isInstanceOf(
                ForceStopAction.class);
    }

    @Test
    public void testGetAnomalyDetector_typeWakeUpAlarm_returnWakeUpAlarmDetector() {
        assertThat(mAnomalyUtils.getAnomalyDetector(Anomaly.AnomalyType.WAKEUP_ALARM)).isInstanceOf(
                WakeupAlarmAnomalyDetector.class);
    }

    @Test
    public void testLogAnomaly() {
        mAnomalyUtils.logAnomaly(mMetricsFeatureProvider, mWakeLockAnomaly, CONTEXT_ID);

        verify(mMetricsFeatureProvider).action(RuntimeEnvironment.application,
                MetricsProto.MetricsEvent.ANOMALY_TYPE_WAKELOCK,
                PACKAGE_NAME_WAKELOCK,
                Pair.create(
                        MetricsProto.MetricsEvent.FIELD_CONTEXT,
                        CONTEXT_ID),
                Pair.create(
                        MetricsProto.MetricsEvent.FIELD_ANOMALY_ACTION_TYPE,
                        Anomaly.AnomalyActionType.FORCE_STOP));
    }

    @Test
    public void testLogAnomalies() {
        final List<Anomaly> anomalies = new ArrayList<>();
        anomalies.add(mWakeLockAnomaly);
        anomalies.add(mWakeupAnomaly);

        mAnomalyUtils.logAnomalies(mMetricsFeatureProvider, anomalies, CONTEXT_ID);

        verify(mMetricsFeatureProvider).action(RuntimeEnvironment.application,
                MetricsProto.MetricsEvent.ANOMALY_TYPE_WAKELOCK,
                PACKAGE_NAME_WAKELOCK,
                Pair.create(
                        MetricsProto.MetricsEvent.FIELD_CONTEXT,
                        CONTEXT_ID),
                Pair.create(
                        MetricsProto.MetricsEvent.FIELD_ANOMALY_ACTION_TYPE,
                        Anomaly.AnomalyActionType.FORCE_STOP));
        verify(mMetricsFeatureProvider).action(RuntimeEnvironment.application,
                MetricsProto.MetricsEvent.ANOMALY_TYPE_WAKEUP_ALARM,
                PACKAGE_NAME_WAKEUP,
                Pair.create(
                        MetricsProto.MetricsEvent.FIELD_CONTEXT,
                        CONTEXT_ID),
                Pair.create(
                        MetricsProto.MetricsEvent.FIELD_ANOMALY_ACTION_TYPE,
                        Anomaly.AnomalyActionType.STOP_AND_BACKGROUND_CHECK));
    }
}
