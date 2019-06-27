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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Vibrator;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccessibilitySettingsTest {
    private static final String VIBRATION_PREFERENCE_SCREEN = "vibration_preference_screen";

    private Context mContext;
    private AccessibilitySettings mSettings;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSettings = spy(new AccessibilitySettings());
        doReturn(mContext).when(mSettings).getContext();
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.accessibility_settings);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testUpdateVibrationSummary_shouldUpdateSummary() {
        final Preference vibrationPreferenceScreen = new Preference(mContext);
        doReturn(vibrationPreferenceScreen).when(mSettings).findPreference(
                VIBRATION_PREFERENCE_SCREEN);

        vibrationPreferenceScreen.setKey(VIBRATION_PREFERENCE_SCREEN);

        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
                Vibrator.VIBRATION_INTENSITY_OFF);

        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                Vibrator.VIBRATION_INTENSITY_OFF);

        mSettings.updateVibrationSummary(vibrationPreferenceScreen);
        assertThat(vibrationPreferenceScreen.getSummary()).isEqualTo(
                VibrationIntensityPreferenceController.getIntensityString(mContext,
                        Vibrator.VIBRATION_INTENSITY_OFF));
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void testIsRampingRingerEnabled_bothFlagsOn_Enabled() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, 1 /* ON */);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_TELEPHONY,
                AccessibilitySettings.RAMPING_RINGER_ENABLED, "true", false /* makeDefault*/);
        assertThat(AccessibilitySettings.isRampingRingerEnabled(mContext)).isTrue();
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void testIsRampingRingerEnabled_settingsFlagOff_Disabled() {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, 0 /* OFF */);
        assertThat(AccessibilitySettings.isRampingRingerEnabled(mContext)).isFalse();
    }

    @Test
    @Config(shadows = {ShadowDeviceConfig.class})
    public void testIsRampingRingerEnabled_deviceConfigFlagOff_Disabled() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_TELEPHONY,
                AccessibilitySettings.RAMPING_RINGER_ENABLED, "false", false /* makeDefault*/);
        assertThat(AccessibilitySettings.isRampingRingerEnabled(mContext)).isFalse();
    }
}
