/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.Menu;
import android.view.MenuItem;

import com.android.settings.R;
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

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION
)
public final class ForceStopOptionsMenuControllerTest {

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
    private ForceStopOptionsMenuController mController;
    private Context mShadowContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowContext = spy(RuntimeEnvironment.application);
        mFragment = spy(new AppInfoDashboardFragment());
        ReflectionHelpers.setField(mFragment, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mShadowContext).when(mFragment).getContext();
        doReturn(mPackageManager).when(mActivity).getPackageManager();
        when(mShadowContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mController = spy(new ForceStopOptionsMenuController(
            mShadowContext, mFragment, mDevicePolicyManager,
            null /* metricsFeatureProvider */, null /* lifecycle */));

        // Default to not considering any apps to be instant (individual tests can override this).
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));
    }

    @Test
    public void onCreateOptionsMenu_shouldAddForceStop() {
        final Menu menu = mock(Menu.class);
        when(menu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mock(MenuItem.class));

        mController.onCreateOptionsMenu(menu, null /* inflater */);

        verify(menu).add(anyInt(), eq(AppInfoDashboardFragment.FORCE_STOP_MENU), anyInt(),
            eq(R.string.force_stop));
    }

    @Test
    public void onPrepareOptionsMenu_shouldUpdateForceStopMenu() {
        final Menu menu = mock(Menu.class);
        doNothing().when(mController).updateForceStopMenu(any(), any());
        doReturn(mock(AppEntry.class)).when(mFragment).getAppEntry();
        doReturn(mock(PackageInfo.class)).when(mFragment).getPackageInfo();

        mController.onPrepareOptionsMenu(menu);

        verify(mController).updateForceStopMenu(any(), any());
    }

    @Test
    public void onOptionsItemSelected_shouldHandleForceStopMenuClick() {
        doReturn(mock(AppEntry.class)).when(mFragment).getAppEntry();
        doNothing().when(mController).handleForceStopMenuClick();
        final MenuItem menu = mock(MenuItem.class);
        when(menu.getItemId()).thenReturn(AppInfoDashboardFragment.FORCE_STOP_MENU);

        mController.onOptionsItemSelected(menu);

        verify(mController).handleForceStopMenuClick();
    }

    // Tests that we don't show the force stop button for instant apps (they aren't allowed to run
    // when they aren't in the foreground).
    @Test
    public void updateForceStopMenu_instantApps_shouldNotShowForceStop() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        final MenuItem forceStopMenu = mock(MenuItem.class);
        ReflectionHelpers.setField(mController, "mForceStopMenu", forceStopMenu);
        // Make this app appear to be instant.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
            (InstantAppDataProvider) (i -> true));
        final PackageInfo packageInfo = mock(PackageInfo.class);
        final AppEntry appEntry = mock(AppEntry.class);
        final ApplicationInfo info = new ApplicationInfo();
        appEntry.info = info;

        mController.updateForceStopMenu(appEntry, packageInfo);

        verify(forceStopMenu).setVisible(false);
    }

    @Test
    public void updateForceStopMenu_hasActiveAdmin_shouldDisableForceStop() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        final MenuItem forceStopMenu = mock(MenuItem.class);
        ReflectionHelpers.setField(mController, "mForceStopMenu", forceStopMenu);
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
            (InstantAppDataProvider) (i -> false));
        final String packageName = "Package1";
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        final AppEntry appEntry = mock(AppEntry.class);
        when(mDevicePolicyManager.packageHasActiveAdmins(packageName)).thenReturn(true);

        mController.updateForceStopMenu(appEntry, packageInfo);

        verify(forceStopMenu).setEnabled(false);
    }

    @Test
    public void updateForceStopMenu_appRunning_shouldEnableForceStop() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        final MenuItem forceStopMenu = mock(MenuItem.class);
        ReflectionHelpers.setField(mController, "mForceStopMenu", forceStopMenu);
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
            (InstantAppDataProvider) (i -> false));
        final PackageInfo packageInfo = mock(PackageInfo.class);
        final AppEntry appEntry = mock(AppEntry.class);
        final ApplicationInfo info = new ApplicationInfo();
        appEntry.info = info;

        mController.updateForceStopMenu(appEntry, packageInfo);

        verify(forceStopMenu).setEnabled(true);
    }

    @Test
    public void updateForceStopMenu_appStopped_shouldQueryPackageRestart() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
            (InstantAppDataProvider) (i -> false));
        final PackageInfo packageInfo = mock(PackageInfo.class);
        final AppEntry appEntry = mock(AppEntry.class);
        final ApplicationInfo info = new ApplicationInfo();
        appEntry.info = info;
        info.flags = ApplicationInfo.FLAG_STOPPED;
        info.packageName = "com.android.setting";

        mController.updateForceStopMenu(appEntry, packageInfo);

        verify(mShadowContext).sendOrderedBroadcastAsUser(argThat(intent-> intent != null
                && intent.getAction().equals(Intent.ACTION_QUERY_PACKAGE_RESTART)),
            any(UserHandle.class), nullable(String.class), any(BroadcastReceiver.class),
            nullable(Handler.class), anyInt(), nullable(String.class), nullable(Bundle.class));
    }

}
