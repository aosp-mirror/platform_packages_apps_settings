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

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.testutils.ApplicationTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;

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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
    @Mock
    private SiteMapManager mSiteMapManager;

    private InstalledAppResultLoader mLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        FakeFeatureFactory factory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(factory.searchFeatureProvider.getSiteMapManager())
                .thenReturn(mSiteMapManager);
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getString(R.string.applications_settings))
                .thenReturn("app");
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
                                0 /* targetSdkVersion */),
                        ApplicationTestUtils.buildInfo(0 /* uid */, "appBuffer", 0 /* flags */,
                                0 /* targetSdkVersion */)));
    }

    @Test
    public void query_noMatchingQuery_shouldReturnEmptyResult() {
        final String query = "abc";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void query_matchingQuery_shouldReturnNonSystemApps() {
        final String query = "app";

        mLoader = spy(new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager));
        when(mLoader.getContext()).thenReturn(mContext);
        when(mSiteMapManager.buildBreadCrumb(eq(mContext), anyString(), anyString()))
                .thenReturn(Arrays.asList(new String[]{"123"}));

        assertThat(mLoader.loadInBackground().size()).isEqualTo(3);
        verify(mSiteMapManager)
                .buildBreadCrumb(eq(mContext), anyString(), anyString());
    }

    @Test
    public void query_matchingQuery_shouldReturnSystemAppUpdates() {
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app1", FLAG_UPDATED_SYSTEM_APP,
                                0 /* targetSdkVersion */)));
        final String query = "app";

        mLoader = spy(new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager));
        when(mLoader.getContext()).thenReturn(mContext);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
        verify(mSiteMapManager)
                .buildBreadCrumb(eq(mContext), anyString(), anyString());
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

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_matchingQuery_shouldNotReturnSystemAppIfNotLaunchable() {
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, "app1", FLAG_SYSTEM,
                                0 /* targetSdkVersion */)));
        when(mPackageManagerWrapper.queryIntentActivitiesAsUser(
                any(Intent.class), anyInt(), anyInt()))
                .thenReturn(null);

        final String query = "app";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground()).isEmpty();
        verify(mSiteMapManager, never())
                .buildBreadCrumb(eq(mContext), anyString(), anyString());
    }

    @Test
    public void query_matchingQuery_shouldRankBasedOnSimilarity() {
        final String query = "app";

        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);
        final List<? extends SearchResult> results = mLoader.loadInBackground();

        // List is sorted by rank
        assertThat(results.get(0).rank).isAtMost(results.get(1).rank);
        assertThat(results.get(0).title).isEqualTo("app4");
        assertThat(results.get(1).title).isEqualTo("app");
        assertThat(results.get(2).title).isEqualTo("appBuffer");
    }

    @Test
    public void query_normalWord_MatchPrefix() {
        final String query = "ba";
        final String packageName = "Bananas";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_CapitalCase_DoestMatchSecondWord() {
        final String query = "Apples";
        final String packageName = "BananasApples";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(0);
    }

    @Test
    public void query_TwoWords_MatchesFirstWord() {
        final String query = "Banana";
        final String packageName = "Bananas Apples";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_TwoWords_MatchesSecondWord() {
        final String query = "Apple";
        final String packageName = "Bananas Apples";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_ThreeWords_MatchesThirdWord() {
        final String query = "Pear";
        final String packageName = "Bananas Apples Pears";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_DoubleSpacedWords_MatchesSecondWord() {
        final String query = "Apple";
        final String packageName = "Bananas  Apples";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_SpecialChar_MatchesSecondWord() {
        final String query = "Apple";
        final String packageName = "Bananas & Apples";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_TabSeparated_MatchesSecondWord() {
        final String query = "Apple";
        final String packageName = "Bananas\tApples";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_LeadingNumber_MatchesWord() {
        final String query = "4";
        final String packageName = "4Bananas";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(1);
    }

    @Test
    public void query_FirstWordPrefixOfQuery_NoMatch() {
        final String query = "Bananass";
        final String packageName = "Bananas Apples";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(0);
    }

    @Test
    public void query_QueryLongerThanAppName_NoMatch() {
        final String query = "BananasApples";
        final String packageName = "Bananas";
        when(mPackageManagerWrapper.getInstalledApplicationsAsUser(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(0 /* uid */, packageName, 0 /* flags */,
                                0 /* targetSdkVersion */)));
        mLoader = new InstalledAppResultLoader(mContext, mPackageManagerWrapper, query,
                mSiteMapManager);

        assertThat(mLoader.loadInBackground().size()).isEqualTo(0);
    }
}
