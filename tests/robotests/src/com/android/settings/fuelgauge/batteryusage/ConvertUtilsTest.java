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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.os.BatteryManager;
import android.os.BatteryUsageStats;
import android.os.LocaleList;
import android.os.UserHandle;
import android.text.format.DateUtils;

import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
    private BatteryEntry mMockBatteryEntry;

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
    public void convertToContentValues_returnsExpectedContentValues() {
        final int expectedType = 3;
        when(mMockBatteryEntry.getUid()).thenReturn(1001);
        when(mMockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mMockBatteryEntry.getDefaultPackageName())
                .thenReturn("com.google.android.settings.battery");
        when(mMockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mMockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        mMockBatteryEntry.mPercent = 0.3;
        when(mMockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mMockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mMockBatteryEntry.getPowerComponentId()).thenReturn(expectedType);
        when(mMockBatteryEntry.getConsumerType())
                .thenReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);

        final ContentValues values =
                ConvertUtils.convertToContentValues(
                        mMockBatteryEntry,
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
    public void convertToContentValues_nullBatteryEntry_returnsExpectedContentValues() {
        final ContentValues values =
                ConvertUtils.convertToContentValues(
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
    public void convertToBatteryHistEntry_returnsExpectedResult() {
        final int expectedType = 3;
        when(mMockBatteryEntry.getUid()).thenReturn(1001);
        when(mMockBatteryEntry.getLabel()).thenReturn("Settings");
        when(mMockBatteryEntry.getDefaultPackageName())
                .thenReturn("com.android.settings.battery");
        when(mMockBatteryEntry.isHidden()).thenReturn(true);
        when(mBatteryUsageStats.getConsumedPower()).thenReturn(5.1);
        when(mMockBatteryEntry.getConsumedPower()).thenReturn(1.1);
        mMockBatteryEntry.mPercent = 0.3;
        when(mMockBatteryEntry.getTimeInForegroundMs()).thenReturn(1234L);
        when(mMockBatteryEntry.getTimeInBackgroundMs()).thenReturn(5689L);
        when(mMockBatteryEntry.getPowerComponentId()).thenReturn(expectedType);
        when(mMockBatteryEntry.getConsumerType())
                .thenReturn(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);

        final BatteryHistEntry batteryHistEntry =
                ConvertUtils.convertToBatteryHistEntry(
                        mMockBatteryEntry,
                        mBatteryUsageStats);

        assertThat(batteryHistEntry.mUid).isEqualTo(1001L);
        assertThat(batteryHistEntry.mUserId)
                .isEqualTo(UserHandle.getUserId(1001));
        assertThat(batteryHistEntry.mAppLabel)
                .isEqualTo("Settings");
        assertThat(batteryHistEntry.mPackageName)
                .isEqualTo("com.android.settings.battery");
        assertThat(batteryHistEntry.mIsHidden).isTrue();
        assertThat(batteryHistEntry.mBootTimestamp)
                .isEqualTo(0L);
        assertThat(batteryHistEntry.mTimestamp).isEqualTo(0L);
        assertThat(batteryHistEntry.mZoneId)
                .isEqualTo(TimeZone.getDefault().getID());
        assertThat(batteryHistEntry.mTotalPower).isEqualTo(5.1);
        assertThat(batteryHistEntry.mConsumePower).isEqualTo(1.1);
        assertThat(batteryHistEntry.mPercentOfTotal).isEqualTo(0.3);
        assertThat(batteryHistEntry.mForegroundUsageTimeInMs)
                .isEqualTo(1234L);
        assertThat(batteryHistEntry.mBackgroundUsageTimeInMs)
                .isEqualTo(5689L);
        assertThat(batteryHistEntry.mDrainType).isEqualTo(expectedType);
        assertThat(batteryHistEntry.mConsumerType)
                .isEqualTo(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        assertThat(batteryHistEntry.mBatteryLevel).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryStatus).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryHealth).isEqualTo(0);
    }

    @Test
    public void convertToBatteryHistEntry_nullBatteryEntry_returnsExpectedResult() {
        final BatteryHistEntry batteryHistEntry =
                ConvertUtils.convertToBatteryHistEntry(
                        /*entry=*/ null,
                        /*batteryUsageStats=*/ null);

        assertThat(batteryHistEntry.mBootTimestamp)
                .isEqualTo(0L);
        assertThat(batteryHistEntry.mTimestamp)
                .isEqualTo(0);
        assertThat(batteryHistEntry.mZoneId)
                .isEqualTo(TimeZone.getDefault().getID());
        assertThat(batteryHistEntry.mBatteryLevel).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryStatus).isEqualTo(0);
        assertThat(batteryHistEntry.mBatteryHealth).isEqualTo(0);
        assertThat(batteryHistEntry.mPackageName)
                .isEqualTo(ConvertUtils.FAKE_PACKAGE_NAME);
    }

    @Test
    public void getIndexedUsageMap_nullOrEmptyHistoryMap_returnEmptyCollection() {
        final int timeSlotSize = 2;
        final long[] batteryHistoryKeys = new long[]{101L, 102L, 103L, 104L, 105L};

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
    public void getIndexedUsageMap_returnsExpectedResult() {
        // Creates the fake testing data.
        final int timeSlotSize = 2;
        final long[] batteryHistoryKeys = new long[]{generateTimestamp(0), generateTimestamp(1),
                generateTimestamp(2), generateTimestamp(3), generateTimestamp(4)};
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
        when(mPowerUsageFeatureProvider.getBatteryHistorySinceLastFullCharge(mContext))
                .thenReturn(batteryHistoryMap);

        final List<BatteryDiffEntry> batteryDiffEntryList =
                BatteryChartPreferenceController.getAppBatteryUsageData(mContext);

        assertThat(batteryDiffEntryList).isNotEmpty();
        final BatteryDiffEntry resultEntry = batteryDiffEntryList.get(0);
        assertThat(resultEntry.getPackageName()).isEqualTo("package2");
    }

    @Test
    public void getIndexedUsageMap_usageTimeExceed_returnsExpectedResult() {
        final int timeSlotSize = 1;
        final long[] batteryHistoryKeys = new long[]{101L, 102L, 103L};
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

    @Test
    public void getIndexedUsageMap_hideBackgroundUsageTime_returnsExpectedResult() {
        final long[] batteryHistoryKeys = new long[]{101L, 102L, 103L};
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
        when(mPowerUsageFeatureProvider.getHideBackgroundUsageTimeSet(mContext))
                .thenReturn(new HashSet(Arrays.asList((CharSequence) "package3")));

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

    @Test
    public void resolveMultiUsersData_replaceOtherUsersItemWithExpectedEntry() {
        final int currentUserId = mContext.getUserId();
        final Map<Integer, List<BatteryDiffEntry>> entryMap = new HashMap<>();
        // Without other users time slot.
        entryMap.put(0, Arrays.asList(
                createBatteryDiffEntry(
                        currentUserId,
                        ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /*consumePercentage=*/ 50)));
        // With other users time slot.
        final List<BatteryDiffEntry> withOtherUsersList = new ArrayList<>();
        entryMap.put(1, withOtherUsersList);
        withOtherUsersList.add(
                createBatteryDiffEntry(
                        currentUserId + 1,
                        ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY,
                        /*consumePercentage=*/ 20));
        withOtherUsersList.add(
                createBatteryDiffEntry(
                        currentUserId + 2,
                        ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /*consumePercentage=*/ 30));
        withOtherUsersList.add(
                createBatteryDiffEntry(
                        currentUserId + 3,
                        ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /*consumePercentage=*/ 40));

        ConvertUtils.resolveMultiUsersData(mContext, entryMap);

        assertThat(entryMap.get(0).get(0).getPercentOfTotal()).isEqualTo(50);
        // Asserts with other users items.
        final List<BatteryDiffEntry> entryList = entryMap.get(1);
        assertThat(entryList).hasSize(2);
        assertBatteryDiffEntry(
                entryList.get(0),
                currentUserId + 1,
                /*uid=*/ 0,
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY,
                /*consumePercentage=*/ 20);
        assertBatteryDiffEntry(
                entryList.get(1),
                BatteryUtils.UID_OTHER_USERS,
                BatteryUtils.UID_OTHER_USERS,
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*consumePercentage=*/ 70);
    }

    private BatteryDiffEntry createBatteryDiffEntry(
            long userId, int counsumerType, double consumePercentage) {
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_USER_ID, userId);
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, counsumerType);
        final BatteryDiffEntry batteryDiffEntry =
                new BatteryDiffEntry(
                        mContext,
                        /*foregroundUsageTimeInMs=*/ 0,
                        /*backgroundUsageTimeInMs=*/ 0,
                        /*consumePower=*/ consumePercentage,
                        new BatteryHistEntry(values));
        batteryDiffEntry.setTotalConsumePower(100f);
        return batteryDiffEntry;
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
            BatteryDiffEntry entry, long userId, long uid, int counsumerType,
            double consumePercentage) {
        assertThat(entry.mBatteryHistEntry.mUid).isEqualTo(uid);
        assertThat(entry.mBatteryHistEntry.mUserId).isEqualTo(userId);
        assertThat(entry.mBatteryHistEntry.mConsumerType).isEqualTo(counsumerType);
        assertThat(entry.getPercentOfTotal()).isEqualTo(consumePercentage);
    }

    private static void assertBatteryDiffEntry(
            BatteryDiffEntry entry, int percentOfTotal,
            long foregroundUsageTimeInMs, long backgroundUsageTimeInMs) {
        assertThat((int) entry.getPercentOfTotal()).isEqualTo(percentOfTotal);
        assertThat(entry.mForegroundUsageTimeInMs).isEqualTo(foregroundUsageTimeInMs);
        assertThat(entry.mBackgroundUsageTimeInMs).isEqualTo(backgroundUsageTimeInMs);
    }

    private static Long generateTimestamp(int index) {
        // "2021-04-23 07:00:00 UTC" + index hours
        return 1619247600000L + index * DateUtils.HOUR_IN_MILLIS;
    }
}
