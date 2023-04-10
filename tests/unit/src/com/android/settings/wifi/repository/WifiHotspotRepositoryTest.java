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
import static android.net.wifi.WifiAvailableChannel.OP_MODE_SAP;

import static com.android.settings.wifi.repository.WifiHotspotRepository.BAND_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.BAND_2GHZ_5GHZ_6GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiAvailableChannel;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.util.SparseIntArray;

import androidx.lifecycle.MutableLiveData;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WifiHotspotRepositoryTest {
    static final String WIFI_SSID = "wifi_ssid";
    static final String WIFI_PASSWORD = "wifi_password";
    static final String WIFI_CURRENT_COUNTRY_CODE = "US";

    static final int WIFI_5GHZ_BAND_PREFERRED = BAND_2GHZ_5GHZ;
    static final int WIFI_6GHZ_BAND_PREFERRED = BAND_2GHZ_5GHZ_6GHZ;
    static final int CHANNEL_NOT_FOUND = -1;
    static final int FREQ_5GHZ = 5000;
    static final int FREQ_6GHZ = 6000;

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
    ArgumentCaptor<SoftApConfiguration> mSoftApConfigCaptor =
            ArgumentCaptor.forClass(SoftApConfiguration.class);

    @Before
    public void setUp() {
        doReturn(SPEED_6GHZ).when(mSpeedType).getValue();

        mWifiHotspotRepository = new WifiHotspotRepository(mContext, mWifiManager);
        mWifiHotspotRepository.mSpeedType = mSpeedType;
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
        mWifiHotspotRepository.getSpeedType();

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

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_singleBand5gPreferred_get5gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

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

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_singleBand6gPreferred_get6gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_6GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

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

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_dualBand2gAnd5g_get2gAnd5gSpeedType() {
        mWifiHotspotRepository.mIsDualBand = true;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

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

        mWifiHotspotRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void setSpeedType_sameValue_doNotSetConfig() {
        doReturn(SPEED_6GHZ).when(mSpeedType).getValue();

        mWifiHotspotRepository.setSpeedType(SPEED_6GHZ);

        verify(mWifiManager, never()).setSoftApConfiguration(any());
    }

    @Test
    public void setSpeedType_from2g5ghz_setConfigBandTo6ghzPreferred() {
        mockGetSoftApConfiguration(SPEED_2GHZ_5GHZ);

        mWifiHotspotRepository.setSpeedType(SPEED_6GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(BAND_2GHZ_5GHZ_6GHZ);
    }

    @Test
    public void setSpeedType_from6ghz_setConfigBandsTo2g5ghz() {
        mockGetSoftApConfiguration(SPEED_6GHZ);
        mWifiHotspotRepository.mIsDualBand = true;

        mWifiHotspotRepository.setSpeedType(SPEED_2GHZ_5GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        SparseIntArray channels = mSoftApConfigCaptor.getValue().getChannels();
        assertThat(channels.get(BAND_2GHZ, CHANNEL_NOT_FOUND)).isNotEqualTo(CHANNEL_NOT_FOUND);
        assertThat(channels.get(BAND_2GHZ_5GHZ, CHANNEL_NOT_FOUND)).isNotEqualTo(CHANNEL_NOT_FOUND);
    }

    @Test
    public void setSpeedType_from2ghz_setConfigBandTo5ghzPreferred() {
        mockGetSoftApConfiguration(SPEED_2GHZ);

        mWifiHotspotRepository.setSpeedType(SPEED_5GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(WIFI_5GHZ_BAND_PREFERRED);
    }

    @Test
    public void setSpeedType_from5ghz_setConfigBandTo6ghzPreferred() {
        mockGetSoftApConfiguration(SPEED_5GHZ);

        mWifiHotspotRepository.setSpeedType(SPEED_6GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(WIFI_6GHZ_BAND_PREFERRED);
    }

    @Test
    public void setSpeedType_from5gTo6ghz_setConfigBandTo2ghz() {
        mockGetSoftApConfiguration(SPEED_6GHZ);

        mWifiHotspotRepository.setSpeedType(SPEED_2GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(BAND_2GHZ);
    }

    @Test
    public void isDualBand_resultSameAsWifiManager() {
        // Reset mIsDualBand to trigger an update
        mWifiHotspotRepository.mIsDualBand = null;
        when(mWifiManager.isBridgedApConcurrencySupported()).thenReturn(true);

        assertThat(mWifiHotspotRepository.isDualBand()).isTrue();

        // Reset mIsDualBand to trigger an update
        mWifiHotspotRepository.mIsDualBand = null;
        when(mWifiManager.isBridgedApConcurrencySupported()).thenReturn(false);

        assertThat(mWifiHotspotRepository.isDualBand()).isFalse();
    }

    @Test
    public void is5GHzBandSupported_resultSameAsWifiManager() {
        // Reset mIs5gBandSupported to trigger an update
        mWifiHotspotRepository.mIs5gBandSupported = null;
        when(mWifiManager.is5GHzBandSupported()).thenReturn(true);

        assertThat(mWifiHotspotRepository.is5GHzBandSupported()).isTrue();

        // Reset mIs5gBandSupported to trigger an update
        mWifiHotspotRepository.mIs5gBandSupported = null;
        when(mWifiManager.is5GHzBandSupported()).thenReturn(false);

        assertThat(mWifiHotspotRepository.is5GHzBandSupported()).isFalse();
    }

    @Test
    public void is5gAvailable_hasUsableChannels_returnTrue() {
        mWifiHotspotRepository.mIs5gBandSupported = true;
        // Reset mIs5gAvailable to trigger an update
        mWifiHotspotRepository.mIs5gAvailable = null;
        List<WifiAvailableChannel> channels =
                Arrays.asList(new WifiAvailableChannel(FREQ_5GHZ, OP_MODE_SAP));
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP))
                .thenReturn(channels);

        assertThat(mWifiHotspotRepository.is5gAvailable()).isTrue();
    }

    @Test
    public void is5gAvailable_noUsableChannels_returnFalse() {
        mWifiHotspotRepository.mIs5gBandSupported = true;
        // Reset mIs5gAvailable to trigger an update
        mWifiHotspotRepository.mIs5gAvailable = null;
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP))
                .thenReturn(null);

        assertThat(mWifiHotspotRepository.is5gAvailable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void get5gAvailable_shouldNotReturnNull() {
        // Reset m5gAvailable to trigger an update
        mWifiHotspotRepository.m5gAvailable = null;

        assertThat(mWifiHotspotRepository.get5gAvailable()).isNotNull();
    }

    @Test
    public void is6GHzBandSupported_resultSameAsWifiManager() {
        // Reset mIs6gBandSupported to trigger an update
        mWifiHotspotRepository.mIs6gBandSupported = null;
        when(mWifiManager.is6GHzBandSupported()).thenReturn(true);

        assertThat(mWifiHotspotRepository.is6GHzBandSupported()).isTrue();

        // Reset mIs6gBandSupported to trigger an update
        mWifiHotspotRepository.mIs6gBandSupported = null;
        when(mWifiManager.is6GHzBandSupported()).thenReturn(false);

        assertThat(mWifiHotspotRepository.is6GHzBandSupported()).isFalse();
    }

    @Test
    public void is6gAvailable_hasUsableChannels_returnTrue() {
        mWifiHotspotRepository.mIs6gBandSupported = true;
        // Reset mIs6gAvailable to trigger an update
        mWifiHotspotRepository.mIs6gAvailable = null;
        List<WifiAvailableChannel> channels =
                Arrays.asList(new WifiAvailableChannel(FREQ_6GHZ, OP_MODE_SAP));
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_6_GHZ, OP_MODE_SAP))
                .thenReturn(channels);

        assertThat(mWifiHotspotRepository.is6gAvailable()).isTrue();
    }

    @Test
    public void is6gAvailable_noUsableChannels_returnFalse() {
        mWifiHotspotRepository.mIs6gBandSupported = true;
        // Reset mIs6gAvailable to trigger an update
        mWifiHotspotRepository.mIs6gAvailable = null;
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_6_GHZ, OP_MODE_SAP))
                .thenReturn(null);

        assertThat(mWifiHotspotRepository.is6gAvailable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void get6gAvailable_shouldNotReturnNull() {
        // Reset m6gAvailable to trigger an update
        mWifiHotspotRepository.m6gAvailable = null;

        assertThat(mWifiHotspotRepository.get6gAvailable()).isNotNull();
    }

    private void mockGetSoftApConfiguration(int speedType) {
        doReturn(speedType).when(mSpeedType).getValue();
        mWifiHotspotRepository.mIsDualBand = true;
        SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder();
        if (speedType == SPEED_2GHZ) {
            mWifiHotspotRepository.mIsDualBand = false;
            configBuilder.setBand(BAND_2GHZ);
        } else if (speedType == SPEED_5GHZ) {
            mWifiHotspotRepository.mIsDualBand = false;
            configBuilder.setBand(BAND_2GHZ_5GHZ);
        } else if (speedType == SPEED_2GHZ_5GHZ) {
            int[] bands = {BAND_2GHZ, BAND_2GHZ_5GHZ};
            configBuilder.setBands(bands);
        } else if (speedType == SPEED_6GHZ) {
            configBuilder.setBand(BAND_2GHZ_5GHZ_6GHZ);
        }
        when(mWifiManager.getSoftApConfiguration()).thenReturn(configBuilder.build());
    }
}
