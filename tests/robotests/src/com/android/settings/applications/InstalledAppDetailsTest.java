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


import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.instantapps.InstantAppButtonsController;
import com.android.settings.applications.instantapps.InstantAppButtonsController.ShowDialogDelegate;
import com.android.settings.enterprise.DevicePolicyManagerWrapper;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(
    manifest = TestConfig.MANIFEST_PATH,
    sdk = TestConfig.SDK_VERSION,
    shadows = InstalledAppDetailsTest.ShadowUtils.class
)
public final class InstalledAppDetailsTest {

    private static final String PACKAGE_NAME = "test_package_name";
    private static final int TARGET_UID = 111;
    private static final int OTHER_UID = 222;
    private static final double BATTERY_LEVEL = 60;
    private static final String BATTERY_LEVEL_STRING = "60%";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mActivity;
    @Mock
    private DevicePolicyManagerWrapper mDevicePolicyManager;
    @Mock
    private BatterySipper mBatterySipper;
    @Mock
    private BatterySipper mOtherBatterySipper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatteryStats.Uid mUid;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private LoaderManager mLoaderManager;
    @Mock
    private AppOpsManager mAppOpsManager;

    private FakeFeatureFactory mFeatureFactory;
    private InstalledAppDetails mAppDetail;
    private Context mShadowContext;
    private Preference mBatteryPreference;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest(mContext);
        mShadowContext = RuntimeEnvironment.application;
        mAppDetail = spy(new InstalledAppDetails());
        mAppDetail.mBatteryUtils = mBatteryUtils;

        mBatteryPreference = new Preference(mShadowContext);
        mAppDetail.mBatteryPreference = mBatteryPreference;

        mBatterySipper.drainType = BatterySipper.DrainType.IDLE;
        mBatterySipper.uidObj = mUid;
        doReturn(TARGET_UID).when(mBatterySipper).getUid();
        doReturn(OTHER_UID).when(mOtherBatterySipper).getUid();
        doReturn(mActivity).when(mAppDetail).getActivity();
        doReturn(mShadowContext).when(mAppDetail).getContext();
        doReturn(mPackageManager).when(mActivity).getPackageManager();
        doReturn(mAppOpsManager).when(mActivity).getSystemService(Context.APP_OPS_SERVICE);

