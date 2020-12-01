/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.AirplaneModeEnabler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class AirplaneSafeNetworksSliceTest {

    private static final String VIEW_AIRPLANE_SAFE_NETWORKS = "View airplane-safe networks";
    private static final String TURN_OFF_AIRPLANE_MODE = "Turn off Airplane Mode";

    @Rule
    public MockitoRule mMocks = MockitoJUnit.rule();
    @Mock
    private WifiManager mWifiManager;

    private Context mContext;
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private AirplaneSafeNetworksSlice mAirplaneSafeNetworksSlice;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        mAirplaneModeEnabler =
                new AirplaneModeEnabler(mContext, null /* OnAirplaneModeChangedListener */);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        mAirplaneSafeNetworksSlice = new AirplaneSafeNetworksSlice(mContext);
    }

    @Test
    public void getSlice_airplaneModeOff_shouldBeNull() {
        mAirplaneModeEnabler.setAirplaneMode(false);

        assertThat(mAirplaneSafeNetworksSlice.getSlice()).isNull();
    }

    @Test
    public void getSlice_wifiDisabled_shouldShowViewAirplaneSafeNetworks() {
        mAirplaneModeEnabler.setAirplaneMode(true);
        when(mWifiManager.isWifiEnabled()).thenReturn(false);

        final Slice slice = mAirplaneSafeNetworksSlice.getSlice();

        assertThat(slice).isNotNull();
        final SliceItem sliceTitle =
                SliceMetadata.from(mContext, slice).getListContent().getHeader().getTitleItem();
        assertThat(sliceTitle.getText()).isEqualTo(VIEW_AIRPLANE_SAFE_NETWORKS);
    }

    @Test
    public void getSlice_wifiEnabled_shouldShowTurnOffAirplaneMode() {
        mAirplaneModeEnabler.setAirplaneMode(true);
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        final Slice slice = mAirplaneSafeNetworksSlice.getSlice();

        assertThat(slice).isNotNull();
        final SliceItem sliceTitle =
                SliceMetadata.from(mContext, slice).getListContent().getHeader().getTitleItem();
        assertThat(sliceTitle.getText()).isEqualTo(TURN_OFF_AIRPLANE_MODE);
    }

    @Test
    public void onNotifyChange_viewAirplaneSafeNetworks_shouldSetWifiEnabled() {
        mAirplaneModeEnabler.setAirplaneMode(true);
        when(mWifiManager.isWifiEnabled()).thenReturn(false);
        Intent intent = mAirplaneSafeNetworksSlice.getIntent();

        mAirplaneSafeNetworksSlice.onNotifyChange(intent);

        verify(mWifiManager).setWifiEnabled(true);
    }

    @Test
    public void onNotifyChange_turnOffAirplaneMode_shouldSetAirplaneModeOff() {
        mAirplaneModeEnabler.setAirplaneMode(true);
        when(mWifiManager.isWifiEnabled()).thenReturn(true);
        Intent intent = mAirplaneSafeNetworksSlice.getIntent();

        mAirplaneSafeNetworksSlice.onNotifyChange(intent);

        assertThat(mAirplaneModeEnabler.isAirplaneModeOn()).isFalse();
    }
}
