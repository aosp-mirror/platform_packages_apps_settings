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

package com.android.settings.fuelgauge.batteryusage.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.BatteryTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link BatteryStateDao}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryStateDaoTest {
    private static final int CURSOR_COLUMN_SIZE = 9;
    private static final long CURRENT = System.currentTimeMillis();
    private static final long TIMESTAMP1 = CURRENT;
    private static final long TIMESTAMP2 = CURRENT + 2;
    private static final long TIMESTAMP3 = CURRENT + 4;
    private static final String PACKAGE_NAME1 = "com.android.apps.settings";
    private static final String PACKAGE_NAME2 = "com.android.apps.calendar";
    private static final String PACKAGE_NAME3 = "com.android.apps.gmail";

    private Context mContext;
    private BatteryStateDatabase mDatabase;
    private BatteryStateDao mBatteryStateDao;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDatabase = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mBatteryStateDao = mDatabase.batteryStateDao();
        BatteryTestUtils.insertDataToBatteryStateTable(mContext, TIMESTAMP3, PACKAGE_NAME3);
        BatteryTestUtils.insertDataToBatteryStateTable(mContext, TIMESTAMP2, PACKAGE_NAME2);
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext,
                TIMESTAMP1,
                PACKAGE_NAME1,
                /* multiple= */ true,
                /* isFullChargeStart= */ true);
    }

    @After
    public void closeDb() {
        mDatabase.close();
        BatteryStateDatabase.setBatteryStateDatabase(/* database= */ null);
    }

    @Test
    public void insertAll_normalFlow_expectedBehavior() throws Exception {
        final List<BatteryState> states = mBatteryStateDao.getAllAfter(TIMESTAMP1);
        assertThat(states).hasSize(2);
        // Verifies the queried battery states.
        assertBatteryState(states.get(0), TIMESTAMP3, PACKAGE_NAME3);
        assertBatteryState(states.get(1), TIMESTAMP2, PACKAGE_NAME2);
    }

    @Test
    public void getLatestTimestamp_normalFlow_expectedBehavior() throws Exception {
        final Cursor cursor1 = mBatteryStateDao.getLatestTimestampBefore(TIMESTAMP1 - 1);
        assertThat(cursor1.getCount()).isEqualTo(1);
        cursor1.moveToFirst();
        assertThat(cursor1.getLong(0)).isEqualTo(0L);

        final Cursor cursor2 = mBatteryStateDao.getLatestTimestampBefore(TIMESTAMP2);
        assertThat(cursor2.getCount()).isEqualTo(1);
        cursor2.moveToFirst();
        assertThat(cursor2.getLong(0)).isEqualTo(TIMESTAMP2);

        final Cursor cursor3 = mBatteryStateDao.getLatestTimestampBefore(TIMESTAMP3 + 1);
        assertThat(cursor3.getCount()).isEqualTo(1);
        cursor3.moveToFirst();
        assertThat(cursor3.getLong(0)).isEqualTo(TIMESTAMP3);
    }

    @Test
    public void getBatteryStatesAfter_normalFlow_expectedBehavior() throws Exception {
        final Cursor cursor1 = mBatteryStateDao.getBatteryStatesAfter(TIMESTAMP1);
        assertThat(cursor1.getCount()).isEqualTo(3);
        assertThat(cursor1.getColumnCount()).isEqualTo(CURSOR_COLUMN_SIZE);
        // Verifies the queried first battery state.
        cursor1.moveToFirst();
        assertThat(cursor1.getString(3 /*packageName*/)).isEqualTo(PACKAGE_NAME1);
        // Verifies the queried second battery state.
        cursor1.moveToNext();
        assertThat(cursor1.getString(3 /*packageName*/)).isEqualTo(PACKAGE_NAME2);
        // Verifies the queried third battery state.
        cursor1.moveToNext();
        assertThat(cursor1.getString(3 /*packageName*/)).isEqualTo(PACKAGE_NAME3);

        final Cursor cursor2 = mBatteryStateDao.getBatteryStatesAfter(TIMESTAMP3);
        assertThat(cursor2.getCount()).isEqualTo(1);
        assertThat(cursor2.getColumnCount()).isEqualTo(CURSOR_COLUMN_SIZE);
        // Verifies the queried first battery state.
        cursor2.moveToFirst();
        assertThat(cursor2.getString(3 /*packageName*/)).isEqualTo(PACKAGE_NAME3);
    }

    @Test
    public void clearAllBefore_normalFlow_expectedBehavior() throws Exception {
        mBatteryStateDao.clearAllBefore(TIMESTAMP2);

        final List<BatteryState> states = mBatteryStateDao.getAllAfter(0);
        assertThat(states).hasSize(1);
        // Verifies the queried battery state.
        assertBatteryState(states.get(0), TIMESTAMP3, PACKAGE_NAME3);
    }

    @Test
    public void clearAll_normalFlow_expectedBehavior() throws Exception {
        assertThat(mBatteryStateDao.getAllAfter(0)).hasSize(3);
        mBatteryStateDao.clearAll();
        assertThat(mBatteryStateDao.getAllAfter(0)).isEmpty();
    }

    @Test
    public void getInstance_createNewInstance_returnsExpectedResult() throws Exception {
        BatteryStateDatabase.setBatteryStateDatabase(/* database= */ null);
        assertThat(BatteryStateDatabase.getInstance(mContext)).isNotNull();
    }

    @Test
    public void getDistinctTimestampCount_normalFlow_returnsExpectedResult() {
        assertThat(mBatteryStateDao.getDistinctTimestampCount(/* timestamp= */ 0)).isEqualTo(3);
        assertThat(mBatteryStateDao.getDistinctTimestampCount(TIMESTAMP1)).isEqualTo(2);
    }

    @Test
    public void getDistinctTimestamps_normalFlow_returnsExpectedResult() {
        final List<Long> timestamps = mBatteryStateDao.getDistinctTimestamps(/* timestamp= */ 0);

        assertThat(timestamps).hasSize(3);
        assertThat(timestamps).containsExactly(TIMESTAMP1, TIMESTAMP2, TIMESTAMP3);
    }

    private static void assertBatteryState(BatteryState state, long timestamp, String packageName) {
        assertThat(state.timestamp).isEqualTo(timestamp);
        assertThat(state.packageName).isEqualTo(packageName);
    }
}
