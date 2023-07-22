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
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_SPLIT_SCREEN;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_UNSET;

import static com.android.settings.applications.appcompat.UserAspectRatioManager.KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.provider.DeviceConfig;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * To run this test: atest SettingsUnitTests:UserAspectRatioManagerTest
 */
@RunWith(AndroidJUnit4.class)
public class UserAspectRatioManagerTest {

    private Context mContext;
    private UserAspectRatioManager mUtils;
    private String mOriginalFlag;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mUtils = spy(new UserAspectRatioManager(mContext));
        mOriginalFlag = DeviceConfig.getProperty(DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS);
    }

    @After
    public void tearDown() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS, mOriginalFlag, true /* makeDefault */);
    }

    @Test
    public void testCanDisplayAspectRatioUi() {
        final ApplicationInfo canDisplay = new ApplicationInfo();
        canDisplay.packageName = "com.app.candisplay";
        addResolveInfoLauncherEntry(canDisplay.packageName);

        assertTrue(mUtils.canDisplayAspectRatioUi(canDisplay));

        final ApplicationInfo noLauncherEntry = new ApplicationInfo();
        noLauncherEntry.packageName = "com.app.nolauncherentry";

        assertFalse(mUtils.canDisplayAspectRatioUi(noLauncherEntry));
    }

    @Test
    public void testIsFeatureEnabled() {
        assertFalse(UserAspectRatioManager.isFeatureEnabled(mContext));

        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                KEY_ENABLE_USER_ASPECT_RATIO_SETTINGS, "true", false /* makeDefault */);
        assertTrue(UserAspectRatioManager.isFeatureEnabled(mContext));
    }

    @Test
    public void testGetUserMinAspectRatioEntry() {
        // R.string.user_aspect_ratio_app_default
        final String appDefault = ResourcesUtils.getResourcesString(mContext,
                "user_aspect_ratio_app_default");
        assertThat(mUtils.getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_UNSET))
                .isEqualTo(appDefault);
        // should always return default if value does not correspond to anything
        assertThat(mUtils.getUserMinAspectRatioEntry(-1))
                .isEqualTo(appDefault);
        // R.string.user_aspect_ratio_half_screen
        assertThat(mUtils.getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_SPLIT_SCREEN))
                .isEqualTo(ResourcesUtils.getResourcesString(mContext,
                        "user_aspect_ratio_half_screen"));
        // R.string.user_aspect_ratio_3_2
        assertThat(mUtils.getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_3_2))
                .isEqualTo(ResourcesUtils.getResourcesString(mContext, "user_aspect_ratio_3_2"));
        // R,string.user_aspect_ratio_4_3
        assertThat(mUtils.getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_4_3))
                .isEqualTo(ResourcesUtils.getResourcesString(mContext, "user_aspect_ratio_4_3"));
        // R.string.user_aspect_ratio_16_9
        assertThat(mUtils.getUserMinAspectRatioEntry(USER_MIN_ASPECT_RATIO_16_9))
                .isEqualTo(ResourcesUtils.getResourcesString(mContext, "user_aspect_ratio_16_9"));
    }

    private void addResolveInfoLauncherEntry(String packageName) {
        final ResolveInfo resolveInfo = mock(ResolveInfo.class);
        final ActivityInfo activityInfo = mock(ActivityInfo.class);
        activityInfo.packageName = packageName;
        resolveInfo.activityInfo = activityInfo;
        mUtils.addInfoHasLauncherEntry(resolveInfo);
    }
}
