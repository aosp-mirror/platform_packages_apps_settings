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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SecondaryDisplayPreferenceControllerTest {

    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    /**
     * 0: None
     * 1: 480p
     * 2: 480p (secure)
     * 3: 720p
     * 4: 720p (secure)
     * 5: 1080p
     * 6: 1080p (secure)
     * 7: 4K
     * 8: 4K (secure)
     * 9: 4K (upscaled)
     * 10: 4K (upscaled, secure)
     * 11: 720p, 1080p (dual screen)
     */
    private String[] mListValues;
    private String[] mListSummaries;
    private Context mContext;
    private SecondaryDisplayPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final Resources resources = mContext.getResources();
        mListValues = resources.getStringArray(
                com.android.settingslib.R.array.overlay_display_devices_values);
        mListSummaries = resources.getStringArray(
                com.android.settingslib.R.array.overlay_display_devices_entries);
        mController = new SecondaryDisplayPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_set480p_shouldEnable480p() {
        mController.onPreferenceChange(mPreference, mListValues[1]);

        final String value = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES);
        assertThat(value).isEqualTo(mListValues[1]);
    }

    @Test
    public void onPreferenceChange_set720p_shouldEnable720p() {
        mController.onPreferenceChange(mPreference, mListValues[3]);

        final String value = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES);
        assertThat(value).isEqualTo(mListValues[3]);
    }

    @Test
    public void updateState_set480p_shouldSetPreferenceTo480p() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES, mListValues[1]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[1]);
        verify(mPreference).setSummary(mListSummaries[1]);
    }

    @Test
    public void updateState_set720p_shouldSetPreferenceTo720p() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES, mListValues[3]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[3]);
        verify(mPreference).setSummary(mListSummaries[3]);
    }

    @Test
    public void updateState_nothingSet_shouldSetDefaultToNone() {
        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[0]);
        verify(mPreference).setSummary(mListSummaries[0]);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreferenceAndSetToNone() {
        mController.onDeveloperOptionsSwitchDisabled();

        final String value = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.OVERLAY_DISPLAY_DEVICES);
        assertThat(value).isNull();
        verify(mPreference).setEnabled(false);
        verify(mPreference).setValue(mListValues[0]);
        verify(mPreference).setSummary(mListSummaries[0]);
    }
}
