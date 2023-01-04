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

import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.getEffectivePackageName;
import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.utcToLocalTime;

import android.app.usage.IUsageStatsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.BatteryConsumer;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.PowerProfile;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.fuelgauge.BatteryStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class to process data loaded from database and make the data easy to use for battery
 * usage UI.
 */
public final class DataProcessor {
    private static final boolean DEBUG = false;
    private static final String TAG = "DataProcessor";
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final int MIN_DAILY_DATA_SIZE = 2;
    private static final int MIN_TIMESTAMP_DATA_SIZE = 2;
    private static final int MAX_DIFF_SECONDS_OF_UPPER_TIMESTAMP = 5;
    // Maximum total time value for each hourly slot cumulative data at most 2 hours.
    private static final float TOTAL_HOURLY_TIME_THRESHOLD = DateUtils.HOUR_IN_MILLIS * 2;
    private static final long MIN_TIME_SLOT = DateUtils.HOUR_IN_MILLIS * 2;
    private static final String MEDIASERVER_PACKAGE_NAME = "mediaserver";
    private static final Map<String, BatteryHistEntry> EMPTY_BATTERY_MAP = new HashMap<>();
    private static final BatteryHistEntry EMPTY_BATTERY_HIST_ENTRY =
            new BatteryHistEntry(new ContentValues());

    @VisibleForTesting
    static final long DEFAULT_USAGE_DURATION_FOR_INCOMPLETE_INTERVAL =
            DateUtils.SECOND_IN_MILLIS * 30;

    @VisibleForTesting
    static final int SELECTED_INDEX_ALL = BatteryChartViewModel.SELECTED_INDEX_ALL;

    @VisibleForTesting
    static long sFakeCurrentTimeMillis = 0;

    @VisibleForTesting
    static IUsageStatsManager sUsageStatsManager =
            IUsageStatsManager.Stub.asInterface(
                    ServiceManager.getService(Context.USAGE_STATS_SERVICE));

    public static final String CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER =
            "CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER";
    public static final Comparator<AppUsageEvent> TIMESTAMP_COMPARATOR =
            Comparator.comparing(AppUsageEvent::getTimestamp);

    /** A callback listener when battery usage loading async task is executed. */
    public interface UsageMapAsyncResponse {
        /** The callback function when batteryUsageMap is loaded. */
        void onBatteryCallbackDataLoaded(BatteryCallbackData batteryCallbackData);
    }

    private DataProcessor() {
    }

    /**
     * @return Returns battery usage data of different entries.
     * Returns null if the input is invalid or there is no enough data.
     */
    @Nullable
    public static Map<Integer, Map<Integer, BatteryDiffData>> getBatteryUsageData(
            Context context,
            @Nullable final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        if (batteryHistoryMap == null || batteryHistoryMap.isEmpty()) {
            Log.d(TAG, "getBatteryLevelData() returns null");
            return null;
        }
        // Process raw history map data into hourly timestamps.
        final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap =
                getHistoryMapWithExpectedTimestamps(context, batteryHistoryMap);
        // Wrap and processed history map into easy-to-use format for UI rendering.
        final BatteryLevelData batteryLevelData =
                getLevelDataThroughProcessedHistoryMap(context, processedBatteryHistoryMap);
        return batteryLevelData == null
                ? null
                : getBatteryUsageMap(
                        context,
                        batteryLevelData.getHourlyBatteryLevelsPerDay(),
                        processedBatteryHistoryMap,
                        /*appUsagePeriodMap=*/ null);
    }

    /**
     * Gets the {@link BatteryUsageStats} from system service.
     */
    @Nullable
    public static BatteryUsageStats getBatteryUsageStats(final Context context) {
        final BatteryUsageStatsQuery batteryUsageStatsQuery =
                new BatteryUsageStatsQuery
                        .Builder()
                        .includeBatteryHistory()
                        .includeProcessStateData()
                        .build();
        return context.getSystemService(BatteryStatsManager.class)
                .getBatteryUsageStats(batteryUsageStatsQuery);
    }

