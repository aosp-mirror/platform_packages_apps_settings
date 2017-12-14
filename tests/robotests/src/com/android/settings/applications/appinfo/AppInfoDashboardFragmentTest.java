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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserManager;

import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.DevicePolicyManagerWrapper;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION
)
public final class AppInfoDashboardFragmentTest {

    private static final String PACKAGE_NAME = "test_package_name";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock
    private SettingsActivity mActivity;
    @Mock
    private DevicePolicyManagerWrapper mDevicePolicyManager;
    @Mock
    private PackageManager mPackageManager;

    private AppInfoDashboardFragment mFragment;
    private Context mShadowContext;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowContext = RuntimeEnvironment.application;
        mFragment = spy(new AppInfoDashboardFragment());
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mShadowContext).when(mFragment).getContext();
        doReturn(mPackageManager).when(mActivity).getPackageManager();

        // Default to not considering any apps to be instant (individual tests can override this).
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));
    }

    @Test
    public void shouldShowUninstallForAll_installForOneOtherUserOnly_shouldReturnTrue() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);
        ReflectionHelpers.setField(mFragment, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        final ApplicationInfo info = new ApplicationInfo();
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        assertThat(mFragment.shouldShowUninstallForAll(appEntry)).isTrue();
    }

    @Test
    public void shouldShowUninstallForAll_installForSelfOnly_shouldReturnFalse() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);
        ReflectionHelpers.setField(mFragment, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        assertThat(mFragment.shouldShowUninstallForAll(appEntry)).isFalse();
    }

    @Test
    public void launchFragment_hasNoPackageInfo_shouldFinish() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", null);

        assertThat(mFragment.ensurePackageInfoAvailable(mActivity)).isFalse();
        verify(mActivity).finishAndRemoveTask();
    }

    @Test
    public void launchFragment_hasPackageInfo_shouldReturnTrue() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        assertThat(mFragment.ensurePackageInfoAvailable(mActivity)).isTrue();
        verify(mActivity, never()).finishAndRemoveTask();
    }

    @Test
    public void packageSizeChange_isOtherPackage_shouldNotRefreshUi() {
        ReflectionHelpers.setField(mFragment, "mPackageName", PACKAGE_NAME);
        mFragment.onPackageSizeChanged("Not_" + PACKAGE_NAME);

        verify(mFragment, never()).refreshUi();
    }

    @Test
    public void packageSizeChange_isOwnPackage_shouldRefreshUi() {
        doReturn(Boolean.TRUE).when(mFragment).refreshUi();
        ReflectionHelpers.setField(mFragment, "mPackageName", PACKAGE_NAME);

        mFragment.onPackageSizeChanged(PACKAGE_NAME);

        verify(mFragment).refreshUi();
    }

    // Tests that we don't show the "uninstall for all users" button for instant apps.
    @Test
    public void instantApps_noUninstallForAllButton() {
        // Make this app appear to be instant.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);

        final ApplicationInfo info = new ApplicationInfo();
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);

        ReflectionHelpers.setField(mFragment, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        assertThat(mFragment.shouldShowUninstallForAll(appEntry)).isFalse();
    }

    @Test
    public void onActivityResult_uninstalledUpdates_shouldInvalidateOptionsMenu() {
        doReturn(true).when(mFragment).refreshUi();

        mFragment.onActivityResult(mFragment.REQUEST_UNINSTALL, 0, mock(Intent.class));

        verify(mActivity).invalidateOptionsMenu();
    }

    @Test
    public void getNumberOfUserWithPackageInstalled_twoUsersInstalled_shouldReturnTwo()
            throws PackageManager.NameNotFoundException{
        final String packageName = "Package1";
        final int userID1 = 1;
        final int userID2 = 2;
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo(userID1, "User1", UserInfo.FLAG_PRIMARY));
        userInfos.add(new UserInfo(userID2, "yue", UserInfo.FLAG_GUEST));
        when(mUserManager.getUsers(true)).thenReturn(userInfos);
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.flags = ApplicationInfo.FLAG_INSTALLED;
        when(mPackageManager.getApplicationInfoAsUser(
                packageName, PackageManager.GET_META_DATA, userID1))
                .thenReturn(appInfo);
        when(mPackageManager.getApplicationInfoAsUser(
                packageName, PackageManager.GET_META_DATA, userID2))
                .thenReturn(appInfo);
        ReflectionHelpers.setField(mFragment, "mPm", mPackageManager);

        assertThat(mFragment.getNumberOfUserWithPackageInstalled(packageName)).isEqualTo(2);
    }

    @Test
    public void getNumberOfUserWithPackageInstalled_oneUserInstalled_shouldReturnOne()
            throws PackageManager.NameNotFoundException{
        final String packageName = "Package1";
        final int userID1 = 1;
        final int userID2 = 2;
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo(userID1, "User1", UserInfo.FLAG_PRIMARY));
        userInfos.add(new UserInfo(userID2, "yue", UserInfo.FLAG_GUEST));
        when(mUserManager.getUsers(true)).thenReturn(userInfos);
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.flags = ApplicationInfo.FLAG_INSTALLED;
        when(mPackageManager.getApplicationInfoAsUser(
                packageName, PackageManager.GET_META_DATA, userID1))
                .thenReturn(appInfo);
        when(mPackageManager.getApplicationInfoAsUser(
                packageName, PackageManager.GET_META_DATA, userID2))
                .thenThrow(new PackageManager.NameNotFoundException());
        ReflectionHelpers.setField(mFragment, "mPm", mPackageManager);

        assertThat(mFragment.getNumberOfUserWithPackageInstalled(packageName)).isEqualTo(1);

    }
}
