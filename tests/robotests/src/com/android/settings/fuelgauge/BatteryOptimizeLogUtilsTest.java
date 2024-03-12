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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.PrintWriter;
import java.io.StringWriter;

@RunWith(RobolectricTestRunner.class)
public final class BatteryOptimizeLogUtilsTest {

    private final StringWriter mTestStringWriter = new StringWriter();
    private final PrintWriter mTestPrintWriter = new PrintWriter(mTestStringWriter);

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        BatteryOptimizeLogUtils.getSharedPreferences(mContext).edit().clear().commit();
    }

    @Test
    public void printHistoricalLog_withDefaultLogs() {
        BatteryOptimizeLogUtils.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);
        assertThat(mTestStringWriter.toString()).contains("nothing to dump");
    }

    @Test
    public void writeLog_withExpectedLogs() {
        BatteryOptimizeLogUtils.writeLog(mContext, Action.APPLY, "pkg1", "logs");
        BatteryOptimizeLogUtils.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);

        assertThat(mTestStringWriter.toString()).contains("pkg1\taction:APPLY\tevent:logs");
    }

    @Test
    public void writeLog_multipleLogs_withCorrectCounts() {
        final int expectedCount = 10;
        for (int i = 0; i < expectedCount; i++) {
            BatteryOptimizeLogUtils.writeLog(mContext, Action.LEAVE, "pkg" + i, "logs");
        }
        BatteryOptimizeLogUtils.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);

        assertActionCount("LEAVE", expectedCount);
    }

    @Test
    public void writeLog_overMaxEntriesLogs_withCorrectCounts() {
        for (int i = 0; i < BatteryOptimizeLogUtils.MAX_ENTRIES + 10; i++) {
            BatteryOptimizeLogUtils.writeLog(mContext, Action.RESET, "pkg" + i, "logs");
        }
        BatteryOptimizeLogUtils.printBatteryOptimizeHistoricalLog(mContext, mTestPrintWriter);

        assertActionCount("RESET", BatteryOptimizeLogUtils.MAX_ENTRIES);
    }

    private void assertActionCount(String token, int count) {
        final String dumpResults = mTestStringWriter.toString();
        assertThat(dumpResults.split(token).length).isEqualTo(count + 1);
    }
}