        // Default to not considering any apps to be instant (individual tests can override this).
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));
    }

    @Test
    public void shouldShowUninstallForAll_installForOneOtherUserOnly_shouldReturnTrue() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);
        ReflectionHelpers.setField(mAppDetail, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mAppDetail, "mUserManager", mUserManager);
        final ApplicationInfo info = new ApplicationInfo();
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);

        assertThat(mAppDetail.shouldShowUninstallForAll(appEntry)).isTrue();
    }

    @Test
    public void shouldShowUninstallForAll_installForSelfOnly_shouldReturnFalse() {
        when(mDevicePolicyManager.packageHasActiveAdmins(nullable(String.class))).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);
        ReflectionHelpers.setField(mAppDetail, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mAppDetail, "mUserManager", mUserManager);
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);

        assertThat(mAppDetail.shouldShowUninstallForAll(appEntry)).isFalse();
    }

    @Test
    public void getStorageSummary_shouldWorkForExternal() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getTotalBytes()).thenReturn(1L);

        assertThat(InstalledAppDetails.getStorageSummary(context, stats, true))
                .isEqualTo("1.00B used in external storage");
    }

    @Test
    public void getStorageSummary_shouldWorkForInternal() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getTotalBytes()).thenReturn(1L);

        assertThat(InstalledAppDetails.getStorageSummary(context, stats, false))
                .isEqualTo("1.00B used in internal storage");
    }

    @Test
    public void launchFragment_hasNoPackageInfo_shouldFinish() {
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", null);

        assertThat(mAppDetail.ensurePackageInfoAvailable(mActivity)).isFalse();
        verify(mActivity).finishAndRemoveTask();
    }

    @Test
    public void launchFragment_hasPackageInfo_shouldReturnTrue() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);

        assertThat(mAppDetail.ensurePackageInfoAvailable(mActivity)).isTrue();
        verify(mActivity, never()).finishAndRemoveTask();
    }

    @Test
    public void packageSizeChange_isOtherPackage_shouldNotRefreshUi() {
        ReflectionHelpers.setField(mAppDetail, "mPackageName", PACKAGE_NAME);
        mAppDetail.onPackageSizeChanged("Not_" + PACKAGE_NAME);

        verify(mAppDetail, never()).refreshUi();
    }

    @Test
    public void packageSizeChange_isOwnPackage_shouldRefreshUi() {
        doReturn(Boolean.TRUE).when(mAppDetail).refreshUi();
        ReflectionHelpers.setField(mAppDetail, "mPackageName", PACKAGE_NAME);

        mAppDetail.onPackageSizeChanged(PACKAGE_NAME);

        verify(mAppDetail).refreshUi();
    }

    @Test
    public void launchPowerUsageDetailFragment_shouldNotCrash() {
        mAppDetail.mBatteryPreference = mBatteryPreference;
        mAppDetail.mSipper = mBatterySipper;
        mAppDetail.mBatteryHelper = mBatteryStatsHelper;

        // Should not crash
        mAppDetail.onPreferenceClick(mBatteryPreference);
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

        ReflectionHelpers.setField(mAppDetail, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mAppDetail, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);

        assertThat(mAppDetail.shouldShowUninstallForAll(appEntry)).isFalse();
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
        final Button uninstallButton = mock(Button.class);

        ReflectionHelpers.setField(mAppDetail, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mAppDetail, "mAppEntry", appEntry);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);
        ReflectionHelpers.setField(mAppDetail, "mUninstallButton", uninstallButton);

        mAppDetail.initUnintsallButtonForUserApp();
        verify(uninstallButton).setVisibility(View.GONE);
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
        final Button forceStopButton = mock(Button.class);

        ReflectionHelpers.setField(mAppDetail, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);
        ReflectionHelpers.setField(mAppDetail, "mAppEntry", appEntry);
        ReflectionHelpers.setField(mAppDetail, "mForceStopButton", forceStopButton);

        mAppDetail.checkForceStop();
        verify(forceStopButton).setVisibility(View.GONE);
    }

    @Test
    public void instantApps_buttonControllerHandlesDialog() {
        InstantAppButtonsController mockController = mock(InstantAppButtonsController.class);
        ReflectionHelpers.setField(
                mAppDetail, "mInstantAppButtonsController", mockController);
        // Make sure first that button controller is not called for supported dialog id
        AlertDialog mockDialog = mock(AlertDialog.class);
        when(mockController.createDialog(InstantAppButtonsController.DLG_CLEAR_APP))
                .thenReturn(mockDialog);
        assertThat(mAppDetail.createDialog(InstantAppButtonsController.DLG_CLEAR_APP, 0))
                .isEqualTo(mockDialog);
        verify(mockController).createDialog(InstantAppButtonsController.DLG_CLEAR_APP);
    }

    // A helper class for testing the InstantAppButtonsController - it lets us look up the
    // preference associated with a key for instant app buttons and get back a mock
    // LayoutPreference (to avoid a null pointer exception).
    public static class InstalledAppDetailsWithMockInstantButtons extends InstalledAppDetails {
        @Mock
        private LayoutPreference mInstantButtons;

        public InstalledAppDetailsWithMockInstantButtons() {
            super();
            MockitoAnnotations.initMocks(this);
        }

        @Override
        public Preference findPreference(CharSequence key) {
            if (key == "instant_app_buttons") {
                return mInstantButtons;
            }
            return super.findPreference(key);
        }
    }

    @Test
    public void instantApps_instantSpecificButtons() {
        // Make this app appear to be instant.
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));
        final PackageInfo packageInfo = mock(PackageInfo.class);

        final InstalledAppDetailsWithMockInstantButtons
                fragment = new InstalledAppDetailsWithMockInstantButtons();
        ReflectionHelpers.setField(fragment, "mPackageInfo", packageInfo);
        ReflectionHelpers.setField(fragment, "mApplicationFeatureProvider",
                mFeatureFactory.applicationFeatureProvider);

        final InstantAppButtonsController buttonsController =
                mock(InstantAppButtonsController.class);
        when(buttonsController.setPackageName(nullable(String.class)))
                .thenReturn(buttonsController);
        when(mFeatureFactory.applicationFeatureProvider.newInstantAppButtonsController(
                nullable(Fragment.class), nullable(View.class), nullable(ShowDialogDelegate.class)))
                .thenReturn(buttonsController);

        fragment.maybeAddInstantAppButtons();
        verify(buttonsController).setPackageName(nullable(String.class));
        verify(buttonsController).show();
    }

    @Test
    public void instantApps_removeCorrectPref() {
        PreferenceScreen mockPreferenceScreen = mock(PreferenceScreen.class);
        PreferenceManager mockPreferenceManager = mock(PreferenceManager.class);
        AppDomainsPreference mockAppDomainsPref = mock(AppDomainsPreference.class);
        Preference mockLaunchPreference = mock(Preference.class);
        PackageInfo mockPackageInfo = mock(PackageInfo.class);
        PackageManager mockPackageManager = mock(PackageManager.class);
        ReflectionHelpers.setField(
                mAppDetail, "mLaunchPreference", mockLaunchPreference);
        ReflectionHelpers.setField(
                mAppDetail, "mInstantAppDomainsPreference", mockAppDomainsPref);
        ReflectionHelpers.setField(
                mAppDetail, "mPreferenceManager", mockPreferenceManager);
        ReflectionHelpers.setField(
                mAppDetail, "mPackageInfo", mockPackageInfo);
        ReflectionHelpers.setField(
                mAppDetail, "mPm", mockPackageManager);
        when(mockPreferenceManager.getPreferenceScreen()).thenReturn(mockPreferenceScreen);

        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> false));
        mAppDetail.prepareInstantAppPrefs();

        // For the non instant case we remove the app domain pref, and leave the launch pref
        verify(mockPreferenceScreen).removePreference(mockAppDomainsPref);
        verify(mockPreferenceScreen, never()).removePreference(mockLaunchPreference);

        // For the instant app case we remove the launch preff, and leave the app domain pref
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                (InstantAppDataProvider) (i -> true));

        mAppDetail.prepareInstantAppPrefs();
        verify(mockPreferenceScreen).removePreference(mockLaunchPreference);
        // Will be 1 still due to above call
        verify(mockPreferenceScreen, times(1))
                .removePreference(mockAppDomainsPref);
    }

    @Test
    public void onActivityResult_uninstalledUpdates_shouldInvalidateOptionsMenu() {
        doReturn(true).when(mAppDetail).refreshUi();

        mAppDetail.onActivityResult(InstalledAppDetails.REQUEST_UNINSTALL, 0, mock(Intent.class));

        verify(mActivity).invalidateOptionsMenu();
    }

    @Test
    public void findTargetSipper_findCorrectSipper() {
        List<BatterySipper> usageList = new ArrayList<>();
        usageList.add(mBatterySipper);
        usageList.add(mOtherBatterySipper);
        doReturn(usageList).when(mBatteryStatsHelper).getUsageList();

        assertThat(mAppDetail.findTargetSipper(mBatteryStatsHelper, TARGET_UID)).isEqualTo(
                mBatterySipper);
    }

    @Test
    public void updateBattery_noBatteryStats_summaryNo() {
        doReturn(mShadowContext.getString(R.string.no_battery_summary)).when(mAppDetail).getString(
                R.string.no_battery_summary);
        mAppDetail.updateBattery();

        assertThat(mBatteryPreference.getSummary()).isEqualTo(
                "No battery use since last full charge");
    }

    @Test
    public void updateBattery_hasBatteryStats_summaryPercent() {
        mAppDetail.mBatteryHelper = mBatteryStatsHelper;
        mAppDetail.mSipper = mBatterySipper;
        doReturn(BATTERY_LEVEL).when(mBatteryUtils).calculateBatteryPercent(anyDouble(),
                anyDouble(), anyDouble(), anyInt());
        doReturn(mShadowContext.getString(R.string.battery_summary, BATTERY_LEVEL_STRING)).when(
                mAppDetail).getString(R.string.battery_summary, BATTERY_LEVEL_STRING);
        doReturn(new ArrayList<>()).when(mBatteryStatsHelper).getUsageList();

        mAppDetail.updateBattery();

        assertThat(mBatteryPreference.getSummary()).isEqualTo("60% use since last full charge");
    }

    @Test
    public void isBatteryStatsAvailable_hasBatteryStatsHelperAndSipper_returnTrue() {
        mAppDetail.mBatteryHelper = mBatteryStatsHelper;
        mAppDetail.mSipper = mBatterySipper;

        assertThat(mAppDetail.isBatteryStatsAvailable()).isTrue();
    }

    @Test
    public void isBatteryStatsAvailable_parametersNull_returnFalse() {
        assertThat(mAppDetail.isBatteryStatsAvailable()).isFalse();
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

        ReflectionHelpers.setField(mAppDetail, "mHomePackages", homePackages);
        ReflectionHelpers.setField(mAppDetail, "mAppEntry", appEntry);
        final Button button = mock(Button.class);

        assertThat(mAppDetail.handleDisableable(button)).isFalse();
        verify(button).setText(R.string.disable_text);
    }

    @Test
    public void handleDisableable_appIsEnabled_buttonShouldWork() {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = "pkg";
        info.enabled = true;
        info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        when(mFeatureFactory.applicationFeatureProvider.getKeepEnabledPackages()).thenReturn(
                new HashSet<>());

        ReflectionHelpers.setField(mAppDetail, "mApplicationFeatureProvider",
                mFeatureFactory.applicationFeatureProvider);
        ReflectionHelpers.setField(mAppDetail, "mAppEntry", appEntry);
        final Button button = mock(Button.class);

        assertThat(mAppDetail.handleDisableable(button)).isTrue();
        verify(button).setText(R.string.disable_text);
    }

    @Test
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

        ReflectionHelpers.setField(mAppDetail, "mApplicationFeatureProvider",
                mFeatureFactory.applicationFeatureProvider);
        ReflectionHelpers.setField(mAppDetail, "mAppEntry", appEntry);

        final Button button = mock(Button.class);

        assertThat(mAppDetail.handleDisableable(button)).isFalse();
        verify(button).setText(R.string.disable_text);
    }

    @Test
    public void testRestartBatteryStatsLoader() {
        doReturn(mLoaderManager).when(mAppDetail).getLoaderManager();

        mAppDetail.restartBatteryStatsLoader();

        verify(mLoaderManager).restartLoader(InstalledAppDetails.LOADER_BATTERY, Bundle.EMPTY,
                mAppDetail.mBatteryCallbacks);
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
