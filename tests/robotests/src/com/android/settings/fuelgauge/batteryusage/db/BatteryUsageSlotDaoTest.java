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

/** Tests for {@link BatteryUsageSlotDao}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryUsageSlotDaoTest {
    private static final int CURSOR_COLUMN_SIZE = 3;
    private static final long CURRENT = System.currentTimeMillis();
    private static final long TIMESTAMP1 = CURRENT;
    private static final long TIMESTAMP2 = CURRENT + 2;
    private static final String BATTERY_USAGE_SLOT_STRING1 = "BATTERY_USAGE_SLOT_STRING1";
    private static final String BATTERY_USAGE_SLOT_STRING2 = "BATTERY_USAGE_SLOT_STRING2";

    private Context mContext;
    private BatteryStateDatabase mDatabase;
    private BatteryUsageSlotDao mBatteryUsageSlotDao;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDatabase = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mBatteryUsageSlotDao = mDatabase.batteryUsageSlotDao();
        mBatteryUsageSlotDao.insert(
                new BatteryUsageSlotEntity(TIMESTAMP1, BATTERY_USAGE_SLOT_STRING1));
        mBatteryUsageSlotDao.insert(
                new BatteryUsageSlotEntity(TIMESTAMP2, BATTERY_USAGE_SLOT_STRING2));
    }

    @After
    public void closeDb() {
        mDatabase.close();
        BatteryStateDatabase.setBatteryStateDatabase(/* database= */ null);
    }

    @Test
    public void getAll_normalFlow_expectedBehavior() throws Exception {
        final List<BatteryUsageSlotEntity> entities = mBatteryUsageSlotDao.getAll();
        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).timestamp).isEqualTo(TIMESTAMP1);
        assertThat(entities.get(0).batteryUsageSlot).isEqualTo(BATTERY_USAGE_SLOT_STRING1);
        assertThat(entities.get(1).timestamp).isEqualTo(TIMESTAMP2);
        assertThat(entities.get(1).batteryUsageSlot).isEqualTo(BATTERY_USAGE_SLOT_STRING2);
    }

    @Test
    public void getAllAfter_normalFlow_expectedBehavior() throws Exception {
        final Cursor cursor1 = mBatteryUsageSlotDao.getAllAfter(TIMESTAMP1);
        assertThat(cursor1.getCount()).isEqualTo(2);
        assertThat(cursor1.getColumnCount()).isEqualTo(CURSOR_COLUMN_SIZE);
        cursor1.moveToFirst();
        assertThat(cursor1.getLong(1 /*timestamp*/)).isEqualTo(TIMESTAMP1);
        cursor1.moveToNext();
        assertThat(cursor1.getLong(1 /*timestamp*/)).isEqualTo(TIMESTAMP2);

        final Cursor cursor2 = mBatteryUsageSlotDao.getAllAfter(TIMESTAMP1 + 1);
        assertThat(cursor2.getCount()).isEqualTo(1);
        assertThat(cursor2.getColumnCount()).isEqualTo(CURSOR_COLUMN_SIZE);
        cursor2.moveToFirst();
        assertThat(cursor2.getLong(1 /*timestamp*/)).isEqualTo(TIMESTAMP2);
    }

    @Test
    public void clearAllBefore_normalFlow_expectedBehavior() throws Exception {
        mBatteryUsageSlotDao.clearAllBefore(TIMESTAMP1);

        final List<BatteryUsageSlotEntity> entities = mBatteryUsageSlotDao.getAll();
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).timestamp).isEqualTo(TIMESTAMP2);
        assertThat(entities.get(0).batteryUsageSlot).isEqualTo(BATTERY_USAGE_SLOT_STRING2);
    }

    @Test
    public void clearAll_normalFlow_expectedBehavior() throws Exception {
        mBatteryUsageSlotDao.clearAll();

        assertThat(mBatteryUsageSlotDao.getAll()).isEmpty();
    }
}
