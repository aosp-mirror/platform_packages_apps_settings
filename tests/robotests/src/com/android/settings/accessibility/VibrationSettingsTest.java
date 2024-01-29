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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link VibrationSettings} fragment. */
@RunWith(RobolectricTestRunner.class)
@RequiresFlagsEnabled(Flags.FLAG_SEPARATE_ACCESSIBILITY_VIBRATION_SETTINGS_FRAGMENTS)
public class VibrationSettingsTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private Context mContext;
    private Resources mResources;
    private VibrationSettings mFragment;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        mFragment = new VibrationSettings();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_VIBRATION);
    }

    @Test
    public void getHelpResource_returnsCorrectString() {
        assertThat(mFragment.getHelpResource()).isEqualTo(
                R.string.help_uri_accessibility_vibration);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_vibration_settings);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("VibrationSettings");
    }

    @Test
    public void isPageSearchEnabled_oneIntensityLevel_returnsTrue() {
        when(mResources.getInteger(R.integer.config_vibration_supported_intensity_levels))
                .thenReturn(1);
        assertThat(VibrationSettings.isPageSearchEnabled(mContext)).isTrue();
    }

    @Test
    public void isPageSearchEnabled_multipleIntensityLevels_returnsFalse() {
        when(mResources.getInteger(R.integer.config_vibration_supported_intensity_levels))
                .thenReturn(2);
        assertThat(VibrationSettings.isPageSearchEnabled(mContext)).isFalse();
    }
}
