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

/** Tests for {@link BatteryEventDao}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryEventDaoTest {
    private Context mContext;
    private BatteryStateDatabase mDatabase;
    private BatteryEventDao mBatteryEventDao;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDatabase = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mBatteryEventDao = mDatabase.batteryEventDao();
    }

    @After
    public void closeDb() {
        mDatabase.close();
        BatteryStateDatabase.setBatteryStateDatabase(/*database=*/ null);
    }

    @Test
    public void getAllAfter_returnExpectedResult() {
        mBatteryEventDao.insert(BatteryEventEntity.newBuilder()
                .setTimestamp(100L)
                .setBatteryEventType(1)
                .setBatteryLevel(66)
                .build());
        mBatteryEventDao.insert(BatteryEventEntity.newBuilder()
                .setTimestamp(200L)
                .setBatteryEventType(2)
                .setBatteryLevel(88)
                .build());

        final Cursor cursor = mBatteryEventDao.getAllAfter(160L);
        assertThat(cursor.getCount()).isEqualTo(1);
        cursor.moveToFirst();
        assertThat(cursor.getLong(cursor.getColumnIndex(BatteryEventEntity.KEY_TIMESTAMP)))
                .isEqualTo(200L);
        assertThat(cursor.getInt(cursor.getColumnIndex(BatteryEventEntity.KEY_BATTERY_EVENT_TYPE)))
                .isEqualTo(2);
        assertThat(cursor.getInt(cursor.getColumnIndex(BatteryEventEntity.KEY_BATTERY_LEVEL)))
                .isEqualTo(88);

        mBatteryEventDao.clearAll();
        assertThat(mBatteryEventDao.getAll()).isEmpty();
    }
}
