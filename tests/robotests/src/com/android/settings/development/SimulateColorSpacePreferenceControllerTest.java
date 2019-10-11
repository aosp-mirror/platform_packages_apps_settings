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

package com.android.settings.development;

import static com.android.settings.development.SimulateColorSpacePreferenceController
        .SETTING_VALUE_OFF;
import static com.android.settings.development.SimulateColorSpacePreferenceController
        .SETTING_VALUE_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SimulateColorSpacePreferenceControllerTest {

    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    /**
     * 0: Disabled
     * 1: Monochromacy
     * 2: Deuteranomaly (red-green)
     * 3: Protanomaly (red-green)
     * 4: Tritanomaly (blue-yellow)
     */
    private String[] mListValues;
    private Context mContext;
    private SimulateColorSpacePreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListValues = mContext.getResources().getStringArray(R.array.simulate_color_space_values);
        mController = new SimulateColorSpacePreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_disabledSelected_shouldTurnOffPreference()
            throws Settings.SettingNotFoundException {
        mController.onPreferenceChange(mPreference, mListValues[0]);

        final int enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);

        assertThat(enabled).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    public void onPreferenceChange_monochromacySelected_shouldEnableAndSelectPreference()
            throws Settings.SettingNotFoundException {
        mController.onPreferenceChange(mPreference, mListValues[1]);

        final int enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);
        final int settingValue = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER);

        assertThat(enabled).isEqualTo(SETTING_VALUE_ON);
        assertThat(settingValue).isEqualTo(Integer.valueOf(mListValues[1]));
    }

    @Test
    public void updateState_settingOff_shouldSetValueToDisabled() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, SETTING_VALUE_OFF);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[0]);
    }

    @Test
    public void updateState_settingOnMonochromacyEnabled_shouldSelectMonochromacy() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, SETTING_VALUE_ON);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, Integer.valueOf(mListValues[1]));

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[1]);
        verify(mPreference).setSummary("%s");
    }

    @Test
    public void updateState_settingOnControlledByAccessibility_shouldSetOverridedSummary() {
        Resources res = mContext.getResources();
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, SETTING_VALUE_ON);
        when(mPreference.findIndexOfValue(anyString())).thenReturn(-1);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(res.getString(R.string.daltonizer_type_overridden,
                res.getString(R.string.accessibility_display_daltonizer_preference_title)));
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_notControlledByDevOptions_shouldDisableAndReset()
            throws Settings.SettingNotFoundException {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, SETTING_VALUE_ON);
        when(mPreference.findIndexOfValue(anyString())).thenReturn(-1);

        mController.onDeveloperOptionsDisabled();

        final int settingValue = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);
        assertThat(settingValue).isEqualTo(SETTING_VALUE_ON);
        verify(mPreference).setEnabled(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_controlledByDevOptions_shouldDisableAndNotReset()
            throws Settings.SettingNotFoundException {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, SETTING_VALUE_ON);

        mController.onDeveloperOptionsDisabled();

        final int settingValue = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);
        assertThat(settingValue).isEqualTo(SETTING_VALUE_OFF);
        verify(mPreference).setEnabled(false);
    }
}
