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

import com.android.settings.testutils.BatteryTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Tests of {@link BugReportContentProvider}. */
@RunWith(RobolectricTestRunner.class)
public final class BugReportContentProviderTest {
    private static final String PACKAGE_NAME1 = "com.android.settings1";
    private static final String PACKAGE_NAME2 = "com.android.settings2";
    private static final String PACKAGE_NAME3 = "com.android.settings3";
    private static final String PACKAGE_NAME4 = "com.android.settings4";

    private Context mContext;
    private PrintWriter mPrintWriter;
    private StringWriter mStringWriter;
    private BugReportContentProvider mBugReportContentProvider;

    @Before
    public void setUp() {
        mStringWriter = new StringWriter();
        mPrintWriter = new PrintWriter(mStringWriter);
        mContext = ApplicationProvider.getApplicationContext();
        mBugReportContentProvider = new BugReportContentProvider();
        mBugReportContentProvider.attachInfo(mContext, /* info= */ null);
        // Inserts fake data into database for testing.
        BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, System.currentTimeMillis(), PACKAGE_NAME1);
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, System.currentTimeMillis(), PACKAGE_NAME2);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, /* userId= */ 1, System.currentTimeMillis(), PACKAGE_NAME3);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, /* userId= */ 1, System.currentTimeMillis(), PACKAGE_NAME4);
    }

    @Test
    public void dump_nullContext_notDumpsBatteryUsageData() {
        mBugReportContentProvider = new BugReportContentProvider();
        mBugReportContentProvider.attachInfo(/* context= */ null, /* info= */ null);

        mBugReportContentProvider.dump(FileDescriptor.out, mPrintWriter, new String[] {});

        assertThat(mStringWriter.toString()).isEmpty();
    }

    @Test
    public void dump_inWorkProfileMode_notDumpsBatteryUsageData() {
        BatteryTestUtils.setWorkProfile(mContext);
        mBugReportContentProvider.dump(FileDescriptor.out, mPrintWriter, new String[] {});
        assertThat(mStringWriter.toString()).isEmpty();
    }

    @Test
    public void dump_dumpsBatteryUsageHistory() {
        mBugReportContentProvider.dump(FileDescriptor.out, mPrintWriter, new String[] {});

        String dumpContent = mStringWriter.toString();
        assertThat(dumpContent).contains("Battery PeriodicJob History");
        assertThat(dumpContent).contains("Battery DatabaseHistory");
        assertThat(dumpContent).contains(PACKAGE_NAME1);
        assertThat(dumpContent).contains(PACKAGE_NAME2);
        assertThat(dumpContent).contains("distinct timestamp count:2");
        assertThat(dumpContent).contains("App DatabaseHistory");
        assertThat(dumpContent).contains(PACKAGE_NAME3);
        assertThat(dumpContent).contains(PACKAGE_NAME4);
    }
}
