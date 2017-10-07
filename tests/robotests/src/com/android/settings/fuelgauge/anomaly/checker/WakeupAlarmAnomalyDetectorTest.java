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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.BatteryStats;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.ArraySet;

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
import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WakeupAlarmAnomalyDetectorTest {
    private static final String TARGET_PACKAGE_NAME = "com.android.target";
    private static final String ANOMALY_PACKAGE_NAME = "com.android.anomaly";
    private static final boolean TARGET_BACKGROUND_RESTRICTION_ON = false;
    private static final boolean ANOMALY_BACKGROUND_RESTRICTION_ON = true;
    private static final int TARGET_SDK = Build.VERSION_CODES.L;
    private static final int ANOMALY_SDK = Build.VERSION_CODES.O;
    private static final int ANOMALY_UID = 111;
    private static final int NORMAL_UID = 222;
    private static final int TARGET_UID = 333;
    private static final long RUNNING_TIME_MS =
            1 * DateUtils.HOUR_IN_MILLIS + 10 * DateUtils.MINUTE_IN_MILLIS;
    private static final int ANOMALY_WAKEUP_COUNT = 500;
    private static final int NORMAL_WAKEUP_COUNT = 61;
    private static final int BLACKLISTED_WAKEUP_COUNT = 37;
    private static final int ANOMALY_WAKEUP_FREQUENCY = 428; // count per hour
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
    private BatteryStats.Uid.Pkg mPkg;
    @Mock
    private BatteryStats.Counter mCounter;
    @Mock
    private BatteryStats.Counter mCounter2;
    @Mock
    private AnomalyDetectionPolicy mPolicy;
    @Mock
    private AnomalyAction mAnomalyAction;
    @Mock
    private AnomalyUtils mAnomalyUtils;

    private WakeupAlarmAnomalyDetector mWakeupAlarmAnomalyDetector;
    private Context mContext;
    private List<BatterySipper> mUsageList;
    private Anomaly mAnomaly;
    private Anomaly mTargetAnomaly;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        ReflectionHelpers.setField(mPolicy, "wakeupAlarmThreshold", 60);
        final Set<String> blacklistedTags = new ArraySet<>();
        blacklistedTags.add("blacklistedTag");
        ReflectionHelpers.setField(mPolicy, "wakeupBlacklistedTags", blacklistedTags);

        doReturn(false).when(mBatteryUtils).shouldHideSipper(any());
        doReturn(RUNNING_TIME_MS).when(mBatteryUtils).calculateRunningTimeBasedOnStatsType(any(),
                anyInt());
        doReturn(true).when(mAnomalyAction).isActionActive(any());
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

        doReturn(TARGET_PACKAGE_NAME).when(mBatteryUtils).getPackageName(TARGET_UID);
        doReturn(ANOMALY_PACKAGE_NAME).when(mBatteryUtils).getPackageName(ANOMALY_UID);
        doReturn(TARGET_SDK).when(mBatteryUtils).getTargetSdkVersion(TARGET_PACKAGE_NAME);
        doReturn(ANOMALY_SDK).when(mBatteryUtils).getTargetSdkVersion(ANOMALY_PACKAGE_NAME);
        doReturn(TARGET_BACKGROUND_RESTRICTION_ON).when(mBatteryUtils)
                .isBackgroundRestrictionEnabled(TARGET_SDK, TARGET_UID, TARGET_PACKAGE_NAME);
        doReturn(ANOMALY_BACKGROUND_RESTRICTION_ON).when(mBatteryUtils)
                .isBackgroundRestrictionEnabled(ANOMALY_SDK, ANOMALY_UID, ANOMALY_PACKAGE_NAME);

        mAnomaly = new Anomaly.Builder()
                .setUid(ANOMALY_UID)
                .setPackageName(ANOMALY_PACKAGE_NAME)
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setTargetSdkVersion(ANOMALY_SDK)
                .setBackgroundRestrictionEnabled(ANOMALY_BACKGROUND_RESTRICTION_ON)
                .setWakeupAlarmCount(ANOMALY_WAKEUP_FREQUENCY)
                .build();
        mTargetAnomaly = new Anomaly.Builder()
                .setUid(TARGET_UID)
                .setPackageName(TARGET_PACKAGE_NAME)
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .setTargetSdkVersion(TARGET_SDK)
                .setBackgroundRestrictionEnabled(TARGET_BACKGROUND_RESTRICTION_ON)
                .setWakeupAlarmCount(ANOMALY_WAKEUP_FREQUENCY)
                .build();

        mWakeupAlarmAnomalyDetector = spy(
                new WakeupAlarmAnomalyDetector(mContext, mPolicy, mAnomalyUtils));
        mWakeupAlarmAnomalyDetector.mBatteryUtils = mBatteryUtils;
    }

    @Test
    public void testDetectAnomalies_containsAnomaly_detectIt() {
        doReturn(-1).when(mBatteryUtils).getPackageUid(nullable(String.class));
        doReturn(ANOMALY_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mAnomalyUid);
        doReturn(ANOMALY_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mTargetUid);
        doReturn(NORMAL_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mNormalUid);

        List<Anomaly> mAnomalies = mWakeupAlarmAnomalyDetector.detectAnomalies(mBatteryStatsHelper);

        assertThat(mAnomalies).containsExactly(mAnomaly, mTargetAnomaly);
    }

    @Test
    public void testDetectAnomalies_detectTargetAnomaly_detectIt() {
        doReturn(TARGET_UID).when(mBatteryUtils).getPackageUid(TARGET_PACKAGE_NAME);
        doReturn(ANOMALY_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mAnomalyUid);
        doReturn(ANOMALY_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mTargetUid);
        doReturn(NORMAL_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mNormalUid);

        List<Anomaly> mAnomalies = mWakeupAlarmAnomalyDetector.detectAnomalies(mBatteryStatsHelper,
                TARGET_PACKAGE_NAME);

        assertThat(mAnomalies).containsExactly(mTargetAnomaly);
    }

    @Test
    public void testGetWakeupAlarmCountFromUid_countCorrect() {
        final ArrayMap<String, BatteryStats.Uid.Pkg> packageStats = new ArrayMap<>();
        final ArrayMap<String, BatteryStats.Counter> alarms = new ArrayMap<>();
        doReturn(alarms).when(mPkg).getWakeupAlarmStats();
        doReturn(NORMAL_WAKEUP_COUNT).when(mCounter).getCountLocked(anyInt());
        doReturn(packageStats).when(mAnomalyUid).getPackageStats();
        packageStats.put("", mPkg);
        alarms.put("1", mCounter);
        alarms.put("2", mCounter);

        assertThat(mWakeupAlarmAnomalyDetector.getWakeupAlarmCountFromUid(mAnomalyUid)).isEqualTo(
                2 * NORMAL_WAKEUP_COUNT);
    }

    @Test
    public void testGetWakeupAlarmCountFromUid_filterOutBlacklistedTags() {
        final ArrayMap<String, BatteryStats.Uid.Pkg> packageStats = new ArrayMap<>();
        final ArrayMap<String, BatteryStats.Counter> alarms = new ArrayMap<>();
        doReturn(alarms).when(mPkg).getWakeupAlarmStats();
        doReturn(NORMAL_WAKEUP_COUNT).when(mCounter).getCountLocked(anyInt());
        doReturn(BLACKLISTED_WAKEUP_COUNT).when(mCounter2).getCountLocked(anyInt());
        doReturn(packageStats).when(mAnomalyUid).getPackageStats();
        packageStats.put("", mPkg);
        alarms.put("allowedTag", mCounter);
        alarms.put("blacklistedTag", mCounter2);

        assertThat(mWakeupAlarmAnomalyDetector.getWakeupAlarmCountFromUid(mAnomalyUid)).isEqualTo(
                NORMAL_WAKEUP_COUNT);
    }
}
