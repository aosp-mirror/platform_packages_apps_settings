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

package com.android.settings.fuelgauge.batterytip.detectors;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.AppLabelPredicate;
import com.android.settings.fuelgauge.batterytip.tips.AppRestrictionPredicate;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.DatabaseTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class RestrictAppDetectorTest {

    private static final int RESTRICTED_UID = 222;
    private static final int UNRESTRICTED_UID = 333;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String UNINSTALLED_PACKAGE_NAME = "com.android.uninstalled";
    private static final String RESTRICTED_PACKAGE_NAME = "com.android.restricted";
    private static final String UNRESTRICTED_PACKAGE_NAME = "com.android.unrestricted";
    private Context mContext;
    private BatteryTipPolicy mPolicy;
    private RestrictAppDetector mRestrictAppDetector;
    private List<AppInfo> mAppInfoList;
    private AppInfo mAppInfo;
    @Mock
    private BatteryDatabaseManager mBatteryDatabaseManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private AppOpsManager mAppOpsManager;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        mAppInfoList = new ArrayList<>();
        mAppInfo = new AppInfo.Builder().setPackageName(PACKAGE_NAME).build();
        mAppInfoList.add(mAppInfo);

        mContext = spy(RuntimeEnvironment.application);
        mPolicy = spy(new BatteryTipPolicy(mContext));

        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mAppOpsManager).when(mContext).getSystemService(AppOpsManager.class);
        doReturn(AppOpsManager.MODE_IGNORED).when(mAppOpsManager).checkOpNoThrow(
                AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, RESTRICTED_UID, RESTRICTED_PACKAGE_NAME);
        doReturn(AppOpsManager.MODE_ALLOWED).when(mAppOpsManager).checkOpNoThrow(
                AppOpsManager.OP_RUN_ANY_IN_BACKGROUND, UNRESTRICTED_UID,
                UNRESTRICTED_PACKAGE_NAME);

        BatteryDatabaseManager.setUpForTest(mBatteryDatabaseManager);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(any(),
                anyInt());
        doReturn(PACKAGE_NAME).when(mApplicationInfo).loadLabel(any());
        doThrow(new PackageManager.NameNotFoundException()).when(
                mPackageManager).getApplicationInfo(eq(UNINSTALLED_PACKAGE_NAME), anyInt());

        mRestrictAppDetector = new RestrictAppDetector(mContext, mPolicy);
        mRestrictAppDetector.mBatteryDatabaseManager = mBatteryDatabaseManager;
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setStaticField(AppLabelPredicate.class, "sInstance", null);
        ReflectionHelpers.setStaticField(AppRestrictionPredicate.class, "sInstance", null);
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testDetect_hasAnomaly_tipNew() {
        doReturn(mAppInfoList).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), eq(AnomalyDatabaseHelper.State.NEW));

        assertThat(mRestrictAppDetector.detect().getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    @Test
    public void testDetect_hasAutoHandledAnomaly_tipHandled() {
        mAppInfoList.add(new AppInfo.Builder()
                .setUid(RESTRICTED_UID)
                .setPackageName(RESTRICTED_PACKAGE_NAME)
                .build());
        doReturn(new ArrayList<AppInfo>()).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), eq(AnomalyDatabaseHelper.State.NEW));
        doReturn(mAppInfoList).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), eq(AnomalyDatabaseHelper.State.AUTO_HANDLED));

        assertThat(mRestrictAppDetector.detect().getState())
                .isEqualTo(BatteryTip.StateType.HANDLED);
    }

    @Test
    public void testDetect_typeNewHasUninstalledAnomaly_removeIt() {
        mAppInfoList.add(new AppInfo.Builder()
                .setPackageName(UNINSTALLED_PACKAGE_NAME)
                .build());
        doReturn(mAppInfoList).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), eq(AnomalyDatabaseHelper.State.NEW));

        final RestrictAppTip restrictAppTip = (RestrictAppTip) mRestrictAppDetector.detect();
        assertThat(restrictAppTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
        assertThat(restrictAppTip.getRestrictAppList()).containsExactly(mAppInfo);
    }

    @Test
    public void testDetect_typeNewHasRestrictedAnomaly_removeIt() throws
            PackageManager.NameNotFoundException {
        mAppInfoList.add(new AppInfo.Builder()
                .setUid(RESTRICTED_UID)
                .setPackageName(RESTRICTED_PACKAGE_NAME)
                .build());
        doReturn(mAppInfoList).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), eq(AnomalyDatabaseHelper.State.NEW));
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(
                eq(RESTRICTED_PACKAGE_NAME), anyInt());

        final RestrictAppTip restrictAppTip = (RestrictAppTip) mRestrictAppDetector.detect();
        assertThat(restrictAppTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
        assertThat(restrictAppTip.getRestrictAppList()).containsExactly(mAppInfo);
    }

    @Test
    public void testDetect_typeHandledHasUnRestrictedAnomaly_removeIt() throws
            PackageManager.NameNotFoundException {
        mAppInfoList.clear();
        mAppInfoList.add(new AppInfo.Builder()
                .setUid(UNRESTRICTED_UID)
                .setPackageName(UNRESTRICTED_PACKAGE_NAME)
                .build());
        doReturn(new ArrayList<>()).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), eq(AnomalyDatabaseHelper.State.NEW));
        doReturn(mAppInfoList).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), eq(AnomalyDatabaseHelper.State.AUTO_HANDLED));
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(
                eq(UNRESTRICTED_PACKAGE_NAME), anyInt());

        final RestrictAppTip restrictAppTip = (RestrictAppTip) mRestrictAppDetector.detect();
        assertThat(restrictAppTip.getState()).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void testDetect_noAnomaly_tipInvisible() {
        doReturn(new ArrayList<AppInfo>()).when(mBatteryDatabaseManager)
                .queryAllAnomalies(anyLong(), anyInt());

        assertThat(mRestrictAppDetector.detect().getState())
                .isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void testUseFakeData_alwaysFalse() {
        assertThat(RestrictAppDetector.USE_FAKE_DATA).isFalse();
    }
}
