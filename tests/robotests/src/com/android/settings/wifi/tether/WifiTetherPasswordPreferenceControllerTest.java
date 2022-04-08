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

import com.android.settings.widget.ValidatedEditTextPreference;
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
public class WifiTetherPasswordPreferenceControllerTest {

    private static final String VALID_PASS = "12345678";
    private static final String VALID_PASS2 = "23456789";
    private static final String INITIAL_PASSWORD = "test_password";
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

    private WifiTetherPasswordPreferenceController mController;
    private ValidatedEditTextPreference mPreference;
    private SoftApConfiguration mConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPreference = new ValidatedEditTextPreference(RuntimeEnvironment.application);
        mConfig = new SoftApConfiguration.Builder().setSsid("test_1234")
                .setPassphrase(INITIAL_PASSWORD, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK)
                .build();

        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mConfig);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"1", "2"});
        when(mContext.getResources()).thenReturn(RuntimeEnvironment.application.getResources());
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);

        mController = new WifiTetherPasswordPreferenceController(mContext, mListener,
                mMetricsFeatureProvider);
    }

    @Test
    public void displayPreference_shouldStylePreference() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getText()).isEqualTo(mConfig.getPassphrase());
        assertThat(mPreference.getSummary()).isEqualTo(mConfig.getPassphrase());
    }

    @Test
    public void changePreference_shouldUpdateValue() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, VALID_PASS);
        assertThat(mController.getPasswordValidated(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK))
                .isEqualTo(VALID_PASS);

        mController.onPreferenceChange(mPreference, VALID_PASS2);
        assertThat(mController.getPasswordValidated(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK))
                .isEqualTo(VALID_PASS2);

        verify(mListener, times(2)).onTetherConfigUpdated(mController);
    }

    @Test
    public void changePreference_shouldLogActionWhenChanged() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, VALID_PASS);
        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_SETTINGS_CHANGE_WIFI_HOTSPOT_PASSWORD);
    }

    @Test
    public void changePreference_shouldNotLogActionWhenNotChanged() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, INITIAL_PASSWORD);
        verify(mMetricsFeatureProvider, never()).action(mContext,
                SettingsEnums.ACTION_SETTINGS_CHANGE_WIFI_HOTSPOT_PASSWORD);
    }

    @Test
    public void updateDisplay_shouldUpdateValue() {
        // Set controller password to anything and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, VALID_PASS);
        assertThat(mController.getPasswordValidated(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK))
                .isEqualTo(VALID_PASS);

        // Create a new config using different password
        final SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(VALID_PASS2, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mController.getPasswordValidated(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK))
                .isEqualTo(config.getPassphrase());
        assertThat(mPreference.getSummary()).isEqualTo(config.getPassphrase());
    }

    @Test
    public void updateDisplay_shouldSetInputType() {
        // Set controller password to anything and verify is set.
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, VALID_PASS);
        assertThat(mController.getPasswordValidated(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK))
                .isEqualTo(VALID_PASS);

        // Create a new config using different password
        final SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setPassphrase(VALID_PASS2, SoftApConfiguration.SECURITY_TYPE_WPA2_PSK).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        // Call updateDisplay and verify it's changed.
        mController.updateDisplay();
        assertThat(mPreference.isPassword()).isTrue();
    }
}
