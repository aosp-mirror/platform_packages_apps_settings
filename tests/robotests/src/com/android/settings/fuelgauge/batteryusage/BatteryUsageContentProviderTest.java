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
        ContentValues values = new ContentValues();
        values.put("uid", Long.valueOf(101L));
        values.put("userId", Long.valueOf(1001L));
        values.put("appLabel", new String("Settings"));
        values.put("packageName", new String("com.android.settings"));
        values.put("timestamp", Long.valueOf(2100021L));
        values.put("isHidden", Boolean.valueOf(true));
        values.put("totalPower", Double.valueOf(99.0));
        values.put("consumePower", Double.valueOf(9.0));
        values.put("percentOfTotal", Double.valueOf(0.9));
        values.put("foregroundUsageTimeInMs", Long.valueOf(1000));
        values.put("backgroundUsageTimeInMs", Long.valueOf(2000));
        values.put("drainType", Integer.valueOf(1));
        values.put("consumerType", Integer.valueOf(2));
        values.put("batteryLevel", Integer.valueOf(51));
        values.put("batteryStatus", Integer.valueOf(2));
        values.put("batteryHealth", Integer.valueOf(3));

        final Uri uri = mProvider.insert(VALID_BATTERY_STATE_CONTENT_URI, values);

        assertThat(uri).isEqualTo(VALID_BATTERY_STATE_CONTENT_URI);
        // Verifies the BatteryState content.
        final List<BatteryState> states =
                BatteryStateDatabase.getInstance(mContext).batteryStateDao().getAllAfter(0);
        assertThat(states).hasSize(1);
        assertThat(states.get(0).uid).isEqualTo(101L);
        assertThat(states.get(0).userId).isEqualTo(1001L);
        assertThat(states.get(0).appLabel).isEqualTo("Settings");
        assertThat(states.get(0).packageName).isEqualTo("com.android.settings");
        assertThat(states.get(0).isHidden).isTrue();
        assertThat(states.get(0).timestamp).isEqualTo(2100021L);
        assertThat(states.get(0).totalPower).isEqualTo(99.0);
        assertThat(states.get(0).consumePower).isEqualTo(9.0);
        assertThat(states.get(0).percentOfTotal).isEqualTo(0.9);
        assertThat(states.get(0).foregroundUsageTimeInMs).isEqualTo(1000);
        assertThat(states.get(0).backgroundUsageTimeInMs).isEqualTo(2000);
        assertThat(states.get(0).drainType).isEqualTo(1);
        assertThat(states.get(0).consumerType).isEqualTo(2);
        assertThat(states.get(0).batteryLevel).isEqualTo(51);
        assertThat(states.get(0).batteryStatus).isEqualTo(2);
        assertThat(states.get(0).batteryHealth).isEqualTo(3);
    }

    @Test
    public void insert_partialFieldsContentValues_returnsExpectedResult() {
        mProvider.onCreate();
        final ContentValues values = new ContentValues();
        values.put("packageName", new String("fake_data"));
        values.put("timestamp", Long.valueOf(2100022L));
        values.put("batteryLevel", Integer.valueOf(52));
        values.put("batteryStatus", Integer.valueOf(3));
        values.put("batteryHealth", Integer.valueOf(2));

        final Uri uri = mProvider.insert(VALID_BATTERY_STATE_CONTENT_URI, values);

        assertThat(uri).isEqualTo(VALID_BATTERY_STATE_CONTENT_URI);
        // Verifies the BatteryState content.
        final List<BatteryState> states =
                BatteryStateDatabase.getInstance(mContext).batteryStateDao().getAllAfter(0);
        assertThat(states).hasSize(1);
        assertThat(states.get(0).packageName).isEqualTo("fake_data");
        assertThat(states.get(0).timestamp).isEqualTo(2100022L);
        assertThat(states.get(0).batteryLevel).isEqualTo(52);
        assertThat(states.get(0).batteryStatus).isEqualTo(3);
        assertThat(states.get(0).batteryHealth).isEqualTo(2);
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
