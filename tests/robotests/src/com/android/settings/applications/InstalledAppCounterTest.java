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

package com.android.settings.applications;

import static com.android.settings.testutils.ApplicationTestUtils.buildInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.FakeFeatureFlagsImpl;
import android.content.pm.FeatureFlags;
import android.content.pm.Flags;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.ApplicationInfoFlags;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.flag.junit.SetFlagsRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public final class InstalledAppCounterTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String APP_1 = "app1";
    private static final String APP_2 = "app2";
    private static final String APP_3 = "app3";
    private static final String APP_4 = "app4";
    private static final String APP_5 = "app5";
    private static final String APP_6 = "app6";
    private static final String APP_7 = "app7";

    private final int MAIN_USER_ID = 0;
    private final int MANAGED_PROFILE_ID = 10;

    private final int PER_USER_UID_RANGE = 100000;
    private final int MAIN_USER_APP_UID = MAIN_USER_ID * PER_USER_UID_RANGE;
    private final int MANAGED_PROFILE_APP_UID = MANAGED_PROFILE_ID * PER_USER_UID_RANGE;

    @Mock
    private UserManager mUserManager;
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private int mInstalledAppCount = -1;
    private ApplicationInfo mApp1;
    private ApplicationInfo mApp2;
    private ApplicationInfo mApp3;
    private ApplicationInfo mApp4;
    private ApplicationInfo mApp5;
    private ApplicationInfo mApp6;
    private ApplicationInfo mApp7;

    private FakeFeatureFlagsImpl mFakeFeatureFlags;

    @Before
    public void setUp() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        mFakeFeatureFlags = new FakeFeatureFlagsImpl();
        mFakeFeatureFlags.setFlag(Flags.FLAG_ARCHIVING, true);

        mApp1 = buildInfo(MAIN_USER_APP_UID, APP_1,
                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP, 0 /* targetSdkVersion */);
        mApp2 = buildInfo(MAIN_USER_APP_UID, APP_2, 0 /* flags */,
                0 /* targetSdkVersion */);
        mApp3 = buildInfo(MAIN_USER_APP_UID, APP_3, ApplicationInfo.FLAG_SYSTEM,
                0 /* targetSdkVersion */);
        mApp4 = buildInfo(MAIN_USER_APP_UID, APP_4, ApplicationInfo.FLAG_SYSTEM,
                0 /* targetSdkVersion */);
        mApp5 = buildInfo(MANAGED_PROFILE_APP_UID, APP_5, 0 /* flags */,
                0 /* targetSdkVersion */);
        mApp6 = buildInfo(MANAGED_PROFILE_APP_UID, APP_6, ApplicationInfo.FLAG_SYSTEM,
                0 /* targetSdkVersion */);
        mApp7 = buildInfo(MAIN_USER_APP_UID, APP_7, 0 /* flags */,
                0 /* targetSdkVersion */);
        mApp7.isArchived = true;
    }

    private void expectQueryIntentActivities(int userId, String packageName, boolean launchable) {
        when(mPackageManager.queryIntentActivitiesAsUser(
                argThat(isLaunchIntentFor(packageName)),
                eq(PackageManager.GET_DISABLED_COMPONENTS | PackageManager.MATCH_DIRECT_BOOT_AWARE
                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE),
                eq(userId))).thenReturn(launchable
                        ? Collections.singletonList(new ResolveInfo())
                        : new ArrayList<>());
    }

    private void testCountInstalledAppsAcrossAllUsers(boolean async) {
        // There are two users.
        when(mUserManager.getProfiles(UserHandle.myUserId())).thenReturn(Arrays.asList(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN),
                new UserInfo(MANAGED_PROFILE_ID, "managed profile", 0)));
        configurePackageManager();

        // Count the number of all apps installed, irrespective of install reason.
        count(InstalledAppCounter.IGNORE_INSTALL_REASON, async);
        assertThat(mInstalledAppCount).isEqualTo(5);

        // Verify that installed packages were retrieved the current user and the user's managed
        // profile only.
        verify(mPackageManager)
                .getInstalledApplicationsAsUser(
                        any(ApplicationInfoFlags.class),
                        eq(MAIN_USER_ID));
        verify(mPackageManager)
                .getInstalledApplicationsAsUser(
                        any(ApplicationInfoFlags.class),
                        eq(MANAGED_PROFILE_ID));
        verify(mPackageManager, atLeast(0))
            .queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt());
        verifyNoMoreInteractions(mPackageManager);

        // Count once more, considering apps installed by enterprise policy only.
        count(PackageManager.INSTALL_REASON_POLICY, async);
        assertThat(mInstalledAppCount).isEqualTo(3);
    }

    @Test
    public void testIncludeInCount() {
        configurePackageManager();
        assertThat(InstalledAppCounter.includeInCount(InstalledAppCounter.IGNORE_INSTALL_REASON,
                mPackageManager, mApp1)).isTrue();
        assertThat(InstalledAppCounter.includeInCount(InstalledAppCounter.IGNORE_INSTALL_REASON,
                mPackageManager, mApp2)).isTrue();
        assertThat(InstalledAppCounter.includeInCount(InstalledAppCounter.IGNORE_INSTALL_REASON,
                mPackageManager, mApp3)).isTrue();
        assertThat(InstalledAppCounter.includeInCount(InstalledAppCounter.IGNORE_INSTALL_REASON,
                mPackageManager, mApp4)).isFalse();
        assertThat(InstalledAppCounter.includeInCount(InstalledAppCounter.IGNORE_INSTALL_REASON,
                mPackageManager, mApp5)).isTrue();
        assertThat(InstalledAppCounter.includeInCount(InstalledAppCounter.IGNORE_INSTALL_REASON,
                mPackageManager, mApp6)).isTrue();

        assertThat(InstalledAppCounter.includeInCount(PackageManager.INSTALL_REASON_POLICY,
                mPackageManager, mApp1)).isTrue();
        assertThat(InstalledAppCounter.includeInCount(PackageManager.INSTALL_REASON_POLICY,
                mPackageManager, mApp2)).isFalse();
        assertThat(InstalledAppCounter.includeInCount(PackageManager.INSTALL_REASON_POLICY,
                mPackageManager, mApp3)).isTrue();
        assertThat(InstalledAppCounter.includeInCount(PackageManager.INSTALL_REASON_POLICY,
                mPackageManager, mApp4)).isFalse();
        assertThat(InstalledAppCounter.includeInCount(PackageManager.INSTALL_REASON_POLICY,
                mPackageManager, mApp5)).isTrue();
        assertThat(InstalledAppCounter.includeInCount(PackageManager.INSTALL_REASON_POLICY,
                mPackageManager, mApp6)).isFalse();
    }

    @Ignore("b/313578776")
    @Test
    public void testCountInstalledAppsAcrossAllUsersSync() {
        testCountInstalledAppsAcrossAllUsers(false /* async */);
    }

    @Ignore("b/313578776")
    @Test
    public void testCountInstalledAppsAcrossAllUsersAsync() {
        testCountInstalledAppsAcrossAllUsers(true /* async */);
    }

    @Test
    public void testCountInstalledApps_archivingDisabled() {
        when(mUserManager.getProfiles(UserHandle.myUserId())).thenReturn(List.of(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN)));
        // The user has four apps installed:
        // * app2 is a user-installed app. It should be counted.
        // * app7 is a user-archived app. It should not be counted.
        when(mPackageManager.getInstalledApplicationsAsUser(
                argThat(isApplicationInfoFlagsEqualTo(
                        ApplicationInfoFlags.of(
                                PackageManager.GET_DISABLED_COMPONENTS
                                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                                        | PackageManager.MATCH_ANY_USER))),
                eq(MAIN_USER_ID))).thenReturn(Arrays.asList(mApp2));

        mFakeFeatureFlags.setFlag(Flags.FLAG_ARCHIVING, false);
        mSetFlagsRule.disableFlags(com.android.settings.flags.Flags.FLAG_APP_ARCHIVING);
        // Count the number of all apps installed, irrespective of install reason.
        count(InstalledAppCounter.IGNORE_INSTALL_REASON, mFakeFeatureFlags);
        assertThat(mInstalledAppCount).isEqualTo(1);
    }

    @Test
    public void testCountInstalledApps_archivingEnabled() {
        when(mUserManager.getProfiles(UserHandle.myUserId())).thenReturn(List.of(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN)));
        // The user has four apps installed:
        // * app2 is a user-installed app. It should be counted.
        // * app7 is a user-archived app. It should be counted.
        when(mPackageManager.getInstalledApplicationsAsUser(
                argThat(isApplicationInfoFlagsEqualTo(
                        ApplicationInfoFlags.of(
                                PackageManager.GET_DISABLED_COMPONENTS
                                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                                        | PackageManager.MATCH_ANY_USER
                                        | PackageManager.MATCH_ARCHIVED_PACKAGES))),
                eq(MAIN_USER_ID))).thenReturn(Arrays.asList(mApp2, mApp7));

        // Count the number of all apps installed, irrespective of install reason.
        count(InstalledAppCounter.IGNORE_INSTALL_REASON, mFakeFeatureFlags);
        assertThat(mInstalledAppCount).isEqualTo(2);
    }

    private void count(int installReason, boolean async) {
        mInstalledAppCount = -1;
        final InstalledAppCounterTestable counter = new InstalledAppCounterTestable(installReason);
        if (async) {
            counter.execute();
            // Wait for the background task to finish.
            ShadowApplication.runBackgroundTasks();
        } else {
            counter.executeInForeground();
        }
    }

    private void count(int installReason, FeatureFlags featureFlags) {
        mInstalledAppCount = -1;
        final InstalledAppCounterTestable counter =
                new InstalledAppCounterTestable(installReason, featureFlags);
        counter.executeInForeground();
    }

    private void configurePackageManager() {
        // The first user has four apps installed:
        // * app1 is an updated system app. It should be counted.
        // * app2 is a user-installed app. It should be counted.
        // * app3 is a system app that provides a launcher icon. It should be counted.
        // * app4 is a system app that provides no launcher icon. It should not be counted.
        ApplicationInfoFlags infoFlags1 = ApplicationInfoFlags.of(
                PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_ANY_USER);
        when(mPackageManager.getInstalledApplicationsAsUser(
                argThat(isApplicationInfoFlagsEqualTo(infoFlags1)),
                eq(MAIN_USER_ID))
        ).thenReturn(Arrays.asList(mApp1, mApp2, mApp3, mApp4));
        // For system apps, InstalledAppCounter checks whether they handle the default launcher
        // intent to decide whether to include them in the count of installed apps or not.
        expectQueryIntentActivities(MAIN_USER_ID, APP_3, true /* launchable */);
        expectQueryIntentActivities(MAIN_USER_ID, APP_4, false /* launchable */);

        // app1, app3 and app4 are installed by enterprise policy.
        final UserHandle mainUser = new UserHandle(MAIN_USER_ID);
        when(mPackageManager.getInstallReason(APP_1, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);
        when(mPackageManager.getInstallReason(APP_2, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_3, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);
        when(mPackageManager.getInstallReason(APP_4, mainUser))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);

        // The second user has two apps installed:
        // * app5 is a user-installed app. It should be counted.
        // * app6 is a system app that provides a launcher icon. It should be counted.
        ApplicationInfoFlags infoFlags2 = ApplicationInfoFlags.of(
                PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS);
        when(mPackageManager.getInstalledApplicationsAsUser(
                argThat(isApplicationInfoFlagsEqualTo(infoFlags2)), eq(MANAGED_PROFILE_ID))
        ).thenReturn(Arrays.asList(mApp5, mApp6));
        expectQueryIntentActivities(MANAGED_PROFILE_ID, APP_6, true /* launchable */);

        // app5 is installed by enterprise policy.
        final UserHandle managedProfileUser = new UserHandle(MANAGED_PROFILE_ID);
        when(mPackageManager.getInstallReason(APP_5, managedProfileUser))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);
        when(mPackageManager.getInstallReason(APP_6, managedProfileUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
    }

    private class InstalledAppCounterTestable extends InstalledAppCounter {
        private InstalledAppCounterTestable(int installReason) {
            super(mContext, installReason, mPackageManager);
        }

        private InstalledAppCounterTestable(int installReason, FeatureFlags featureFlags) {
            super(mContext, installReason, mPackageManager, featureFlags);
        }

        @Override
        protected void onCountComplete(int num) {
            mInstalledAppCount = num;
        }
    }

    private ArgumentMatcher<Intent> isLaunchIntentFor(String packageName) {
        return intent -> {
            if (intent == null) {
                return false;
            }
            if (!Intent.ACTION_MAIN.equals(intent.getAction())) {
                return false;
            }
            final Set<String> categories = intent.getCategories();
            if (categories == null || categories.size() != 1 ||
                    !categories.contains(Intent.CATEGORY_LAUNCHER)) {
                return false;
            }
            if (!packageName.equals(intent.getPackage())) {
                return false;
            }
            return true;
        };
    }

    private ArgumentMatcher<ApplicationInfoFlags> isApplicationInfoFlagsEqualTo(
            ApplicationInfoFlags infoFlags) {
        return flags -> {
            if (flags == null) {
                return false;
            }
            return flags.getValue() == infoFlags.getValue();
        };
    }
}
