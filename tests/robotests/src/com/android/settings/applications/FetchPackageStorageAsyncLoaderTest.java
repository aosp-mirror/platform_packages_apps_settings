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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.UserHandle;

import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FetchPackageStorageAsyncLoaderTest {

    private static final String PACKAGE_NAME = "com.test.package";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private StorageStatsSource mSource;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void worksForValidPackageNameAndUid() throws Exception {
        AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getCodeBytes()).thenReturn(1L);
        when(stats.getDataBytes()).thenReturn(2L);
        when(stats.getCacheBytes()).thenReturn(3L);
        when(mSource.getStatsForPackage(nullable(String.class), nullable(String.class),
                any(UserHandle.class)))
                .thenReturn(stats);
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = PACKAGE_NAME;

        FetchPackageStorageAsyncLoader task = new FetchPackageStorageAsyncLoader(
                mContext, mSource, info, new UserHandle(0));
        assertThat(task.loadInBackground()).isEqualTo(stats);
    }

    @Test
    public void installerExceptionHandledCleanly() throws Exception {
        when(mSource.getStatsForPackage(anyString(), anyString(), any(UserHandle.class))).
                thenThrow(new IOException("intentional failure"));
        ApplicationInfo info = new ApplicationInfo();
        info.packageName = PACKAGE_NAME;
        FetchPackageStorageAsyncLoader task = new FetchPackageStorageAsyncLoader(
                mContext, mSource, info, new UserHandle(0));

        assertThat(task.loadInBackground()).isNull();
    }
}
