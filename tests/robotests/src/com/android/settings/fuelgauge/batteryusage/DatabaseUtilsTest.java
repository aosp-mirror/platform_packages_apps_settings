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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.testutils.BatteryTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public final class DatabaseUtilsTest {

    private Context mContext;

    @Mock
    private PackageManager mPackageManager;
    @Mock private ContentResolver mMockContentResolver;
    @Mock private ContentResolver mMockContentResolver2;
    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Mock private BatteryEntry mMockBatteryEntry1;
    @Mock private BatteryEntry mMockBatteryEntry2;
    @Mock private BatteryEntry mMockBatteryEntry3;
    @Mock private Context mMockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mMockContentResolver2).when(mMockContext).getContentResolver();
        doReturn(mMockContentResolver).when(mContext).getContentResolver();
        doReturn(mPackageManager).when(mMockContext).getPackageManager();
        doReturn(mPackageManager).when(mContext).getPackageManager();
    }

    @Test
    public void isWorkProfile_defaultValue_returnFalse() {
        assertThat(DatabaseUtils.isWorkProfile(mContext)).isFalse();
    }

    @Test
    public void isWorkProfile_withManagedUser_returnTrue() {
        BatteryTestUtils.setWorkProfile(mContext);
        assertThat(DatabaseUtils.isWorkProfile(mContext)).isTrue();
    }

    @Test
    public void isWorkProfile_withSystemUser_returnFalse() {
        BatteryTestUtils.setWorkProfile(mContext);
        Shadows.shadowOf(mContext.getSystemService(UserManager.class)).setIsSystemUser(true);

        assertThat(DatabaseUtils.isWorkProfile(mContext)).isFalse();
    }

    @Test
    public void sendBatteryEntryData_nullBatteryIntent_returnsNullValue() {
        doReturn(null).when(mContext).registerReceiver(any(), any());
        assertThat(
                DatabaseUtils.sendBatteryEntryData(
                        mContext, /*batteryEntryList=*/ null, mBatteryUsageStats))
                .isNull();
    }

    @Test
    public void sendBatteryEntryData_returnsExpectedList() {
        doReturn(getBatteryIntent()).when(mContext).registerReceiver(any(), any());
        // Configures the testing BatteryEntry data.
        final List<BatteryEntry> batteryEntryList = new ArrayList<>();
        batteryEntryList.add(mMockBatteryEntry1);
        batteryEntryList.add(mMockBatteryEntry2);
        batteryEntryList.add(mMockBatteryEntry3);
        doReturn(0.0).when(mMockBatteryEntry1).getConsumedPower();
        doReturn(0.5).when(mMockBatteryEntry2).getConsumedPower();
        doReturn(0.0).when(mMockBatteryEntry3).getConsumedPower();
        doReturn(1L).when(mMockBatteryEntry3).getTimeInForegroundMs();

        final List<ContentValues> valuesList =
                DatabaseUtils.sendBatteryEntryData(
                        mContext, batteryEntryList, mBatteryUsageStats);

        assertThat(valuesList).hasSize(2);
        // Verifies the ContentValues content.
        verifyContentValues(0.5, valuesList.get(0));
        verifyContentValues(0.0, valuesList.get(1));
        // Verifies the inserted ContentValues into content provider.
        final ContentValues[] valuesArray =
                new ContentValues[] {valuesList.get(0), valuesList.get(1)};
        verify(mMockContentResolver).bulkInsert(
                DatabaseUtils.BATTERY_CONTENT_URI, valuesArray);
        verify(mMockContentResolver).notifyChange(
                DatabaseUtils.BATTERY_CONTENT_URI, /*observer=*/ null);
    }

    @Test
    public void sendBatteryEntryData_emptyBatteryEntryList_sendFakeDataIntoProvider() {
        doReturn(getBatteryIntent()).when(mContext).registerReceiver(any(), any());

        final List<ContentValues> valuesList =
                DatabaseUtils.sendBatteryEntryData(
                        mContext,
                        new ArrayList<>(),
                        mBatteryUsageStats);

        assertThat(valuesList).hasSize(1);
        verifyFakeContentValues(valuesList.get(0));
        // Verifies the inserted ContentValues into content provider.
        verify(mMockContentResolver).insert(any(), any());
        verify(mMockContentResolver).notifyChange(
                DatabaseUtils.BATTERY_CONTENT_URI, /*observer=*/ null);
    }

    @Test
    public void sendBatteryEntryData_nullBatteryEntryList_sendFakeDataIntoProvider() {
        doReturn(getBatteryIntent()).when(mContext).registerReceiver(any(), any());

        final List<ContentValues> valuesList =
                DatabaseUtils.sendBatteryEntryData(
                        mContext,
                        /*batteryEntryList=*/ null,
                        mBatteryUsageStats);

        assertThat(valuesList).hasSize(1);
        verifyFakeContentValues(valuesList.get(0));
        // Verifies the inserted ContentValues into content provider.
        verify(mMockContentResolver).insert(any(), any());
        verify(mMockContentResolver).notifyChange(
                DatabaseUtils.BATTERY_CONTENT_URI, /*observer=*/ null);
    }

    @Test
    public void sendBatteryEntryData_nullBatteryUsageStats_sendFakeDataIntoProvider() {
        doReturn(getBatteryIntent()).when(mContext).registerReceiver(any(), any());

        final List<ContentValues> valuesList =
                DatabaseUtils.sendBatteryEntryData(
                        mContext,
                        /*batteryEntryList=*/ null,
                        /*batteryUsageStats=*/ null);

        assertThat(valuesList).hasSize(1);
        verifyFakeContentValues(valuesList.get(0));
        // Verifies the inserted ContentValues into content provider.
        verify(mMockContentResolver).insert(any(), any());
        verify(mMockContentResolver).notifyChange(
                DatabaseUtils.BATTERY_CONTENT_URI, /*observer=*/ null);
    }

    @Test
    public void getHistoryMapSinceLastFullCharge_emptyCursorContent_returnEmptyMap() {
        final MatrixCursor cursor = new MatrixCursor(
                new String[] {
                        BatteryHistEntry.KEY_UID,
                        BatteryHistEntry.KEY_USER_ID,
                        BatteryHistEntry.KEY_TIMESTAMP});
        doReturn(cursor).when(mMockContentResolver).query(any(), any(), any(), any());

        assertThat(DatabaseUtils.getHistoryMapSinceLastFullCharge(
                mContext, /*calendar=*/ null)).isEmpty();
    }

    @Test
    public void getHistoryMapSinceLastFullCharge_nullCursor_returnEmptyMap() {
        doReturn(null).when(mMockContentResolver).query(any(), any(), any(), any());
        assertThat(DatabaseUtils.getHistoryMapSinceLastFullCharge(
                mContext, /*calendar=*/ null)).isEmpty();
    }

    @Test
    public void getHistoryMapSinceLastFullCharge_returnExpectedMap() {
        final Long timestamp1 = Long.valueOf(1001L);
        final Long timestamp2 = Long.valueOf(1002L);
        final MatrixCursor cursor = getMatrixCursor();
        doReturn(cursor).when(mMockContentResolver).query(any(), any(), any(), any());
        // Adds fake data into the cursor.
        cursor.addRow(new Object[] {
                "app name1", timestamp1, 1, ConvertUtils.CONSUMER_TYPE_UID_BATTERY});
        cursor.addRow(new Object[] {
                "app name2", timestamp2, 2, ConvertUtils.CONSUMER_TYPE_UID_BATTERY});
        cursor.addRow(new Object[] {
                "app name3", timestamp2, 3, ConvertUtils.CONSUMER_TYPE_UID_BATTERY});
        cursor.addRow(new Object[] {
                "app name4", timestamp2, 4, ConvertUtils.CONSUMER_TYPE_UID_BATTERY});

        final Map<Long, Map<String, BatteryHistEntry>> batteryHistMap =
                DatabaseUtils.getHistoryMapSinceLastFullCharge(
                        mContext, /*calendar=*/ null);

        assertThat(batteryHistMap).hasSize(2);
        // Verifies the BatteryHistEntry data for timestamp1.
        Map<String, BatteryHistEntry> batteryMap = batteryHistMap.get(timestamp1);
        assertThat(batteryMap).hasSize(1);
        assertThat(batteryMap.get("1").mPackageName).isEqualTo("app name1");
        // Verifies the BatteryHistEntry data for timestamp2.
        batteryMap = batteryHistMap.get(timestamp2);
        assertThat(batteryMap).hasSize(3);
        assertThat(batteryMap.get("2").mPackageName).isEqualTo("app name2");
        assertThat(batteryMap.get("3").mPackageName).isEqualTo("app name3");
        assertThat(batteryMap.get("4").mPackageName).isEqualTo("app name4");
    }

    @Test
    public void getHistoryMapSinceLastFullCharge_withWorkProfile_returnExpectedMap()
            throws PackageManager.NameNotFoundException {
        doReturn("com.fake.package").when(mContext).getPackageName();
        doReturn(mMockContext).when(mContext).createPackageContextAsUser(
                "com.fake.package", /*flags=*/ 0, UserHandle.OWNER);
        BatteryTestUtils.setWorkProfile(mContext);
        doReturn(getMatrixCursor()).when(mMockContentResolver2)
                .query(any(), any(), any(), any());
        doReturn(null).when(mMockContentResolver).query(any(), any(), any(), any());

        final Map<Long, Map<String, BatteryHistEntry>> batteryHistMap =
                DatabaseUtils.getHistoryMapSinceLastFullCharge(
                        mContext, /*calendar=*/ null);

        assertThat(batteryHistMap).isEmpty();
    }

    @Test
    public void saveLastFullChargeTimestampPref_notFullCharge_returnsFalse() {
        DatabaseUtils.saveLastFullChargeTimestampPref(
                mContext,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
                /* level */ 10,
                /* timestamp */ 1);
        assertThat(DatabaseUtils.getLastFullChargeTimestampPref(mContext)).isEqualTo(0);
    }

    @Test
    public void saveLastFullChargeTimestampPref_fullStatus_returnsTrue() {
        long expectedTimestamp = 1;
        DatabaseUtils.saveLastFullChargeTimestampPref(
                mContext,
                BatteryManager.BATTERY_STATUS_FULL,
                /* level */ 10,
                /* timestamp */ expectedTimestamp);
        assertThat(DatabaseUtils.getLastFullChargeTimestampPref(mContext))
                .isEqualTo(expectedTimestamp);
    }

    @Test
    public void saveLastFullChargeTimestampPref_level100_returnsTrue() {
        long expectedTimestamp = 1;
        DatabaseUtils.saveLastFullChargeTimestampPref(
                mContext,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
                /* level */ 100,
                /* timestamp */ expectedTimestamp);
        assertThat(DatabaseUtils.getLastFullChargeTimestampPref(mContext))
                .isEqualTo(expectedTimestamp);
    }

    @Test
    public void getStartTimestampForLastFullCharge_noTimestampPreference_returnsSixDaysAgo() {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(2022, 6, 5, 6, 30, 50); // 2022-07-05 06:30:50
        Calendar expectedCalendar = Calendar.getInstance();
        expectedCalendar.set(2022, 5, 29, 0, 0, 0); // 2022-06-29 00:00:00
        expectedCalendar.set(Calendar.MILLISECOND, 0);

        assertThat(DatabaseUtils.getStartTimestampForLastFullCharge(mContext, currentCalendar))
                .isEqualTo(expectedCalendar.getTimeInMillis());
    }

    @Test
    public void getStartTimestampForLastFullCharge_lastFullChargeEarlier_returnsSixDaysAgo() {
        Calendar lastFullCalendar = Calendar.getInstance();
        lastFullCalendar.set(2021, 11, 25, 6, 30, 50); // 2021-12-25 06:30:50
        DatabaseUtils.saveLastFullChargeTimestampPref(
                mContext,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
                /* level */ 100,
                /* timestamp */ lastFullCalendar.getTimeInMillis());
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(2022, 0, 2, 6, 30, 50); // 2022-01-02 06:30:50
        Calendar expectedCalendar = Calendar.getInstance();
        expectedCalendar.set(2021, 11, 27, 0, 0, 0); // 2021-12-27 00:00:00
        expectedCalendar.set(Calendar.MILLISECOND, 0);

        assertThat(DatabaseUtils.getStartTimestampForLastFullCharge(mContext, currentCalendar))
                .isEqualTo(expectedCalendar.getTimeInMillis());
    }

    @Test
    public void getStartTimestampForLastFullCharge_lastFullChargeLater_returnsLastFullCharge() {
        Calendar lastFullCalendar = Calendar.getInstance();
        lastFullCalendar.set(2022, 6, 1, 6, 30, 50); // 2022-07-01 06:30:50
        long expectedTimestamp = lastFullCalendar.getTimeInMillis();
        DatabaseUtils.saveLastFullChargeTimestampPref(
                mContext,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
                /* level */ 100,
                /* timestamp */ expectedTimestamp);
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(2022, 6, 5, 6, 30, 50); // 2022-07-05 06:30:50

        assertThat(DatabaseUtils.getStartTimestampForLastFullCharge(mContext, currentCalendar))
                .isEqualTo(expectedTimestamp);
    }

    private static void verifyContentValues(double consumedPower, ContentValues values) {
        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(
                        values, BatteryHistEntry.KEY_BATTERY_INFORMATION);
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();
        assertThat(batteryInformation.getConsumePower()).isEqualTo(consumedPower);
        assertThat(deviceBatteryState.getBatteryLevel()).isEqualTo(20);
        assertThat(deviceBatteryState.getBatteryStatus())
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(deviceBatteryState.getBatteryHealth())
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    private static void verifyFakeContentValues(ContentValues values) {
        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(
                        values, BatteryHistEntry.KEY_BATTERY_INFORMATION);
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();
        assertThat(deviceBatteryState.getBatteryLevel()).isEqualTo(20);
        assertThat(deviceBatteryState.getBatteryStatus())
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(deviceBatteryState.getBatteryHealth())
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
        assertThat(values.getAsString("packageName"))
                .isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    private static Intent getBatteryIntent() {
        final Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, 20);
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_FULL);
        intent.putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_COLD);
        return intent;
    }

    private static MatrixCursor getMatrixCursor() {
        return new MatrixCursor(
                new String[] {
                        BatteryHistEntry.KEY_PACKAGE_NAME,
                        BatteryHistEntry.KEY_TIMESTAMP,
                        BatteryHistEntry.KEY_UID,
                        BatteryHistEntry.KEY_CONSUMER_TYPE});
    }
}
