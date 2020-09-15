/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Looper;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiTetherSecurityPreferenceControllerTest {

    private static final String PREF_KEY = "wifi_tether_security";
    private static final String WPA3_SAE =
            String.valueOf(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE);
    private static final String WPA3_SAE_TRANSITION =
            String.valueOf(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION);
    private static final String WPA2_PSK =
            String.valueOf(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
    private static final String NONE = String.valueOf(SoftApConfiguration.SECURITY_TYPE_OPEN);

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;

    private WifiTetherSecurityPreferenceController mController;
    private ListPreference mPreference;
    private SoftApConfiguration mConfig;

    @Before
    public void setUp() {
        final Context context = spy(ApplicationProvider.getApplicationContext());
        mConfig = new SoftApConfiguration.Builder().setSsid("test_1234")
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN).build();
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mConfig);

        mController = new WifiTetherSecurityPreferenceController(context, mListener);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(context);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(context);
        mPreference = new ListPreference(context);
        mPreference.setKey(PREF_KEY);
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void onPreferenceChange_toWpa3Sae_shouldUpdateSecurityValue() {
        mController.onPreferenceChange(mPreference, WPA3_SAE);

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA3-Personal");
    }

    @Test
    public void onPreferenceChange_toWpa3SaeTransition_shouldUpdateSecurityValue() {
        mController.onPreferenceChange(mPreference, WPA3_SAE_TRANSITION);

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2/WPA3-Personal");
    }

    @Test
    public void onPreferenceChange_toWpa2Psk_shouldUpdateSecurityValue() {
        mController.onPreferenceChange(mPreference, WPA2_PSK);

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2-Personal");
    }

    @Test
    public void onPreferenceChange_toNone_shouldUpdateSecurityValue() {
        mController.onPreferenceChange(mPreference, NONE);

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_OPEN);
        assertThat(mPreference.getSummary().toString()).isEqualTo("None");
    }

    @Test
    public void updateDisplay_toWpa3Sae_shouldUpdateSecurityValue() {
        SoftApConfiguration config = new SoftApConfiguration.Builder(mConfig)
                .setPassphrase("test_password",
                        SoftApConfiguration.SECURITY_TYPE_WPA3_SAE).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.updateDisplay();

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA3-Personal");
    }

    @Test
    public void updateDisplay_toWpa3SaeTransition_shouldUpdateSecurityValue() {
        SoftApConfiguration config = new SoftApConfiguration.Builder(mConfig)
                .setPassphrase("test_password",
                        SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.updateDisplay();

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2/WPA3-Personal");
    }

    @Test
    public void updateDisplay_toWpa2Psk_shouldUpdateSecurityValue() {
        SoftApConfiguration config = new SoftApConfiguration.Builder(mConfig)
                .setPassphrase("test_password",
                        SoftApConfiguration.SECURITY_TYPE_WPA2_PSK).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.updateDisplay();

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2-Personal");
    }

    @Test
    public void updateDisplay_toNone_shouldUpdateSecurityValue() {
        SoftApConfiguration config = new SoftApConfiguration.Builder(mConfig)
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.updateDisplay();

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_OPEN);
        assertThat(mPreference.getSummary().toString()).isEqualTo("None");
    }

    @Test
    public void updateDisplay_toWpa3SaeButNotSupportWpa3_shouldBeDefaultToWpa2() {
        mController.mIsWpa3Supported = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder(mConfig)
                .setPassphrase("test_password",
                        SoftApConfiguration.SECURITY_TYPE_WPA3_SAE).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.updateDisplay();

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2-Personal");
    }

    @Test
    public void updateDisplay_toWpa3SaeTransitionButNotSupportWpa3_shouldBeDefaultToWpa2() {
        mController.mIsWpa3Supported = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder(mConfig)
                .setPassphrase("test_password",
                        SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mController.updateDisplay();

        assertThat(mController.getSecurityType())
                .isEqualTo(SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
        assertThat(mPreference.getSummary().toString()).isEqualTo("WPA2-Personal");
    }
}
