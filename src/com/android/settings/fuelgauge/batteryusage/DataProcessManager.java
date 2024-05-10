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

import android.app.usage.UsageEvents;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the async tasks to process battery and app usage data.
 *
 * <p>For now, there exist 4 async tasks in this manager:
 *
 * <ul>
 *   <li>loadCurrentBatteryHistoryMap: load the latest battery history data from battery stats
 *       service.
 *   <li>loadCurrentAppUsageList: load the latest app usage data (last timestamp in database - now)
 *       from usage stats service.
 *   <li>loadDatabaseAppUsageList: load the necessary app usage data (after last full charge) from
 *       database
 *   <li>loadAndApplyBatteryMapFromServiceOnly: load all the battery history data (should be after
 *       last full charge) from battery stats service and apply the callback function directly
 * </ul>
 *
 * If there is battery level data, the first 3 async tasks will be started at the same time.
 *
 * <ul>
 *   <li>After loadCurrentAppUsageList and loadDatabaseAppUsageList complete, which means all app
 *       usage data has been loaded, the intermediate usage result will be generated.
 *   <li>Then after all 3 async tasks complete, the battery history data and app usage data will be
 *       combined to generate final data used for UI rendering. And the callback function will be
 *       applied.
 *   <li>If current user is locked, which means we couldn't get the latest app usage data, screen-on
 *       time will not be shown in the UI and empty screen-on time data will be returned.
 * </ul>
 *
 * If there is no battery level data, the 4th async task will be started only and the usage map
 * callback function will be applied directly to show the app list on the UI.
 */
public class DataProcessManager {
    private static final String TAG = "DataProcessManager";
    private static final List<BatteryEventType> POWER_CONNECTION_EVENTS =
            List.of(BatteryEventType.POWER_CONNECTED, BatteryEventType.POWER_DISCONNECTED);

    // For testing only.
    @VisibleForTesting static Map<Long, Map<String, BatteryHistEntry>> sFakeBatteryHistoryMap;

    // Raw start timestamp with round to the nearest hour.
    private final long mRawStartTimestamp;
    private final long mLastFullChargeTimestamp;
    private final Context mContext;
    private final Handler mHandler;
    private final UserManager mUserManager;
    private final OnBatteryDiffDataMapLoadedListener mCallbackFunction;
    private final List<AppUsageEvent> mAppUsageEventList = new ArrayList<>();
    private final List<BatteryEvent> mBatteryEventList = new ArrayList<>();
    private final List<BatteryUsageSlot> mBatteryUsageSlotList = new ArrayList<>();
    private final List<BatteryLevelData.PeriodBatteryLevelData> mHourlyBatteryLevelsPerDay;
    private final Map<Long, Map<String, BatteryHistEntry>> mBatteryHistoryMap;

    private boolean mIsCurrentBatteryHistoryLoaded = false;
    private boolean mIsCurrentAppUsageLoaded = false;
    private boolean mIsDatabaseAppUsageLoaded = false;
    private boolean mIsBatteryEventLoaded = false;
    private boolean mIsBatteryUsageSlotLoaded = false;
    // Used to identify whether screen-on time data should be shown in the UI.
    private boolean mShowScreenOnTime = true;
    private Set<String> mSystemAppsPackageNames = null;
    private Set<Integer> mSystemAppsUids = null;

    /**
     * The indexed {@link AppUsagePeriod} list data for each corresponding time slot.
     *
     * <p>{@code Long} stands for the userId.
     *
     * <p>{@code String} stands for the packageName.
     */
    private Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
            mAppUsagePeriodMap;

    /**
     * A callback listener when all the data is processed. This happens when all the async tasks
     * complete and generate the final callback.
     */
    public interface OnBatteryDiffDataMapLoadedListener {
        /** The callback function when all the data is processed. */
        void onBatteryDiffDataMapLoaded(Map<Long, BatteryDiffData> batteryDiffDataMap);
    }

