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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiTetherApBandPreferenceControllerTest {

    private static final String ALL_BANDS = "5.0 GHz Band preferred";
    private static final String TWO_GHZ_STRING = "2.4 GHz Band";
    private static final String FIVE_GHZ_STRING = "5.0 GHz Band";
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;
    @Mock
    private PreferenceScreen mScreen;

    private WifiTetherApBandPreferenceController mController;
    private ListPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mPreference = new ListPreference(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        when(mContext.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        WifiConfiguration config = new WifiConfiguration();
        config.apBand = WifiConfiguration.AP_BAND_ANY;
        when(mWifiManager.getWifiApConfiguration()).thenReturn(new WifiConfiguration());
        when(mWifiManager.isDualModeSupported()).thenReturn(false);

        mController = new WifiTetherApBandPreferenceController(mContext, mListener);
    }

    @Test
    public void display_5GhzSupported_shouldDisplayFullList() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.isDualBandSupported()).thenReturn(true);
        when(mWifiManager.isDualModeSupported()).thenReturn(true);

        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "-1");

        assertThat(mPreference.getSummary()).isEqualTo(ALL_BANDS);
    }

    @Test
    public void display_noCountryCode_shouldDisable() {
        when(mWifiManager.getCountryCode()).thenReturn(null);
        when(mWifiManager.isDualBandSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.wifi_ap_choose_2G));
    }

    @Test
    public void display_5GhzNotSupported_shouldDisable() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.isDualBandSupported()).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.wifi_ap_choose_2G));
    }

    @Test
    public void changePreference_noDualModeWith5G_shouldUpdateValue() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.isDualBandSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        // -1 is WifiConfiguration.AP_BAND_ANY, for 'Auto' option. This should be prevented from
        // being set since it is invalid for this configuration
        mController.onPreferenceChange(mPreference, "-1");
        assertThat(mController.getBandIndex()).isEqualTo(1);
        assertThat(mPreference.getSummary()).isEqualTo(FIVE_GHZ_STRING);
        verify(mListener, times(1)).onTetherConfigUpdated();

        // set to 5 Ghz
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getBandIndex()).isEqualTo(1);
        assertThat(mPreference.getSummary()).isEqualTo(FIVE_GHZ_STRING);
        verify(mListener, times(2)).onTetherConfigUpdated();

        // set to 2 Ghz
        mController.onPreferenceChange(mPreference, "0");
        assertThat(mController.getBandIndex()).isEqualTo(0);
        assertThat(mPreference.getSummary()).isEqualTo(TWO_GHZ_STRING);
        verify(mListener, times(3)).onTetherConfigUpdated();
    }

    @Test
    public void changePreference_dualModeWith5G_shouldUpdateValue() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.isDualBandSupported()).thenReturn(true);
        when(mWifiManager.isDualModeSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        // -1 is WifiConfiguration.AP_BAND_ANY, for 'Auto' option.
        mController.onPreferenceChange(mPreference, "-1");
        assertThat(mController.getBandIndex()).isEqualTo(-1);
        assertThat(mPreference.getSummary()).isEqualTo(ALL_BANDS);
        verify(mListener, times(1)).onTetherConfigUpdated();

        // should revert to the default for 5 Ghz only since this is not supported with this config
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getBandIndex()).isEqualTo(-1);
        assertThat(mPreference.getSummary()).isEqualTo(ALL_BANDS);
        verify(mListener, times(2)).onTetherConfigUpdated();

        // set to 2 Ghz
        mController.onPreferenceChange(mPreference, "0");
        assertThat(mController.getBandIndex()).isEqualTo(0);
        assertThat(mPreference.getSummary()).isEqualTo(TWO_GHZ_STRING);
        verify(mListener, times(3)).onTetherConfigUpdated();
    }

    @Test
    public void updateDisplay_shouldUpdateValue() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.isDualBandSupported()).thenReturn(true);

        // Set controller band index to 1 and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");
        assertThat(mController.getBandIndex()).isEqualTo(1);

        // Disable 5Ghz band
        when(mWifiManager.isDualBandSupported()).thenReturn(false);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mController.getBandIndex()).isEqualTo(0);
    }
}
