/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowParcel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

@RunWith(SettingsRobolectricTestRunner.class)
public class HighFrequencyPreferenceControllerTest {

    private Context mContext;
    private SwitchPreference mPreference;

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private IBinder mSurfaceFlingerBinder;

    private HighFrequencyDisplayPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = spy(new HighFrequencyDisplayPreferenceController(mContext));
        ReflectionHelpers.setField(mController, "mSurfaceFlingerBinder", mSurfaceFlingerBinder);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_settingToggledOn_shouldWriteTrueToHighFrequencySetting() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        verify(mController).writeHighFrequencyDisplaySetting(true);
    }

    @Test
    public void onPreferenceChange_settingToggledOff_shouldWriteFalseToHighFrequencySetting() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        verify(mController).writeHighFrequencyDisplaySetting(false);
    }

    @Test
    public void updateState_settingEnabled_shouldCheckPreference() throws RemoteException {
        mController.writeHighFrequencyDisplaySetting(true);
        mController.updateState(mPreference);

        verify(mController).readHighFrequencyDisplaySetting();
    }

    @Test
    public void updateState_settingDisabled_shouldUnCheckPreference() throws RemoteException {
        mController.writeHighFrequencyDisplaySetting(true);
        mController.updateState(mPreference);

        verify(mController).readHighFrequencyDisplaySetting();
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceChecked_shouldTurnOffPreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mController).writeHighFrequencyDisplaySetting(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceUnchecked_shouldNotTurnOffPreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mController).writeHighFrequencyDisplaySetting(false);
    }
}
