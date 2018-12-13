/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings.Secure;
import android.view.View;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class NightDisplayActivationPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    private LayoutPreference mPreference;
    private Context mContext;
    private NightDisplayActivationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new LayoutPreference(mContext, R.layout.night_display_activation_button);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new NightDisplayActivationPreferenceController(mContext,
            "night_display_activation");
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void isAvailable_configuredAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_nightDisplayAvailable, true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configuredUnavailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_nightDisplayAvailable, false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final NightDisplayActivationPreferenceController controller =
                new NightDisplayActivationPreferenceController(mContext,"night_display_activated");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final NightDisplayActivationPreferenceController controller =
                new NightDisplayActivationPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void onClick_activates() {
        Secure.putInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, 0);

        final View view = mPreference.findViewById(R.id.night_display_turn_on_button);
        assertThat(view.getVisibility()).isEqualTo(View.VISIBLE);
        view.performClick();

        assertThat(Secure.getInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, -1))
                .isEqualTo(1);
    }

    @Test
    public void onClick_deactivates() {
        Secure.putInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, 1);

        final View view = mPreference.findViewById(R.id.night_display_turn_on_button);
        assertThat(view.getVisibility()).isEqualTo(View.VISIBLE);
        view.performClick();

        assertThat(Secure.getInt(mContext.getContentResolver(), Secure.NIGHT_DISPLAY_ACTIVATED, -1))
                .isEqualTo(0);
    }
}
