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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.ApplicationTestUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import org.robolectric.shadows.ShadowApplication;
/**
 * Tests for {@link ApplicationFeatureProviderImpl}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {ShadowUserManager.class})
public final class ApplicationFeatureProviderImplTest {

    private final int MAIN_USER_ID = 0;
    private final int MANAGED_PROFILE_ID = 10;

    private final String APP_1 = "app1";
    private final String APP_2 = "app2";

    private @Mock UserManager mUserManager;
    private @Mock Context mContext;
    private @Mock PackageManagerWrapper mPackageManager;

    private ApplicationFeatureProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mProvider = new ApplicationFeatureProviderImpl(mContext, mPackageManager);
    }

    @Test
    public void testCalculateNumberOfInstalledApps() {
        final Integer[] numberOfInstalledApps = new Integer[1];
        numberOfInstalledApps[0] = null;

        when(mUserManager.getUsers(true)).thenReturn(Arrays.asList(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN),
                new UserInfo(MANAGED_PROFILE_ID, "managed profile", 0)));

        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                | PackageManager.MATCH_ANY_USER,
                MAIN_USER_ID)).thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(MAIN_USER_ID, APP_1, 0 /* flags */)));
        when(mPackageManager.getInstallReason(APP_1, new UserHandle(MAIN_USER_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);

        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS,
                MANAGED_PROFILE_ID)).thenReturn(Arrays.asList(
                        ApplicationTestUtils.buildInfo(MANAGED_PROFILE_ID, APP_2, 0 /* flags */)));
        when(mPackageManager.getInstallReason(APP_2, new UserHandle(MANAGED_PROFILE_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);

        // Count all installed apps.
        mProvider.calculateNumberOfInstalledApps(ApplicationFeatureProvider.IGNORE_INSTALL_REASON,
                (num) -> {
                    numberOfInstalledApps[0] = num;
                });
        ShadowApplication.runBackgroundTasks();
        assertThat(numberOfInstalledApps[0]).isEqualTo(2);

        // Count apps with specific install reason only.
        numberOfInstalledApps[0] = null;
        mProvider.calculateNumberOfInstalledApps(PackageManager.INSTALL_REASON_POLICY,
                (num) -> {
                    numberOfInstalledApps[0] = num;
                });
        ShadowApplication.runBackgroundTasks();
        assertThat(numberOfInstalledApps[0]).isEqualTo(1);
    }
}
