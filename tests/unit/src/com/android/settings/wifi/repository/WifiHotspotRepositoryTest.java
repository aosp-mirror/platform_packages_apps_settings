/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.repository;

import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiHotspotRepositoryTest {
    static final String WIFI_SSID = "wifi_ssid";
    static final String WIFI_PASSWORD = "wifi_password";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    WifiManager mWifiManager;

    WifiHotspotRepository mWifiHotspotRepository;
    SoftApConfiguration mSoftApConfiguration;

    @Before
    public void setUp() {
        mWifiHotspotRepository = new WifiHotspotRepository(mContext, mWifiManager);
    }

    @Test
    public void queryLastPasswordIfNeeded_securityTypeIsOpen_queryLastPassword() {
        mSoftApConfiguration = new SoftApConfiguration.Builder()
                .setSsid(WIFI_SSID)
                .setPassphrase(null, SECURITY_TYPE_OPEN)
                .build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mSoftApConfiguration);

        mWifiHotspotRepository.queryLastPasswordIfNeeded();

        verify(mWifiManager).queryLastConfiguredTetheredApPassphraseSinceBoot(any(), any());
    }

    @Test
    public void queryLastPasswordIfNeeded_securityTypeIsNotOpen_notQueryLastPassword() {
        mSoftApConfiguration = new SoftApConfiguration.Builder()
                .setSsid(WIFI_SSID)
                .setPassphrase(WIFI_PASSWORD, SECURITY_TYPE_WPA3_SAE)
                .build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mSoftApConfiguration);

        mWifiHotspotRepository.queryLastPasswordIfNeeded();

        verify(mWifiManager, never())
                .queryLastConfiguredTetheredApPassphraseSinceBoot(any(), any());
    }

    @Test
    public void generatePassword_haveLastPassword_returnLastPassword() {
        mWifiHotspotRepository.mLastPassword = WIFI_PASSWORD;

        assertThat(mWifiHotspotRepository.generatePassword()).isEqualTo(WIFI_PASSWORD);
    }

    @Test
    public void generatePassword_noLastPassword_returnRandomPassword() {
        mWifiHotspotRepository.mLastPassword = "";

        String password = mWifiHotspotRepository.generatePassword();

        assertThat(password).isNotEqualTo(WIFI_PASSWORD);
        assertThat(password.length()).isNotEqualTo(0);
    }
}
