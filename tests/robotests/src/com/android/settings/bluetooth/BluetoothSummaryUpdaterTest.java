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

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.widget.SummaryUpdater.OnSummaryChangeListener;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothSummaryUpdaterTest {
    private static final String DEVICE_NAME = "Nightshade";
    private static final String DEVICE_KEYBOARD_NAME = "Bluetooth Keyboard";

    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mBluetoothManager;
    @Mock
    private LocalBluetoothAdapter mBtAdapter;
    @Mock
    private BluetoothDevice mConnectedDevice;
    @Mock
    private BluetoothDevice mConnectedKeyBoardDevice;
    @Mock
    private SummaryListener mListener;

    private BluetoothSummaryUpdater mSummaryUpdater;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mBluetoothManager.getBluetoothAdapter()).thenReturn(mBtAdapter);
        when(mBtAdapter.isEnabled()).thenReturn(true);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_CONNECTED);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mSummaryUpdater = new BluetoothSummaryUpdater(mContext, mListener, mBluetoothManager);
    }

    @Test
    public void register_true_shouldRegisterListener() {
        mSummaryUpdater.register(true);

        verify(mBluetoothManager.getEventManager()).registerCallback(mSummaryUpdater);
    }

    @Test
    public void register_false_shouldUnregisterListener() {
        mSummaryUpdater.register(false);

        verify(mBluetoothManager.getEventManager()).unregisterCallback(mSummaryUpdater);
    }

    @Test
    public void register_true_shouldSendSummaryChange() {
        prepareConnectedDevice(false);

        mSummaryUpdater.register(true);

        verify(mListener).onSummaryChanged(
                mContext.getString(R.string.bluetooth_connected_summary, DEVICE_NAME));
    }

    @Test
    public void onBluetoothStateChanged_btDisabled_shouldSendDisabledSummary() {
        mSummaryUpdater.register(true);
        mSummaryUpdater.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_disabled));
    }

    @Test
    public void onBluetoothStateChanged_btEnabled_connected_shouldSendConnectedSummary() {
        prepareConnectedDevice(false);

        mSummaryUpdater.register(true);
        mSummaryUpdater.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);

        verify(mListener).onSummaryChanged(
                mContext.getString(R.string.bluetooth_connected_summary, DEVICE_NAME));
    }

    @Test
    public void onBluetoothStateChanged_btEnabled_notConnected_shouldSendDisconnectedMessage() {
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_DISCONNECTED);
        mSummaryUpdater.register(true);
        mSummaryUpdater.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_ON);

        verify(mListener).onSummaryChanged(
                mContext.getString(R.string.disconnected));
    }

    @Test
    public void onBluetoothStateChanged_ConnectedDisabledEnabled_shouldSendDisconnectedSummary() {
        final boolean[] connected = {false};
        final List<CachedBluetoothDevice> devices = new ArrayList<>();
        devices.add(mock(CachedBluetoothDevice.class));
        doAnswer(invocation -> connected[0]).when(devices.get(0)).isConnected();
        when(mBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy())
                .thenReturn(devices);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_DISCONNECTED);
        prepareConnectedDevice(false);

        mSummaryUpdater.register(true);
        verify(mListener).onSummaryChanged(mContext.getString(R.string.disconnected));

        connected[0] = true;
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_CONNECTED);
        mSummaryUpdater.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_CONNECTED);
        verify(mListener).onSummaryChanged(
                mContext.getString(R.string.bluetooth_connected_summary, DEVICE_NAME));

        mSummaryUpdater.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);
        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_disabled));

        connected[0] = false;
        mSummaryUpdater.onBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_ON);
        verify(mListener, times(2)).onSummaryChanged(mContext.getString(R.string.disconnected));
        verify(mListener, times(4)).onSummaryChanged(anyString());
    }

    @Test
    public void onConnectionStateChanged_connected_shouldSendConnectedMessage() {
        final List<CachedBluetoothDevice> devices = new ArrayList<>();
        devices.add(mock(CachedBluetoothDevice.class));
        when(devices.get(0).isConnected()).thenReturn(true);
        when(mBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy())
                .thenReturn(devices);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_DISCONNECTED);
        prepareConnectedDevice(false);

        mSummaryUpdater.register(true);

        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_CONNECTED);
        mSummaryUpdater.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_CONNECTED);

        verify(mListener).onSummaryChanged(
                mContext.getString(R.string.bluetooth_connected_summary, DEVICE_NAME));
    }

    @Test
    public void onConnectionStateChanged_inconsistentState_shouldSendDisconnectedMessage() {
        mSummaryUpdater.register(true);
        mSummaryUpdater.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_CONNECTED);

        verify(mListener).onSummaryChanged(
                mContext.getString(R.string.disconnected));
    }

    @Test
    public void onConnectionStateChanged_connecting_shouldSendConnectingMessage() {
        mSummaryUpdater.register(true);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_CONNECTING);
        mSummaryUpdater.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_CONNECTING);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_connecting));
    }

    @Test
    public void onConnectionStateChanged_disconnecting_shouldSendDisconnectingMessage() {
        mSummaryUpdater.register(true);
        when(mBtAdapter.getConnectionState()).thenReturn(BluetoothAdapter.STATE_DISCONNECTING);
        mSummaryUpdater.onConnectionStateChanged(null /* device */,
                BluetoothAdapter.STATE_DISCONNECTING);

        verify(mListener).onSummaryChanged(mContext.getString(R.string.bluetooth_disconnecting));
    }

    @Test
    public void getConnectedDeviceSummary_hasConnectedDevice_returnOneDeviceSummary() {
        prepareConnectedDevice(false);
        final String expectedSummary = mContext.getString(R.string.bluetooth_connected_summary,
                DEVICE_NAME);

        assertThat(mSummaryUpdater.getConnectedDeviceSummary()).isEqualTo(expectedSummary);
    }

    @Test
    public void getConnectedDeviceSummary_multipleDevices_returnMultipleDevicesSummary() {
        prepareConnectedDevice(true);
        final String expectedSummary = mContext.getString(
                R.string.bluetooth_connected_multiple_devices_summary);

        assertThat(mSummaryUpdater.getConnectedDeviceSummary()).isEqualTo(expectedSummary);
    }

    private void prepareConnectedDevice(boolean multipleDevices) {
        final Set<BluetoothDevice> devices = new HashSet<>();
        doReturn(DEVICE_NAME).when(mConnectedDevice).getName();
        doReturn(true).when(mConnectedDevice).isConnected();
        devices.add(mConnectedDevice);
        if (multipleDevices) {
            // Add one more device if we need to test multiple devices
            doReturn(DEVICE_KEYBOARD_NAME).when(mConnectedKeyBoardDevice).getName();
            doReturn(true).when(mConnectedKeyBoardDevice).isConnected();
            devices.add(mConnectedKeyBoardDevice);
        }

        doReturn(devices).when(mBtAdapter).getBondedDevices();
    }

    private class SummaryListener implements OnSummaryChangeListener {
        String summary;

        @Override
        public void onSummaryChanged(String summary) {
            this.summary = summary;
        }
    }

}
