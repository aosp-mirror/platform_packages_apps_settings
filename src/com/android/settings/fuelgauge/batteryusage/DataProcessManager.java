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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the async tasks to process battery and app usage data.
 *
 * For now, there exist 4 async tasks in this manager:
 * <ul>
 *  <li>loadCurrentBatteryHistoryMap: load the latest battery history data from battery stats
 *  service.</li>
 *  <li>loadCurrentAppUsageList: load the latest app usage data (last timestamp in database - now)
 *  from usage stats service.</li>
 *  <li>loadDatabaseAppUsageList: load the necessary app usage data (after last full charge) from
 *  database</li>
 *  <li>loadAndApplyBatteryMapFromServiceOnly: load all the battery history data (should be after
 *  last full charge) from battery stats service and apply the callback function directly</li>
 * </ul>
 *
 * If there is battery level data, the first 3 async tasks will be started at the same time.
 * <ul>
 *  <li>After loadCurrentAppUsageList and loadDatabaseAppUsageList complete, which means all app
 *  usage data has been loaded, the intermediate usage result will be generated.</li>
 *  <li>Then after all 3 async tasks complete, the battery history data and app usage data will be
 *  combined to generate final data used for UI rendering. And the callback function will be
 *  applied.</li>
 *  <li>If current user is locked, which means we couldn't get the latest app usage data,
 *  screen-on time will not be shown in the UI and empty screen-on time data will be returned.</li>
 * </ul>
 *
 * If there is no battery level data, the 4th async task will be started only and the usage map
 * callback function will be applied directly to show the app list on the UI.
 */
public class DataProcessManager {
    private static final String TAG = "DataProcessManager";

    private final Handler mHandler;
    private final DataProcessor.UsageMapAsyncResponse mCallbackFunction;

    private Context mContext;
    private UserManager mUserManager;
    private List<BatteryLevelData.PeriodBatteryLevelData> mHourlyBatteryLevelsPerDay;
    private Map<Long, Map<String, BatteryHistEntry>> mBatteryHistoryMap;

    // The start timestamp of battery level data. As we don't know when is the full charge cycle
    // start time when loading app usage data, this value is used as the start time of querying app
    // usage data.
    private long mStartTimestampOfLevelData;

    private boolean mIsCurrentBatteryHistoryLoaded = false;
    private boolean mIsCurrentAppUsageLoaded = false;
    private boolean mIsDatabaseAppUsageLoaded = false;
    // Used to identify whether screen-on time data should be shown in the UI.
    private boolean mShowScreenOnTime = true;
    // Used to identify whether battery level data should be shown in the UI.
    private boolean mShowBatteryLevel = true;

    private List<AppUsageEvent> mAppUsageEventList = new ArrayList<>();
    /**
     * The indexed {@link AppUsagePeriod} list data for each corresponding time slot.
     * <p>{@code Long} stands for the userId.</p>
     * <p>{@code String} stands for the packageName.</p>
     */
    private Map<Integer, Map<Integer, Map<Long, Map<String, List<AppUsagePeriod>>>>>
            mAppUsagePeriodMap;

    /**
     * Constructor when there exists battery level data.
     */
    DataProcessManager(
            Context context,
            Handler handler,
            @NonNull final DataProcessor.UsageMapAsyncResponse callbackFunction,
            @NonNull final List<BatteryLevelData.PeriodBatteryLevelData> hourlyBatteryLevelsPerDay,
            @NonNull final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap) {
        mContext = context.getApplicationContext();
        mHandler = handler;
        mUserManager = mContext.getSystemService(UserManager.class);
        mCallbackFunction = callbackFunction;
        mHourlyBatteryLevelsPerDay = hourlyBatteryLevelsPerDay;
        mBatteryHistoryMap = batteryHistoryMap;
        mStartTimestampOfLevelData = getStartTimestampOfBatteryLevelData();
    }

    /**
     * Constructor when there is no battery level data.
     */
    DataProcessManager(
            Context context,
            Handler handler,
            @NonNull final DataProcessor.UsageMapAsyncResponse callbackFunction) {
        mContext = context.getApplicationContext();
        mHandler = handler;
        mUserManager = mContext.getSystemService(UserManager.class);
        mCallbackFunction = callbackFunction;
        // When there is no battery level data, don't show screen-on time and battery level chart on
        // the UI.
        mShowScreenOnTime = false;
        mShowBatteryLevel = false;
    }

    /**
     * Starts the async tasks to load battery history data and app usage data.
     */
    public void start() {
        // If we have battery level data, load the battery history map and app usage simultaneously.
        if (mShowBatteryLevel) {
            // Loads the latest battery history data from the service.
            loadCurrentBatteryHistoryMap();
            // Loads app usage list from database.
            loadDatabaseAppUsageList();
            // Loads the latest app usage list from the service.
            loadCurrentAppUsageList();
        } else {
            // If there is no battery level data, only load the battery history data from service
            // and show it as the app list directly.
            loadAndApplyBatteryMapFromServiceOnly();
        }
    }

