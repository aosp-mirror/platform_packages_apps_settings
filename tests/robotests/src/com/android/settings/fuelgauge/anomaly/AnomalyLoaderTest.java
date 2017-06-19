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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.anomaly.checker.BluetoothScanAnomalyDetector;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.checker.WakeLockAnomalyDetector;
import com.android.settings.fuelgauge.anomaly.checker.WakeupAlarmAnomalyDetector;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AnomalyLoaderTest {
    private static final String PACKAGE_NAME = "com.android.settings";
    private static final CharSequence DISPLAY_NAME = "Settings";
    private static final int UID = 0;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private WakeLockAnomalyDetector mWakeLockAnomalyDetector;
    @Mock
    private WakeupAlarmAnomalyDetector mWakeupAlarmAnomalyDetector;
    @Mock
    private BluetoothScanAnomalyDetector mBluetoothScanAnomalyDetector;
    @Mock
    private AnomalyDetectionPolicy mAnomalyDetectionPolicy;
    @Mock
    private UserManager mUserManager;
    private Anomaly mWakeLockAnomaly;
    private Anomaly mWakeupAlarmAnomaly;
    private Anomaly mBluetoothScanAnomaly;
    private List<Anomaly> mWakeLockAnomalies;
    private List<Anomaly> mWakeupAlarmAnomalies;
    private List<Anomaly> mBluetoothScanAnomalies;
    private AnomalyLoader mAnomalyLoader;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        doReturn(true).when(mAnomalyDetectionPolicy).isAnomalyDetectorEnabled(anyInt());
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        when(mContext.getPackageManager().getPackageUid(anyString(), anyInt())).thenReturn(UID);

        mWakeLockAnomalies = new ArrayList<>();
        mWakeLockAnomaly = createAnomaly(Anomaly.AnomalyType.WAKE_LOCK);
        mWakeLockAnomalies.add(mWakeLockAnomaly);
        doReturn(mWakeLockAnomalies).when(mWakeLockAnomalyDetector).detectAnomalies(any(), any());

        mWakeupAlarmAnomalies = new ArrayList<>();
        mWakeupAlarmAnomaly = createAnomaly(Anomaly.AnomalyType.WAKEUP_ALARM);
        mWakeupAlarmAnomalies.add(mWakeupAlarmAnomaly);
        doReturn(mWakeupAlarmAnomalies).when(mWakeupAlarmAnomalyDetector).detectAnomalies(any(),
                any());

        mBluetoothScanAnomalies = new ArrayList<>();
        mBluetoothScanAnomaly = createAnomaly(Anomaly.AnomalyType.BLUETOOTH_SCAN);
        mBluetoothScanAnomalies.add(mBluetoothScanAnomaly);
        doReturn(mBluetoothScanAnomalies).when(mBluetoothScanAnomalyDetector).detectAnomalies(any(),
                any());

        mAnomalyLoader = new AnomalyLoader(mContext, mBatteryStatsHelper, null,
                mAnomalyDetectionPolicy);
        mAnomalyLoader.mAnomalyUtils = spy(new AnomalyUtils(mContext));
    }

    @Test
    public void testLoadInBackground_containsValidAnomalies() {
        doReturn(mWakeLockAnomalyDetector).when(mAnomalyLoader.mAnomalyUtils).getAnomalyDetector(
                Anomaly.AnomalyType.WAKE_LOCK);
        doReturn(mWakeupAlarmAnomalyDetector).when(mAnomalyLoader.mAnomalyUtils).getAnomalyDetector(
                Anomaly.AnomalyType.WAKEUP_ALARM);
        doReturn(mBluetoothScanAnomalyDetector).when(
                mAnomalyLoader.mAnomalyUtils).getAnomalyDetector(
                Anomaly.AnomalyType.BLUETOOTH_SCAN);

        List<Anomaly> anomalies = mAnomalyLoader.loadInBackground();

        assertThat(anomalies).containsExactly(mWakeLockAnomaly, mWakeupAlarmAnomaly,
                mBluetoothScanAnomaly);
    }

    private Anomaly createAnomaly(@Anomaly.AnomalyType int type) {
        return new Anomaly.Builder()
                .setType(type)
                .setUid(UID)
                .setPackageName(PACKAGE_NAME)
                .setDisplayName(DISPLAY_NAME)
                .build();
    }

    @Test
    public void testGenerateFakeData() {
        List<Anomaly> anomalies = mAnomalyLoader.generateFakeData();

        assertThat(anomalies).containsExactly(mWakeLockAnomaly, mWakeupAlarmAnomaly,
                mBluetoothScanAnomaly);
    }
}
