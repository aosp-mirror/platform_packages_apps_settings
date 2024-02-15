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

import static android.app.usage.UsageStatsManager.USAGE_SOURCE_CURRENT_ACTIVITY;
import static android.app.usage.UsageStatsManager.USAGE_SOURCE_TASK_ROOT_ACTIVITY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.usage.IUsageStatsManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryEventEntity;
import com.android.settings.testutils.BatteryTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public final class DatabaseUtilsTest {

    private Context mContext;

    @Mock private PackageManager mPackageManager;
    @Mock private UserManager mUserManager;
    @Mock private ContentResolver mMockContentResolver;
    @Mock private ContentResolver mMockContentResolver2;
    @Mock private BatteryUsageStats mBatteryUsageStats;
    @Mock private BatteryEntry mMockBatteryEntry1;
    @Mock private BatteryEntry mMockBatteryEntry2;
    @Mock private BatteryEntry mMockBatteryEntry3;
    @Mock private Context mMockContext;
    @Mock private IUsageStatsManager mUsageStatsManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mMockContentResolver2).when(mMockContext).getContentResolver();
        doReturn(mMockContentResolver).when(mContext).getContentResolver();
        doReturn(mPackageManager).when(mMockContext).getPackageManager();
        doReturn(mPackageManager).when(mContext).getPackageManager();
        DatabaseUtils.getSharedPreferences(mContext).edit().clear().apply();
        DataProcessor.sUsageStatsManager = mUsageStatsManager;
    }

    @Test
    public void sendAppUsageEventData_returnsExpectedList() {
        // Configures the testing AppUsageEvent data.
        final List<AppUsageEvent> appUsageEventList = new ArrayList<>();
        final AppUsageEvent appUsageEvent1 =
                AppUsageEvent.newBuilder()
                        .setUid(101L)
                        .setType(AppUsageEventType.ACTIVITY_RESUMED)
                        .build();
        final AppUsageEvent appUsageEvent2 =
                AppUsageEvent.newBuilder()
                        .setUid(1001L)
                        .setType(AppUsageEventType.ACTIVITY_STOPPED)
                        .build();
        final AppUsageEvent appUsageEvent3 =
                AppUsageEvent.newBuilder().setType(AppUsageEventType.DEVICE_SHUTDOWN).build();
        appUsageEventList.add(appUsageEvent1);
        appUsageEventList.add(appUsageEvent2);
        appUsageEventList.add(appUsageEvent3);

        final List<ContentValues> valuesList =
                DatabaseUtils.sendAppUsageEventData(mContext, appUsageEventList);

        assertThat(valuesList).hasSize(2);
        assertThat(valuesList.get(0).getAsInteger(AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE))
                .isEqualTo(1);
        assertThat(valuesList.get(1).getAsInteger(AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE))
                .isEqualTo(2);
        // Verifies the inserted ContentValues into content provider.
        final ContentValues[] valuesArray =
                new ContentValues[] {valuesList.get(0), valuesList.get(1)};
        verify(mMockContentResolver).bulkInsert(DatabaseUtils.APP_USAGE_EVENT_URI, valuesArray);
        verify(mMockContentResolver)
                .notifyChange(DatabaseUtils.APP_USAGE_EVENT_URI, /* observer= */ null);
    }

    @Test
    public void sendAppUsageEventData_emptyAppUsageEventList_notSend() {
        final List<ContentValues> valuesList =
                DatabaseUtils.sendAppUsageEventData(mContext, new ArrayList<>());
        assertThat(valuesList).hasSize(0);
        verifyNoMoreInteractions(mMockContentResolver);
    }

    @Test
    public void sendBatteryEventData_returnsExpectedList() {
        final BatteryEvent batteryEvent =
                BatteryEvent.newBuilder()
                        .setTimestamp(10001L)
                        .setType(BatteryEventType.POWER_CONNECTED)
                        .setBatteryLevel(66)
                        .build();

        final ContentValues contentValues =
                DatabaseUtils.sendBatteryEventData(mContext, batteryEvent);

        assertThat(contentValues.getAsInteger(BatteryEventEntity.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(contentValues.getAsInteger(BatteryEventEntity.KEY_BATTERY_EVENT_TYPE))
                .isEqualTo(BatteryEventType.POWER_CONNECTED.getNumber());
        assertThat(contentValues.getAsInteger(BatteryEventEntity.KEY_BATTERY_LEVEL)).isEqualTo(66);
        // Verifies the inserted ContentValues into content provider.
        verify(mMockContentResolver).insert(DatabaseUtils.BATTERY_EVENT_URI, contentValues);
    }

    @Test
    public void sendBatteryEntryData_nullBatteryIntent_returnsNullValue() {
        doReturn(null).when(mContext).registerReceiver(any(), any());
        assertThat(
                        DatabaseUtils.sendBatteryEntryData(
                                mContext,
                                System.currentTimeMillis(),
                                /* batteryEntryList= */ null,
                                mBatteryUsageStats,
                                /* isFullChargeStart= */ false))
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
                        mContext,
                        System.currentTimeMillis(),
                        batteryEntryList,
                        mBatteryUsageStats,
                        /* isFullChargeStart= */ false);

        assertThat(valuesList).hasSize(2);
        // Verifies the ContentValues content.
        verifyBatteryEntryContentValues(0.5, valuesList.get(0));
        verifyBatteryEntryContentValues(0.0, valuesList.get(1));
        // Verifies the inserted ContentValues into content provider.
        final ContentValues[] valuesArray =
                new ContentValues[] {valuesList.get(0), valuesList.get(1)};
        verify(mMockContentResolver).bulkInsert(DatabaseUtils.BATTERY_CONTENT_URI, valuesArray);
        verify(mMockContentResolver)
                .notifyChange(DatabaseUtils.BATTERY_CONTENT_URI, /* observer= */ null);
    }

    @Test
    public void sendBatteryEntryData_emptyBatteryEntryList_sendFakeDataIntoProvider() {
        doReturn(getBatteryIntent()).when(mContext).registerReceiver(any(), any());

        final List<ContentValues> valuesList =
                DatabaseUtils.sendBatteryEntryData(
                        mContext,
                        System.currentTimeMillis(),
                        new ArrayList<>(),
                        mBatteryUsageStats,
                        /* isFullChargeStart= */ false);

        assertThat(valuesList).hasSize(1);
        verifyFakeBatteryEntryContentValues(valuesList.get(0));
        // Verifies the inserted ContentValues into content provider.
        verify(mMockContentResolver).insert(any(), any());
        verify(mMockContentResolver)
                .notifyChange(DatabaseUtils.BATTERY_CONTENT_URI, /* observer= */ null);
    }

    @Test
    public void sendBatteryEntryData_nullBatteryEntryList_sendFakeDataIntoProvider() {
        doReturn(getBatteryIntent()).when(mContext).registerReceiver(any(), any());

        final List<ContentValues> valuesList =
                DatabaseUtils.sendBatteryEntryData(
                        mContext,
                        System.currentTimeMillis(),
                        /* batteryEntryList= */ null,
                        mBatteryUsageStats,
                        /* isFullChargeStart= */ false);

        assertThat(valuesList).hasSize(1);
        verifyFakeBatteryEntryContentValues(valuesList.get(0));
        // Verifies the inserted ContentValues into content provider.
        verify(mMockContentResolver).insert(any(), any());
        verify(mMockContentResolver)
                .notifyChange(DatabaseUtils.BATTERY_CONTENT_URI, /* observer= */ null);
    }

    @Test
    public void sendBatteryEntryData_nullBatteryUsageStats_sendFakeDataIntoProvider() {
        doReturn(getBatteryIntent()).when(mContext).registerReceiver(any(), any());

        final List<ContentValues> valuesList =
                DatabaseUtils.sendBatteryEntryData(
                        mContext,
                        System.currentTimeMillis(),
                        /* batteryEntryList= */ null,
                        /* batteryUsageStats= */ null,
                        /* isFullChargeStart= */ false);

        assertThat(valuesList).hasSize(1);
        verifyFakeBatteryEntryContentValues(valuesList.get(0));
        // Verifies the inserted ContentValues into content provider.
        verify(mMockContentResolver).insert(any(), any());
        verify(mMockContentResolver)
                .notifyChange(DatabaseUtils.BATTERY_CONTENT_URI, /* observer= */ null);
    }

    @Test
    public void getAppUsageStartTimestampOfUser_emptyCursorContent_returnEarliestTimestamp() {
        final MatrixCursor cursor =
                new MatrixCursor(new String[] {AppUsageEventEntity.KEY_TIMESTAMP});
        DatabaseUtils.sFakeSupplier = () -> cursor;

        final long earliestTimestamp = 10001L;
        assertThat(
                        DatabaseUtils.getAppUsageStartTimestampOfUser(
                                mContext, /* userId= */ 0, earliestTimestamp))
                .isEqualTo(earliestTimestamp);
    }

    @Test
    public void getAppUsageStartTimestampOfUser_nullCursor_returnEarliestTimestamp() {
        DatabaseUtils.sFakeSupplier = () -> null;
        final long earliestTimestamp = 10001L;
        assertThat(
                        DatabaseUtils.getAppUsageStartTimestampOfUser(
                                mContext, /* userId= */ 0, earliestTimestamp))
                .isEqualTo(earliestTimestamp);
    }

    @Test
    public void getAppUsageStartTimestampOfUser_returnExpectedResult() {
        final long returnedTimestamp = 10001L;
        final MatrixCursor cursor =
                new MatrixCursor(new String[] {AppUsageEventEntity.KEY_TIMESTAMP});
        // Adds fake data into the cursor.
        cursor.addRow(new Object[] {returnedTimestamp});
        DatabaseUtils.sFakeSupplier = () -> cursor;

        final long earliestTimestamp1 = 1001L;
        assertThat(
                        DatabaseUtils.getAppUsageStartTimestampOfUser(
                                mContext, /* userId= */ 0, earliestTimestamp1))
                .isEqualTo(returnedTimestamp + 1);
        final long earliestTimestamp2 = 100001L;
        assertThat(
                        DatabaseUtils.getAppUsageStartTimestampOfUser(
                                mContext, /* userId= */ 0, earliestTimestamp2))
                .isEqualTo(earliestTimestamp2);
    }

    @Test
    public void getAppUsageEventForUsers_emptyCursorContent_returnEmptyMap() {
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            AppUsageEventEntity.KEY_UID,
                            AppUsageEventEntity.KEY_USER_ID,
                            AppUsageEventEntity.KEY_PACKAGE_NAME,
                            AppUsageEventEntity.KEY_TIMESTAMP,
                            AppUsageEventEntity.KEY_APP_USAGE_EVENT_TYPE
                        });
        DatabaseUtils.sFakeSupplier = () -> cursor;

        assertThat(
                        DatabaseUtils.getAppUsageEventForUsers(
                                mContext,
                                /* calendar= */ null,
                                /* userIds= */ new ArrayList<>(),
                                /* startTimestampOfLevelData= */ 0))
                .isEmpty();
    }

    @Test
    public void getAppUsageEventForUsers_nullCursor_returnEmptyMap() {
        DatabaseUtils.sFakeSupplier = () -> null;
        assertThat(
                        DatabaseUtils.getAppUsageEventForUsers(
                                mContext,
                                /* calendar= */ null,
                                /* userIds= */ new ArrayList<>(),
                                /* startTimestampOfLevelData= */ 0))
                .isEmpty();
    }

    @Test
    public void getAppUsageEventForUsers_returnExpectedMap() {
        final Long timestamp1 = 1001L;
        final Long timestamp2 = 1002L;
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            AppUsageEventEntity.KEY_UID,
                            AppUsageEventEntity.KEY_PACKAGE_NAME,
                            AppUsageEventEntity.KEY_TIMESTAMP
                        });
        // Adds fake data into the cursor.
        cursor.addRow(new Object[] {101L, "app name1", timestamp1});
        cursor.addRow(new Object[] {101L, "app name2", timestamp2});
        cursor.addRow(new Object[] {101L, "app name3", timestamp2});
        cursor.addRow(new Object[] {101L, "app name4", timestamp2});
        DatabaseUtils.sFakeSupplier = () -> cursor;

        final List<AppUsageEvent> appUsageEventList =
                DatabaseUtils.getAppUsageEventForUsers(
                        mContext,
                        /* calendar= */ null,
                        /* userIds= */ new ArrayList<>(),
                        /* startTimestampOfLevelData= */ 0);

        assertThat(appUsageEventList.get(0).getPackageName()).isEqualTo("app name1");
        assertThat(appUsageEventList.get(1).getPackageName()).isEqualTo("app name2");
        assertThat(appUsageEventList.get(2).getPackageName()).isEqualTo("app name3");
        assertThat(appUsageEventList.get(3).getPackageName()).isEqualTo("app name4");
    }

    @Test
    public void getHistoryMap_emptyCursorContent_returnEmptyMap() {
        final MatrixCursor cursor =
                new MatrixCursor(
                        new String[] {
                            BatteryHistEntry.KEY_UID,
                            BatteryHistEntry.KEY_USER_ID,
                            BatteryHistEntry.KEY_TIMESTAMP
                        });
        DatabaseUtils.sFakeSupplier = () -> cursor;

        assertThat(DatabaseUtils.getHistoryMapSinceQueryTimestamp(mContext, 0)).isEmpty();
    }

    @Test
    public void getHistoryMap_nullCursor_returnEmptyMap() {
        DatabaseUtils.sFakeSupplier = () -> null;
        assertThat(DatabaseUtils.getHistoryMapSinceQueryTimestamp(mContext, 0)).isEmpty();
    }

    @Test
    public void getHistoryMap_returnExpectedMap() {
        final Long timestamp1 = Long.valueOf(1001L);
        final Long timestamp2 = Long.valueOf(1002L);
        final MatrixCursor cursor = getMatrixCursor();
        // Adds fake data into the cursor.
        cursor.addRow(
                new Object[] {
                    "app name1", timestamp1, 1, ConvertUtils.CONSUMER_TYPE_UID_BATTERY, true
                });
        cursor.addRow(
                new Object[] {
                    "app name2", timestamp2, 2, ConvertUtils.CONSUMER_TYPE_UID_BATTERY, false
                });
        cursor.addRow(
                new Object[] {
                    "app name3", timestamp2, 3, ConvertUtils.CONSUMER_TYPE_UID_BATTERY, false
                });
        cursor.addRow(
                new Object[] {
                    "app name4", timestamp2, 4, ConvertUtils.CONSUMER_TYPE_UID_BATTERY, false
                });
        DatabaseUtils.sFakeSupplier = () -> cursor;

        final Map<Long, Map<String, BatteryHistEntry>> batteryHistMap =
                DatabaseUtils.getHistoryMapSinceQueryTimestamp(mContext, timestamp1);

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
    public void getHistoryMap_withWorkProfile_returnExpectedMap()
            throws PackageManager.NameNotFoundException {
        doReturn("com.fake.package").when(mContext).getPackageName();
        doReturn(mMockContext)
                .when(mContext)
                .createPackageContextAsUser("com.fake.package", /* flags= */ 0, UserHandle.OWNER);
        doReturn(UserHandle.CURRENT).when(mContext).getUser();
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(true).when(mUserManager).isManagedProfile();
        doReturn(UserHandle.SYSTEM).when(mUserManager).getProfileParent(UserHandle.CURRENT);

        DatabaseUtils.sFakeSupplier = () -> getMatrixCursor();

        final Map<Long, Map<String, BatteryHistEntry>> batteryHistMap =
                DatabaseUtils.getHistoryMapSinceQueryTimestamp(mContext, 0);

        assertThat(batteryHistMap).isEmpty();
    }

    @Test
    public void removeUsageSource_hasNoData() {
        DatabaseUtils.removeUsageSource(mContext);
        assertThat(
                        DatabaseUtils.getSharedPreferences(mContext)
                                .contains(DatabaseUtils.KEY_LAST_USAGE_SOURCE))
                .isFalse();
    }

    @Test
    public void removeUsageSource_hasData_deleteUsageSource() {
        final SharedPreferences sharedPreferences = DatabaseUtils.getSharedPreferences(mContext);
        sharedPreferences
                .edit()
                .putInt(DatabaseUtils.KEY_LAST_USAGE_SOURCE, USAGE_SOURCE_TASK_ROOT_ACTIVITY)
                .apply();

        DatabaseUtils.removeUsageSource(mContext);

        assertThat(
                        DatabaseUtils.getSharedPreferences(mContext)
                                .contains(DatabaseUtils.KEY_LAST_USAGE_SOURCE))
                .isFalse();
    }

    @Test
    public void getUsageSource_hasData() {
        final SharedPreferences sharedPreferences = DatabaseUtils.getSharedPreferences(mContext);
        sharedPreferences
                .edit()
                .putInt(DatabaseUtils.KEY_LAST_USAGE_SOURCE, USAGE_SOURCE_TASK_ROOT_ACTIVITY)
                .apply();

        assertThat(DatabaseUtils.getUsageSource(mContext, mUsageStatsManager))
                .isEqualTo(USAGE_SOURCE_TASK_ROOT_ACTIVITY);
    }

    @Test
    public void getUsageSource_notHasData_writeLoadedData() throws RemoteException {
        when(mUsageStatsManager.getUsageSource()).thenReturn(USAGE_SOURCE_TASK_ROOT_ACTIVITY);

        assertThat(DatabaseUtils.getUsageSource(mContext, mUsageStatsManager))
                .isEqualTo(USAGE_SOURCE_TASK_ROOT_ACTIVITY);
        assertThat(
                        DatabaseUtils.getSharedPreferences(mContext)
                                .getInt(
                                        DatabaseUtils.KEY_LAST_USAGE_SOURCE,
                                        USAGE_SOURCE_CURRENT_ACTIVITY))
                .isEqualTo(USAGE_SOURCE_TASK_ROOT_ACTIVITY);
    }

    @Test
    public void getUsageSource_throwException_writeDefaultData() throws RemoteException {
        when(mUsageStatsManager.getUsageSource()).thenThrow(new RemoteException());

        assertThat(DatabaseUtils.getUsageSource(mContext, mUsageStatsManager))
                .isEqualTo(USAGE_SOURCE_CURRENT_ACTIVITY);
        assertThat(
                        DatabaseUtils.getSharedPreferences(mContext)
                                .getInt(
                                        DatabaseUtils.KEY_LAST_USAGE_SOURCE,
                                        USAGE_SOURCE_CURRENT_ACTIVITY))
                .isEqualTo(USAGE_SOURCE_CURRENT_ACTIVITY);
    }

    @Test
    public void recordDateTime_writeDataIntoSharedPreferences() {
        final String preferenceKey = "test_preference_key";
        DatabaseUtils.recordDateTime(mContext, preferenceKey);

        assertThat(DatabaseUtils.getSharedPreferences(mContext).contains(preferenceKey)).isTrue();
    }

    @Test
    public void dump_dumpExpectedData() {
        DatabaseUtils.recordDateTime(mContext, Intent.ACTION_BATTERY_LEVEL_CHANGED);
        DatabaseUtils.recordDateTime(
                mContext, BatteryUsageBroadcastReceiver.ACTION_BATTERY_UNPLUGGING);
        DatabaseUtils.recordDateTime(mContext, DatabaseUtils.KEY_LAST_LOAD_FULL_CHARGE_TIME);
        DatabaseUtils.recordDateTime(mContext, DatabaseUtils.KEY_LAST_UPLOAD_FULL_CHARGE_TIME);
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        DatabaseUtils.dump(mContext, printWriter);

        String dumpContent = stringWriter.toString();
        assertThat(dumpContent.contains("BatteryLevelChanged")).isTrue();
        assertThat(dumpContent.contains("BatteryUnplugging")).isTrue();
        assertThat(dumpContent.contains("ClearBatteryCacheData")).isTrue();
        assertThat(dumpContent.contains("LastLoadFullChargeTime")).isTrue();
        assertThat(dumpContent.contains("LastUploadFullChargeTime")).isTrue();
    }

    @Test
    public void loadFromContentProvider_workProfile_transferToUserProfile() throws Exception {
        // Test to verify b/297036263
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(true).when(mUserManager).isManagedProfile();
        doReturn(UserHandle.CURRENT).when(mContext).getUser();
        doReturn(UserHandle.SYSTEM).when(mUserManager).getProfileParent(UserHandle.CURRENT);

        DatabaseUtils.loadFromContentProvider(mContext, Uri.EMPTY, null, cursor -> 1);

        verify(mContext).createPackageContextAsUser(anyString(), anyInt(), any());
    }

    private static void verifyBatteryEntryContentValues(
            double consumedPower, ContentValues values) {
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

    private static void verifyFakeBatteryEntryContentValues(ContentValues values) {
        final BatteryInformation batteryInformation =
                ConvertUtils.getBatteryInformation(
                        values, BatteryHistEntry.KEY_BATTERY_INFORMATION);
        final DeviceBatteryState deviceBatteryState = batteryInformation.getDeviceBatteryState();
        assertThat(deviceBatteryState.getBatteryLevel()).isEqualTo(20);
        assertThat(deviceBatteryState.getBatteryStatus())
                .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(deviceBatteryState.getBatteryHealth())
                .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
        assertThat(values.getAsString("packageName")).isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
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
                    BatteryHistEntry.KEY_CONSUMER_TYPE,
                    BatteryHistEntry.KEY_IS_FULL_CHARGE_CYCLE_START
                });
    }
}
