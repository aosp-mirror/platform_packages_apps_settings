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

package com.android.settings.wifi.tether;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.widget.ValidatedEditTextPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherSSIDPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;
    @Mock
    private PreferenceScreen mScreen;

    private WifiTetherSSIDPreferenceController mController;
    private WifiTetherSsidPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPreference = new WifiTetherSsidPreference(RuntimeEnvironment.application);

        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        when(mContext.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = new WifiTetherSSIDPreferenceController(mContext, mListener);
    }

    @Test
    public void displayPreference_noWifiConfig_shouldDisplayDefaultSSID() {
        when(mWifiManager.getWifiApConfiguration()).thenReturn(null);

        mController.displayPreference(mScreen);
        assertThat(mController.getSSID())
                .isEqualTo(WifiTetherSSIDPreferenceController.DEFAULT_SSID);
    }

    @Test
    public void displayPreference_hasCustomWifiConfig_shouldDisplayCustomSSID() {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "test_1234";
        when(mWifiManager.getWifiApConfiguration()).thenReturn(config);

        mController.displayPreference(mScreen);
        assertThat(mController.getSSID()).isEqualTo(config.SSID);
    }

    @Test
    public void changePreference_shouldUpdateValue() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getSSID()).isEqualTo("1");

        mController.onPreferenceChange(mPreference, "0");
        assertThat(mController.getSSID()).isEqualTo("0");

        verify(mListener, times(2)).onTetherConfigUpdated();
    }

    @Test
    public void updateDisplay_shouldUpdateValue() {
        // Set controller ssid to anything and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getSSID()).isEqualTo("1");

        // Create a new config using different SSID
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "test_1234";
        when(mWifiManager.getWifiApConfiguration()).thenReturn(config);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mController.getSSID()).isEqualTo(config.SSID);
        assertThat(mPreference.getSummary()).isEqualTo(config.SSID);
    }

    @Test
    public void displayPreference_wifiApDisabled_shouldHideQrCodeIcon() {
        when(mWifiManager.isWifiApEnabled()).thenReturn(false);
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "test_1234";
        config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
        when(mWifiManager.getWifiApConfiguration()).thenReturn(config);

        mController.displayPreference(mScreen);
        assertThat(mController.isQrCodeButtonAvailable()).isEqualTo(false);
    }

    @Test
    public void displayPreference_wifiApEnabled_shouldShowQrCodeIcon() {
        when(mWifiManager.isWifiApEnabled()).thenReturn(true);
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "test_1234";
        config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
        when(mWifiManager.getWifiApConfiguration()).thenReturn(config);

        mController.displayPreference(mScreen);
        assertThat(mController.isQrCodeButtonAvailable()).isEqualTo(true);
    }
}
