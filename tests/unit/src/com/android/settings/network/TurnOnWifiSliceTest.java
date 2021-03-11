/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class TurnOnWifiSliceTest {

    @Rule
    public MockitoRule mMocks = MockitoJUnit.rule();
    @Mock
    private WifiManager mWifiManager;

    private Context mContext;
    private TurnOnWifiSlice mSlice;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mSlice = new TurnOnWifiSlice(mContext);
    }

    @Test
    public void getSlice_wifiEnabled_shouldBeNull() {
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        assertThat(mSlice.getSlice()).isNull();
    }

    @Test
    public void getSlice_wifiDisabled_shouldBeNotNull() {
        when(mWifiManager.isWifiEnabled()).thenReturn(false);

        assertThat(mSlice.getSlice()).isNotNull();
    }

    @Test
    public void onNotifyChange_shouldSetWifiEnabled() {
        Intent intent = mSlice.getIntent();

        mSlice.onNotifyChange(intent);

        verify(mWifiManager).setWifiEnabled(true);
    }
}
