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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.net.wifi.SoftApConfiguration;

import androidx.lifecycle.MutableLiveData;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class WifiTetherViewModelTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    Application mApplication;
    @Mock
    Executor mExecutor;
    @Mock
    WifiHotspotRepository mWifiHotspotRepository;
    @Mock
    MutableLiveData<Integer> mSpeedType;

    WifiTetherViewModel mViewModel;

    @Before
    public void setUp() {
        when(mApplication.getMainExecutor()).thenReturn(mExecutor);

        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.getWifiFeatureProvider().getWifiHotspotRepository())
                .thenReturn(mWifiHotspotRepository);
        when(mWifiHotspotRepository.getSpeedType()).thenReturn(mSpeedType);

        mViewModel = new WifiTetherViewModel(mApplication);
    }

    @Test
    public void constructor_setAutoRefreshTrue() {
        verify(mWifiHotspotRepository).setAutoRefresh(true);
    }

    @Test
    public void onCleared_setAutoRefreshFalse() {
        mViewModel.onCleared();

        verify(mWifiHotspotRepository).setAutoRefresh(false);
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
    public void getSpeedSummary_returnNotNull() {
        mViewModel.mSpeedSummary = null;

        mViewModel.getSpeedSummary();

        assertThat(mViewModel.mSpeedSummary).isNotNull();
    }
}
