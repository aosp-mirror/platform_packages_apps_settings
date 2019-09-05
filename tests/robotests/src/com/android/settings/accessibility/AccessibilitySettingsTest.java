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

import android.app.UiModeManager;
import android.content.ContentResolver;
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
    private static final String ACCESSIBILITY_CONTROL_TIMEOUT_PREFERENCE =
            "accessibility_control_timeout_preference_fragment";
    private static final String DARK_UI_MODE_PREFERENCE =
            "dark_ui_mode_accessibility";

    private Context mContext;
    private ContentResolver mContentResolver;
    private AccessibilitySettings mSettings;
    private UiModeManager mUiModeManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mSettings = spy(new AccessibilitySettings());
        doReturn(mContext).when(mSettings).getContext();
        mUiModeManager = mContext.getSystemService(UiModeManager.class);
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
    public void testUpdateAccessibilityTimeoutSummary_shouldUpdateSummary() {
        String[] testingValues = {null, "0", "10000", "30000", "60000", "120000"};
        int[] exceptedResIds = {R.string.accessibility_timeout_default,
                R.string.accessibility_timeout_default,
                R.string.accessibility_timeout_10secs,
                R.string.accessibility_timeout_30secs,
                R.string.accessibility_timeout_1min,
                R.string.accessibility_timeout_2mins
        };

        for (int i = 0; i < testingValues.length; i++) {
            Settings.Secure.putString(mContentResolver,
                    Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, testingValues[i]);

            verifyAccessibilityTimeoutSummary(ACCESSIBILITY_CONTROL_TIMEOUT_PREFERENCE,
                    exceptedResIds[i]);
        }
    }

    @Test
    public void testUpdateAccessibilityControlTimeoutSummary_invalidData_shouldUpdateSummary() {
        String[] testingValues = {"-9009", "98277466643738977979666555536362343", "Hello,a prank"};

        for (String value : testingValues) {
            Settings.Secure.putString(mContentResolver,
                    Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS, value);

            verifyAccessibilityTimeoutSummary(ACCESSIBILITY_CONTROL_TIMEOUT_PREFERENCE,
                    R.string.accessibility_timeout_default);

            Settings.Secure.putString(mContentResolver,
                    Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS, value);

            verifyAccessibilityTimeoutSummary(ACCESSIBILITY_CONTROL_TIMEOUT_PREFERENCE,
                    R.string.accessibility_timeout_default);
        }
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

    private void verifyAccessibilityTimeoutSummary(String preferenceKey, int resId) {
        final Preference preference = new Preference(mContext);
        doReturn(preference).when(mSettings).findPreference(preferenceKey);
        preference.setKey(preferenceKey);
        mSettings.updateAccessibilityTimeoutSummary(mContentResolver, preference);

        assertThat(preference.getSummary()).isEqualTo(mContext.getResources().getString(resId));
    }

    private String modeToDescription(int mode) {
        String[] values = mContext.getResources().getStringArray(R.array.dark_ui_mode_entries);
        switch (mode) {
            case UiModeManager.MODE_NIGHT_YES:
                return values[0];
            case UiModeManager.MODE_NIGHT_NO:
            case UiModeManager.MODE_NIGHT_AUTO:
            default:
                return values[1];
        }
    }
}
