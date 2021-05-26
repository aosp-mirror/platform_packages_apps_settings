/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;

import java.util.concurrent.TimeUnit;

public class CachedStorageValuesHelper {

    @VisibleForTesting public static final String SHARED_PREFERENCES_NAME = "CachedStorageValues";
    public static final String TIMESTAMP_KEY = "last_query_timestamp";
    public static final String FREE_BYTES_KEY = "free_bytes";
    public static final String TOTAL_BYTES_KEY = "total_bytes";
    public static final String GAME_APPS_SIZE_KEY = "game_apps_size";
    public static final String AUDIO_SIZE_KEY = "audio_size";
    public static final String VIDEOS_SIZE_KEY = "videos_size";
    public static final String IMAGES_SIZE_KEY = "images_size";
    public static final String DOCUMENTS_AND_OTHER_SIZE_KEY = "documents_and_other_size";
    public static final String TRASH_SIZE_KEY = "trash_size";
    public static final String OTHER_APPS_SIZE_KEY = "other_apps_size";
    public static final String CACHE_APPS_SIZE_KEY = "cache_apps_size";
    public static final String EXTERNAL_TOTAL_BYTES = "external_total_bytes";
    public static final String EXTERNAL_AUDIO_BYTES = "external_audio_bytes";
    public static final String EXTERNAL_VIDEO_BYTES = "external_video_bytes";
    public static final String EXTERNAL_IMAGE_BYTES = "external_image_bytes";
    public static final String EXTERNAL_APP_BYTES = "external_apps_bytes";
    public static final String USER_ID_KEY = "user_id";
    private final Long mClobberThreshold;
    private final SharedPreferences mSharedPreferences;
    private final int mUserId;
    // This clock is used to provide the time. By default, it uses the system clock, but can be
    // replaced for test purposes.
    protected Clock mClock;

    public CachedStorageValuesHelper(Context context, int userId) {
        mSharedPreferences =
                context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mClock = new Clock();
        mUserId = userId;
        mClobberThreshold =
                Settings.Global.getLong(
                        context.getContentResolver(),
                        Settings.Global.STORAGE_SETTINGS_CLOBBER_THRESHOLD,
                        TimeUnit.MINUTES.toMillis(5));
    }

    public PrivateStorageInfo getCachedPrivateStorageInfo() {
        if (!isDataValid()) {
            return null;
        }
        final long freeBytes = mSharedPreferences.getLong(FREE_BYTES_KEY, -1);
        final long totalBytes = mSharedPreferences.getLong(TOTAL_BYTES_KEY, -1);
        if (freeBytes < 0 || totalBytes < 0) {
            return null;
        }

        return new PrivateStorageInfo(freeBytes, totalBytes);
    }

