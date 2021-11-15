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
import android.content.Context;
import android.os.BatteryUsageStats;
import android.os.LocaleList;
import android.os.UserHandle;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.overlay.FeatureFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.time.Duration;
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
    private static final boolean DEBUG = false;
    private static final String TAG = "ConvertUtils";
    private static final Map<String, BatteryHistEntry> EMPTY_BATTERY_MAP = new HashMap<>();
    private static final BatteryHistEntry EMPTY_BATTERY_HIST_ENTRY =
        new BatteryHistEntry(new ContentValues());
    // Maximum total time value for each slot cumulative data at most 2 hours.
    private static final float TOTAL_TIME_THRESHOLD = DateUtils.HOUR_IN_MILLIS * 2;

    // Keys for metric metadata.
    static final int METRIC_KEY_PACKAGE = 1;
    static final int METRIC_KEY_BATTERY_LEVEL = 2;
    static final int METRIC_KEY_BATTERY_USAGE = 3;

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

    // For language is changed.
    @VisibleForTesting static Locale sLocale;
    @VisibleForTesting static Locale sLocaleForHour;
    // For time zone is changed.
    @VisibleForTesting static String sZoneId;
    @VisibleForTesting static String sZoneIdForHour;
    private static boolean sIs24HourFormat;

    @VisibleForTesting
    static SimpleDateFormat sSimpleDateFormat;
    @VisibleForTesting
    static SimpleDateFormat sSimpleDateFormatForHour;

    private ConvertUtils() {}

    public static ContentValues convert(
            BatteryEntry entry,
            BatteryUsageStats batteryUsageStats,
            int batteryLevel,
            int batteryStatus,
            int batteryHealth,
            long bootTimestamp,
            long timestamp) {
        final ContentValues values = new ContentValues();
        if (entry != null && batteryUsageStats != null) {
            values.put(BatteryHistEntry.KEY_UID, Long.valueOf(entry.getUid()));
            values.put(BatteryHistEntry.KEY_USER_ID,
                Long.valueOf(UserHandle.getUserId(entry.getUid())));
            values.put(BatteryHistEntry.KEY_APP_LABEL, entry.getLabel());
            values.put(BatteryHistEntry.KEY_PACKAGE_NAME,
                entry.getDefaultPackageName());
            values.put(BatteryHistEntry.KEY_IS_HIDDEN, Boolean.valueOf(entry.isHidden()));
            values.put(BatteryHistEntry.KEY_TOTAL_POWER,
                Double.valueOf(batteryUsageStats.getConsumedPower()));
            values.put(BatteryHistEntry.KEY_CONSUME_POWER,
                Double.valueOf(entry.getConsumedPower()));
            values.put(BatteryHistEntry.KEY_PERCENT_OF_TOTAL,
                Double.valueOf(entry.percent));
            values.put(BatteryHistEntry.KEY_FOREGROUND_USAGE_TIME,
                Long.valueOf(entry.getTimeInForegroundMs()));
            values.put(BatteryHistEntry.KEY_BACKGROUND_USAGE_TIME,
                Long.valueOf(entry.getTimeInBackgroundMs()));
            values.put(BatteryHistEntry.KEY_DRAIN_TYPE,
                Integer.valueOf(entry.getPowerComponentId()));
            values.put(BatteryHistEntry.KEY_CONSUMER_TYPE,
                Integer.valueOf(entry.getConsumerType()));
        } else {
            values.put(BatteryHistEntry.KEY_PACKAGE_NAME, FAKE_PACKAGE_NAME);
        }
        values.put(BatteryHistEntry.KEY_BOOT_TIMESTAMP, Long.valueOf(bootTimestamp));
        values.put(BatteryHistEntry.KEY_TIMESTAMP, Long.valueOf(timestamp));
        values.put(BatteryHistEntry.KEY_ZONE_ID, TimeZone.getDefault().getID());
        values.put(BatteryHistEntry.KEY_BATTERY_LEVEL, Integer.valueOf(batteryLevel));
        values.put(BatteryHistEntry.KEY_BATTERY_STATUS, Integer.valueOf(batteryStatus));
        values.put(BatteryHistEntry.KEY_BATTERY_HEALTH, Integer.valueOf(batteryHealth));
        return values;
    }

    /** Converts UTC timestamp to human readable local time string. */
    public static String utcToLocalTime(Context context, long timestamp) {
        final Locale currentLocale = getLocale(context);
        final String currentZoneId = TimeZone.getDefault().getID();
        if (!currentZoneId.equals(sZoneId)
                || !currentLocale.equals(sLocale)
                || sSimpleDateFormat == null) {
            sLocale = currentLocale;
            sZoneId = currentZoneId;
            sSimpleDateFormat =
                new SimpleDateFormat("MMM dd,yyyy HH:mm:ss", currentLocale);
        }
        return sSimpleDateFormat.format(new Date(timestamp));
    }

    /** Converts UTC timestamp to local time hour data. */
    public static String utcToLocalTimeHour(
            Context context, long timestamp, boolean is24HourFormat) {
        final Locale currentLocale = getLocale(context);
        final String currentZoneId = TimeZone.getDefault().getID();
        if (!currentZoneId.equals(sZoneIdForHour)
                || !currentLocale.equals(sLocaleForHour)
                || sIs24HourFormat != is24HourFormat
                || sSimpleDateFormatForHour == null) {
            sLocaleForHour = currentLocale;
            sZoneIdForHour = currentZoneId;
            sIs24HourFormat = is24HourFormat;
            sSimpleDateFormatForHour = new SimpleDateFormat(
                    sIs24HourFormat ? "HH" : "h", currentLocale);
        }
        return sSimpleDateFormatForHour.format(new Date(timestamp))
            .toLowerCase(currentLocale);
    }

    /** Gets indexed battery usage data for each corresponding time slot. */
    public static Map<Integer, List<BatteryDiffEntry>> getIndexedUsageMap(
            final Context context,
            final int timeSlotSize,
            final long[] batteryHistoryKeys,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final boolean purgeLowPercentageAndFakeData) {
        if (batteryHistoryMap == null || batteryHistoryMap.isEmpty()) {
            return new HashMap<>();
        }
        final Map<Integer, List<BatteryDiffEntry>> resultMap = new HashMap<>();
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
                batteryHistoryMap.getOrDefault(currentTimestamp, EMPTY_BATTERY_MAP);
            final Map<String, BatteryHistEntry> nextBatteryHistMap =
                batteryHistoryMap.getOrDefault(nextTimestamp, EMPTY_BATTERY_MAP);
            final Map<String, BatteryHistEntry> nextTwoBatteryHistMap =
                batteryHistoryMap.getOrDefault(nextTwoTimestamp, EMPTY_BATTERY_MAP);
            // We should not get the empty list since we have at least one fake data to record
            // the battery level and status in each time slot, the empty list is used to
            // represent there is no enough data to apply interpolation arithmetic.
            if (currentBatteryHistMap.isEmpty()
                    || nextBatteryHistMap.isEmpty()
                    || nextTwoBatteryHistMap.isEmpty()) {
                resultMap.put(Integer.valueOf(index), new ArrayList<BatteryDiffEntry>());
                continue;
            }

            // Collects all keys in these three time slot records as all populations.
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
                long foregroundUsageTimeInMs =
                    getDiffValue(
                        currentEntry.mForegroundUsageTimeInMs,
                        nextEntry.mForegroundUsageTimeInMs,
                        nextTwoEntry.mForegroundUsageTimeInMs);
                long backgroundUsageTimeInMs =
                    getDiffValue(
                        currentEntry.mBackgroundUsageTimeInMs,
                        nextEntry.mBackgroundUsageTimeInMs,
                        nextTwoEntry.mBackgroundUsageTimeInMs);
                double consumePower =
                    getDiffValue(
                        currentEntry.mConsumePower,
                        nextEntry.mConsumePower,
                        nextTwoEntry.mConsumePower);
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
                // Forces refine the cumulative value since it may introduce deviation
                // error since we will apply the interpolation arithmetic.
                final float totalUsageTimeInMs =
                    foregroundUsageTimeInMs + backgroundUsageTimeInMs;
                if (totalUsageTimeInMs > TOTAL_TIME_THRESHOLD) {
                    final float ratio = TOTAL_TIME_THRESHOLD / totalUsageTimeInMs;
                    if (DEBUG) {
                        Log.w(TAG, String.format("abnormal usage time %d|%d for:\n%s",
                                Duration.ofMillis(foregroundUsageTimeInMs).getSeconds(),
                                Duration.ofMillis(backgroundUsageTimeInMs).getSeconds(),
                                currentEntry));
                    }
                    foregroundUsageTimeInMs =
                        Math.round(foregroundUsageTimeInMs * ratio);
                    backgroundUsageTimeInMs =
                        Math.round(backgroundUsageTimeInMs * ratio);
                    consumePower = consumePower * ratio;
                }
                totalConsumePower += consumePower;
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
        if (purgeLowPercentageAndFakeData) {
            purgeLowPercentageAndFakeData(context, resultMap);
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

    // Removes low percentage data and fake usage data, which will be zero value.
    private static void purgeLowPercentageAndFakeData(
            final Context context,
            final Map<Integer, List<BatteryDiffEntry>> indexedUsageMap) {
        final List<CharSequence> backgroundUsageTimeHideList =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideBackgroundUsageTimeList(context);
        for (List<BatteryDiffEntry> entries : indexedUsageMap.values()) {
            final Iterator<BatteryDiffEntry> iterator = entries.iterator();
            while (iterator.hasNext()) {
                final BatteryDiffEntry entry = iterator.next();
                if (entry.getPercentOfTotal() < PERCENTAGE_OF_TOTAL_THRESHOLD
                        || FAKE_PACKAGE_NAME.equals(entry.getPackageName())) {
                    iterator.remove();
                }
                final String packageName = entry.getPackageName();
                if (packageName != null
                        && !backgroundUsageTimeHideList.isEmpty()
                        && backgroundUsageTimeHideList.contains(packageName)) {
                  entry.mBackgroundUsageTimeInMs = 0;
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

    @VisibleForTesting
    static Locale getLocale(Context context) {
        if (context == null) {
            return Locale.getDefault();
        }
        final LocaleList locales =
            context.getResources().getConfiguration().getLocales();
        return locales != null && !locales.isEmpty() ? locales.get(0)
            : Locale.getDefault();
    }
}
