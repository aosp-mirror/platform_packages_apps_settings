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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.text.format.DateUtils;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.anomaly.AnomalyUtils;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;

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
public class WakeLockAnomalyDetectorTest {
    private static final String TARGET_PACKAGE_NAME = "com.android.app";
    private static final long ANOMALY_WAKELOCK_TIME_MS = 2 * DateUtils.HOUR_IN_MILLIS;
    private static final long NORMAL_WAKELOCK_TIME_MS = DateUtils.SECOND_IN_MILLIS;
    private static final long WAKELOCK_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS;
    private static final int ANOMALY_UID = 111;
    private static final int NORMAL_UID = 222;
    private static final int TARGET_UID = 333;
    private static final int INACTIVE_UID = 444;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatterySipper mAnomalySipper;
    @Mock
    private BatterySipper mTargetSipper;
    @Mock
    private BatterySipper mNormalSipper;
    @Mock
    private BatterySipper mInactiveSipper;
    @Mock
    private BatteryStats.Uid mAnomalyUid;
    @Mock
    private BatteryStats.Uid mNormalUid;
    @Mock
    private BatteryStats.Uid mTargetUid;
    @Mock
    private BatteryStats.Uid mInactiveUid;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private AnomalyDetectionPolicy mPolicy;
    @Mock
    private AnomalyAction mAnomalyAction;
    @Mock
    private AnomalyUtils mAnomalyUtils;

    private WakeLockAnomalyDetector mWakelockAnomalyDetector;
    private Context mContext;
    private List<BatterySipper> mUsageList;
    private Anomaly mAnomaly;
    private Anomaly mTargetAnomaly;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        ReflectionHelpers.setField(mPolicy, "wakeLockThreshold", WAKELOCK_THRESHOLD_MS);

        doReturn(false).when(mBatteryUtils).shouldHideSipper(nullable(BatterySipper.class));
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mApplicationInfo).when(mPackageManager)
                .getApplicationInfo(nullable(String.class), anyInt());
        doReturn(true).when(mAnomalyAction).isActionActive(any());
        doReturn(mAnomalyAction).when(mAnomalyUtils).getAnomalyAction(any());

        mWakelockAnomalyDetector = spy(
                new WakeLockAnomalyDetector(mContext, mPolicy, mAnomalyUtils));
        mWakelockAnomalyDetector.mBatteryUtils = mBatteryUtils;

        mAnomalySipper.uidObj = mAnomalyUid;
        doReturn(ANOMALY_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector)
                .getBackgroundTotalDurationMs(eq(mAnomalyUid), anyLong());
        doReturn(ANOMALY_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector).getCurrentDurationMs(
                eq(mAnomalyUid), anyLong());
        doReturn(ANOMALY_UID).when(mAnomalyUid).getUid();

        mNormalSipper.uidObj = mNormalUid;
        doReturn(NORMAL_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector)
                .getBackgroundTotalDurationMs(eq(mNormalUid), anyLong());
        doReturn(0L).when(mWakelockAnomalyDetector).getCurrentDurationMs(eq(mNormalUid),
                anyLong());
        doReturn(NORMAL_UID).when(mNormalUid).getUid();

        mTargetSipper.uidObj = mTargetUid;
        doReturn(ANOMALY_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector)
                .getBackgroundTotalDurationMs(eq(mTargetUid), anyLong());
        doReturn(ANOMALY_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector).getCurrentDurationMs(
                eq(mTargetUid), anyLong());
        doReturn(TARGET_UID).when(mTargetUid).getUid();

        mInactiveSipper.uidObj = mInactiveUid;
        doReturn(ANOMALY_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector)
                .getBackgroundTotalDurationMs(eq(mInactiveUid), anyLong());
        doReturn(0L).when(mWakelockAnomalyDetector).getCurrentDurationMs(eq(mInactiveUid),
                anyLong());
        doReturn(INACTIVE_UID).when(mInactiveUid).getUid();

        mUsageList = new ArrayList<>();
        mUsageList.add(mAnomalySipper);
        mUsageList.add(mNormalSipper);
        mUsageList.add(mTargetSipper);
        mUsageList.add(mInactiveSipper);
        doReturn(mUsageList).when(mBatteryStatsHelper).getUsageList();

        mAnomaly = createWakeLockAnomaly(ANOMALY_UID);
        mTargetAnomaly = createWakeLockAnomaly(TARGET_UID);
    }

    @Test
    public void testDetectAnomalies_containsAnomaly_detectIt() {
        doReturn(BatteryUtils.UID_NULL).when(mBatteryUtils).getPackageUid(nullable(String.class));

        List<Anomaly> mAnomalies = mWakelockAnomalyDetector.detectAnomalies(mBatteryStatsHelper);

        assertThat(mAnomalies).containsExactly(mAnomaly, mTargetAnomaly);
    }

    @Test
    public void testDetectAnomalies_containsTargetPackage_detectIt() {
        doReturn(TARGET_UID).when(mBatteryUtils).getPackageUid(TARGET_PACKAGE_NAME);

        List<Anomaly> mAnomalies = mWakelockAnomalyDetector.detectAnomalies(mBatteryStatsHelper,
                TARGET_PACKAGE_NAME);

        assertThat(mAnomalies).containsExactly(mTargetAnomaly);
    }

    @Test
    public void testContainsThresholdFromPolicy() {
        assertThat(mWakelockAnomalyDetector.mWakeLockThresholdMs).isEqualTo(WAKELOCK_THRESHOLD_MS);
    }

    private Anomaly createWakeLockAnomaly(int uid) {
        return new Anomaly.Builder()
                .setUid(uid)
                .setType(Anomaly.AnomalyType.WAKE_LOCK)
                .setWakeLockTimeMs(ANOMALY_WAKELOCK_TIME_MS)
                .build();
    }
}
