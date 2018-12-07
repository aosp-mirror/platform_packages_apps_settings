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

import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.view.IWindowManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class StrictModePreferenceControllerTest {

    @Mock
    private IWindowManager mWindowManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;

    private StrictModePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new StrictModePreferenceController(RuntimeEnvironment.application);
        ReflectionHelpers.setField(mController, "mWindowManager", mWindowManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_shouldTurnOnStrictMode() throws RemoteException {
        mController.onPreferenceChange(mPreference, true /* new value */);

        verify(mWindowManager).setStrictModeVisualIndicatorPreference(
                StrictModePreferenceController.STRICT_MODE_ENABLED);
    }

    @Test
    public void onPreferenceChange_settingDisabled_shouldTurnOffStrictMode()
            throws RemoteException {
        mController.onPreferenceChange(mPreference, false /* new value */);

        verify(mWindowManager).setStrictModeVisualIndicatorPreference(
                StrictModePreferenceController.STRICT_MODE_DISABLED);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        SystemProperties.set(StrictMode.VISUAL_PROPERTY, Boolean.toString(false));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        SystemProperties.set(StrictMode.VISUAL_PROPERTY, Boolean.toString(true));
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldTurnOffPreference() {
        mController.onDeveloperOptionsSwitchDisabled();
        final boolean isEnabled = SystemProperties.getBoolean(StrictMode.VISUAL_PROPERTY,
                false /* default */);

        assertThat(isEnabled).isFalse();
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }
}
