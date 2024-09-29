/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.accessibility.Flags;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Tests for {@link AutoBrightnessPreferenceControllerForSetupWizard}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class AutoBrightnessPreferenceControllerForSetupWizardTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "auto_brightness";

    private Context mContext;
    private AutoBrightnessPreferenceControllerForSetupWizard mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController =
                new AutoBrightnessPreferenceControllerForSetupWizard(mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_configTrueAndFlagOn_shouldReturnAvailableUnsearchable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_automatic_brightness_available, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_configFalseSetAndFlagOn_shouldReturnUnsupportedOnDevice() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_automatic_brightness_available, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @DisableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_flagOff_shouldReturnConditionallyUnavailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_automatic_brightness_available, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}

