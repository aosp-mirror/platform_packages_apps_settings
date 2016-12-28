/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothSettingsSummaryProviderTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private SummaryLoader mSummaryLoader;

    private BluetoothSettings.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mSummaryProvider = new BluetoothSettings.SummaryProvider(mContext, mSummaryLoader,
                mBluetoothManager);
    }

    @Test
    public void setListening_shouldUpdateSummary() {
        mSummaryProvider.setListening(true);

        verify(mBluetoothManager.getEventManager()).registerCallback(mSummaryProvider);
        verify(mSummaryLoader).setSummary(eq(mSummaryProvider), anyString());
    }

    @Test
    public void setNotListening_shouldUnregister() {
        mSummaryProvider.setListening(false);

        verify(mBluetoothManager.getEventManager()).unregisterCallback(mSummaryProvider);
    }

    @Test
    public void updateSummary_btDisabled_shouldShowDisabledMessage() {
        ShadowBluetoothAdapter.getDefaultAdapter().disable();
        mSummaryProvider.setListening(true);

        verify(mSummaryLoader).setSummary(mSummaryProvider,
                mContext.getString(R.string.bluetooth_disabled));
    }

    @Test
    public void updateSummary_btEnabled_noDevice_shouldShowDisconnectedMessage() {
        ShadowBluetoothAdapter.getDefaultAdapter().enable();
        mSummaryProvider.setListening(true);

        verify(mSummaryLoader).setSummary(mSummaryProvider,
                mContext.getString(R.string.bluetooth_disconnected));
    }

    @Test
    public void updateState_btEnabled_noDevice_shouldShowDisconnectedMessage() {
        ShadowBluetoothAdapter.getDefaultAdapter().enable();
        mSummaryProvider.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_ON);

        verify(mSummaryLoader).setSummary(mSummaryProvider,
                mContext.getString(R.string.bluetooth_disconnected));
    }

    @Test
    public void updateState_btDisabled_shouldShowDisabledMessage() {
        ShadowBluetoothAdapter.getDefaultAdapter().enable();
        mSummaryProvider.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_OFF);

        verify(mSummaryLoader).setSummary(mSummaryProvider,
                mContext.getString(R.string.bluetooth_disabled));
    }

    @Test
    public void updateConnectionState_disconnected_shouldShowDisconnectedMessage() {
        ShadowBluetoothAdapter.getDefaultAdapter().enable();
        when(mBluetoothManager.getBluetoothAdapter().getConnectionState())
                .thenReturn(BluetoothAdapter.STATE_DISCONNECTED);

        mSummaryProvider.setListening(true);
        mSummaryProvider.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_DISCONNECTED);

        verify(mSummaryLoader, times(2)).setSummary(mSummaryProvider,
                mContext.getString(R.string.bluetooth_disconnected));
    }


    @Test
    public void updateConnectionState_connected_shouldShowConnectedMessage() {
        ShadowBluetoothAdapter.getDefaultAdapter().enable();
        when(mBluetoothManager.getBluetoothAdapter().getConnectionState())
                .thenReturn(BluetoothAdapter.STATE_CONNECTED);
        final List<CachedBluetoothDevice> devices = new ArrayList<>();
        devices.add(mock(CachedBluetoothDevice.class));
        when(devices.get(0).isConnected()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy())
                .thenReturn(devices);

        mSummaryProvider.setListening(true);
        mSummaryProvider.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_CONNECTED);

        verify(mSummaryLoader).setSummary(mSummaryProvider,
                mContext.getString(R.string.bluetooth_connected));
    }

    @Test
    public void updateConnectionState_inconsistentState_shouldShowDisconnectedMessage() {
        ShadowBluetoothAdapter.getDefaultAdapter().enable();
        when(mBluetoothManager.getBluetoothAdapter().getConnectionState())
                .thenReturn(BluetoothAdapter.STATE_CONNECTED);

        mSummaryProvider.setListening(true);
        mSummaryProvider.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_CONNECTED);

        verify(mSummaryLoader, times(2)).setSummary(mSummaryProvider,
                mContext.getString(R.string.bluetooth_disconnected));
    }

}
