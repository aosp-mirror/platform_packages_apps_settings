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

import static com.android.settings.applications.appinfo.AppInfoDashboardFragment.ACCESS_RESTRICTED_SETTINGS;
import static com.android.settings.applications.appinfo.AppInfoDashboardFragment.ARG_PACKAGE_NAME;
import static com.android.settings.applications.appinfo.AppInfoDashboardFragment.UNINSTALL_ALL_USERS_MENU;
import static com.android.settings.applications.appinfo.AppInfoDashboardFragment.UNINSTALL_UPDATES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.util.ArraySet;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public final class AppInfoDashboardFragmentTest {

    private static final String PACKAGE_NAME = "test_package_name";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock
    private SettingsActivity mActivity;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
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
        when(mUserManager.isAdminUser()).thenReturn(true);

        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        // Default to not considering any apps to be instant (individual tests can override this).
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));
    }

    @After
    public void tearDown() {
        ShadowAppUtils.reset();
    }

    @Test
    public void shouldShowUninstallForAll_installForOneOtherUserOnly_shouldReturnTrue() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);
        ReflectionHelpers.setField(mFragment, "mDpm", mDevicePolicyManager);

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
    public void onPrepareOptionsMenu_setUpdateMenuVisible_byDefaultForSystemApps_shouldBeTrue() {
        Menu menu = onPrepareOptionsMenuTestsSetup();
        mFragment.onPrepareOptionsMenu(menu);

        verify(menu.findItem(UNINSTALL_UPDATES), times(1)).setVisible(true);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void onPrepareOptionsMenu_setUpdateMenuVisible_ifDisabledByDevice_shouldBeFalse() {
        Menu menu = onPrepareOptionsMenuTestsSetup();
        mFragment.onPrepareOptionsMenu(menu);

        verify(menu.findItem(UNINSTALL_UPDATES), times(1)).setVisible(false);
    }

    private Menu onPrepareOptionsMenuTestsSetup() {
        // Menu mocking
        Menu menu = mock(Menu.class);
        final MenuItem uninstallUpdatesMenuItem = mock(MenuItem.class);
        final MenuItem uninstallForAllMenuItem = mock(MenuItem.class);
        final MenuItem accessRestrictedMenuItem = mock(MenuItem.class);
        when(menu.findItem(UNINSTALL_UPDATES)).thenReturn(uninstallUpdatesMenuItem);
        when(menu.findItem(UNINSTALL_ALL_USERS_MENU)).thenReturn(uninstallForAllMenuItem);
        when(menu.findItem(ACCESS_RESTRICTED_SETTINGS)).thenReturn(accessRestrictedMenuItem);

        // Setup work to prevent NPE
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        mFragment.setAppEntry(appEntry);

        return menu;
    }

    @Test
    public void ensurePackageInfoAvailable_hasNoPackageInfo_shouldFinish() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", null);

        assertThat(mFragment.ensurePackageInfoAvailable(mActivity)).isFalse();
        verify(mActivity).finishAndRemoveTask();
    }

    @Test
    public void ensurePackageInfoAvailable_hasPackageInfo_shouldReturnTrue() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        assertThat(mFragment.ensurePackageInfoAvailable(mActivity)).isTrue();
        verify(mActivity, never()).finishAndRemoveTask();
    }

    @Test
    @Config(shadows = ShadowAppUtils.class)
    public void ensureDisplayableModule_hiddenModule_shouldReturnFalse() {
        ShadowAppUtils.addHiddenModule(PACKAGE_NAME);
        ReflectionHelpers.setField(mFragment, "mPackageName", PACKAGE_NAME);


        assertThat(mFragment.ensureDisplayableModule(mActivity)).isFalse();
    }

    @Test
    @Config(shadows = ShadowAppUtils.class)
    public void ensureDisplayableModule_regularApp_shouldReturnTrue() {
        ReflectionHelpers.setField(mFragment, "mPackageName", PACKAGE_NAME);

        assertThat(mFragment.ensureDisplayableModule(mActivity)).isTrue();
    }

    @Test
    public void createPreference_hasNoPackageInfo_shouldSkip() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", null);

        mFragment.onCreatePreferences(new Bundle(), "root_key");

        verify(mActivity).finishAndRemoveTask();
        verify(mFragment, never()).getPreferenceScreen();
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
        ReflectionHelpers.setField(mFragment, "mPackageInfo", packageInfo);

        assertThat(mFragment.shouldShowUninstallForAll(appEntry)).isFalse();
    }

    @Test
    public void onActivityResult_uninstalledUpdates_shouldInvalidateOptionsMenu() {
        doReturn(true).when(mFragment).refreshUi();

        mFragment
                .onActivityResult(AppInfoDashboardFragment.REQUEST_UNINSTALL, 0,
                        mock(Intent.class));

        verify(mActivity).invalidateOptionsMenu();
    }

    @Test
    public void getPreferenceControllers_noPackageInfo_shouldReturnNull() {
        doNothing().when(mFragment).retrieveAppEntry();

        assertThat(mFragment.createPreferenceControllers(mShadowContext)).isNull();
    }

    @Test
    public void getPreferenceControllers_exiting_shouldReturnNull() {
        mFragment.mFinishing = true;

        assertThat(mFragment.createPreferenceControllers(mShadowContext)).isNull();
    }

    @Test
    public void getNumberOfUserWithPackageInstalled_twoUsersInstalled_shouldReturnTwo()
            throws PackageManager.NameNotFoundException {
        final String packageName = "Package1";
        final int userID1 = 1;
        final int userID2 = 2;
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo(userID1, "User1", UserInfo.FLAG_PRIMARY));
        userInfos.add(new UserInfo(userID2, "yue", UserInfo.FLAG_GUEST));
        when(mUserManager.getAliveUsers()).thenReturn(userInfos);
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
            throws PackageManager.NameNotFoundException {
        final String packageName = "Package1";
        final int userID1 = 1;
        final int userID2 = 2;
        final List<UserInfo> userInfos = new ArrayList<>();
        userInfos.add(new UserInfo(userID1, "User1", UserInfo.FLAG_PRIMARY));
        userInfos.add(new UserInfo(userID2, "yue", UserInfo.FLAG_GUEST));
        when(mUserManager.getAliveUsers()).thenReturn(userInfos);
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

    @Test
    public void onDestroy_shouldUnregisterReceiver() {
        final Context context = mock(Context.class);
        doReturn(context).when(mFragment).getContext();
        ReflectionHelpers.setField(mFragment, "mLifecycle", mock(Lifecycle.class));
        mFragment.startListeningToPackageRemove();

        mFragment.onDestroy();

        verify(context).unregisterReceiver(mFragment.mPackageRemovedReceiver);
    }

    @Test
    public void startAppInfoFragment_noCrashOnNullArgs() {
        final SettingsPreferenceFragment caller = mock(SettingsPreferenceFragment.class);
        final SettingsActivity sa = mock(SettingsActivity.class);
        when(caller.getActivity()).thenReturn(sa);
        when(caller.getContext()).thenReturn(sa);
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = mock(ApplicationInfo.class);

        AppInfoDashboardFragment.startAppInfoFragment(AppInfoDashboardFragment.class, 0, null,
                caller, appEntry);
    }

    @Test
    public void startAppInfoFragment_includesNewAndOldArgs() {
        final SettingsPreferenceFragment caller = mock(SettingsPreferenceFragment.class);
        final SettingsActivity sa = mock(SettingsActivity.class);
        when(caller.getContext()).thenReturn(sa);
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = mock(ApplicationInfo.class);

        final Bundle bundle = new Bundle();
        bundle.putString("test", "test");

        AppInfoDashboardFragment.startAppInfoFragment(AppInfoDashboardFragment.class, 0, bundle,
                caller, appEntry);

        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        verify(caller).startActivityForResult(intent.capture(), any(Integer.class));
        assertThat(intent.getValue().getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .containsKey("test"))
                .isTrue();
        assertThat(intent.getValue().getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .containsKey(ARG_PACKAGE_NAME))
                .isTrue();
    }

    @Test
    public void shouldSkipForInitialSUW_returnTrue() {
        assertThat(mFragment.shouldSkipForInitialSUW()).isTrue();
    }

    @Implements(AppUtils.class)
    public static class ShadowAppUtils {

        public static Set<String> sHiddenModules = new ArraySet<>();

        @Resetter
        public static void reset() {
            sHiddenModules.clear();
        }

        public static void addHiddenModule(String pkg) {
            sHiddenModules.add(pkg);
        }

        @Implementation
        protected static boolean isHiddenSystemModule(Context context, String packageName) {
            return sHiddenModules.contains(packageName);
        }
    }
}
