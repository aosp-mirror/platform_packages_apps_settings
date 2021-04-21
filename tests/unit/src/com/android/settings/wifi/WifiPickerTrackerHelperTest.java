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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.wifitrackerlib.MergedCarrierEntry;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiPickerTrackerHelperTest {

    private static final int SUB_ID = 2;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    public WifiManager mWifiManager;
    @Mock
    public CarrierConfigManager mCarrierConfigManager;
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
        final Context context = spy(ApplicationProvider.getApplicationContext());
        when(context.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(context.getSystemService(CarrierConfigManager.class))
                .thenReturn(mCarrierConfigManager);
        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.wifiTrackerLibProvider
                .createWifiPickerTracker(
                        any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(mWifiPickerTracker);
        mWifiPickerTrackerHelper = new WifiPickerTrackerHelper(mock(Lifecycle.class),
                context, null);
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
        doReturn(null).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

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
        doReturn(true).when(mWifiManager).isCarrierNetworkOffloadEnabled(SUB_ID, true /* merged */);

        assertThat(mWifiPickerTrackerHelper.isCarrierNetworkEnabled(SUB_ID)).isTrue();

        doReturn(false).when(mWifiManager)
                .isCarrierNetworkOffloadEnabled(SUB_ID, true /* merged */);

        assertThat(mWifiPickerTrackerHelper.isCarrierNetworkEnabled(SUB_ID)).isFalse();
    }

    @Test
    public void setCarrierNetworkEnabled_shouldSetCorrect() {
        mWifiPickerTrackerHelper.setWifiPickerTracker(mWifiPickerTracker);
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(mMergedCarrierEntry);

        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(true);

        verify(mMergedCarrierEntry).setEnabled(true);

        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(false);

        verify(mMergedCarrierEntry).setEnabled(false);
    }

    @Test
    public void setCarrierNetworkEnabled_mergedCarrierEntryIsNull_shouldNotSet() {
        mWifiPickerTrackerHelper.setWifiPickerTracker(mWifiPickerTracker);
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(null);

        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(true);

        verify(mMergedCarrierEntry, never()).setEnabled(true);

        mWifiPickerTrackerHelper.setCarrierNetworkEnabled(false);

        verify(mMergedCarrierEntry, never()).setEnabled(true);
    }

    @Test
    public void connectCarrierNetwork_returnTrueAndConnect() {
        mWifiPickerTrackerHelper.setWifiPickerTracker(mWifiPickerTracker);
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(mMergedCarrierEntry);
        when(mMergedCarrierEntry.canConnect()).thenReturn(true);

        assertThat(mWifiPickerTrackerHelper.connectCarrierNetwork(mConnectCallback)).isTrue();
        verify(mMergedCarrierEntry).connect(mConnectCallback);
    }

    @Test
    public void connectCarrierNetwork_mergedCarrierEntryIsNull_returnFalse() {
        mWifiPickerTrackerHelper.setWifiPickerTracker(mWifiPickerTracker);
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(null);

        assertThat(mWifiPickerTrackerHelper.connectCarrierNetwork(mConnectCallback)).isFalse();
    }

    @Test
    public void connectCarrierNetwork_canConnectIsFalse_returnFalseAndNeverConnect() {
        mWifiPickerTrackerHelper.setWifiPickerTracker(mWifiPickerTracker);
        when(mWifiPickerTracker.getMergedCarrierEntry()).thenReturn(mMergedCarrierEntry);
        when(mMergedCarrierEntry.canConnect()).thenReturn(false);

        assertThat(mWifiPickerTrackerHelper.connectCarrierNetwork(mConnectCallback)).isFalse();
        verify(mMergedCarrierEntry, never()).connect(mConnectCallback);
    }
}
