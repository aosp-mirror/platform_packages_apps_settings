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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

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
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

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
        mController = new WifiTetherSSIDPreferenceController(mContext, mListener,
                mMetricsFeatureProvider);
    }

    @Test
    public void displayPreference_noWifiConfig_shouldDisplayDefaultSSID() {
        when(mWifiManager.getSoftApConfiguration()).thenReturn(null);

        mController.displayPreference(mScreen);
        assertThat(mController.getSSID())
                .isEqualTo(WifiTetherSSIDPreferenceController.DEFAULT_SSID);
    }

    @Test
    public void displayPreference_hasCustomWifiConfig_shouldDisplayCustomSSID() {
        final SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setSsid("test_1234").build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.displayPreference(mScreen);
        assertThat(mController.getSSID()).isEqualTo(config.getSsid());
    }

    @Test
    public void changePreference_shouldUpdateValue() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getSSID()).isEqualTo("1");

        mController.onPreferenceChange(mPreference, "0");
        assertThat(mController.getSSID()).isEqualTo("0");

        verify(mListener, times(2)).onTetherConfigUpdated(mController);
    }

    @Test
    public void changePreference_shouldLogActionWhenChanged() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_SETTINGS_CHANGE_WIFI_HOTSPOT_NAME);
    }

    @Test
    public void changePreference_shouldNotLogActionWhenNotChanged() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference,
                WifiTetherSSIDPreferenceController.DEFAULT_SSID);
        verify(mMetricsFeatureProvider, never()).action(mContext,
                SettingsEnums.ACTION_SETTINGS_CHANGE_WIFI_HOTSPOT_NAME);
    }

    @Test
    public void updateDisplay_shouldUpdateValue() {
        // Set controller ssid to anything and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getSSID()).isEqualTo("1");

        // Create a new config using different SSID
        final SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setSsid("test_1234").build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mController.getSSID()).isEqualTo(config.getSsid());
        assertThat(mPreference.getSummary()).isEqualTo(config.getSsid());
    }

    @Test
    public void displayPreference_wifiApDisabled_shouldHideQrCodeIcon() {
        when(mWifiManager.isWifiApEnabled()).thenReturn(false);
        final SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setSsid("test_1234").setPassphrase("test_password",
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.displayPreference(mScreen);
        assertThat(mController.isQrCodeButtonAvailable()).isEqualTo(false);
    }

    @Test
    public void displayPreference_wifiApEnabled_shouldShowQrCodeIcon() {
        when(mWifiManager.isWifiApEnabled()).thenReturn(true);
        final SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setSsid("test_1234").setPassphrase("test_password",
                SoftApConfiguration.SECURITY_TYPE_WPA2_PSK).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.displayPreference(mScreen);
        assertThat(mController.isQrCodeButtonAvailable()).isEqualTo(true);
    }
}
