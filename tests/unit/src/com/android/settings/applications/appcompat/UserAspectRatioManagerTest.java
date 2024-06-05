/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.applications.appcompat;

import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_16_9;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_3_2;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_4_3;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_APP_DEFAULT;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_DISPLAY_SIZE;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;
import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;
import static android.view.WindowManager.PROPERTY_COMPAT_ALLOW_ORIENTATION_OVERRIDE;
import static android.view.WindowManager.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE;
import static android.view.WindowManager.PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE;

import static com.android.settings.applications.appcompat.UserAspectRatioManager.KEY_ENABLE_USER_ASPECT_RATIO_FULLSCREEN;
import static com.android.settings.applications.appcompat.UserAspectRatioManager.KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS;
import static com.android.window.flags.Flags.FLAG_USER_MIN_ASPECT_RATIO_APP_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.RemoteException;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.DeviceConfig;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * To run this test: atest SettingsUnitTests:UserAspectRatioManagerTest
 */
@RunWith(AndroidJUnit4.class)
public class UserAspectRatioManagerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);
    private final String mPackageName = "com.test.mypackage";
    private Context mContext;
    private Resources mResources;
    private FakeUserAspectRatioManager mUtils;
    private String mOriginalSettingsFlag;
    private String mOriginalFullscreenFlag;
    private IPackageManager mIPm;
    private PackageManager mPm;
    private List<LauncherActivityInfo> mLauncherActivities;

    @Before
    public void setUp() throws RemoteException, PackageManager.NameNotFoundException {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        final LauncherApps launcherApps = mock(LauncherApps.class);
        mLauncherActivities = mock(List.class);
        mIPm = mock(IPackageManager.class);
        mPm = mock(PackageManager.class);
        when(mContext.getPackageManager()).thenReturn(mPm);

        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(LauncherApps.class)).thenReturn(launcherApps);
        enableAllDefaultAspectRatioOptions();
        mSetFlagsRule.disableFlags(FLAG_USER_MIN_ASPECT_RATIO_APP_DEFAULT);

        mUtils = new FakeUserAspectRatioManager(mContext, mIPm);

        doReturn(mLauncherActivities).when(launcherApps).getActivityList(anyString(), any());

        mOriginalSettingsFlag = DeviceConfig.getProperty(
                DeviceConfig.NAMESPACE_WINDOW_MANAGER, KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS);
        setAspectRatioSettingsBuildTimeFlagEnabled(true);
        setAspectRatioSettingsDeviceConfigEnabled("true" /* enabled */, false /* makeDefault */);

        mOriginalFullscreenFlag = DeviceConfig.getProperty(
                DeviceConfig.NAMESPACE_WINDOW_MANAGER, KEY_ENABLE_USER_ASPECT_RATIO_FULLSCREEN);
        setAspectRatioFullscreenBuildTimeFlagEnabled(true);
        setAspectRatioFullscreenDeviceConfigEnabled("true" /* enabled */, false /* makeDefault */);

        mockProperty(PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE, true);
        mockProperty(PROPERTY_COMPAT_ALLOW_ORIENTATION_OVERRIDE, true);
        mockProperty(PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE, true);
    }

    @After
    public void tearDown() {
        setAspectRatioSettingsDeviceConfigEnabled(mOriginalSettingsFlag, true /* makeDefault */);
        setAspectRatioFullscreenDeviceConfigEnabled(mOriginalFullscreenFlag,
                true /* makeDefault */);
    }

    @Test
    public void testCanDisplayAspectRatioUi() {
        final ApplicationInfo canDisplay = new ApplicationInfo();
        canDisplay.packageName = mPackageName;

        doReturn(false).when(mLauncherActivities).isEmpty();
        assertTrue(mUtils.canDisplayAspectRatioUi(canDisplay));

        final ApplicationInfo noLauncherEntry = new ApplicationInfo();
        noLauncherEntry.packageName = mPackageName;

        doReturn(true).when(mLauncherActivities).isEmpty();
        assertFalse(mUtils.canDisplayAspectRatioUi(noLauncherEntry));
    }

    @Test
    public void testCanDisplayAspectRatioUi_hasLauncher_propertyFalse_returnFalse()
            throws PackageManager.NameNotFoundException {
        mockProperty(PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE, false);
        doReturn(false).when(mLauncherActivities).isEmpty();

        final ApplicationInfo canDisplay = new ApplicationInfo();
        canDisplay.packageName = mPackageName;

        assertFalse(mUtils.canDisplayAspectRatioUi(canDisplay));
    }

    @Test
    public void testCanDisplayAspectRatioUi_noLauncher_propertyTrue_returnFalse()
            throws PackageManager.NameNotFoundException {
        mockProperty(PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_OVERRIDE, true);
        doReturn(true).when(mLauncherActivities).isEmpty();

        final ApplicationInfo noLauncherEntry = new ApplicationInfo();
        noLauncherEntry.packageName = mPackageName;

        assertFalse(mUtils.canDisplayAspectRatioUi(noLauncherEntry));
    }

    @Test
    public void testIsFeatureEnabled() {
        assertTrue(UserAspectRatioManager.isFeatureEnabled(mContext));
    }

    @Test
    public void testIsFeatureEnabled_disabledBuildTimeFlag_returnFalse() {
        setAspectRatioSettingsBuildTimeFlagEnabled(false);
        assertFalse(UserAspectRatioManager.isFeatureEnabled(mContext));
    }

    @Test
    public void testIsFeatureEnabled_disabledRuntimeFlag_returnFalse() {
        setAspectRatioSettingsDeviceConfigEnabled("false" /* enabled */, false /* makeDefault */);
        assertFalse(UserAspectRatioManager.isFeatureEnabled(mContext));
    }

    @Test
    public void testIsFullscreenOptionEnabled() {
        assertTrue(mUtils.isFullscreenOptionEnabled(mPackageName));
    }

    @Test
    public void testIsFullscreenOptionEnabled_settingsDisabled_returnFalse() {
        setAspectRatioFullscreenBuildTimeFlagEnabled(false);
        assertFalse(mUtils.isFullscreenOptionEnabled(mPackageName));
    }

    @Test
    public void testIsFullscreenOptionEnabled_disabledBuildTimeFlag_returnFalse() {
        setAspectRatioFullscreenBuildTimeFlagEnabled(false);
        assertFalse(mUtils.isFullscreenOptionEnabled(mPackageName));
    }

    @Test
    public void testIsFullscreenOptionEnabled_disabledRuntimeFlag_returnFalse() {
        setAspectRatioFullscreenDeviceConfigEnabled("false" /* enabled */, false /*makeDefault */);
        assertFalse(mUtils.isFullscreenOptionEnabled(mPackageName));
    }

    @Test
    public void testIsFullscreenOptionEnabled_propertyFalse_returnsFalse()
            throws PackageManager.NameNotFoundException {
        mockProperty(PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE, false);
        assertFalse(mUtils.isFullscreenOptionEnabled(mPackageName));
    }

    @Test
    public void testIsFullscreenOptionEnabled_propertyTrue_configDisabled_returnsFalse()
            throws PackageManager.NameNotFoundException {
        mockProperty(PROPERTY_COMPAT_ALLOW_USER_ASPECT_RATIO_FULLSCREEN_OVERRIDE, true);
        setAspectRatioFullscreenDeviceConfigEnabled("false" /* enabled */, false /*makeDefault */);

        assertFalse(mUtils.isFullscreenOptionEnabled(mPackageName));
    }

    @Test
    public void testHasAspectRatioOption_fullscreen() {
        assertTrue(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_FULLSCREEN,
                mPackageName));
        assertTrue(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_SPLIT_SCREEN,
                mPackageName));

        // Only fullscreen option should be disabled
        when(mUtils.isFullscreenOptionEnabled(mPackageName)).thenReturn(false);
        assertFalse(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_FULLSCREEN,
                mPackageName));
        assertTrue(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_SPLIT_SCREEN,
                mPackageName));
    }

    private String getUserMinAspectRatioEntry(int aspectRatio, String packageName) {
        return mUtils.getUserMinAspectRatioEntry(aspectRatio, packageName, mContext.getUserId());
    }

    @Test
    public void testGetUserMinAspectRatioEntry() {
        final Context context = ApplicationProvider.getApplicationContext();
        // R.string.user_aspect_ratio_app_default
        final String appDefault = ResourcesUtils.getResourcesString(context,
                "user_aspect_ratio_app_default");
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_UNSET, mPackageName))
                .isEqualTo(appDefault);
        // should always return default if value does not correspond to anything
        assertThat(getUserMinAspectRatioEntry(-1, mPackageName))
                .isEqualTo(appDefault);
        // R.string.user_aspect_ratio_half_screen
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_SPLIT_SCREEN,
                mPackageName)).isEqualTo(ResourcesUtils.getResourcesString(context,
                        "user_aspect_ratio_half_screen"));
        // R.string.user_aspect_ratio_display_size
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_DISPLAY_SIZE,
                mPackageName)).isEqualTo(ResourcesUtils.getResourcesString(context,
                        "user_aspect_ratio_device_size"));
        // R.string.user_aspect_ratio_16_9
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_16_9, mPackageName))
                .isEqualTo(ResourcesUtils.getResourcesString(context, "user_aspect_ratio_16_9"));
        // R.string.user_aspect_ratio_4_3
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_4_3, mPackageName))
                .isEqualTo(ResourcesUtils.getResourcesString(context, "user_aspect_ratio_4_3"));
        // R.string.user_aspect_ratio_3_2
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_3_2, mPackageName))
                .isEqualTo(ResourcesUtils.getResourcesString(context, "user_aspect_ratio_3_2"));
        // R.string.user_aspect_ratio_fullscreen
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_FULLSCREEN,
                mPackageName)).isEqualTo(ResourcesUtils.getResourcesString(context,
                "user_aspect_ratio_fullscreen"));
    }

    @Test
    public void testGetUserMinAspectRatioEntry_fullscreenDisabled_shouldReturnDefault() {
        setAspectRatioFullscreenBuildTimeFlagEnabled(false);
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_FULLSCREEN,
                mPackageName)).isEqualTo(ResourcesUtils.getResourcesString(
                        ApplicationProvider.getApplicationContext(),
                        "user_aspect_ratio_app_default"));
    }

    @Test
    public void testGetUserMinAspectRatioEntry_nonDefaultString_shouldReturnNewString() {
        final String newOptionName = "new_option_name";
        when(mResources.getIntArray(anyInt())).thenReturn(new int[] {USER_MIN_ASPECT_RATIO_UNSET});
        when(mResources.getStringArray(anyInt())).thenReturn(new String[] {newOptionName});

        mUtils = new FakeUserAspectRatioManager(mContext, mIPm);

        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_UNSET, mPackageName))
                .isEqualTo(newOptionName);
    }


    @Test
    public void testGetUserMinAspectRatioMapping_noAppDefault_shouldThrowException() {
        when(mResources.getIntArray(anyInt())).thenReturn(new int[] {USER_MIN_ASPECT_RATIO_4_3});
        when(mResources.getStringArray(anyInt())).thenReturn(new String[] {"4:3"});

        assertThrows(RuntimeException.class, () -> new FakeUserAspectRatioManager(mContext, mIPm));
    }

    @Test
    public void testGetUserMinAspectRatioMapping_configLengthMismatch_shouldThrowException() {
        when(mResources.getIntArray(anyInt())).thenReturn(new int[] {
                USER_MIN_ASPECT_RATIO_UNSET,
                USER_MIN_ASPECT_RATIO_4_3});
        when(mResources.getStringArray(anyInt())).thenReturn(new String[] {"4:3"});

        assertThrows(RuntimeException.class, () -> new FakeUserAspectRatioManager(mContext, mIPm));
    }

    @Test
    public void testGetUserMinAspectRatioMapping_appDefaultFlagEnabled() {
        // Flag is disabled by default, app default not loaded
        assertFalse(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_APP_DEFAULT, mPackageName));

        mSetFlagsRule.enableFlags(FLAG_USER_MIN_ASPECT_RATIO_APP_DEFAULT);
        mUtils = new FakeUserAspectRatioManager(mContext, mIPm);

        assertTrue(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_APP_DEFAULT, mPackageName));
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_APP_DEFAULT, mPackageName))
                .isEqualTo(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_UNSET, mPackageName));
    }

    @Test
    public void testGetUserMinAspectRatioEntry_enabledFullscreenOverride_returnsFullscreen() {
        setIsOverrideToFullscreenEnabled(true);
        // Fullscreen option is pre-selected
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_UNSET, mPackageName))
                .isEqualTo(ResourcesUtils.getResourcesString(
                        ApplicationProvider.getApplicationContext(),
                        "user_aspect_ratio_fullscreen"));

        // App default exists
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_APP_DEFAULT, mPackageName))
                .isEqualTo(ResourcesUtils.getResourcesString(
                        ApplicationProvider.getApplicationContext(),
                        "user_aspect_ratio_app_default"));
    }

    @Test
    public void testGetUserMinAspectRatioEntry_disabledFullscreenOverride_returnsUnchanged() {
        setIsOverrideToFullscreenEnabled(false);
        // Fullscreen option is not pre-selected
        assertThat(getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_UNSET, mPackageName))
                .isEqualTo(ResourcesUtils.getResourcesString(
                        ApplicationProvider.getApplicationContext(),
                        "user_aspect_ratio_app_default"));
    }

    @Test
    public void testIsOverrideToFullscreenEnabled_returnsTrue()
            throws PackageManager.NameNotFoundException {
        setIsOverrideToFullscreenEnabled(true);
        assertTrue(mUtils.isOverrideToFullscreenEnabled(mPackageName, mContext.getUserId()));

        mockProperty(PROPERTY_COMPAT_ALLOW_ORIENTATION_OVERRIDE, true);
        assertTrue(mUtils.isOverrideToFullscreenEnabled(mPackageName, mContext.getUserId()));
    }

    @Test
    public void testIsOverrideToFullscreenEnabled_optOut_returnsFalse()
            throws PackageManager.NameNotFoundException {
        setIsOverrideToFullscreenEnabled(true);
        mockProperty(PROPERTY_COMPAT_ALLOW_ORIENTATION_OVERRIDE, false);
        assertFalse(mUtils.isOverrideToFullscreenEnabled(mPackageName, mContext.getUserId()));
    }

    @Test
    public void testIsOverrideToFullscreenEnabled_flagDisabled_returnsFalse() {
        mUtils.setFullscreenCompatChange(true);
        assertFalse(mUtils.isOverrideToFullscreenEnabled(mPackageName, mContext.getUserId()));
    }

    @Test
    public void testIsOverrideToFullscreenEnabled_optionDisabled_returnsFalse() {
        mUtils.setFullscreenCompatChange(true);
        when(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_FULLSCREEN, mPackageName))
                .thenReturn(false);
        assertFalse(mUtils.isOverrideToFullscreenEnabled(mPackageName, mContext.getUserId()));
    }

    private void setIsOverrideToFullscreenEnabled(boolean enabled) {
        if (enabled) {
            mSetFlagsRule.enableFlags(FLAG_USER_MIN_ASPECT_RATIO_APP_DEFAULT);
            mUtils = new FakeUserAspectRatioManager(mContext, mIPm);
        }
        mUtils.setFullscreenCompatChange(enabled);
        when(mUtils.hasAspectRatioOption(USER_MIN_ASPECT_RATIO_FULLSCREEN, mPackageName))
                .thenReturn(enabled);
    }

    private void enableAllDefaultAspectRatioOptions() {
        final int[] aspectRatioOptions = new int[] {
                USER_MIN_ASPECT_RATIO_UNSET,
                USER_MIN_ASPECT_RATIO_SPLIT_SCREEN,
                USER_MIN_ASPECT_RATIO_DISPLAY_SIZE,
                USER_MIN_ASPECT_RATIO_4_3,
                USER_MIN_ASPECT_RATIO_16_9,
                USER_MIN_ASPECT_RATIO_3_2,
                USER_MIN_ASPECT_RATIO_FULLSCREEN};
        when(mResources.getIntArray(anyInt())).thenReturn(aspectRatioOptions);
        // String array config overlay with @null values so default strings should be used
        when(mResources.getStringArray(anyInt())).thenReturn(new String[aspectRatioOptions.length]);

        final Context context = ApplicationProvider.getApplicationContext();
        mockString(context, "user_aspect_ratio_app_default");
        mockString(context, "user_aspect_ratio_half_screen");
        mockString(context, "user_aspect_ratio_device_size");
        mockString(context, "user_aspect_ratio_4_3");
        mockString(context, "user_aspect_ratio_16_9");
        mockString(context, "user_aspect_ratio_3_2");
        mockString(context, "user_aspect_ratio_fullscreen");
    }

    private void mockString(Context context, String stringResName) {
        final int resId = ResourcesUtils.getResourcesId(context, "string", stringResName);
        final String string = ResourcesUtils.getResourcesString(context, stringResName);
        when(mContext.getString(resId)).thenReturn(string);
    }

    private void mockProperty(String propertyName, boolean value)
            throws PackageManager.NameNotFoundException {
        PackageManager.Property prop = new PackageManager.Property(
                propertyName, value, mPackageName, "" /* className */);
        when(mPm.getProperty(propertyName, mPackageName)).thenReturn(prop);
    }

    private void setAspectRatioSettingsBuildTimeFlagEnabled(boolean enabled) {
        when(mResources.getBoolean(R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled))
                .thenReturn(enabled);
    }

    private void setAspectRatioSettingsDeviceConfigEnabled(String enabled, boolean makeDefault) {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS, enabled, makeDefault);
    }

    private void setAspectRatioFullscreenBuildTimeFlagEnabled(boolean enabled) {
        when(mResources.getBoolean(R.bool.config_appCompatUserAppAspectRatioFullscreenIsEnabled))
                .thenReturn(enabled);
    }

    private void setAspectRatioFullscreenDeviceConfigEnabled(String enabled, boolean makeDefault) {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                KEY_ENABLE_USER_ASPECT_RATIO_FULLSCREEN, enabled, makeDefault);
    }

    private static class FakeUserAspectRatioManager extends UserAspectRatioManager {
        private boolean mFullscreenCompatChange = false;

        private FakeUserAspectRatioManager(@NonNull Context context, IPackageManager pm) {
            super(context, pm);
        }

        @Override
        boolean isFullscreenCompatChangeEnabled(String pkgName, int userId) {
            return mFullscreenCompatChange;
        }

        void setFullscreenCompatChange(boolean enabled) {
            mFullscreenCompatChange = enabled;
        }
    }
}
