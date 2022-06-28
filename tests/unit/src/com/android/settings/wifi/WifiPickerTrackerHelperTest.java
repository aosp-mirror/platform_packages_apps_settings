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

package com.android.settings.wifi;

import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_MAX;
import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_MIN;
import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_UNREACHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.HandlerThread;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.wifitrackerlib.MergedCarrierEntry;
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

@RunWith(AndroidJUnit4.class)
public class WifiPickerTrackerHelperTest {

    private static final int SUB_ID = 2;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    public WifiManager mWifiManager;
    @Mock
    public CarrierConfigCache mCarrierConfigCache;
    @Mock
    public WifiPickerTracker mWifiPickerTracker;
    @Mock
    public MergedCarrierEntry mMergedCarrierEntry;
    @Mock
    public WifiEntry.ConnectCallback mConnectCallback;

    private WifiPickerTrackerHelper mWifiPickerTrackerHelper;

    private FakeFeatureFactory mFeatureFactory;
    private PersistableBundle mCarrierConfig;

    @Before
    public void setUp() {
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigCache.getConfigForSubId(SUB_ID)).thenReturn(mCarrierConfig);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.wifiTrackerLibProvider
                .createWifiPickerTracker(
                        any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(mWifiPickerTracker);
        mWifiPickerTrackerHelper = new WifiPickerTrackerHelper(mock(Lifecycle.class),
                mContext, null);
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(mMergedCarrierEntry);
        mWifiPickerTrackerHelper.setWifiPickerTracker(mWifiPickerTracker);
    }

    @Test
    public void getWifiPickerTracker_returnNonNull() {
        assertThat(mWifiPickerTrackerHelper.getWifiPickerTracker()).isNotNull();
    }

    @Test
    public void onDestroy_workerThreadQuit() {
        final HandlerThread workerThread = mock(HandlerThread.class);
        mWifiPickerTrackerHelper.setWorkerThread(workerThread);

        mWifiPickerTrackerHelper.onDestroy();

        verify(workerThread).quit();
    }

    @Test
    public void isCarrierNetworkProvisionEnabled_getNullConfig_returnFalse() {
        when(mCarrierConfigCache.getConfigForSubId(SUB_ID)).thenReturn(null);

        assertThat(mWifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(SUB_ID)).isFalse();
    }

    @Test
    public void isCarrierNetworkProvisionEnabled_returnCorrect() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_CARRIER_PROVISIONS_WIFI_MERGED_NETWORKS_BOOL, true);

        assertThat(mWifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(SUB_ID)).isTrue();

        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_CARRIER_PROVISIONS_WIFI_MERGED_NETWORKS_BOOL, false);

        assertThat(mWifiPickerTrackerHelper.isCarrierNetworkProvisionEnabled(SUB_ID)).isFalse();
    }

    @Test
    public void isCarrierNetworkEnabled_returnCorrect() {
        when(mMergedCarrierEntry.isEnabled()).thenReturn(true);

        assertThat(mWifiPickerTrackerHelper.isCarrierNetworkEnabled()).isTrue();

        when(mMergedCarrierEntry.isEnabled()).thenReturn(false);

        assertThat(mWifiPickerTrackerHelper.isCarrierNetworkEnabled()).isFalse();
    }

    @Test
    public void setCarrierNetworkEnabled_shouldSetCorrect() {
        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(true);

        verify(mMergedCarrierEntry).setEnabled(true);

        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(false);

        verify(mMergedCarrierEntry).setEnabled(false);
    }

    @Test
    public void setCarrierNetworkEnabled_mergedCarrierEntryIsNull_shouldNotSet() {
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(null);

        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(true);

        verify(mMergedCarrierEntry, never()).setEnabled(true);

        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(false);

        verify(mMergedCarrierEntry, never()).setEnabled(true);
    }

    @Test
    public void connectCarrierNetwork_returnTrueAndConnect() {
        when(mMergedCarrierEntry.canConnect()).thenReturn(true);

        assertThat(mWifiPickerTrackerHelper.connectCarrierNetwork(mConnectCallback)).isTrue();
        verify(mMergedCarrierEntry).connect(mConnectCallback);
    }

    @Test
    public void connectCarrierNetwork_mergedCarrierEntryIsNull_returnFalse() {
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(null);

        assertThat(mWifiPickerTrackerHelper.connectCarrierNetwork(mConnectCallback)).isFalse();
    }

    @Test
    public void connectCarrierNetwork_canConnectIsFalse_returnFalseAndNeverConnect() {
        when(mMergedCarrierEntry.canConnect()).thenReturn(false);

        assertThat(mWifiPickerTrackerHelper.connectCarrierNetwork(mConnectCallback)).isFalse();
        verify(mMergedCarrierEntry, never()).connect(mConnectCallback);
    }

    @Test
    public void getCarrierNetworkLevel_mergedCarrierEntryIsNull_returnMinLevel() {
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(null);

        assertThat(mWifiPickerTrackerHelper.getCarrierNetworkLevel()).isEqualTo(WIFI_LEVEL_MIN);
    }

    @Test
    public void getCarrierNetworkLevel_getUnreachableLevel_returnMinLevel() {
        when(mMergedCarrierEntry.getLevel()).thenReturn(WIFI_LEVEL_UNREACHABLE);

        assertThat(mWifiPickerTrackerHelper.getCarrierNetworkLevel()).isEqualTo(WIFI_LEVEL_MIN);
    }

    @Test
    public void getCarrierNetworkLevel_getAvailableLevel_returnSameLevel() {
        for (int level = WIFI_LEVEL_MIN; level <= WIFI_LEVEL_MAX; level++) {
            when(mMergedCarrierEntry.getLevel()).thenReturn(level);

            assertThat(mWifiPickerTrackerHelper.getCarrierNetworkLevel()).isEqualTo(level);
        }
    }
}
