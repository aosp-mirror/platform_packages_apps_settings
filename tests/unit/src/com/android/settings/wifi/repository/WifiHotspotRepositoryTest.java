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

import static android.net.TetheringManager.TETHERING_WIFI;
import static android.net.wifi.SoftApConfiguration.BAND_2GHZ;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;
import static android.net.wifi.WifiAvailableChannel.OP_MODE_SAP;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED;

import static com.android.settings.wifi.repository.WifiHotspotRepository.BAND_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.BAND_2GHZ_5GHZ_6GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.TetheringManager;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiAvailableChannel;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
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

    static final int WIFI_5GHZ_BAND_PREFERRED = BAND_2GHZ_5GHZ;
    static final int WIFI_6GHZ_BAND_PREFERRED = BAND_2GHZ_5GHZ_6GHZ;
    static final int CHANNEL_NOT_FOUND = -1;
    static final int FREQ_5GHZ = 5000;
    static final int FREQ_6GHZ = 6000;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private MutableLiveData<Integer> mSecurityType;
    @Mock
    private MutableLiveData<Integer> mSpeedType;

    private WifiHotspotRepository mRepository;
    private SoftApConfiguration mSoftApConfiguration = new SoftApConfiguration.Builder().build();
    private ArgumentCaptor<SoftApConfiguration> mSoftApConfigCaptor =
            ArgumentCaptor.forClass(SoftApConfiguration.class);

    @Before
    public void setUp() {
        doReturn(new TestHandler()).when(mContext).getMainThreadHandler();
        doReturn(SPEED_6GHZ).when(mSpeedType).getValue();

        mRepository = new WifiHotspotRepository(mContext, mWifiManager, mTetheringManager);
        mRepository.mSecurityType = mSecurityType;
        mRepository.mSpeedType = mSpeedType;
        mRepository.mIsDualBand = true;
        mRepository.mIs5gAvailable = true;
        mRepository.mIs6gAvailable = true;
    }

    @Test
    public void queryLastPasswordIfNeeded_securityTypeIsOpen_queryLastPassword() {
        mSoftApConfiguration = new SoftApConfiguration.Builder()
                .setSsid(WIFI_SSID)
                .setPassphrase(null, SECURITY_TYPE_OPEN)
                .build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mSoftApConfiguration);

        mRepository.queryLastPasswordIfNeeded();

        verify(mWifiManager).queryLastConfiguredTetheredApPassphraseSinceBoot(any(), any());
    }

    @Test
    public void queryLastPasswordIfNeeded_securityTypeIsNotOpen_notQueryLastPassword() {
        mSoftApConfiguration = new SoftApConfiguration.Builder()
                .setSsid(WIFI_SSID)
                .setPassphrase(WIFI_PASSWORD, SECURITY_TYPE_WPA3_SAE)
                .build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(mSoftApConfiguration);

        mRepository.queryLastPasswordIfNeeded();

        verify(mWifiManager, never())
                .queryLastConfiguredTetheredApPassphraseSinceBoot(any(), any());
    }

    @Test
    public void generatePassword_haveLastPassword_returnLastPassword() {
        mRepository.mLastPassword = WIFI_PASSWORD;

        assertThat(mRepository.generatePassword()).isEqualTo(WIFI_PASSWORD);
    }

    @Test
    public void generatePassword_noLastPassword_returnRandomPassword() {
        mRepository.mLastPassword = "";

        assertThat(mRepository.generatePassword().length()).isNotEqualTo(0);
    }

    @Test
    public void generatePassword_configPasswordIsNotEmpty_returnConfigPassword() {
        mSoftApConfiguration = new SoftApConfiguration.Builder()
                .setPassphrase(WIFI_PASSWORD, SECURITY_TYPE_WPA2_PSK)
                .build();

        assertThat(mRepository.generatePassword(mSoftApConfiguration)).isEqualTo(WIFI_PASSWORD);
    }

    @Test
    public void generatePassword_configPasswordIsEmpty_returnConfigPassword() {
        mSoftApConfiguration = new SoftApConfiguration.Builder().build();
        mRepository.mLastPassword = WIFI_PASSWORD;

        assertThat(mRepository.generatePassword(mSoftApConfiguration)).isEqualTo(WIFI_PASSWORD);
    }

    @Test
    public void getSoftApConfiguration_getConfigFromWifiManager() {
        mRepository.getSoftApConfiguration();

        verify(mWifiManager).getSoftApConfiguration();
    }

    @Test
    public void setSoftApConfiguration_setConfigToWifiManager() {
        mRepository.setSoftApConfiguration(mSoftApConfiguration);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfiguration);
    }

    @Test
    public void setSoftApConfiguration_isRestarting_doNotSetConfig() {
        mRepository.mIsRestarting = true;

        mRepository.setSoftApConfiguration(mSoftApConfiguration);

        verify(mWifiManager, never()).setSoftApConfiguration(mSoftApConfiguration);
    }

    @Test
    public void refresh_liveDataNotUsed_doNothing() {
        // If LiveData is not used then it's null.
        mRepository.mSecurityType = null;
        mRepository.mSpeedType = null;

        mRepository.refresh();

        verify(mWifiManager, never()).getSoftApConfiguration();
    }

    @Test
    public void refresh_liveDataIsUsed_getConfigAndUpdateLiveData() {
        mRepository.getSpeedType();

        mRepository.refresh();

        verify(mWifiManager, atLeast(1)).getSoftApConfiguration();
        verify(mSpeedType).setValue(anyInt());
    }

    @Test
    public void setAutoRefresh_setEnabled_registerCallback() {
        mRepository.mActiveCountryCodeChangedCallback = null;

        mRepository.setAutoRefresh(true);

        verify(mWifiManager).registerActiveCountryCodeChangedCallback(any(), any());
    }

    @Test
    public void setAutoRefresh_setDisabled_registerCallback() {
        mRepository.setAutoRefresh(true);

        mRepository.setAutoRefresh(false);

        verify(mWifiManager).unregisterActiveCountryCodeChangedCallback(any());
    }

    @Test
    @UiThreadTest
    public void getSecurityType_shouldNotReturnNull() {
        // If LiveData is not used then it's null.
        mRepository.mSecurityType = null;
        mockConfigSecurityType(SECURITY_TYPE_OPEN);

        assertThat(mRepository.getSecurityType()).isNotNull();
    }

    @Test
    public void updateSecurityType_securityTypeOpen_setValueCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_OPEN);

        mRepository.updateSecurityType();

        verify(mSecurityType).setValue(SECURITY_TYPE_OPEN);
    }

    @Test
    public void updateSecurityType_securityTypeWpa2_setValueCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_WPA2_PSK);

        mRepository.updateSecurityType();

        verify(mSecurityType).setValue(SECURITY_TYPE_WPA2_PSK);
    }

    @Test
    public void updateSecurityType_securityTypeWpa2Wpa3_setValueCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_WPA3_SAE_TRANSITION);

        mRepository.updateSecurityType();

        verify(mSecurityType).setValue(SECURITY_TYPE_WPA3_SAE_TRANSITION);
    }

    @Test
    public void updateSecurityType_securityTypeWpa3_setValueCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_WPA3_SAE);

        mRepository.updateSecurityType();

        verify(mSecurityType).setValue(SECURITY_TYPE_WPA3_SAE);
    }

    @Test
    public void setSecurityType_sameValue_doNotSetConfig() {
        mockConfigSecurityType(SECURITY_TYPE_WPA3_SAE);

        mRepository.setSecurityType(SECURITY_TYPE_WPA3_SAE);

        verify(mWifiManager, never()).setSoftApConfiguration(any());
    }

    @Test
    public void setSecurityType_wpa3ToWpa2Wpa3_setConfigCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_WPA3_SAE);

        mRepository.setSecurityType(SECURITY_TYPE_WPA3_SAE_TRANSITION);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getSecurityType())
                .isEqualTo(SECURITY_TYPE_WPA3_SAE_TRANSITION);
    }

    @Test
    public void setSecurityType_Wpa2Wpa3ToWpa2_setConfigCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_WPA3_SAE_TRANSITION);

        mRepository.setSecurityType(SECURITY_TYPE_WPA2_PSK);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getSecurityType())
                .isEqualTo(SECURITY_TYPE_WPA2_PSK);
    }

    @Test
    public void setSecurityType_Wpa2ToOpen_setConfigCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_WPA2_PSK);

        mRepository.setSecurityType(SECURITY_TYPE_OPEN);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getSecurityType())
                .isEqualTo(SECURITY_TYPE_OPEN);
    }

    @Test
    public void setSecurityType_OpenToWpa3_setConfigCorrectly() {
        mockConfigSecurityType(SECURITY_TYPE_OPEN);

        mRepository.setSecurityType(SECURITY_TYPE_WPA3_SAE);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getSecurityType())
                .isEqualTo(SECURITY_TYPE_WPA3_SAE);
    }

    @Test
    @UiThreadTest
    public void getSpeedType_shouldNotReturnNull() {
        // If LiveData is not used then it's null.
        mRepository.mSpeedType = null;
        SoftApConfiguration config = new SoftApConfiguration.Builder().setBand(BAND_2GHZ).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        assertThat(mRepository.getSpeedType()).isNotNull();
    }

    @Test
    public void updateSpeedType_singleBand2g_get2gSpeedType() {
        mRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder().setBand(BAND_2GHZ).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_singleBand5gPreferred_get5gSpeedType() {
        mRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_5GHZ);
    }

    @Test
    public void updateSpeedType_singleBand5gPreferredBut5gUnavailable_get2gSpeedType() {
        mRepository.mIsDualBand = false;
        mRepository.mIs5gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_singleBand6gPreferred_get6gSpeedType() {
        mRepository.mIsDualBand = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_6GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_6GHZ);
    }

    @Test
    public void updateSpeedType_singleBand6gPreferredBut6gUnavailable_get5gSpeedType() {
        mRepository.mIsDualBand = false;
        mRepository.mIs6gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_6GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_5GHZ);
    }

    @Test
    public void updateSpeedType_singleBand6gPreferredBut5gAnd6gUnavailable_get2gSpeedType() {
        mRepository.mIsDualBand = false;
        mRepository.mIs5gAvailable = false;
        mRepository.mIs6gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_6GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void updateSpeedType_dualBand2gAnd5g_get2gAnd5gSpeedType() {
        mRepository.mIsDualBand = true;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ_5GHZ);
    }

    @Test
    public void updateSpeedType_dualBand2gAnd5gBut5gUnavailable_get2gSpeedType() {
        mRepository.mIsDualBand = true;
        mRepository.mIs5gAvailable = false;
        SoftApConfiguration config = new SoftApConfiguration.Builder()
                .setBand(WIFI_5GHZ_BAND_PREFERRED).build();
        when(mWifiManager.getSoftApConfiguration()).thenReturn(config);

        mRepository.updateSpeedType();

        verify(mSpeedType).setValue(SPEED_2GHZ);
    }

    @Test
    public void setSpeedType_sameValue_doNotSetConfig() {
        doReturn(SPEED_6GHZ).when(mSpeedType).getValue();

        mRepository.setSpeedType(SPEED_6GHZ);

        verify(mWifiManager, never()).setSoftApConfiguration(any());
    }

    @Test
    public void setSpeedType_2g5ghzTo6ghz_setConfigBandTo6ghzPreferred() {
        mockConfigSpeedType(SPEED_2GHZ_5GHZ);

        mRepository.setSpeedType(SPEED_6GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(BAND_2GHZ_5GHZ_6GHZ);
    }

    @Test
    public void setSpeedType_2g5ghzTo6ghz_setConfigSecurityToWpa3() {
        mockConfig(SPEED_2GHZ_5GHZ, SECURITY_TYPE_WPA3_SAE_TRANSITION);

        mRepository.setSpeedType(SPEED_6GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getSecurityType())
                .isEqualTo(SECURITY_TYPE_WPA3_SAE);
    }

    @Test
    public void setSpeedType_6ghzTo2g5ghz_setConfigBandsTo2g5ghz() {
        mockConfigSpeedType(SPEED_6GHZ);
        mRepository.mIsDualBand = true;

        mRepository.setSpeedType(SPEED_2GHZ_5GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        SparseIntArray channels = mSoftApConfigCaptor.getValue().getChannels();
        assertThat(channels.get(BAND_2GHZ, CHANNEL_NOT_FOUND)).isNotEqualTo(CHANNEL_NOT_FOUND);
        assertThat(channels.get(BAND_2GHZ_5GHZ, CHANNEL_NOT_FOUND)).isNotEqualTo(CHANNEL_NOT_FOUND);
    }

    @Test
    public void setSpeedType_2ghzTo5ghz_setConfigBandTo5ghzPreferred() {
        mockConfigSpeedType(SPEED_2GHZ);

        mRepository.setSpeedType(SPEED_5GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(WIFI_5GHZ_BAND_PREFERRED);
    }

    @Test
    public void setSpeedType_5ghzTo6ghz_setConfigBandTo6ghzPreferred() {
        mockConfigSpeedType(SPEED_5GHZ);

        mRepository.setSpeedType(SPEED_6GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(WIFI_6GHZ_BAND_PREFERRED);
    }

    @Test
    public void setSpeedType_6ghzTo2ghz_setConfigBandTo2ghz() {
        mockConfigSpeedType(SPEED_6GHZ);

        mRepository.setSpeedType(SPEED_2GHZ);

        verify(mWifiManager).setSoftApConfiguration(mSoftApConfigCaptor.capture());
        assertThat(mSoftApConfigCaptor.getValue().getBand()).isEqualTo(BAND_2GHZ);
    }

    @Test
    public void isDualBand_resultSameAsWifiManager() {
        // Reset mIsDualBand to trigger an update
        mRepository.mIsDualBand = null;
        when(mWifiManager.isBridgedApConcurrencySupported()).thenReturn(true);

        assertThat(mRepository.isDualBand()).isTrue();

        // Reset mIsDualBand to trigger an update
        mRepository.mIsDualBand = null;
        when(mWifiManager.isBridgedApConcurrencySupported()).thenReturn(false);

        assertThat(mRepository.isDualBand()).isFalse();
    }

    @Test
    public void is5GHzBandSupported_resultSameAsWifiManager() {
        // Reset mIs5gBandSupported to trigger an update
        mRepository.mIs5gBandSupported = null;
        when(mWifiManager.is5GHzBandSupported()).thenReturn(true);

        assertThat(mRepository.is5GHzBandSupported()).isTrue();

        // Reset mIs5gBandSupported to trigger an update
        mRepository.mIs5gBandSupported = null;
        when(mWifiManager.is5GHzBandSupported()).thenReturn(false);

        assertThat(mRepository.is5GHzBandSupported()).isFalse();
    }

    @Test
    public void is5gAvailable_hasUsableChannels_returnTrue() {
        mRepository.mIs5gBandSupported = true;
        // Reset mIs5gAvailable to trigger an update
        mRepository.mIs5gAvailable = null;
        List<WifiAvailableChannel> channels =
                Arrays.asList(new WifiAvailableChannel(FREQ_5GHZ, OP_MODE_SAP));
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP))
                .thenReturn(channels);

        assertThat(mRepository.is5gAvailable()).isTrue();
    }

    @Test
    public void is5gAvailable_noUsableChannels_returnFalse() {
        mRepository.mIs5gBandSupported = true;
        // Reset mIs5gAvailable to trigger an update
        mRepository.mIs5gAvailable = null;
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP))
                .thenReturn(null);

        assertThat(mRepository.is5gAvailable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void get5gAvailable_shouldNotReturnNull() {
        // Reset m5gAvailable to trigger an update
        mRepository.m5gAvailable = null;

        assertThat(mRepository.get5gAvailable()).isNotNull();
    }

    @Test
    public void is6GHzBandSupported_resultSameAsWifiManager() {
        // Reset mIs6gBandSupported to trigger an update
        mRepository.mIs6gBandSupported = null;
        when(mWifiManager.is6GHzBandSupported()).thenReturn(true);

        assertThat(mRepository.is6GHzBandSupported()).isTrue();

        // Reset mIs6gBandSupported to trigger an update
        mRepository.mIs6gBandSupported = null;
        when(mWifiManager.is6GHzBandSupported()).thenReturn(false);

        assertThat(mRepository.is6GHzBandSupported()).isFalse();
    }

    @Test
    public void is6gAvailable_hasUsableChannels_returnTrue() {
        mRepository.mIs6gBandSupported = true;
        // Reset mIs6gAvailable to trigger an update
        mRepository.mIs6gAvailable = null;
        List<WifiAvailableChannel> channels =
                Arrays.asList(new WifiAvailableChannel(FREQ_6GHZ, OP_MODE_SAP));
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_6_GHZ, OP_MODE_SAP))
                .thenReturn(channels);

        assertThat(mRepository.is6gAvailable()).isTrue();
    }

    @Test
    public void is6gAvailable_noUsableChannels_returnFalse() {
        mRepository.mIs6gBandSupported = true;
        // Reset mIs6gAvailable to trigger an update
        mRepository.mIs6gAvailable = null;
        when(mWifiManager.getUsableChannels(WifiScanner.WIFI_BAND_6_GHZ, OP_MODE_SAP))
                .thenReturn(null);

        assertThat(mRepository.is6gAvailable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void get6gAvailable_shouldNotReturnNull() {
        // Reset m6gAvailable to trigger an update
        mRepository.m6gAvailable = null;

        assertThat(mRepository.get6gAvailable()).isNotNull();
    }

    @Test
    public void isSpeedFeatureAvailable_configNotShow_returnFalse() {
        mRepository.mIsConfigShowSpeed = false;

        assertThat(mRepository.isSpeedFeatureAvailable()).isFalse();
    }

    @Test
    public void isSpeedFeatureAvailable_5gBandNotSupported_returnFalse() {
        mRepository.mIsConfigShowSpeed = true;
        mRepository.mIs5gBandSupported = false;

        assertThat(mRepository.isSpeedFeatureAvailable()).isFalse();
    }

    @Test
    public void isSpeedFeatureAvailable_throwExceptionWhenGet5gSapChannel_returnFalse() {
        mRepository.mIsConfigShowSpeed = true;
        mRepository.mIs5gBandSupported = true;
        doThrow(IllegalArgumentException.class).when(mWifiManager)
                .getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP);

        assertThat(mRepository.isSpeedFeatureAvailable()).isFalse();

        doThrow(UnsupportedOperationException.class).when(mWifiManager)
                .getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP);

        assertThat(mRepository.isSpeedFeatureAvailable()).isFalse();
    }

    @Test
    public void isSpeedFeatureAvailable_throwExceptionWhenGet6gSapChannel_returnFalse() {
        mRepository.mIsConfigShowSpeed = true;
        mRepository.mIs5gBandSupported = true;
        doReturn(Arrays.asList(new WifiAvailableChannel(FREQ_5GHZ, OP_MODE_SAP))).when(mWifiManager)
                .getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP);
        doThrow(IllegalArgumentException.class).when(mWifiManager)
                .getUsableChannels(WifiScanner.WIFI_BAND_6_GHZ, OP_MODE_SAP);

        assertThat(mRepository.isSpeedFeatureAvailable()).isFalse();

        doThrow(UnsupportedOperationException.class).when(mWifiManager)
                .getUsableChannels(WifiScanner.WIFI_BAND_6_GHZ, OP_MODE_SAP);

        assertThat(mRepository.isSpeedFeatureAvailable()).isFalse();
    }

    @Test
    public void isSpeedFeatureAvailable_conditionsAreReady_returnTrue() {
        mRepository.mIsConfigShowSpeed = true;
        mRepository.mIs5gBandSupported = true;
        doReturn(Arrays.asList(new WifiAvailableChannel(FREQ_5GHZ, OP_MODE_SAP))).when(mWifiManager)
                .getUsableChannels(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS, OP_MODE_SAP);
        doReturn(Arrays.asList(new WifiAvailableChannel(FREQ_6GHZ, OP_MODE_SAP))).when(mWifiManager)
                .getUsableChannels(WifiScanner.WIFI_BAND_6_GHZ, OP_MODE_SAP);

        assertThat(mRepository.isSpeedFeatureAvailable()).isTrue();
    }

    @Test
    @UiThreadTest
    public void getRestarting_shouldNotReturnNull() {
        // Reset mIsRestarting to trigger an update
        mRepository.mRestarting = null;

        assertThat(mRepository.getRestarting()).isNotNull();
    }

    @Test
    public void restartTetheringIfNeeded_stateDisabled_doNotStopTethering() {
        mRepository.mWifiApState = WIFI_AP_STATE_DISABLED;

        mRepository.restartTetheringIfNeeded();

        verify(mTetheringManager, never()).stopTethering(TETHERING_WIFI);
    }

    @Test
    public void restartTetheringIfNeeded_stateEnabled_stopTethering() {
        mRepository.mWifiApState = WIFI_AP_STATE_ENABLED;

        mRepository.restartTetheringIfNeeded();

        verify(mTetheringManager).stopTethering(TETHERING_WIFI);
    }

    @Test
    public void onStateChanged_stateDisabledAndRestartingFalse_doNotStartTethering() {
        mRepository.mIsRestarting = false;

        mRepository.mSoftApCallback.onStateChanged(WIFI_AP_STATE_DISABLED, 0);

        verify(mTetheringManager, never()).startTethering(TETHERING_WIFI,
                mContext.getMainExecutor(), mRepository.mStartTetheringCallback);
    }

    @Test
    public void onStateChanged_stateDisabledAndRestartingTrue_startTethering() {
        mRepository.mIsRestarting = true;

        mRepository.mSoftApCallback.onStateChanged(WIFI_AP_STATE_DISABLED, 0);

        verify(mTetheringManager).startTethering(TETHERING_WIFI, mContext.getMainExecutor(),
                mRepository.mStartTetheringCallback);
    }

    @Test
    public void onStateChanged_stateEnabledAndRestartingTrue_setRestartingFalse() {
        mRepository.mIsRestarting = true;

        mRepository.mSoftApCallback.onStateChanged(WIFI_AP_STATE_ENABLED, 0);

        assertThat(mRepository.mIsRestarting).isFalse();
    }

    private void mockConfigSecurityType(int securityType) {
        mockConfig(securityType, SPEED_2GHZ);
    }

    private void mockConfigSpeedType(int speedType) {
        mockConfig(SECURITY_TYPE_WPA3_SAE, speedType);
    }

    private void mockConfig(int securityType, int speedType) {
        SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder();
        // Security Type
        doReturn(securityType).when(mSecurityType).getValue();
        String passphrase = (securityType == SECURITY_TYPE_OPEN) ? null : WIFI_PASSWORD;
        configBuilder.setPassphrase(passphrase, securityType).build();

        // Speed Type
        doReturn(speedType).when(mSpeedType).getValue();
        mRepository.mIsDualBand = true;
        if (speedType == SPEED_2GHZ) {
            mRepository.mIsDualBand = false;
            configBuilder.setBand(BAND_2GHZ);
        } else if (speedType == SPEED_5GHZ) {
            mRepository.mIsDualBand = false;
            configBuilder.setBand(BAND_2GHZ_5GHZ);
        } else if (speedType == SPEED_2GHZ_5GHZ) {
            int[] bands = {BAND_2GHZ, BAND_2GHZ_5GHZ};
            configBuilder.setBands(bands);
        } else if (speedType == SPEED_6GHZ) {
            configBuilder.setBand(BAND_2GHZ_5GHZ_6GHZ);
        }
        when(mWifiManager.getSoftApConfiguration()).thenReturn(configBuilder.build());
    }

    private static class TestHandler extends Handler {

        TestHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
            msg.getCallback().run();
            return true;
        }
    }
}
