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

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link AppUsageEventDao}. */
@RunWith(RobolectricTestRunner.class)
public final class AppUsageEventDaoTest {
    private static final int CURSOR_COLUMN_SIZE = 8;
    private static final long TIMESTAMP1 = System.currentTimeMillis();
    private static final long TIMESTAMP2 = System.currentTimeMillis() + 2;
    private static final long TIMESTAMP3 = System.currentTimeMillis() + 4;
    private static final long USER_ID1 = 1;
    private static final long USER_ID2 = 2;
    private static final String PACKAGE_NAME1 = "com.android.apps.settings";
    private static final String PACKAGE_NAME2 = "com.android.apps.calendar";
    private static final String PACKAGE_NAME3 = "com.android.apps.gmail";

    private Context mContext;
    private BatteryStateDatabase mDatabase;
    private AppUsageEventDao mAppUsageEventDao;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDatabase = BatteryTestUtils.setUpBatteryStateDatabase(mContext);
        mAppUsageEventDao = mDatabase.appUsageEventDao();
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID1, TIMESTAMP3, PACKAGE_NAME3);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID2, TIMESTAMP2, PACKAGE_NAME2);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID1, TIMESTAMP1, PACKAGE_NAME1, /* multiple= */ true);
    }

    @After
    public void closeDb() {
        mDatabase.close();
        BatteryStateDatabase.setBatteryStateDatabase(/* database= */ null);
    }

    @Test
    public void appUsageEventDao_insertAll() throws Exception {
        final List<AppUsageEventEntity> entities = mAppUsageEventDao.getAllAfter(TIMESTAMP1);
        assertThat(entities).hasSize(2);
        // Verifies the queried battery states.
        assertAppUsageEvent(entities.get(0), TIMESTAMP3, PACKAGE_NAME3);
        assertAppUsageEvent(entities.get(1), TIMESTAMP2, PACKAGE_NAME2);
    }

    @Test
    public void appUsageEventDao_getAllForUsersAfter() {
        final List<Long> userIds1 = new ArrayList<>();
        final long notExistingUserId = 3;
        userIds1.add(USER_ID1);
        userIds1.add(USER_ID2);
        userIds1.add(notExistingUserId);
        final Cursor cursor1 = mAppUsageEventDao.getAllForUsersAfter(userIds1, TIMESTAMP1);
        assertThat(cursor1.getCount()).isEqualTo(3);
        assertThat(cursor1.getColumnCount()).isEqualTo(CURSOR_COLUMN_SIZE);
        // Verifies the queried first battery state.
        cursor1.moveToFirst();
        assertThat(cursor1.getString(5 /*packageName*/)).isEqualTo(PACKAGE_NAME1);
        // Verifies the queried second battery state.
        cursor1.moveToNext();
        assertThat(cursor1.getString(5 /*packageName*/)).isEqualTo(PACKAGE_NAME2);
        // Verifies the queried third battery state.
        cursor1.moveToNext();
        assertThat(cursor1.getString(5 /*packageName*/)).isEqualTo(PACKAGE_NAME3);

        final List<Long> userIds2 = new ArrayList<>();
        userIds2.add(USER_ID1);
        final Cursor cursor2 = mAppUsageEventDao.getAllForUsersAfter(userIds2, TIMESTAMP3);
        assertThat(cursor2.getCount()).isEqualTo(1);
        assertThat(cursor2.getColumnCount()).isEqualTo(CURSOR_COLUMN_SIZE);
        // Verifies the queried first battery state.
        cursor2.moveToFirst();
        assertThat(cursor2.getString(5 /*packageName*/)).isEqualTo(PACKAGE_NAME3);
    }

    @Test
    public void appUsageEventDao_getLatestTimestampOfUser() throws Exception {
        final Cursor cursor1 = mAppUsageEventDao.getLatestTimestampOfUser(USER_ID1);
        assertThat(cursor1.getCount()).isEqualTo(1);
        cursor1.moveToFirst();
        assertThat(cursor1.getLong(0)).isEqualTo(TIMESTAMP3);

        final Cursor cursor2 = mAppUsageEventDao.getLatestTimestampOfUser(USER_ID2);
        assertThat(cursor2.getCount()).isEqualTo(1);
        cursor2.moveToFirst();
        assertThat(cursor2.getLong(0)).isEqualTo(TIMESTAMP2);

        final long notExistingUserId = 3;
        final Cursor cursor3 = mAppUsageEventDao.getLatestTimestampOfUser(notExistingUserId);
        assertThat(cursor3.getCount()).isEqualTo(1);
        cursor3.moveToFirst();
        assertThat(cursor3.getLong(0)).isEqualTo(0);
    }

    @Test
    public void appUsageEventDao_clearAllBefore() throws Exception {
        mAppUsageEventDao.clearAllBefore(TIMESTAMP2);

        final List<AppUsageEventEntity> entities = mAppUsageEventDao.getAllAfter(0);
        assertThat(entities).hasSize(1);
        // Verifies the queried battery state.
        assertAppUsageEvent(entities.get(0), TIMESTAMP3, PACKAGE_NAME3);
    }

    @Test
    public void appUsageEventDao_clearAll() throws Exception {
        assertThat(mAppUsageEventDao.getAllAfter(0)).hasSize(3);
        mAppUsageEventDao.clearAll();
        assertThat(mAppUsageEventDao.getAllAfter(0)).isEmpty();
    }

    @Test
    public void getInstance_createNewInstance() throws Exception {
        BatteryStateDatabase.setBatteryStateDatabase(/* database= */ null);
        assertThat(BatteryStateDatabase.getInstance(mContext)).isNotNull();
    }

    private static void assertAppUsageEvent(
            AppUsageEventEntity entity, long timestamp, String packageName) {
        assertThat(entity.timestamp).isEqualTo(timestamp);
        assertThat(entity.packageName).isEqualTo(packageName);
    }
}
