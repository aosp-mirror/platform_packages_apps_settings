/*
 * Copyright (C) 2017 The Android Open Source Project
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
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothSummaryHelperTest {

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private LocalBluetoothAdapter mBtAdapter;

    private BluetoothSummaryHelper mHelper;
    @Mock
    private SummaryListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mBluetoothManager.getBluetoothAdapter()).thenReturn(mBtAdapter);
        when(mBtAdapter.isEnabled()).thenReturn(true);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_CONNECTED);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mHelper = new BluetoothSummaryHelper(mContext, mBluetoothManager);
        mHelper.setOnSummaryChangeListener(mListener);
    }

    @Test
    public void setListening_shouldRegisterListener() {
        mHelper.setListening(true);

        verify(mBluetoothManager.getEventManager()).registerCallback(mHelper);
    }

    @Test
    public void setNotListening_shouldUnregisterListener() {
        mHelper.setListening(false);

        verify(mBluetoothManager.getEventManager()).unregisterCallback(mHelper);
    }

    @Test
    public void setListening_shouldSendSummaryChange() {
        mHelper.setListening(true);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_connected));
    }

    @Test
    public void onBluetoothStateChanged_btDisabled_shouldSendDisabledSummary() {
        mHelper.setListening(true);
        mHelper.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_disabled));
    }

    @Test
    public void onBluetoothStateChanged_btEnabled_connected_shouldSendConnectedSummary() {
        mHelper.setListening(true);
        mHelper.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_connected));
    }

    @Test
    public void onBluetoothStateChanged_btEnabled_notConnected_shouldSendDisconnectedMessage() {
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_DISCONNECTED);
        mHelper.setListening(true);
        mHelper.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_ON);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_disconnected));
    }

    @Test
    public void onConnectionStateChanged_connected_shouldSendConnectedMessage() {
        final List<CachedBluetoothDevice> devices = new ArrayList<>();
        devices.add(mock(CachedBluetoothDevice.class));
        when(devices.get(0).isConnected()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy())
            .thenReturn(devices);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_DISCONNECTED);
        mHelper.setListening(true);

        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_CONNECTED);
        mHelper.onConnectionStateChanged(null /* device */, BluetoothAdapter.STATE_CONNECTED);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_connected));
    }

    @Test
    public void onConnectionStateChanged_inconsistentState_shouldSendDisconnectedMessage() {
        mHelper.setListening(true);
        mHelper.onConnectionStateChanged(null /* device */, BluetoothAdapter.STATE_CONNECTED);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_disconnected));
    }

    @Test
    public void onConnectionStateChanged_connecting_shouldSendConnectingMessage() {
        mHelper.setListening(true);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_CONNECTING);
        mHelper.onConnectionStateChanged(null /* device */, BluetoothAdapter.STATE_CONNECTING);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_connecting));
    }

    @Test
    public void onConnectionStateChanged_disconnecting_shouldSendDisconnectingMessage() {
        mHelper.setListening(true);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_DISCONNECTING);
        mHelper.onConnectionStateChanged(null /* device */, BluetoothAdapter.STATE_DISCONNECTING);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_disconnecting));
    }

    private class SummaryListener implements BluetoothSummaryHelper.OnSummaryChangeListener {
        String summary;

        @Override
        public void onSummaryChanged(String summary) {
            this.summary = summary;
        }
    }

}
