/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserManager;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.testutils.ApplicationTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InstalledAppResultLoaderTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PackageManagerWrapper mPackageManagerWrapper;
    @Mock
    private UserManager mUserManager;

    private InstalledAppResultLoader mLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app1", FLAG_SYSTEM,
                                0 /* targetSdkVersion */),
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app2", FLAG_SYSTEM,
                                0 /* targetSdkVersion */),
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app3", FLAG_SYSTEM,
                                0 /* targetSdkVersion */),
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app4", 0 /* flags */,
                                0 /* targetSdkVersion */),
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app", 0 /* flags */,
                                0 /* targetSdkVersion */)));
    }

    @Test
    public void query_noMatchingQuery_shouldReturnEmptyResult() {
        final String query = "abc";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query);

        assertThat(mLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void query_matchingQuery_shouldReturnNonSystemApps() {
        final String query = "app";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(2);
    }

    @Test
    public void query_matchingQuery_shouldReturnSystemAppUpdates() {
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app1", FLAG_UPDATED_SYSTEM_APP,
                                0 /* targetSdkVersion */)));
        final String query = "app";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_matchingQuery_shouldReturnSystemAppIfLaunchable() {
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app1", FLAG_SYSTEM,
                                0 /* targetSdkVersion */)));
        final List<ResolveInfo> list = mock(List.class);
        when(list.size()).thenReturn(1);
        when(mPackageManagerWrapper.queryIntentActivitiesAsUser(
                any(Intent.class), anyInt(), anyInt()))
                .thenReturn(list);

        final String query = "app";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_matchingQuery_shouldNOtReturnSystemAppIfNotLaunchable() {
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app1", FLAG_SYSTEM,
                                0 /* targetSdkVersion */)));
        when(mPackageManagerWrapper.queryIntentActivitiesAsUser(
                any(Intent.class), anyInt(), anyInt()))
                .thenReturn(null);

        final String query = "app";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query);

        assertThat(mLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void query_matchingQuery_shouldRankBasedOnSimilarity() {
        final String query = "app";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query);
        final List<SearchResult> results = mLoader.loadInBackground();

        // List is sorted by rank
        assertThat(results.get(0).rank).isLessThan(results.get(1).rank);
        // perfect match first
        assertThat(results.get(0).title).isEqualTo(query);
        // Then partial match
        assertThat(results.get(1).title).isNotEqualTo(query);
    }
}
