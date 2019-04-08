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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.widget.AppEntitiesHeaderController;
import com.android.settingslib.widget.AppEntityInfo;
import com.android.settingslib.widget.LayoutPreference;

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
public class RecentAppsPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private ApplicationsState mAppState;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private Fragment mFragment;

    private RecentAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);
        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mAppState);
        doReturn(mUserManager).when(context).getSystemService(Context.USER_SERVICE);
        doReturn(mPackageManager).when(context).getPackageManager();
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});

        final View appEntitiesHeaderView = LayoutInflater.from(context).inflate(
                R.layout.app_entities_header, null /* root */);
        final Preference dividerPreference = new Preference(context);
        final LayoutPreference recentAppsPreference =
                spy(new LayoutPreference(context, appEntitiesHeaderView));

        mController = spy(new RecentAppsPreferenceController(context, "test_key"));
        mController.setFragment(mFragment);

        mController.mAppEntitiesController = mock(AppEntitiesHeaderController.class);
        mController.mRecentAppsPreference = recentAppsPreference;
        mController.mDivider = dividerPreference;

        when(mScreen.findPreference(RecentAppsPreferenceController.KEY_DIVIDER))
                .thenReturn(dividerPreference);
        when(mScreen.findPreference("test_key")).thenReturn(recentAppsPreference);
        when(recentAppsPreference.findViewById(R.id.app_entities_header)).thenReturn(
                appEntitiesHeaderView);
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAVAILABLE_UNSEARCHABLE() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void displayPreference_shouldSetupAppEntitiesHeaderController() {
        mController.displayPreference(mScreen);

        assertThat(mController.mAppEntitiesController).isNotNull();
    }

    @Test
    public void onReloadDataCompleted_threeValidRecentOpenAppsSet_setAppEntityThreeTime() {
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        final UsageStats stat2 = new UsageStats();
        final UsageStats stat3 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);

        stat2.mLastTimeUsed = System.currentTimeMillis();
        stat2.mPackageName = "pkg.class2";
        stats.add(stat2);

        stat3.mLastTimeUsed = System.currentTimeMillis();
        stat3.mPackageName = "pkg.class3";
        stats.add(stat3);
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat3.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        mAppEntry.info = mApplicationInfo;

        mController.onReloadDataCompleted(stats);

        verify(mController.mAppEntitiesController, times(3))
                .setAppEntity(anyInt(), any(AppEntityInfo.class));
        assertThat(mController.mRecentAppsPreference.isVisible()).isTrue();
        assertThat(mController.mDivider.isVisible()).isTrue();
    }

    @Test
    public void onReloadDataCompleted_noRecentOpenAppsSet_shouldHideRecentAppPreference() {
        final List<UsageStats> stats = new ArrayList<>();

        mController.onReloadDataCompleted(stats);

        assertThat(mController.mRecentAppsPreference.isVisible()).isFalse();
        assertThat(mController.mDivider.isVisible()).isFalse();
    }
}
