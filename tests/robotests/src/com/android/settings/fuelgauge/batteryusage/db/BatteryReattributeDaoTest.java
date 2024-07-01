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

package com.android.settings.fuelgauge.batteryusage.db;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.BatteryTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link BatteryReattributeDao}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryReattributeDaoTest {

    private Context mContext;
    private BatteryStateDatabase mDatabase;
    private BatteryReattributeDao mBatteryReattributeDao;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDatabase = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mBatteryReattributeDao = mDatabase.batteryReattributeDao();
        insert(100L, 200L, "reattributeData1");
        insert(300L, 400L, "reattributeData3");
        insert(200L, 300L, "reattributeData2");
        insert(400L, 500L, "reattributeData4");
        // Ensure there was data inserted into the database.
        assertThat(getAllEntityData()).isNotEmpty();
    }

    @Test
    public void getAllAfter_returnExpectedEntityData() {
        final List<BatteryReattributeEntity> entityDataList =
            mBatteryReattributeDao.getAllAfter(/* timestampStart= */ 300L);

        assertThat(entityDataList).hasSize(2);
        assertEntity(entityDataList.get(0), 400L, 500L, "reattributeData4");
        assertEntity(entityDataList.get(1), 300L, 400L, "reattributeData3");
    }

    @Test
    public void clearAll_clearAllData() {
        mBatteryReattributeDao.clearAll();

        assertThat(getAllEntityData()).isEmpty();
    }

    @Test
    public void clearAllBefore_clearAllExpectedData() {
        mBatteryReattributeDao.clearAllBefore(/* timestampStart= */ 300L);

        final List<BatteryReattributeEntity> entityDataList = getAllEntityData();
        assertThat(entityDataList).hasSize(1);
        assertEntity(entityDataList.get(0), 400L, 500L, "reattributeData4");
    }

    @Test
    public void clearAllAfter_clearAllExpectedData() {
        mBatteryReattributeDao.clearAllAfter(/* timestampStart= */ 300L);

        final List<BatteryReattributeEntity> entityDataList = getAllEntityData();
        assertThat(entityDataList).hasSize(2);
        assertEntity(entityDataList.get(0), 200L, 300L, "reattributeData2");
        assertEntity(entityDataList.get(1), 100L, 200L, "reattributeData1");
    }

    @Test
    public void insert_samePrimaryKeyEntityData_replaceIntoNewEntityData() {
        // Verify the original data before update.
        assertEntity(getAllEntityData().get(0), 400L, 500L, "reattributeData4");

        insert(400L, 600L, "reattribute4Update");

        // Verify the new update entity data.
        assertEntity(getAllEntityData().get(0), 400L, 600L, "reattribute4Update");
    }

    private void insert(long timestampStart, long timestampEnd, String reattributeData) {
        mBatteryReattributeDao.insert(
                new BatteryReattributeEntity(
                        timestampStart, timestampEnd, reattributeData));
    }

    private List<BatteryReattributeEntity> getAllEntityData() {
        return mBatteryReattributeDao.getAllAfter(/* timestampStart= */ 0L);
    }

    private static void assertEntity(BatteryReattributeEntity entity, long timestampStart,
            long timestampEnd, String reattributeData) {
        assertThat(entity.timestampStart).isEqualTo(timestampStart);
        assertThat(entity.timestampEnd).isEqualTo(timestampEnd);
        assertThat(entity.reattributeData).isEqualTo(reattributeData);
    }
}
