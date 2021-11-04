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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.SparseLongArray;

import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.testutils.DatabaseTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BatteryDatabaseManagerTest {
    private static String PACKAGE_NAME_NEW = "com.android.app1";
    private static int UID_NEW = 345;
    private static int TYPE_NEW = 1;
    private static String PACKAGE_NAME_OLD = "com.android.app2";
    private static int UID_OLD = 543;
    private static int TYPE_OLD = 2;
    private static long NOW = System.currentTimeMillis();
    private static long ONE_DAY_BEFORE = NOW - DateUtils.DAY_IN_MILLIS;
    private static long TWO_DAYS_BEFORE = NOW - 2 * DateUtils.DAY_IN_MILLIS;

    private Context mContext;
    private BatteryDatabaseManager mBatteryDatabaseManager;
    private AppInfo mNewAppInfo;
    private AppInfo mOldAppInfo;
    private AppInfo mCombinedAppInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mBatteryDatabaseManager = spy(BatteryDatabaseManager.getInstance(mContext));

        mNewAppInfo = new AppInfo.Builder()
                .setUid(UID_NEW)
                .setPackageName(PACKAGE_NAME_NEW)
                .addAnomalyType(TYPE_NEW)
                .build();
        mOldAppInfo = new AppInfo.Builder()
                .setUid(UID_OLD)
                .setPackageName(PACKAGE_NAME_OLD)
                .addAnomalyType(TYPE_OLD)
                .build();
        mCombinedAppInfo = new AppInfo.Builder()
                .setUid(UID_NEW)
                .setPackageName(PACKAGE_NAME_NEW)
                .addAnomalyType(TYPE_NEW)
                .addAnomalyType(TYPE_OLD)
                .build();
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void allAnomalyFunctions() {
        mBatteryDatabaseManager.insertAnomaly(UID_NEW, PACKAGE_NAME_NEW, TYPE_NEW,
                AnomalyDatabaseHelper.State.NEW, NOW);
        mBatteryDatabaseManager.insertAnomaly(UID_OLD, PACKAGE_NAME_OLD, TYPE_OLD,
                AnomalyDatabaseHelper.State.NEW, TWO_DAYS_BEFORE);

        // In database, it contains two record
        List<AppInfo> totalAppInfos = mBatteryDatabaseManager.queryAllAnomalies(0 /* timeMsAfter */,
                AnomalyDatabaseHelper.State.NEW);
        assertThat(totalAppInfos).containsExactly(mNewAppInfo, mOldAppInfo);

        // Only one record shows up if we query by timestamp
        List<AppInfo> appInfos = mBatteryDatabaseManager.queryAllAnomalies(ONE_DAY_BEFORE,
                AnomalyDatabaseHelper.State.NEW);
        assertThat(appInfos).containsExactly(mNewAppInfo);

        mBatteryDatabaseManager.deleteAllAnomaliesBeforeTimeStamp(ONE_DAY_BEFORE);

        // The obsolete record is removed from database
        List<AppInfo> appInfos1 = mBatteryDatabaseManager.queryAllAnomalies(0 /* timeMsAfter */,
                AnomalyDatabaseHelper.State.NEW);
        assertThat(appInfos1).containsExactly(mNewAppInfo);
    }

    @Test
    public void updateAnomalies_updateSuccessfully() {
        mBatteryDatabaseManager.insertAnomaly(UID_NEW, PACKAGE_NAME_NEW, TYPE_NEW,
                AnomalyDatabaseHelper.State.NEW, NOW);
        mBatteryDatabaseManager.insertAnomaly(UID_OLD, PACKAGE_NAME_OLD, TYPE_OLD,
                AnomalyDatabaseHelper.State.NEW, NOW);
        final AppInfo appInfo = new AppInfo.Builder().setPackageName(PACKAGE_NAME_OLD).build();
        final List<AppInfo> updateAppInfos = new ArrayList<>();
        updateAppInfos.add(appInfo);

        // Change state of PACKAGE_NAME_OLD to handled
        mBatteryDatabaseManager.updateAnomalies(updateAppInfos,
                AnomalyDatabaseHelper.State.HANDLED);

        // The state of PACKAGE_NAME_NEW is still new
        List<AppInfo> newAppInfos = mBatteryDatabaseManager.queryAllAnomalies(ONE_DAY_BEFORE,
                AnomalyDatabaseHelper.State.NEW);
        assertThat(newAppInfos).containsExactly(mNewAppInfo);

        // The state of PACKAGE_NAME_OLD is changed to handled
        List<AppInfo> handledAppInfos = mBatteryDatabaseManager.queryAllAnomalies(ONE_DAY_BEFORE,
                AnomalyDatabaseHelper.State.HANDLED);
        assertThat(handledAppInfos).containsExactly(mOldAppInfo);
    }

    @Test
    public void queryAnomalies_removeDuplicateByUid() {
        mBatteryDatabaseManager.insertAnomaly(UID_NEW, PACKAGE_NAME_NEW, TYPE_NEW,
                AnomalyDatabaseHelper.State.NEW, NOW);
        mBatteryDatabaseManager.insertAnomaly(UID_NEW, PACKAGE_NAME_NEW, TYPE_OLD,
                AnomalyDatabaseHelper.State.NEW, NOW);

        // Only contain one AppInfo with multiple types
        List<AppInfo> newAppInfos = mBatteryDatabaseManager.queryAllAnomalies(ONE_DAY_BEFORE,
                AnomalyDatabaseHelper.State.NEW);
        assertThat(newAppInfos).containsExactly(mCombinedAppInfo);
    }

    @Test
    public void allActionFunctions() {
        final long timestamp = System.currentTimeMillis();
        mBatteryDatabaseManager.insertAction(AnomalyDatabaseHelper.ActionType.RESTRICTION, UID_OLD,
                PACKAGE_NAME_OLD, 0);
        mBatteryDatabaseManager.insertAction(AnomalyDatabaseHelper.ActionType.RESTRICTION, UID_OLD,
                PACKAGE_NAME_OLD, 1);
        mBatteryDatabaseManager.insertAction(AnomalyDatabaseHelper.ActionType.RESTRICTION, UID_NEW,
                PACKAGE_NAME_NEW, timestamp);

        final SparseLongArray timeArray = mBatteryDatabaseManager.queryActionTime(
                AnomalyDatabaseHelper.ActionType.RESTRICTION);
        assertThat(timeArray.size()).isEqualTo(2);
        assertThat(timeArray.get(UID_OLD)).isEqualTo(1);
        assertThat(timeArray.get(UID_NEW)).isEqualTo(timestamp);

        mBatteryDatabaseManager.deleteAction(AnomalyDatabaseHelper.ActionType.RESTRICTION, UID_NEW,
                PACKAGE_NAME_NEW);
        final SparseLongArray recentTimeArray = mBatteryDatabaseManager.queryActionTime(
                AnomalyDatabaseHelper.ActionType.RESTRICTION);
        assertThat(recentTimeArray.size()).isEqualTo(1);
        assertThat(timeArray.get(UID_OLD)).isEqualTo(1);
    }
}