    @VisibleForTesting
    long getStartTimestampOfBatteryLevelData() {
        for (int dailyIndex = 0; dailyIndex < mHourlyBatteryLevelsPerDay.size(); dailyIndex++) {
            if (mHourlyBatteryLevelsPerDay.get(dailyIndex) == null) {
                continue;
            }
            final List<Long> timestamps =
                    mHourlyBatteryLevelsPerDay.get(dailyIndex).getTimestamps();
            if (timestamps.size() > 0) {
                return timestamps.get(0);
            }
        }
        return 0;
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
    boolean getIsCurrentBatteryHistoryLoaded() {
        return mIsCurrentBatteryHistoryLoaded;
    }

    @VisibleForTesting
    boolean getShowScreenOnTime() {
        return mShowScreenOnTime;
    }

    @VisibleForTesting
    boolean getShowBatteryLevel() {
        return mShowBatteryLevel;
    }

    private void loadCurrentBatteryHistoryMap() {
        new AsyncTask<Void, Void, Map<String, BatteryHistEntry>>() {
            @Override
            protected Map<String, BatteryHistEntry> doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                // Loads the current battery usage data from the battery stats service.
                final Map<String, BatteryHistEntry> currentBatteryHistoryMap =
                        DataProcessor.getCurrentBatteryHistoryMapFromStatsService(
                                mContext);
                Log.d(TAG, String.format("execute loadCurrentBatteryHistoryMap size=%d in %d/ms",
                        currentBatteryHistoryMap.size(), (System.currentTimeMillis() - startTime)));
                return currentBatteryHistoryMap;
            }

            @Override
            protected void onPostExecute(
                    final Map<String, BatteryHistEntry> currentBatteryHistoryMap) {
                if (mBatteryHistoryMap != null) {
                    // Replaces the placeholder in mBatteryHistoryMap.
                    for (Map.Entry<Long, Map<String, BatteryHistEntry>> mapEntry
                            : mBatteryHistoryMap.entrySet()) {
                        if (mapEntry.getValue().containsKey(
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
                                mContext, currentUserId, mStartTimestampOfLevelData);
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
                                    mContext, workProfileUserId, mStartTimestampOfLevelData);
                } else {
                    Log.d(TAG, "there is no work profile");
                }

                final Map<Long, UsageEvents> usageEventsMap = new HashMap<>();
                usageEventsMap.put(Long.valueOf(currentUserId), usageEventsForCurrentUser);
                if (usageEventsForWorkProfile != null) {
                    Log.d(TAG, "usageEventsForWorkProfile is null");
                    usageEventsMap.put(Long.valueOf(workProfileUserId), usageEventsForWorkProfile);
                }

                final List<AppUsageEvent> appUsageEventList =
                        DataProcessor.generateAppUsageEventListFromUsageEvents(
                                mContext, usageEventsMap);
                Log.d(TAG, String.format("execute loadCurrentAppUsageList size=%d in %d/ms",
                        appUsageEventList.size(), (System.currentTimeMillis() - startTime)));
                return appUsageEventList;
            }

            @Override
            protected void onPostExecute(
                    final List<AppUsageEvent> currentAppUsageList) {
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
                // Loads the current battery usage data from the battery stats service.
                final List<AppUsageEvent> appUsageEventList =
                        DatabaseUtils.getAppUsageEventForUsers(
                                mContext, Calendar.getInstance(), getCurrentUserIds(),
                                mStartTimestampOfLevelData);
                Log.d(TAG, String.format("execute loadDatabaseAppUsageList size=%d in %d/ms",
                        appUsageEventList.size(), (System.currentTimeMillis() - startTime)));
                return appUsageEventList;
            }

            @Override
            protected void onPostExecute(
                    final List<AppUsageEvent> databaseAppUsageList) {
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

    private void loadAndApplyBatteryMapFromServiceOnly() {
        new AsyncTask<Void, Void, BatteryCallbackData>() {
            @Override
            protected BatteryCallbackData doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                final Map<Integer, Map<Integer, BatteryDiffData>> batteryUsageMap =
                        DataProcessor.getBatteryUsageMapFromStatsService(mContext);
                DataProcessor.loadLabelAndIcon(batteryUsageMap);
                Log.d(TAG, String.format(
                        "execute loadAndApplyBatteryMapFromServiceOnly size=%d in %d/ms",
                        batteryUsageMap.size(), (System.currentTimeMillis() - startTime)));
                return new BatteryCallbackData(batteryUsageMap, /*deviceScreenOnTime=*/ null);
            }

            @Override
            protected void onPostExecute(
                    final BatteryCallbackData batteryCallbackData) {
                // Set the unused variables to null.
                mContext = null;
                // Post results back to main thread to refresh UI.
                if (mHandler != null && mCallbackFunction != null) {
                    mHandler.post(() -> {
                        mCallbackFunction.onBatteryCallbackDataLoaded(batteryCallbackData);
                    });
                }
            }
        }.execute();
    }

    private void tryToProcessAppUsageData() {
        // Only when all app usage events has been loaded, start processing app usage data to an
        // intermediate result for further use.
        if (!mIsCurrentAppUsageLoaded || !mIsDatabaseAppUsageLoaded) {
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
        mAppUsagePeriodMap = DataProcessor.generateAppUsagePeriodMap(
                mHourlyBatteryLevelsPerDay, mAppUsageEventList);
    }

    private void tryToGenerateFinalDataAndApplyCallback() {
        // Only when both battery history data and app usage events data has been loaded, start the
        // final data processing.
        if (!mIsCurrentBatteryHistoryLoaded
                || !mIsCurrentAppUsageLoaded
                || !mIsDatabaseAppUsageLoaded) {
            return;
        }
        generateFinalDataAndApplyCallback();
    }

    private void generateFinalDataAndApplyCallback() {
        new AsyncTask<Void, Void, BatteryCallbackData>() {
            @Override
            protected BatteryCallbackData doInBackground(Void... voids) {
                final long startTime = System.currentTimeMillis();
                final Map<Integer, Map<Integer, BatteryDiffData>> batteryUsageMap =
                        DataProcessor.getBatteryUsageMap(
                                mContext, mHourlyBatteryLevelsPerDay, mBatteryHistoryMap,
                                mAppUsagePeriodMap);
                final Map<Integer, Map<Integer, Long>> deviceScreenOnTime =
                        DataProcessor.getDeviceScreenOnTime(mAppUsagePeriodMap);
                DataProcessor.loadLabelAndIcon(batteryUsageMap);
                Log.d(TAG, String.format("execute generateFinalDataAndApplyCallback in %d/ms",
                        (System.currentTimeMillis() - startTime)));
                return new BatteryCallbackData(batteryUsageMap, deviceScreenOnTime);
            }

            @Override
            protected void onPostExecute(final BatteryCallbackData batteryCallbackData) {
                // Set the unused variables to null.
                mContext = null;
                mHourlyBatteryLevelsPerDay = null;
                mBatteryHistoryMap = null;
                // Post results back to main thread to refresh UI.
                if (mHandler != null && mCallbackFunction != null) {
                    mHandler.post(() -> {
                        mCallbackFunction.onBatteryCallbackDataLoaded(batteryCallbackData);
                    });
                }
            }
        }.execute();
    }

    // Whether we should load app usage data from service or database.
    private boolean shouldLoadAppUsageData() {
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
        final UserHandle userHandle =
                Utils.getManagedProfile(mUserManager);
        return userHandle != null ? userHandle.getIdentifier() : Integer.MIN_VALUE;
    }

    /**
     * @return Returns battery level data and start async task to compute battery diff usage data
     * and load app labels + icons.
     * Returns null if the input is invalid or not having at least 2 hours data.
     */
    @Nullable
    public static BatteryLevelData getBatteryLevelData(
            Context context,
            @Nullable Handler handler,
            @Nullable final Map<Long, Map<String, BatteryHistEntry>> batteryHistoryMap,
            final DataProcessor.UsageMapAsyncResponse asyncResponseDelegate) {
        if (batteryHistoryMap == null || batteryHistoryMap.isEmpty()) {
            Log.d(TAG, "batteryHistoryMap is null in getBatteryLevelData()");
            new DataProcessManager(context, handler, asyncResponseDelegate).start();
            return null;
        }
        handler = handler != null ? handler : new Handler(Looper.getMainLooper());
        // Process raw history map data into hourly timestamps.
        final Map<Long, Map<String, BatteryHistEntry>> processedBatteryHistoryMap =
                DataProcessor.getHistoryMapWithExpectedTimestamps(context, batteryHistoryMap);
        // Wrap and processed history map into easy-to-use format for UI rendering.
        final BatteryLevelData batteryLevelData =
                DataProcessor.getLevelDataThroughProcessedHistoryMap(
                        context, processedBatteryHistoryMap);
        if (batteryLevelData == null) {
            new DataProcessManager(context, handler, asyncResponseDelegate).start();
            Log.d(TAG, "getBatteryLevelData() returns null");
            return null;
        }

        // Start the async task to compute diff usage data and load labels and icons.
        new DataProcessManager(
                context,
                handler,
                asyncResponseDelegate,
                batteryLevelData.getHourlyBatteryLevelsPerDay(),
                processedBatteryHistoryMap).start();

        return batteryLevelData;
    }
}
