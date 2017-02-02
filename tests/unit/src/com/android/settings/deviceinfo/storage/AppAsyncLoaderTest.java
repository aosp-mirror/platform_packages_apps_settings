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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.settings.applications.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppAsyncLoaderTest {
    @Mock
    private StorageStatsSource mSource;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManagerWrapper mPackageManager;
    ArrayList<ApplicationInfo> mInfo = new ArrayList<>();

    private AppsAsyncLoader mLoader;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mInfo = new ArrayList<>();
        mLoader = new AppsAsyncLoader(mContext, 1, "id", mSource, mPackageManager);
        when(mPackageManager.getInstalledApplicationsAsUser(anyInt(), anyInt())).thenReturn(mInfo);
    }

    @Test
    public void testLoadingApps() throws Exception {
        addPackage(1001, 0, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);
        addPackage(1002, 0, 100, 1000, ApplicationInfo.CATEGORY_UNDEFINED);

        AppsAsyncLoader.AppsStorageResult result = mLoader.loadInBackground();

        assertThat(result.gamesSize).isEqualTo(0L);
        assertThat(result.otherAppsSize).isEqualTo(1111L);
    }

    @Test
    public void testGamesAreFiltered() throws Exception {
        addPackage(1001, 0, 1, 10, ApplicationInfo.CATEGORY_GAME);

        AppsAsyncLoader.AppsStorageResult result = mLoader.loadInBackground();

        assertThat(result.gamesSize).isEqualTo(11L);
        assertThat(result.otherAppsSize).isEqualTo(0);
    }

    @Test
    public void testDuplicateUidsAreSkipped() throws Exception {
        addPackage(1001, 0, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);
        addPackage(1001, 0, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);

        AppsAsyncLoader.AppsStorageResult result = mLoader.loadInBackground();

        assertThat(result.otherAppsSize).isEqualTo(11L);
    }

    @Test
    public void testCacheIsIgnored() throws Exception {
        addPackage(1001, 100, 1, 10, ApplicationInfo.CATEGORY_UNDEFINED);

        AppsAsyncLoader.AppsStorageResult result = mLoader.loadInBackground();

        assertThat(result.otherAppsSize).isEqualTo(11L);
    }

    private void addPackage(int uid, long cacheSize, long codeSize, long dataSize, int category) {
        StorageStatsSource.AppStorageStats storageStats =
                mock(StorageStatsSource.AppStorageStats.class);
        when(storageStats.getCodeBytes()).thenReturn(codeSize);
        when(storageStats.getDataBytes()).thenReturn(dataSize);
        when(mSource.getStatsForUid(anyString(), eq(uid))).thenReturn(storageStats);

        ApplicationInfo info = new ApplicationInfo();
        info.uid = uid;
        info.category = category;
        mInfo.add(info);
    }
}
