/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.applications.AppsPreferenceController.KEY_ALL_APP_INFO;
import static com.android.settings.applications.AppsPreferenceController.KEY_GENERAL_CATEGORY;
import static com.android.settings.applications.AppsPreferenceController.KEY_RECENT_APPS_CATEGORY;
import static com.android.settings.applications.AppsPreferenceController.KEY_SEE_ALL;
import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.UserHandle;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppsPreferenceControllerTest {

    @Mock
    private ApplicationsState mAppState;
    @Mock
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private Fragment mFragment;
    @Mock
    private PreferenceScreen mScreen;

    private AppsPreferenceController mController;
    private List<UsageStats> mUsageStats;
    private PreferenceCategory mRecentAppsCategory;
    private PreferenceCategory mGeneralCategory;
    private Preference mSeeAllPref;
    private Preference mAllAppsInfoPref;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mAppState);

        mRecentAppsCategory = spy(new PreferenceCategory(context));
        mGeneralCategory = new PreferenceCategory(context);
        mSeeAllPref = new Preference(context);
        mAllAppsInfoPref = new Preference(context);
        when(mScreen.findPreference(KEY_RECENT_APPS_CATEGORY)).thenReturn(mRecentAppsCategory);
        when(mScreen.findPreference(KEY_GENERAL_CATEGORY)).thenReturn(mGeneralCategory);
        when(mScreen.findPreference(KEY_SEE_ALL)).thenReturn(mSeeAllPref);
        when(mScreen.findPreference(KEY_ALL_APP_INFO)).thenReturn(mAllAppsInfoPref);

        mController = spy(new AppsPreferenceController(context));
        mController.setFragment(mFragment);
        mController.mRecentAppsCategory = mRecentAppsCategory;
        mController.mGeneralCategory = mGeneralCategory;
        mController.mSeeAllPref = mSeeAllPref;
        mController.mAllAppsInfoPref = mAllAppsInfoPref;
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAVAILABLE_UNSEARCHABLE() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void displayPreference_noRecentApps_showAllAppsInfo() {
        doNothing().when(mController).loadAllAppsCount();

        mController.displayPreference(mScreen);

        assertThat(mAllAppsInfoPref.isVisible()).isTrue();
        assertThat(mRecentAppsCategory.isVisible()).isFalse();
        assertThat(mGeneralCategory.isVisible()).isFalse();
        assertThat(mSeeAllPref.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_hasRecentApps_showRecentApps() {
        doNothing().when(mController).loadAllAppsCount();
        doReturn(true).when(mRecentAppsCategory).addPreference(any());
        initRecentApps();
        doReturn(mUsageStats).when(mController).loadRecentApps();

        mController.displayPreference(mScreen);

        assertThat(mAllAppsInfoPref.isVisible()).isFalse();
        assertThat(mRecentAppsCategory.isVisible()).isTrue();
        assertThat(mGeneralCategory.isVisible()).isTrue();
        assertThat(mSeeAllPref.isVisible()).isTrue();
    }

    @Test
    public void updateState_shouldRefreshUi() {
        doNothing().when(mController).loadAllAppsCount();

        mController.updateState(mRecentAppsCategory);

        verify(mController).refreshUi();
    }

    private void initRecentApps() {
        mUsageStats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        final UsageStats stat2 = new UsageStats();
        final UsageStats stat3 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        mUsageStats.add(stat1);

        stat2.mLastTimeUsed = System.currentTimeMillis();
        stat2.mPackageName = "pkg.class2";
        mUsageStats.add(stat2);

        stat3.mLastTimeUsed = System.currentTimeMillis();
        stat3.mPackageName = "pkg.class3";
        mUsageStats.add(stat3);
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat3.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        mAppEntry.info = mApplicationInfo;
    }
}
