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
 * limitations under the License
 */

package com.android.settings.applications;

import static com.android.settings.testutils.ApplicationTestUtils.buildInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public final class InstalledAppListerTest {

    private final String APP_1 = "app1";
    private final String APP_2 = "app2";
    private final String APP_3 = "app3";
    private final String APP_4 = "app4";
    private final String APP_5 = "app5";
    private final String APP_6 = "app6";

    private final int MAIN_USER_ID = 0;
    private final int MANAGED_PROFILE_ID = 10;

    private final int PER_USER_UID_RANGE = 100000;
    private final int MAIN_USER_APP_UID = MAIN_USER_ID * PER_USER_UID_RANGE;
    private final int MANAGED_PROFILE_APP_UID = MANAGED_PROFILE_ID * PER_USER_UID_RANGE;

    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;

    private List<UserAppInfo> mInstalledAppList = Collections.emptyList();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void expectQueryIntentActivities(int userId, String packageName, boolean launchable) {
        when(mPackageManager.queryIntentActivitiesAsUser(
                argThat(isLaunchIntentFor(packageName)),
                eq(PackageManager.GET_DISABLED_COMPONENTS | PackageManager.MATCH_DIRECT_BOOT_AWARE
                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE),
                eq(userId))).thenReturn(launchable
                        ? Collections.singletonList(new ResolveInfo())
                        : Collections.emptyList());
    }

    @Test
    public void testCountInstalledAppsAcrossAllUsers() {
        // There are two users.
        when(mUserManager.getProfiles(UserHandle.myUserId())).thenReturn(Arrays.asList(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN),
                new UserInfo(MANAGED_PROFILE_ID, "managed profile", 0)));

        // The first user has four apps installed:
        // * app1 is an updated system app. It should be listed.
        // * app2 is a user-installed app. It should be listed.
        // * app3 is a system app that provides a launcher icon. It should be listed.
        // * app4 is a system app that provides no launcher icon. It should not be listed.
        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                | PackageManager.MATCH_ANY_USER,
                MAIN_USER_ID)).thenReturn(Arrays.asList(
                        buildInfo(MAIN_USER_APP_UID, APP_1,
                                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP, 0 /* targetSdkVersion */),
                        buildInfo(MAIN_USER_APP_UID, APP_2, 0 /* flags */,
                                0 /* targetSdkVersion */),
                        buildInfo(MAIN_USER_APP_UID, APP_3, ApplicationInfo.FLAG_SYSTEM,
                                0 /* targetSdkVersion */),
                        buildInfo(MAIN_USER_APP_UID, APP_4, ApplicationInfo.FLAG_SYSTEM,
                                0 /* targetSdkVersion */)));
        // For system apps, InstalledAppLister checks whether they handle the default launcher
        // intent to decide whether to include them in the list of installed apps or not.
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
        // * app5 is a user-installed app. It should be listed.
        // * app6 is a system app that provides a launcher icon. It should be listed.
        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS,
                MANAGED_PROFILE_ID)).thenReturn(Arrays.asList(
                        buildInfo(MANAGED_PROFILE_APP_UID, APP_5, 0 /* flags */,
                                0 /* targetSdkVersion */),
                        buildInfo(MANAGED_PROFILE_APP_UID, APP_6, ApplicationInfo.FLAG_SYSTEM,
                                0 /* targetSdkVersion */)));
        expectQueryIntentActivities(MANAGED_PROFILE_ID, APP_6, true /* launchable */);

        // app5 is installed by enterprise policy.
        final UserHandle managedProfileUser = new UserHandle(MANAGED_PROFILE_ID);
        when(mPackageManager.getInstallReason(APP_5, managedProfileUser))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);
        when(mPackageManager.getInstallReason(APP_6, managedProfileUser))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);

        // List apps, considering apps installed by enterprise policy only.
        mInstalledAppList = Collections.emptyList();
        final InstalledAppListerTestable counter = new InstalledAppListerTestable();
        counter.execute();
        // Wait for the background task to finish.
        ShadowApplication.runBackgroundTasks();

        assertThat(mInstalledAppList.size()).isEqualTo(3);

        assertThat(checkAppFound(mInstalledAppList, APP_1, MAIN_USER_ID)).isTrue();
        assertThat(checkAppFound(mInstalledAppList, APP_2, MAIN_USER_ID)).isFalse();
        assertThat(checkAppFound(mInstalledAppList, APP_3, MAIN_USER_ID)).isTrue();
        assertThat(checkAppFound(mInstalledAppList, APP_4, MAIN_USER_ID)).isFalse();
        assertThat(checkAppFound(mInstalledAppList, APP_5, MANAGED_PROFILE_ID)).isTrue();
        assertThat(checkAppFound(mInstalledAppList, APP_6, MANAGED_PROFILE_ID)).isFalse();

        // Verify that installed packages were retrieved for the current user and the user's
        // managed profile.
        verify(mPackageManager).getInstalledApplicationsAsUser(anyInt(), eq(MAIN_USER_ID));
        verify(mPackageManager).getInstalledApplicationsAsUser(anyInt(), eq(MANAGED_PROFILE_ID));
        verify(mPackageManager, atLeast(0))
            .queryIntentActivitiesAsUser(any(), anyInt(), anyInt());
    }

    public static boolean checkAppFound(List<UserAppInfo> mInstalledAppList, String appId,
            int userId) {
       for (UserAppInfo info : mInstalledAppList) {
           if (appId.equals(info.appInfo.packageName) && (info.userInfo.id == userId)) {
                return true;
            }
        }
        return false;
    }

    public static void verifyListUniqueness(List<UserAppInfo> list) {
        assertThat((new HashSet<>(list)).size()).isEqualTo(list.size());
    }

    private class InstalledAppListerTestable extends InstalledAppLister {
        private InstalledAppListerTestable() {
            super(mPackageManager, mUserManager);
        }

        @Override
        protected void onAppListBuilt(List<UserAppInfo> list) {
            mInstalledAppList = list;
        }
    }

    private static ArgumentMatcher<Intent> isLaunchIntentFor(String packageName) {
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
}
