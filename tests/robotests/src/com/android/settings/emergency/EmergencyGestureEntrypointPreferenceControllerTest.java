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
import static com.android.settings.emergency.EmergencyGestureEntrypointPreferenceController.ACTION_EMERGENCY_GESTURE_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
public class EmergencyGestureEntrypointPreferenceControllerTest {

    private static final String TEST_PKG_NAME = "test_pkg";
    private static final String TEST_CLASS_NAME = "name";
    private static final Intent SETTING_INTENT = new Intent(ACTION_EMERGENCY_GESTURE_SETTINGS)
            .setPackage(TEST_PKG_NAME);

    private Context mContext;
    private ShadowPackageManager mPackageManager;
    private static final String PREF_KEY = "gesture_emergency_button";

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
    public void constructor_hasCustomPackageConfig_shouldSetIntent() {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_emergency_gesture_settings,
                Boolean.TRUE);
        SettingsShadowResources.overrideResource(
                R.string.emergency_gesture_settings_package,
                TEST_PKG_NAME);
        prepareCustomIntent();

        EmergencyGestureEntrypointPreferenceController controller =
                new EmergencyGestureEntrypointPreferenceController(mContext, PREF_KEY);

        assertThat(controller.mIntent).isNotNull();
    }

    @Test
    public void getAvailabilityStatus_configIsTrue_shouldReturnAvailable() {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_emergency_gesture_settings,
                Boolean.TRUE);
        EmergencyGestureEntrypointPreferenceController controller =
                new EmergencyGestureEntrypointPreferenceController(mContext, PREF_KEY);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_configIsFalse_shouldReturnUnsupported() {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_emergency_gesture_settings,
                Boolean.FALSE);
        EmergencyGestureEntrypointPreferenceController controller =
                new EmergencyGestureEntrypointPreferenceController(mContext, PREF_KEY);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_noSuitableIntent_shouldReturnAvailable() {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_emergency_gesture_settings,
                Boolean.TRUE);
        // Provide override package name but don't provide resolvable intent
        SettingsShadowResources.overrideResource(
                R.string.emergency_gesture_settings_package,
                TEST_PKG_NAME);

        EmergencyGestureEntrypointPreferenceController controller =
                new EmergencyGestureEntrypointPreferenceController(mContext, PREF_KEY);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(controller.mIntent).isNull();
    }

    private void prepareCustomIntent() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = TEST_PKG_NAME;
        info.activityInfo.name = TEST_CLASS_NAME;

        mPackageManager.addResolveInfoForIntent(SETTING_INTENT, info);
    }
}
