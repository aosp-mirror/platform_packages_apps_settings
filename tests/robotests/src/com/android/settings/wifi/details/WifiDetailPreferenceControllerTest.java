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
package com.android.settings.wifi.details;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.NetworkBadging;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.wifi.WifiDetailPreference;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiDetailPreferenceControllerTest {

    private static final int LEVEL = 1;
    private static final int RSSI = -55;
    private static final String SECURITY = "None";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mockScreen;

    @Mock private AccessPoint mockAccessPoint;
    @Mock private WifiManager mockWifiManager;
    @Mock private NetworkInfo mockNetworkInfo;
    @Mock private WifiConfiguration mockWifiConfig;
    @Mock private WifiInfo mockWifiInfo;

    @Mock private Preference mockConnectionDetailPref;
    @Mock private WifiDetailPreference mockSignalStrengthPref;
    @Mock private WifiDetailPreference mockFrequencyPref;
    @Mock private WifiDetailPreference mockSecurityPref;
    @Mock private WifiDetailPreference mockIpAddressPref;
    @Mock private WifiDetailPreference mockRouterPref;
    @Mock private WifiDetailPreference mockSubnetPref;
    @Mock private WifiDetailPreference mockDnsPref;
    @Mock private PreferenceCategory mockIpv6AddressCategory;

    private Context mContext = RuntimeEnvironment.application;
    private Lifecycle mLifecycle;
    private WifiDetailPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mLifecycle = new Lifecycle();
        mController = new WifiDetailPreferenceController(
                mockAccessPoint, mContext, mLifecycle, mockWifiManager);

        when(mockAccessPoint.getConfig()).thenReturn(mockWifiConfig);
        when(mockAccessPoint.getLevel()).thenReturn(LEVEL);
        when(mockAccessPoint.getNetworkInfo()).thenReturn(mockNetworkInfo);
        when(mockAccessPoint.getRssi()).thenReturn(RSSI);
        when(mockAccessPoint.getSecurityString(false)).thenReturn(SECURITY);

        setupMockedPreferenceScreen();

        when (mockWifiInfo.getRssi()).thenReturn(RSSI);
        when(mockWifiManager.getConnectionInfo()).thenReturn(mockWifiInfo);
        when(mockWifiManager.getWifiApConfiguration()).thenReturn(mockWifiConfig);
    }

    private void setupMockedPreferenceScreen() {

        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_CONNECTION_DETAIL_PREF))
                .thenReturn(mockConnectionDetailPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SIGNAL_STRENGTH_PREF))
                .thenReturn(mockSignalStrengthPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_FREQUENCY_PREF))
                .thenReturn(mockFrequencyPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SECURITY_PREF))
                .thenReturn(mockSecurityPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_IP_ADDRESS_PREF))
                .thenReturn(mockIpAddressPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_ROUTER_PREF))
                .thenReturn(mockRouterPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_SUBNET_MASK_PREF))
                .thenReturn(mockSubnetPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_DNS_PREF))
                .thenReturn(mockDnsPref);
        when(mockScreen.findPreference(WifiDetailPreferenceController.KEY_IPV6_ADDRESS_CATEGORY))
                .thenReturn(mockIpv6AddressCategory);

        mController.displayPreference(mockScreen);
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void securityPreference_stringShouldBeSet() {
        verify(mockSecurityPref).setDetailText(SECURITY);
    }

    @Test
    public void latestWifiInfoAndConfig_shouldBeFetchedOnResume() {
        mController.onResume();

        verify(mockWifiManager).getConnectionInfo();
        verify(mockWifiManager).getWifiApConfiguration();
    }

    @Test
    public void connectionDetailPref_shouldHaveIconSet() {
        Drawable expectedIcon =
                NetworkBadging.getWifiIcon(LEVEL, NetworkBadging.BADGING_NONE, mContext.getTheme());

        mController.onResume();

        verify(mockConnectionDetailPref).setIcon(expectedIcon);
    }

    @Test
    public void connectionDetailPref_shouldHaveTitleSet() {
        String summary = "summary";
        when(mockAccessPoint.getSettingsSummary()).thenReturn(summary);

        mController.onResume();

        verify(mockConnectionDetailPref).setTitle(summary);
    }

    @Test
    public void signalStrengthPref_shouldHaveIconSet() {
        mController.onResume();

        verify(mockSignalStrengthPref).setIcon(any(Drawable.class));
    }

    @Test
    public void signalStrengthPref_shouldHaveDetailTextSet() {
        String expectedStrength =
                mContext.getResources().getStringArray(R.array.wifi_signal)[LEVEL];

        mController.onResume();

        verify(mockSignalStrengthPref).setDetailText(expectedStrength);
    }
}
