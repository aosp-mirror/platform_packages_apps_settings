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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiVerboseLoggingPreferenceControllerTest {
    @Mock
    private Context mContext;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private WifiVerboseLoggingPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        mController = new WifiVerboseLoggingPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChange_settingEnabled_shouldEnableVerboseLogging() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        verify(mWifiManager).setVerboseLoggingEnabled(true);
    }

    @Test
    public void onPreferenceChange_settingDisabled_shouldDisablVerboseLogging() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        verify(mWifiManager).setVerboseLoggingEnabled(false);
    }

    @Test
    public void updateState_settingEnabled_shouldEnablePreference() {
        when(mWifiManager.isVerboseLoggingEnabled()).thenReturn(true);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_shouldDisablePreference() {
        when(mWifiManager.isVerboseLoggingEnabled()).thenReturn(false);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mWifiManager).setVerboseLoggingEnabled(false);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }
}
