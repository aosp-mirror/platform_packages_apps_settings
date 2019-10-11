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

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class LocationScanningPreferenceControllerTest {
    private Context mContext;
    private LocationScanningPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new LocationScanningPreferenceController(mContext);
    }

    @Test
    public void testLocationScanning_byDefault_shouldBeShown() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testLocationScanning_WifiOnBleOn() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 1);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.scanning_status_text_wifi_on_ble_on));
    }

    @Test
    public void testLocationScanning_WifiOnBleOff() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 1);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.scanning_status_text_wifi_on_ble_off));
    }

    @Test
    public void testLocationScanning_WifiOffBleOn() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1);
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.scanning_status_text_wifi_off_ble_on));
    }

    @Test
    public void testLocationScanning_WifiOffBleOff() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0);
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