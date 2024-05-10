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
import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.isSystemConsumer;
import static com.android.settings.fuelgauge.batteryusage.ConvertUtils.isUidConsumer;
import static com.android.settingslib.fuelgauge.BatteryStatus.BATTERY_LEVEL_UNKNOWN;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.PowerProfile;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.fuelgauge.BatteryStatus;
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryUtil;

import com.google.common.base.Preconditions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    private static final String TAG = "DataProcessor";
    private static final int POWER_COMPONENT_SYSTEM_SERVICES = 7;
    private static final int POWER_COMPONENT_WAKELOCK = 12;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final int MIN_DAILY_DATA_SIZE = 2;
    private static final int MAX_DIFF_SECONDS_OF_UPPER_TIMESTAMP = 5;
    private static final String MEDIASERVER_PACKAGE_NAME = "mediaserver";
    private static final String ANDROID_CORE_APPS_SHARED_USER_ID = "android.uid.shared";
    private static final Map<String, BatteryHistEntry> EMPTY_BATTERY_MAP = new ArrayMap<>();
    private static final BatteryHistEntry EMPTY_BATTERY_HIST_ENTRY =
            new BatteryHistEntry(new ContentValues());

    @VisibleForTesting
    static final long DEFAULT_USAGE_DURATION_FOR_INCOMPLETE_INTERVAL =
            DateUtils.SECOND_IN_MILLIS * 30;

    @VisibleForTesting
    static final int SELECTED_INDEX_ALL = BatteryChartViewModel.SELECTED_INDEX_ALL;

    @VisibleForTesting
    static final Comparator<AppUsageEvent> APP_USAGE_EVENT_TIMESTAMP_COMPARATOR =
            Comparator.comparing(AppUsageEvent::getTimestamp);

    @VisibleForTesting
    static final Comparator<BatteryEvent> BATTERY_EVENT_TIMESTAMP_COMPARATOR =
            Comparator.comparing(BatteryEvent::getTimestamp);

    @VisibleForTesting static boolean sDebug = false;

    @VisibleForTesting static long sTestCurrentTimeMillis = 0;

    @VisibleForTesting static Set<String> sTestSystemAppsPackageNames;

    @VisibleForTesting
    static IUsageStatsManager sUsageStatsManager =
            IUsageStatsManager.Stub.asInterface(
                    ServiceManager.getService(Context.USAGE_STATS_SERVICE));

    public static final String CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER =
            "CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER";

    /** A callback listener when battery usage loading async task is executed. */
    public interface UsageMapAsyncResponse {
        /** The callback function when batteryUsageMap is loaded. */
        void onBatteryCallbackDataLoaded(
                Map<Integer, Map<Integer, BatteryDiffData>> batteryCallbackData);
    }

    private DataProcessor() {}

    /**
     * @return Returns battery usage data of different entries. <br>
     *     Returns null if the input is invalid or there is no enough data.
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
        // Loads the current battery usage data from the battery stats service.
        final Map<String, BatteryHistEntry> currentBatteryHistoryMap =
                getCurrentBatteryHistoryMapFromStatsService(context);
        // Replaces the placeholder in processedBatteryHistoryMap.
        for (Map.Entry<Long, Map<String, BatteryHistEntry>> mapEntry :
                processedBatteryHistoryMap.entrySet()) {
            if (mapEntry.getValue().containsKey(CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER)) {
                mapEntry.setValue(currentBatteryHistoryMap);
            }
        }
        return batteryLevelData == null
                ? null
                : generateBatteryUsageMap(
                        context,
                        getBatteryDiffDataMap(
                                context,
                                batteryLevelData.getHourlyBatteryLevelsPerDay(),
                                processedBatteryHistoryMap,
                                /* appUsagePeriodMap= */ null,
                                getSystemAppsPackageNames(context),
                                getSystemAppsUids(context)),
                        batteryLevelData);
    }

    /** Gets the {@link BatteryUsageStats} from system service. */
    @Nullable
    public static BatteryUsageStats getBatteryUsageStats(final Context context) {
        final BatteryUsageStatsQuery batteryUsageStatsQuery =
                new BatteryUsageStatsQuery.Builder()
                        .includeBatteryHistory()
                        .includeProcessStateData()
                        .build();
        return context.getSystemService(BatteryStatsManager.class)
                .getBatteryUsageStats(batteryUsageStatsQuery);
    }

    /** Gets the {@link UsageEvents} from system service for all unlocked users. */
    @Nullable
    public static Map<Long, UsageEvents> getAppUsageEvents(Context context) {
        final long start = System.currentTimeMillis();
        context = DatabaseUtils.getParentContext(context);
        if (context == null) {
            return null;
        }
        final Map<Long, UsageEvents> resultMap = new ArrayMap();
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (userManager == null) {
            return null;
        }
        final long sixDaysAgoTimestamp =
                DatabaseUtils.getTimestampSixDaysAgo(Calendar.getInstance());
        for (final UserInfo user : userManager.getAliveUsers()) {
            final UsageEvents events =
                    getAppUsageEventsForUser(context, userManager, user.id, sixDaysAgoTimestamp);
            if (events != null) {
                resultMap.put(Long.valueOf(user.id), events);
            }
        }
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(
                TAG,
                String.format("getAppUsageEvents() for all unlocked users in %d/ms", elapsedTime));
        return resultMap.isEmpty() ? null : resultMap;
    }

    /** Gets the {@link UsageEvents} from system service for the specific user. */
    @Nullable
    public static UsageEvents getAppUsageEventsForUser(
            Context context, final int userID, final long startTimestampOfLevelData) {
        final long start = System.currentTimeMillis();
        context = DatabaseUtils.getParentContext(context);
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
        final UsageEvents events =
                getAppUsageEventsForUser(context, userManager, userID, earliestTimestamp);
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(
                TAG,
                String.format(
                        "getAppUsageEventsForUser() for user %d in %d/ms", userID, elapsedTime));
        return events;
    }

    /** Closes the {@link BatteryUsageStats} after using it. */
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
     * <p>There could be 2 cases of the returned value:
     *
     * <ul>
     *   <li>null: empty or invalid data.
     *   <li>non-null: must be a 2d map and composed by:
     *       <p>[0][0] ~ [maxDailyIndex][maxHourlyIndex]
     * </ul>
     *
     * <p>The structure is consistent with the battery usage map returned by {@code
     * generateBatteryUsageMap}.
     *
     * <p>{@code Long} stands for the userId.
     *
     * <p>{@code String} stands for the packageName.
     */
    @Nullable
    public static Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
            generateAppUsagePeriodMap(
                    Context context,
                    final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
                    final List<AppUsageEvent> appUsageEventList,
                    final List<BatteryEvent> batteryEventList) {
        if (appUsageEventList.isEmpty()) {
            Log.w(TAG, "appUsageEventList is empty");
            return null;
        }
        // Sorts the appUsageEventList and batteryEventList in ascending order based on the
        // timestamp before distribution.
        Collections.sort(appUsageEventList, APP_USAGE_EVENT_TIMESTAMP_COMPARATOR);
        Collections.sort(batteryEventList, BATTERY_EVENT_TIMESTAMP_COMPARATOR);
        final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>> resultMap =
                new ArrayMap<>();

        for (int dailyIndex = 0; dailyIndex < hourlyBatteryLevelsPerDay.size(); dailyIndex++) {
            final Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>> dailyMap =
                    new ArrayMap<>();
            resultMap.put(dailyIndex, dailyMap);
            if (hourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> timestamps = hourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            for (int hourlyIndex = 0; hourlyIndex < timestamps.size() - 1; hourlyIndex++) {
                final long startTimestamp = timestamps.get(hourlyIndex);
                final long endTimestamp = timestamps.get(hourlyIndex + 1);
                // Gets the app usage event list for this hourly slot first.
                final List<AppUsageEvent> hourlyAppUsageEventList =
                        getAppUsageEventListWithinTimeRangeWithBuffer(
                                appUsageEventList, startTimestamp, endTimestamp);

                // The value could be null when there is no data in the hourly slot.
                dailyMap.put(
                        hourlyIndex,
                        buildAppUsagePeriodList(
                                context,
                                hourlyAppUsageEventList,
                                batteryEventList,
                                startTimestamp,
                                endTimestamp));
            }
        }
        return resultMap;
    }

    /** Generates the list of {@link AppUsageEvent} from the supplied {@link UsageEvents}. */
    public static List<AppUsageEvent> generateAppUsageEventListFromUsageEvents(
            Context context, Map<Long, UsageEvents> usageEventsMap) {
        final List<AppUsageEvent> appUsageEventList = new ArrayList<>();
        long numEventsFetched = 0;
        long numAllEventsFetched = 0;
        final Set<String> ignoreScreenOnTimeTaskRootSet =
                FeatureFactory.getFeatureFactory()
                        .getPowerUsageFeatureProvider()
                        .getIgnoreScreenOnTimeTaskRootSet();
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
                            Log.w(
                                    TAG,
                                    String.format(
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
        Log.w(
                TAG,
                String.format(
                        "Read %d relevant events (%d total) from UsageStatsManager",
                        numEventsFetched, numAllEventsFetched));
        return appUsageEventList;
    }

    /** Generates the list of {@link BatteryEntry} from the supplied {@link BatteryUsageStats}. */
    @Nullable
    public static List<BatteryEntry> generateBatteryEntryListFromBatteryUsageStats(
            final Context context, @Nullable final BatteryUsageStats batteryUsageStats) {
        if (batteryUsageStats == null) {
            Log.w(TAG, "batteryUsageStats is null content");
            return null;
        }
        if (!shouldShowBatteryAttributionList(context)) {
            return null;
        }
        final BatteryUtils batteryUtils = BatteryUtils.getInstance(context);
        final int dischargePercentage = Math.max(0, batteryUsageStats.getDischargePercentage());
        final List<BatteryEntry> usageList =
                getCoalescedUsageList(
                        context,
                        batteryUtils,
                        batteryUsageStats,
                        /* loadDataInBackground= */ false);
        final double totalPower = batteryUsageStats.getConsumedPower();
        for (int i = 0; i < usageList.size(); i++) {
            final BatteryEntry entry = usageList.get(i);
            final double percentOfTotal =
                    batteryUtils.calculateBatteryPercent(
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
        return batteryHistEntryList == null
                ? new ArrayMap<>()
                : batteryHistEntryList.stream().collect(Collectors.toMap(e -> e.getKey(), e -> e));
    }

    /**
     * @return Returns the processed history map which has interpolated to every hour data.
     *     <p>The start timestamp is the first timestamp in batteryHistoryMap. The end timestamp is
     *     current time. The keys of processed history map should contain every hour between the
     *     start and end timestamp. If there's no data in some key, the value will be the empty map.
     */
    static Map<Long, Map<String, BatteryHistEntry>> getHistoryMapWithExpectedTimestamps(
            Context context, final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        final long startTime = System.currentTimeMillis();
        final List<Long> rawTimestampList = new ArrayList<>(batteryHistoryMap.keySet());
        final Map<Long, Map<String, BatteryHistEntry>> resultMap = new ArrayMap();
        if (rawTimestampList.isEmpty()) {
            Log.d(TAG, "empty batteryHistoryMap in getHistoryMapWithExpectedTimestamps()");
            return resultMap;
        }
        Collections.sort(rawTimestampList);
        final long currentTime = getCurrentTimeMillis();
        final List<Long> expectedTimestampList = getTimestampSlots(rawTimestampList, currentTime);
        interpolateHistory(
                context, rawTimestampList, expectedTimestampList, batteryHistoryMap, resultMap);
        Log.d(
                TAG,
                String.format(
                        "getHistoryMapWithExpectedTimestamps() size=%d in %d/ms",
                        resultMap.size(), (System.currentTimeMillis() - startTime)));
        return resultMap;
    }

    @Nullable
    static BatteryLevelData getLevelDataThroughProcessedHistoryMap(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap) {
        // There should be at least the start and end timestamps. Otherwise, return null to not show
        // data in usage chart.
        if (processedBatteryHistoryMap.size() < MIN_DAILY_DATA_SIZE) {
            return null;
        }
        Map<Long, Integer> batteryLevelMap = new ArrayMap<>();
        for (Long timestamp : processedBatteryHistoryMap.keySet()) {
            batteryLevelMap.put(
                    timestamp, getLevel(context, processedBatteryHistoryMap, timestamp));
        }
        return new BatteryLevelData(batteryLevelMap);
    }

    /**
     * Computes expected timestamp slots. The start timestamp is the first timestamp in
     * rawTimestampList. The end timestamp is current time. The middle timestamps are the sharp hour
     * timestamps between the start and end timestamps.
     */
    @VisibleForTesting
    static List<Long> getTimestampSlots(final List<Long> rawTimestampList, final long currentTime) {
        final List<Long> timestampSlots = new ArrayList<>();
        if (rawTimestampList.isEmpty()) {
            return timestampSlots;
        }
        final long startTimestamp = rawTimestampList.get(0);
        final long endTimestamp = currentTime;
        // If the start timestamp is later or equal the end one, return the empty list.
        if (startTimestamp >= endTimestamp) {
            return timestampSlots;
        }
        timestampSlots.add(startTimestamp);
        for (long timestamp = TimestampUtils.getNextHourTimestamp(startTimestamp);
                timestamp < endTimestamp;
                timestamp += DateUtils.HOUR_IN_MILLIS) {
            timestampSlots.add(timestamp);
        }
        timestampSlots.add(endTimestamp);
        return timestampSlots;
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
        final long[] results = new long[] {Long.MIN_VALUE, Long.MAX_VALUE};
        // Searches the nearest lower and upper timestamp value.
        timestamps.forEach(
                timestamp -> {
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

    static Map<Long, BatteryDiffData> getBatteryDiffDataMap(
            Context context,
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
                    appUsagePeriodMap,
            final @NonNull Set<String> systemAppsPackageNames,
            final @NonNull Set<Integer> systemAppsUids) {
        final Map<Long, BatteryDiffData> batteryDiffDataMap = new ArrayMap<>();
        final int currentUserId = context.getUserId();
        final UserHandle userHandle =
                Utils.getManagedProfile(context.getSystemService(UserManager.class));
        final int workProfileUserId =
                userHandle != null ? userHandle.getIdentifier() : Integer.MIN_VALUE;
        // Each time slot usage diff data =
        //     sum(Math.abs(timestamp[i+1] data - timestamp[i] data));
        // since we want to aggregate every hour usage diff data into a single time slot.
        for (int dailyIndex = 0; dailyIndex < hourlyBatteryLevelsPerDay.size(); dailyIndex++) {
            if (hourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> hourlyTimestamps =
                    hourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            for (int hourlyIndex = 0; hourlyIndex < hourlyTimestamps.size() - 1; hourlyIndex++) {
                final Long startTimestamp = hourlyTimestamps.get(hourlyIndex);
                final Long endTimestamp = hourlyTimestamps.get(hourlyIndex + 1);
                final int startBatteryLevel =
                        hourlyBatteryLevelsPerDay.get(dailyIndex).getLevels().get(hourlyIndex);
                final int endBatteryLevel =
                        hourlyBatteryLevelsPerDay.get(dailyIndex).getLevels().get(hourlyIndex + 1);
                final long slotDuration = endTimestamp - startTimestamp;
                List<Map<String, BatteryHistEntry>> slotBatteryHistoryList = new ArrayList<>();
                slotBatteryHistoryList.add(
                        batteryHistoryMap.getOrDefault(startTimestamp, EMPTY_BATTERY_MAP));
                for (Long timestamp = TimestampUtils.getNextHourTimestamp(startTimestamp);
                        timestamp < endTimestamp;
                        timestamp += DateUtils.HOUR_IN_MILLIS) {
                    slotBatteryHistoryList.add(
                            batteryHistoryMap.getOrDefault(timestamp, EMPTY_BATTERY_MAP));
                }
                slotBatteryHistoryList.add(
                        batteryHistoryMap.getOrDefault(endTimestamp, EMPTY_BATTERY_MAP));

                final BatteryDiffData hourlyBatteryDiffData =
                        insertHourlyUsageDiffDataPerSlot(
                                context,
                                startTimestamp,
                                endTimestamp,
                                startBatteryLevel,
                                endBatteryLevel,
                                currentUserId,
                                workProfileUserId,
                                slotDuration,
                                systemAppsPackageNames,
                                systemAppsUids,
                                appUsagePeriodMap == null
                                                || appUsagePeriodMap.get(dailyIndex) == null
                                        ? null
                                        : appUsagePeriodMap.get(dailyIndex).get(hourlyIndex),
                                slotBatteryHistoryList);
                batteryDiffDataMap.put(startTimestamp, hourlyBatteryDiffData);
            }
        }
        return batteryDiffDataMap;
    }

    /**
     * @return Returns the indexed battery usage data for each corresponding time slot.
     *     <p>There could be 2 cases of the returned value:
     *     <ul>
     *       <li>null: empty or invalid data.
     *       <li>1 part: if batteryLevelData is null.
     *           <p>[SELECTED_INDEX_ALL][SELECTED_INDEX_ALL]
     *       <li>3 parts: if batteryLevelData is not null.
     *           <p>1 - [SELECTED_INDEX_ALL][SELECTED_INDEX_ALL]
     *           <p>2 - [0][SELECTED_INDEX_ALL] ~ [maxDailyIndex][SELECTED_INDEX_ALL]
     *           <p>3 - [0][0] ~ [maxDailyIndex][maxHourlyIndex]
     *     </ul>
     */
    static Map<Integer, Map<Integer, BatteryDiffData>> generateBatteryUsageMap(
            final Context context,
            final Map<Long, BatteryDiffData> batteryDiffDataMap,
            final @Nullable BatteryLevelData batteryLevelData) {
        final Map<Integer, Map<Integer, BatteryDiffData>> resultMap = new ArrayMap<>();
        if (batteryLevelData == null) {
            Preconditions.checkArgument(batteryDiffDataMap.size() == 1);
            BatteryDiffData batteryDiffData = batteryDiffDataMap.values().stream().toList().get(0);
            final Map<Integer, BatteryDiffData> allUsageMap = new ArrayMap<>();
            allUsageMap.put(SELECTED_INDEX_ALL, batteryDiffData);
            resultMap.put(SELECTED_INDEX_ALL, allUsageMap);
            return resultMap;
        }
        List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay =
                batteryLevelData.getHourlyBatteryLevelsPerDay();
        // Insert diff data from [0][0] to [maxDailyIndex][maxHourlyIndex].
        insertHourlyUsageDiffData(hourlyBatteryLevelsPerDay, batteryDiffDataMap, resultMap);
        // Insert diff data from [0][SELECTED_INDEX_ALL] to [maxDailyIndex][SELECTED_INDEX_ALL].
        insertDailyUsageDiffData(context, hourlyBatteryLevelsPerDay, resultMap);
        // Insert diff data [SELECTED_INDEX_ALL][SELECTED_INDEX_ALL].
        insertAllUsageDiffData(context, resultMap);
        if (!isUsageMapValid(resultMap, hourlyBatteryLevelsPerDay)) {
            return null;
        }
        return resultMap;
    }

    @VisibleForTesting
    @Nullable
    static BatteryDiffData generateBatteryDiffData(
            final Context context,
            final long startTimestamp,
            final List<BatteryHistEntry> batteryHistEntryList,
            final @NonNull Set<String> systemAppsPackageNames,
            final @NonNull Set<Integer> systemAppsUids) {
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        final List<BatteryDiffEntry> systemEntries = new ArrayList<>();
        if (batteryHistEntryList == null || batteryHistEntryList.isEmpty()) {
            Log.w(TAG, "batteryHistEntryList is null or empty in generateBatteryDiffData()");
            return new BatteryDiffData(
                    context,
                    startTimestamp,
                    getCurrentTimeMillis(),
                    /* startBatteryLevel= */ 100,
                    getCurrentLevel(context),
                    /* screenOnTime= */ 0L,
                    appEntries,
                    systemEntries,
                    systemAppsPackageNames,
                    systemAppsUids,
                    /* isAccumulated= */ false);
        }
        final int currentUserId = context.getUserId();
        final UserHandle userHandle =
                Utils.getManagedProfile(context.getSystemService(UserManager.class));
        final int workProfileUserId =
                userHandle != null ? userHandle.getIdentifier() : Integer.MIN_VALUE;

        for (BatteryHistEntry entry : batteryHistEntryList) {
            final boolean isFromOtherUsers =
                    isConsumedFromOtherUsers(currentUserId, workProfileUserId, entry);
            // Not show other users' battery usage data.
            if (isFromOtherUsers) {
                continue;
            } else {
                final BatteryDiffEntry currentBatteryDiffEntry =
                        new BatteryDiffEntry(
                                context,
                                entry.mUid,
                                entry.mUserId,
                                entry.getKey(),
                                entry.mIsHidden,
                                entry.mDrainType,
                                entry.mPackageName,
                                entry.mAppLabel,
                                entry.mConsumerType,
                                entry.mForegroundUsageTimeInMs,
                                entry.mForegroundServiceUsageTimeInMs,
                                entry.mBackgroundUsageTimeInMs,
                                /* screenOnTimeInMs= */ 0,
                                entry.mConsumePower,
                                entry.mForegroundUsageConsumePower,
                                entry.mForegroundServiceUsageConsumePower,
                                entry.mBackgroundUsageConsumePower,
                                entry.mCachedUsageConsumePower);
                if (currentBatteryDiffEntry.isSystemEntry()) {
                    systemEntries.add(currentBatteryDiffEntry);
                } else {
                    appEntries.add(currentBatteryDiffEntry);
                }
            }
        }
        return new BatteryDiffData(
                context,
                startTimestamp,
                getCurrentTimeMillis(),
                /* startBatteryLevel= */ 100,
                getCurrentLevel(context),
                /* screenOnTime= */ 0L,
                appEntries,
                systemEntries,
                systemAppsPackageNames,
                systemAppsUids,
                /* isAccumulated= */ false);
    }

    /**
     * {@code Long} stands for the userId.
     *
     * <p>{@code String} stands for the packageName.
     */
    @VisibleForTesting
    @Nullable
    static Map<Long, Map<String, List<AppUsagePeriod>>> buildAppUsagePeriodList(
            Context context,
            final List<AppUsageEvent> appUsageEvents,
            final List<BatteryEvent> batteryEventList,
            final long startTime,
            final long endTime) {
        if (appUsageEvents.isEmpty()) {
            return null;
        }

        // Attributes the list of AppUsagePeriod into device events and instance events for further
        // use.
        final List<AppUsageEvent> deviceEvents = new ArrayList<>();
        final ArrayMap<Integer, List<AppUsageEvent>> usageEventsByInstanceId = new ArrayMap<>();
        for (final AppUsageEvent event : appUsageEvents) {
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

        final Map<Long, Map<String, List<AppUsagePeriod>>> allUsagePeriods = new ArrayMap<>();

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
            final String packageName =
                    getEffectivePackageName(
                            context,
                            sUsageStatsManager,
                            firstEvent.getPackageName(),
                            firstEvent.getTaskRootPackageName());
            usageEvents.addAll(deviceEvents);
            // Sorts the usageEvents in ascending order based on the timestamp before computing the
            // period.
            Collections.sort(usageEvents, APP_USAGE_EVENT_TIMESTAMP_COMPARATOR);

            // A package might have multiple instances. Computes the usage period per instance id
            // and then merges them into the same user-package map.
            final List<AppUsagePeriod> usagePeriodList =
                    excludePowerConnectedTimeFromAppUsagePeriodList(
                            buildAppUsagePeriodListPerInstance(usageEvents, startTime, endTime),
                            batteryEventList);
            if (!usagePeriodList.isEmpty()) {
                addToUsagePeriodMap(allUsagePeriods, usagePeriodList, eventUserId, packageName);
            }
        }

        // Sorts all usage periods by start time.
        for (final long userId : allUsagePeriods.keySet()) {
            if (allUsagePeriods.get(userId) == null) {
                continue;
            }
            for (final String packageName : allUsagePeriods.get(userId).keySet()) {
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

        for (final AppUsageEvent event : usageEvents) {
            final long eventTime = event.getTimestamp();

            if (event.getType() == AppUsageEventType.ACTIVITY_RESUMED) {
                // If there is an existing start time, simply ignore this start event.
                // If there was no start time, then start a new period.
                if (!pendingUsagePeriod.hasStartTime()) {
                    pendingUsagePeriod.setStartTime(eventTime);
                }
            } else if (event.getType() == AppUsageEventType.ACTIVITY_STOPPED) {
                pendingUsagePeriod.setEndTime(eventTime);
                if (!pendingUsagePeriod.hasStartTime()) {
                    pendingUsagePeriod.setStartTime(
                            getStartTimeForIncompleteUsagePeriod(pendingUsagePeriod));
                }
                // If we already have start time, add it directly.
                validateAndAddToPeriodList(
                        usagePeriodList, pendingUsagePeriod.build(), startTime, endTime);
                pendingUsagePeriod.clear();
            } else if (event.getType() == AppUsageEventType.DEVICE_SHUTDOWN) {
                // The end event might be lost when device is shutdown. Use the estimated end
                // time for the period.
                if (pendingUsagePeriod.hasStartTime()) {
                    pendingUsagePeriod.setEndTime(
                            getEndTimeForIncompleteUsagePeriod(pendingUsagePeriod, eventTime));
                    validateAndAddToPeriodList(
                            usagePeriodList, pendingUsagePeriod.build(), startTime, endTime);
                    pendingUsagePeriod.clear();
                }
            }
        }
        // If there exists unclosed period, the stop event might happen in the next time
        // slot. Use the endTime for the period.
        if (pendingUsagePeriod.hasStartTime() && pendingUsagePeriod.getStartTime() < endTime) {
            pendingUsagePeriod.setEndTime(endTime);
            validateAndAddToPeriodList(
                    usagePeriodList, pendingUsagePeriod.build(), startTime, endTime);
            pendingUsagePeriod.clear();
        }
        return usagePeriodList;
    }

    @VisibleForTesting
    static List<AppUsagePeriod> excludePowerConnectedTimeFromAppUsagePeriodList(
            final List<AppUsagePeriod> usagePeriodList, final List<BatteryEvent> batteryEventList) {
        final List<AppUsagePeriod> resultList = new ArrayList<>();
        int index = 0;
        for (AppUsagePeriod inputPeriod : usagePeriodList) {
            long lastStartTime = inputPeriod.getStartTime();
            while (index < batteryEventList.size()) {
                BatteryEvent batteryEvent = batteryEventList.get(index);
                if (batteryEvent.getTimestamp() < inputPeriod.getStartTime()) {
                    // Because the batteryEventList has been sorted, here is to mark the power
                    // connection state when the usage period starts. If power is connected when
                    // the usage period starts, the starting period will be ignored; otherwise it
                    // will be added.
                    if (batteryEvent.getType() == BatteryEventType.POWER_CONNECTED) {
                        lastStartTime = 0;
                    } else if (batteryEvent.getType() == BatteryEventType.POWER_DISCONNECTED) {
                        lastStartTime = inputPeriod.getStartTime();
                    }
                    index++;
                    continue;
                }
                if (batteryEvent.getTimestamp() > inputPeriod.getEndTime()) {
                    // Because the batteryEventList has been sorted, if any event is already after
                    // the end time, all the following events should be able to drop directly.
                    break;
                }

                if (batteryEvent.getType() == BatteryEventType.POWER_CONNECTED
                        && lastStartTime != 0) {
                    resultList.add(
                            AppUsagePeriod.newBuilder()
                                    .setStartTime(lastStartTime)
                                    .setEndTime(batteryEvent.getTimestamp())
                                    .build());
                    lastStartTime = 0;
                } else if (batteryEvent.getType() == BatteryEventType.POWER_DISCONNECTED) {
                    lastStartTime = batteryEvent.getTimestamp();
                }
                index++;
            }
            if (lastStartTime != 0) {
                resultList.add(
                        AppUsagePeriod.newBuilder()
                                .setStartTime(lastStartTime)
                                .setEndTime(inputPeriod.getEndTime())
                                .build());
            }
        }
        return resultList;
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

    static Map<Long, BatteryDiffData> getBatteryDiffDataMapFromStatsService(
            final Context context,
            final long startTimestamp,
            @NonNull final Set<String> systemAppsPackageNames,
            @NonNull final Set<Integer> systemAppsUids) {
        Map<Long, BatteryDiffData> batteryDiffDataMap = new ArrayMap<>(1);
        batteryDiffDataMap.put(
                startTimestamp,
                generateBatteryDiffData(
                        context,
                        startTimestamp,
                        getBatteryHistListFromFromStatsService(context),
                        systemAppsPackageNames,
                        systemAppsUids));
        return batteryDiffDataMap;
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
            batteryUsageMapForAll.getAppDiffEntryList().forEach(entry -> entry.loadLabelAndIcon());
            batteryUsageMapForAll
                    .getSystemDiffEntryList()
                    .forEach(entry -> entry.loadLabelAndIcon());
        }
    }

    static Set<String> getSystemAppsPackageNames(Context context) {
        return sTestSystemAppsPackageNames != null
                ? sTestSystemAppsPackageNames
                : AppListRepositoryUtil.getSystemPackageNames(context, context.getUserId());
    }

    static Set<Integer> getSystemAppsUids(Context context) {
        Set<Integer> result = new ArraySet<>(1);
        try {
            result.add(
                    context.getPackageManager()
                            .getUidForSharedUser(ANDROID_CORE_APPS_SHARED_USER_ID));
        } catch (PackageManager.NameNotFoundException e) {
            // No Android Core Apps
        }
        return result;
    }

    /**
     * Generates the list of {@link AppUsageEvent} within the specific time range. The buffer is
     * added to make sure the app usage calculation near the boundaries is correct.
     *
     * <p>Note: The appUsageEventList should have been sorted when calling this function.
     */
    private static List<AppUsageEvent> getAppUsageEventListWithinTimeRangeWithBuffer(
            final List<AppUsageEvent> appUsageEventList, final long startTime, final long endTime) {
        final long start = startTime - DatabaseUtils.USAGE_QUERY_BUFFER_HOURS;
        final long end = endTime + DatabaseUtils.USAGE_QUERY_BUFFER_HOURS;
        final List<AppUsageEvent> resultList = new ArrayList<>();
        for (final AppUsageEvent event : appUsageEventList) {
            final long eventTime = event.getTimestamp();
            // Because the appUsageEventList has been sorted, if any event is already after the end
            // time, all the following events should be able to drop directly.
            if (eventTime > end) {
                break;
            }
            // If the event timestamp is in [start, end], add it into the result list.
            if (eventTime >= start) {
                resultList.add(event);
            }
        }
        return resultList;
    }

    private static void validateAndAddToPeriodList(
            final List<AppUsagePeriod> appUsagePeriodList,
            final AppUsagePeriod appUsagePeriod,
            final long startTime,
            final long endTime) {
        final long periodStartTime =
                trimPeriodTime(appUsagePeriod.getStartTime(), startTime, endTime);
        final long periodEndTime = trimPeriodTime(appUsagePeriod.getEndTime(), startTime, endTime);
        // Only when the period is valid, add it into the list.
        if (periodStartTime < periodEndTime) {
            final AppUsagePeriod period =
                    AppUsagePeriod.newBuilder()
                            .setStartTime(periodStartTime)
                            .setEndTime(periodEndTime)
                            .build();
            appUsagePeriodList.add(period);
        }
    }

    private static long trimPeriodTime(
            final long originalTime, final long startTime, final long endTime) {
        long finalTime = Math.max(originalTime, startTime);
        finalTime = Math.min(finalTime, endTime);
        return finalTime;
    }

    private static void addToUsagePeriodMap(
            final Map<Long, Map<String, List<AppUsagePeriod>>> usagePeriodMap,
            final List<AppUsagePeriod> usagePeriodList,
            final long userId,
            final String packageName) {
        usagePeriodMap.computeIfAbsent(userId, key -> new ArrayMap<>());
        final Map<String, List<AppUsagePeriod>> packageNameMap = usagePeriodMap.get(userId);
        packageNameMap.computeIfAbsent(packageName, key -> new ArrayList<>());
        packageNameMap.get(packageName).addAll(usagePeriodList);
    }

    /** Returns the start time that gives {@code usagePeriod} the default usage duration. */
    private static long getStartTimeForIncompleteUsagePeriod(
            final AppUsagePeriodOrBuilder usagePeriod) {
        return usagePeriod.getEndTime() - DEFAULT_USAGE_DURATION_FOR_INCOMPLETE_INTERVAL;
    }

    /** Returns the end time that gives {@code usagePeriod} the default usage duration. */
    private static long getEndTimeForIncompleteUsagePeriod(
            final AppUsagePeriodOrBuilder usagePeriod, final long eventTime) {
        return Math.min(
                usagePeriod.getStartTime() + DEFAULT_USAGE_DURATION_FOR_INCOMPLETE_INTERVAL,
                eventTime);
    }

    @Nullable
    private static UsageEvents getAppUsageEventsForUser(
            Context context,
            final UserManager userManager,
            final int userID,
            final long earliestTimestamp) {
        final String callingPackage = context.getPackageName();
        final long now = System.currentTimeMillis();
        // When the user is not unlocked, UsageStatsManager will return null, so bypass the
        // following data loading logics directly.
        if (!userManager.isUserUnlocked(userID)) {
            Log.w(TAG, "fail to load app usage event for user :" + userID + " because locked");
            return null;
        }
        final long startTime =
                DatabaseUtils.getAppUsageStartTimestampOfUser(context, userID, earliestTimestamp);
        return loadAppUsageEventsForUserFromService(
                sUsageStatsManager, startTime, now, userID, callingPackage);
    }

    @Nullable
    private static UsageEvents loadAppUsageEventsForUserFromService(
            final IUsageStatsManager usageStatsManager,
            final long startTime,
            final long endTime,
            final int userId,
            final String callingPackage) {
        final long start = System.currentTimeMillis();
        UsageEvents events = null;
        try {
            events =
                    usageStatsManager.queryEventsForUser(
                            startTime, endTime, userId, callingPackage);
        } catch (RemoteException e) {
            Log.e(TAG, "Error fetching usage events: ", e);
        }
        final long elapsedTime = System.currentTimeMillis() - start;
        Log.d(
                TAG,
                String.format(
                        "getAppUsageEventsForUser(): %d from %d to %d in %d/ms",
                        userId, startTime, endTime, elapsedTime));
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
            Log.e(TAG, "load batteryUsageStats:", e);
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
                .filter(
                        entry -> {
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
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap) {
        if (rawTimestampList.isEmpty() || expectedTimestampSlots.isEmpty()) {
            return;
        }
        final int expectedTimestampSlotsSize = expectedTimestampSlots.size();
        final long startTimestamp = expectedTimestampSlots.get(0);
        final long endTimestamp = expectedTimestampSlots.get(expectedTimestampSlotsSize - 1);

        resultMap.put(startTimestamp, batteryHistoryMap.get(startTimestamp));
        for (int index = 1; index < expectedTimestampSlotsSize - 1; index++) {
            interpolateHistoryForSlot(
                    context,
                    expectedTimestampSlots.get(index),
                    rawTimestampList,
                    batteryHistoryMap,
                    resultMap);
        }
        resultMap.put(
                endTimestamp,
                Map.of(CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER, EMPTY_BATTERY_HIST_ENTRY));
    }

    private static void interpolateHistoryForSlot(
            Context context,
            final long currentSlot,
            final List<Long> rawTimestampList,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap) {
        final long[] nearestTimestamps = findNearestTimestamp(rawTimestampList, currentSlot);
        final long lowerTimestamp = nearestTimestamps[0];
        final long upperTimestamp = nearestTimestamps[1];
        // Case 1: upper timestamp is zero since scheduler is delayed!
        if (upperTimestamp == 0) {
            log(context, "job scheduler is delayed", currentSlot, null);
            resultMap.put(currentSlot, new ArrayMap<>());
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
            resultMap.put(currentSlot, new ArrayMap<>());
            return;
        }
        interpolateHistoryForSlot(
                context, currentSlot, lowerTimestamp, upperTimestamp, batteryHistoryMap, resultMap);
    }

    private static void interpolateHistoryForSlot(
            Context context,
            final long currentSlot,
            final long lowerTimestamp,
            final long upperTimestamp,
            final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final Map<Long, Map<String, BatteryHistEntry>> resultMap) {
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
                && !TimestampUtils.isMidnight(currentSlot)) {
            // Provides an opportunity to force align the slot directly.
            if ((upperTimestamp - currentSlot) < 10 * DateUtils.MINUTE_IN_MILLIS) {
                log(context, "force align into the nearest slot", currentSlot, null);
                resultMap.put(currentSlot, upperEntryDataMap);
            } else {
                log(context, "in the different booting section", currentSlot, null);
                resultMap.put(currentSlot, new ArrayMap<>());
            }
            return;
        }
        log(context, "apply interpolation arithmetic", currentSlot, null);
        final Map<String, BatteryHistEntry> newHistEntryMap = new ArrayMap<>();
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
                            /* ratio= */ timestampDiff / timestampLength,
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

    private static Integer getLevel(
            Context context,
            final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap,
            final long timestamp) {
        final Map<String, BatteryHistEntry> entryMap = processedBatteryHistoryMap.get(timestamp);
        if (entryMap == null || entryMap.isEmpty()) {
            Log.e(
                    TAG,
                    "abnormal entry list in the timestamp:"
                            + ConvertUtils.utcToLocalTimeForLogging(timestamp));
            return BATTERY_LEVEL_UNKNOWN;
        }
        // The current time battery history hasn't been loaded yet, returns the current battery
        // level.
        if (entryMap.containsKey(CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER)) {
            return getCurrentLevel(context);
        }
        // Averages the battery level in each time slot to avoid corner conditions.
        float batteryLevelCounter = 0;
        for (BatteryHistEntry entry : entryMap.values()) {
            batteryLevelCounter += entry.mBatteryLevel;
        }
        return Math.round(batteryLevelCounter / entryMap.size());
    }

    private static int getCurrentLevel(Context context) {
        final Intent intent = BatteryUtils.getBatteryIntent(context);
        return BatteryStatus.getBatteryLevel(intent);
    }

    private static void insertHourlyUsageDiffData(
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            final Map<Long, BatteryDiffData> batteryDiffDataMap,
            final Map<Integer, Map<Integer, BatteryDiffData>> resultMap) {
        // Each time slot usage diff data =
        //     sum(Math.abs(timestamp[i+1] data - timestamp[i] data));
        // since we want to aggregate every hour usage diff data into a single time slot.
        for (int dailyIndex = 0; dailyIndex < hourlyBatteryLevelsPerDay.size(); dailyIndex++) {
            final Map<Integer, BatteryDiffData> dailyDiffMap = new ArrayMap<>();
            resultMap.put(dailyIndex, dailyDiffMap);
            if (hourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> hourlyTimestamps =
                    hourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            for (int hourlyIndex = 0; hourlyIndex < hourlyTimestamps.size() - 1; hourlyIndex++) {
                final Long startTimestamp = hourlyTimestamps.get(hourlyIndex);
                dailyDiffMap.put(hourlyIndex, batteryDiffDataMap.get(startTimestamp));
            }
        }
    }

    private static void insertDailyUsageDiffData(
            final Context context,
            final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            final Map<Integer, Map<Integer, BatteryDiffData>> resultMap) {
        for (int index = 0; index < hourlyBatteryLevelsPerDay.size(); index++) {
            Map<Integer, BatteryDiffData> dailyUsageMap = resultMap.get(index);
            if (dailyUsageMap == null) {
                dailyUsageMap = new ArrayMap<>();
                resultMap.put(index, dailyUsageMap);
            }
            dailyUsageMap.put(
                    SELECTED_INDEX_ALL,
                    getAccumulatedUsageDiffData(context, dailyUsageMap.values()));
        }
    }

    private static void insertAllUsageDiffData(
            final Context context, final Map<Integer, Map<Integer, BatteryDiffData>> resultMap) {
        final List<BatteryDiffData> diffDataList = new ArrayList<>();
        resultMap
                .keySet()
                .forEach(key -> diffDataList.add(resultMap.get(key).get(SELECTED_INDEX_ALL)));
        final Map<Integer, BatteryDiffData> allUsageMap = new ArrayMap<>();
        allUsageMap.put(SELECTED_INDEX_ALL, getAccumulatedUsageDiffData(context, diffDataList));
        resultMap.put(SELECTED_INDEX_ALL, allUsageMap);
    }

    @Nullable
    private static BatteryDiffData insertHourlyUsageDiffDataPerSlot(
            final Context context,
            final long startTimestamp,
            final long endTimestamp,
            final int startBatteryLevel,
            final int endBatteryLevel,
            final int currentUserId,
            final int workProfileUserId,
            final long slotDuration,
            final Set<String> systemAppsPackageNames,
            final Set<Integer> systemAppsUids,
            final Map<Long, Map<String, List<AppUsagePeriod>>> appUsageMap,
            final List<Map<String, BatteryHistEntry>> slotBatteryHistoryList) {
        long slotScreenOnTime = 0L;
        if (appUsageMap != null) {
            final List<AppUsagePeriod> flatAppUsagePeriodList = new ArrayList<>();
            for (final long userId : appUsageMap.keySet()) {
                if ((userId != currentUserId && userId != workProfileUserId)
                        || appUsageMap.get(userId) == null) {
                    continue;
                }
                for (final String packageName : appUsageMap.get(userId).keySet()) {
                    final List<AppUsagePeriod> appUsagePeriodList =
                            appUsageMap.get(userId).get(packageName);
                    if (appUsagePeriodList != null) {
                        flatAppUsagePeriodList.addAll(appUsagePeriodList);
                    }
                }
            }
            slotScreenOnTime = Math.min(slotDuration, getScreenOnTime(flatAppUsagePeriodList));
        }

        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        final List<BatteryDiffEntry> systemEntries = new ArrayList<>();

        // Collects all keys in these three time slot records as all populations.
        final Set<String> allBatteryHistEntryKeys = new ArraySet<>();
        for (Map<String, BatteryHistEntry> slotBatteryHistMap : slotBatteryHistoryList) {
            if (slotBatteryHistMap.isEmpty()) {
                // We should not get the empty list since we have at least one fake data to record
                // the battery level and status in each time slot, the empty list is used to
                // represent there is no enough data to apply interpolation arithmetic.
                return new BatteryDiffData(
                        context,
                        startTimestamp,
                        endTimestamp,
                        startBatteryLevel,
                        endBatteryLevel,
                        /* screenOnTime= */ 0L,
                        appEntries,
                        systemEntries,
                        systemAppsPackageNames,
                        systemAppsUids,
                        /* isAccumulated= */ false);
            }
            allBatteryHistEntryKeys.addAll(slotBatteryHistMap.keySet());
        }

        // Calculates all packages diff usage data in a specific time slot.
        for (String key : allBatteryHistEntryKeys) {
            if (key == null) {
                continue;
            }

            BatteryHistEntry selectedBatteryEntry = null;
            final List<BatteryHistEntry> batteryHistEntries = new ArrayList<>();
            for (Map<String, BatteryHistEntry> slotBatteryHistMap : slotBatteryHistoryList) {
                BatteryHistEntry entry =
                        slotBatteryHistMap.getOrDefault(key, EMPTY_BATTERY_HIST_ENTRY);
                batteryHistEntries.add(entry);
                if (selectedBatteryEntry == null && entry != EMPTY_BATTERY_HIST_ENTRY) {
                    selectedBatteryEntry = entry;
                }
            }
            if (selectedBatteryEntry == null) {
                continue;
            }

            // Not show other users' battery usage data.
            final boolean isFromOtherUsers =
                    isConsumedFromOtherUsers(
                            currentUserId, workProfileUserId, selectedBatteryEntry);
            if (isFromOtherUsers) {
                continue;
            }

            // Cumulative values is a specific time slot for a specific app.
            long foregroundUsageTimeInMs = 0;
            long foregroundServiceUsageTimeInMs = 0;
            long backgroundUsageTimeInMs = 0;
            double consumePower = 0;
            double foregroundUsageConsumePower = 0;
            double foregroundServiceUsageConsumePower = 0;
            double backgroundUsageConsumePower = 0;
            double cachedUsageConsumePower = 0;
            for (int i = 0; i < batteryHistEntries.size() - 1; i++) {
                final BatteryHistEntry currentEntry = batteryHistEntries.get(i);
                final BatteryHistEntry nextEntry = batteryHistEntries.get(i + 1);
                foregroundUsageTimeInMs +=
                        getDiffValue(
                                currentEntry.mForegroundUsageTimeInMs,
                                nextEntry.mForegroundUsageTimeInMs);
                foregroundServiceUsageTimeInMs +=
                        getDiffValue(
                                currentEntry.mForegroundServiceUsageTimeInMs,
                                nextEntry.mForegroundServiceUsageTimeInMs);
                backgroundUsageTimeInMs +=
                        getDiffValue(
                                currentEntry.mBackgroundUsageTimeInMs,
                                nextEntry.mBackgroundUsageTimeInMs);
                consumePower += getDiffValue(currentEntry.mConsumePower, nextEntry.mConsumePower);
                foregroundUsageConsumePower +=
                        getDiffValue(
                                currentEntry.mForegroundUsageConsumePower,
                                nextEntry.mForegroundUsageConsumePower);
                foregroundServiceUsageConsumePower +=
                        getDiffValue(
                                currentEntry.mForegroundServiceUsageConsumePower,
                                nextEntry.mForegroundServiceUsageConsumePower);
                backgroundUsageConsumePower +=
                        getDiffValue(
                                currentEntry.mBackgroundUsageConsumePower,
                                nextEntry.mBackgroundUsageConsumePower);
                cachedUsageConsumePower +=
                        getDiffValue(
                                currentEntry.mCachedUsageConsumePower,
                                nextEntry.mCachedUsageConsumePower);
            }
            if (isSystemConsumer(selectedBatteryEntry.mConsumerType)
                    && selectedBatteryEntry.mDrainType == BatteryConsumer.POWER_COMPONENT_SCREEN) {
                // Replace Screen system component time with screen on time.
                foregroundUsageTimeInMs = slotScreenOnTime;
            }
            // Excludes entry since we don't have enough data to calculate.
            if (foregroundUsageTimeInMs == 0
                    && foregroundServiceUsageTimeInMs == 0
                    && backgroundUsageTimeInMs == 0
                    && consumePower == 0) {
                continue;
            }
            // Forces refine the cumulative value since it may introduce deviation error since we
            // will apply the interpolation arithmetic.
            final float totalUsageTimeInMs =
                    foregroundUsageTimeInMs
                            + backgroundUsageTimeInMs
                            + foregroundServiceUsageTimeInMs;
            if (totalUsageTimeInMs > slotDuration) {
                final float ratio = slotDuration / totalUsageTimeInMs;
                if (sDebug) {
                    Log.w(
                            TAG,
                            String.format(
                                    "abnormal usage time %d|%d|%d for:\n%s",
                                    Duration.ofMillis(foregroundUsageTimeInMs).getSeconds(),
                                    Duration.ofMillis(foregroundServiceUsageTimeInMs).getSeconds(),
                                    Duration.ofMillis(backgroundUsageTimeInMs).getSeconds(),
                                    selectedBatteryEntry));
                }
                foregroundUsageTimeInMs = Math.round(foregroundUsageTimeInMs * ratio);
                foregroundServiceUsageTimeInMs = Math.round(foregroundServiceUsageTimeInMs * ratio);
                backgroundUsageTimeInMs = Math.round(backgroundUsageTimeInMs * ratio);
                consumePower = consumePower * ratio;
                foregroundUsageConsumePower = foregroundUsageConsumePower * ratio;
                foregroundServiceUsageConsumePower = foregroundServiceUsageConsumePower * ratio;
                backgroundUsageConsumePower = backgroundUsageConsumePower * ratio;
                cachedUsageConsumePower = cachedUsageConsumePower * ratio;
            }

            // Compute the screen on time and make sure it won't exceed the threshold.
            final long screenOnTime =
                    Math.min(
                            (long) slotDuration,
                            getScreenOnTime(
                                    appUsageMap,
                                    selectedBatteryEntry.mUserId,
                                    selectedBatteryEntry.mPackageName));
            // Ensure the following value will not exceed the threshold.
            // value = background + foregroundService + screen-on
            backgroundUsageTimeInMs =
                    Math.min(backgroundUsageTimeInMs, (long) slotDuration - screenOnTime);
            foregroundServiceUsageTimeInMs =
                    Math.min(
                            foregroundServiceUsageTimeInMs,
                            (long) slotDuration - screenOnTime - backgroundUsageTimeInMs);
            final BatteryDiffEntry currentBatteryDiffEntry =
                    new BatteryDiffEntry(
                            context,
                            selectedBatteryEntry.mUid,
                            selectedBatteryEntry.mUserId,
                            selectedBatteryEntry.getKey(),
                            selectedBatteryEntry.mIsHidden,
                            selectedBatteryEntry.mDrainType,
                            selectedBatteryEntry.mPackageName,
                            selectedBatteryEntry.mAppLabel,
                            selectedBatteryEntry.mConsumerType,
                            foregroundUsageTimeInMs,
                            foregroundServiceUsageTimeInMs,
                            backgroundUsageTimeInMs,
                            screenOnTime,
                            consumePower,
                            foregroundUsageConsumePower,
                            foregroundServiceUsageConsumePower,
                            backgroundUsageConsumePower,
                            cachedUsageConsumePower);
            if (currentBatteryDiffEntry.isSystemEntry()) {
                systemEntries.add(currentBatteryDiffEntry);
            } else {
                appEntries.add(currentBatteryDiffEntry);
            }
        }
        return new BatteryDiffData(
                context,
                startTimestamp,
                endTimestamp,
                startBatteryLevel,
                endBatteryLevel,
                slotScreenOnTime,
                appEntries,
                systemEntries,
                systemAppsPackageNames,
                systemAppsUids,
                /* isAccumulated= */ false);
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
        return isUidConsumer(batteryHistEntry.mConsumerType)
                && batteryHistEntry.mUserId != currentUserId
                && batteryHistEntry.mUserId != workProfileUserId;
    }

    @Nullable
    private static BatteryDiffData getAccumulatedUsageDiffData(
            final Context context, final Collection<BatteryDiffData> batteryDiffDataList) {
        final Map<String, BatteryDiffEntry> diffEntryMap = new ArrayMap<>();
        final List<BatteryDiffEntry> appEntries = new ArrayList<>();
        final List<BatteryDiffEntry> systemEntries = new ArrayList<>();

        long startTimestamp = Long.MAX_VALUE;
        long endTimestamp = 0;
        int startBatteryLevel = BATTERY_LEVEL_UNKNOWN;
        int endBatteryLevel = BATTERY_LEVEL_UNKNOWN;
        long totalScreenOnTime = 0;
        for (BatteryDiffData batteryDiffData : batteryDiffDataList) {
            if (batteryDiffData == null) {
                continue;
            }
            if (startTimestamp > batteryDiffData.getStartTimestamp()) {
                startTimestamp = batteryDiffData.getStartTimestamp();
                startBatteryLevel = batteryDiffData.getStartBatteryLevel();
            }
            if (endTimestamp > batteryDiffData.getEndTimestamp()) {
                endTimestamp = batteryDiffData.getEndTimestamp();
                endBatteryLevel = batteryDiffData.getEndBatteryLevel();
            }
            totalScreenOnTime += batteryDiffData.getScreenOnTime();
            for (BatteryDiffEntry entry : batteryDiffData.getAppDiffEntryList()) {
                computeUsageDiffDataPerEntry(entry, diffEntryMap);
            }
            for (BatteryDiffEntry entry : batteryDiffData.getSystemDiffEntryList()) {
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

        return new BatteryDiffData(
                context,
                startTimestamp,
                endTimestamp,
                startBatteryLevel,
                endBatteryLevel,
                totalScreenOnTime,
                appEntries,
                systemEntries,
                /* systemAppsPackageNames= */ new ArraySet<>(),
                /* systemAppsUids= */ new ArraySet<>(),
                /* isAccumulated= */ true);
    }

    private static void computeUsageDiffDataPerEntry(
            final BatteryDiffEntry entry, final Map<String, BatteryDiffEntry> diffEntryMap) {
        final String key = entry.getKey();
        final BatteryDiffEntry oldBatteryDiffEntry = diffEntryMap.get(key);
        // Creates new BatteryDiffEntry if we don't have it.
        if (oldBatteryDiffEntry == null) {
            diffEntryMap.put(key, entry.clone());
        } else {
            // Sums up some field data into the existing one.
            oldBatteryDiffEntry.mForegroundUsageTimeInMs += entry.mForegroundUsageTimeInMs;
            oldBatteryDiffEntry.mForegroundServiceUsageTimeInMs +=
                    entry.mForegroundServiceUsageTimeInMs;
            oldBatteryDiffEntry.mBackgroundUsageTimeInMs += entry.mBackgroundUsageTimeInMs;
            oldBatteryDiffEntry.mScreenOnTimeInMs += entry.mScreenOnTimeInMs;
            oldBatteryDiffEntry.mConsumePower += entry.mConsumePower;
            oldBatteryDiffEntry.mForegroundUsageConsumePower += entry.mForegroundUsageConsumePower;
            oldBatteryDiffEntry.mForegroundServiceUsageConsumePower +=
                    entry.mForegroundServiceUsageConsumePower;
            oldBatteryDiffEntry.mBackgroundUsageConsumePower += entry.mBackgroundUsageConsumePower;
            oldBatteryDiffEntry.mCachedUsageConsumePower += entry.mCachedUsageConsumePower;
        }
    }

    private static boolean shouldShowBatteryAttributionList(final Context context) {
        final PowerProfile powerProfile = new PowerProfile(context);
        // Cheap hack to try to figure out if the power_profile.xml was populated.
        final double averagePowerForOrdinal =
                powerProfile.getAveragePowerForOrdinal(
                        PowerProfile.POWER_GROUP_DISPLAY_SCREEN_FULL, 0);
        final boolean shouldShowBatteryAttributionList =
                averagePowerForOrdinal >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP;
        if (!shouldShowBatteryAttributionList) {
            Log.w(TAG, "shouldShowBatteryAttributionList(): " + averagePowerForOrdinal);
        }
        return shouldShowBatteryAttributionList;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that exists for
     * all users of the same app. We detect this case and merge the power use for dex2oat to the
     * device OWNER's use of the app.
     *
     * @return A sorted list of apps using power.
     */
    private static List<BatteryEntry> getCoalescedUsageList(
            final Context context,
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
        uidBatteryConsumers.sort(
                Comparator.comparingInt(
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
                batteryEntryList.put(
                        uid,
                        new BatteryEntry(
                                context,
                                userManager,
                                consumer,
                                isHidden,
                                uid,
                                packages,
                                null,
                                loadDataInBackground));
            } else {
                // Combine BatterySippers if we already have one with this UID.
                final BatteryEntry existingSipper = batteryEntryList.valueAt(index);
                existingSipper.add(consumer);
            }
        }

        final BatteryConsumer deviceConsumer =
                batteryUsageStats.getAggregateBatteryConsumer(
                        BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_DEVICE);

        for (int componentId = 0;
                componentId < BatteryConsumer.POWER_COMPONENT_COUNT;
                componentId++) {
            results.add(
                    new BatteryEntry(
                            context,
                            componentId,
                            deviceConsumer.getConsumedPower(componentId),
                            deviceConsumer.getUsageDurationMillis(componentId),
                            componentId == POWER_COMPONENT_SYSTEM_SERVICES
                                    || componentId == POWER_COMPONENT_WAKELOCK));
        }

        for (int componentId = BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID;
                componentId
                        < BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID
                                + deviceConsumer.getCustomPowerComponentCount();
                componentId++) {
            results.add(
                    new BatteryEntry(
                            context,
                            componentId,
                            deviceConsumer.getCustomPowerComponentName(componentId),
                            deviceConsumer.getConsumedPowerForCustomComponent(componentId)));
        }

        final List<UserBatteryConsumer> userBatteryConsumers =
                batteryUsageStats.getUserBatteryConsumers();
        for (int i = 0, size = userBatteryConsumers.size(); i < size; i++) {
            final UserBatteryConsumer consumer = userBatteryConsumers.get(i);
            results.add(
                    new BatteryEntry(
                            context,
                            userManager,
                            consumer, /* isHidden */
                            true,
                            Process.INVALID_UID,
                            null,
                            null,
                            loadDataInBackground));
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
            realUid =
                    UserHandle.getUid(
                            UserHandle.USER_SYSTEM,
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
                Log.e(
                        TAG,
                        "no ["
                                + dailyIndex
                                + "][SELECTED_INDEX_ALL] in batteryUsageMap, "
                                + "daily size is: "
                                + hourlyBatteryLevelsPerDay.size());
                return false;
            }
            if (hourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> timestamps = hourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            // Length of hourly usage map should be the length of hourly level data - 1.
            for (int hourlyIndex = 0; hourlyIndex < timestamps.size() - 1; hourlyIndex++) {
                if (!batteryUsageMap.get(dailyIndex).containsKey(hourlyIndex)) {
                    Log.e(
                            TAG,
                            "no ["
                                    + dailyIndex
                                    + "]["
                                    + hourlyIndex
                                    + "] in batteryUsageMap, "
                                    + "hourly size is: "
                                    + (timestamps.size() - 1));
                    return false;
                }
            }
        }
        return true;
    }

    private static long getDiffValue(long v1, long v2) {
        return v2 > v1 ? v2 - v1 : 0;
    }

    private static double getDiffValue(double v1, double v2) {
        return v2 > v1 ? v2 - v1 : 0;
    }

    private static long getCurrentTimeMillis() {
        return sTestCurrentTimeMillis > 0 ? sTestCurrentTimeMillis : System.currentTimeMillis();
    }

    private static void log(
            Context context,
            final String content,
            final long timestamp,
            final BatteryHistEntry entry) {
        if (sDebug) {
            Log.d(
                    TAG,
                    String.format(
                            entry != null ? "%s %s:\n%s" : "%s %s:%s",
                            ConvertUtils.utcToLocalTimeForLogging(timestamp),
                            content,
                            entry));
        }
    }
}