    /** Returns cached storage result or null if it's not available. */
    public SparseArray<StorageAsyncLoader.StorageResult> getCachedStorageResult() {
        if (!isDataValid()) {
            return null;
        }
        final long gamesSize = mSharedPreferences.getLong(GAME_APPS_SIZE_KEY, -1);
        final long audioSize = mSharedPreferences.getLong(AUDIO_SIZE_KEY, -1);
        final long videosSize = mSharedPreferences.getLong(VIDEOS_SIZE_KEY, -1);
        final long imagesSize = mSharedPreferences.getLong(IMAGES_SIZE_KEY, -1);
        final long documentsAndOtherSize =
                mSharedPreferences.getLong(DOCUMENTS_AND_OTHER_SIZE_KEY, -1);
        final long trashSize = mSharedPreferences.getLong(TRASH_SIZE_KEY, -1);
        final long allAppsExceptGamesSize = mSharedPreferences.getLong(OTHER_APPS_SIZE_KEY, -1);
        final long cacheSize = mSharedPreferences.getLong(CACHE_APPS_SIZE_KEY, -1);
        if (gamesSize < 0
                || audioSize < 0
                || videosSize < 0
                || imagesSize < 0
                || documentsAndOtherSize < 0
                || trashSize < 0
                || allAppsExceptGamesSize < 0
                || cacheSize < 0) {
            return null;
        }

        final long externalTotalBytes = mSharedPreferences.getLong(EXTERNAL_TOTAL_BYTES, -1);
        final long externalAudioBytes = mSharedPreferences.getLong(EXTERNAL_AUDIO_BYTES, -1);
        final long externalVideoBytes = mSharedPreferences.getLong(EXTERNAL_VIDEO_BYTES, -1);
        final long externalImageBytes = mSharedPreferences.getLong(EXTERNAL_IMAGE_BYTES, -1);
        final long externalAppBytes = mSharedPreferences.getLong(EXTERNAL_APP_BYTES, -1);
        if (externalTotalBytes < 0
                || externalAudioBytes < 0
                || externalVideoBytes < 0
                || externalImageBytes < 0
                || externalAppBytes < 0) {
            return null;
        }

        final StorageStatsSource.ExternalStorageStats externalStats =
                new StorageStatsSource.ExternalStorageStats(
                        externalTotalBytes,
                        externalAudioBytes,
                        externalVideoBytes,
                        externalImageBytes,
                        externalAppBytes);
        final StorageAsyncLoader.StorageResult result = new StorageAsyncLoader.StorageResult();
        result.gamesSize = gamesSize;
        result.audioSize = audioSize;
        result.videosSize = videosSize;
        result.imagesSize = imagesSize;
        result.documentsAndOtherSize = documentsAndOtherSize;
        result.trashSize = trashSize;
        result.allAppsExceptGamesSize = allAppsExceptGamesSize;
        result.cacheSize = cacheSize;
        result.externalStats = externalStats;
        final SparseArray<StorageAsyncLoader.StorageResult> resultArray = new SparseArray<>();
        resultArray.append(mUserId, result);
        return resultArray;
    }

    public void cacheResult(
            PrivateStorageInfo storageInfo, StorageAsyncLoader.StorageResult result) {
        mSharedPreferences
                .edit()
                .putLong(FREE_BYTES_KEY, storageInfo.freeBytes)
                .putLong(TOTAL_BYTES_KEY, storageInfo.totalBytes)
                .putLong(GAME_APPS_SIZE_KEY, result.gamesSize)
                .putLong(AUDIO_SIZE_KEY, result.audioSize)
                .putLong(VIDEOS_SIZE_KEY, result.videosSize)
                .putLong(IMAGES_SIZE_KEY, result.imagesSize)
                .putLong(DOCUMENTS_AND_OTHER_SIZE_KEY, result.documentsAndOtherSize)
                .putLong(TRASH_SIZE_KEY, result.trashSize)
                .putLong(OTHER_APPS_SIZE_KEY, result.allAppsExceptGamesSize)
                .putLong(CACHE_APPS_SIZE_KEY, result.cacheSize)
                .putLong(EXTERNAL_TOTAL_BYTES, result.externalStats.totalBytes)
                .putLong(EXTERNAL_AUDIO_BYTES, result.externalStats.audioBytes)
                .putLong(EXTERNAL_VIDEO_BYTES, result.externalStats.videoBytes)
                .putLong(EXTERNAL_IMAGE_BYTES, result.externalStats.imageBytes)
                .putLong(EXTERNAL_APP_BYTES, result.externalStats.appBytes)
                .putInt(USER_ID_KEY, mUserId)
                .putLong(TIMESTAMP_KEY, mClock.getCurrentTime())
                .apply();
    }

    private boolean isDataValid() {
        final int cachedUserId = mSharedPreferences.getInt(USER_ID_KEY, -1);
        if (cachedUserId != mUserId) {
            return false;
        }

        final long lastQueryTime = mSharedPreferences.getLong(TIMESTAMP_KEY, Long.MAX_VALUE);
        final long currentTime = mClock.getCurrentTime();
        return currentTime - lastQueryTime < mClobberThreshold;
    }

    /** Clock provides the current time. */
    static class Clock {
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
    }
}
