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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.IActivityManager;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import com.android.settings.widget.RadioButtonPickerFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ColorModePreferenceFragmentTest {
    @Mock
    private IBinder mSurfaceFlinger;
    @Mock
    private IActivityManager mActivityManager;

    private ColorModePreferenceFragment mFragment;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SettingsShadowSystemProperties.clear();

        mFragment = spy(new ColorModePreferenceFragment());
        doReturn(RuntimeEnvironment.application).when(mFragment).getContext();
        doNothing().when(mFragment).updateConfiguration();

        ReflectionHelpers.setField(mFragment, "mSurfaceFlinger", mSurfaceFlinger);
        ReflectionHelpers.setField(mFragment, "mActivityManager", mActivityManager);
    }

    @Test
    public void verifyMetricsConstant() {
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.COLOR_MODE_SETTINGS);
    }

    @Test
    public void getCandidates() {
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
        SettingsShadowSystemProperties.set(
                ColorModePreferenceFragment.PERSISTENT_PROPERTY_SATURATION,
                Float.toString(ColorModePreferenceFragment.COLOR_SATURATION_NATURAL));
        SettingsShadowSystemProperties.set(
                ColorModePreferenceFragment.PERSISTENT_PROPERTY_NATIVE_MODE, "0");

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void getKey_boosted() {
        SettingsShadowSystemProperties.set(
                ColorModePreferenceFragment.PERSISTENT_PROPERTY_SATURATION,
                Float.toString(ColorModePreferenceFragment.COLOR_SATURATION_BOOSTED));
        SettingsShadowSystemProperties.set(
                ColorModePreferenceFragment.PERSISTENT_PROPERTY_NATIVE_MODE, "0");

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void getKey_saturated() {
        SettingsShadowSystemProperties.set(
                ColorModePreferenceFragment.PERSISTENT_PROPERTY_NATIVE_MODE, "1");

        assertThat(mFragment.getDefaultKey())
                .isEqualTo(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void setKey_natural() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_NATURAL);

        String saturation = SettingsShadowSystemProperties
                .get(ColorModePreferenceFragment.PERSISTENT_PROPERTY_SATURATION);
        assertThat(saturation)
                .isEqualTo(Float.toString(ColorModePreferenceFragment.COLOR_SATURATION_NATURAL));

        String nativeMode = SettingsShadowSystemProperties
                .get(ColorModePreferenceFragment.PERSISTENT_PROPERTY_NATIVE_MODE);
        assertThat(nativeMode).isEqualTo("0");
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void setKey_boosted() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_BOOSTED);

        String saturation = SettingsShadowSystemProperties
                .get(ColorModePreferenceFragment.PERSISTENT_PROPERTY_SATURATION);
        assertThat(saturation)
                .isEqualTo(Float.toString(ColorModePreferenceFragment.COLOR_SATURATION_BOOSTED));

        String nativeMode = SettingsShadowSystemProperties
                .get(ColorModePreferenceFragment.PERSISTENT_PROPERTY_NATIVE_MODE);
        assertThat(nativeMode).isEqualTo("0");
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void setKey_saturated() {
        mFragment.setDefaultKey(ColorModePreferenceFragment.KEY_COLOR_MODE_SATURATED);

        String saturation = SettingsShadowSystemProperties
                .get(ColorModePreferenceFragment.PERSISTENT_PROPERTY_SATURATION);
        assertThat(saturation)
                .isEqualTo(Float.toString(ColorModePreferenceFragment.COLOR_SATURATION_NATURAL));

        String nativeMode = SettingsShadowSystemProperties
                .get(ColorModePreferenceFragment.PERSISTENT_PROPERTY_NATIVE_MODE);
        assertThat(nativeMode).isEqualTo("1");
    }
}
