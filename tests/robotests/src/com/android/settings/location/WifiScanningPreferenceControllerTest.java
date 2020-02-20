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
package com.android.settings.location;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiScanningPreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private WifiManager mWifiManager;

    private Context mContext;
    private ContentResolver mContentResolver;
    private WifiScanningPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);

        mController = new WifiScanningPreferenceController(mContext);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void updateState_wifiScanningEnabled_shouldCheckedPreference() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_wifiScanningDisabled_shouldUncheckedPreference() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void handlePreferenceTreeClick_checked_shouldEnableWifiScanning() {
        when(mPreference.isChecked()).thenReturn(true);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mWifiManager).setScanAlwaysAvailable(true);
    }

    @Test
    public void handlePreferenceTreeClick_unchecked_shouldDisableWifiScanning() {
        when(mPreference.isChecked()).thenReturn(false);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mWifiManager).setScanAlwaysAvailable(false);
    }
}
