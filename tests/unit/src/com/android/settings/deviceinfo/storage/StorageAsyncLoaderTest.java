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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.DataUnit;
import android.util.SparseArray;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;
import com.android.settingslib.applications.StorageStatsSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class StorageAsyncLoaderTest {
    private static final int PRIMARY_USER_ID = 0;
    private static final int SECONDARY_USER_ID = 10;
    private static final String PACKAGE_NAME_1 = "com.blah.test";
    private static final String PACKAGE_NAME_2 = "com.blah.test2";
    private static final String PACKAGE_NAME_3 = "com.blah.test3";
    private static final long DEFAULT_QUOTA = DataUnit.MEBIBYTES.toBytes(64);

    @Mock
    private StorageStatsSource mSource;
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private UserManager mUserManager;
    private List<ApplicationInfo> mInfo = new ArrayList<>();
    private List<UserInfo> mUsers;

    private StorageAsyncLoader mLoader;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mInfo = new ArrayList<>();
        mLoader = new StorageAsyncLoader(mContext, mUserManager, "id", mSource, mPackageManager);
        when(mPackageManager.getInstalledApplicationsAsUser(eq(PRIMARY_USER_ID), anyInt()))
                .thenReturn(mInfo);
        UserInfo info = new UserInfo();
        mUsers = new ArrayList<>();
        mUsers.add(info);
        when(mUserManager.getUsers()).thenReturn(mUsers);
        when(mSource.getCacheQuotaBytes(anyString(), anyInt())).thenReturn(DEFAULT_QUOTA);
        final Resources resources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(resources);
        doReturn("content://com.android.providers.media.documents/root/videos_root")
                .when(resources).getString(R.string.config_videos_storage_category_uri);
    }

    @Test
    public void testLoadingApps() throws Exception {
        addPackage(PACKAGE_NAME_1, 0, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);
        addPackage(PACKAGE_NAME_2, 0, 100, 1000, ApplicationInfo.CATEGORY_UNDEFINED);

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(PRIMARY_USER_ID).gamesSize).isEqualTo(0L);
        assertThat(result.get(PRIMARY_USER_ID).allAppsExceptGamesSize).isEqualTo(1111L);
    }

    @Test
    public void testGamesAreFiltered() throws Exception {
        addPackage(PACKAGE_NAME_1, 0, 1, 10, ApplicationInfo.CATEGORY_GAME);

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(PRIMARY_USER_ID).gamesSize).isEqualTo(11L);
        assertThat(result.get(PRIMARY_USER_ID).allAppsExceptGamesSize).isEqualTo(0);
    }

    @Test
    public void testLegacyGamesAreFiltered() throws Exception {
        ApplicationInfo info =
                addPackage(PACKAGE_NAME_1, 0, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);
        info.flags = ApplicationInfo.FLAG_IS_GAME;

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(PRIMARY_USER_ID).gamesSize).isEqualTo(11L);
        assertThat(result.get(PRIMARY_USER_ID).allAppsExceptGamesSize).isEqualTo(0);
    }

    @Test
    public void testCacheIsNotIgnored() throws Exception {
        addPackage(PACKAGE_NAME_1, 100, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(PRIMARY_USER_ID).allAppsExceptGamesSize).isEqualTo(111L);
    }

    @Test
    public void testMultipleUsers() throws Exception {
        UserInfo info = new UserInfo();
        info.id = SECONDARY_USER_ID;
        mUsers.add(info);
        when(mSource.getExternalStorageStats(anyString(), eq(UserHandle.SYSTEM)))
                .thenReturn(new StorageStatsSource.ExternalStorageStats(9, 2, 3, 4, 0));
        when(mSource.getExternalStorageStats(anyString(), eq(new UserHandle(SECONDARY_USER_ID))))
                .thenReturn(new StorageStatsSource.ExternalStorageStats(10, 3, 3, 4, 0));

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(PRIMARY_USER_ID).externalStats.totalBytes).isEqualTo(9L);
        assertThat(result.get(SECONDARY_USER_ID).externalStats.totalBytes).isEqualTo(10L);
    }

    @Test
    public void testUpdatedSystemAppCodeSizeIsCounted() throws Exception {
        ApplicationInfo systemApp =
                addPackage(PACKAGE_NAME_1, 100, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);
        systemApp.flags = ApplicationInfo.FLAG_SYSTEM & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(PRIMARY_USER_ID).allAppsExceptGamesSize).isEqualTo(111L);
    }

    @Test
    public void testRemovedPackageDoesNotCrash() throws Exception {
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = PACKAGE_NAME_1;
        info.category = ApplicationInfo.CATEGORY_UNDEFINED;
        mInfo.add(info);
        when(mSource.getStatsForPackage(anyString(), anyString(), any(UserHandle.class)))
                .thenThrow(new NameNotFoundException());

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        // Should not crash.
    }

    @Test
    public void testCacheOveragesAreCountedAsFree() throws Exception {
        addPackage(PACKAGE_NAME_1, DEFAULT_QUOTA + 100, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(PRIMARY_USER_ID).allAppsExceptGamesSize)
                .isEqualTo(DEFAULT_QUOTA + 11);
    }

    @Test
    public void testAppsAreFiltered() throws Exception {
        addPackage(PACKAGE_NAME_1, 0, 1, 10, ApplicationInfo.CATEGORY_IMAGE);
        addPackage(PACKAGE_NAME_2, 0, 1, 10, ApplicationInfo.CATEGORY_VIDEO);
        addPackage(PACKAGE_NAME_3, 0, 1, 10, ApplicationInfo.CATEGORY_AUDIO);

        SparseArray<StorageAsyncLoader.StorageResult> result = mLoader.loadInBackground();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(PRIMARY_USER_ID).allAppsExceptGamesSize).isEqualTo(33L);
    }

    private ApplicationInfo addPackage(String packageName, long cacheSize, long codeSize,
            long dataSize, int category) throws Exception {
        StorageStatsSource.AppStorageStats storageStats =
                mock(StorageStatsSource.AppStorageStats.class);
        when(storageStats.getCodeBytes()).thenReturn(codeSize);
        when(storageStats.getDataBytes()).thenReturn(dataSize + cacheSize);
        when(storageStats.getCacheBytes()).thenReturn(cacheSize);
        when(mSource.getStatsForPackage(anyString(), eq(packageName), any(UserHandle.class)))
                .thenReturn(storageStats);

        ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        info.category = category;
        mInfo.add(info);
        return info;
    }

}
