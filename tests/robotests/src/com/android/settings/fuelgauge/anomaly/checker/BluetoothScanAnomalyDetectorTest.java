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

package com.android.settings.fuelgauge.anomaly.checker;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.BatteryStats;
import android.text.format.DateUtils;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.anomaly.AnomalyUtils;
import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothScanAnomalyDetectorTest {
    private static final String TARGET_PACKAGE_NAME = "com.android.app";
    private static final int ANOMALY_UID = 111;
    private static final int NORMAL_UID = 222;
    private static final int TARGET_UID = 333;
    private static final long ANOMALY_BLUETOOTH_SCANNING_TIME = DateUtils.HOUR_IN_MILLIS;
    private static final long NORMAL_BLUETOOTH_SCANNING_TIME = DateUtils.MINUTE_IN_MILLIS;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatterySipper mAnomalySipper;
    @Mock
    private BatterySipper mNormalSipper;
    @Mock
    private BatterySipper mTargetSipper;
    @Mock
    private BatteryStats.Uid mAnomalyUid;
    @Mock
    private BatteryStats.Uid mNormalUid;
    @Mock
    private BatteryStats.Uid mTargetUid;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private AnomalyDetectionPolicy mPolicy;
    @Mock
    private AnomalyAction mAnomalyAction;
    @Mock
    private AnomalyUtils mAnomalyUtils;

    private BluetoothScanAnomalyDetector mBluetoothScanAnomalyDetector;
    private Context mContext;
    private List<BatterySipper> mUsageList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        ReflectionHelpers.setField(mPolicy, "bluetoothScanThreshold",
                30 * DateUtils.MINUTE_IN_MILLIS);
        doReturn(mAnomalyAction).when(mAnomalyUtils).getAnomalyAction(any());

        mAnomalySipper.uidObj = mAnomalyUid;
        doReturn(ANOMALY_UID).when(mAnomalyUid).getUid();
        mNormalSipper.uidObj = mNormalUid;
        doReturn(NORMAL_UID).when(mNormalUid).getUid();
        mTargetSipper.uidObj = mTargetUid;
        doReturn(TARGET_UID).when(mTargetUid).getUid();

        mUsageList = new ArrayList<>();
        mUsageList.add(mAnomalySipper);
        mUsageList.add(mNormalSipper);
        mUsageList.add(mTargetSipper);
        doReturn(mUsageList).when(mBatteryStatsHelper).getUsageList();

        mBluetoothScanAnomalyDetector = spy(new BluetoothScanAnomalyDetector(mContext, mPolicy,
                mAnomalyUtils));
        mBluetoothScanAnomalyDetector.mBatteryUtils = mBatteryUtils;
        doReturn(false).when(mBatteryUtils).shouldHideSipper(any());
        doReturn(true).when(mAnomalyAction).isActionActive(any());

        doReturn(ANOMALY_BLUETOOTH_SCANNING_TIME).when(
                mBluetoothScanAnomalyDetector).getBluetoothUnoptimizedBgTimeMs(eq(mAnomalyUid),
                anyLong());
        doReturn(ANOMALY_BLUETOOTH_SCANNING_TIME).when(
                mBluetoothScanAnomalyDetector).getBluetoothUnoptimizedBgTimeMs(eq(mTargetUid),
                anyLong());
        doReturn(NORMAL_BLUETOOTH_SCANNING_TIME).when(
                mBluetoothScanAnomalyDetector).getBluetoothUnoptimizedBgTimeMs(eq(mNormalUid),
                anyLong());
    }

    @Test
    public void testDetectAnomalies_containsAnomaly_detectIt() {
        doReturn(-1).when(mBatteryUtils).getPackageUid(nullable(String.class));
        final Anomaly anomaly = createBluetoothAnomaly(ANOMALY_UID);
        final Anomaly targetAnomaly = createBluetoothAnomaly(TARGET_UID);

        List<Anomaly> mAnomalies = mBluetoothScanAnomalyDetector.detectAnomalies(
                mBatteryStatsHelper);

        assertThat(mAnomalies).containsExactly(anomaly, targetAnomaly);
    }

    @Test
    public void testDetectAnomalies_detectTargetAnomaly_detectIt() {
        doReturn(TARGET_UID).when(mBatteryUtils).getPackageUid(TARGET_PACKAGE_NAME);
        final Anomaly targetAnomaly = createBluetoothAnomaly(TARGET_UID);

        List<Anomaly> mAnomalies = mBluetoothScanAnomalyDetector.detectAnomalies(
                mBatteryStatsHelper, TARGET_PACKAGE_NAME);

        assertThat(mAnomalies).containsExactly(targetAnomaly);

    }

    private Anomaly createBluetoothAnomaly(int uid) {
        return new Anomaly.Builder()
                .setUid(uid)
                .setType(Anomaly.AnomalyType.BLUETOOTH_SCAN)
                .setBluetoothScanningTimeMs(ANOMALY_BLUETOOTH_SCANNING_TIME)
                .build();
    }

}
