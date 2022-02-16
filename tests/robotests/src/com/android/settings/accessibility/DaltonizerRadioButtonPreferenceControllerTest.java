/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DaltonizerRadioButtonPreferenceControllerTest implements
        DaltonizerRadioButtonPreferenceController.OnChangeListener {
    private static final String PREF_KEY = "daltonizer_mode_protanomaly";
    private static final String PREF_VALUE = "11";
    private static final String PREF_FAKE_VALUE = "-1";

    private DaltonizerRadioButtonPreferenceController mController;

    @Mock
    private RadioButtonPreference mMockPref;
    private Context mContext;

    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new DaltonizerRadioButtonPreferenceController(mContext, mock(Lifecycle.class),
                PREF_KEY);
        mController.setOnChangeListener(this);

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mMockPref);
        when(mMockPref.getKey()).thenReturn(PREF_KEY);
        mController.displayPreference(mScreen);
    }

    @Override
    public void onCheckedChanged(Preference preference) {
        mController.updateState(preference);
    }

    @Test
    public void isAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_notChecked() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, PREF_FAKE_VALUE);

        mController.updateState(mMockPref);

        // the first checked state is set to false by control
        verify(mMockPref, atLeastOnce()).setChecked(false);
        verify(mMockPref, never()).setChecked(true);
    }

    @Test
    public void updateState_checked() {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, PREF_VALUE);

        mController.updateState(mMockPref);

        // the first checked state is set to false by control
        verify(mMockPref, atLeastOnce()).setChecked(false);
        verify(mMockPref, atLeastOnce()).setChecked(true);
    }

    @Test
    public void onRadioButtonClick_shouldReturnDaltonizerValue() {
        mController.onRadioButtonClicked(mMockPref);
        final String accessibilityDaltonizerValue = Settings.Secure.getString(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER);

        assertThat(accessibilityDaltonizerValue).isEqualTo(PREF_VALUE);
    }
}
