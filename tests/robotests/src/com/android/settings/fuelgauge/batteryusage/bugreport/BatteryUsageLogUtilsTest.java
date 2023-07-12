/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.BatteryUsageHistoricalLogEntry.Action;

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
public final class BatteryUsageLogUtilsTest {

    private StringWriter mTestStringWriter;
    private PrintWriter mTestPrintWriter;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTestStringWriter = new StringWriter();
        mTestPrintWriter = new PrintWriter(mTestStringWriter);
        BatteryUsageLogUtils.getSharedPreferences(mContext).edit().clear().commit();
    }

    @Test
    public void printHistoricalLog_withDefaultLogs() {
        final String expectedInformation = "nothing to dump";
        // Environment checking.
        assertThat(mTestStringWriter.toString().contains(expectedInformation)).isFalse();

        BatteryUsageLogUtils.printHistoricalLog(mContext, mTestPrintWriter);
        assertThat(mTestStringWriter.toString()).contains(expectedInformation);
    }

    @Test
    public void writeLog_multipleLogs_withCorrectCounts() {
        final int expectedCount = 10;
        for (int i = 0; i < expectedCount; i++) {
            BatteryUsageLogUtils.writeLog(mContext, Action.SCHEDULE_JOB, "");
        }
        BatteryUsageLogUtils.writeLog(mContext, Action.EXECUTE_JOB, "");

        BatteryUsageLogUtils.printHistoricalLog(mContext, mTestPrintWriter);

        assertActionCount("SCHEDULE_JOB", expectedCount);
        assertActionCount("EXECUTE_JOB", 1);
    }

    @Test
    public void writeLog_overMaxEntriesLogs_withCorrectCounts() {
        BatteryUsageLogUtils.writeLog(mContext, Action.SCHEDULE_JOB, "");
        BatteryUsageLogUtils.writeLog(mContext, Action.SCHEDULE_JOB, "");
        for (int i = 0; i < BatteryUsageLogUtils.MAX_ENTRIES * 2; i++) {
            BatteryUsageLogUtils.writeLog(mContext, Action.EXECUTE_JOB, "");
        }

        BatteryUsageLogUtils.printHistoricalLog(mContext, mTestPrintWriter);

        final String dumpResults = mTestStringWriter.toString();
        assertThat(dumpResults.contains("SCHEDULE_JOB")).isFalse();
        assertActionCount("EXECUTE_JOB", BatteryUsageLogUtils.MAX_ENTRIES);
    }

    private void assertActionCount(String token, int count) {
        final String dumpResults = mTestStringWriter.toString();
        assertThat(dumpResults.split(token).length).isEqualTo(count + 1);
    }
}
