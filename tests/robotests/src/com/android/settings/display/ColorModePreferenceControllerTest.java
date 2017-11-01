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

import android.content.Context;
import android.os.IBinder;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ColorModePreferenceControllerTest {
    @Mock
    private ColorModePreferenceController.ConfigurationWrapper mConfigWrapper;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Context mContext;
    @Mock
    private IBinder mSurfaceFlinger;

    private ColorModePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SettingsShadowSystemProperties.clear();

        mController = new ColorModePreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mSurfaceFlinger", mSurfaceFlinger);
        ReflectionHelpers.setField(mController, "mConfigWrapper", mConfigWrapper);

        when(mConfigWrapper.isScreenWideColorGamut()).thenReturn(true);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void shouldCheckPreference() {
        SettingsShadowSystemProperties.set(
                ColorModePreferenceController.PERSISTENT_PROPERTY_SATURATION,
                Float.toString(ColorModePreferenceController.COLOR_SATURATION_VIVID));

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void shouldUncheckPreference() {
        SettingsShadowSystemProperties.set(
                ColorModePreferenceController.PERSISTENT_PROPERTY_SATURATION,
                Float.toString(ColorModePreferenceController.COLOR_SATURATION_DEFAULT));

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void shouldBoostSaturationOnCheck() {
        mController.onPreferenceChange(mPreference, true);

        String saturation = SettingsShadowSystemProperties
                .get(ColorModePreferenceController.PERSISTENT_PROPERTY_SATURATION);
        assertThat(saturation)
                .isEqualTo(Float.toString(ColorModePreferenceController.COLOR_SATURATION_VIVID));
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void shouldResetSaturationOnUncheck() {
        mController.onPreferenceChange(mPreference, false);

        String saturation = SettingsShadowSystemProperties
                .get(ColorModePreferenceController.PERSISTENT_PROPERTY_SATURATION);
        assertThat(saturation)
                .isEqualTo(Float.toString(ColorModePreferenceController.COLOR_SATURATION_DEFAULT));
    }
}
