/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.emergency;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class MoreSettingsPreferenceControllerTest {

    private static final String TEST_PKG_NAME = "test_pkg";
    private static final String TEST_CLASS_NAME = "name";

    private static final Intent SETTING_INTENT = new Intent(Intent.ACTION_MAIN)
            .setPackage(TEST_PKG_NAME);

    private Context mContext;
    private ShadowPackageManager mPackageManager;
    private static final String PREF_KEY = "more_settings_button";

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPackageManager = Shadows.shadowOf(mContext.getPackageManager());
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void constructor_hasPackageConfig_shouldSetIntent() {
        // Provide override package name and provide resolvable intent
        SettingsShadowResources.overrideResource(
                R.string.config_emergency_package_name,
                TEST_PKG_NAME);
        prepareCustomIntent();

        MoreSettingsPreferenceController controller = new MoreSettingsPreferenceController(
                mContext, PREF_KEY);

        assertThat(controller.mIntent).isNotNull();
    }

    @Test
    public void getAvailabilityStatus_whenIntentSet_shouldReturnAvailable() {
        // Provide override package name and provide resolvable intent
        SettingsShadowResources.overrideResource(
                R.string.config_emergency_package_name,
                TEST_PKG_NAME);
        prepareCustomIntent();

        MoreSettingsPreferenceController controller = new MoreSettingsPreferenceController(
                mContext, PREF_KEY);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_whenPackageConfigIsUnavailable_shouldReturnUnsupported() {
        // No package name is configured
        MoreSettingsPreferenceController controller = new MoreSettingsPreferenceController(
                mContext, PREF_KEY);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_noSuitableIntent_shouldReturnUnsupportedDevice() {
        // Provide override package name but don't provide resolvable intent
        SettingsShadowResources.overrideResource(
                R.string.config_emergency_package_name,
                TEST_PKG_NAME);

        MoreSettingsPreferenceController controller = new MoreSettingsPreferenceController(
                mContext, PREF_KEY);

        assertThat(controller.mIntent).isNull();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_intentForANonSystemApp_shouldReturnUnsupportedDevice() {
        // Provide override package name and provide resolvable intent for a non system app.
        SettingsShadowResources.overrideResource(
                R.string.config_emergency_package_name,
                TEST_PKG_NAME);
        prepareCustomIntentForANonSystemApp();

        MoreSettingsPreferenceController controller = new MoreSettingsPreferenceController(
                mContext, PREF_KEY);

        assertThat(controller.mIntent).isNull();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    private void prepareCustomIntent() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = TEST_PKG_NAME;
        info.activityInfo.name = TEST_CLASS_NAME;

        // Resolve to a system app.
        info.activityInfo.applicationInfo = new ApplicationInfo();
        info.activityInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        mPackageManager.addResolveInfoForIntent(SETTING_INTENT, info);
    }


    private void prepareCustomIntentForANonSystemApp() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = TEST_PKG_NAME;
        info.activityInfo.name = TEST_CLASS_NAME;

        mPackageManager.addResolveInfoForIntent(SETTING_INTENT, info);
    }
}
