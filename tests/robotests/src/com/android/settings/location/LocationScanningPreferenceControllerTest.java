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

package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class LocationScanningPreferenceControllerTest {
    @Mock
    private WifiManager mWifiManager;
    private Context mContext;
    private LocationScanningPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        mController = new LocationScanningPreferenceController(mContext, "key");
    }

    @Test
    public void testLocationScanning_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testLocationScanning_WifiOnBleOn() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.scanning_status_text_wifi_on_ble_on));
    }

    @Test
    public void testLocationScanning_WifiOnBleOff() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.scanning_status_text_wifi_on_ble_off));
    }

    @Test
    public void testLocationScanning_WifiOffBleOn() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.scanning_status_text_wifi_off_ble_on));
    }

    @Test
    public void testLocationScanning_WifiOffBleOff() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.scanning_status_text_wifi_off_ble_off));
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testLocationScanning_ifDisabled_shouldNotBeShown() {
        assertThat(mController.isAvailable()).isFalse();
    }
}