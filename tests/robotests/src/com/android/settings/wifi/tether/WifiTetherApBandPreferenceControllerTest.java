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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherApBandPreferenceControllerTest {

    private static final String ALL_BANDS = "5.0 GHz Band preferred";
    private static final String TWO_GHZ_STRING = "2.4 GHz Band";
    private static final String FIVE_GHZ_STRING = "5.0 GHz Band";
    private static final String VAL_2GHZ_STR  = "1";
    private static final String VAL_5GHZ_STR = "2";
    private static final String VAL_2_5_GHZ_STR = "3";
    private static final int VAL_2GHZ_INT = 1;
    private static final int VAL_5GHZ_INT = 2;
    private static final int VAL_2_5_GHZ_INT = 3;

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
        when(mWifiManager.getSoftApConfiguration()).thenReturn(
                new SoftApConfiguration.Builder().build());

        mController = new WifiTetherApBandPreferenceController(mContext, mListener);
    }

    @Test
    public void display_5GhzSupported_shouldDisplayFullList() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.is5GHzBandSupported()).thenReturn(true);

        // Create a new instance
        mController = new WifiTetherApBandPreferenceController(mContext, mListener);

        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, VAL_2_5_GHZ_STR);

        assertThat(mPreference.getSummary()).isEqualTo(ALL_BANDS);
    }

    @Test
    public void display_noCountryCode_shouldDisable() {
        when(mWifiManager.getCountryCode()).thenReturn(null);
        when(mWifiManager.is5GHzBandSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.wifi_ap_choose_2G));
    }

    @Test
    public void display_5GhzNotSupported_shouldDisable() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.is5GHzBandSupported()).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.getSummary())
                .isEqualTo(RuntimeEnvironment.application.getString(R.string.wifi_ap_choose_2G));
    }

    @Test
    public void changePreference_With5G_shouldUpdateValue() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.is5GHzBandSupported()).thenReturn(true);

        // Create a new instance to pick the proper value of isDualModeSupported()
        mController = new WifiTetherApBandPreferenceController(mContext, mListener);

        mController.displayPreference(mScreen);

        // 'Auto' option
        mController.onPreferenceChange(mPreference, VAL_2_5_GHZ_STR);
        assertThat(mController.getBandIndex()).isEqualTo(VAL_2_5_GHZ_INT);
        assertThat(mPreference.getSummary()).isEqualTo(ALL_BANDS);
        verify(mListener, times(1)).onTetherConfigUpdated(mController);

        // should revert to the default for 5 Ghz only since this is not supported with this config
        mController.onPreferenceChange(mPreference, VAL_5GHZ_STR);
        assertThat(mController.getBandIndex()).isEqualTo(VAL_2_5_GHZ_INT);
        assertThat(mPreference.getSummary()).isEqualTo(ALL_BANDS);
        verify(mListener, times(2)).onTetherConfigUpdated(mController);

        // set to 2 Ghz
        mController.onPreferenceChange(mPreference, VAL_2GHZ_STR);
        assertThat(mController.getBandIndex()).isEqualTo(VAL_2GHZ_INT);
        assertThat(mPreference.getSummary()).isEqualTo(TWO_GHZ_STRING);
        verify(mListener, times(3)).onTetherConfigUpdated(mController);
    }

    @Test
    public void updateDisplay_shouldUpdateValue() {
        when(mWifiManager.getCountryCode()).thenReturn("US");
        when(mWifiManager.is5GHzBandSupported()).thenReturn(true);

        // Set controller band index to 5GHz and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, VAL_5GHZ_STR);
        assertThat(mController.getBandIndex()).isEqualTo(VAL_2_5_GHZ_INT);

        // Disable 5Ghz band
        when(mWifiManager.is5GHzBandSupported()).thenReturn(false);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mController.getBandIndex()).isEqualTo(VAL_2GHZ_INT);
    }
}
