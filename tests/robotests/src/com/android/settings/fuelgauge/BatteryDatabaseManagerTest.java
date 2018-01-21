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

import com.android.settings.TestConfig;
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
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BatteryDatabaseManagerTest {
    private static String PACKAGE_NAME_NEW = "com.android.app1";
    private static int TYPE_NEW = 1;
    private static String PACKAGE_NAME_OLD = "com.android.app2";
    private static int TYPE_OLD = 2;
    private static long NOW = System.currentTimeMillis();
    private static long ONE_DAY_BEFORE = NOW - DateUtils.DAY_IN_MILLIS;
    private static long TWO_DAYS_BEFORE = NOW - 2 * DateUtils.DAY_IN_MILLIS;
    private Context mContext;
    private BatteryDatabaseManager mBatteryDatabaseManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mBatteryDatabaseManager = spy(new BatteryDatabaseManager(mContext));
    }

    @After
    public void cleanUp() {
        DatabaseTestUtils.clearDb(mContext);
    }

    @Test
    public void testAllFunctions() {
        mBatteryDatabaseManager.insertAnomaly(PACKAGE_NAME_NEW, TYPE_NEW, NOW);
        mBatteryDatabaseManager.insertAnomaly(PACKAGE_NAME_OLD, TYPE_OLD, TWO_DAYS_BEFORE);

        // In database, it contains two record
        List<AppInfo> totalAppInfos = mBatteryDatabaseManager.queryAllAnomaliesAfter(0);
        assertThat(totalAppInfos).hasSize(2);
        verifyAppInfo(totalAppInfos.get(0), PACKAGE_NAME_NEW, TYPE_NEW);
        verifyAppInfo(totalAppInfos.get(1), PACKAGE_NAME_OLD, TYPE_OLD);

        // Only one record shows up if we query by timestamp
        List<AppInfo> appInfos = mBatteryDatabaseManager.queryAllAnomaliesAfter(ONE_DAY_BEFORE);
        assertThat(appInfos).hasSize(1);
        verifyAppInfo(appInfos.get(0), PACKAGE_NAME_NEW, TYPE_NEW);

        mBatteryDatabaseManager.deleteAllAnomaliesBeforeTimeStamp(ONE_DAY_BEFORE);

        // The obsolete record is removed from database
        List<AppInfo> appInfos1 = mBatteryDatabaseManager.queryAllAnomaliesAfter(0);
        assertThat(appInfos1).hasSize(1);
        verifyAppInfo(appInfos1.get(0), PACKAGE_NAME_NEW, TYPE_NEW);

    }

    private void verifyAppInfo(final AppInfo appInfo, String packageName, int type) {
        assertThat(appInfo.packageName).isEqualTo(packageName);
        assertThat(appInfo.anomalyType).isEqualTo(type);
    }

}
