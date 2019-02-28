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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.widget.AppEntitiesHeaderController;
import com.android.settingslib.widget.AppEntityInfo;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
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
    private UsageStatsManager mUsageStatsManager;
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
    private PowerManager mPowerManager;
    @Mock
    private Fragment mFragment;

    private LayoutPreference mRecentAppsPreference;
    private RecentAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);
        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mAppState);
        doReturn(mUsageStatsManager).when(context).getSystemService(Context.USAGE_STATS_SERVICE);
        doReturn(mUserManager).when(context).getSystemService(Context.USER_SERVICE);
        doReturn(mPackageManager).when(context).getPackageManager();
        doReturn(mPowerManager).when(context).getSystemService(PowerManager.class);
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});

        final View appEntitiesHeaderView = LayoutInflater.from(context).inflate(
                R.layout.app_entities_header, null /* root */);
        final Preference dividerPreference = new Preference(context);
        mRecentAppsPreference = spy(new LayoutPreference(context, appEntitiesHeaderView));

        mController = spy(new RecentAppsPreferenceController(context, "test_key"));
        mController.setFragment(mFragment);
        mController.mAppEntitiesController = mock(AppEntitiesHeaderController.class);
        mController.mRecentAppsPreference = mRecentAppsPreference;
        mController.mDivider = dividerPreference;

        when(mScreen.findPreference(RecentAppsPreferenceController.KEY_DIVIDER))
                .thenReturn(dividerPreference);
        when(mScreen.findPreference("test_key")).thenReturn(mRecentAppsPreference);
        when(mRecentAppsPreference.findViewById(R.id.app_entities_header)).thenReturn(
                appEntitiesHeaderView);
    }

    @Test
    public void getAvailabilityStatus_hasRecentApps_shouldReturnAvailable() {
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);
        // stat1 is valid app.
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;
        mController.reloadData();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noRecentApps_shouldReturnConditionallyUnavailable() {
        // No data
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_powerSaverModeOn_shouldReturnConditionallyUnavailable() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(true);

        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();

        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);

        // stat1, stat2 are valid apps. stat3 is invalid.
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;
        mController.reloadData();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_shouldNotReloadData() {
        mController.displayPreference(mScreen);

        verify(mController, never()).reloadData();
    }

    @Test
    public void displayPreference_shouldRefreshUi() {
        mController.displayPreference(mScreen);

        verify(mController).refreshUi();
    }

    @Test
    public void displayPreference_shouldSetupAppEntitiesHeaderController() {
        mController.displayPreference(mScreen);

        assertThat(mController.mAppEntitiesController).isNotNull();
    }

    @Test
    public void updateState_threeValidRecentOpenAppsSet_setAppEntityThreeTime() {
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

        // stat1, stat2 are valid apps. stat3 is invalid.
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat3.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;
        mController.reloadData();

        mController.updateState(mRecentAppsPreference);

        verify(mController.mAppEntitiesController, times(3))
                .setAppEntity(anyInt(), any(AppEntityInfo.class));
        assertThat(mController.mRecentAppsPreference.isVisible()).isTrue();
        assertThat(mController.mDivider.isVisible()).isTrue();
    }

    @Test
    public void updateState_oneValidRecentOpenAppSet_setAppEntityOneTime() {
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
        stat3.mPackageName = "pkg.class3";
        stats.add(stat3);

        // stat1, stat2 are valid apps. stat3 is invalid.
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat3.mPackageName, UserHandle.myUserId()))
                .thenReturn(null);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;
        mController.reloadData();

        mController.updateState(mRecentAppsPreference);

        // Only add stat1. stat2 is skipped because of the package name, stat3 skipped because
        // it's invalid app.
        verify(mController.mAppEntitiesController, times(1))
                .setAppEntity(anyInt(), any(AppEntityInfo.class));
        assertThat(mController.mRecentAppsPreference.isVisible()).isTrue();
        assertThat(mController.mDivider.isVisible()).isTrue();
    }

    @Test
    public void updateState_instantAppSet_shouldSetAppEntityForInstantApp() {
        // Regular app.
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "com.foo.bar";
        stats.add(stat1);

        // Instant app.
        final UsageStats stat2 = new UsageStats();
        stat2.mLastTimeUsed = System.currentTimeMillis() + 200;
        stat2.mPackageName = "com.foo.barinstant";
        stats.add(stat2);

        ApplicationsState.AppEntry stat1Entry = mock(ApplicationsState.AppEntry.class);
        ApplicationsState.AppEntry stat2Entry = mock(ApplicationsState.AppEntry.class);
        stat1Entry.info = mApplicationInfo;
        stat2Entry.info = mApplicationInfo;

        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId())).thenReturn(stat1Entry);
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId())).thenReturn(stat2Entry);

        // Only the regular app stat1 should have its intent resolve.
        when(mPackageManager.resolveActivity(argThat(intentMatcher(stat1.mPackageName)), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);

        // Make sure stat2 is considered an instant app.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (ApplicationInfo info) -> info == stat2Entry.info);
        mController.reloadData();

        mController.updateState(mRecentAppsPreference);

        verify(mController.mAppEntitiesController, times(2))
                .setAppEntity(anyInt(), any(AppEntityInfo.class));
    }

    @Test
    public void updateState_withNullAppEntryOrInfo_shouldNotCrash() {
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        final UsageStats stat2 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);

        stat2.mLastTimeUsed = System.currentTimeMillis();
        stat2.mPackageName = "pkg.class2";
        stats.add(stat2);

        // app1 has AppEntry with null info, app2 has null AppEntry.
        mAppEntry.info = null;
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId()))
                .thenReturn(null);

        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);

        // We should not crash here.
        mController.updateState(mRecentAppsPreference);
    }

    @Test
    public void updateState_hiddenSystemModuleSet_shouldNotShowHiddenSystemModule() {
        final List<UsageStats> stats = new ArrayList<>();
        // Regular app.
        final UsageStats stat1 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "com.foo.bar";
        stats.add(stat1);

        // Hidden system module.
        final UsageStats stat2 = new UsageStats();
        stat2.mLastTimeUsed = System.currentTimeMillis() + 200;
        stat2.mPackageName = "com.foo.hidden";
        stats.add(stat2);

        ApplicationsState.AppEntry stat1Entry = mock(ApplicationsState.AppEntry.class);
        ApplicationsState.AppEntry stat2Entry = mock(ApplicationsState.AppEntry.class);
        stat1Entry.info = mApplicationInfo;
        stat2Entry.info = mApplicationInfo;

        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId())).thenReturn(stat1Entry);
        when(mAppState.getEntry(stat2.mPackageName, UserHandle.myUserId())).thenReturn(stat2Entry);

        final ModuleInfo moduleInfo1 = new ModuleInfo();
        moduleInfo1.setPackageName(stat1.mPackageName);
        moduleInfo1.setHidden(false);

        final ModuleInfo moduleInfo2 = new ModuleInfo();
        moduleInfo2.setPackageName(stat2.mPackageName);
        moduleInfo2.setHidden(true);

        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", null);
        final List<ModuleInfo> modules = new ArrayList<>();
        modules.add(moduleInfo2);
        when(mPackageManager.getInstalledModules(anyInt() /* flags */))
                .thenReturn(modules);

        when(mPackageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mController.reloadData();

        mController.updateState(mRecentAppsPreference);

        // Only add stat1. stat2 is skipped because it is hidden module.
        verify(mController.mAppEntitiesController).setAppEntity(anyInt(), any(AppEntityInfo.class));
    }

    // Used for matching an intent with a specific package name.
    private static ArgumentMatcher<Intent> intentMatcher(String packageName) {
        return intent -> packageName.equals(intent.getPackage());
    }
}
