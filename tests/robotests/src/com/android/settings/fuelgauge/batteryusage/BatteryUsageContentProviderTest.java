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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.fuelgauge.batteryusage.db.BatteryUsageSlotEntity;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Tests for {@link BatteryUsageContentProvider}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryUsageContentProviderTest {
    private static final Uri VALID_BATTERY_STATE_CONTENT_URI = DatabaseUtils.BATTERY_CONTENT_URI;
    private static final long TIMESTAMP1 = System.currentTimeMillis();
    private static final long TIMESTAMP2 = System.currentTimeMillis() + 2;
    private static final long TIMESTAMP3 = System.currentTimeMillis() + 4;
    private static final String PACKAGE_NAME1 = "com.android.settings1";
    private static final String PACKAGE_NAME2 = "com.android.settings2";
    private static final String PACKAGE_NAME3 = "com.android.settings3";
    private static final long USER_ID1 = 1;
    private static final long USER_ID2 = 2;

    private Context mContext;
    private BatteryUsageContentProvider mProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mProvider = new BatteryUsageContentProvider();
        mProvider.attachInfo(mContext, /* info= */ null);
        BatteryTestUtils.setUpBatteryStateDatabase(mContext);
    }

    @Test
    public void onCreate_withoutWorkProfileMode_returnsTrue() {
        assertThat(mProvider.onCreate()).isTrue();
    }

    @Test
    public void onCreate_withWorkProfileMode_returnsFalse() {
        BatteryTestUtils.setWorkProfile(mContext);
        assertThat(mProvider.onCreate()).isFalse();
    }

    @Test
    public void queryAndInsert_incorrectContentUri_throwsIllegalArgumentException() {
        final Uri.Builder builder =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_STATE_TABLE + "/0");
        final Uri uri = builder.build();
        mProvider.onCreate();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mProvider.query(
                                uri,
                                /* strings= */ null,
                                /* s= */ null,
                                /* strings1= */ null,
                                /* s1= */ null));
        assertThrows(
                IllegalArgumentException.class,
                () -> mProvider.insert(uri, /* contentValues= */ null));
    }

    @Test
    public void queryAndInsert_incorrectAuthority_throwsIllegalArgumentException() {
        final Uri.Builder builder =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY + ".debug")
                        .appendPath(DatabaseUtils.BATTERY_STATE_TABLE);
        final Uri uri = builder.build();
        mProvider.onCreate();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mProvider.query(
                                uri,
                                /* strings= */ null,
                                /* s= */ null,
                                /* strings1= */ null,
                                /* s1= */ null));
        assertThrows(
                IllegalArgumentException.class,
                () -> mProvider.insert(uri, /* contentValues= */ null));
    }

    @Test
    public void query_getLastFullChargeTimestamp_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        ContentValues values = new ContentValues();
        values.put(BatteryEventEntity.KEY_TIMESTAMP, 10001L);
        values.put(
                BatteryEventEntity.KEY_BATTERY_EVENT_TYPE,
                BatteryEventType.FULL_CHARGED.getNumber());
        values.put(BatteryEventEntity.KEY_BATTERY_LEVEL, 100);
        mProvider.insert(DatabaseUtils.BATTERY_EVENT_URI, values);

        final Cursor cursor = getCursorOfLastFullChargeTimestamp();

        assertThat(cursor.getCount()).isEqualTo(1);
        cursor.moveToFirst();
        final long lastFullChargeTimestamp = cursor.getLong(0);
        assertThat(lastFullChargeTimestamp).isEqualTo(10001L);
    }

    @Test
    public void query_batteryState_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        final long expiredTimeCutoff = currentTime.toMillis() - 8;

        final Cursor cursor = insertBatteryState(currentTime, Long.toString(expiredTimeCutoff));

        // Verifies the result not include expired data.
        assertThat(cursor.getCount()).isEqualTo(3);
        final int packageNameIndex = cursor.getColumnIndex("packageName");
        // Verifies the first data package name.
        cursor.moveToFirst();
        final String actualPackageName1 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName1).isEqualTo(PACKAGE_NAME1);
        // Verifies the second data package name.
        cursor.moveToNext();
        final String actualPackageName2 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName2).isEqualTo(PACKAGE_NAME2);
        // Verifies the third data package name.
        cursor.moveToNext();
        final String actualPackageName3 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName3).isEqualTo(PACKAGE_NAME3);
        cursor.close();
    }

    @Test
    public void query_batteryStateTimestamp_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        final long expiredTimeCutoff = currentTime.toMillis() - 2;

        final Cursor cursor = insertBatteryState(currentTime, Long.toString(expiredTimeCutoff));

        // Verifies the result not include expired data.
        assertThat(cursor.getCount()).isEqualTo(2);
        final int packageNameIndex = cursor.getColumnIndex("packageName");
        // Verifies the first data package name.
        cursor.moveToFirst();
        final String actualPackageName1 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName1).isEqualTo(PACKAGE_NAME2);
        // Verifies the third data package name.
        cursor.moveToNext();
        final String actualPackageName2 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName2).isEqualTo(PACKAGE_NAME3);
        cursor.close();
    }

    @Test
    public void query_getBatteryStateLatestTimestamp_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        insertBatteryState(currentTime, Long.toString(currentTime.toMillis()));

        final Cursor cursor1 = getCursorOfBatteryStateLatestTimestamp(currentTime.toMillis() - 5);
        assertThat(cursor1.getCount()).isEqualTo(1);
        cursor1.moveToFirst();
        final long latestTimestamp1 = cursor1.getLong(0);
        assertThat(latestTimestamp1).isEqualTo(currentTime.toMillis() - 6);

        final Cursor cursor2 = getCursorOfBatteryStateLatestTimestamp(currentTime.toMillis() - 2);
        assertThat(cursor2.getCount()).isEqualTo(1);
        cursor2.moveToFirst();
        final long latestTimestamp2 = cursor2.getLong(0);
        assertThat(latestTimestamp2).isEqualTo(currentTime.toMillis() - 2);
    }

    @Test
    public void query_appUsageEvent_returnsExpectedResult() {
        insertAppUsageEvent();

        final List<Long> userIds1 = new ArrayList<>();
        final long notExistingUserId = 3;
        userIds1.add(USER_ID1);
        userIds1.add(USER_ID2);
        userIds1.add(notExistingUserId);
        final Cursor cursor1 = getCursorOfAppUsage(userIds1, TIMESTAMP1);
        assertThat(cursor1.getCount()).isEqualTo(3);
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
        final Cursor cursor2 = getCursorOfAppUsage(userIds2, TIMESTAMP3);
        assertThat(cursor2.getCount()).isEqualTo(1);
        // Verifies the queried first battery state.
        cursor2.moveToFirst();
        assertThat(cursor2.getString(5 /*packageName*/)).isEqualTo(PACKAGE_NAME3);
    }

    @Test
    public void query_appUsageTimestamp_returnsExpectedResult() throws Exception {
        insertAppUsageEvent();

        final Cursor cursor1 = getCursorOfLatestTimestamp(USER_ID1);
        assertThat(cursor1.getCount()).isEqualTo(1);
        cursor1.moveToFirst();
        assertThat(cursor1.getLong(0)).isEqualTo(TIMESTAMP3);

        final Cursor cursor2 = getCursorOfLatestTimestamp(USER_ID2);
        assertThat(cursor2.getCount()).isEqualTo(1);
        cursor2.moveToFirst();
        assertThat(cursor2.getLong(0)).isEqualTo(TIMESTAMP2);

        final long notExistingUserId = 3;
        final Cursor cursor3 = getCursorOfLatestTimestamp(notExistingUserId);
        assertThat(cursor3.getCount()).isEqualTo(1);
        cursor3.moveToFirst();
        assertThat(cursor3.getLong(0)).isEqualTo(0);
    }

    @Test
    public void insert_batteryState_returnsExpectedResult() {
        mProvider.onCreate();
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState.newBuilder()
                        .setBatteryLevel(51)
                        .setBatteryStatus(2)
                        .setBatteryHealth(3)
                        .build();
        final BatteryInformation batteryInformation =
                BatteryInformation.newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .setAppLabel("Settings")
                        .setIsHidden(true)
                        .setBootTimestamp(101L)
                        .setTotalPower(99)
                        .setConsumePower(9)
                        .setForegroundUsageConsumePower(1)
                        .setForegroundServiceUsageConsumePower(2)
                        .setBackgroundUsageConsumePower(3)
                        .setCachedUsageConsumePower(3)
                        .setPercentOfTotal(0.9)
                        .setForegroundUsageTimeInMs(1000)
                        .setBackgroundUsageTimeInMs(2000)
                        .setForegroundServiceUsageTimeInMs(1500)
                        .setDrainType(1)
                        .build();
        final String expectedBatteryInformationString =
                ConvertUtils.convertBatteryInformationToString(batteryInformation);
        ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_UID, Long.valueOf(101L));
        values.put(BatteryHistEntry.KEY_USER_ID, Long.valueOf(1001L));
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, new String("com.android.settings"));
        values.put(BatteryHistEntry.KEY_TIMESTAMP, Long.valueOf(2100021L));
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, Integer.valueOf(2));
        values.put(BatteryHistEntry.KEY_IS_FULL_CHARGE_CYCLE_START, true);
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION, expectedBatteryInformationString);

        final Uri uri = mProvider.insert(VALID_BATTERY_STATE_CONTENT_URI, values);

        assertThat(uri).isEqualTo(VALID_BATTERY_STATE_CONTENT_URI);
        // Verifies the BatteryState content.
        final List<BatteryState> states =
                BatteryStateDatabase.getInstance(mContext).batteryStateDao().getAllAfter(0);
        assertThat(states).hasSize(1);
        assertThat(states.get(0).uid).isEqualTo(101L);
        assertThat(states.get(0).userId).isEqualTo(1001L);
        assertThat(states.get(0).packageName).isEqualTo("com.android.settings");
        assertThat(states.get(0).timestamp).isEqualTo(2100021L);
        assertThat(states.get(0).consumerType).isEqualTo(2);
        assertThat(states.get(0).isFullChargeCycleStart).isTrue();
        assertThat(states.get(0).batteryInformation).isEqualTo(expectedBatteryInformationString);
    }

    @Test
    public void insert_partialFieldsContentValues_returnsExpectedResult() {
        mProvider.onCreate();
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState.newBuilder()
                        .setBatteryLevel(52)
                        .setBatteryStatus(3)
                        .setBatteryHealth(2)
                        .build();
        final BatteryInformation batteryInformation =
                BatteryInformation.newBuilder().setDeviceBatteryState(deviceBatteryState).build();
        final String expectedBatteryInformationString =
                ConvertUtils.convertBatteryInformationToString(batteryInformation);
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, new String("fake_data"));
        values.put(BatteryHistEntry.KEY_TIMESTAMP, Long.valueOf(2100022L));
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION, expectedBatteryInformationString);

        final Uri uri = mProvider.insert(VALID_BATTERY_STATE_CONTENT_URI, values);

        assertThat(uri).isEqualTo(VALID_BATTERY_STATE_CONTENT_URI);
        // Verifies the BatteryState content.
        final List<BatteryState> states =
                BatteryStateDatabase.getInstance(mContext).batteryStateDao().getAllAfter(0);
        assertThat(states).hasSize(1);
        assertThat(states.get(0).packageName).isEqualTo("fake_data");
        assertThat(states.get(0).timestamp).isEqualTo(2100022L);
        assertThat(states.get(0).batteryInformation).isEqualTo(expectedBatteryInformationString);
    }

    @Test
    public void insert_appUsageEvent_returnsExpectedResult() {
        mProvider.onCreate();
        ContentValues values = new ContentValues();
        values.put(AppUsageEventEntity.KEY_UID, 101L);
        values.put(AppUsageEventEntity.KEY_USER_ID, 1001L);
        values.put(AppUsageEventEntity.KEY_TIMESTAMP, 10001L);
        values.put(AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE, 1);
        values.put(AppUsageEventEntity.KEY_PACKAGE_NAME, "com.android.settings1");
        values.put(AppUsageEventEntity.KEY_INSTANCE_ID, 100001L);
        values.put(AppUsageEventEntity.KEY_TASK_ROOT_PACKAGE_NAME, "com.android.settings2");

        final Uri uri = mProvider.insert(DatabaseUtils.APP_USAGE_EVENT_URI, values);

        assertThat(uri).isEqualTo(DatabaseUtils.APP_USAGE_EVENT_URI);
        // Verifies the AppUsageEventEntity content.
        final List<AppUsageEventEntity> entities =
                BatteryStateDatabase.getInstance(mContext).appUsageEventDao().getAllAfter(0);
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).uid).isEqualTo(101L);
        assertThat(entities.get(0).userId).isEqualTo(1001L);
        assertThat(entities.get(0).timestamp).isEqualTo(10001L);
        assertThat(entities.get(0).appUsageEventType).isEqualTo(1);
        assertThat(entities.get(0).packageName).isEqualTo("com.android.settings1");
        assertThat(entities.get(0).instanceId).isEqualTo(100001L);
        assertThat(entities.get(0).taskRootPackageName).isEqualTo("com.android.settings2");
    }

    @Test
    public void insertAndQuery_batteryEvent_returnsExpectedResult() {
        mProvider.onCreate();
        ContentValues values = new ContentValues();
        values.put(BatteryEventEntity.KEY_TIMESTAMP, 10001L);
        values.put(
                BatteryEventEntity.KEY_BATTERY_EVENT_TYPE,
                BatteryEventType.POWER_CONNECTED.getNumber());
        values.put(BatteryEventEntity.KEY_BATTERY_LEVEL, 66);

        final Uri uri = mProvider.insert(DatabaseUtils.BATTERY_EVENT_URI, values);

        assertThat(uri).isEqualTo(DatabaseUtils.BATTERY_EVENT_URI);
        // Verifies the BatteryEventEntity content.
        final List<BatteryEventEntity> entities =
                BatteryStateDatabase.getInstance(mContext).batteryEventDao().getAll();
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).timestamp).isEqualTo(10001L);
        assertThat(entities.get(0).batteryEventType)
                .isEqualTo(BatteryEventType.POWER_CONNECTED.getNumber());
        assertThat(entities.get(0).batteryLevel).isEqualTo(66);

        final Cursor cursor1 =
                getCursorOfBatteryEvents(0L, List.of(BatteryEventType.POWER_CONNECTED.getNumber()));
        assertThat(cursor1.getCount()).isEqualTo(1);
        cursor1.moveToFirst();
        assertThat(cursor1.getLong(cursor1.getColumnIndex(BatteryEventEntity.KEY_TIMESTAMP)))
                .isEqualTo(10001L);
        assertThat(
                        cursor1.getInt(
                                cursor1.getColumnIndex(BatteryEventEntity.KEY_BATTERY_EVENT_TYPE)))
                .isEqualTo(BatteryEventType.POWER_CONNECTED.getNumber());
        assertThat(cursor1.getInt(cursor1.getColumnIndex(BatteryEventEntity.KEY_BATTERY_LEVEL)))
                .isEqualTo(66);

        final Cursor cursor2 =
                getCursorOfBatteryEvents(
                        0L, List.of(BatteryEventType.POWER_DISCONNECTED.getNumber()));
        assertThat(cursor2.getCount()).isEqualTo(0);
    }

    @Test
    public void insertAndQuery_batteryUsageSlot_returnsExpectedResult() {
        mProvider.onCreate();
        ContentValues values = new ContentValues();
        values.put(BatteryUsageSlotEntity.KEY_TIMESTAMP, 10001L);
        values.put(BatteryUsageSlotEntity.KEY_BATTERY_USAGE_SLOT, "TEST_STRING");

        final Uri uri = mProvider.insert(DatabaseUtils.BATTERY_USAGE_SLOT_URI, values);
        // Verifies the BatteryUsageSlotEntity content.
        assertThat(uri).isEqualTo(DatabaseUtils.BATTERY_USAGE_SLOT_URI);
        final List<BatteryUsageSlotEntity> entities =
                BatteryStateDatabase.getInstance(mContext).batteryUsageSlotDao().getAll();
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).timestamp).isEqualTo(10001L);
        assertThat(entities.get(0).batteryUsageSlot).isEqualTo("TEST_STRING");

        final Cursor cursor1 = getCursorOfBatteryUsageSlots(10001L);
        assertThat(cursor1.getCount()).isEqualTo(1);
        cursor1.moveToFirst();
        assertThat(cursor1.getLong(cursor1.getColumnIndex(BatteryUsageSlotEntity.KEY_TIMESTAMP)))
                .isEqualTo(10001L);
        assertThat(
                        cursor1.getString(
                                cursor1.getColumnIndex(
                                        BatteryUsageSlotEntity.KEY_BATTERY_USAGE_SLOT)))
                .isEqualTo("TEST_STRING");

        final Cursor cursor2 = getCursorOfBatteryUsageSlots(10002L);
        assertThat(cursor2.getCount()).isEqualTo(0);
    }

    @Test
    public void delete_throwsUnsupportedOperationException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> mProvider.delete(/* uri= */ null, /* s= */ null, /* strings= */ null));
    }

    @Test
    public void update_throwsUnsupportedOperationException() {
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        mProvider.update(
                                /* uri= */ null,
                                /* contentValues= */ null,
                                /* s= */ null,
                                /* strings= */ null));
    }

    private Cursor insertBatteryState(Duration currentTime, String queryTimestamp)
            throws Exception {
        mProvider.onCreate();
        final FakeClock fakeClock = new FakeClock();
        fakeClock.setCurrentTime(currentTime);
        mProvider.setClock(fakeClock);
        final long currentTimestamp = currentTime.toMillis();
        // Inserts some valid testing data.
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, currentTimestamp - 6, PACKAGE_NAME1, /* isFullChargeStart= */ true);
        BatteryTestUtils.insertDataToBatteryStateTable(
                mContext, currentTimestamp - 2, PACKAGE_NAME2);
        BatteryTestUtils.insertDataToBatteryStateTable(mContext, currentTimestamp, PACKAGE_NAME3);

        final Uri batteryStateQueryContentUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_STATE_TABLE)
                        .appendQueryParameter(DatabaseUtils.QUERY_KEY_TIMESTAMP, queryTimestamp)
                        .build();

        final Cursor cursor = query(batteryStateQueryContentUri);

        return cursor;
    }

    private Cursor getCursorOfLastFullChargeTimestamp() {
        final Uri lastFullChargeTimestampContentUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.LAST_FULL_CHARGE_TIMESTAMP_PATH)
                        .build();

        return query(lastFullChargeTimestampContentUri);
    }

    private Cursor getCursorOfBatteryStateLatestTimestamp(final long queryTimestamp) {
        final Uri batteryStateLatestTimestampUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_STATE_LATEST_TIMESTAMP_PATH)
                        .appendQueryParameter(
                                DatabaseUtils.QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .build();

        return query(batteryStateLatestTimestampUri);
    }

    private void insertAppUsageEvent() {
        mProvider.onCreate();
        // Inserts some valid testing data.
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID1, TIMESTAMP1, PACKAGE_NAME1);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID2, TIMESTAMP2, PACKAGE_NAME2);
        BatteryTestUtils.insertDataToAppUsageEventTable(
                mContext, USER_ID1, TIMESTAMP3, PACKAGE_NAME3);
    }

    private Cursor getCursorOfLatestTimestamp(final long userId) {
        final Uri appUsageLatestTimestampQueryContentUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.APP_USAGE_LATEST_TIMESTAMP_PATH)
                        .appendQueryParameter(DatabaseUtils.QUERY_KEY_USERID, Long.toString(userId))
                        .build();

        return query(appUsageLatestTimestampQueryContentUri);
    }

    private Cursor getCursorOfAppUsage(final List<Long> userIds, final long queryTimestamp) {
        final String queryUserIdString =
                userIds.stream()
                        .map(userId -> String.valueOf(userId))
                        .collect(Collectors.joining(","));
        final Uri appUsageEventUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.APP_USAGE_EVENT_TABLE)
                        .appendQueryParameter(
                                DatabaseUtils.QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .appendQueryParameter(DatabaseUtils.QUERY_KEY_USERID, queryUserIdString)
                        .build();

        return query(appUsageEventUri);
    }

    private Cursor getCursorOfBatteryEvents(
            final long queryTimestamp, final List<Integer> batteryEventTypes) {
        final String batteryEventTypesString =
                batteryEventTypes.stream()
                        .map(type -> String.valueOf(type))
                        .collect(Collectors.joining(","));
        final Uri batteryEventUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_EVENT_TABLE)
                        .appendQueryParameter(
                                DatabaseUtils.QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .appendQueryParameter(
                                DatabaseUtils.QUERY_BATTERY_EVENT_TYPE, batteryEventTypesString)
                        .build();

        return query(batteryEventUri);
    }

    private Cursor getCursorOfBatteryUsageSlots(final long queryTimestamp) {
        final Uri batteryUsageSlotUri =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_USAGE_SLOT_TABLE)
                        .appendQueryParameter(
                                DatabaseUtils.QUERY_KEY_TIMESTAMP, Long.toString(queryTimestamp))
                        .build();

        return query(batteryUsageSlotUri);
    }

    private Cursor query(Uri uri) {
        return mProvider.query(
                uri, /* strings= */ null, /* s= */ null, /* strings1= */ null, /* s1= */ null);
    }
}
