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

package com.android.settings.deviceinfo.storage;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A utility class to cache and restore the storage size information.
 */
public class StorageCacheHelper {

    private static final String SHARED_PREFERENCE_NAME = "StorageCache";
    private static final String TOTAL_SIZE_KEY = "total_size_key";
    private static final String TOTAL_USED_SIZE_KEY = "total_used_size_key";
    private static final String IMAGES_SIZE_KEY = "images_size_key";
    private static final String VIDEOS_SIZE_KEY = "videos_size_key";
    private static final String AUDIO_SIZE_KEY = "audio_size_key";
    private static final String APPS_SIZE_KEY = "apps_size_key";
    private static final String GAMES_SIZE_KEY = "games_size_key";
    private static final String DOCUMENTS_AND_OTHER_SIZE_KEY = "documents_and_other_size_key";
    private static final String TRASH_SIZE_KEY = "trash_size_key";
    private static final String SYSTEM_SIZE_KEY = "system_size_key";
    private static final String USED_SIZE_KEY = "used_size_key";

    private final SharedPreferences mSharedPreferences;

    public StorageCacheHelper(Context context, int userId) {
        String sharedPrefName = SHARED_PREFERENCE_NAME + userId;
        mSharedPreferences = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
    }

    /**
     * Returns true if there's a cached size info.
     */
    public boolean hasCachedSizeInfo() {
        return mSharedPreferences.getAll().size() > 0;
    }

    /**
     * Cache the size info
     * @param data a data about the file size info.
     */
    public void cacheSizeInfo(StorageCache data) {
        mSharedPreferences
                .edit()
                .putLong(IMAGES_SIZE_KEY, data.imagesSize)
                .putLong(VIDEOS_SIZE_KEY, data.videosSize)
                .putLong(AUDIO_SIZE_KEY, data.audioSize)
                .putLong(APPS_SIZE_KEY, data.allAppsExceptGamesSize)
                .putLong(GAMES_SIZE_KEY, data.gamesSize)
                .putLong(DOCUMENTS_AND_OTHER_SIZE_KEY, data.documentsAndOtherSize)
                .putLong(TRASH_SIZE_KEY, data.trashSize)
                .putLong(SYSTEM_SIZE_KEY, data.systemSize)
                .apply();
    }

    /**
     * Cache total size and total used size
     */
    public void cacheTotalSizeAndTotalUsedSize(long totalSize, long totalUsedSize) {
        mSharedPreferences
                .edit()
                .putLong(TOTAL_SIZE_KEY, totalSize)
                .putLong(TOTAL_USED_SIZE_KEY, totalUsedSize)
                .apply();
    }

    /**
     * Cache used size info when a user is treated as a secondary user.
     */
    public void cacheUsedSize(long usedSize) {
        mSharedPreferences.edit().putLong(USED_SIZE_KEY, usedSize).apply();
    }

    /**
     * Returns used size for secondary user.
     */
    public long retrieveUsedSize() {
        return mSharedPreferences.getLong(USED_SIZE_KEY, 0);
    }

    /**
     * Returns a cached data about all file size information.
     */
    public StorageCache retrieveCachedSize() {
        StorageCache result = new StorageCache();
        result.totalSize = mSharedPreferences.getLong(TOTAL_SIZE_KEY, 0);
        result.totalUsedSize = mSharedPreferences.getLong(TOTAL_USED_SIZE_KEY, 0);
        result.imagesSize = mSharedPreferences.getLong(IMAGES_SIZE_KEY, 0);
        result.videosSize = mSharedPreferences.getLong(VIDEOS_SIZE_KEY, 0);
        result.audioSize = mSharedPreferences.getLong(AUDIO_SIZE_KEY, 0);
        result.allAppsExceptGamesSize = mSharedPreferences.getLong(APPS_SIZE_KEY, 0);
        result.gamesSize = mSharedPreferences.getLong(GAMES_SIZE_KEY, 0);
        result.documentsAndOtherSize = mSharedPreferences.getLong(DOCUMENTS_AND_OTHER_SIZE_KEY, 0);
        result.trashSize = mSharedPreferences.getLong(TRASH_SIZE_KEY, 0);
        result.systemSize = mSharedPreferences.getLong(SYSTEM_SIZE_KEY, 0);
        return result;
    }

    /**
     *  All the cached data about the file size information.
     */
    public static class StorageCache {
        public long totalSize;
        public long totalUsedSize;
        public long gamesSize;
        public long allAppsExceptGamesSize;
        public long audioSize;
        public long imagesSize;
        public long videosSize;
        public long documentsAndOtherSize;
        public long trashSize;
        public long systemSize;
    }
}
