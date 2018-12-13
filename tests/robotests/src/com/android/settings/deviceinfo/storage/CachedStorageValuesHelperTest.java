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

import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.CACHE_APPS_SIZE_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.EXTERNAL_APP_BYTES;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper
        .EXTERNAL_AUDIO_BYTES;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper
        .EXTERNAL_IMAGE_BYTES;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper
        .EXTERNAL_TOTAL_BYTES;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper
        .EXTERNAL_VIDEO_BYTES;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.FREE_BYTES_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.GAME_APPS_SIZE_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.MUSIC_APPS_SIZE_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.OTHER_APPS_SIZE_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.PHOTO_APPS_SIZE_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper
        .SHARED_PREFERENCES_NAME;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.TIMESTAMP_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.TOTAL_BYTES_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.USER_ID_KEY;
import static com.android.settings.deviceinfo.storage.CachedStorageValuesHelper.VIDEO_APPS_SIZE_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseArray;

import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CachedStorageValuesHelperTest {

    private Context mContext;

    @Mock private CachedStorageValuesHelper.Clock mMockClock;
    private CachedStorageValuesHelper mCachedValuesHelper;
    private SharedPreferences mSharedPreferences;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSharedPreferences = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
        mCachedValuesHelper = new CachedStorageValuesHelper(mContext, 0);
        mCachedValuesHelper.mClock = mMockClock;
    }

    @Test
    public void getCachedPrivateStorageInfo_cachedValuesAreLoaded() {
        when(mMockClock.getCurrentTime()).thenReturn(10001L);
        mSharedPreferences
                .edit()
                .putLong(GAME_APPS_SIZE_KEY, 0)
                .putLong(MUSIC_APPS_SIZE_KEY, 10)
                .putLong(VIDEO_APPS_SIZE_KEY, 100)
                .putLong(PHOTO_APPS_SIZE_KEY, 1000)
                .putLong(OTHER_APPS_SIZE_KEY, 10000)
                .putLong(CACHE_APPS_SIZE_KEY, 100000)
                .putLong(EXTERNAL_TOTAL_BYTES, 2)
                .putLong(EXTERNAL_AUDIO_BYTES, 22)
                .putLong(EXTERNAL_VIDEO_BYTES, 222)
                .putLong(EXTERNAL_IMAGE_BYTES, 2222)
                .putLong(EXTERNAL_APP_BYTES, 22222)
                .putLong(FREE_BYTES_KEY, 1000L)
                .putLong(TOTAL_BYTES_KEY, 6000L)
                .putInt(USER_ID_KEY, 0)
                .putLong(TIMESTAMP_KEY, 10000L)
                .apply();

        final PrivateStorageInfo info = mCachedValuesHelper.getCachedPrivateStorageInfo();

        assertThat(info.freeBytes).isEqualTo(1000L);
        assertThat(info.totalBytes).isEqualTo(6000L);
    }

    @Test
    public void getCachedAppsStorageResult_cachedValuesAreLoaded() {
        when(mMockClock.getCurrentTime()).thenReturn(10001L);
        mSharedPreferences
                .edit()
                .putLong(GAME_APPS_SIZE_KEY, 1)
                .putLong(MUSIC_APPS_SIZE_KEY, 10)
                .putLong(VIDEO_APPS_SIZE_KEY, 100)
                .putLong(PHOTO_APPS_SIZE_KEY, 1000)
                .putLong(OTHER_APPS_SIZE_KEY, 10000)
                .putLong(CACHE_APPS_SIZE_KEY, 100000)
                .putLong(EXTERNAL_TOTAL_BYTES, 222222)
                .putLong(EXTERNAL_AUDIO_BYTES, 22)
                .putLong(EXTERNAL_VIDEO_BYTES, 222)
                .putLong(EXTERNAL_IMAGE_BYTES, 2222)
                .putLong(EXTERNAL_APP_BYTES, 22222)
                .putLong(FREE_BYTES_KEY, 1000L)
                .putLong(TOTAL_BYTES_KEY, 5000L)
                .putInt(USER_ID_KEY, 0)
                .putLong(TIMESTAMP_KEY, 10000L)
                .apply();

        final SparseArray<StorageAsyncLoader.AppsStorageResult> result =
                mCachedValuesHelper.getCachedAppsStorageResult();

        StorageAsyncLoader.AppsStorageResult primaryResult = result.get(0);
        assertThat(primaryResult.gamesSize).isEqualTo(1L);
        assertThat(primaryResult.musicAppsSize).isEqualTo(10L);
        assertThat(primaryResult.videoAppsSize).isEqualTo(100L);
        assertThat(primaryResult.photosAppsSize).isEqualTo(1000L);
        assertThat(primaryResult.otherAppsSize).isEqualTo(10000L);
        assertThat(primaryResult.cacheSize).isEqualTo(100000L);
        assertThat(primaryResult.externalStats.totalBytes).isEqualTo(222222L);
        assertThat(primaryResult.externalStats.audioBytes).isEqualTo(22L);
        assertThat(primaryResult.externalStats.videoBytes).isEqualTo(222L);
        assertThat(primaryResult.externalStats.imageBytes).isEqualTo(2222L);
        assertThat(primaryResult.externalStats.appBytes).isEqualTo(22222L);
    }

    @Test
    public void getCachedPrivateStorageInfo_nullIfDataIsStale() {
        when(mMockClock.getCurrentTime()).thenReturn(10000000L);
        mSharedPreferences
                .edit()
                .putLong(GAME_APPS_SIZE_KEY, 0)
                .putLong(MUSIC_APPS_SIZE_KEY, 10)
                .putLong(VIDEO_APPS_SIZE_KEY, 100)
                .putLong(PHOTO_APPS_SIZE_KEY, 1000)
                .putLong(OTHER_APPS_SIZE_KEY, 10000)
                .putLong(CACHE_APPS_SIZE_KEY, 100000)
                .putLong(EXTERNAL_TOTAL_BYTES, 2)
                .putLong(EXTERNAL_AUDIO_BYTES, 22)
                .putLong(EXTERNAL_VIDEO_BYTES, 222)
                .putLong(EXTERNAL_IMAGE_BYTES, 2222)
                .putLong(EXTERNAL_APP_BYTES, 22222)
                .putLong(FREE_BYTES_KEY, 1000L)
                .putLong(TOTAL_BYTES_KEY, 5000L)
                .putInt(USER_ID_KEY, 0)
                .putLong(TIMESTAMP_KEY, 10000L)
                .apply();

        final PrivateStorageInfo info = mCachedValuesHelper.getCachedPrivateStorageInfo();
        assertThat(info).isNull();
    }

    @Test
    public void getCachedAppsStorageResult_nullIfDataIsStale() {
        when(mMockClock.getCurrentTime()).thenReturn(10000000L);
        mSharedPreferences
                .edit()
                .putLong(GAME_APPS_SIZE_KEY, 0)
                .putLong(MUSIC_APPS_SIZE_KEY, 10)
                .putLong(VIDEO_APPS_SIZE_KEY, 100)
                .putLong(PHOTO_APPS_SIZE_KEY, 1000)
                .putLong(OTHER_APPS_SIZE_KEY, 10000)
                .putLong(CACHE_APPS_SIZE_KEY, 100000)
                .putLong(EXTERNAL_TOTAL_BYTES, 2)
                .putLong(EXTERNAL_AUDIO_BYTES, 22)
                .putLong(EXTERNAL_VIDEO_BYTES, 222)
                .putLong(EXTERNAL_IMAGE_BYTES, 2222)
                .putLong(EXTERNAL_APP_BYTES, 22222)
                .putLong(FREE_BYTES_KEY, 1000L)
                .putLong(TOTAL_BYTES_KEY, 5000L)
                .putInt(USER_ID_KEY, 0)
                .putLong(TIMESTAMP_KEY, 10000L)
                .apply();

        final SparseArray<StorageAsyncLoader.AppsStorageResult> result =
                mCachedValuesHelper.getCachedAppsStorageResult();
        assertThat(result).isNull();
    }

    @Test
    public void getCachedPrivateStorageInfo_nullIfWrongUser() {
        when(mMockClock.getCurrentTime()).thenReturn(10001L);
        mSharedPreferences
                .edit()
                .putLong(GAME_APPS_SIZE_KEY, 0)
                .putLong(MUSIC_APPS_SIZE_KEY, 10)
                .putLong(VIDEO_APPS_SIZE_KEY, 100)
                .putLong(PHOTO_APPS_SIZE_KEY, 1000)
                .putLong(OTHER_APPS_SIZE_KEY, 10000)
                .putLong(CACHE_APPS_SIZE_KEY, 100000)
                .putLong(EXTERNAL_TOTAL_BYTES, 2)
                .putLong(EXTERNAL_AUDIO_BYTES, 22)
                .putLong(EXTERNAL_VIDEO_BYTES, 222)
                .putLong(EXTERNAL_IMAGE_BYTES, 2222)
                .putLong(EXTERNAL_APP_BYTES, 22222)
                .putLong(FREE_BYTES_KEY, 1000L)
                .putLong(TOTAL_BYTES_KEY, 5000L)
                .putInt(USER_ID_KEY, 1)
                .putLong(TIMESTAMP_KEY, 10000L)
                .apply();

        final PrivateStorageInfo info = mCachedValuesHelper.getCachedPrivateStorageInfo();
        assertThat(info).isNull();
    }

    @Test
    public void getCachedAppsStorageResult_nullIfWrongUser() {
        when(mMockClock.getCurrentTime()).thenReturn(10001L);
        mSharedPreferences
                .edit()
                .putLong(GAME_APPS_SIZE_KEY, 0)
                .putLong(MUSIC_APPS_SIZE_KEY, 10)
                .putLong(VIDEO_APPS_SIZE_KEY, 100)
                .putLong(PHOTO_APPS_SIZE_KEY, 1000)
                .putLong(OTHER_APPS_SIZE_KEY, 10000)
                .putLong(CACHE_APPS_SIZE_KEY, 100000)
                .putLong(EXTERNAL_TOTAL_BYTES, 2)
                .putLong(EXTERNAL_AUDIO_BYTES, 22)
                .putLong(EXTERNAL_VIDEO_BYTES, 222)
                .putLong(EXTERNAL_IMAGE_BYTES, 2222)
                .putLong(EXTERNAL_APP_BYTES, 22222)
                .putLong(FREE_BYTES_KEY, 1000L)
                .putLong(TOTAL_BYTES_KEY, 5000L)
                .putInt(USER_ID_KEY, 1)
                .putLong(TIMESTAMP_KEY, 10000L)
                .apply();

        final SparseArray<StorageAsyncLoader.AppsStorageResult> result =
                mCachedValuesHelper.getCachedAppsStorageResult();
        assertThat(result).isNull();
    }

    @Test
    public void getCachedPrivateStorageInfo_nullIfEmpty() {
        final PrivateStorageInfo info = mCachedValuesHelper.getCachedPrivateStorageInfo();
        assertThat(info).isNull();
    }

    @Test
    public void getCachedAppsStorageResult_nullIfEmpty() {
        final SparseArray<StorageAsyncLoader.AppsStorageResult> result =
                mCachedValuesHelper.getCachedAppsStorageResult();
        assertThat(result).isNull();
    }

    @Test
    public void cacheResult_succeeds() {
        when(mMockClock.getCurrentTime()).thenReturn(10000L);
        final StorageStatsSource.ExternalStorageStats externalStats =
                new StorageStatsSource.ExternalStorageStats(22222L, 2L, 20L, 200L, 2000L);
        final StorageAsyncLoader.AppsStorageResult result =
                new StorageAsyncLoader.AppsStorageResult();
        result.gamesSize = 1L;
        result.musicAppsSize = 10L;
        result.videoAppsSize = 100L;
        result.photosAppsSize = 1000L;
        result.otherAppsSize = 10000L;
        result.cacheSize = 100000L;
        result.externalStats = externalStats;
        final PrivateStorageInfo info = new PrivateStorageInfo(1000L, 6000L);

        mCachedValuesHelper.cacheResult(info, result);

        assertThat(mSharedPreferences.getLong(GAME_APPS_SIZE_KEY, -1)).isEqualTo(1L);
        assertThat(mSharedPreferences.getLong(MUSIC_APPS_SIZE_KEY, -1)).isEqualTo(10L);
        assertThat(mSharedPreferences.getLong(VIDEO_APPS_SIZE_KEY, -1)).isEqualTo(100L);
        assertThat(mSharedPreferences.getLong(PHOTO_APPS_SIZE_KEY, -1)).isEqualTo(1000L);
        assertThat(mSharedPreferences.getLong(OTHER_APPS_SIZE_KEY, -1)).isEqualTo(10000L);
        assertThat(mSharedPreferences.getLong(CACHE_APPS_SIZE_KEY, -1)).isEqualTo(100000L);
        assertThat(mSharedPreferences.getLong(EXTERNAL_TOTAL_BYTES, -1)).isEqualTo(22222L);
        assertThat(mSharedPreferences.getLong(EXTERNAL_AUDIO_BYTES, -1)).isEqualTo(2L);
        assertThat(mSharedPreferences.getLong(EXTERNAL_VIDEO_BYTES, -1)).isEqualTo(20L);
        assertThat(mSharedPreferences.getLong(EXTERNAL_IMAGE_BYTES, -1)).isEqualTo(200L);
        assertThat(mSharedPreferences.getLong(EXTERNAL_APP_BYTES, -1)).isEqualTo(2000L);
        assertThat(mSharedPreferences.getLong(FREE_BYTES_KEY, -1)).isEqualTo(1000L);
        assertThat(mSharedPreferences.getLong(TOTAL_BYTES_KEY, -1)).isEqualTo(6000L);
        assertThat(mSharedPreferences.getInt(USER_ID_KEY, -1)).isEqualTo(0);
        assertThat(mSharedPreferences.getLong(TIMESTAMP_KEY, -1)).isEqualTo(10000L);
    }
}
