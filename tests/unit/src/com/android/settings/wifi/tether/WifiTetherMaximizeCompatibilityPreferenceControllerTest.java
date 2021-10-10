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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiTetherMaximizeCompatibilityPreferenceControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiTetherBasePreferenceController.OnTetherConfigUpdateListener mListener;

    private Context mContext;
    private WifiTetherMaximizeCompatibilityPreferenceController mController;
    private SwitchPreference mPreference;
    private SoftApConfiguration mConfig;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mConfig = new SoftApConfiguration.Builder()
                .setSsid("test_Ssid")
                .setPassphrase(null, SoftApConfiguration.SECURITY_TYPE_OPEN)
                .setBridgedModeOpportunisticShutdownEnabled(true)
                .build();
        doReturn(mWifiManager).when(mContext).getSystemService(Context.WIFI_SERVICE);
        doReturn(true).when(mWifiManager).isBridgedApConcurrencySupported();
        doReturn(mConfig).when(mWifiManager).getSoftApConfiguration();

        mController = new WifiTetherMaximizeCompatibilityPreferenceController(mContext, mListener);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new SwitchPreference(mContext);
        mPreference.setKey(WifiTetherMaximizeCompatibilityPreferenceController.PREF_KEY);
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @Test
    public void getPreferenceKey_shouldBeCorrect() {
        assertThat(mController.getPreferenceKey())
                .isEqualTo(WifiTetherMaximizeCompatibilityPreferenceController.PREF_KEY);
    }

    @Test
    public void updateDisplay_notSupport5GHzBand_setPreferenceDisabled() {
        doReturn(false).when(mWifiManager).is5GHzBandSupported();

        mController.updateDisplay();

        assertThat(mPreference.isEnabled()).isEqualTo(false);
    }

    @Test
    public void updateDisplay_getNullCountryCode_setPreferenceDisabled() {
        doReturn(null).when(mWifiManager).getCountryCode();

        mController.updateDisplay();

        assertThat(mPreference.isEnabled()).isEqualTo(false);
    }

    @Test
    public void updateDisplay_notSupportedBridgedApConcurrency_setSingleApSummary() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();

        mController.updateDisplay();

        assertThat(mPreference.getSummary()).isEqualTo(ResourcesUtils.getResourcesString(mContext,
                "wifi_hotspot_maximize_compatibility_single_ap_summary"));
    }

    @Test
    public void updateDisplay_supportedBridgedApConcurrency_setDualApSummary() {
        doReturn(true).when(mWifiManager).isBridgedApConcurrencySupported();

        mController.updateDisplay();

        assertThat(mPreference.getSummary()).isEqualTo(ResourcesUtils.getResourcesString(mContext,
                "wifi_hotspot_maximize_compatibility_dual_ap_summary"));
    }

    @Test
    public void updateDisplay_supported5GHzBandAndCountryCodeIsNotNull_setPreferenceEnabled() {
        doReturn(true).when(mWifiManager).is5GHzBandSupported();
        doReturn("US").when(mWifiManager).getCountryCode();

        mController.updateDisplay();

        assertThat(mPreference.isEnabled()).isEqualTo(true);
    }

    @Test
    public void onPreferenceChange_callbackOnTetherConfigUpdated() {
        mController.onPreferenceChange(mPreference, true);
        verify(mListener).onTetherConfigUpdated(any());
    }

    @Test
    public void isMaximizeCompatibilityEnabled_concurrencySupportedAndEnabled_returnFalse() {
        // The preconditions are ready in setup().

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(false);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_concurrencySupportedAndDisabled_returnTrue() {
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBridgedModeOpportunisticShutdownEnabled(false)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(true);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_noConcurrencyAndGetBand2gOnly_returnFalse() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(SoftApConfiguration.BAND_2GHZ)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(true);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_noConcurrencyAndGetBand5gOnly_returnTrue() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(SoftApConfiguration.BAND_5GHZ)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(false);
    }

    @Test
    public void isMaximizeCompatibilityEnabled_noConcurrencyAndGetBand2gAnd5g_returnTrue() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ)
                .build();
        doReturn(config).when(mWifiManager).getSoftApConfiguration();

        assertThat(mController.isMaximizeCompatibilityEnabled()).isEqualTo(false);
    }

    @Test
    public void setupMaximizeCompatibility_concurrencySupportedAndDisabled_setEnabled() {
        // The precondition of the concurrency supported is ready in setup().
        mController.onPreferenceChange(mPreference, false);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().isBridgedModeOpportunisticShutdownEnabled()).isEqualTo(true);
    }

    @Test
    public void setupMaximizeCompatibility_concurrencySupportedAndEnabled_setDisabled() {
        // The precondition of the concurrency supported is ready in setup().
        mController.onPreferenceChange(mPreference, true);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().isBridgedModeOpportunisticShutdownEnabled()).isEqualTo(false);
    }

    @Test
    public void setupMaximizeCompatibility_noConcurrencyAndSetDisabled_setBand2gOnly() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        mController.onPreferenceChange(mPreference, false);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().getBand())
                .isEqualTo(SoftApConfiguration.BAND_2GHZ | SoftApConfiguration.BAND_5GHZ);
    }

    @Test
    public void setupMaximizeCompatibility_noConcurrencyAndSetEnabled_setBand2gAnd5g() {
        doReturn(false).when(mWifiManager).isBridgedApConcurrencySupported();
        mController.onPreferenceChange(mPreference, true);

        SoftApConfiguration.Builder builder = new SoftApConfiguration.Builder();
        mController.setupMaximizeCompatibility(builder);

        assertThat(builder.build().getBand()).isEqualTo(SoftApConfiguration.BAND_2GHZ);
    }
}
