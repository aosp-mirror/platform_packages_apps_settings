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
public class WifiScanThrottlingPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Context mContext;
    @Mock
    private WifiManager mWifiManager;
    private WifiScanThrottlingPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        mController = new WifiScanThrottlingPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void onPreferenceChanged_turnOnScanThrottling() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        verify(mWifiManager).setScanThrottleEnabled(true);
    }

    @Test
    public void onPreferenceChanged_turnOffScanThrottling() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        verify(mWifiManager).setScanThrottleEnabled(false);
    }

    @Test
    public void updateState_preferenceShouldBeChecked() {
        when(mWifiManager.isScanThrottleEnabled()).thenReturn(true);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_preferenceShouldNotBeChecked() {
        when(mWifiManager.isScanThrottleEnabled()).thenReturn(false);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsDisabled();
        verify(mWifiManager).setScanThrottleEnabled(true);
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(true);
    }
}
