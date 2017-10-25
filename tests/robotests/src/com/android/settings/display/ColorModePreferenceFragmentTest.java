/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import com.android.settings.widget.RadioButtonPickerFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ColorModePreferenceFragmentTest {

    private ColorModePreferenceFragment mFragment;

    @Mock
    private NightDisplayController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SettingsShadowSystemProperties.clear();

        mFragment = spy(new ColorModePreferenceFragment());
        ReflectionHelpers.setField(mFragment, "mController", mController);
    }

    @Test
    public void verifyMetricsConstant() {
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.COLOR_MODE_SETTINGS);
    }

    @Test
    public void getCandidates() {
        when(mFragment.getContext()).thenReturn(RuntimeEnvironment.application);
        List<? extends RadioButtonPickerFragment.CandidateInfo> candidates =
                mFragment.getCandidates();

        assertThat(candidates.size()).isEqualTo(3);
        assertThat(candidates.get(0).getKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);
        assertThat(candidates.get(1).getKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);
        assertThat(candidates.get(2).getKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void getKey_natural() {
        Mockito.when(mController.getColorMode()).thenReturn(
            NightDisplayController.COLOR_MODE_NATURAL);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void getKey_boosted() {
        Mockito.when(mController.getColorMode()).thenReturn(
            NightDisplayController.COLOR_MODE_BOOSTED);

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void getKey_saturated() {
        Mockito.when(mController.getColorMode()).thenReturn(
            NightDisplayController.COLOR_MODE_SATURATED);

        assertThat(mFragment.getDefaultKey())
            .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void setKey_natural() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);
        Mockito.verify(mController).setColorMode(NightDisplayController.COLOR_MODE_NATURAL);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void setKey_boosted() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);
        Mockito.verify(mController).setColorMode(NightDisplayController.COLOR_MODE_BOOSTED);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void setKey_saturated() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);
        Mockito.verify(mController).setColorMode(NightDisplayController.COLOR_MODE_SATURATED);
    }
}
