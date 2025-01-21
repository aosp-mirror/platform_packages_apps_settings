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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

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
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests for {@link BrightnessLevelPreferenceControllerForSetupWizard}.
 */
@RunWith(RobolectricTestRunner.class)
public class BrightnessLevelPreferenceControllerForSetupWizardTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private BrightnessLevelPreferenceControllerForSetupWizard mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new BrightnessLevelPreferenceControllerForSetupWizard(mContext,
                /* lifecycle= */ null);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void displayPreference_flagOn_preferenceVisibleTrue() {
        Preference preference = displayPreference(/* restricted= */ false);

        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void displayPreference_flagOnAndRestricted_preferenceVisibleFalse() {
        Preference preference = displayPreference(/* restricted= */ true);

        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void displayPreference_flagOff_preferenceVisibleFalse() {
        Preference preference = displayPreference(/* restricted= */ false);

        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_flagOn_available() {
        displayPreference(/* restricted= */ false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_flagOnAndRestricted_conditionallyUnavailable() {
        displayPreference(/* restricted= */ true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_ADD_BRIGHTNESS_SETTINGS_IN_SUW)
    public void getAvailabilityStatus_flagOff_conditionallyUnavailable() {
        displayPreference(/* restricted= */ false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    private RestrictedPreference displayPreference(boolean restricted) {
        final PreferenceManager manager = new PreferenceManager(mContext);
        final PreferenceScreen screen = manager.createPreferenceScreen(mContext);
        final RestrictedPreference preference = new RestrictedPreference(mContext);
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
