/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.bugreport;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.fuelgauge.batteryusage.BatteryReattribute;
import com.android.settings.fuelgauge.batteryusage.db.BatteryReattributeDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryReattributeEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;

import java.io.PrintWriter;
import java.io.StringWriter;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner.class)
public final class LogUtilsTest {

    private StringWriter mTestStringWriter;
    private PrintWriter mTestPrintWriter;
    private Context mContext;
    private BatteryStateDatabase mDatabase;
    private BatteryReattributeDao mBatteryReattributeDao;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTestStringWriter = new StringWriter();
        mTestPrintWriter = new PrintWriter(mTestStringWriter);
        mDatabase = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mBatteryReattributeDao = mDatabase.batteryReattributeDao();
        mPowerUsageFeatureProvider = FakeFeatureFactory.setupForTest().powerUsageFeatureProvider;
        when(mPowerUsageFeatureProvider.isBatteryUsageReattributeEnabled()).thenReturn(true);
    }

    @After
    public void cleanUp() {
        mBatteryReattributeDao.clearAll();
    }

    @Test
    public void dumpBatteryReattributeDatabaseHist_noData_printExpectedResult() {
        LogUtils.dumpBatteryReattributeDatabaseHist(mBatteryReattributeDao, mTestPrintWriter);

        assertThat(mTestStringWriter.toString())
                .contains("BatteryReattribute DatabaseHistory:");
    }

    @Test
    public void dumpBatteryReattributeDatabaseHist_printExpectedResult() {
        final long currentTimeMillis = System.currentTimeMillis();
        // Insert the first testing data.
        final BatteryReattribute batteryReattribute1 =
                BatteryReattribute.newBuilder()
                        .setTimestampStart(currentTimeMillis - 20000)
                        .setTimestampEnd(currentTimeMillis - 10000)
                        .putReattributeData(1001, 0.1f)
                        .putReattributeData(1002, 0.99f)
                        .build();
        mBatteryReattributeDao.insert(new BatteryReattributeEntity(batteryReattribute1));
        // Insert the second testing data.
        final BatteryReattribute batteryReattribute2 =
                BatteryReattribute.newBuilder()
                        .setTimestampStart(currentTimeMillis - 40000)
                        .setTimestampEnd(currentTimeMillis - 20000)
                        .putReattributeData(1003, 1f)
                        .build();
        mBatteryReattributeDao.insert(new BatteryReattributeEntity(batteryReattribute2));

        LogUtils.dumpBatteryReattributeDatabaseHist(mBatteryReattributeDao, mTestPrintWriter);

        final String result = mTestStringWriter.toString();
        assertThat(result).contains("BatteryReattribute DatabaseHistory:");
        assertThat(result).contains(batteryReattribute1.toString());
        assertThat(result).contains(batteryReattribute2.toString());
    }

    @Test
    public void dumpBatteryReattributeDatabaseHist_featureDisable_notPrintData() {
        mBatteryReattributeDao.insert(new BatteryReattributeEntity(
                BatteryReattribute.getDefaultInstance()));
        when(mPowerUsageFeatureProvider.isBatteryUsageReattributeEnabled()).thenReturn(false);

        LogUtils.dumpBatteryReattributeDatabaseHist(mBatteryReattributeDao, mTestPrintWriter);

        final String result = mTestStringWriter.toString();
        assertThat(result).contains("BatteryReattribute is disabled!");
        assertThat(result.contains("BatteryReattribute DatabaseHistory:")).isFalse();
    }
}
