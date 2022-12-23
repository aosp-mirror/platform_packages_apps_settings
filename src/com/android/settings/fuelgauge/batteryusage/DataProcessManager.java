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
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the async tasks to process battery and app usage data.
 *
 * For now, there exist 3 async tasks in this manager:
 * <ul>
 *  <li>loadCurrentBatteryHistoryMap: load the latest battery history data from battery stats
 *  service.</li>
 *  <li>loadCurrentAppUsageList: load the latest app usage data (last timestamp in database - now)
 *  from usage stats service.</li>
 *  <li>loadDatabaseAppUsageList: load the necessary app usage data (after last full charge) from
 *  database</li>
 * </ul>
 *
 * The 3 async tasks will be started at the same time.
 * <ul>
 *  <li>After loadCurrentAppUsageList and loadDatabaseAppUsageList complete, which means all app
 *  usage data has been loaded, the intermediate usage result will be generated.</li>
 *  <li>Then after all 3 async tasks complete, the battery history data and app usage data will be
 *  combined to generate final data used for UI rendering. And the callback function will be
 *  applied.</li>
 *  <li>If current user is locked, which means we couldn't get the latest app usage data,
 *  screen-on time will not be shown in the UI and empty screen-on time data will be returned.</li>
 * </ul>
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
    private long mStartTimestampOfLevelData = 0;

    private boolean mIsCurrentBatteryHistoryLoaded = false;
    private boolean mIsCurrentAppUsageLoaded = false;
    private boolean mIsDatabaseAppUsageLoaded = false;
    // Used to identify whether screen-on time data should be shown in the UI.
    private boolean mShowScreenOnTime = true;

    private List<AppUsageEvent> mAppUsageEventList = new ArrayList<>();

    /**
     * Constructor when this exists battery level data.
     */
    DataProcessManager(
            Context context,
            Handler handler,
            final DataProcessor.UsageMapAsyncResponse callbackFunction,
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
     * Starts the async tasks to load battery history data and app usage data.
     */
    public void start() {
        // Load the latest battery history data from the service.
        loadCurrentBatteryHistoryMap();
        // Load app usage list from database.
        loadDatabaseAppUsageList();
        // Load the latest app usage list from the service.
        loadCurrentAppUsageList();
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
        // Sort the appUsageEventList in ascending order based on the timestamp.
        Collections.sort(mAppUsageEventList, DataProcessor.TIMESTAMP_COMPARATOR);
        // TODO: process app usage data to an intermediate result for further use.
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
        // TODO: generate the final data including battery usage map and device screen-on time and
        // then apply the callback function.
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
}
