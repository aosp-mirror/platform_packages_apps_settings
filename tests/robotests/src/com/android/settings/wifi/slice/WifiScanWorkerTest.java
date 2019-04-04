/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.wifi.slice;

import static com.android.settings.slices.CustomSliceRegistry.WIFI_SLICE_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.slice.SliceProvider;
import androidx.slice.widget.SliceLiveData;

import com.android.settingslib.wifi.AccessPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiScanWorkerTest {

    private static final String AP_NAME = "ap";

    private Context mContext;
    private ContentResolver mResolver;
    private WifiManager mWifiManager;
    private WifiScanWorker mWifiScanWorker;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mResolver = mock(ContentResolver.class);
        doReturn(mResolver).when(mContext).getContentResolver();
        mWifiManager = mContext.getSystemService(WifiManager.class);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
        mWifiManager.setWifiEnabled(true);

        mWifiScanWorker = new WifiScanWorker(mContext, WIFI_SLICE_URI);
    }

    @Test
    public void onWifiStateChanged_shouldNotifyChange() {
        mWifiScanWorker.onWifiStateChanged(WifiManager.WIFI_STATE_DISABLED);

        verify(mResolver).notifyChange(WIFI_SLICE_URI, null);
    }

    private AccessPoint createAccessPoint(String name, State state) {
        final NetworkInfo info = mock(NetworkInfo.class);
        doReturn(state).when(info).getState();

        final Bundle savedState = new Bundle();
        savedState.putString("key_ssid", name);
        savedState.putParcelable("key_networkinfo", info);
        return new AccessPoint(mContext, savedState);
    }

    @Test
    public void SliceAccessPoint_sameState_shouldBeTheSame() {
        final AccessPoint ap1 = createAccessPoint(AP_NAME, State.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(AP_NAME, State.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isTrue();
    }

    @Test
    public void SliceAccessPoint_differentState_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(AP_NAME, State.CONNECTING);
        final AccessPoint ap2 = createAccessPoint(AP_NAME, State.CONNECTED);

        assertThat(mWifiScanWorker.areListsTheSame(Arrays.asList(ap1), Arrays.asList(ap2)))
                .isFalse();
    }

    @Test
    public void SliceAccessPoint_differentLength_shouldBeDifferent() {
        final AccessPoint ap1 = createAccessPoint(AP_NAME, State.CONNECTED);
        final AccessPoint ap2 = createAccessPoint(AP_NAME, State.CONNECTED);
        final List<AccessPoint> list = new ArrayList<>();
        list.add(ap1);
        list.add(ap2);

        assertThat(mWifiScanWorker.areListsTheSame(list, Arrays.asList(ap1))).isFalse();
    }
}
