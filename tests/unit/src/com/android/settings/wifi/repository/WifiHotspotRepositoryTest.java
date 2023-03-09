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

import static android.net.wifi.SoftApConfiguration.BAND_2GHZ;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;

import static com.android.settings.wifi.repository.WifiHotspotRepository.BAND_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.BAND_2GHZ_5GHZ_6GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;

import androidx.lifecycle.MutableLiveData;
import androidx.test.annotation.UiThreadTest;
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
    static final String WIFI_CURRENT_COUNTRY_CODE = "US";

    static final int WIFI_5GHZ_BAND_PREFERRED = BAND_2GHZ_5GHZ;
    static final int WIFI_6GHZ_BAND_PREFERRED = BAND_2GHZ_5GHZ_6GHZ;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    WifiManager mWifiManager;
    @Mock
    MutableLiveData<Integer> mSpeedType;

    WifiHotspotRepository mWifiHotspotRepository;
    SoftApConfiguration mSoftApConfiguration;

    @Before
    public void setUp() {
        mWifiHotspotRepository = new WifiHotspotRepository(mContext, mWifiManager);
        mWifiHotspotRepository.mCurrentCountryCode = WIFI_CURRENT_COUNTRY_CODE;
        mWifiHotspotRepository.mIsDualBand = true;
        mWifiHotspotRepository.mIs5gAvailable = true;
        mWifiHotspotRepository.mIs6gAvailable = true;
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

    @Test
    public void setSoftApConfiguration_setConfigByWifiManager() {
        SoftApConfiguration config = new SoftApConfiguration.Builder().build();

        mWifiHotspotRepository.setSoftApConfiguration(config);

        verify(mWifiManager).setSoftApConfiguration(config);
    }

    @Test
    public void refresh_liveDataNotUsed_doNothing() {
        // If LiveData is not used then it's null.
        mWifiHotspotRepository.mSpeedType = null;

        mWifiHotspotRepository.refresh();

        verify(mWifiManager, never()).getSoftApConfiguration();
    }

    @Test
    public void refresh_liveDataIsUsed_getConfigAndUpdateLiveData() {
        // If LiveData is used then it's not null.
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.refresh();

        verify(mWifiManager).getSoftApConfiguration();
        verify(mSpeedType).setValue(anyInt());
    }

    @Test
    public void setAutoRefresh_setEnabled_registerCallback() {
        mWifiHotspotRepository.mActiveCountryCodeChangedCallback = null;

        mWifiHotspotRepository.setAutoRefresh(true);

        verify(mWifiManager).registerActiveCountryCodeChangedCallback(any(), any());
    }

    @Test
    public void setAutoRefresh_setDisabled_registerCallback() {
        mWifiHotspotRepository.setAutoRefresh(true);

        mWifiHotspotRepository.setAutoRefresh(false);

        verify(mWifiManager).unregisterActiveCountryCodeChangedCallback(any());
    }

    @Test
    @UiThreadTest
    public void getSpeedType_shouldNotReturnNull() {
        // If LiveData is not used then it's null.
        mWifiHotspotRepository.mSpeedType = null;
        SoftApConfiguration config = new SoftApConfiguration.Builder().setBand(BAND_2GHZ).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        assertThat(mWifiHotspotRepository.getSpeedType()).isNotNull();
    }

    @Test
    public void updateSpeedType_singleBand2g_get2gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder().setBand(BAND_2GHZ).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_singleBand5gPreferred_get5gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_5GHZ);
    }

    @Test
    public void updateSpeedType_singleBand5gPreferredBut5gUnavailable_get2gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        mWifiHotspotRepository.mIs5gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_singleBand6gPreferred_get6gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_6GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_6GHZ);
    }

    @Test
    public void updateSpeedType_singleBand6gPreferredBut6gUnavailable_get5gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        mWifiHotspotRepository.mIs6gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_6GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_5GHZ);
    }

    @Test
    public void updateSpeedType_singleBand6gPreferredBut5gAnd6gUnavailable_get2gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        mWifiHotspotRepository.mIs5gAvailable = false;
        mWifiHotspotRepository.mIs6gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_6GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_dualBand2gAnd5g_get2gAnd5gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = true;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ_5GHZ);
    }

    @Test
    public void updateSpeedType_dualBand2gAnd5gBut5gUnavailable_get2gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = true;
        mWifiHotspotRepository.mIs5gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);
        mWifiHotspotRepository.mSpeedType = mSpeedType;

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }
}
