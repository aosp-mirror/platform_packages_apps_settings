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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class WifiScanWorkerTest {

    private static final int SUB_ID = 2;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    WifiManager mWifiManager;
    @Mock
    WifiPickerTracker mWifiPickerTracker;
    @Mock
    WifiPickerTrackerHelper mWifiPickerTrackerHelper;

    private WifiScanWorker mWifiScanWorker;

    @Before
    public void setUp() {
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);

        mWifiScanWorker = new WifiScanWorker(mContext, WIFI_SLICE_URI);
        mWifiScanWorker.mWifiPickerTracker = mWifiPickerTracker;
        mWifiScanWorker.mWifiPickerTrackerHelper = mWifiPickerTrackerHelper;
        when(mWifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(SUB_ID)).thenReturn(false);
    }

    @Test
    public void onConstructor_shouldBeInCreatedState() {
        assertThat(mWifiScanWorker.getLifecycle().getCurrentState())
                .isEqualTo(Lifecycle.State.CREATED);
    }

    @Test
    public void onSlicePinned_shouldBeInResumedState() {
        mWifiScanWorker.onSlicePinned();

        assertThat(mWifiScanWorker.getLifecycle().getCurrentState())
                .isEqualTo(Lifecycle.State.RESUMED);
    }

    @Test
    public void onSliceUnpinned_shouldBeInCreatedState() {
        mWifiScanWorker.onSliceUnpinned();

        assertThat(mWifiScanWorker.getLifecycle().getCurrentState())
                .isEqualTo(Lifecycle.State.CREATED);
    }

    @Test
    public void close_shouldBeInDestroyedState() {
        mWifiScanWorker.close();

        assertThat(mWifiScanWorker.getLifecycle().getCurrentState())
                .isEqualTo(Lifecycle.State.DESTROYED);
    }

    @Test
    public void getWifiEntry_connectedWifiKey_shouldGetConnectedWifi() {
        final String key = "key";
        final WifiEntry connectedWifiEntry = mock(WifiEntry.class);
        when(connectedWifiEntry.getKey()).thenReturn(key);
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(connectedWifiEntry);

        assertThat(mWifiScanWorker.getWifiEntry(key)).isEqualTo(connectedWifiEntry);
    }

    @Test
    public void getWifiEntry_reachableWifiKey_shouldGetReachableWifi() {
        final String key = "key";
        final WifiEntry reachableWifiEntry = mock(WifiEntry.class);
        when(reachableWifiEntry.getKey()).thenReturn(key);
        when(mWifiPickerTracker.getWifiEntries()).thenReturn(Arrays.asList(reachableWifiEntry));

        assertThat(mWifiScanWorker.getWifiEntry(key)).isEqualTo(reachableWifiEntry);
    }

    @Test
    public void setCarrierNetworkEnabledIfNeeded_shouldSetCarrierNetworkEnabled() {
        mWifiScanWorker.setCarrierNetworkEnabledIfNeeded(true, SUB_ID);

        verify(mWifiPickerTrackerHelper).setCarrierNetworkEnabled(true);

        mWifiScanWorker.setCarrierNetworkEnabledIfNeeded(false, SUB_ID);

        verify(mWifiPickerTrackerHelper).setCarrierNetworkEnabled(false);
    }

    @Test
    public void setCarrierNetworkEnabledIfNeeded_enabledProvision_neverSetCarrierNetworkEnabled() {
        when(mWifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(SUB_ID)).thenReturn(true);

        mWifiScanWorker.setCarrierNetworkEnabledIfNeeded(true, SUB_ID);

        verify(mWifiPickerTrackerHelper, never()).setCarrierNetworkEnabled(true);

        mWifiScanWorker.setCarrierNetworkEnabledIfNeeded(false, SUB_ID);

        verify(mWifiPickerTrackerHelper, never()).setCarrierNetworkEnabled(false);
    }

    @Test
    public void connectCarrierNetwork_shouldConnectCarrierNetwork() {
        mWifiScanWorker.connectCarrierNetwork();

        verify(mWifiPickerTrackerHelper).connectCarrierNetwork(any());
    }
}
