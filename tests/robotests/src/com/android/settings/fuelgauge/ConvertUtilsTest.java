/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.LocaleList;
import android.os.UserHandle;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class ConvertUtilsTest {

    private Context mContext;

    @Mock
    private BatteryUsageStats mBatteryUsageStats;
    @Mock
    private BatteryEntry mockBatteryEntry;

    private FakeFeatureFactory mFeatureFactory;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mPowerUsageFeatureProvider = mFeatureFactory.powerUsageFeatureProvider;
    }

    @Test
    public void testConvert_returnsExpectedContentValues() {
        final int expectedType = 3;
        when(mockBatteryEntry.getUid()).thenReturn(1001);
        when(mockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mockBatteryEntry.getDefaultPackageName())
            .thenReturn("com.google.android.settings.battery");
        when(mockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        mockBatteryEntry.percent = 0.3;
        when(mockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mockBatteryEntry.getPowerComponentId()).thenReturn(expectedType);
        when(mockBatteryEntry.getConsumerType())
            .thenReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);

        final ContentValues values =
            ConvertUtils.convert(
                mockBatteryEntry,
                mBatteryUsageStats,
                /*batteryLevel=*/ 12,
                /*batteryStatus=*/ BatteryManager.BATTERY_STATUS_FULL,
                /*batteryHealth=*/ BatteryManager.BATTERY_HEALTH_COLD,
                /*bootTimestamp=*/ 101L,
                /*timestamp=*/ 10001L);

        assertThat(values.getAsLong(BatteryHistEntry.KEY_UID)).isEqualTo(1001L);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_USER_ID))
            .isEqualTo(UserHandle.getUserId(1001));
        assertThat(values.getAsString(BatteryHistEntry.KEY_APP_LABEL))
            .isEqualTo("Settings");
        assertThat(values.getAsString(BatteryHistEntry.KEY_PACKAGE_NAME))
            .isEqualTo("com.google.android.settings.battery");
        assertThat(values.getAsBoolean(BatteryHistEntry.KEY_IS_HIDDEN)).isTrue();
        assertThat(values.getAsLong(BatteryHistEntry.KEY_BOOT_TIMESTAMP))
            .isEqualTo(101L);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_TIMESTAMP)).isEqualTo(10001L);
        assertThat(values.getAsString(BatteryHistEntry.KEY_ZONE_ID))
            .isEqualTo(TimeZone.getDefault().getID());
        assertThat(values.getAsDouble(BatteryHistEntry.KEY_TOTAL_POWER)).isEqualTo(5.1);
        assertThat(values.getAsDouble(BatteryHistEntry.KEY_CONSUME_POWER)).isEqualTo(1.1);
        assertThat(values.getAsDouble(BatteryHistEntry.KEY_PERCENT_OF_TOTAL)).isEqualTo(0.3);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_FOREGROUND_USAGE_TIME))
            .isEqualTo(1234L);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_BACKGROUND_USAGE_TIME))
            .isEqualTo(5689L);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_DRAIN_TYPE)).isEqualTo(expectedType);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_CONSUMER_TYPE))
            .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_BATTERY_LEVEL)).isEqualTo(12);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_BATTERY_STATUS))
            .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_BATTERY_HEALTH))
            .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
    }

    @Test
    public void testConvert_nullBatteryEntry_returnsExpectedContentValues() {
        final ContentValues values =
            ConvertUtils.convert(
                /*entry=*/ null,
                /*batteryUsageStats=*/ null,
                /*batteryLevel=*/ 12,
                /*batteryStatus=*/ BatteryManager.BATTERY_STATUS_FULL,
                /*batteryHealth=*/ BatteryManager.BATTERY_HEALTH_COLD,
                /*bootTimestamp=*/ 101L,
                /*timestamp=*/ 10001L);

        assertThat(values.getAsLong(BatteryHistEntry.KEY_BOOT_TIMESTAMP))
            .isEqualTo(101L);
        assertThat(values.getAsLong(BatteryHistEntry.KEY_TIMESTAMP))
            .isEqualTo(10001L);
        assertThat(values.getAsString(BatteryHistEntry.KEY_ZONE_ID))
            .isEqualTo(TimeZone.getDefault().getID());
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_BATTERY_LEVEL)).isEqualTo(12);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_BATTERY_STATUS))
            .isEqualTo(BatteryManager.BATTERY_STATUS_FULL);
        assertThat(values.getAsInteger(BatteryHistEntry.KEY_BATTERY_HEALTH))
            .isEqualTo(BatteryManager.BATTERY_HEALTH_COLD);
        assertThat(values.getAsString(BatteryHistEntry.KEY_PACKAGE_NAME))
            .isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    @Test
    public void testGetIndexedUsageMap_nullOrEmptyHistoryMap_returnEmptyCollection() {
        final int timeSlotSize = 2;
        final long[] batteryHistoryKeys = new long[] {101L, 102L, 103L, 104L, 105L};

        assertThat(ConvertUtils.getIndexedUsageMap(
                mContext, timeSlotSize, batteryHistoryKeys,
                /*batteryHistoryMap=*/ null, /*purgeLowPercentageAndFakeData=*/ true))
            .isEmpty();
        assertThat(ConvertUtils.getIndexedUsageMap(
                mContext, timeSlotSize, batteryHistoryKeys,
                new HashMap<Long, Map<String, BatteryHistEntry>>(),
                /*purgeLowPercentageAndFakeData=*/ true))
            .isEmpty();
    }
    @Test
    public void testGetIndexedUsageMap_returnsExpectedResult() {
        // Creates the fake testing data.
        final int timeSlotSize = 2;
        final long[] batteryHistoryKeys = new long[] {101L, 102L, 103L, 104L, 105L};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
            new HashMap<>();
        final BatteryHistEntry fakeEntry = createBatteryHistEntry(
            ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", 0, 0L, 0L, 0L);
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        BatteryHistEntry entry = createBatteryHistEntry(
            "package1", "label1", 5.0, 1L, 10L, 20L);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[0]), entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[1]), entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
            "package2", "label2", 10.0, 2L, 15L, 25L);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[2]), entryMap);
        // Adds the index = 3 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
            "package2", "label2", 15.0, 2L, 25L, 35L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
            "package3", "label3", 5.0, 3L, 5L, 5L);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[3]), entryMap);
        // Adds the index = 4 data.
        entryMap = new HashMap<>();
        entry = createBatteryHistEntry(
            "package2", "label2", 30.0, 2L, 30L, 40L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
            "package2", "label2", 75.0, 4L, 40L, 50L);
        entryMap.put(entry.getKey(), entry);
        entry = createBatteryHistEntry(
            "package3", "label3", 5.0, 3L, 5L, 5L);
        entryMap.put(entry.getKey(), entry);
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[4]), entryMap);

        final Map<Integer, List<BatteryDiffEntry>> resultMap =
            ConvertUtils.getIndexedUsageMap(
                mContext, timeSlotSize, batteryHistoryKeys, batteryHistoryMap,
                /*purgeLowPercentageAndFakeData=*/ false);

        assertThat(resultMap).hasSize(3);
        // Verifies the first timestamp result.
        List<BatteryDiffEntry> entryList = resultMap.get(Integer.valueOf(0));
        assertThat(entryList).hasSize(1);
        assertBatteryDiffEntry(entryList.get(0), 100, 15L, 25L);
        // Verifies the second timestamp result.
        entryList = resultMap.get(Integer.valueOf(1));
        assertThat(entryList).hasSize(3);
        assertBatteryDiffEntry(entryList.get(1), 5, 5L, 5L);
        assertBatteryDiffEntry(entryList.get(2), 75, 40L, 50L);
        assertBatteryDiffEntry(entryList.get(0), 20, 15L, 15L);
        // Verifies the last 24 hours aggregate result.
        entryList = resultMap.get(Integer.valueOf(-1));
        assertThat(entryList).hasSize(3);
        assertBatteryDiffEntry(entryList.get(1), 4, 5L, 5L);
        assertBatteryDiffEntry(entryList.get(2), 68, 40L, 50L);
        assertBatteryDiffEntry(entryList.get(0), 27, 30L, 40L);

        // Test getIndexedUsageMap() with purged data.
        ConvertUtils.PERCENTAGE_OF_TOTAL_THRESHOLD = 50;
        final Map<Integer, List<BatteryDiffEntry>> purgedResultMap =
            ConvertUtils.getIndexedUsageMap(
                mContext, timeSlotSize, batteryHistoryKeys, batteryHistoryMap,
                 /*purgeLowPercentageAndFakeData=*/ true);

        assertThat(purgedResultMap).hasSize(3);
        // Verifies the first timestamp result.
        entryList = purgedResultMap.get(Integer.valueOf(0));
        assertThat(entryList).hasSize(1);
        // Verifies the second timestamp result.
        entryList = purgedResultMap.get(Integer.valueOf(1));
        assertThat(entryList).hasSize(1);
        assertBatteryDiffEntry(entryList.get(0), 75, 40L, 50L);
        // Verifies the last 24 hours aggregate result.
        entryList = purgedResultMap.get(Integer.valueOf(-1));
        assertThat(entryList).hasSize(1);
        // Verifies the fake data is cleared out.
        assertThat(entryList.get(0).getPackageName())
            .isNotEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);

        // Adds lacked data into the battery history map.
        final int remainingSize = 25 - batteryHistoryKeys.length;
        for (int index = 0; index < remainingSize; index++) {
            batteryHistoryMap.put(105L + index + 1, new HashMap<>());
        }
        when(mPowerUsageFeatureProvider.getBatteryHistory(mContext))
            .thenReturn(batteryHistoryMap);

        final List<BatteryDiffEntry> batteryDiffEntryList =
            BatteryChartPreferenceController.getBatteryLast24HrUsageData(mContext);

        assertThat(batteryDiffEntryList).isNotEmpty();
        final BatteryDiffEntry resultEntry = batteryDiffEntryList.get(0);
        assertThat(resultEntry.getPackageName()).isEqualTo("package2");
    }

    @Test
    public void testGetIndexedUsageMap_usageTimeExceed_returnsExpectedResult() {
        final int timeSlotSize = 1;
        final long[] batteryHistoryKeys = new long[] {101L, 102L, 103L};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
            new HashMap<>();
        final BatteryHistEntry fakeEntry = createBatteryHistEntry(
            ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", 0, 0L, 0L, 0L);
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[0]), entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[1]), entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        final BatteryHistEntry entry = createBatteryHistEntry(
            "package3", "label3", 500, 5L, 3600000L, 7200000L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[2]), entryMap);

        final Map<Integer, List<BatteryDiffEntry>> purgedResultMap =
            ConvertUtils.getIndexedUsageMap(
                mContext, timeSlotSize, batteryHistoryKeys, batteryHistoryMap,
                /*purgeLowPercentageAndFakeData=*/ true);

        assertThat(purgedResultMap).hasSize(2);
        final List<BatteryDiffEntry> entryList = purgedResultMap.get(0);
        assertThat(entryList).hasSize(1);
        // Verifies the clipped usage time.
        final float ratio = (float) (7200) / (float) (3600 + 7200);
        final BatteryDiffEntry resultEntry = entryList.get(0);
        assertThat(resultEntry.mForegroundUsageTimeInMs)
            .isEqualTo(Math.round(entry.mForegroundUsageTimeInMs * ratio));
        assertThat(resultEntry.mBackgroundUsageTimeInMs)
            .isEqualTo(Math.round(entry.mBackgroundUsageTimeInMs * ratio));
        assertThat(resultEntry.mConsumePower)
            .isEqualTo(entry.mConsumePower * ratio);
    }

    @Ignore
    @Test
    public void testUtcToLocalTime_returnExpectedResult() {
        ConvertUtils.sZoneId = null;
        ConvertUtils.sLocale = null;
        final long timestamp = 1619196786769L;
        final String expectedZoneId = "America/Los_Angeles";
        ConvertUtils.sSimpleDateFormat = null;
        // Invokes the method first to create the SimpleDateFormat.
        ConvertUtils.utcToLocalTime(mContext, /*timestamp=*/ 0);
        ConvertUtils.sSimpleDateFormat
            .setTimeZone(TimeZone.getTimeZone(expectedZoneId));
        mContext.getResources().getConfiguration().setLocales(
            new LocaleList(new Locale("en_US")));

        assertThat(ConvertUtils.utcToLocalTime(mContext, timestamp))
            .isEqualTo("Apr 24,2021 00:53:06");
        assertThat(ConvertUtils.sZoneId).isNotEqualTo(expectedZoneId);
        assertThat(ConvertUtils.sLocale).isEqualTo(new Locale("en_US"));
    }

    @Ignore
    @Test
    public void testUtcToLocalTimeHour_12HourFormat_returnExpectedResult() {
        ConvertUtils.sZoneIdForHour = null;
        ConvertUtils.sLocaleForHour = null;
        final long timestamp = 1619000086769L;
        final String expectedZoneId = "America/Los_Angeles";
        ConvertUtils.sSimpleDateFormatForHour = null;
        // Invokes the method first to create the SimpleDateFormat.
        ConvertUtils.utcToLocalTimeHour(
            mContext, /*timestamp=*/ 0, /*is24HourFormat=*/ false);
        ConvertUtils.sSimpleDateFormatForHour
            .setTimeZone(TimeZone.getTimeZone(expectedZoneId));
        mContext.getResources().getConfiguration().setLocales(
            new LocaleList(new Locale("en_US")));

        assertThat(ConvertUtils.utcToLocalTimeHour(
            mContext, timestamp, /*is24HourFormat=*/ false)).isEqualTo("6");
        assertThat(ConvertUtils.sZoneIdForHour).isNotEqualTo(expectedZoneId);
        assertThat(ConvertUtils.sLocaleForHour).isEqualTo(new Locale("en_US"));
    }

    @Ignore
    @Test
    public void testUtcToLocalTimeHour_24HourFormat_returnExpectedResult() {
        ConvertUtils.sZoneIdForHour = null;
        ConvertUtils.sLocaleForHour = null;
        final long timestamp = 1619000086769L;
        final String expectedZoneId = "America/Los_Angeles";
        ConvertUtils.sSimpleDateFormatForHour = null;
        // Invokes the method first to create the SimpleDateFormat.
        ConvertUtils.utcToLocalTimeHour(
            mContext, /*timestamp=*/ 0, /*is24HourFormat=*/ false);
        ConvertUtils.sSimpleDateFormatForHour
            .setTimeZone(TimeZone.getTimeZone(expectedZoneId));
        mContext.getResources().getConfiguration().setLocales(
            new LocaleList(new Locale("en_US")));

        assertThat(ConvertUtils.utcToLocalTimeHour(
            mContext, timestamp, /*is24HourFormat=*/ true)).isEqualTo("18");
        assertThat(ConvertUtils.sZoneIdForHour).isNotEqualTo(expectedZoneId);
        assertThat(ConvertUtils.sLocaleForHour).isEqualTo(new Locale("en_US"));
    }

    @Test
    public void testGetIndexedUsageMap_hideBackgroundUsageTime_returnsExpectedResult() {
        final long[] batteryHistoryKeys = new long[] {101L, 102L, 103L};
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap = new HashMap<>();
        final BatteryHistEntry fakeEntry = createBatteryHistEntry(
            ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", 0, 0L, 0L, 0L);
        // Adds the index = 0 data.
        Map<String, BatteryHistEntry> entryMap = new HashMap<>();
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[0]), entryMap);
        // Adds the index = 1 data.
        entryMap = new HashMap<>();
        entryMap.put(fakeEntry.getKey(), fakeEntry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[1]), entryMap);
        // Adds the index = 2 data.
        entryMap = new HashMap<>();
        final BatteryHistEntry entry = createBatteryHistEntry(
            "package3", "label3", 500, 5L, 3600000L, 7200000L);
        entryMap.put(entry.getKey(), entry);
        batteryHistoryMap.put(Long.valueOf(batteryHistoryKeys[2]), entryMap);
        when(mPowerUsageFeatureProvider.getHideBackgroundUsageTimeList(mContext))
            .thenReturn(Arrays.asList((CharSequence) "package3"));

        final Map<Integer, List<BatteryDiffEntry>> purgedResultMap =
            ConvertUtils.getIndexedUsageMap(
                mContext, /*timeSlotSize=*/ 1, batteryHistoryKeys, batteryHistoryMap,
                /*purgeLowPercentageAndFakeData=*/ true);

        final BatteryDiffEntry resultEntry = purgedResultMap.get(0).get(0);
        assertThat(resultEntry.mBackgroundUsageTimeInMs).isEqualTo(0);
    }

    @Test
    public void getLocale_nullContext_returnDefaultLocale() {
        assertThat(ConvertUtils.getLocale(/*context=*/ null))
            .isEqualTo(Locale.getDefault());
    }

    @Test
    public void getLocale_nullLocaleList_returnDefaultLocale() {
        mContext.getResources().getConfiguration().setLocales(null);
        assertThat(ConvertUtils.getLocale(mContext)).isEqualTo(Locale.getDefault());
    }

    @Test
    public void getLocale_emptyLocaleList_returnDefaultLocale() {
        mContext.getResources().getConfiguration().setLocales(new LocaleList());
        assertThat(ConvertUtils.getLocale(mContext)).isEqualTo(Locale.getDefault());
    }

    private static BatteryHistEntry createBatteryHistEntry(
            String packageName, String appLabel, double consumePower,
            long uid, long foregroundUsageTimeInMs, long backgroundUsageTimeInMs) {
        // Only insert required fields.
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, packageName);
        values.put(BatteryHistEntry.KEY_APP_LABEL, appLabel);
        values.put(BatteryHistEntry.KEY_UID, Long.valueOf(uid));
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE,
            Integer.valueOf(ConvertUtils.CONSUMER_TYPE_UID_BATTERY));
        values.put(BatteryHistEntry.KEY_CONSUME_POWER, consumePower);
        values.put(BatteryHistEntry.KEY_FOREGROUND_USAGE_TIME,
            Long.valueOf(foregroundUsageTimeInMs));
        values.put(BatteryHistEntry.KEY_BACKGROUND_USAGE_TIME,
            Long.valueOf(backgroundUsageTimeInMs));
        return new BatteryHistEntry(values);
    }

    private static void assertBatteryDiffEntry(
            BatteryDiffEntry entry, int percentOfTotal,
            long foregroundUsageTimeInMs, long backgroundUsageTimeInMs) {
        assertThat((int) entry.getPercentOfTotal()).isEqualTo(percentOfTotal);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(foregroundUsageTimeInMs);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(backgroundUsageTimeInMs);
    }
}
