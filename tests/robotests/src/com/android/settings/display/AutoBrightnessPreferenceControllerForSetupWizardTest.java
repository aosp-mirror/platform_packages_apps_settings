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

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.accessibility.Flags;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

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
    public void displayPreference_flagOn_preferenceVisibleTrue() {
        Preference preference =
                displayPreference(/* configAvailable= */ true, /* restricted= */ false);
        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void displayPreference_flagOnAndRestricted_preferenceVisibleFalse() {
        Preference preference =
                displayPreference(/* configAvailable= */ true, /* restricted= */ true);
        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_configTrueAndFlagOn_availableUnsearchable() {
        displayPreference(/* configAvailable= */ true, /* restricted= */ false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_configTrueAndFlagOnAndRestricted_conditionallyUnavailable() {
        displayPreference(/* configAvailable= */ true, /* restricted= */ true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_configFalseAndFlagOn_unsupportedOnDevice() {
        displayPreference(/* configAvailable= */ false, /* restricted= */ false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_configFalseAndFlagOnAndRestricted_conditionallyUnavailable() {
        displayPreference(/* configAvailable= */ false, /* restricted= */ true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_flagOff_conditionallyUnavailable() {
        displayPreference(/* configAvailable= */ true, /* restricted= */ false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    private RestrictedSwitchPreference displayPreference(
            boolean configAvailable, boolean restricted) {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_automatic_brightness_available, configAvailable);

        final PreferenceManager manager = new PreferenceManager(mContext);
        final PreferenceScreen screen = manager.createPreferenceScreen(mContext);
        final RestrictedSwitchPreference preference = new RestrictedSwitchPreference(mContext);
        preference.setKey(mController.getPreferenceKey());
        preference.setDisabledByAdmin(restricted
                ? mock(RestrictedLockUtils.EnforcedAdmin.class)
                : null);
        assertThat(preference.isDisabledByAdmin()).isEqualTo(restricted);
        screen.addPreference(preference);

        mController.displayPreference(screen);
        return preference;
    }
}