    /** Constructor when there exists battery level data. */
    DataProcessManager(
            Context context,
            Handler handler,
            final long rawStartTimestamp,
            final long lastFullChargeTimestamp,
            @NonNull final OnBatteryDiffDataMapLoadedListener callbackFunction,
            @NonNull final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            @NonNull final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        mContext = context.getApplicationContext();
        mHandler = handler;
        mUserManager = mContext.getSystemService(UserManager.class);
        mRawStartTimestamp = rawStartTimestamp;
        mLastFullChargeTimestamp = lastFullChargeTimestamp;
        mCallbackFunction = callbackFunction;
        mHourlyBatteryLevelsPerDay = hourlyBatteryLevelsPerDay;
        mBatteryHistoryMap = batteryHistoryMap;
    }

    /** Constructor when there is no battery level data. */
    DataProcessManager(
            Context context,
            Handler handler,
            @NonNull final OnBatteryDiffDataMapLoadedListener callbackFunction) {
        mContext = context.getApplicationContext();
        mHandler = handler;
        mUserManager = mContext.getSystemService(UserManager.class);
        mCallbackFunction = callbackFunction;
        mRawStartTimestamp = 0L;
        mLastFullChargeTimestamp = 0L;
        mHourlyBatteryLevelsPerDay = null;
        mBatteryHistoryMap = null;
        // When there is no battery level data, don't show screen-on time and battery level chart on
        // the UI.
        mShowScreenOnTime = false;
    }

    /** Starts the async tasks to load battery history data and app usage data. */
    public void start() {
        start(/* isFromPeriodJob= */ false);
    }

    /** Starts the async tasks to load battery history data and app usage data. */
    public void start(boolean isFromPeriodJob) {
        // If we have battery level data, load the battery history map and app usage simultaneously.
        if (mHourlyBatteryLevelsPerDay != null) {
            if (isFromPeriodJob) {
                mIsCurrentBatteryHistoryLoaded = true;
                mIsCurrentAppUsageLoaded = true;
                mIsBatteryUsageSlotLoaded = true;
            } else {
                // Loads the latest battery history data from the service.
                loadCurrentBatteryHistoryMap();
                // Loads the latest app usage list from the service.
                loadCurrentAppUsageList();
                // Loads existing battery usage slots from database.
                loadBatteryUsageSlotList();
            }
            // Loads app usage list from database.
            loadDatabaseAppUsageList();
            // Loads the battery event list from database.
            loadPowerConnectionBatteryEventList();
        } else {
            // If there is no battery level data, only load the battery history data from service
            // and show it as the app list directly.
            loadAndApplyBatteryMapFromServiceOnly();
        }
    }

    @VisibleForTesting
    List<AppUsageEvent> getAppUsageEventList() {
        return mAppUsageEventList;
    }

    @VisibleForTesting
    Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
            getAppUsagePeriodMap() {
        return mAppUsagePeriodMap;
    }

    @VisibleForTesting
    boolean getIsCurrentAppUsageLoaded() {
        return mIsCurrentAppUsageLoaded;
    }

    @VisibleForTesting
    boolean getIsDatabaseAppUsageLoaded() {
        return mIsDatabaseAppUsageLoaded;
    }

    @VisibleForTesting
    boolean getIsBatteryEventLoaded() {
        return mIsBatteryEventLoaded;
    }

    @VisibleForTesting
    boolean getIsCurrentBatteryHistoryLoaded() {
        return mIsCurrentBatteryHistoryLoaded;
    }

    @VisibleForTesting
    boolean getShowScreenOnTime() {
        return mShowScreenOnTime;
    }

