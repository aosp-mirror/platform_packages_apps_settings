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

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.accessibility.Flags;
import com.android.settings.widget.SeekBarPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link DaltonizerSaturationSeekbarPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class DaltonizerSaturationSeekbarPreferenceControllerTest {

    private ContentResolver mContentResolver;
    private DaltonizerSaturationSeekbarPreferenceController mController;

    private int mOriginalSaturationLevel = -1;

    private PreferenceScreen mScreen;

    @Mock
    private SeekBarPreference mPreference;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();
        mContentResolver = context.getContentResolver();
        mOriginalSaturationLevel = Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7);

        mScreen = spy(new PreferenceScreen(context, /* attrs= */ null));
        when(mScreen.findPreference(ToggleDaltonizerPreferenceFragment.KEY_SATURATION))
                .thenReturn(mPreference);

        mController = new DaltonizerSaturationSeekbarPreferenceController(
                context,
                ToggleDaltonizerPreferenceFragment.KEY_SATURATION);
    }

    @After
    public void cleanup() {
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                mOriginalSaturationLevel);
    }


    @Test
    @DisableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagDisabled_unavailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void getAvailabilityStatus_flagEnabled_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void constructor_defaultValuesMatch() {
        assertThat(mController.getSliderPosition()).isEqualTo(7);
        assertThat(mController.getMax()).isEqualTo(10);
        assertThat(mController.getMin()).isEqualTo(0);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void displayPreference_enabled_visible() {
        mController.displayPreference(mScreen);

        verify(mPreference).setMax(eq(10));
        verify(mPreference).setMin(eq(0));
        verify(mPreference).setProgress(eq(7));
        verify(mPreference).setContinuousUpdates(eq(true));
        verify(mPreference).setOnPreferenceChangeListener(eq(mController));
        verify(mPreference).setVisible(eq(true));
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_COLOR_CORRECTION_SATURATION)
    public void displayPreference_disabled_notVisible() {
        mController.displayPreference(mScreen);

        verify(mPreference).setMax(eq(10));
        verify(mPreference).setMin(eq(0));
        verify(mPreference).setProgress(eq(7));
        verify(mPreference).setContinuousUpdates(eq(true));
        verify(mPreference, never()).setOnPreferenceChangeListener(any());
        verify(mPreference).setVisible(eq(false));
    }

    @Test
    public void setSliderPosition_inRange_secureSettingsUpdated() {
        var isSliderSet = mController.setSliderPosition(9);

        assertThat(isSliderSet).isTrue();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(9);
    }

    @Test
    public void setSliderPosition_min_secureSettingsUpdated() {
        var isSliderSet = mController.setSliderPosition(0);

        assertThat(isSliderSet).isTrue();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(0);
    }

    @Test
    public void setSliderPosition_max_secureSettingsUpdated() {
        var isSliderSet = mController.setSliderPosition(10);

        assertThat(isSliderSet).isTrue();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(10);
    }

    @Test
    public void setSliderPosition_tooLarge_secureSettingsNotUpdated() {
        var isSliderSet = mController.setSliderPosition(11);

        assertThat(isSliderSet).isFalse();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(7);
    }

    @Test
    public void setSliderPosition_tooSmall_secureSettingsNotUpdated() {
        var isSliderSet = mController.setSliderPosition(-1);

        assertThat(isSliderSet).isFalse();
        assertThat(Settings.Secure.getInt(
                mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                7)).isEqualTo(7);
    }
}