    /**
     * Gets the {@link UsageEvents} from system service for all unlocked users.
     */
    @Nullable
    public static Map<Long, UsageEvents> getAppUsageEvents(Context context) {
        final long start = System.currentTimeMillis();
        context = DatabaseUtils.getOwnerContext(context);
        if (context == null) {
            return null;
        }
        final Map<Long, UsageEvents> resultMap = new HashMap();
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) {
            return null;
        }
        final long sixDaysAgoTimestamp =
                DatabaseUtils.getTimestampSixDaysAgo(Calendar.getInstance());
        for (final UserInfo user : userManager.getAliveUsers()) {
            final UsageEvents events = getAppUsageEventsForUser(
                    context, userManager, user.id, sixDaysAgoTimestamp);
            if (events != null) {
                resultMap.put(Long.valueOf(user.id), events);
            }
        }
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(TAG, String.format("getAppUsageEvents() for all unlocked users in %d/ms",
                elapsedTime));
        return resultMap.isEmpty() ? null : resultMap;
    }

    /**
     * Gets the {@link UsageEvents} from system service for the specific user.
     */
    @Nullable
    public static UsageEvents getAppUsageEventsForUser(
            Context context, final int userID, final long startTimestampOfLevelData) {
        final long start = System.currentTimeMillis();
        context = DatabaseUtils.getOwnerContext(context);
        if (context == null) {
            return null;
        }
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) {
            return null;
        }
        final long sixDaysAgoTimestamp =
                DatabaseUtils.getTimestampSixDaysAgo(Calendar.getInstance());
        final long earliestTimestamp = Math.max(sixDaysAgoTimestamp, startTimestampOfLevelData);
        final UsageEvents events = getAppUsageEventsForUser(
                context, userManager, userID, earliestTimestamp);
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(TAG, String.format("getAppUsageEventsForUser() for user %d in %d/ms",
                userID, elapsedTime));
        return events;
    }

    /**
     * Closes the {@link BatteryUsageStats} after using it.
     */
    public static void closeBatteryUsageStats(BatteryUsageStats batteryUsageStats) {
        if (batteryUsageStats != null) {
            try {
                batteryUsageStats.close();
            } catch (Exception e) {
                Log.e(TAG, "BatteryUsageStats.close() failed", e);
            }
        }
    }

    /**
     * Generates the indexed {@link AppUsagePeriod} list data for each corresponding time slot.
     * Attributes the list of {@link AppUsageEvent} into hourly time slots and reformat them into
     * {@link AppUsagePeriod} for easier use in the following process.
     *
     * <p>There could be 2 cases of the returned value:</p>
     * <ul>
     * <li>null: empty or invalid data.</li>
     * <li>non-null: must be a 2d map and composed by:
     * <p>  [0][0] ~ [maxDailyIndex][maxHourlyIndex]</p></li>
     * </ul>
     *
     * <p>The structure is consistent with the battery usage map returned by
     * {@code getBatteryUsageMap}.</p>
     *
     * <p>{@code Long} stands for the userId.</p>
     * <p>{@code String} stands for the packageName.</p>
     */
    @Nullable
    public static Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
            generateAppUsagePeriodMap(
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            final List<AppUsageEvent> appUsageEventList,
            final long startTimestampOfLevelData) {
        if (appUsageEventList.isEmpty()) {
            Log.w(TAG, "appUsageEventList is empty");
            return null;
        }
        // Sorts the appUsageEventList in ascending order based on the timestamp before
        // distribution.
        Collections.sort(appUsageEventList, TIMESTAMP_COMPARATOR);
        final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>> resultMap =
                new HashMap<>();
        // Starts from the first index within expected time range. The events before the time range
        // will be bypassed directly.
        int currentAppUsageEventIndex =
                getFirstUsageEventIndex(appUsageEventList, startTimestampOfLevelData);
        final int appUsageEventSize = appUsageEventList.size();

        for (int dailyIndex = 0; dailyIndex < hourlyBatteryLevelsPerDay.size(); dailyIndex++) {
            final Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>> dailyMap =
                    new HashMap<>();
            resultMap.put(dailyIndex, dailyMap);
            if (hourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> timestamps = hourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            for (int hourlyIndex = 0; hourlyIndex < timestamps.size() - 1; hourlyIndex++) {
                // The start and end timestamps of this slot should be the adjacent timestamps.
                final long startTimestamp = timestamps.get(hourlyIndex);
                final long endTimestamp = timestamps.get(hourlyIndex + 1);

                // Gets the app usage event list for this hourly slot first.
                final List<AppUsageEvent> hourlyAppUsageEventList = new ArrayList<>();
                while (currentAppUsageEventIndex < appUsageEventSize) {
                    // If current event is null, go for next directly.
                    if (appUsageEventList.get(currentAppUsageEventIndex) == null) {
                        currentAppUsageEventIndex++;
                        continue;
                    }
                    final long timestamp =
                            appUsageEventList.get(currentAppUsageEventIndex).getTimestamp();
                    // If the timestamp is already later than the end time, stop the loop.
                    if (timestamp >= endTimestamp) {
                        break;
                    }
                    // If timestamp is within the time range, add it into the list.
                    if (timestamp >= startTimestamp) {
                        hourlyAppUsageEventList.add(
                                appUsageEventList.get(currentAppUsageEventIndex));
                    }
                    currentAppUsageEventIndex++;
                }

                // The value could be null when there is no data in the hourly slot.
                dailyMap.put(
                        hourlyIndex,
                        buildAppUsagePeriodList(
                                hourlyAppUsageEventList, startTimestamp, endTimestamp));
            }
        }
        return resultMap;
    }

    /**
     * Generates the list of {@link AppUsageEvent} from the supplied {@link UsageEvents}.
     */
    public static List<AppUsageEvent> generateAppUsageEventListFromUsageEvents(
            Context context, Map<Long, UsageEvents> usageEventsMap) {
        final List<AppUsageEvent> appUsageEventList = new ArrayList<>();
        long numEventsFetched = 0;
        long numAllEventsFetched = 0;
        final Set<CharSequence> ignoreScreenOnTimeTaskRootSet =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getIgnoreScreenOnTimeTaskRootSet(context);
        for (final long userId : usageEventsMap.keySet()) {
            final UsageEvents usageEvents = usageEventsMap.get(userId);
            while (usageEvents.hasNextEvent()) {
                final Event event = new Event();
                usageEvents.getNextEvent(event);
                numAllEventsFetched++;
                switch (event.getEventType()) {
                    case Event.ACTIVITY_RESUMED:
                    case Event.ACTIVITY_STOPPED:
                    case Event.DEVICE_SHUTDOWN:
                        final String taskRootClassName = event.getTaskRootClassName();
                        if (!TextUtils.isEmpty(taskRootClassName)
                                && ignoreScreenOnTimeTaskRootSet.contains(taskRootClassName)) {
                            Log.w(TAG, String.format(
                                    "Ignoring a usage event with task root class name %s, "
                                            + "(timestamp=%d, type=%d)",
                                    taskRootClassName,
                                    event.getTimeStamp(),
                                    event.getEventType()));
                            break;
                        }
                        final AppUsageEvent appUsageEvent =
                                ConvertUtils.convertToAppUsageEvent(
                                        context, sUsageStatsManager, event, userId);
                        if (appUsageEvent != null) {
                            numEventsFetched++;
                            appUsageEventList.add(appUsageEvent);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        Log.w(TAG, String.format(
                "Read %d relevant events (%d total) from UsageStatsManager", numEventsFetched,
                numAllEventsFetched));
        return appUsageEventList;
    }

    /**
     * @return Returns the device screen-on time data.
     *
     * <p>There could be 2 cases of the returned value:</p>
     * <ul>
     * <li>null: empty or invalid data.</li>
     * <li>non-null: must be a 2d map and composed by 3 parts:</li>
     * <p>  1 - [SELECTED_INDEX_ALL][SELECTED_INDEX_ALL]</p>
     * <p>  2 - [0][SELECTED_INDEX_ALL] ~ [maxDailyIndex][SELECTED_INDEX_ALL]</p>
     * <p>  3 - [0][0] ~ [maxDailyIndex][maxHourlyIndex]</p>
     * </ul>
     *
     * <p>The structure is consistent with the battery usage map returned by
     * {@code getBatteryUsageMap}.</p>
     */
    @Nullable
    public static Map<Integer, Map<Integer, Long>> getDeviceScreenOnTime(
            final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                    appUsagePeriodMap) {
        if (appUsagePeriodMap == null) {
            return null;
        }
        final Map<Integer, Map<Integer, Long>> deviceScreenOnTime = new HashMap<>();
        insertHourlyDeviceScreenOnTime(appUsagePeriodMap, deviceScreenOnTime);
        insertDailyDeviceScreenOnTime(appUsagePeriodMap, deviceScreenOnTime);
        insertAllDeviceScreenOnTime(deviceScreenOnTime);
        return deviceScreenOnTime;
    }

    /**
     * Generates the list of {@link BatteryEntry} from the supplied {@link BatteryUsageStats}.
     */
    @Nullable
    public static List<BatteryEntry> generateBatteryEntryListFromBatteryUsageStats(
            final Context context,
            @Nullable final BatteryUsageStats batteryUsageStats) {
        if (batteryUsageStats == null) {
            Log.w(TAG, "batteryUsageStats is null content");
            return null;
        }
        if (!shouldShowBatteryAttributionList(context)) {
            return null;
        }
        final BatteryUtils batteryUtils = BatteryUtils.getInstance(context);
        final int dischargePercentage = Math.max(0, batteryUsageStats.getDischargePercentage());
        final List<BatteryEntry> usageList = getCoalescedUsageList(
                context, batteryUtils, batteryUsageStats, /*loadDataInBackground=*/ false);
        final double totalPower = batteryUsageStats.getConsumedPower();
        for (int i = 0; i < usageList.size(); i++) {
            final BatteryEntry entry = usageList.get(i);
            final double percentOfTotal = batteryUtils.calculateBatteryPercent(
                    entry.getConsumedPower(), totalPower, dischargePercentage);
            entry.mPercent = percentOfTotal;
        }
        return usageList;
    }

    /**
     * @return Returns the latest battery history map loaded from the battery stats service.
     */
    public static Map<String, BatteryHistEntry> getCurrentBatteryHistoryMapFromStatsService(
            final Context context) {
        final List<BatteryHistEntry> batteryHistEntryList =
                getBatteryHistListFromFromStatsService(context);
        return batteryHistEntryList == null ? new HashMap<>()
                : batteryHistEntryList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
    }

    /**
     * @return Returns the processed history map which has interpolated to every hour data.
     * The start and end timestamp must be the even hours.
     * The keys of processed history map should contain every hour between the start and end
     * timestamp. If there's no data in some key, the value will be the empty hashmap.
     */
    static Map<Long, Map<String, BatteryHistEntry>> getHistoryMapWithExpectedTimestamps(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        final long startTime = System.currentTimeMillis();
        final List<Long> rawTimestampList = new ArrayList<>(batteryHistoryMap.keySet());
        final Map<Long, Map<String, BatteryHistEntry>> resultMap = new HashMap();
        if (rawTimestampList.isEmpty()) {
            Log.d(TAG, "empty batteryHistoryMap in getHistoryMapWithExpectedTimestamps()");
            return resultMap;
        }
        Collections.sort(rawTimestampList);
        final long currentTime = getCurrentTimeMillis();
        final List<Long> expectedTimestampList = getTimestampSlots(rawTimestampList, currentTime);
        final boolean isFromFullCharge =
                isFromFullCharge(batteryHistoryMap.get(rawTimestampList.get(0)));
        interpolateHistory(
                context, rawTimestampList, expectedTimestampList, currentTime, isFromFullCharge,
                batteryHistoryMap, resultMap);
        Log.d(TAG, String.format("getHistoryMapWithExpectedTimestamps() size=%d in %d/ms",
                resultMap.size(), (System.currentTimeMillis() - startTime)));
        return resultMap;
    }

    @Nullable
    static BatteryLevelData getLevelDataThroughProcessedHistoryMap(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap) {
        final List<Long> timestampList = new ArrayList<>(processedBatteryHistoryMap.keySet());
        Collections.sort(timestampList);
        final List<Long> dailyTimestamps = getDailyTimestamps(timestampList);
        // There should be at least the start and end timestamps. Otherwise, return null to not show
        // data in usage chart.
        if (dailyTimestamps.size() < MIN_DAILY_DATA_SIZE) {
            return null;
        }

        final List<List<Long>> hourlyTimestamps = getHourlyTimestamps(dailyTimestamps);
        final BatteryLevelData.PeriodBatteryLevelData dailyLevelData =
                getPeriodBatteryLevelData(context, processedBatteryHistoryMap, dailyTimestamps);
        final List<BatteryLevelData.PeriodBatteryLevelData> hourlyLevelData =
                getHourlyPeriodBatteryLevelData(
                        context, processedBatteryHistoryMap, hourlyTimestamps);
        return new BatteryLevelData(dailyLevelData, hourlyLevelData);
    }

    /**
     * Computes expected timestamp slots for last full charge, which will return hourly timestamps
     * between start and end two even hour values.
     */
    @VisibleForTesting
    static List<Long> getTimestampSlots(final List<Long> rawTimestampList, final long currentTime) {
        final List<Long> timestampSlots = new ArrayList<>();
        if (rawTimestampList.isEmpty()) {
            return timestampSlots;
        }
        final long rawStartTimestamp = rawTimestampList.get(0);
        // No matter the start is from last full charge or 6 days ago, use the nearest even hour.
        final long startTimestamp = getNearestEvenHourTimestamp(rawStartTimestamp);
        // Use the first even hour after the current time as the end.
        final long endTimestamp = getFirstEvenHourAfterTimestamp(currentTime);
        // If the start timestamp is later or equal the end one, return the empty list.
        if (startTimestamp >= endTimestamp) {
            return timestampSlots;
        }
        for (long timestamp = startTimestamp; timestamp <= endTimestamp;
                timestamp += DateUtils.HOUR_IN_MILLIS) {
            timestampSlots.add(timestamp);
        }
        return timestampSlots;
    }

    /**
     * Computes expected daily timestamp slots.
     *
     * The valid result should be composed of 3 parts:
     * 1) start timestamp
     * 2) every 00:00 timestamp (default timezone) between the start and end
     * 3) end timestamp
     * Otherwise, returns an empty list.
     */
    @VisibleForTesting
    static List<Long> getDailyTimestamps(final List<Long> timestampList) {
        final List<Long> dailyTimestampList = new ArrayList<>();
        // If timestamp number is smaller than 2, the following computation is not necessary.
        if (timestampList.size() < MIN_TIMESTAMP_DATA_SIZE) {
            return dailyTimestampList;
        }
        final long startTime = timestampList.get(0);
        final long endTime = timestampList.get(timestampList.size() - 1);
        // If the timestamp diff is smaller than MIN_TIME_SLOT, returns the empty list directly.
        if (endTime - startTime < MIN_TIME_SLOT) {
            return dailyTimestampList;
        }
        long nextDay = getTimestampOfNextDay(startTime);
        // Only if the timestamp diff in the first day is bigger than MIN_TIME_SLOT, start from the
        // first day. Otherwise, start from the second day.
        if (nextDay - startTime >= MIN_TIME_SLOT) {
            dailyTimestampList.add(startTime);
        }
        while (nextDay < endTime) {
            dailyTimestampList.add(nextDay);
            nextDay += DateUtils.DAY_IN_MILLIS;
        }
        final long lastDailyTimestamp = dailyTimestampList.get(dailyTimestampList.size() - 1);
        // Only if the timestamp diff in the last day is bigger than MIN_TIME_SLOT, add the
        // last day.
        if (endTime - lastDailyTimestamp >= MIN_TIME_SLOT) {
            dailyTimestampList.add(endTime);
        }
        // The dailyTimestampList must have the start and end timestamp, otherwise, return an empty
        // list.
        if (dailyTimestampList.size() < MIN_TIMESTAMP_DATA_SIZE) {
            return new ArrayList<>();
        }
        return dailyTimestampList;
    }

    @VisibleForTesting
    static boolean isFromFullCharge(@Nullable final Map<String, BatteryHistEntry> entryList) {
        if (entryList == null) {
            Log.d(TAG, "entryList is null in isFromFullCharge()");
            return false;
        }
        final List<String> entryKeys = new ArrayList<>(entryList.keySet());
        if (entryKeys.isEmpty()) {
            Log.d(TAG, "empty entryList in isFromFullCharge()");
            return false;
        }
        // The hist entries in the same timestamp should have same battery status and level.
        // Checking the first one should be enough.
        final BatteryHistEntry firstHistEntry = entryList.get(entryKeys.get(0));
        return BatteryStatus.isCharged(firstHistEntry.mBatteryStatus, firstHistEntry.mBatteryLevel);
    }

    @VisibleForTesting
    static long[] findNearestTimestamp(final List<Long> timestamps, final long target) {
        final long[] results = new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
        // Searches the nearest lower and upper timestamp value.
        timestamps.forEach(timestamp -> {
            if (timestamp <= target && timestamp > results[0]) {
                results[0] = timestamp;
            }
            if (timestamp >= target && timestamp < results[1]) {
                results[1] = timestamp;
            }
        });
        // Uses zero value to represent invalid searching result.
        results[0] = results[0] == Long.MIN_VALUE ? 0 : results[0];
        results[1] = results[1] == Long.MAX_VALUE ? 0 : results[1];
        return results;
    }

    /**
     * @return Returns the timestamp for 00:00 1 day after the given timestamp based on local
     * timezone.
     */
    @VisibleForTesting
    static long getTimestampOfNextDay(long timestamp) {
        return getTimestampWithDayDiff(timestamp, /*dayDiff=*/ 1);
    }

    /**
     * Returns whether currentSlot will be used in daily chart.
     */
    @VisibleForTesting
    static boolean isForDailyChart(final boolean isStartOrEnd, final long currentSlot) {
        // The start and end timestamps will always be used in daily chart.
        if (isStartOrEnd) {
            return true;
        }

        // The timestamps for 00:00 will be used in daily chart.
        final long startOfTheDay = getTimestampWithDayDiff(currentSlot, /*dayDiff=*/ 0);
        return currentSlot == startOfTheDay;
    }

    /**
     * @return Returns the indexed battery usage data for each corresponding time slot.
     *
     * <p>There could be 2 cases of the returned value:</p>
     * <ul>
     * <li>null: empty or invalid data.</li>
     * <li>non-null: must be a 2d map and composed by 3 parts:</li>
     * <p>  1 - [SELECTED_INDEX_ALL][SELECTED_INDEX_ALL]</p>
     * <p>  2 - [0][SELECTED_INDEX_ALL] ~ [maxDailyIndex][SELECTED_INDEX_ALL]</p>
     * <p>  3 - [0][0] ~ [maxDailyIndex][maxHourlyIndex]</p>
     * </ul>
     */
    @Nullable
    static Map<Integer, Map<Integer, BatteryDiffData>> getBatteryUsageMap(
            final Context context,
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                    appUsagePeriodMap) {
        if (batteryHistoryMap.isEmpty()) {
            return null;
        }
        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap = new HashMap<>();
        // Insert diff data from [0][0] to [maxDailyIndex][maxHourlyIndex].
        insertHourlyUsageDiffData(
                context, hourlyBatteryLevelsPerDay, batteryHistoryMap, appUsagePeriodMap,
                resultMap);
        // Insert diff data from [0][SELECTED_INDEX_ALL] to [maxDailyIndex][SELECTED_INDEX_ALL].
        insertDailyUsageDiffData(hourlyBatteryLevelsPerDay, resultMap);
        // Insert diff data [SELECTED_INDEX_ALL][SELECTED_INDEX_ALL].
        insertAllUsageDiffData(resultMap);
        processBatteryDiffData(context, resultMap);
        if (!isUsageMapValid(resultMap, hourlyBatteryLevelsPerDay)) {
            return null;
        }
        return resultMap;
    }

    @VisibleForTesting
    @Nullable
    static BatteryDiffData generateBatteryDiffData(
            final Context context,
            final List<BatteryHistEntry> batteryHistEntryList) {
        if (batteryHistEntryList == null || batteryHistEntryList.isEmpty()) {
            Log.w(TAG, "batteryHistEntryList is null or empty in generateBatteryDiffData()");
            return null;
        }
        final int currentUserId = context.getUserId();
        final UserHandle userHandle =
                Utils.getManagedProfile(context.getSystemService(UserManager.class));
        final int workProfileUserId =
                userHandle != null ? userHandle.getIdentifier() : Integer.MIN_VALUE;
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        final List<BatteryDiffEntry> systemEntries = new ArrayList<>();

        for (BatteryHistEntry entry : batteryHistEntryList) {
            final boolean isFromOtherUsers = isConsumedFromOtherUsers(
                    currentUserId, workProfileUserId, entry);
            // Not show other users' battery usage data.
            if (isFromOtherUsers) {
                continue;
            } else {
                final BatteryDiffEntry currentBatteryDiffEntry = new BatteryDiffEntry(
                        context,
                        entry.mForegroundUsageTimeInMs,
                        entry.mBackgroundUsageTimeInMs,
                        /*screenOnTimeInMs=*/ 0,
                        entry.mConsumePower,
                        entry.mForegroundUsageConsumePower,
                        entry.mForegroundServiceUsageConsumePower,
                        entry.mBackgroundUsageConsumePower,
                        entry.mCachedUsageConsumePower,
                        entry);
                if (currentBatteryDiffEntry.isSystemEntry()) {
                    systemEntries.add(currentBatteryDiffEntry);
                } else {
                    appEntries.add(currentBatteryDiffEntry);
                }
            }
        }

        // If there is no data, return null instead of empty item.
        if (appEntries.isEmpty() && systemEntries.isEmpty()) {
            return null;
        }

        return new BatteryDiffData(appEntries, systemEntries);
    }

    /**
     * <p>{@code Long} stands for the userId.</p>
     * <p>{@code String} stands for the packageName.</p>
     */
    @VisibleForTesting
    @Nullable
    static Map<Long, Map<String, List<AppUsagePeriod>>> buildAppUsagePeriodList(
            final List<AppUsageEvent> allAppUsageEvents, final long startTime, final long endTime) {
        if (allAppUsageEvents.isEmpty()) {
            return null;
        }

        // Attributes the list of AppUsagePeriod into device events and instance events for further
        // use.
        final List<AppUsageEvent> deviceEvents = new ArrayList<>();
        final ArrayMap<Integer, List<AppUsageEvent>> usageEventsByInstanceId = new ArrayMap<>();
        for (final AppUsageEvent event : allAppUsageEvents) {
            final AppUsageEventType eventType = event.getType();
            if (eventType == AppUsageEventType.ACTIVITY_RESUMED
                    || eventType == AppUsageEventType.ACTIVITY_STOPPED) {
                final int instanceId = event.getInstanceId();
                if (usageEventsByInstanceId.get(instanceId) == null) {
                    usageEventsByInstanceId.put(instanceId, new ArrayList<>());
                }
                usageEventsByInstanceId.get(instanceId).add(event);
            } else if (eventType == AppUsageEventType.DEVICE_SHUTDOWN) {
                // Track device-wide events in their own list as they affect any app.
                deviceEvents.add(event);
            }
        }
        if (usageEventsByInstanceId.isEmpty()) {
            return null;
        }

        final Map<Long, Map<String, List<AppUsagePeriod>>> allUsagePeriods = new HashMap<>();

        for (int i = 0; i < usageEventsByInstanceId.size(); i++) {
            // The usage periods for an instance are determined by the usage events with its
            // instance id and any device-wide events such as device shutdown.
            final List<AppUsageEvent> usageEvents = usageEventsByInstanceId.valueAt(i);
            if (usageEvents == null || usageEvents.isEmpty()) {
                continue;
            }
            // The same instance must have same userId and packageName.
            final AppUsageEvent firstEvent = usageEvents.get(0);
            final long eventUserId = firstEvent.getUserId();
            final String packageName = getEffectivePackageName(
                    sUsageStatsManager,
                    firstEvent.getPackageName(),
                    firstEvent.getTaskRootPackageName());
            usageEvents.addAll(deviceEvents);
            // Sorts the usageEvents in ascending order based on the timestamp before computing the
            // period.
            Collections.sort(usageEvents, TIMESTAMP_COMPARATOR);

            // A package might have multiple instances. Computes the usage period per instance id
            // and then merges them into the same user-package map.
            final List<AppUsagePeriod> usagePeriodList =
                    buildAppUsagePeriodListPerInstance(usageEvents, startTime, endTime);
            if (!usagePeriodList.isEmpty()) {
                addToUsagePeriodMap(allUsagePeriods, usagePeriodList, eventUserId, packageName);
            }
        }

        // Sorts all usage periods by start time.
        for (final long userId : allUsagePeriods.keySet()) {
            if (allUsagePeriods.get(userId) == null) {
                continue;
            }
            for (final String packageName: allUsagePeriods.get(userId).keySet()) {
                Collections.sort(
                        allUsagePeriods.get(userId).get(packageName),
                        Comparator.comparing(AppUsagePeriod::getStartTime));
            }
        }
        return allUsagePeriods.isEmpty() ? null : allUsagePeriods;
    }

    @VisibleForTesting
    static List<AppUsagePeriod> buildAppUsagePeriodListPerInstance(
            final List<AppUsageEvent> usageEvents, final long startTime, final long endTime) {
        final List<AppUsagePeriod> usagePeriodList = new ArrayList<>();
        AppUsagePeriod.Builder pendingUsagePeriod = AppUsagePeriod.newBuilder();
        boolean hasMetStartEvent = false;

        for (final AppUsageEvent event : usageEvents) {
            final long eventTime = event.getTimestamp();

            if (event.getType() == AppUsageEventType.ACTIVITY_RESUMED) {
                hasMetStartEvent = true;
                // If there is an existing start time, simply ignore this start event.
                // If there was no start time, then start a new period.
                if (!pendingUsagePeriod.hasStartTime()) {
                    pendingUsagePeriod.setStartTime(eventTime);
                }
            } else if (event.getType() == AppUsageEventType.ACTIVITY_STOPPED) {
                pendingUsagePeriod.setEndTime(eventTime);
                if (!pendingUsagePeriod.hasStartTime()) {
                    // If we haven't met start event in this list, the start event might happen
                    // in the previous time slot. use the startTime for this period.
                    // Otherwise, add one for the default duration.
                    if (!hasMetStartEvent) {
                        hasMetStartEvent = true;
                        pendingUsagePeriod.setStartTime(startTime);
                    } else {
                        pendingUsagePeriod.setStartTime(
                                getStartTimeForIncompleteUsagePeriod(pendingUsagePeriod));
                    }
                }
                // If we already have start time, add it directly.
                addToPeriodList(usagePeriodList, pendingUsagePeriod.build());
                pendingUsagePeriod.clear();
            } else if (event.getType() == AppUsageEventType.DEVICE_SHUTDOWN) {
                hasMetStartEvent = true;
                // The end event might be lost when device is shutdown. Use the estimated end
                // time for the period.
                if (pendingUsagePeriod.hasStartTime()) {
                    pendingUsagePeriod.setEndTime(
                            getEndTimeForIncompleteUsagePeriod(pendingUsagePeriod, eventTime));
                    addToPeriodList(usagePeriodList, pendingUsagePeriod.build());
                    pendingUsagePeriod.clear();
                }
            }
        }
        // If there exists unclosed period, the stop event might happen in the next time
        // slot. Use the endTime for the period.
        if (pendingUsagePeriod.hasStartTime()) {
            pendingUsagePeriod.setEndTime(endTime);
            addToPeriodList(usagePeriodList, pendingUsagePeriod.build());
            pendingUsagePeriod.clear();
        }
        return usagePeriodList;
    }

    @VisibleForTesting
    static long getScreenOnTime(
            final Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap,
            final long userId,
            final String packageName) {
        if (appUsageMap == null || appUsageMap.get(userId) == null) {
            return 0;
        }

        return getScreenOnTime(appUsageMap.get(userId).get(packageName));
    }

    /**
     * @return Returns the overall battery usage data from battery stats service directly.
     *
     * The returned value should be always a 2d map and composed by only 1 part:
     * - [SELECTED_INDEX_ALL][SELECTED_INDEX_ALL]
     */
    static Map<Integer, Map<Integer, BatteryDiffData>> getBatteryUsageMapFromStatsService(
            final Context context) {
        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap = new HashMap<>();
        final Map<Integer, BatteryDiffData> allUsageMap = new HashMap<>();
        // Always construct the map whether the value is null or not.
        allUsageMap.put(SELECTED_INDEX_ALL,
                generateBatteryDiffData(context, getBatteryHistListFromFromStatsService(context)));
        resultMap.put(SELECTED_INDEX_ALL, allUsageMap);
        processBatteryDiffData(context, resultMap);
        return resultMap;
    }

    static void loadLabelAndIcon(
            @Nullable final Map<Integer, Map<Integer, BatteryDiffData>> batteryUsageMap) {
        if (batteryUsageMap == null) {
            return;
        }
        // Pre-loads each BatteryDiffEntry relative icon and label for all slots.
        final BatteryDiffData batteryUsageMapForAll =
                batteryUsageMap.get(SELECTED_INDEX_ALL).get(SELECTED_INDEX_ALL);
        if (batteryUsageMapForAll != null) {
            batteryUsageMapForAll.getAppDiffEntryList().forEach(
                    entry -> entry.loadLabelAndIcon());
            batteryUsageMapForAll.getSystemDiffEntryList().forEach(
                    entry -> entry.loadLabelAndIcon());
        }
    }

    private static void addToPeriodList(
            final List<AppUsagePeriod> appUsagePeriodList, final AppUsagePeriod appUsagePeriod) {
        // Only when the period is valid, add it into the list.
        if (appUsagePeriod.getStartTime() < appUsagePeriod.getEndTime()) {
            appUsagePeriodList.add(appUsagePeriod);
        }
    }

    private static void addToUsagePeriodMap(
            final Map<Long, Map<String, List<AppUsagePeriod>>> usagePeriodMap,
            final List<AppUsagePeriod> usagePeriodList,
            final long userId,
            final String packageName) {
        usagePeriodMap.computeIfAbsent(userId, key -> new HashMap<>());
        final Map<String, List<AppUsagePeriod>> packageNameMap = usagePeriodMap.get(userId);
        packageNameMap.computeIfAbsent(packageName, key -> new ArrayList<>());
        packageNameMap.get(packageName).addAll(usagePeriodList);
    }

    /**
     * Returns the start time that gives {@code usagePeriod} the default usage duration.
     */
    private static long getStartTimeForIncompleteUsagePeriod(
            final AppUsagePeriodOrBuilder usagePeriod) {
        return usagePeriod.getEndTime() - DEFAULT_USAGE_DURATION_FOR_INCOMPLETE_INTERVAL;
    }

    /**
     * Returns the end time that gives {@code usagePeriod} the default usage duration.
     */
    private static long getEndTimeForIncompleteUsagePeriod(
            final AppUsagePeriodOrBuilder usagePeriod, final long eventTime) {
        return Math.min(
                usagePeriod.getStartTime() + DEFAULT_USAGE_DURATION_FOR_INCOMPLETE_INTERVAL,
                eventTime);
    }

    private static int getFirstUsageEventIndex(
            final List<AppUsageEvent> appUsageEventList,
            final long startTimestampOfLevelData) {
        int currentIndex = 0;
        while (currentIndex < appUsageEventList.size()
                && (appUsageEventList.get(currentIndex) == null
                || appUsageEventList.get(currentIndex).getTimestamp()
                < startTimestampOfLevelData)) {
            currentIndex++;
        }
        return currentIndex;
    }

    private static void insertHourlyDeviceScreenOnTime(
            final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                    appUsagePeriodMap,
            final Map<Integer, Map<Integer, Long>> resultMap) {
        for (final int dailyIndex : appUsagePeriodMap.keySet()) {
            final Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>> dailyAppUsageMap =
                    appUsagePeriodMap.get(dailyIndex);
            final Map<Integer, Long> dailyScreenOnTime = new HashMap<>();
            resultMap.put(dailyIndex, dailyScreenOnTime);
            if (dailyAppUsageMap == null) {
                continue;
            }

            for (final int hourlyIndex : dailyAppUsageMap.keySet()) {
                final Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap =
                        dailyAppUsageMap.get(hourlyIndex);
                if (appUsageMap == null || appUsageMap.isEmpty()) {
                    dailyScreenOnTime.put(hourlyIndex, 0L);
                } else {
                    final List<AppUsagePeriod> flatUsageList = new ArrayList<>();
                    for (final long userId: appUsageMap.keySet()) {
                        if (appUsageMap.get(userId) == null) {
                            continue;
                        }
                        for (final String packageName: appUsageMap.get(userId).keySet()) {
                            final List<AppUsagePeriod> appUsagePeriodList =
                                    appUsageMap.get(userId).get(packageName);
                            if (appUsagePeriodList != null && !appUsagePeriodList.isEmpty()) {
                                flatUsageList.addAll(appUsagePeriodList);
                            }
                        }
                    }
                    // Compute the screen on time and make sure it won't exceed the threshold.
                    final long screenOnTime = Math.min(
                            (long) TOTAL_HOURLY_TIME_THRESHOLD, getScreenOnTime(flatUsageList));
                    dailyScreenOnTime.put(hourlyIndex, screenOnTime);
                }
            }
        }
    }

    private static void insertDailyDeviceScreenOnTime(
            final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                    appUsagePeriodMap,
            final Map<Integer, Map<Integer, Long>> resultMap) {
        for (final int dailyIndex : appUsagePeriodMap.keySet()) {
            Map<Integer, Long> dailyResultMap = resultMap.get(dailyIndex);
            if (dailyResultMap == null) {
                dailyResultMap = new HashMap<>();
                resultMap.put(dailyIndex, dailyResultMap);
            }
            dailyResultMap.put(
                    SELECTED_INDEX_ALL,
                    getAccumulatedScreenOnTime(dailyResultMap));
        }
    }

    private static void insertAllDeviceScreenOnTime(
            final Map<Integer, Map<Integer, Long>> resultMap) {
        final Map<Integer, Long> dailyAllMap = new HashMap<>();
        resultMap.keySet().forEach(
                key -> dailyAllMap.put(key, resultMap.get(key).get(SELECTED_INDEX_ALL)));
        final Map<Integer, Long> allUsageMap = new HashMap<>();
        allUsageMap.put(SELECTED_INDEX_ALL, getAccumulatedScreenOnTime(dailyAllMap));
        resultMap.put(SELECTED_INDEX_ALL, allUsageMap);
    }

    private static long getAccumulatedScreenOnTime(final Map<Integer, Long> screenOnTimeMap) {
        if (screenOnTimeMap == null || screenOnTimeMap.isEmpty()) {
            return 0;
        }
        long sum = 0;
        for (final int index : screenOnTimeMap.keySet()) {
            sum += screenOnTimeMap.get(index) == null ? 0 : screenOnTimeMap.get(index);
        }
        return sum;
    }

    @Nullable
    private static UsageEvents getAppUsageEventsForUser(
            Context context, final UserManager userManager, final int userID,
            final long earliestTimestamp) {
        final String callingPackage = context.getPackageName();
        final long now = System.currentTimeMillis();
        // When the user is not unlocked, UsageStatsManager will return null, so bypass the
        // following data loading logics directly.
        if (!userManager.isUserUnlocked(userID)) {
            Log.w(TAG, "fail to load app usage event for user :" + userID + " because locked");
            return null;
        }
        final long startTime = DatabaseUtils.getAppUsageStartTimestampOfUser(
                context, userID, earliestTimestamp);
        return loadAppUsageEventsForUserFromService(
                sUsageStatsManager, startTime, now, userID, callingPackage);
    }

    @Nullable
    private static UsageEvents loadAppUsageEventsForUserFromService(
            final IUsageStatsManager usageStatsManager, final long startTime, final long endTime,
            final int userId, final String callingPackage) {
        final long start = System.currentTimeMillis();
        UsageEvents events = null;
        try {
            events = usageStatsManager.queryEventsForUser(
                    startTime, endTime, userId, callingPackage);
        } catch (RemoteException e) {
            Log.e(TAG, "Error fetching usage events: ", e);
        }
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(TAG, String.format("getAppUsageEventsForUser(): %d from %d to %d in %d/ms", userId,
                startTime, endTime, elapsedTime));
        return events;
    }

    @Nullable
    private static List<BatteryHistEntry> getBatteryHistListFromFromStatsService(
            final Context context) {
        List<BatteryHistEntry> batteryHistEntryList = null;
        try {
            final BatteryUsageStats batteryUsageStats = getBatteryUsageStats(context);
            final List<BatteryEntry> batteryEntryList =
                    generateBatteryEntryListFromBatteryUsageStats(context, batteryUsageStats);
            batteryHistEntryList = convertToBatteryHistEntry(batteryEntryList, batteryUsageStats);
            closeBatteryUsageStats(batteryUsageStats);
        } catch (RuntimeException e) {
            Log.e(TAG, "load batteryUsageStats:" + e);
        }

        return batteryHistEntryList;
    }

    @VisibleForTesting
    @Nullable
    static List<BatteryHistEntry> convertToBatteryHistEntry(
            @Nullable final List<BatteryEntry> batteryEntryList,
            final BatteryUsageStats batteryUsageStats) {
        if (batteryEntryList == null || batteryEntryList.isEmpty()) {
            Log.w(TAG, "batteryEntryList is null or empty in convertToBatteryHistEntry()");
            return null;
        }
        return batteryEntryList.stream()
                .filter(entry -> {
                    final long foregroundMs = entry.getTimeInForegroundMs();
                    final long backgroundMs = entry.getTimeInBackgroundMs();
                    return entry.getConsumedPower() > 0
                            || (entry.getConsumedPower() == 0
                            && (foregroundMs != 0 || backgroundMs != 0));
                })
                .map(entry -> ConvertUtils.convertToBatteryHistEntry(entry, batteryUsageStats))
                .collect(Collectors.toList());
    }

    /**
     * Interpolates history map based on expected timestamp slots and processes the corner case when
     * the expected start timestamp is earlier than what we have.
     */
    private static void interpolateHistory(
            Context context,
            final List<Long> rawTimestampList,
            final List<Long> expectedTimestampSlots,
            final long currentTime,
            final boolean isFromFullCharge,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap) {
        if (rawTimestampList.isEmpty() || expectedTimestampSlots.isEmpty()) {
            return;
        }
        final long expectedStartTimestamp = expectedTimestampSlots.get(0);
        final long rawStartTimestamp = rawTimestampList.get(0);
        int startIndex = 0;
        // If the expected start timestamp is full charge or earlier than what we have, use the
        // first data of what we have directly. This should be OK because the expected start
        // timestamp is the nearest even hour of the raw start timestamp, their time diff is no
        // more than 1 hour.
        if (isFromFullCharge || expectedStartTimestamp < rawStartTimestamp) {
            startIndex = 1;
            resultMap.put(expectedStartTimestamp, batteryHistoryMap.get(rawStartTimestamp));
        }
        final int expectedTimestampSlotsSize = expectedTimestampSlots.size();
        for (int index = startIndex; index < expectedTimestampSlotsSize; index++) {
            final long currentSlot = expectedTimestampSlots.get(index);
            if (currentSlot > currentTime) {
                // The slot timestamp is greater than the current time. Puts a placeholder first,
                // then in the async task, loads the real time battery usage data from the battery
                // stats service.
                // If current time is odd hour, one placeholder is added. If the current hour is
                // even hour, two placeholders are added. This is because the method
                // insertHourlyUsageDiffDataPerSlot() requires continuing three hours data.
                resultMap.put(currentSlot,
                        Map.of(CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER, EMPTY_BATTERY_HIST_ENTRY));
                continue;
            }
            final boolean isStartOrEnd = index == 0 || index == expectedTimestampSlotsSize - 1;
            interpolateHistoryForSlot(
                    context, currentSlot, rawTimestampList, batteryHistoryMap, resultMap,
                    isStartOrEnd);
        }
    }

    private static void interpolateHistoryForSlot(
            Context context,
            final long currentSlot,
            final List<Long> rawTimestampList,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap,
            final boolean isStartOrEnd) {
        final long[] nearestTimestamps = findNearestTimestamp(rawTimestampList, currentSlot);
        final long lowerTimestamp = nearestTimestamps[0];
        final long upperTimestamp = nearestTimestamps[1];
        // Case 1: upper timestamp is zero since scheduler is delayed!
        if (upperTimestamp == 0) {
            log(context, "job scheduler is delayed", currentSlot, null);
            resultMap.put(currentSlot, new HashMap<>());
            return;
        }
        // Case 2: upper timestamp is closed to the current timestamp.
        if ((upperTimestamp - currentSlot)
                < MAX_DIFF_SECONDS_OF_UPPER_TIMESTAMP * DateUtils.SECOND_IN_MILLIS) {
            log(context, "force align into the nearest slot", currentSlot, null);
            resultMap.put(currentSlot, batteryHistoryMap.get(upperTimestamp));
            return;
        }
        // Case 3: lower timestamp is zero before starting to collect data.
        if (lowerTimestamp == 0) {
            log(context, "no lower timestamp slot data", currentSlot, null);
            resultMap.put(currentSlot, new HashMap<>());
            return;
        }
        interpolateHistoryForSlot(context,
                currentSlot, lowerTimestamp, upperTimestamp, batteryHistoryMap, resultMap,
                isStartOrEnd);
    }

    private static void interpolateHistoryForSlot(
            Context context,
            final long currentSlot,
            final long lowerTimestamp,
            final long upperTimestamp,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap,
            final boolean isStartOrEnd) {
        final Map<String, BatteryHistEntry> lowerEntryDataMap =
                batteryHistoryMap.get(lowerTimestamp);
        final Map<String, BatteryHistEntry> upperEntryDataMap =
                batteryHistoryMap.get(upperTimestamp);
        // Verifies whether the lower data is valid to use or not by checking boot time.
        final BatteryHistEntry upperEntryDataFirstEntry =
                upperEntryDataMap.values().stream().findFirst().get();
        final long upperEntryDataBootTimestamp =
                upperEntryDataFirstEntry.mTimestamp - upperEntryDataFirstEntry.mBootTimestamp;
        // Lower data is captured before upper data corresponding device is booting.
        // Skips the booting-specific logics and always does interpolation for daily chart level
        // data.
        if (lowerTimestamp < upperEntryDataBootTimestamp
                && !isForDailyChart(isStartOrEnd, currentSlot)) {
            // Provides an opportunity to force align the slot directly.
            if ((upperTimestamp - currentSlot) < 10 * DateUtils.MINUTE_IN_MILLIS) {
                log(context, "force align into the nearest slot", currentSlot, null);
                resultMap.put(currentSlot, upperEntryDataMap);
            } else {
                log(context, "in the different booting section", currentSlot, null);
                resultMap.put(currentSlot, new HashMap<>());
            }
            return;
        }
        log(context, "apply interpolation arithmetic", currentSlot, null);
        final Map<String, BatteryHistEntry> newHistEntryMap = new HashMap<>();
        final double timestampLength = upperTimestamp - lowerTimestamp;
        final double timestampDiff = currentSlot - lowerTimestamp;
        // Applies interpolation arithmetic for each BatteryHistEntry.
        for (String entryKey : upperEntryDataMap.keySet()) {
            final BatteryHistEntry lowerEntry = lowerEntryDataMap.get(entryKey);
            final BatteryHistEntry upperEntry = upperEntryDataMap.get(entryKey);
            // Checks whether there is any abnormal battery reset conditions.
            if (lowerEntry != null) {
                final boolean invalidForegroundUsageTime =
                        lowerEntry.mForegroundUsageTimeInMs > upperEntry.mForegroundUsageTimeInMs;
                final boolean invalidBackgroundUsageTime =
                        lowerEntry.mBackgroundUsageTimeInMs > upperEntry.mBackgroundUsageTimeInMs;
                if (invalidForegroundUsageTime || invalidBackgroundUsageTime) {
                    newHistEntryMap.put(entryKey, upperEntry);
                    log(context, "abnormal reset condition is found", currentSlot, upperEntry);
                    continue;
                }
            }
            final BatteryHistEntry newEntry =
                    BatteryHistEntry.interpolate(
                            currentSlot,
                            upperTimestamp,
                            /*ratio=*/ timestampDiff / timestampLength,
                            lowerEntry,
                            upperEntry);
            newHistEntryMap.put(entryKey, newEntry);
            if (lowerEntry == null) {
                log(context, "cannot find lower entry data", currentSlot, upperEntry);
                continue;
            }
        }
        resultMap.put(currentSlot, newHistEntryMap);
    }

    /**
     * @return Returns the nearest even hour timestamp of the given timestamp.
     */
    private static long getNearestEvenHourTimestamp(long rawTimestamp) {
        // If raw hour is even, the nearest even hour should be the even hour before raw
        // start. The hour doesn't need to change and just set the minutes and seconds to 0.
        // Otherwise, the nearest even hour should be raw hour + 1.
        // For example, the nearest hour of 14:30:50 should be 14:00:00. While the nearest
        // hour of 15:30:50 should be 16:00:00.
        return getEvenHourTimestamp(rawTimestamp, /*addHourOfDay*/ 1);
    }

    /**
     * @return Returns the fist even hour timestamp after the given timestamp.
     */
    private static long getFirstEvenHourAfterTimestamp(long rawTimestamp) {
        return getLastEvenHourBeforeTimestamp(rawTimestamp + DateUtils.HOUR_IN_MILLIS * 2);
    }

    /**
     * @return Returns the last even hour timestamp before the given timestamp.
     */
    private static long getLastEvenHourBeforeTimestamp(long rawTimestamp) {
        // If raw hour is even, the hour doesn't need to change as well.
        // Otherwise, the even hour before raw end should be raw hour - 1.
        // For example, the even hour before 14:30:50 should be 14:00:00. While the even
        // hour before 15:30:50 should be 14:00:00.
        return getEvenHourTimestamp(rawTimestamp, /*addHourOfDay*/ -1);
    }

    private static long getEvenHourTimestamp(long rawTimestamp, int addHourOfDay) {
        final Calendar evenHourCalendar = Calendar.getInstance();
        evenHourCalendar.setTimeInMillis(rawTimestamp);
        // Before computing the evenHourCalendar, record raw hour based on local timezone.
        final int rawHour = evenHourCalendar.get(Calendar.HOUR_OF_DAY);
        if (rawHour % 2 != 0) {
            evenHourCalendar.add(Calendar.HOUR_OF_DAY, addHourOfDay);
        }
        evenHourCalendar.set(Calendar.MINUTE, 0);
        evenHourCalendar.set(Calendar.SECOND, 0);
        evenHourCalendar.set(Calendar.MILLISECOND, 0);
        return evenHourCalendar.getTimeInMillis();
    }

    private static List<List<Long>> getHourlyTimestamps(final List<Long> dailyTimestamps) {
        final List<List<Long>> hourlyTimestamps = new ArrayList<>();
        if (dailyTimestamps.size() < MIN_DAILY_DATA_SIZE) {
            return hourlyTimestamps;
        }

        for (int dailyStartIndex = 0; dailyStartIndex < dailyTimestamps.size() - 1;
                dailyStartIndex++) {
            long currentTimestamp = dailyTimestamps.get(dailyStartIndex);
            final long dailyEndTimestamp = dailyTimestamps.get(dailyStartIndex + 1);
            final List<Long> hourlyTimestampsPerDay = new ArrayList<>();
            while (currentTimestamp <= dailyEndTimestamp) {
                hourlyTimestampsPerDay.add(currentTimestamp);
                currentTimestamp += MIN_TIME_SLOT;
            }
            hourlyTimestamps.add(hourlyTimestampsPerDay);
        }
        return hourlyTimestamps;
    }

    private static List<BatteryLevelData.PeriodBatteryLevelData> getHourlyPeriodBatteryLevelData(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap,
            final List<List<Long>> timestamps) {
        final List<BatteryLevelData.PeriodBatteryLevelData> levelData = new ArrayList<>();
        timestamps.forEach(
                timestampList -> levelData.add(
                        getPeriodBatteryLevelData(
                                context, processedBatteryHistoryMap, timestampList)));
        return levelData;
    }

    private static BatteryLevelData.PeriodBatteryLevelData getPeriodBatteryLevelData(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap,
            final List<Long> timestamps) {
        final List<Integer> levels = new ArrayList<>();
        timestamps.forEach(
                timestamp -> levels.add(getLevel(context, processedBatteryHistoryMap, timestamp)));
        return new BatteryLevelData.PeriodBatteryLevelData(timestamps, levels);
    }

    private static Integer getLevel(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap,
            final long timestamp) {
        final Map<String, BatteryHistEntry> entryMap = processedBatteryHistoryMap.get(timestamp);
        if (entryMap == null || entryMap.isEmpty()) {
            Log.e(TAG, "abnormal entry list in the timestamp:"
                    + utcToLocalTime(context, timestamp));
            return null;
        }
        // The current time battery history hasn't been loaded yet, returns the current battery
        // level.
        if (entryMap.containsKey(CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER)) {
            final Intent intent = BatteryUtils.getBatteryIntent(context);
            return BatteryStatus.getBatteryLevel(intent);
        }
        // Averages the battery level in each time slot to avoid corner conditions.
        float batteryLevelCounter = 0;
        for (BatteryHistEntry entry : entryMap.values()) {
            batteryLevelCounter += entry.mBatteryLevel;
        }
        return Math.round(batteryLevelCounter / entryMap.size());
    }

    private static void insertHourlyUsageDiffData(
            Context context,
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                    appUsagePeriodMap,
            final Map<Integer, Map<Integer, BatteryDiffData>> resultMap) {
        final int currentUserId = context.getUserId();
        final UserHandle userHandle =
                Utils.getManagedProfile(context.getSystemService(UserManager.class));
        final int workProfileUserId =
                userHandle != null ? userHandle.getIdentifier() : Integer.MIN_VALUE;
        // Each time slot usage diff data =
        //     Math.abs(timestamp[i+2] data - timestamp[i+1] data) +
        //     Math.abs(timestamp[i+1] data - timestamp[i] data);
        // since we want to aggregate every two hours data into a single time slot.
        for (int dailyIndex = 0; dailyIndex < hourlyBatteryLevelsPerDay.size(); dailyIndex++) {
            final Map<Integer, BatteryDiffData> dailyDiffMap = new HashMap<>();
            resultMap.put(dailyIndex, dailyDiffMap);
            if (hourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> timestamps = hourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            for (int hourlyIndex = 0; hourlyIndex < timestamps.size() - 1; hourlyIndex++) {
                final BatteryDiffData hourlyBatteryDiffData =
                        insertHourlyUsageDiffDataPerSlot(
                                context,
                                currentUserId,
                                workProfileUserId,
                                hourlyIndex,
                                timestamps,
                                appUsagePeriodMap == null
                                        || appUsagePeriodMap.get(dailyIndex) == null
                                        ? null
                                        : appUsagePeriodMap.get(dailyIndex).get(hourlyIndex),
                                batteryHistoryMap);
                dailyDiffMap.put(hourlyIndex, hourlyBatteryDiffData);
            }
        }
    }

    private static void insertDailyUsageDiffData(
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            final Map<Integer, Map<Integer, BatteryDiffData>> resultMap) {
        for (int index = 0; index < hourlyBatteryLevelsPerDay.size(); index++) {
            Map<Integer, BatteryDiffData> dailyUsageMap = resultMap.get(index);
            if (dailyUsageMap == null) {
                dailyUsageMap = new HashMap<>();
                resultMap.put(index, dailyUsageMap);
            }
            dailyUsageMap.put(
                    SELECTED_INDEX_ALL,
                    getAccumulatedUsageDiffData(dailyUsageMap.values()));
        }
    }

    private static void insertAllUsageDiffData(
            final Map<Integer, Map<Integer, BatteryDiffData>> resultMap) {
        final List<BatteryDiffData> diffDataList = new ArrayList<>();
        resultMap.keySet().forEach(
                key -> diffDataList.add(resultMap.get(key).get(SELECTED_INDEX_ALL)));
        final Map<Integer, BatteryDiffData> allUsageMap = new HashMap<>();
        allUsageMap.put(SELECTED_INDEX_ALL, getAccumulatedUsageDiffData(diffDataList));
        resultMap.put(SELECTED_INDEX_ALL, allUsageMap);
    }

    @Nullable
    private static BatteryDiffData insertHourlyUsageDiffDataPerSlot(
            Context context,
            final int currentUserId,
            final int workProfileUserId,
            final int currentIndex,
            final List<Long> timestamps,
            final Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        final List<BatteryDiffEntry> systemEntries = new ArrayList<>();

        final Long currentTimestamp = timestamps.get(currentIndex);
        final Long nextTimestamp = currentTimestamp + DateUtils.HOUR_IN_MILLIS;
        final Long nextTwoTimestamp = nextTimestamp + DateUtils.HOUR_IN_MILLIS;
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
            return null;
        }

        // Collects all keys in these three time slot records as all populations.
        final Set<String> allBatteryHistEntryKeys = new ArraySet<>();
        allBatteryHistEntryKeys.addAll(currentBatteryHistMap.keySet());
        allBatteryHistEntryKeys.addAll(nextBatteryHistMap.keySet());
        allBatteryHistEntryKeys.addAll(nextTwoBatteryHistMap.keySet());

        // Calculates all packages diff usage data in a specific time slot.
        for (String key : allBatteryHistEntryKeys) {
            if (key == null) {
                continue;
            }
            final BatteryHistEntry currentEntry =
                    currentBatteryHistMap.getOrDefault(key, EMPTY_BATTERY_HIST_ENTRY);
            final BatteryHistEntry nextEntry =
                    nextBatteryHistMap.getOrDefault(key, EMPTY_BATTERY_HIST_ENTRY);
            final BatteryHistEntry nextTwoEntry =
                    nextTwoBatteryHistMap.getOrDefault(key, EMPTY_BATTERY_HIST_ENTRY);

            final BatteryHistEntry selectedBatteryEntry =
                    selectBatteryHistEntry(currentEntry, nextEntry, nextTwoEntry);
            if (selectedBatteryEntry == null) {
                continue;
            }

            // Not show other users' battery usage data.
            final boolean isFromOtherUsers = isConsumedFromOtherUsers(
                    currentUserId, workProfileUserId, selectedBatteryEntry);
            if (isFromOtherUsers) {
                continue;
            }

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
            double foregroundUsageConsumePower =
                    getDiffValue(
                            currentEntry.mForegroundUsageConsumePower,
                            nextEntry.mForegroundUsageConsumePower,
                            nextTwoEntry.mForegroundUsageConsumePower);
            double foregroundServiceUsageConsumePower =
                    getDiffValue(
                            currentEntry.mForegroundServiceUsageConsumePower,
                            nextEntry.mForegroundServiceUsageConsumePower,
                            nextTwoEntry.mForegroundServiceUsageConsumePower);
            double backgroundUsageConsumePower =
                    getDiffValue(
                            currentEntry.mBackgroundUsageConsumePower,
                            nextEntry.mBackgroundUsageConsumePower,
                            nextTwoEntry.mBackgroundUsageConsumePower);
            double cachedUsageConsumePower =
                    getDiffValue(
                            currentEntry.mCachedUsageConsumePower,
                            nextEntry.mCachedUsageConsumePower,
                            nextTwoEntry.mCachedUsageConsumePower);
            // Excludes entry since we don't have enough data to calculate.
            if (foregroundUsageTimeInMs == 0
                    && backgroundUsageTimeInMs == 0
                    && consumePower == 0) {
                continue;
            }
            // Forces refine the cumulative value since it may introduce deviation error since we
            // will apply the interpolation arithmetic.
            final float totalUsageTimeInMs =
                    foregroundUsageTimeInMs + backgroundUsageTimeInMs;
            if (totalUsageTimeInMs > TOTAL_HOURLY_TIME_THRESHOLD) {
                final float ratio = TOTAL_HOURLY_TIME_THRESHOLD / totalUsageTimeInMs;
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
                foregroundUsageConsumePower = foregroundUsageConsumePower * ratio;
                foregroundServiceUsageConsumePower = foregroundServiceUsageConsumePower * ratio;
                backgroundUsageConsumePower = backgroundUsageConsumePower * ratio;
                cachedUsageConsumePower = cachedUsageConsumePower * ratio;
            }

            // Compute the screen on time and make sure it won't exceed the threshold.
            final long screenOnTime = Math.min(
                    (long) TOTAL_HOURLY_TIME_THRESHOLD,
                    getScreenOnTime(
                            appUsageMap,
                            selectedBatteryEntry.mUserId,
                            selectedBatteryEntry.mPackageName));
            final BatteryDiffEntry currentBatteryDiffEntry = new BatteryDiffEntry(
                    context,
                    foregroundUsageTimeInMs,
                    backgroundUsageTimeInMs,
                    screenOnTime,
                    consumePower,
                    foregroundUsageConsumePower,
                    foregroundServiceUsageConsumePower,
                    backgroundUsageConsumePower,
                    cachedUsageConsumePower,
                    selectedBatteryEntry);
            if (currentBatteryDiffEntry.isSystemEntry()) {
                systemEntries.add(currentBatteryDiffEntry);
            } else {
                appEntries.add(currentBatteryDiffEntry);
            }
        }

        // If there is no data, return null instead of empty item.
        if (appEntries.isEmpty() && systemEntries.isEmpty()) {
            return null;
        }

        return new BatteryDiffData(appEntries, systemEntries);
    }

    private static long getScreenOnTime(@Nullable final List<AppUsagePeriod> appUsagePeriodList) {
        if (appUsagePeriodList == null || appUsagePeriodList.isEmpty()) {
            return 0;
        }
        // Create a list of endpoints (the beginning or the end) of usage periods and order the list
        // chronologically.
        final List<AppUsageEndPoint> endPoints =
                appUsagePeriodList.stream()
                        .flatMap(
                                foregroundUsage ->
                                        Stream.of(
                                                AppUsageEndPoint.newBuilder()
                                                        .setTimestamp(
                                                                foregroundUsage.getStartTime())
                                                        .setType(AppUsageEndPointType.START)
                                                        .build(),
                                                AppUsageEndPoint.newBuilder()
                                                        .setTimestamp(foregroundUsage.getEndTime())
                                                        .setType(AppUsageEndPointType.END)
                                                        .build()))
                        .sorted((x, y) -> (int) (x.getTimestamp() - y.getTimestamp()))
                        .collect(Collectors.toList());

        // Traverse the list of endpoints in order to determine the non-overlapping usage duration.
        int numberOfActiveAppUsagePeriods = 0;
        long startOfCurrentContiguousAppUsagePeriod = 0;
        long totalScreenOnTime = 0;
        for (final AppUsageEndPoint endPoint : endPoints) {
            if (endPoint.getType() == AppUsageEndPointType.START) {
                if (numberOfActiveAppUsagePeriods++ == 0) {
                    startOfCurrentContiguousAppUsagePeriod = endPoint.getTimestamp();
                }
            } else {
                if (--numberOfActiveAppUsagePeriods == 0) {
                    totalScreenOnTime +=
                            (endPoint.getTimestamp() - startOfCurrentContiguousAppUsagePeriod);
                }
            }
        }

        return totalScreenOnTime;
    }

    private static boolean isConsumedFromOtherUsers(
            final int currentUserId,
            final int workProfileUserId,
            final BatteryHistEntry batteryHistEntry) {
        return batteryHistEntry.mConsumerType == ConvertUtils.CONSUMER_TYPE_UID_BATTERY
                && batteryHistEntry.mUserId != currentUserId
                && batteryHistEntry.mUserId != workProfileUserId;
    }

    @Nullable
    private static BatteryDiffData getAccumulatedUsageDiffData(
            final Collection<BatteryDiffData> diffEntryListData) {
        final Map<String, BatteryDiffEntry> diffEntryMap = new HashMap<>();
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        final List<BatteryDiffEntry> systemEntries = new ArrayList<>();

        for (BatteryDiffData diffEntryList : diffEntryListData) {
            if (diffEntryList == null) {
                continue;
            }
            for (BatteryDiffEntry entry : diffEntryList.getAppDiffEntryList()) {
                computeUsageDiffDataPerEntry(entry, diffEntryMap);
            }
            for (BatteryDiffEntry entry : diffEntryList.getSystemDiffEntryList()) {
                computeUsageDiffDataPerEntry(entry, diffEntryMap);
            }
        }

        final Collection<BatteryDiffEntry> diffEntryList = diffEntryMap.values();
        for (BatteryDiffEntry entry : diffEntryList) {
            if (entry.isSystemEntry()) {
                systemEntries.add(entry);
            } else {
                appEntries.add(entry);
            }
        }

        return diffEntryList.isEmpty() ? null : new BatteryDiffData(appEntries, systemEntries);
    }

    private static void computeUsageDiffDataPerEntry(
            final BatteryDiffEntry entry,
            final Map<String, BatteryDiffEntry> diffEntryMap) {
        final String key = entry.mBatteryHistEntry.getKey();
        final BatteryDiffEntry oldBatteryDiffEntry = diffEntryMap.get(key);
        // Creates new BatteryDiffEntry if we don't have it.
        if (oldBatteryDiffEntry == null) {
            diffEntryMap.put(key, entry.clone());
        } else {
            // Sums up some field data into the existing one.
            oldBatteryDiffEntry.mForegroundUsageTimeInMs +=
                    entry.mForegroundUsageTimeInMs;
            oldBatteryDiffEntry.mBackgroundUsageTimeInMs +=
                    entry.mBackgroundUsageTimeInMs;
            oldBatteryDiffEntry.mScreenOnTimeInMs +=
                    entry.mScreenOnTimeInMs;
            oldBatteryDiffEntry.mConsumePower += entry.mConsumePower;
            oldBatteryDiffEntry.mForegroundUsageConsumePower += entry.mForegroundUsageConsumePower;
            oldBatteryDiffEntry.mForegroundServiceUsageConsumePower
                    += entry.mForegroundServiceUsageConsumePower;
            oldBatteryDiffEntry.mBackgroundUsageConsumePower += entry.mBackgroundUsageConsumePower;
            oldBatteryDiffEntry.mCachedUsageConsumePower += entry.mCachedUsageConsumePower;
        }
    }

    // Process every battery diff data in the battery usage result map.
    // (1) Removes low percentage data and fake usage data, which will be zero value.
    // (2) Sets total consume power, so the usage percentage is updated.
    // (3) Sorts the result.
    private static void processBatteryDiffData(
            final Context context,
            final Map<Integer, Map<Integer, BatteryDiffData>> resultMap) {
        final Set<Integer> hideSystemComponentSet =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideSystemComponentSet(context);
        final Set<CharSequence> hideBackgroundUsageTimeSet =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideBackgroundUsageTimeSet(context);
        final Set<CharSequence> hideApplicationSet =
                FeatureFactory.getFactory(context)
                        .getPowerUsageFeatureProvider(context)
                        .getHideApplicationSet(context);
        resultMap.keySet().forEach(dailyKey -> {
            final Map<Integer, BatteryDiffData> dailyUsageMap = resultMap.get(dailyKey);
            dailyUsageMap.values().forEach(batteryDiffData -> {
                if (batteryDiffData == null) {
                    return;
                }
                purgeFakeAndHiddenPackages(
                        batteryDiffData.getAppDiffEntryList(),
                        hideSystemComponentSet,
                        hideApplicationSet,
                        hideBackgroundUsageTimeSet);
                purgeFakeAndHiddenPackages(
                        batteryDiffData.getSystemDiffEntryList(),
                        hideSystemComponentSet,
                        hideApplicationSet,
                        hideBackgroundUsageTimeSet);
                batteryDiffData.setTotalConsumePower();
                batteryDiffData.sortEntries();
            });
        });
    }

    private static void purgeFakeAndHiddenPackages(
            final List<BatteryDiffEntry> entries,
            final Set<Integer> hideSystemComponentSet,
            final Set<CharSequence> hideApplicationSet,
            final Set<CharSequence> hideBackgroundUsageTimeSet) {
        final Iterator<BatteryDiffEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            final BatteryDiffEntry entry = iterator.next();
            final String packageName = entry.getPackageName();
            final Integer componentId = entry.mBatteryHistEntry.mDrainType;
            if (ConvertUtils.FAKE_PACKAGE_NAME.equals(packageName)
                    || hideSystemComponentSet.contains(componentId)
                    || hideApplicationSet.contains(packageName)) {
                iterator.remove();
            }
            if (hideBackgroundUsageTimeSet.contains(packageName)) {
                entry.mBackgroundUsageTimeInMs = 0;
            }
        }
    }

    private static boolean shouldShowBatteryAttributionList(final Context context) {
        final PowerProfile powerProfile = new PowerProfile(context);
        // Cheap hack to try to figure out if the power_profile.xml was populated.
        final double averagePowerForOrdinal = powerProfile.getAveragePowerForOrdinal(
                PowerProfile.POWER_GROUP_DISPLAY_SCREEN_FULL, 0);
        final boolean shouldShowBatteryAttributionList =
                averagePowerForOrdinal >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP;
        if (!shouldShowBatteryAttributionList) {
            Log.w(TAG, "shouldShowBatteryAttributionList(): " + averagePowerForOrdinal);
        }
        return shouldShowBatteryAttributionList;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     *
     * @return A sorted list of apps using power.
     */
    private static List<BatteryEntry> getCoalescedUsageList(final Context context,
            final BatteryUtils batteryUtils,
            final BatteryUsageStats batteryUsageStats,
            final boolean loadDataInBackground) {
        final PackageManager packageManager = context.getPackageManager();
        final UserManager userManager = context.getSystemService(UserManager.class);
        final SparseArray<BatteryEntry> batteryEntryList = new SparseArray<>();
        final ArrayList<BatteryEntry> results = new ArrayList<>();
        final List<UidBatteryConsumer> uidBatteryConsumers =
                batteryUsageStats.getUidBatteryConsumers();

        // Sort to have all apps with "real" UIDs first, followed by apps that are supposed
        // to be combined with the real ones.
        uidBatteryConsumers.sort(Comparator.comparingInt(
                consumer -> consumer.getUid() == getRealUid(consumer) ? 0 : 1));

        for (int i = 0, size = uidBatteryConsumers.size(); i < size; i++) {
            final UidBatteryConsumer consumer = uidBatteryConsumers.get(i);
            final int uid = getRealUid(consumer);

            final String[] packages = packageManager.getPackagesForUid(uid);
            if (batteryUtils.shouldHideUidBatteryConsumerUnconditionally(consumer, packages)) {
                continue;
            }

            final boolean isHidden = batteryUtils.shouldHideUidBatteryConsumer(consumer, packages);
            final int index = batteryEntryList.indexOfKey(uid);
            if (index < 0) {
                // New entry.
                batteryEntryList.put(uid, new BatteryEntry(context, userManager, consumer,
                        isHidden, uid, packages, null, loadDataInBackground));
            } else {
                // Combine BatterySippers if we already have one with this UID.
                final BatteryEntry existingSipper = batteryEntryList.valueAt(index);
                existingSipper.add(consumer);
            }
        }

        final BatteryConsumer deviceConsumer = batteryUsageStats.getAggregateBatteryConsumer(
                BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_DEVICE);

        for (int componentId = 0; componentId < BatteryConsumer.POWER_COMPONENT_COUNT;
                componentId++) {
            results.add(new BatteryEntry(context, componentId,
                    deviceConsumer.getConsumedPower(componentId),
                    deviceConsumer.getUsageDurationMillis(componentId)));
        }

        for (int componentId = BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID;
                componentId < BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID
                        + deviceConsumer.getCustomPowerComponentCount();
                componentId++) {
            results.add(new BatteryEntry(context, componentId,
                    deviceConsumer.getCustomPowerComponentName(componentId),
                    deviceConsumer.getConsumedPowerForCustomComponent(componentId)));
        }

        final List<UserBatteryConsumer> userBatteryConsumers =
                batteryUsageStats.getUserBatteryConsumers();
        for (int i = 0, size = userBatteryConsumers.size(); i < size; i++) {
            final UserBatteryConsumer consumer = userBatteryConsumers.get(i);
            results.add(new BatteryEntry(context, userManager, consumer, /* isHidden */ true,
                    Process.INVALID_UID, null, null, loadDataInBackground));
        }

        final int numUidSippers = batteryEntryList.size();

        for (int i = 0; i < numUidSippers; i++) {
            results.add(batteryEntryList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        results.sort(BatteryEntry.COMPARATOR);
        return results;
    }

    private static int getRealUid(final UidBatteryConsumer consumer) {
        int realUid = consumer.getUid();

        // Check if this UID is a shared GID. If so, we combine it with the OWNER's
        // actual app UID.
        if (isSharedGid(consumer.getUid())) {
            realUid = UserHandle.getUid(UserHandle.USER_SYSTEM,
                    UserHandle.getAppIdFromSharedAppGid(consumer.getUid()));
        }

        // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
        if (isSystemUid(realUid)
                && !MEDIASERVER_PACKAGE_NAME.equals(consumer.getPackageWithHighestDrain())) {
            // Use the system UID for all UIDs running in their own sandbox that
            // are not apps. We exclude mediaserver because we already are expected to
            // report that as a separate item.
            realUid = Process.SYSTEM_UID;
        }
        return realUid;
    }

    private static boolean isSharedGid(final int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(final int uid) {
        final int appUid = UserHandle.getAppId(uid);
        return appUid >= Process.SYSTEM_UID && appUid < Process.FIRST_APPLICATION_UID;
    }

    private static boolean isUsageMapValid(
            final Map<Integer, Map<Integer, BatteryDiffData>> batteryUsageMap,
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay) {
        if (batteryUsageMap.get(SELECTED_INDEX_ALL) == null
                || !batteryUsageMap.get(SELECTED_INDEX_ALL).containsKey(SELECTED_INDEX_ALL)) {
            Log.e(TAG, "no [SELECTED_INDEX_ALL][SELECTED_INDEX_ALL] in batteryUsageMap");
            return false;
        }
        for (int dailyIndex = 0; dailyIndex < hourlyBatteryLevelsPerDay.size(); dailyIndex++) {
            if (batteryUsageMap.get(dailyIndex) == null
                    || !batteryUsageMap.get(dailyIndex).containsKey(SELECTED_INDEX_ALL)) {
                Log.e(TAG, "no [" + dailyIndex + "][SELECTED_INDEX_ALL] in batteryUsageMap, "
                        + "daily size is: " + hourlyBatteryLevelsPerDay.size());
                return false;
            }
            if (hourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> timestamps = hourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            // Length of hourly usage map should be the length of hourly level data - 1.
            for (int hourlyIndex = 0; hourlyIndex < timestamps.size() - 1; hourlyIndex++) {
                if (!batteryUsageMap.get(dailyIndex).containsKey(hourlyIndex)) {
                    Log.e(TAG, "no [" + dailyIndex + "][" + hourlyIndex + "] in batteryUsageMap, "
                            + "hourly size is: " + (timestamps.size() - 1));
                    return false;
                }
            }
        }
        return true;
    }

    private static long getTimestampWithDayDiff(final long timestamp, final int dayDiff) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.add(Calendar.DAY_OF_YEAR, dayDiff);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static long getDiffValue(long v1, long v2, long v3) {
        return (v2 > v1 ? v2 - v1 : 0) + (v3 > v2 ? v3 - v2 : 0);
    }

    private static double getDiffValue(double v1, double v2, double v3) {
        return (v2 > v1 ? v2 - v1 : 0) + (v3 > v2 ? v3 - v2 : 0);
    }

    @Nullable
    private static BatteryHistEntry selectBatteryHistEntry(
            final BatteryHistEntry... batteryHistEntries) {
        for (BatteryHistEntry entry : batteryHistEntries) {
            if (entry != null && entry != EMPTY_BATTERY_HIST_ENTRY) {
                return entry;
            }
        }
        return null;
    }

    private static long getCurrentTimeMillis() {
        return sFakeCurrentTimeMillis > 0 ? sFakeCurrentTimeMillis : System.currentTimeMillis();
    }

    private static void log(Context context, final String content, final long timestamp,
            final BatteryHistEntry entry) {
        if (DEBUG) {
            Log.d(TAG, String.format(entry != null ? "%s %s:\n%s" : "%s %s:%s",
                    utcToLocalTime(context, timestamp), content, entry));
        }
    }
}