    private void loadCurrentBatteryHistoryMap() {
        new AsyncTask<Void, Void, Map<String, BatteryHistEntry>>() {
            @Override
            protected Map<String, BatteryHistEntry> doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                // Loads the current battery usage data from the battery stats service.
                final Map<String, BatteryHistEntry> currentBatteryHistoryMap =
                        DataProcessor.getCurrentBatteryHistoryMapFromStatsService(mContext);
                Log.d(
                        TAG,
                        String.format(
                                "execute loadCurrentBatteryHistoryMap size=%d in %d/ms",
                                currentBatteryHistoryMap.size(),
                                (System.currentTimeMillis() - startTime)));
                return currentBatteryHistoryMap;
            }

            @Override
            protected void onPostExecute(
                    final Map<String, BatteryHistEntry> currentBatteryHistoryMap) {
                if (mBatteryHistoryMap != null) {
                    // Replaces the placeholder in mBatteryHistoryMap.
                    for (Map.Entry<Long, Map<String, BatteryHistEntry>> mapEntry :
                            mBatteryHistoryMap.entrySet()) {
                        if (mapEntry.getValue()
                                .containsKey(
                                        DataProcessor.CURRENT_TIME_BATTERY_HISTORY_PLACEHOLDER)) {
                            mapEntry.setValue(currentBatteryHistoryMap);
                        }
                    }
                }
                mIsCurrentBatteryHistoryLoaded = true;
                tryToGenerateFinalDataAndApplyCallback();
            }
        }.execute();
    }

    private void loadCurrentAppUsageList() {
        new AsyncTask<Void, Void, List<AppUsageEvent>>() {
            @Override
            protected List<AppUsageEvent> doInBackground(Void... voids) {
                if (!shouldLoadAppUsageData()) {
                    Log.d(TAG, "not loadCurrentAppUsageList");
                    return null;
                }
                final long startTime = System.currentTimeMillis();
                // Loads the current battery usage data from the battery stats service.
                final int currentUserId = getCurrentUserId();
                final int workProfileUserId = getWorkProfileUserId();
                final UsageEvents usageEventsForCurrentUser =
                        DataProcessor.getAppUsageEventsForUser(
                                mContext, currentUserId, mRawStartTimestamp);
                // If fail to load usage events for current user, return null directly and screen-on
                // time will not be shown in the UI.
                if (usageEventsForCurrentUser == null) {
                    Log.w(TAG, "usageEventsForCurrentUser is null");
                    return null;
                }
                UsageEvents usageEventsForWorkProfile = null;
                if (workProfileUserId != Integer.MIN_VALUE) {
                    usageEventsForWorkProfile =
                            DataProcessor.getAppUsageEventsForUser(
                                    mContext, workProfileUserId, mRawStartTimestamp);
                } else {
                    Log.d(TAG, "there is no work profile");
                }

                final Map<Long, UsageEvents> usageEventsMap = new ArrayMap<>();
                usageEventsMap.put(Long.valueOf(currentUserId), usageEventsForCurrentUser);
                if (usageEventsForWorkProfile != null) {
                    Log.d(TAG, "usageEventsForWorkProfile is null");
                    usageEventsMap.put(Long.valueOf(workProfileUserId), usageEventsForWorkProfile);
                }

                final List<AppUsageEvent> appUsageEventList =
                        DataProcessor.generateAppUsageEventListFromUsageEvents(
                                mContext, usageEventsMap);
                Log.d(
                        TAG,
                        String.format(
                                "execute loadCurrentAppUsageList size=%d in %d/ms",
                                appUsageEventList.size(),
                                (System.currentTimeMillis() - startTime)));
                return appUsageEventList;
            }

            @Override
            protected void onPostExecute(final List<AppUsageEvent> currentAppUsageList) {
                if (currentAppUsageList == null || currentAppUsageList.isEmpty()) {
                    Log.d(TAG, "currentAppUsageList is null or empty");
                } else {
                    mAppUsageEventList.addAll(currentAppUsageList);
                }
                mIsCurrentAppUsageLoaded = true;
                tryToProcessAppUsageData();
            }
        }.execute();
    }

    private void loadDatabaseAppUsageList() {
        new AsyncTask<Void, Void, List<AppUsageEvent>>() {
            @Override
            protected List<AppUsageEvent> doInBackground(Void... voids) {
                if (!shouldLoadAppUsageData()) {
                    Log.d(TAG, "not loadDatabaseAppUsageList");
                    return null;
                }
                final long startTime = System.currentTimeMillis();
                // Loads the app usage data from the database.
                final List<AppUsageEvent> appUsageEventList =
                        DatabaseUtils.getAppUsageEventForUsers(
                                mContext,
                                Calendar.getInstance(),
                                getCurrentUserIds(),
                                mRawStartTimestamp);
                Log.d(
                        TAG,
                        String.format(
                                "execute loadDatabaseAppUsageList size=%d in %d/ms",
                                appUsageEventList.size(),
                                (System.currentTimeMillis() - startTime)));
                return appUsageEventList;
            }

            @Override
            protected void onPostExecute(final List<AppUsageEvent> databaseAppUsageList) {
                if (databaseAppUsageList == null || databaseAppUsageList.isEmpty()) {
                    Log.d(TAG, "databaseAppUsageList is null or empty");
                } else {
                    mAppUsageEventList.addAll(databaseAppUsageList);
                }
                mIsDatabaseAppUsageLoaded = true;
                tryToProcessAppUsageData();
            }
        }.execute();
    }

    private void loadPowerConnectionBatteryEventList() {
        new AsyncTask<Void, Void, List<BatteryEvent>>() {
            @Override
            protected List<BatteryEvent> doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                // Loads the battery event data from the database.
                final List<BatteryEvent> batteryEventList =
                        DatabaseUtils.getBatteryEvents(
                                mContext,
                                Calendar.getInstance(),
                                mRawStartTimestamp,
                                POWER_CONNECTION_EVENTS);
                Log.d(
                        TAG,
                        String.format(
                                "execute loadPowerConnectionBatteryEventList size=%d in %d/ms",
                                batteryEventList.size(), (System.currentTimeMillis() - startTime)));
                return batteryEventList;
            }

            @Override
            protected void onPostExecute(final List<BatteryEvent> batteryEventList) {
                if (batteryEventList == null || batteryEventList.isEmpty()) {
                    Log.d(TAG, "batteryEventList is null or empty");
                } else {
                    mBatteryEventList.clear();
                    mBatteryEventList.addAll(batteryEventList);
                }
                mIsBatteryEventLoaded = true;
                tryToProcessAppUsageData();
            }
        }.execute();
    }

    private void loadBatteryUsageSlotList() {
        new AsyncTask<Void, Void, List<BatteryUsageSlot>>() {
            @Override
            protected List<BatteryUsageSlot> doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                // Loads the battery usage slot data from the database.
                final List<BatteryUsageSlot> batteryUsageSlotList =
                        DatabaseUtils.getBatteryUsageSlots(
                                mContext, Calendar.getInstance(), mLastFullChargeTimestamp);
                Log.d(
                        TAG,
                        String.format(
                                "execute loadBatteryUsageSlotList size=%d in %d/ms",
                                batteryUsageSlotList.size(),
                                (System.currentTimeMillis() - startTime)));
                return batteryUsageSlotList;
            }

            @Override
            protected void onPostExecute(final List<BatteryUsageSlot> batteryUsageSlotList) {
                if (batteryUsageSlotList == null || batteryUsageSlotList.isEmpty()) {
                    Log.d(TAG, "batteryUsageSlotList is null or empty");
                } else {
                    mBatteryUsageSlotList.clear();
                    mBatteryUsageSlotList.addAll(batteryUsageSlotList);
                }
                mIsBatteryUsageSlotLoaded = true;
                tryToGenerateFinalDataAndApplyCallback();
            }
        }.execute();
    }

    private void loadAndApplyBatteryMapFromServiceOnly() {
        new AsyncTask<Void, Void, Map<Long, BatteryDiffData>>() {
            @Override
            protected Map<Long, BatteryDiffData> doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                final Map<Long, BatteryDiffData> batteryDiffDataMap =
                        DataProcessor.getBatteryDiffDataMapFromStatsService(
                                mContext,
                                mRawStartTimestamp,
                                getSystemAppsPackageNames(),
                                getSystemAppsUids());
                Log.d(
                        TAG,
                        String.format(
                                "execute loadAndApplyBatteryMapFromServiceOnly size=%d in %d/ms",
                                batteryDiffDataMap.size(),
                                (System.currentTimeMillis() - startTime)));
                return batteryDiffDataMap;
            }

            @Override
            protected void onPostExecute(final Map<Long, BatteryDiffData> batteryDiffDataMap) {
                // Post results back to main thread to refresh UI.
                if (mHandler != null && mCallbackFunction != null) {
                    mHandler.post(
                            () -> {
                                mCallbackFunction.onBatteryDiffDataMapLoaded(batteryDiffDataMap);
                            });
                }
            }
        }.execute();
    }

    private void tryToProcessAppUsageData() {
        // Ignore processing the data if any required data is not loaded.
        if (!mIsCurrentAppUsageLoaded || !mIsDatabaseAppUsageLoaded || !mIsBatteryEventLoaded) {
            return;
        }
        processAppUsageData();
        tryToGenerateFinalDataAndApplyCallback();
    }

    private void processAppUsageData() {
        // If there is no screen-on time data, no need to process.
        if (!mShowScreenOnTime) {
            return;
        }
        // Generates the indexed AppUsagePeriod list data for each corresponding time slot for
        // further use.
        mAppUsagePeriodMap =
                DataProcessor.generateAppUsagePeriodMap(
                        mContext,
                        mHourlyBatteryLevelsPerDay,
                        mAppUsageEventList,
                        mBatteryEventList);
    }

    private void tryToGenerateFinalDataAndApplyCallback() {
        // Ignore processing the data if any required data is not loaded.
        if (!mIsCurrentBatteryHistoryLoaded
                || !mIsCurrentAppUsageLoaded
                || !mIsDatabaseAppUsageLoaded
                || !mIsBatteryEventLoaded
                || !mIsBatteryUsageSlotLoaded) {
            return;
        }
        generateFinalDataAndApplyCallback();
    }

    private synchronized void generateFinalDataAndApplyCallback() {
        new AsyncTask<Void, Void, Map<Long, BatteryDiffData>>() {
            @Override
            protected Map<Long, BatteryDiffData> doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                final Map<Long, BatteryDiffData> batteryDiffDataMap = new ArrayMap<>();
                for (BatteryUsageSlot batteryUsageSlot : mBatteryUsageSlotList) {
                    batteryDiffDataMap.put(
                            batteryUsageSlot.getStartTimestamp(),
                            ConvertUtils.convertToBatteryDiffData(
                                    mContext,
                                    batteryUsageSlot,
                                    getSystemAppsPackageNames(),
                                    getSystemAppsUids()));
                }
                batteryDiffDataMap.putAll(
                        DataProcessor.getBatteryDiffDataMap(
                                mContext,
                                mHourlyBatteryLevelsPerDay,
                                mBatteryHistoryMap,
                                mAppUsagePeriodMap,
                                getSystemAppsPackageNames(),
                                getSystemAppsUids()));

                Log.d(
                        TAG,
                        String.format(
                                "execute generateFinalDataAndApplyCallback size=%d in %d/ms",
                                batteryDiffDataMap.size(), System.currentTimeMillis() - startTime));
                return batteryDiffDataMap;
            }

            @Override
            protected void onPostExecute(final Map<Long, BatteryDiffData> batteryDiffDataMap) {
                // Post results back to main thread to refresh UI.
                if (mHandler != null && mCallbackFunction != null) {
                    mHandler.post(
                            () -> {
                                mCallbackFunction.onBatteryDiffDataMapLoaded(batteryDiffDataMap);
                            });
                }
            }
        }.execute();
    }

    // Whether we should load app usage data from service or database.
    private synchronized boolean shouldLoadAppUsageData() {
        if (!mShowScreenOnTime) {
            return false;
        }
        final int currentUserId = getCurrentUserId();
        // If current user is locked, no need to load app usage data from service or database.
        if (mUserManager == null || !mUserManager.isUserUnlocked(currentUserId)) {
            Log.d(TAG, "shouldLoadAppUsageData: false, current user is locked");
            mShowScreenOnTime = false;
            return false;
        }
        return true;
    }

    // Returns the list of current user id and work profile id if exists.
    private List<Integer> getCurrentUserIds() {
        final List<Integer> userIds = new ArrayList<>();
        userIds.add(getCurrentUserId());
        final int workProfileUserId = getWorkProfileUserId();
        if (workProfileUserId != Integer.MIN_VALUE) {
            userIds.add(workProfileUserId);
        }
        return userIds;
    }

    private int getCurrentUserId() {
        return mContext.getUserId();
    }

    private int getWorkProfileUserId() {
        final UserHandle userHandle = Utils.getManagedProfile(mUserManager);
        return userHandle != null ? userHandle.getIdentifier() : Integer.MIN_VALUE;
    }

    private synchronized Set<String> getSystemAppsPackageNames() {
        if (mSystemAppsPackageNames == null) {
            mSystemAppsPackageNames = DataProcessor.getSystemAppsPackageNames(mContext);
        }
        return mSystemAppsPackageNames;
    }

    private synchronized Set<Integer> getSystemAppsUids() {
        if (mSystemAppsUids == null) {
            mSystemAppsUids = DataProcessor.getSystemAppsUids(mContext);
        }
        return mSystemAppsUids;
    }

    /**
     * @return Returns battery level data and start async task to compute battery diff usage data
     *     and load app labels + icons. Returns null if the input is invalid or not having at least
     *     2 hours data.
     */
    @Nullable
    public static BatteryLevelData getBatteryLevelData(
            Context context,
            @Nullable Handler handler,
            final boolean isFromPeriodJob,
            final OnBatteryDiffDataMapLoadedListener onBatteryUsageMapLoadedListener) {
        final long start = System.currentTimeMillis();
        final long lastFullChargeTime = DatabaseUtils.getLastFullChargeTime(context);
        final List<BatteryEvent> batteryLevelRecordEvents =
                DatabaseUtils.getBatteryEvents(
                        context,
                        Calendar.getInstance(),
                        lastFullChargeTime,
                        DatabaseUtils.BATTERY_LEVEL_RECORD_EVENTS);
        final long startTimestamp =
                batteryLevelRecordEvents.isEmpty()
                        ? lastFullChargeTime
                        : batteryLevelRecordEvents.get(0).getTimestamp();
        final BatteryLevelData batteryLevelData =
                getPeriodBatteryLevelData(
                        context,
                        handler,
                        startTimestamp,
                        lastFullChargeTime,
                        isFromPeriodJob,
                        onBatteryUsageMapLoadedListener);
        Log.d(
                TAG,
                String.format(
                        "execute getBatteryLevelData in %d/ms,"
                                + " batteryLevelRecordEvents.size=%d",
                        (System.currentTimeMillis() - start), batteryLevelRecordEvents.size()));

        return isFromPeriodJob
                ? batteryLevelData
                : BatteryLevelData.combine(batteryLevelData, batteryLevelRecordEvents);
    }

    private static BatteryLevelData getPeriodBatteryLevelData(
            Context context,
            @Nullable Handler handler,
            final long startTimestamp,
            final long lastFullChargeTime,
            final boolean isFromPeriodJob,
            final OnBatteryDiffDataMapLoadedListener onBatteryDiffDataMapLoadedListener) {
        final long currentTime = System.currentTimeMillis();
        Log.d(
                TAG,
                String.format(
                        "getPeriodBatteryLevelData() startTimestamp=%s",
                        ConvertUtils.utcToLocalTimeForLogging(startTimestamp)));
        if (isFromPeriodJob
                && startTimestamp >= TimestampUtils.getLastEvenHourTimestamp(currentTime)) {
            // Nothing needs to be loaded for period job.
            return null;
        }

        handler = handler != null ? handler : new Handler(Looper.getMainLooper());
        final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap =
                sFakeBatteryHistoryMap != null
                        ? sFakeBatteryHistoryMap
                        : DatabaseUtils.getHistoryMapSinceLatestRecordBeforeQueryTimestamp(
                                context,
                                Calendar.getInstance(),
                                startTimestamp,
                                lastFullChargeTime);
        if (batteryHistoryMap == null || batteryHistoryMap.isEmpty()) {
            Log.d(TAG, "batteryHistoryMap is null in getPeriodBatteryLevelData()");
            new DataProcessManager(context, handler, onBatteryDiffDataMapLoadedListener).start();
            return null;
        }

        // Process raw history map data into hourly timestamps.
        final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap =
                DataProcessor.getHistoryMapWithExpectedTimestamps(context, batteryHistoryMap);
        // Wrap and processed history map into easy-to-use format for UI rendering.
        final BatteryLevelData batteryLevelData =
                DataProcessor.getLevelDataThroughProcessedHistoryMap(
                        context, processedBatteryHistoryMap);
        if (batteryLevelData == null) {
            new DataProcessManager(context, handler, onBatteryDiffDataMapLoadedListener).start();
            Log.d(TAG, "getBatteryLevelData() returns null");
            return null;
        }

        // Start the async task to compute diff usage data and load labels and icons.
        new DataProcessManager(
                        context,
                        handler,
                        startTimestamp,
                        lastFullChargeTime,
                        onBatteryDiffDataMapLoadedListener,
                        batteryLevelData.getHourlyBatteryLevelsPerDay(),
                        processedBatteryHistoryMap)
                .start(isFromPeriodJob);

        return batteryLevelData;
    }
}
