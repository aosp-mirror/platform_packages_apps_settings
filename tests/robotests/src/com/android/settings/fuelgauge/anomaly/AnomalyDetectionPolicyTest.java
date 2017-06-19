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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.KeyValueListParser;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalyDetectionPolicyTest {
    private static final String ANOMALY_DETECTION_CONSTANTS_VALUE = "anomaly_detection_enabled=true"
            + ",wakelock_enabled=false"
            + ",wakelock_threshold=3000"
            + ",wakeup_alarm_enabled=true"
            + ",wakeup_alarm_threshold=100"
            + ",bluetooth_scan_enabled=true"
            + ",bluetooth_scan_threshold=2000";
    private Context mContext;
    private KeyValueListParserWrapper mKeyValueListParserWrapper;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mKeyValueListParserWrapper = spy(
                new KeyValueListParserWrapperImpl(new KeyValueListParser(',')));
    }

    @Test
    public void testInit_containsDataFromSettings() {
        AnomalyDetectionPolicy anomalyDetectionPolicy = createAnomalyPolicyWithConfig();

        assertThat(anomalyDetectionPolicy.anomalyDetectionEnabled).isTrue();
        assertThat(anomalyDetectionPolicy.wakeLockDetectionEnabled).isFalse();
        assertThat(anomalyDetectionPolicy.wakeLockThreshold).isEqualTo(3000);
        assertThat(anomalyDetectionPolicy.wakeupAlarmDetectionEnabled).isTrue();
        assertThat(anomalyDetectionPolicy.wakeupAlarmThreshold).isEqualTo(100);
        assertThat(anomalyDetectionPolicy.bluetoothScanDetectionEnabled).isTrue();
        assertThat(anomalyDetectionPolicy.bluetoothScanThreshold).isEqualTo(2000);
    }

    @Test
    public void testInit_containsDefaultData() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.ANOMALY_DETECTION_CONSTANTS, "");
        // Mock it to avoid noSuchMethodError
        doReturn(true).when(mKeyValueListParserWrapper).getBoolean(anyString(), eq(true));
        doReturn(false).when(mKeyValueListParserWrapper).getBoolean(anyString(), eq(false));

        AnomalyDetectionPolicy anomalyDetectionPolicy = new AnomalyDetectionPolicy(mContext,
                mKeyValueListParserWrapper);

        assertThat(anomalyDetectionPolicy.anomalyDetectionEnabled).isTrue();
        assertThat(anomalyDetectionPolicy.wakeLockDetectionEnabled).isTrue();
        assertThat(anomalyDetectionPolicy.wakeLockThreshold).isEqualTo(DateUtils.HOUR_IN_MILLIS);
        assertThat(anomalyDetectionPolicy.wakeupAlarmDetectionEnabled).isTrue();
        assertThat(anomalyDetectionPolicy.wakeupAlarmThreshold).isEqualTo(60);
        assertThat(anomalyDetectionPolicy.bluetoothScanDetectionEnabled).isTrue();
        assertThat(anomalyDetectionPolicy.bluetoothScanThreshold).isEqualTo(
                30 * DateUtils.MINUTE_IN_MILLIS);
    }

    @Test
    public void testIsAnomalyDetectorEnabled() {
        AnomalyDetectionPolicy anomalyDetectionPolicy = createAnomalyPolicyWithConfig();

        assertThat(anomalyDetectionPolicy.isAnomalyDetectorEnabled(
                Anomaly.AnomalyType.WAKE_LOCK)).isFalse();
        assertThat(anomalyDetectionPolicy.isAnomalyDetectorEnabled(
                Anomaly.AnomalyType.WAKEUP_ALARM)).isTrue();
        assertThat(anomalyDetectionPolicy.isAnomalyDetectorEnabled(
                Anomaly.AnomalyType.BLUETOOTH_SCAN)).isTrue();
    }

    private AnomalyDetectionPolicy createAnomalyPolicyWithConfig() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.ANOMALY_DETECTION_CONSTANTS, ANOMALY_DETECTION_CONSTANTS_VALUE);
        // Mock it to avoid noSuchMethodError
        doReturn(true).when(mKeyValueListParserWrapper).getBoolean(
                AnomalyDetectionPolicy.KEY_ANOMALY_DETECTION_ENABLED, true);
        doReturn(false).when(mKeyValueListParserWrapper).getBoolean(
                AnomalyDetectionPolicy.KEY_WAKELOCK_DETECTION_ENABLED, true);
        doReturn(true).when(mKeyValueListParserWrapper).getBoolean(
                AnomalyDetectionPolicy.KEY_WAKEUP_ALARM_DETECTION_ENABLED, true);
        doReturn(true).when(mKeyValueListParserWrapper).getBoolean(
                AnomalyDetectionPolicy.KEY_BLUETOOTH_SCAN_DETECTION_ENABLED, true);

        return new AnomalyDetectionPolicy(mContext, mKeyValueListParserWrapper);
    }


}
