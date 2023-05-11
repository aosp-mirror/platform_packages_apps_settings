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

import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_2GHZ_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_5GHZ;
import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class WifiHotspotSpeedViewModelTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    WifiHotspotRepository mWifiHotspotRepository;
    @Mock
    MutableLiveData<Integer> mSpeedType;
    @Mock
    MutableLiveData<Boolean> m5gAvailable;
    @Mock
    MutableLiveData<Boolean> m6gAvailable;
    @Mock
    MutableLiveData<Map<Integer, WifiHotspotSpeedViewModel.SpeedInfo>> mSpeedInfoMapData;
    @Mock
    private MutableLiveData<Boolean> mRestarting;

    WifiHotspotSpeedViewModel mViewModel;

    @Before
    public void setUp() {
        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.getWifiFeatureProvider().getWifiHotspotRepository())
                .thenReturn(mWifiHotspotRepository);
        when(mWifiHotspotRepository.getSpeedType()).thenReturn(mSpeedType);
        when(mWifiHotspotRepository.is5GHzBandSupported()).thenReturn(true);
        when(mWifiHotspotRepository.get5gAvailable()).thenReturn(m5gAvailable);
        when(mWifiHotspotRepository.is6GHzBandSupported()).thenReturn(true);
        when(mWifiHotspotRepository.get6gAvailable()).thenReturn(m6gAvailable);
        when(mWifiHotspotRepository.getRestarting()).thenReturn(mRestarting);

        mViewModel = new WifiHotspotSpeedViewModel((Application) mContext);
        mViewModel.mSpeedInfoMapData = mSpeedInfoMapData;
    }

    @Test
    @UiThreadTest
    public void constructor_observeDataAndSetAutoRefresh() {
        verify(mSpeedType).observeForever(mViewModel.mSpeedTypeObserver);
        verify(m5gAvailable).observeForever(mViewModel.m5gAvailableObserver);
        verify(m6gAvailable).observeForever(mViewModel.m6gAvailableObserver);
        verify(mWifiHotspotRepository).setAutoRefresh(true);
    }

    @Test
    @UiThreadTest
    public void constructor_supported6GHzBand_set6gVisible() {
        assertThat(mViewModel.mSpeedInfo6g.mIsVisible).isTrue();
    }

    @Test
    @UiThreadTest
    public void constructor_notSupported6GHzBand_set6gVisible() {
        when(mWifiHotspotRepository.is6GHzBandSupported()).thenReturn(false);

        WifiHotspotSpeedViewModel viewModel = new WifiHotspotSpeedViewModel((Application) mContext);

        assertThat(viewModel.mSpeedInfo6g.mIsVisible).isFalse();
    }

    @Test
    @UiThreadTest
    public void onCleared_removeObserverData() {
        mViewModel.onCleared();

        verify(mSpeedType).removeObserver(mViewModel.mSpeedTypeObserver);
        verify(m5gAvailable).removeObserver(mViewModel.m5gAvailableObserver);
        verify(m6gAvailable).removeObserver(mViewModel.m6gAvailableObserver);
    }

    @Test
    @UiThreadTest
    public void on6gAvailableChanged_itsAvailable_setLiveData6gEnabled() {
        mViewModel.mSpeedInfo6g.mIsEnabled = false;

        mViewModel.on6gAvailableChanged(true);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_6GHZ).mIsEnabled).isTrue();
    }

    @Test
    @UiThreadTest
    public void on6gAvailableChanged_notAvailable_setLiveData6gDisabled() {
        mViewModel.mSpeedInfo6g.mIsEnabled = true;

        mViewModel.on6gAvailableChanged(false);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_6GHZ).mIsEnabled).isFalse();
    }

    @Test
    @UiThreadTest
    public void on5gAvailableChanged_itsAvailable_setLiveData5gEnabled() {
        mViewModel.mSpeedInfo5g.mIsEnabled = false;

        mViewModel.on5gAvailableChanged(true);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsEnabled).isTrue();
    }

    @Test
    @UiThreadTest
    public void on5gAvailableChanged_notAvailable_setLiveData5gDisabled() {
        mViewModel.mSpeedInfo5g.mIsEnabled = true;

        mViewModel.on5gAvailableChanged(false);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsEnabled).isFalse();
    }

    @Test
    @UiThreadTest
    public void on5gAvailableChanged_inSingleBand_setLiveDataToShowSingleBand() {
        when(mWifiHotspotRepository.isDualBand()).thenReturn(false);

        mViewModel.on5gAvailableChanged(true);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ).mIsVisible).isTrue();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsVisible).isTrue();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ_5GHZ).mIsVisible).isFalse();
    }

    @Test
    @UiThreadTest
    public void on5gAvailableChanged_inDualBandAnd5gUnavailable_setLiveDataToShowSingleBand() {
        when(mWifiHotspotRepository.isDualBand()).thenReturn(true);

        mViewModel.on5gAvailableChanged(false);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ).mIsVisible).isTrue();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsVisible).isTrue();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ_5GHZ).mIsVisible).isFalse();
    }

    @Test
    @UiThreadTest
    public void on5gAvailableChanged_inDualBandAnd5gAvailable_setLiveDataToShowDualBand() {
        when(mWifiHotspotRepository.isDualBand()).thenReturn(true);

        mViewModel.on5gAvailableChanged(true);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ).mIsVisible).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsVisible).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ_5GHZ).mIsVisible).isTrue();
    }

    @Test
    @UiThreadTest
    public void onSpeedTypeChanged_toSpeed2g_setLiveData2gChecked() {
        mViewModel.mSpeedInfo2g.mIsChecked = false;

        mViewModel.onSpeedTypeChanged(SPEED_2GHZ);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ).mIsChecked).isTrue();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ_5GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_6GHZ).mIsChecked).isFalse();
    }

    @Test
    @UiThreadTest
    public void onSpeedTypeChanged_toSpeed5g_setLiveData5gChecked() {
        mViewModel.mSpeedInfo5g.mIsChecked = false;

        mViewModel.onSpeedTypeChanged(SPEED_5GHZ);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsChecked).isTrue();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ_5GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_6GHZ).mIsChecked).isFalse();
    }

    @Test
    @UiThreadTest
    public void onSpeedTypeChanged_toSpeed2g5g_setLiveData5gChecked() {
        mViewModel.mSpeedInfo2g5g.mIsChecked = false;

        mViewModel.onSpeedTypeChanged(SPEED_2GHZ_5GHZ);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ_5GHZ).mIsChecked).isTrue();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_6GHZ).mIsChecked).isFalse();
    }

    @Test
    @UiThreadTest
    public void onSpeedTypeChanged_toSpeed6g_setLiveData5gChecked() {
        mViewModel.mSpeedInfo6g.mIsChecked = false;

        mViewModel.onSpeedTypeChanged(SPEED_6GHZ);

        verify(mSpeedInfoMapData).setValue(mViewModel.mSpeedInfoMap);
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_5GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_2GHZ_5GHZ).mIsChecked).isFalse();
        assertThat(mViewModel.mSpeedInfoMap.get(SPEED_6GHZ).mIsChecked).isTrue();
    }

    @Test
    @UiThreadTest
    public void setSpeedType_passedToRepository() {
        mViewModel.setSpeedType(SPEED_2GHZ);

        verify(mWifiHotspotRepository).setSpeedType(SPEED_2GHZ);

        mViewModel.setSpeedType(SPEED_5GHZ);

        verify(mWifiHotspotRepository).setSpeedType(SPEED_5GHZ);

        mViewModel.setSpeedType(SPEED_2GHZ_5GHZ);

        verify(mWifiHotspotRepository).setSpeedType(SPEED_2GHZ_5GHZ);

        mViewModel.setSpeedType(SPEED_6GHZ);

        verify(mWifiHotspotRepository).setSpeedType(SPEED_6GHZ);
    }

    @Test
    @UiThreadTest
    public void getSpeedInfoMapData_shouldNotReturnNull() {
        // Reset mSpeedInfoMapData to trigger an update
        mViewModel.mSpeedInfoMapData = null;

        assertThat(mViewModel.getSpeedInfoMapData()).isNotNull();
    }

    @Test
    public void getRestarting_shouldNotReturnNull() {
        assertThat(mViewModel.getRestarting()).isNotNull();
    }
}
