/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.annotation.IntDef;
import android.content.ContentValues;
import android.os.BatteryConsumer;
import android.os.BatteryUsageStats;
import android.content.Context;
import android.os.SystemBatteryConsumer;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/** A utility class to convert data into another types. */
public final class ConvertUtils {
    private static final String TAG = "ConvertUtils";
    private static final Map<String, BatteryHistEntry> EMPTY_BATTERY_MAP = new HashMap<>();
    private static final BatteryHistEntry EMPTY_BATTERY_HIST_ENTRY =
        new BatteryHistEntry(new ContentValues());

    @VisibleForTesting
    static double PERCENTAGE_OF_TOTAL_THRESHOLD = 1f;

    /** Invalid system battery consumer drain type. */
    public static final int INVALID_DRAIN_TYPE = -1;
    /** A fake package name to represent no BatteryEntry data. */
    public static final String FAKE_PACKAGE_NAME = "fake_package";

    @IntDef(prefix = {"CONSUMER_TYPE"}, value = {
        CONSUMER_TYPE_UNKNOWN,
        CONSUMER_TYPE_UID_BATTERY,
        CONSUMER_TYPE_USER_BATTERY,
        CONSUMER_TYPE_SYSTEM_BATTERY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public static @interface ConsumerType {}

    public static final int CONSUMER_TYPE_UNKNOWN = 0;
    public static final int CONSUMER_TYPE_UID_BATTERY = 1;
    public static final int CONSUMER_TYPE_USER_BATTERY = 2;
    public static final int CONSUMER_TYPE_SYSTEM_BATTERY = 3;

    private static String sZoneId;
    private static String sZoneIdForHour;

    @VisibleForTesting
    static SimpleDateFormat sSimpleDateFormat;
    @VisibleForTesting
    static SimpleDateFormat sSimpleDateFormatForHour;

    private ConvertUtils() {}

    /** Gets consumer type from {@link BatteryConsumer}. */
    @ConsumerType
    public static int getConsumerType(BatteryConsumer consumer) {
        if (consumer instanceof UidBatteryConsumer) {
            return CONSUMER_TYPE_UID_BATTERY;
        } else if (consumer instanceof UserBatteryConsumer) {
            return CONSUMER_TYPE_USER_BATTERY;
        } else if (consumer instanceof SystemBatteryConsumer) {
            return CONSUMER_TYPE_SYSTEM_BATTERY;
        } else {
          return CONSUMER_TYPE_UNKNOWN;
        }
    }

    /** Gets battery drain type for {@link SystemBatteryConsumer}. */
    public static int getDrainType(BatteryConsumer consumer) {
        if (consumer instanceof SystemBatteryConsumer) {
            return ((SystemBatteryConsumer) consumer).getDrainType();
        }
        return INVALID_DRAIN_TYPE;
    }

    public static ContentValues convert(
            BatteryEntry entry,
            BatteryUsageStats batteryUsageStats,
            int batteryLevel,
            int batteryStatus,
            int batteryHealth,
            long timestamp) {
        final ContentValues values = new ContentValues();
        if (entry != null && batteryUsageStats != null) {
            values.put("uid", Long.valueOf(entry.getUid()));
            values.put("userId",
                Long.valueOf(UserHandle.getUserId(entry.getUid())));
            values.put("appLabel", entry.getLabel());
            values.put("packageName", entry.getDefaultPackageName());
            values.put("isHidden", Boolean.valueOf(entry.isHidden()));
            values.put("totalPower",
                Double.valueOf(batteryUsageStats.getConsumedPower()));
            values.put("consumePower", Double.valueOf(entry.getConsumedPower()));
            values.put("percentOfTotal", Double.valueOf(entry.percent));
            values.put("foregroundUsageTimeInMs",
                Long.valueOf(entry.getTimeInForegroundMs()));
            values.put("backgroundUsageTimeInMs",
                Long.valueOf(entry.getTimeInBackgroundMs()));
            values.put("drainType", getDrainType(entry.getBatteryConsumer()));
            values.put("consumerType", getConsumerType(entry.getBatteryConsumer()));
        } else {
            values.put("packageName", FAKE_PACKAGE_NAME);
        }
        values.put("timestamp", Long.valueOf(timestamp));
        values.put("zoneId", TimeZone.getDefault().getID());
        values.put("batteryLevel", Integer.valueOf(batteryLevel));
        values.put("batteryStatus", Integer.valueOf(batteryStatus));
        values.put("batteryHealth", Integer.valueOf(batteryHealth));
        return values;
    }

    /** Converts UTC timestamp to human readable local time string. */
    public static String utcToLocalTime(long timestamp) {
        final String currentZoneId = TimeZone.getDefault().getID();
        if (!currentZoneId.equals(sZoneId) || sSimpleDateFormat == null) {
            sZoneId = currentZoneId;
            sSimpleDateFormat =
                new SimpleDateFormat("MMM dd,yyyy HH:mm:ss", Locale.ENGLISH);
        }
        return sSimpleDateFormat.format(new Date(timestamp));
    }

    /** Converts UTC timestamp to local time hour data. */
    public static String utcToLocalTimeHour(long timestamp) {
        final String currentZoneId = TimeZone.getDefault().getID();
        if (!currentZoneId.equals(sZoneIdForHour) || sSimpleDateFormatForHour == null) {
            sZoneIdForHour = currentZoneId;
            sSimpleDateFormatForHour = new SimpleDateFormat("h aa", Locale.ENGLISH);
        }
        return sSimpleDateFormatForHour.format(new Date(timestamp))
            .toLowerCase(Locale.getDefault());
    }

    /** Gets indexed battery usage data for each corresponding time slot. */
    public static Map<Integer, List<BatteryDiffEntry>> getIndexedUsageMap(
            final Context context,
            final int timeSlotSize,
            final long[] batteryHistoryKeys,
            final Map<Long, List<BatteryHistEntry>> batteryHistoryMap,
            final boolean purgeLowPercentageData) {
        final Map<Integer, List<BatteryDiffEntry>> resultMap = new HashMap<>();
        // Generates a temporary map to calculate diff usage data, which converts the inputted
        // List<BatteryDiffEntry> into Map<String, BatteryHistEntry> with the key comes from
        // the BatteryHistEntry.getKey() method.
        final Map<Long, Map<String, BatteryHistEntry>> newBatteryHistoryMap = new HashMap<>();
        for (int index = 0; index < batteryHistoryKeys.length; index++) {
            final Long timestamp = Long.valueOf(batteryHistoryKeys[index]);
            final List<BatteryHistEntry> entries = batteryHistoryMap.get(timestamp);
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            final Map<String, BatteryHistEntry> slotBatteryHistDataMap = new HashMap<>();
            for (BatteryHistEntry entry : entries) {
                // Excludes auto-generated fake BatteryHistEntry data,
                // which is used to record battery level and status purpose only.
                if (!FAKE_PACKAGE_NAME.equals(entry.mPackageName)) {
                    slotBatteryHistDataMap.put(entry.getKey(), entry);
                }
            }
            newBatteryHistoryMap.put(timestamp, slotBatteryHistDataMap);
        }

        // Each time slot usage diff data =
        //     Math.abs(timestamp[i+2] data - timestamp[i+1] data) +
        //     Math.abs(timestamp[i+1] data - timestamp[i] data);
        // since we want to aggregate every two hours data into a single time slot.
        final int timestampStride = 2;
        for (int index = 0; index < timeSlotSize; index++) {
            final Long currentTimestamp =
                Long.valueOf(batteryHistoryKeys[index * timestampStride]);
            final Long nextTimestamp =
                Long.valueOf(batteryHistoryKeys[index * timestampStride + 1]);
            final Long nextTwoTimestamp =
                Long.valueOf(batteryHistoryKeys[index * timestampStride + 2]);

            // Fetches BatteryHistEntry data from corresponding time slot.
            final Map<String, BatteryHistEntry> currentBatteryHistMap =
                newBatteryHistoryMap.getOrDefault(currentTimestamp, EMPTY_BATTERY_MAP);
            final Map<String, BatteryHistEntry> nextBatteryHistMap =
                newBatteryHistoryMap.getOrDefault(nextTimestamp, EMPTY_BATTERY_MAP);
            final Map<String, BatteryHistEntry> nextTwoBatteryHistMap =
                newBatteryHistoryMap.getOrDefault(nextTwoTimestamp, EMPTY_BATTERY_MAP);

            // Collects all keys in these three time slot records as population.
            final Set<String> allBatteryHistEntryKeys = new HashSet<>();
            allBatteryHistEntryKeys.addAll(currentBatteryHistMap.keySet());
            allBatteryHistEntryKeys.addAll(nextBatteryHistMap.keySet());
            allBatteryHistEntryKeys.addAll(nextTwoBatteryHistMap.keySet());

            double totalConsumePower = 0.0;
            final List<BatteryDiffEntry> batteryDiffEntryList = new ArrayList<>();
            // Adds a specific time slot BatteryDiffEntry list into result map.
            resultMap.put(Integer.valueOf(index), batteryDiffEntryList);

            // Calculates all packages diff usage data in a specific time slot.
            for (String key : allBatteryHistEntryKeys) {
                final BatteryHistEntry currentEntry =
                    currentBatteryHistMap.getOrDefault(key, EMPTY_BATTERY_HIST_ENTRY);
                final BatteryHistEntry nextEntry =
                    nextBatteryHistMap.getOrDefault(key, EMPTY_BATTERY_HIST_ENTRY);
                final BatteryHistEntry nextTwoEntry =
                    nextTwoBatteryHistMap.getOrDefault(key, EMPTY_BATTERY_HIST_ENTRY);
                // Cumulative values is a specific time slot for a specific app.
                final long foregroundUsageTimeInMs =
                    getDiffValue(
                        currentEntry.mForegroundUsageTimeInMs,
                        nextEntry.mForegroundUsageTimeInMs,
                        nextTwoEntry.mForegroundUsageTimeInMs);
                final long backgroundUsageTimeInMs =
                    getDiffValue(
                        currentEntry.mBackgroundUsageTimeInMs,
                        nextEntry.mBackgroundUsageTimeInMs,
                        nextTwoEntry.mBackgroundUsageTimeInMs);
                final double consumePower =
                    getDiffValue(
                        currentEntry.mConsumePower,
                        nextEntry.mConsumePower,
                        nextTwoEntry.mConsumePower);
                totalConsumePower += consumePower;

                // Excludes entry since we don't have enough data to calculate.
                if (foregroundUsageTimeInMs == 0
                    && backgroundUsageTimeInMs == 0
                    && consumePower == 0) {
                    continue;
                }
                final BatteryHistEntry selectedBatteryEntry =
                    selectBatteryHistEntry(currentEntry, nextEntry, nextTwoEntry);
                if (selectedBatteryEntry == null) {
                    continue;
                }
                batteryDiffEntryList.add(
                    new BatteryDiffEntry(
                        context,
                        foregroundUsageTimeInMs,
                        backgroundUsageTimeInMs,
                        consumePower,
                        selectedBatteryEntry));
            }
            // Sets total consume power data into all BatteryDiffEntry in the same slot.
            for (BatteryDiffEntry diffEntry : batteryDiffEntryList) {
                diffEntry.setTotalConsumePower(totalConsumePower);
            }
        }
        insert24HoursData(BatteryChartView.SELECTED_INDEX_ALL, resultMap);
        if (purgeLowPercentageData) {
            purgeLowPercentageData(resultMap);
        }
        return resultMap;
    }

    private static void insert24HoursData(
            final int desiredIndex,
            final Map<Integer, List<BatteryDiffEntry>> indexedUsageMap) {
        final Map<String, BatteryDiffEntry> resultMap = new HashMap<>();
        double totalConsumePower = 0.0;
        // Loops for all BatteryDiffEntry and aggregate them together.
        for (List<BatteryDiffEntry> entryList : indexedUsageMap.values()) {
            for (BatteryDiffEntry entry : entryList) {
                final String key = entry.mBatteryHistEntry.getKey();
                final BatteryDiffEntry oldBatteryDiffEntry = resultMap.get(key);
                // Creates new BatteryDiffEntry if we don't have it.
                if (oldBatteryDiffEntry == null) {
                    resultMap.put(key, entry.clone());
                } else {
                    // Sums up some fields data into the existing one.
                    oldBatteryDiffEntry.mForegroundUsageTimeInMs +=
                        entry.mForegroundUsageTimeInMs;
                    oldBatteryDiffEntry.mBackgroundUsageTimeInMs +=
                        entry.mBackgroundUsageTimeInMs;
                    oldBatteryDiffEntry.mConsumePower += entry.mConsumePower;
                }
                totalConsumePower += entry.mConsumePower;
            }
        }
        final List<BatteryDiffEntry> resultList = new ArrayList<>(resultMap.values());
        // Sets total 24 hours consume power data into all BatteryDiffEntry.
        for (BatteryDiffEntry entry : resultList) {
            entry.setTotalConsumePower(totalConsumePower);
        }
        indexedUsageMap.put(Integer.valueOf(desiredIndex), resultList);
    }

    private static void purgeLowPercentageData(
            final Map<Integer, List<BatteryDiffEntry>> indexedUsageMap) {
        for (List<BatteryDiffEntry> entries : indexedUsageMap.values()) {
            final Iterator<BatteryDiffEntry> iterator = entries.iterator();
            while (iterator.hasNext()) {
                final BatteryDiffEntry entry = iterator.next();
                if (entry.getPercentOfTotal() < PERCENTAGE_OF_TOTAL_THRESHOLD) {
                    iterator.remove();
                }
            }
        }
    }

    private static long getDiffValue(long v1, long v2, long v3) {
        return (v2 > v1 ? v2 - v1 : 0) + (v3 > v2 ? v3 - v2 : 0);
    }

    private static double getDiffValue(double v1, double v2, double v3) {
        return (v2 > v1 ? v2 - v1 : 0) + (v3 > v2 ? v3 - v2 : 0);
    }

    private static BatteryHistEntry selectBatteryHistEntry(
            BatteryHistEntry entry1,
            BatteryHistEntry entry2,
            BatteryHistEntry entry3) {
        if (entry1 != null && entry1 != EMPTY_BATTERY_HIST_ENTRY) {
            return entry1;
        } else if (entry2 != null && entry2 != EMPTY_BATTERY_HIST_ENTRY) {
            return entry2;
        } else {
            return entry3 != null && entry3 != EMPTY_BATTERY_HIST_ENTRY
                ? entry3 : null;
        }
    }
}
