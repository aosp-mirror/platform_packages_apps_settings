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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StorageCacheHelperTest {
    private static final long FAKE_IMAGES_SIZE = 7000L;
    private static final long FAKE_VIDEOS_SIZE = 8900L;
    private static final long FAKE_AUDIO_SIZE = 3500L;
    private static final long FAKE_APPS_SIZE = 4000L;
    private static final long FAKE_GAMES_SIZE = 5000L;
    private static final long FAKE_DOCS_SIZE = 1500L;
    private static final long FAKE_TRASH_SIZE = 500L;
    private static final long FAKE_SYSTEM_SIZE = 2300L;
    private static final long FAKE_TOTAL_SIZE = 256000L;
    private static final long FAKE_TOTAL_USED_SIZE = 50000L;
    private static final long FAKE_USED_SIZE = 6500L;

    private Context mContext;
    private StorageCacheHelper mHelper;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mHelper = new StorageCacheHelper(mContext, UserHandle.myUserId());
    }

    @Test
    public void hasCachedSizeInfo_noCacheData_shouldReturnFalse() {
        assertThat(mHelper.hasCachedSizeInfo()).isFalse();
    }

    @Test
    public void hasCachedSizeInfo_hasCacheData_shouldReturnTrue() {
        mHelper.cacheSizeInfo(getFakeStorageCache());

        assertThat(mHelper.hasCachedSizeInfo()).isTrue();
    }

    @Test
    public void cacheSizeInfo_shouldSaveToSharedPreference() {
        mHelper.cacheSizeInfo(getFakeStorageCache());

        StorageCacheHelper.StorageCache storageCache = mHelper.retrieveCachedSize();

        assertThat(storageCache.imagesSize).isEqualTo(FAKE_IMAGES_SIZE);
        assertThat(storageCache.totalSize).isEqualTo(0);
    }

    @Test
    public void cacheTotalSizeAndUsedSize_shouldSaveToSharedPreference() {
        mHelper.cacheTotalSizeAndTotalUsedSize(FAKE_TOTAL_SIZE, FAKE_TOTAL_USED_SIZE);

        StorageCacheHelper.StorageCache storageCache = mHelper.retrieveCachedSize();

        assertThat(storageCache.totalSize).isEqualTo(FAKE_TOTAL_SIZE);
        assertThat(storageCache.totalUsedSize).isEqualTo(FAKE_TOTAL_USED_SIZE);
    }

    @Test
    public void cacheUsedSize_shouldSaveToSharedPreference() {
        mHelper.cacheUsedSize(FAKE_USED_SIZE);

        assertThat(mHelper.retrieveUsedSize()).isEqualTo(FAKE_USED_SIZE);
    }

    private StorageCacheHelper.StorageCache getFakeStorageCache() {
        StorageCacheHelper.StorageCache result = new StorageCacheHelper.StorageCache();
        result.trashSize = FAKE_TRASH_SIZE;
        result.systemSize = FAKE_SYSTEM_SIZE;
        result.imagesSize = FAKE_IMAGES_SIZE;
        result.documentsAndOtherSize = FAKE_DOCS_SIZE;
        result.audioSize = FAKE_AUDIO_SIZE;
        result.gamesSize = FAKE_GAMES_SIZE;
        result.videosSize = FAKE_VIDEOS_SIZE;
        result.allAppsExceptGamesSize = FAKE_APPS_SIZE;
        return result;
    }
}
