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

package com.android.settings.wifi.tether;

import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;

import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;
import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_NONE;
import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_WPA2;
import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_WPA2_WPA3;
import static com.android.settings.wifi.tether.WifiHotspotSecurityViewModel.KEY_SECURITY_WPA3;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@UiThreadTest
public class WifiHotspotSecurityViewModelTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    WifiHotspotRepository mWifiHotspotRepository;
    @Mock
    MutableLiveData<Integer> mSecurityType;
    @Mock
    MutableLiveData<Integer> mSpeedType;
    @Mock
    private MutableLiveData<Boolean> mRestarting;

    WifiHotspotSecurityViewModel mViewModel;

    @Before
    public void setUp() {
        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.getWifiFeatureProvider().getWifiHotspotRepository())
                .thenReturn(mWifiHotspotRepository);
        when(mWifiHotspotRepository.getSecurityType()).thenReturn(mSecurityType);
        when(mWifiHotspotRepository.getSpeedType()).thenReturn(mSpeedType);
        when(mWifiHotspotRepository.getRestarting()).thenReturn(mRestarting);

        mViewModel = new WifiHotspotSecurityViewModel((Application) mContext);
    }

    @Test
    public void constructor_observeDataAndSetAutoRefresh() {
        verify(mSecurityType).observeForever(mViewModel.mSecurityTypeObserver);
        verify(mSpeedType).observeForever(mViewModel.mSpeedTypeObserver);
    }

    @Test
    public void onCleared_removeObserverData() {
        mViewModel.onCleared();

        verify(mSecurityType).removeObserver(mViewModel.mSecurityTypeObserver);
        verify(mSpeedType).removeObserver(mViewModel.mSpeedTypeObserver);
    }

    @Test
    public void onSecurityTypeChanged_securityTypeWpa3_setCheckedCorrectly() {
        mViewModel.onSecurityTypeChanged(SECURITY_TYPE_WPA3_SAE);

        assertItemChecked(true, false, false, false);
    }

    @Test
    public void onSecurityTypeChanged_securityTypeWpa2Wpa3_setCheckedCorrectly() {
        mViewModel.onSecurityTypeChanged(SECURITY_TYPE_WPA3_SAE_TRANSITION);

        assertItemChecked(false, true, false, false);
    }

    @Test
    public void onSecurityTypeChanged_securityTypeWpa2_setCheckedCorrectly() {
        mViewModel.onSecurityTypeChanged(SECURITY_TYPE_WPA2_PSK);

        assertItemChecked(false, false, true, false);
    }

    @Test
    public void onSecurityTypeChanged_securityTypeNone_setCheckedCorrectly() {
        mViewModel.onSecurityTypeChanged(SECURITY_TYPE_OPEN);

        assertItemChecked(false, false, false, true);
    }

    @Test
    public void onSpeedTypeChanged_speed6g_setEnabledCorrectly() {
        mViewModel.onSpeedTypeChanged(SPEED_6GHZ);

        assertItemEnabled(true, false, false, false);
    }

    @Test
    public void onSpeedTypeChanged_speed2g5g_setEnabledCorrectly() {
        mViewModel.onSpeedTypeChanged(SPEED_2GHZ_5GHZ);

        assertItemEnabled(true, true, true, true);
    }

    @Test
    public void onSpeedTypeChanged_speed5g_setEnabledCorrectly() {
        mViewModel.onSpeedTypeChanged(SPEED_5GHZ);

        assertItemEnabled(true, true, true, true);
    }

    @Test
    public void onSpeedTypeChanged_speed2g_setEnabledCorrectly() {
        mViewModel.onSpeedTypeChanged(SPEED_2GHZ);

        assertItemEnabled(true, true, true, true);
    }

    @Test
    public void handleRadioButtonClicked_keyWpa3_setSecurityTypeCorrectly() {
        mViewModel.handleRadioButtonClicked(KEY_SECURITY_WPA3);

        verify(mWifiHotspotRepository).setSecurityType(SECURITY_TYPE_WPA3_SAE);
    }

    @Test
    public void handleRadioButtonClicked_keyWpa2Wpa3_setSecurityTypeCorrectly() {
        mViewModel.handleRadioButtonClicked(KEY_SECURITY_WPA2_WPA3);

        verify(mWifiHotspotRepository).setSecurityType(SECURITY_TYPE_WPA3_SAE_TRANSITION);
    }

    @Test
    public void handleRadioButtonClicked_keyWpa2_setSecurityTypeCorrectly() {
        mViewModel.handleRadioButtonClicked(KEY_SECURITY_WPA2);

        verify(mWifiHotspotRepository).setSecurityType(SECURITY_TYPE_WPA2_PSK);
    }

    @Test
    public void handleRadioButtonClicked_keyNone_setSecurityTypeCorrectly() {
        mViewModel.handleRadioButtonClicked(KEY_SECURITY_NONE);

        verify(mWifiHotspotRepository).setSecurityType(SECURITY_TYPE_OPEN);
    }

    @Test
    public void getViewItemListData_shouldNotReturnNull() {
        // Reset mViewInfoListData to trigger an update
        mViewModel.mViewInfoListData = null;

        assertThat(mViewModel.getViewItemListData()).isNotNull();
    }

    @Test
    public void getRestarting_shouldNotReturnNull() {
        assertThat(mViewModel.getRestarting()).isNotNull();
    }

    private void assertItemChecked(boolean checkedWpa3, boolean checkedWpa2Wpa3,
            boolean checkedWpa2, boolean checkedNone) {
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_WPA3_SAE).mIsChecked)
                .isEqualTo(checkedWpa3);
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_WPA3_SAE_TRANSITION).mIsChecked)
                .isEqualTo(checkedWpa2Wpa3);
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_WPA2_PSK).mIsChecked)
                .isEqualTo(checkedWpa2);
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_OPEN).mIsChecked)
                .isEqualTo(checkedNone);
    }

    private void assertItemEnabled(boolean enabledWpa3, boolean enabledWpa2Wpa3,
            boolean enabledWpa2, boolean enabledNone) {
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_WPA3_SAE).mIsEnabled)
                .isEqualTo(enabledWpa3);
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_WPA3_SAE_TRANSITION).mIsEnabled)
                .isEqualTo(enabledWpa2Wpa3);
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_WPA2_PSK).mIsEnabled)
                .isEqualTo(enabledWpa2);
        assertThat(mViewModel.mViewItemMap.get(SECURITY_TYPE_OPEN).mIsEnabled)
                .isEqualTo(enabledNone);
    }
}
