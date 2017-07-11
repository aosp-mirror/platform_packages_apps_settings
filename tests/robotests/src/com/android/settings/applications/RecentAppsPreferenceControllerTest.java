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

import android.app.Application;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RecentAppsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private Preference mSeeAllPref;
    @Mock
    private PreferenceCategory mDivider;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mMockContext;
    @Mock
    private UsageStatsManager mUsageStatsManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private ApplicationsState mAppState;

    private Context mContext;
    private RecentAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mMockContext.getSystemService(Context.USAGE_STATS_SERVICE))
                .thenReturn(mUsageStatsManager);
        when(mMockContext.getSystemService(Context.USER_SERVICE))
                .thenReturn(mUserManager);

        mContext = RuntimeEnvironment.application;
        mController = new RecentAppsPreferenceController(mContext, mAppState, null);
        when(mScreen.findPreference(anyString())).thenReturn(mCategory);

        when(mScreen.findPreference(RecentAppsPreferenceController.KEY_SEE_ALL))
                .thenReturn(mSeeAllPref);
        when(mScreen.findPreference(RecentAppsPreferenceController.KEY_DIVIDER))
                .thenReturn(mDivider);
        when(mCategory.getContext()).thenReturn(mContext);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void doNotIndexCategory() {
        final List<String> nonIndexable = new ArrayList<>();

        mController.updateNonIndexableKeys(nonIndexable);

        assertThat(nonIndexable).containsAllOf(mController.getPreferenceKey(),
                RecentAppsPreferenceController.KEY_DIVIDER);
    }

    @Test
    public void onDisplayAndUpdateState_shouldRefreshUi() {
        mController = spy(
                new RecentAppsPreferenceController(mMockContext, (Application) null, null));

        doNothing().when(mController).refreshUi(mContext);

        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        verify(mController, times(2)).refreshUi(mContext);
    }

    @Test
    public void display_shouldNotShowRecents_showAppInfoPreference() {
        mController = new RecentAppsPreferenceController(mMockContext, mAppState, null);
        when(mMockContext.getResources().getBoolean(R.bool.config_display_recent_apps))
                .thenReturn(false);

        mController.displayPreference(mScreen);

        verify(mCategory, never()).addPreference(any(Preference.class));
        verify(mCategory).setTitle(null);
        verify(mSeeAllPref).setTitle(R.string.applications_settings);
        verify(mSeeAllPref).setIcon(null);
        verify(mDivider).setVisible(false);
    }

    @Test
    public void display_showRecents() {
        when(mMockContext.getResources().getBoolean(R.bool.config_display_recent_apps))
                .thenReturn(true);
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        final UsageStats stat2 = new UsageStats();
        final UsageStats stat3 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);

        stat2.mLastTimeUsed = System.currentTimeMillis();
        stat2.mPackageName = "com.android.settings";
        stats.add(stat2);

        stat3.mLastTimeUsed = System.currentTimeMillis();
        stat3.mPackageName = "pkg.class2";
        stats.add(stat3);

        // stat1, stat2 are valid apps. stat3 is invalid.
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mock(ApplicationsState.AppEntry.class));
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId()))
                .thenReturn(mock(ApplicationsState.AppEntry.class));
        when(mAppState.getEntry(stat3.mPackageName, UserHandle.myUserId()))
                .thenReturn(null);
        when(mMockContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        final Configuration configuration = new Configuration();
        configuration.locale = Locale.US;
        when(mMockContext.getResources().getConfiguration()).thenReturn(configuration);

        mController = new RecentAppsPreferenceController(mMockContext, mAppState, null);
        mController.displayPreference(mScreen);

        verify(mCategory).setTitle(R.string.recent_app_category_title);
        // Only add stat1. stat2 is skipped because of the package name, stat3 skipped because
        // it's invalid app.
        verify(mCategory, times(1)).addPreference(any(Preference.class));

        verify(mSeeAllPref).setSummary(null);
        verify(mSeeAllPref).setIcon(R.drawable.ic_chevron_right_24dp);
        verify(mDivider).setVisible(true);
    }

    @Test
    public void display_hasRecentButNoneDisplayable_showAppInfo() {
        when(mMockContext.getResources().getBoolean(R.bool.config_display_recent_apps))
                .thenReturn(true);
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        final UsageStats stat2 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "com.android.phone";
        stats.add(stat1);

        stat2.mLastTimeUsed = System.currentTimeMillis();
        stat2.mPackageName = "com.android.settings";
        stats.add(stat2);

        // stat1, stat2 are not displayable
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mock(ApplicationsState.AppEntry.class));
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId()))
                .thenReturn(mock(ApplicationsState.AppEntry.class));
        when(mMockContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);

        mController = new RecentAppsPreferenceController(mMockContext, mAppState, null);
        mController.displayPreference(mScreen);

        verify(mCategory, never()).addPreference(any(Preference.class));
        verify(mCategory).setTitle(null);
        verify(mSeeAllPref).setTitle(R.string.applications_settings);
        verify(mSeeAllPref).setIcon(null);
    }

    @Test
    public void display_showRecents_formatSummary() {
        when(mMockContext.getResources().getBoolean(R.bool.config_display_recent_apps))
            .thenReturn(true);
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);

        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
            .thenReturn(mock(ApplicationsState.AppEntry.class));
        when(mMockContext.getPackageManager().resolveActivity(any(Intent.class), anyInt()))
            .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
            .thenReturn(stats);

        when(mMockContext.getResources().getText(eq(R.string.recent_app_summary)))
            .thenReturn(mContext.getResources().getText(R.string.recent_app_summary));
        final Configuration configuration = new Configuration();
        configuration.locale = Locale.US;
        when(mMockContext.getResources().getConfiguration()).thenReturn(configuration);

        mController = new RecentAppsPreferenceController(mMockContext, mAppState, null);
        mController.displayPreference(mScreen);

        verify(mCategory).addPreference(argThat(summaryMatches("0m ago")));
    }

    private static ArgumentMatcher<Preference> summaryMatches(String expected) {
        return preference -> TextUtils.equals(expected, preference.getSummary());
    }

}
