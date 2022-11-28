/*
 * Copyright (C) 2019 The Android Open Source Project
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RecentAppStatsMixinTest {

    private static final UserHandle NORMAL_USER = UserHandle.SYSTEM;
    private static final UserHandle CLONE_USER = new UserHandle(2222);
    private static final UserHandle WORK_USER = new UserHandle(3333);

    @Mock
    private UsageStatsManager mUsageStatsManager;
    @Mock
    private UsageStatsManager mWorkUsageStatsManager;
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
    private UsageStatsManager mCloneUsageStatsManager;
    @Mock
    Context mMockContext;

    private Context mContext;

    private RecentAppStatsMixin mRecentAppStatsMixin;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        ReflectionHelpers.setStaticField(ApplicationsState.class, "sInstance", mAppState);
        doReturn(mUsageStatsManager).when(mContext).getSystemService(UsageStatsManager.class);
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mPowerManager).when(mContext).getSystemService(PowerManager.class);
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[]{});

        doReturn(mMockContext).when(mContext).createContextAsUser(any(), anyInt());
        doReturn(mMockContext).when(mContext).createPackageContextAsUser(any(), anyInt(), any());
        when(mUserManager.getUserProfiles())
                .thenReturn(new ArrayList<>(Arrays.asList(NORMAL_USER)));

        mRecentAppStatsMixin = new RecentAppStatsMixin(mContext, 3 /* maximumApps */);
    }

    @Test
    public void loadDisplayableRecentApps_oneValidRecentAppSet_shouldHaveOneRecentApp() {
        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();
        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);
        // stat1 is valid app.
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(1);
    }

    @Test
    public void loadDisplayableRecentApps_threeValidRecentAppsSet_shouldHaveThreeRecentApps() {
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
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(3);
    }

    @Test
    public void loadDisplayableRecentApps_oneValidAndTwoInvalidSet_shouldHaveOneRecentApp() {
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
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        // Only stat1. stat2 is skipped because of the package name, stat3 skipped because
        // it's invalid app.
        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(1);
    }

    @Test
    public void loadDisplayableRecentApps_oneInstantAppSet_shouldHaveOneRecentApp() {
        final List<UsageStats> stats = new ArrayList<>();
        // Instant app.
        final UsageStats stat = new UsageStats();
        stat.mLastTimeUsed = System.currentTimeMillis() + 200;
        stat.mPackageName = "com.foo.barinstant";
        stats.add(stat);

        ApplicationsState.AppEntry statEntry = mock(ApplicationsState.AppEntry.class);
        statEntry.info = mApplicationInfo;

        when(mAppState.getEntry(stat.mPackageName, UserHandle.myUserId())).thenReturn(statEntry);
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);

        // Make sure stat is considered an instant app.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (ApplicationInfo info) -> info == statEntry.info);

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(1);
    }

    @Test
    public void loadDisplayableRecentApps_withNullAppEntryOrInfo_shouldNotCrash() {
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
        mRecentAppStatsMixin.loadDisplayableRecentApps(3);
    }

    @Test
    public void loadDisplayableRecentApps_hiddenSystemModuleSet_shouldNotHaveHiddenSystemModule() {
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

        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(1);
    }

    @Test
    public void loadDisplayableRecentApps_powerSaverModeOn_shouldHaveEmptyList() {
        when(mPowerManager.isPowerSaveMode()).thenReturn(true);

        final List<UsageStats> stats = new ArrayList<>();
        final UsageStats stat1 = new UsageStats();

        stat1.mLastTimeUsed = System.currentTimeMillis();
        stat1.mPackageName = "pkg.class";
        stats.add(stat1);

        // stat1, stat2 are valid apps. stat3 is invalid.
        when(mAppState.getEntry(stat1.mPackageName, UserHandle.myUserId()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(stats);
        mAppEntry.info = mApplicationInfo;

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        assertThat(mRecentAppStatsMixin.mRecentApps).isEmpty();
    }

    @Test
    public void loadDisplayableRecentApps_usePersonalAndWorkApps_shouldBeSortedByLastTimeUse() {
        final List<UsageStats> personalStats = new ArrayList<>();
        final UsageStats stats1 = new UsageStats();
        final UsageStats stats2 = new UsageStats();
        stats1.mLastTimeUsed = System.currentTimeMillis();
        stats1.mPackageName = "personal.pkg.class";
        personalStats.add(stats1);

        stats2.mLastTimeUsed = System.currentTimeMillis() - 5000;
        stats2.mPackageName = "personal.pkg.class2";
        personalStats.add(stats2);

        final List<UsageStats> workStats = new ArrayList<>();
        final UsageStats stat3 = new UsageStats();
        stat3.mLastTimeUsed = System.currentTimeMillis() - 2000;
        stat3.mPackageName = "work.pkg.class3";
        workStats.add(stat3);

        when(mAppState.getEntry(anyString(), anyInt()))
                .thenReturn(mAppEntry);
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        when(mUserManager.getUserProfiles())
                .thenReturn(new ArrayList<>(Arrays.asList(NORMAL_USER, WORK_USER)));
        when(mMockContext.getSystemService(UsageStatsManager.class))
                .thenReturn(mWorkUsageStatsManager);
        // personal app stats
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(personalStats);
        // work app stats
        when(mWorkUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(workStats);
        mAppEntry.info = mApplicationInfo;

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(3);
        assertThat(mRecentAppStatsMixin.mRecentApps.get(0).mUsageStats.mPackageName).isEqualTo(
                "personal.pkg.class");
        assertThat(mRecentAppStatsMixin.mRecentApps.get(1).mUsageStats.mPackageName).isEqualTo(
                "work.pkg.class3");
        assertThat(mRecentAppStatsMixin.mRecentApps.get(2).mUsageStats.mPackageName).isEqualTo(
                "personal.pkg.class2");
    }

    @Test
    public void loadDisplayableRecentApps_usePersonalAndWorkApps_shouldBeUniquePerProfile()
            throws PackageManager.NameNotFoundException {
        final String firstAppPackageName = "app1.pkg.class";
        final String secondAppPackageName = "app2.pkg.class";
        final List<UsageStats> personalStats = new ArrayList<>();
        final UsageStats personalStatsFirstApp = new UsageStats();
        final UsageStats personalStatsFirstAppOlderUse = new UsageStats();
        final UsageStats personalStatsSecondApp = new UsageStats();
        personalStatsFirstApp.mLastTimeUsed = System.currentTimeMillis();
        personalStatsFirstApp.mPackageName = firstAppPackageName;
        personalStats.add(personalStatsFirstApp);

        personalStatsFirstAppOlderUse.mLastTimeUsed = System.currentTimeMillis() - 5000;
        personalStatsFirstAppOlderUse.mPackageName = firstAppPackageName;
        personalStats.add(personalStatsFirstAppOlderUse);

        personalStatsSecondApp.mLastTimeUsed = System.currentTimeMillis() - 2000;
        personalStatsSecondApp.mPackageName = secondAppPackageName;
        personalStats.add(personalStatsSecondApp);

        final List<UsageStats> workStats = new ArrayList<>();
        final UsageStats workStatsSecondApp = new UsageStats();
        workStatsSecondApp.mLastTimeUsed = System.currentTimeMillis() - 1000;
        workStatsSecondApp.mPackageName = secondAppPackageName;
        workStats.add(workStatsSecondApp);

        when(mAppState.getEntry(anyString(), anyInt()))
                .thenReturn(mAppEntry);
        when(mUserManager.getUserProfiles())
                .thenReturn(new ArrayList<>(Arrays.asList(NORMAL_USER, WORK_USER)));
        when(mMockContext.getSystemService(UsageStatsManager.class))
                .thenReturn(mWorkUsageStatsManager);
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        // personal app stats
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(personalStats);
        // work app stats
        when(mWorkUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(workStats);
        mAppEntry.info = mApplicationInfo;

        mRecentAppStatsMixin.loadDisplayableRecentApps(3);

        // The output should have the first app once since the duplicate use in the personal profile
        // is filtered out, and the second app twice - once for each profile.
        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(3);
        assertThat(mRecentAppStatsMixin.mRecentApps.get(0).mUsageStats.mPackageName).isEqualTo(
                firstAppPackageName);
        assertThat(mRecentAppStatsMixin.mRecentApps.get(1).mUsageStats.mPackageName).isEqualTo(
                secondAppPackageName);
        assertThat(mRecentAppStatsMixin.mRecentApps.get(2).mUsageStats.mPackageName).isEqualTo(
                secondAppPackageName);
    }

    @Test
    public void loadDisplayableRecentApps_multipleProfileApps_shouldBeSortedByLastTimeUse()
            throws PackageManager.NameNotFoundException {
        final List<UsageStats> personalStats = new ArrayList<>();
        final UsageStats stats1 = new UsageStats();
        final UsageStats stats2 = new UsageStats();
        stats1.mLastTimeUsed = System.currentTimeMillis();
        stats1.mPackageName = "personal.pkg.class";
        personalStats.add(stats1);

        stats2.mLastTimeUsed = System.currentTimeMillis() - 5000;
        stats2.mPackageName = "personal.pkg.class2";
        personalStats.add(stats2);

        final List<UsageStats> workStats = new ArrayList<>();
        final UsageStats stat3 = new UsageStats();
        stat3.mLastTimeUsed = System.currentTimeMillis() - 2000;
        stat3.mPackageName = "work.pkg.class3";
        workStats.add(stat3);

        final List<UsageStats> cloneStats = new ArrayList<>();
        final UsageStats stat4 = new UsageStats();
        stat4.mLastTimeUsed = System.currentTimeMillis() - 1000;
        stat4.mPackageName = "clone.pkg.class4";
        cloneStats.add(stat4);

        when(mAppState.getEntry(anyString(), anyInt()))
                .thenReturn(mAppEntry);
        when(mUserManager.getUserProfiles())
                .thenReturn(new ArrayList<>(Arrays.asList(NORMAL_USER, CLONE_USER, WORK_USER)));
        when(mMockContext.getSystemService(UsageStatsManager.class))
                .thenReturn(mCloneUsageStatsManager, mWorkUsageStatsManager);
        when(mPackageManager.resolveActivityAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(new ResolveInfo());
        // personal app stats
        when(mUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(personalStats);
        // work app stats
        when(mWorkUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(workStats);
        // clone app stats
        when(mCloneUsageStatsManager.queryUsageStats(anyInt(), anyLong(), anyLong()))
                .thenReturn(cloneStats);

        mAppEntry.info = mApplicationInfo;

        mRecentAppStatsMixin.loadDisplayableRecentApps(4);

        assertThat(mRecentAppStatsMixin.mRecentApps.size()).isEqualTo(4);
        assertThat(mRecentAppStatsMixin.mRecentApps.get(0).mUsageStats.mPackageName).isEqualTo(
                "personal.pkg.class");
        assertThat(mRecentAppStatsMixin.mRecentApps.get(1).mUsageStats.mPackageName).isEqualTo(
                "clone.pkg.class4");
        assertThat(mRecentAppStatsMixin.mRecentApps.get(2).mUsageStats.mPackageName).isEqualTo(
                "work.pkg.class3");
        assertThat(mRecentAppStatsMixin.mRecentApps.get(3).mUsageStats.mPackageName).isEqualTo(
                "personal.pkg.class2");
    }
}
