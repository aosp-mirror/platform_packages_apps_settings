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

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Tests for {@link BatteryUsageContentProvider}. */
@RunWith(RobolectricTestRunner.class)
public final class BatteryUsageContentProviderTest {
    private static final Uri VALID_BATTERY_STATE_CONTENT_URI = DatabaseUtils.BATTERY_CONTENT_URI;

    private Context mContext;
    private BatteryUsageContentProvider mProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mProvider = new BatteryUsageContentProvider();
        mProvider.attachInfo(mContext, /*info=*/ null);
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
                                uri, /*strings=*/ null, /*s=*/ null, /*strings1=*/ null,
                                /*s1=*/ null));
        assertThrows(
                IllegalArgumentException.class,
                () -> mProvider.insert(uri, /*contentValues=*/ null));
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
                                uri, /*strings=*/ null, /*s=*/ null, /*strings1=*/ null,
                                /*s1=*/ null));
        assertThrows(
                IllegalArgumentException.class,
                () -> mProvider.insert(uri, /*contentValues=*/ null));
    }

    @Test
    public void query_batteryState_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        final long expiredTimeCutoff = currentTime.toMillis()
                - BatteryUsageContentProvider.QUERY_DURATION_HOURS.toMillis();
        testQueryBatteryState(currentTime, expiredTimeCutoff, /*hasQueryTimestamp=*/ false);
    }

    @Test
    public void query_batteryStateTimestamp_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        final long expiredTimeCutoff = currentTime.toMillis() - Duration.ofHours(10).toMillis();
        testQueryBatteryState(currentTime, expiredTimeCutoff, /*hasQueryTimestamp=*/ true);
    }

    @Test
    public void query_incorrectParameterFormat_returnsExpectedResult() throws Exception {
        mProvider.onCreate();
        final Duration currentTime = Duration.ofHours(52);
        final long expiredTimeCutoff =
                currentTime.toMillis()
                        - BatteryUsageContentProvider.QUERY_DURATION_HOURS.toMillis();
        testQueryBatteryState(
                currentTime,
                expiredTimeCutoff,
                /*hasQueryTimestamp=*/ false,
                /*customParameter=*/ "invalid number format");
    }

    @Test
    public void insert_batteryState_returnsExpectedResult() {
        mProvider.onCreate();
        final DeviceBatteryState deviceBatteryState =
                DeviceBatteryState
                        .newBuilder()
                        .setBatteryLevel(51)
                        .setBatteryStatus(2)
                        .setBatteryHealth(3)
                        .build();
        final BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .setAppLabel("Settings")
                        .setIsHidden(true)
                        .setBootTimestamp(101L)
                        .setTotalPower(99)
                        .setConsumePower(9)
                        .setPercentOfTotal(0.9)
                        .setForegroundUsageTimeInMs(1000)
                        .setBackgroundUsageTimeInMs(2000)
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
                DeviceBatteryState
                        .newBuilder()
                        .setBatteryLevel(52)
                        .setBatteryStatus(3)
                        .setBatteryHealth(2)
                        .build();
        final BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .build();
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
    public void delete_throwsUnsupportedOperationException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> mProvider.delete(/*uri=*/ null, /*s=*/ null, /*strings=*/ null));
    }

    @Test
    public void update_throwsUnsupportedOperationException() {
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        mProvider.update(
                                /*uri=*/ null, /*contentValues=*/ null, /*s=*/ null,
                                /*strings=*/ null));
    }

    private void testQueryBatteryState(
            Duration currentTime, long expiredTimeCutoff, boolean hasQueryTimestamp)
            throws Exception {
        testQueryBatteryState(currentTime, expiredTimeCutoff, hasQueryTimestamp, null);
    }

    private void testQueryBatteryState(
            Duration currentTime,
            long expiredTimeCutoff,
            boolean hasQueryTimestamp,
            String customParameter)
            throws Exception {
        mProvider.onCreate();
        final FakeClock fakeClock = new FakeClock();
        fakeClock.setCurrentTime(currentTime);
        mProvider.setClock(fakeClock);
        // Inserts some expired testing data.
        BatteryTestUtils.insertDataToBatteryStateDatabase(
                mContext, expiredTimeCutoff - 1, "com.android.sysui1");
        BatteryTestUtils.insertDataToBatteryStateDatabase(
                mContext, expiredTimeCutoff - 2, "com.android.sysui2");
        BatteryTestUtils.insertDataToBatteryStateDatabase(
                mContext, expiredTimeCutoff - 3, "com.android.sysui3");
        // Inserts some valid testing data.
        final String packageName1 = "com.android.settings1";
        final String packageName2 = "com.android.settings2";
        final String packageName3 = "com.android.settings3";
        BatteryTestUtils.insertDataToBatteryStateDatabase(
                mContext, currentTime.toMillis(), packageName1);
        BatteryTestUtils.insertDataToBatteryStateDatabase(
                mContext, expiredTimeCutoff + 2, packageName2);
        BatteryTestUtils.insertDataToBatteryStateDatabase(
                mContext, expiredTimeCutoff, packageName3);

        final Uri.Builder builder =
                new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_CONTENT)
                        .authority(DatabaseUtils.AUTHORITY)
                        .appendPath(DatabaseUtils.BATTERY_STATE_TABLE);
        if (customParameter != null) {
            builder.appendQueryParameter(
                    BatteryUsageContentProvider.QUERY_KEY_TIMESTAMP, customParameter);
        } else if (hasQueryTimestamp) {
            builder.appendQueryParameter(
                    BatteryUsageContentProvider.QUERY_KEY_TIMESTAMP,
                    Long.toString(expiredTimeCutoff));
        }
        final Uri batteryStateQueryContentUri = builder.build();

        final Cursor cursor =
                mProvider.query(
                        batteryStateQueryContentUri,
                        /*strings=*/ null,
                        /*s=*/ null,
                        /*strings1=*/ null,
                        /*s1=*/ null);

        // Verifies the result not include expired data.
        assertThat(cursor.getCount()).isEqualTo(3);
        final int packageNameIndex = cursor.getColumnIndex("packageName");
        // Verifies the first data package name.
        cursor.moveToFirst();
        final String actualPackageName1 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName1).isEqualTo(packageName1);
        // Verifies the second data package name.
        cursor.moveToNext();
        final String actualPackageName2 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName2).isEqualTo(packageName2);
        // Verifies the third data package name.
        cursor.moveToNext();
        final String actualPackageName3 = cursor.getString(packageNameIndex);
        assertThat(actualPackageName3).isEqualTo(packageName3);
        cursor.close();
        // Verifies the broadcast intent.
        TimeUnit.SECONDS.sleep(1);
        final List<Intent> intents = Shadows.shadowOf((Application) mContext).getBroadcastIntents();
        assertThat(intents).hasSize(1);
        assertThat(intents.get(0).getAction()).isEqualTo(
                BootBroadcastReceiver.ACTION_PERIODIC_JOB_RECHECK);
    }
}
