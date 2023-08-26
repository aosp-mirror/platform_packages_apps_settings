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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class InstantAppButtonsPreferenceControllerTest {

    private static final String TEST_INSTALLER_PACKAGE_NAME = "com.installer";
    private static final String TEST_INSTALLER_ACTIVITY_NAME = "com.installer.InstallerActivity";
    private static final String TEST_AIA_PACKAGE_NAME = "test.aia.package";

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mAppInfo;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private LayoutPreference mPreference;

    private Context mContext;
    private PreferenceScreen mScreen;
    private PreferenceManager mPreferenceManager;
    private Button mLaunchButton;
    private Button mInstallButton;
    private Button mClearAppButton;
    private InstantAppButtonsPreferenceController mController;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = mAppInfo;
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        final View buttons = View.inflate(
            RuntimeEnvironment.application, R.layout.instant_app_buttons, null /* parent */);
        mLaunchButton = buttons.findViewById(R.id.launch);
        mInstallButton = buttons.findViewById(R.id.install);
        mClearAppButton = buttons.findViewById(R.id.clear_data);
        mController = spy(new InstantAppButtonsPreferenceController(
            mContext, mFragment, TEST_AIA_PACKAGE_NAME, null /* lifecycle */));
        when(mPreference.getKey()).thenReturn("instant_app_buttons");
        mScreen.addPreference(mPreference);
        when(mPreference.findViewById(R.id.instant_app_button_container)).thenReturn(buttons);
        final InstallSourceInfo installSourceInfo = mock(InstallSourceInfo.class);
        when(mPackageManager.getInstallSourceInfo(TEST_AIA_PACKAGE_NAME))
            .thenReturn(installSourceInfo);
        when(installSourceInfo.getInstallingPackageName()).thenReturn(TEST_INSTALLER_PACKAGE_NAME);
    }

    @Test
    public void getAvailabilityStatus_notInstantApp_shouldReturnDisabled() {
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_isInstantApp_shouldReturnAvailable() {
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void onCreateOptionsMenu_noLaunchUri_shouldNotAddInstallInstantAppMenu() {
        final Menu menu = mock(Menu.class);
        when(menu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mock(MenuItem.class));

        mController.onCreateOptionsMenu(menu, null /* inflater */);

        verify(menu, never()).add(anyInt(), eq(AppInfoDashboardFragment.INSTALL_INSTANT_APP_MENU),
            anyInt(), eq(R.string.install_text));
    }

    @Test
    public void onCreateOptionsMenu_hasLaunchUri_shouldAddForceStop() {
        ReflectionHelpers.setField(mController, "mLaunchUri", "www.test.launch");
        final Menu menu = mock(Menu.class);
        when(menu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mock(MenuItem.class));

        mController.onCreateOptionsMenu(menu, null /* inflater */);

        verify(menu).add(anyInt(), eq(AppInfoDashboardFragment.INSTALL_INSTANT_APP_MENU),
            anyInt(), eq(R.string.install_text));
    }

    @Test
    public void onPrepareOptionsMenu_noAppStoreLink_shoulDisableInstallInstantAppMenu() {
        ReflectionHelpers.setField(mController, "mLaunchUri", "www.test.launch");
        final Menu menu = mock(Menu.class);
        final MenuItem menuItem = mock(MenuItem.class);
        when(menu.findItem(AppInfoDashboardFragment.INSTALL_INSTANT_APP_MENU)).thenReturn(menuItem);

        mController.onPrepareOptionsMenu(menu);

        verify(menuItem).setEnabled(false);
    }

    @Test
    public void onPrepareOptionsMenu_hasAppStoreLink_shoulNotDisableInstallInstantAppMenu() {
        ReflectionHelpers.setField(mController, "mLaunchUri", "www.test.launch");
        initAppStoreInfo();
        final Menu menu = mock(Menu.class);
        final MenuItem menuItem = mock(MenuItem.class);
        when(menu.findItem(AppInfoDashboardFragment.INSTALL_INSTANT_APP_MENU)).thenReturn(menuItem);

        mController.onPrepareOptionsMenu(menu);

        verify(menuItem, never()).setEnabled(false);
    }

    @Test
    public void onPrepareOptionsMenu_installMenuNotFound_shoulNotCrash() {
        final Menu menu = mock(Menu.class);

        mController.onPrepareOptionsMenu(menu);

        // no crash
    }

    @Test
    public void onOptionsItemSelected_shouldOpenAppStore() {
        initAppStoreInfo();
        mController.displayPreference(mScreen);
        final ComponentName componentName =
            new ComponentName(TEST_INSTALLER_PACKAGE_NAME, TEST_INSTALLER_ACTIVITY_NAME);
        final MenuItem menu = mock(MenuItem.class);
        when(menu.getItemId()).thenReturn(AppInfoDashboardFragment.INSTALL_INSTANT_APP_MENU);

        mController.onOptionsItemSelected(menu);

        verify(mFragment).startActivity(argThat(intent-> intent != null
            && intent.getAction().equals(Intent.ACTION_SHOW_APP_INFO)
            && intent.getComponent().equals(componentName)));
    }

    @Test
    public void displayPreference_noLaunchUri_shouldShowHideLaunchButton() {
        mController.displayPreference(mScreen);

        assertThat(mLaunchButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void displayPreference_hasLaunchUri_shouldShowHideInstallButton() {
        ReflectionHelpers.setField(mController, "mLaunchUri", "www.test.launch");

        mController.displayPreference(mScreen);

        assertThat(mInstallButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void displayPreference_noAppStoreLink_shoulDisableInstallButton() {
        mController.displayPreference(mScreen);

        assertThat(mInstallButton.isEnabled()).isFalse();
    }

    @Test
    public void displayPreference_hasAppStoreLink_shoulSetClickListenerForInstallButton() {
        initAppStoreInfo();

        mController.displayPreference(mScreen);

        assertThat(mInstallButton.hasOnClickListeners()).isTrue();
    }

    @Test
    public void displayPreference_shoulSetClickListenerForClearButton() {
        mController.displayPreference(mScreen);

        assertThat(mClearAppButton.hasOnClickListeners()).isTrue();
    }

    @Test
    public void clickLaunchButton_shouldLaunchViewIntent() {
        final String launchUri = "www.test.launch";
        ReflectionHelpers.setField(mController, "mLaunchUri", launchUri);
        mController.displayPreference(mScreen);

        mLaunchButton.callOnClick();

        verify(mFragment).startActivity(argThat(intent-> intent != null
            && intent.getAction().equals(Intent.ACTION_VIEW)
            && intent.hasCategory(Intent.CATEGORY_BROWSABLE)
            && (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0
            && TextUtils.equals(intent.getPackage(), TEST_AIA_PACKAGE_NAME)
            && TextUtils.equals(intent.getDataString(), launchUri)));
    }

    @Test
    public void clickInstallButton_shouldOpenAppStore() {
        initAppStoreInfo();
        mController.displayPreference(mScreen);
        final ComponentName componentName =
            new ComponentName(TEST_INSTALLER_PACKAGE_NAME, TEST_INSTALLER_ACTIVITY_NAME);

        mInstallButton.callOnClick();

        verify(mFragment).startActivity(argThat(intent-> intent != null
            && intent.getAction().equals(Intent.ACTION_SHOW_APP_INFO)
            && intent.getComponent().equals(componentName)));
    }

    @Test
    public void clickClearAppButton_shouldLaunchInstantAppButtonDialogFragment() {
        final FragmentManager fragmentManager = mock(FragmentManager.class);
        final FragmentTransaction fragmentTransaction = mock(FragmentTransaction.class);
        when(mFragment.getFragmentManager()).thenReturn(fragmentManager);
        when(fragmentManager.beginTransaction()).thenReturn(fragmentTransaction);
        mController.displayPreference(mScreen);

        mClearAppButton.callOnClick();

        verify(fragmentTransaction).add(any(InstantAppButtonDialogFragment.class),
            eq("instant_app_buttons"));
    }

    private void initAppStoreInfo() {
        final ResolveInfo resolveInfo = mock(ResolveInfo.class);
        final ActivityInfo activityInfo = mock(ActivityInfo.class);
        resolveInfo.activityInfo = activityInfo;
        activityInfo.packageName = TEST_INSTALLER_PACKAGE_NAME;
        activityInfo.name = TEST_INSTALLER_ACTIVITY_NAME;
        when(mPackageManager.resolveActivity(any(), anyInt())).thenReturn(resolveInfo);
    }
}
