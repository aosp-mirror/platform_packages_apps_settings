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

import static com.android.settings.wifi.tether.WifiTetherViewModel.RES_INSTANT_HOTSPOT_SUMMARY_OFF;
import static com.android.settings.wifi.tether.WifiTetherViewModel.RES_INSTANT_HOTSPOT_SUMMARY_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.sharedconnectivity.app.SharedConnectivitySettingsState;

import androidx.lifecycle.MutableLiveData;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.wifi.repository.SharedConnectivityRepository;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class WifiTetherViewModelTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Application mApplication = ApplicationProvider.getApplicationContext();
    @Mock
    Executor mExecutor;
    @Mock
    WifiHotspotRepository mWifiHotspotRepository;
    @Mock
    MutableLiveData<Integer> mSecurityType;
    @Mock
    MutableLiveData<Integer> mSpeedType;
    @Mock
    private MutableLiveData<Boolean> mRestarting;
    @Mock
    private SharedConnectivityRepository mSharedConnectivityRepository;
    @Mock
    private MutableLiveData<SharedConnectivitySettingsState> mSettingsState;
    @Mock
    private MutableLiveData<String> mInstantHotspotSummary;

    WifiTetherViewModel mViewModel;

    @Before
    public void setUp() {
        when(mApplication.getMainExecutor()).thenReturn(mExecutor);

        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.getWifiFeatureProvider().getWifiHotspotRepository())
                .thenReturn(mWifiHotspotRepository);
        when(mWifiHotspotRepository.getSecurityType()).thenReturn(mSecurityType);
        when(mWifiHotspotRepository.getSpeedType()).thenReturn(mSpeedType);
        when(mWifiHotspotRepository.getRestarting()).thenReturn(mRestarting);
        when(featureFactory.getWifiFeatureProvider().getSharedConnectivityRepository())
                .thenReturn(mSharedConnectivityRepository);
        when(mSharedConnectivityRepository.isServiceAvailable()).thenReturn(true);
        when(mSharedConnectivityRepository.getSettingsState()).thenReturn(mSettingsState);

        mViewModel = new WifiTetherViewModel(mApplication);
        mViewModel.mInstantHotspotSummary = mInstantHotspotSummary;
    }

    @Test
    public void constructor_observeData() {
        verify(mSettingsState).observeForever(mViewModel.mInstantHotspotStateObserver);
    }

    @Test
    public void onCleared_removeObservers() {
        mViewModel.getSecuritySummary();
        mViewModel.getSpeedSummary();

        mViewModel.onCleared();

        verify(mSecurityType).removeObserver(mViewModel.mSecurityTypeObserver);
        verify(mSpeedType).removeObserver(mViewModel.mSpeedTypeObserver);
        verify(mSettingsState).removeObserver(mViewModel.mInstantHotspotStateObserver);
    }

    @Test
    public void getSoftApConfiguration_getConfigFromRepository() {
        mViewModel.getSoftApConfiguration();

        verify(mWifiHotspotRepository).getSoftApConfiguration();
    }

    @Test
    public void setSoftApConfiguration_setConfigByRepository() {
        SoftApConfiguration config = new SoftApConfiguration.Builder().build();

        mViewModel.setSoftApConfiguration(config);

        verify(mWifiHotspotRepository).setSoftApConfiguration(config);
    }

    @Test
    public void refresh_refreshByRepository() {
        mViewModel.refresh();

        verify(mWifiHotspotRepository).refresh();
    }

    @Test
    @UiThreadTest
    public void getSecuritySummary_returnNotNull() {
        mViewModel.mSecuritySummary = null;

        mViewModel.getSecuritySummary();

        assertThat(mViewModel.mSecuritySummary).isNotNull();
        verify(mSecurityType).observeForever(mViewModel.mSecurityTypeObserver);
    }

    @Test
    @UiThreadTest
    public void getSpeedSummary_returnNotNull() {
        mViewModel.mSpeedSummary = null;

        mViewModel.getSpeedSummary();

        assertThat(mViewModel.mSpeedSummary).isNotNull();
        verify(mSpeedType).observeForever(mViewModel.mSpeedTypeObserver);
    }

    @Test
    public void isSpeedFeatureAvailable_verifyRepositoryIsCalled() {
        mViewModel.isSpeedFeatureAvailable();

        verify(mWifiHotspotRepository).isSpeedFeatureAvailable();
    }

    @Test
    public void getRestarting_shouldNotReturnNull() {
        assertThat(mViewModel.getRestarting()).isNotNull();
    }

    @Test
    public void isInstantHotspotFeatureAvailable_serviceAvailable_returnTrue() {
        when(mSharedConnectivityRepository.isServiceAvailable()).thenReturn(true);

        assertThat(mViewModel.isInstantHotspotFeatureAvailable()).isTrue();
    }

    @Test
    public void isInstantHotspotFeatureAvailable_serviceNotAvailable_returnFalse() {
        when(mSharedConnectivityRepository.isServiceAvailable()).thenReturn(false);

        assertThat(mViewModel.isInstantHotspotFeatureAvailable()).isFalse();
    }

    @Test
    public void getInstantHotspotSummary_isNotNull() {
        assertThat(mViewModel.getInstantHotspotSummary()).isNotNull();
    }

    @Test
    public void onInstantHotspotStateChanged_stageNull_summarySetValueNull() {
        mViewModel.onInstantHotspotStateChanged(null);

        verify(mInstantHotspotSummary).setValue(null);
    }

    @Test
    public void onInstantHotspotStateChanged_stateEnabled_summarySetValueOn() {
        SharedConnectivitySettingsState state = new SharedConnectivitySettingsState.Builder()
                .setInstantTetherEnabled(true).build();

        mViewModel.onInstantHotspotStateChanged(state);

        verify(mInstantHotspotSummary)
                .setValue(mApplication.getString(RES_INSTANT_HOTSPOT_SUMMARY_ON));
    }

    @Test
    public void onInstantHotspotStateChanged_stateNotEnabled_recordVisibleSummaryOff() {
        SharedConnectivitySettingsState state = new SharedConnectivitySettingsState.Builder()
                .setInstantTetherEnabled(false).build();

        mViewModel.onInstantHotspotStateChanged(state);

        verify(mInstantHotspotSummary)
                .setValue(mApplication.getString(RES_INSTANT_HOTSPOT_SUMMARY_OFF));
    }

    @Test
    public void launchInstantHotspotSettings_launchSettingsByRepository() {
        mViewModel.launchInstantHotspotSettings();

        verify(mSharedConnectivityRepository).launchSettings();
    }
}
