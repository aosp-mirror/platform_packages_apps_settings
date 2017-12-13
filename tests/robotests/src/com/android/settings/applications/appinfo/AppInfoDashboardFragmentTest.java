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

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.ActionButtonPreferenceTest;
import com.android.settings.wrapper.DevicePolicyManagerWrapper;
import com.android.settingslib.Utils;
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
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION
)
public final class AppInfoDashboardFragmentTest {

    private static final String PACKAGE_NAME = "test_package_name";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mActivity;
    @Mock
    private DevicePolicyManagerWrapper mDevicePolicyManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AppOpsManager mAppOpsManager;

    private FakeFeatureFactory mFeatureFactory;
    private AppInfoDashboardFragment mFragment;
    private Context mShadowContext;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mShadowContext = RuntimeEnvironment.application;
        mFragment = spy(new AppInfoDashboardFragment());
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mShadowContext).when(mFragment).getContext();
        doReturn(mPackageManager).when(mActivity).getPackageManager();
        doReturn(mAppOpsManager).when(mActivity).getSystemService(Context.APP_OPS_SERVICE);
        mFragment.mActionButtons = ActionButtonPreferenceTest.createMock();

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

    // Tests that we don't show the uninstall button for instant apps"
    @Test
    public void instantApps_noUninstallButton() {
        // Make this app appear to be instant.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = info;

        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mAppEntry", appEntry);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        mFragment.initUninstallButtonForUserApp();
        verify(mFragment.mActionButtons).setButton1Visible(false);
    }

    // Tests that we don't show the force stop button for instant apps (they aren't allowed to run
    // when they aren't in the foreground).
    @Test
    public void instantApps_noForceStop() {
        // Make this app appear to be instant.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));
        final PackageInfo packageInfo = mock(PackageInfo.class);
        final AppEntry appEntry = mock(AppEntry.class);
        final ApplicationInfo info = new ApplicationInfo();
        appEntry.info = info;

        ReflectionHelpers.setField(mFragment, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);
        ReflectionHelpers.setField(mFragment, "mAppEntry", appEntry);

        mFragment.checkForceStop();
        verify(mFragment.mActionButtons).setButton2Visible(false);
    }

    @Test
    public void onActivityResult_uninstalledUpdates_shouldInvalidateOptionsMenu() {
        doReturn(true).when(mFragment).refreshUi();

        mFragment.onActivityResult(mFragment.REQUEST_UNINSTALL, 0, mock(Intent.class));

        verify(mActivity).invalidateOptionsMenu();
    }

    @Test
    public void handleDisableable_appIsHomeApp_buttonShouldNotWork() {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = "pkg";
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final HashSet<String> homePackages = new HashSet<>();
        homePackages.add(info.packageName);

        ReflectionHelpers.setField(mFragment, "mHomePackages", homePackages);
        ReflectionHelpers.setField(mFragment, "mAppEntry", appEntry);

        assertThat(mFragment.handleDisableable()).isFalse();
        verify(mFragment.mActionButtons).setButton1Text(R.string.disable_text);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void handleDisableable_appIsEnabled_buttonShouldWork() {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = "pkg";
        info.enabled = true;
        info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        when(mFeatureFactory.applicationFeatureProvider.getKeepEnabledPackages()).thenReturn(
                new HashSet<>());

        ReflectionHelpers.setField(mFragment, "mApplicationFeatureProvider",
                mFeatureFactory.applicationFeatureProvider);
        ReflectionHelpers.setField(mFragment, "mAppEntry", appEntry);

        assertThat(mFragment.handleDisableable()).isTrue();
        verify(mFragment.mActionButtons).setButton1Text(R.string.disable_text);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void handleDisableable_appIsDisabled_buttonShouldShowEnable() {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = "pkg";
        info.enabled = false;
        info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        when(mFeatureFactory.applicationFeatureProvider.getKeepEnabledPackages()).thenReturn(
                new HashSet<>());

        ReflectionHelpers.setField(mFragment, "mApplicationFeatureProvider",
                mFeatureFactory.applicationFeatureProvider);
        ReflectionHelpers.setField(mFragment, "mAppEntry", appEntry);

        assertThat(mFragment.handleDisableable()).isTrue();
        verify(mFragment.mActionButtons).setButton1Text(R.string.enable_text);
        verify(mFragment.mActionButtons).setButton1Positive(true);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void handleDisableable_appIsEnabledAndInKeepEnabledWhitelist_buttonShouldNotWork() {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = "pkg";
        info.enabled = true;
        info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;

        final HashSet<String> packages = new HashSet<>();
        packages.add(info.packageName);
        when(mFeatureFactory.applicationFeatureProvider.getKeepEnabledPackages()).thenReturn(
                packages);

        ReflectionHelpers.setField(mFragment, "mApplicationFeatureProvider",
                mFeatureFactory.applicationFeatureProvider);
        ReflectionHelpers.setField(mFragment, "mAppEntry", appEntry);

        assertThat(mFragment.handleDisableable()).isFalse();
        verify(mFragment.mActionButtons).setButton1Text(R.string.disable_text);
    }

    @Test
    public void initUninstallButtonForUserApp_shouldSetNegativeButton() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = info;
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        mFragment.initUninstallButtonForUserApp();

        verify(mFragment.mActionButtons).setButton1Positive(false);
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

    @Implements(Utils.class)
    public static class ShadowUtils {
        @Implementation
        public static boolean isSystemPackage(Resources resources, PackageManager pm,
                PackageInfo pkg) {
            return false;
        }
    }
}
