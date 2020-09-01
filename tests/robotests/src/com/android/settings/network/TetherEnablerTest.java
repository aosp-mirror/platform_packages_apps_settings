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

package com.android.settings.network;

import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.ConnectivityManager.TETHERING_WIFI;
import static android.net.TetheringManager.TETHERING_ETHERNET;

import static com.android.settings.network.TetherEnabler.TETHERING_BLUETOOTH_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_ETHERNET_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_OFF;
import static com.android.settings.network.TetherEnabler.TETHERING_USB_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_WIFI_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.network.TetherEnabler.OnTetherStateUpdateListener;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.widget.SwitchWidgetController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class TetherEnablerTest {
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private TetheringManager mTetheringManager;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private BluetoothPan mBluetoothPan;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;

    private SwitchBar mSwitchBar;
    private TetherEnabler mEnabler;
    private SwitchWidgetController mSwitchWidgetController;
    private static final String[] USB_TETHERED = {"usb"};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = spy(ApplicationProvider.getApplicationContext());
        AtomicReference<BluetoothPan> panReference = spy(AtomicReference.class);
        mSwitchBar = spy(new SwitchBar(context));
        mSwitchWidgetController = spy(new SwitchBarController(mSwitchBar));
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
                mConnectivityManager);
        when(context.getSystemService(Context.TETHERING_SERVICE)).thenReturn(mTetheringManager);
        when(context.getSystemService(Context.NETWORK_POLICY_SERVICE)).thenReturn(
                mNetworkPolicyManager);
        when(mConnectivityManager.getTetherableIfaces()).thenReturn(new String[0]);
        when(mConnectivityManager.getTetheredIfaces()).thenReturn(new String[0]);
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(new String[0]);
        panReference.set(mBluetoothPan);
        mEnabler = spy(new TetherEnabler(context, mSwitchWidgetController, panReference));
        ReflectionHelpers.setField(mEnabler, "mBluetoothAdapter", mBluetoothAdapter);
    }

    @Test
    public void lifecycle_onStart_setCheckedCorrectly() {
        when(mConnectivityManager.getTetheredIfaces()).thenReturn(USB_TETHERED);
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(USB_TETHERED);

        mEnabler.onStart();
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    public void lifecycle_onStart_shoudRegisterTetheringEventCallback() {
        mEnabler.onStart();
        verify(mTetheringManager).registerTetheringEventCallback(any(),
                eq(mEnabler.mTetheringEventCallback));
    }

    @Test
    public void lifecycle_onStop_shouldUnregisterTetheringEventCallback() {
        mEnabler.onStart();
        TetheringManager.TetheringEventCallback callback = mEnabler.mTetheringEventCallback;

        mEnabler.onStop();
        verify(mTetheringManager).unregisterTetheringEventCallback(callback);
        assertThat(mEnabler.mTetheringEventCallback).isNull();
    }

    @Test
    public void lifecycle_onStop_resetBluetoothTetheringStoppedByUser() {
        mEnabler.onStart();
        mEnabler.mBluetoothTetheringStoppedByUser = true;

        mEnabler.onStop();
        assertThat(mEnabler.mBluetoothTetheringStoppedByUser).isFalse();
    }

    @Test
    public void startTether_fail_resetSwitchBar() {
        when(mNetworkPolicyManager.getRestrictBackground()).thenReturn(false);
        mEnabler.onStart();
        mEnabler.startTethering(TetheringManager.TETHERING_WIFI);

        when(mConnectivityManager.getTetheredIfaces()).thenReturn(new String[0]);
        mEnabler.mOnStartTetheringCallback.onTetheringFailed();

        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onDataSaverChanged_setsEnabledCorrectly() {
        mSwitchBar.setEnabled(true);

        // try to turn data saver on
        when(mNetworkPolicyManager.getRestrictBackground()).thenReturn(true);
        mEnabler.onDataSaverChanged(true);
        assertThat(mSwitchBar.isEnabled()).isFalse();

        // lets turn data saver off again
        when(mNetworkPolicyManager.getRestrictBackground()).thenReturn(false);
        mEnabler.onDataSaverChanged(false);
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onSwitchToggled_onlyStartsWifiTetherWhenNeeded() {
        doReturn(TETHERING_WIFI_ON).when(mEnabler).getTetheringState(null /* tethered */);
        mEnabler.onSwitchToggled(true);
        verify(mConnectivityManager, never()).startTethering(anyInt(), anyBoolean(), any(), any());

        doReturn(TETHERING_OFF).when(mEnabler).getTetheringState(null /* tethered */);
        mEnabler.onSwitchToggled(true);
        verify(mConnectivityManager).startTethering(eq(TETHERING_WIFI), anyBoolean(), any(), any());
        verify(mConnectivityManager, never()).startTethering(eq(TETHERING_USB), anyBoolean(), any(),
                any());
        verify(mConnectivityManager, never()).startTethering(eq(TETHERING_BLUETOOTH), anyBoolean(),
                any(), any());
        verify(mConnectivityManager, never()).startTethering(eq(TETHERING_ETHERNET), anyBoolean(),
                any(), any());
    }

    @Test
    public void onSwitchToggled_stopAllTetheringInterfaces() {
        mEnabler.onStart();

        doReturn(TETHERING_WIFI_ON).when(mEnabler).getTetheringState(null /* tethered */);
        mEnabler.onSwitchToggled(false);
        verify(mConnectivityManager).stopTethering(TETHERING_WIFI);

        doReturn(TETHERING_USB_ON).when(mEnabler).getTetheringState(null /* tethered */);
        mEnabler.onSwitchToggled(false);
        verify(mConnectivityManager).stopTethering(TETHERING_USB);

        doReturn(TETHERING_BLUETOOTH_ON).when(mEnabler).getTetheringState(null /* tethered */);
        mEnabler.onSwitchToggled(false);
        verify(mConnectivityManager).stopTethering(TETHERING_BLUETOOTH);

        doReturn(TETHERING_ETHERNET_ON).when(mEnabler).getTetheringState(null /* tethered */);
        mEnabler.onSwitchToggled(false);
        verify(mConnectivityManager).stopTethering(TETHERING_ETHERNET);
    }

    @Test
    public void startTethering_startsBluetoothTetherWhenOff() {
        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_OFF);

        mEnabler.startTethering(ConnectivityManager.TETHERING_BLUETOOTH);
        verify(mBluetoothAdapter).enable();

        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_ON);
        mEnabler.startTethering(ConnectivityManager.TETHERING_BLUETOOTH);
        verify(mConnectivityManager).startTethering(
                eq(ConnectivityManager.TETHERING_BLUETOOTH), anyBoolean(), any(), any());
    }

    @Test
    public void stopTethering_setBluetoothTetheringStoppedByUserAndUpdateState() {
        mSwitchWidgetController.setListener(mEnabler);
        mSwitchWidgetController.startListening();
        doReturn(TETHERING_BLUETOOTH_ON).when(mEnabler).getTetheringState(null /* tethered */);

        mEnabler.stopTethering(TetheringManager.TETHERING_BLUETOOTH);
        assertThat(mEnabler.mBluetoothTetheringStoppedByUser).isTrue();
        verify(mEnabler).updateState(null);
    }

    @Test
    public void updateState_onSwitchToggleNeverCalled() {
        mSwitchWidgetController.setListener(mEnabler);
        mSwitchWidgetController.startListening();

        mEnabler.updateState(null /* tethered */);
        verify(mEnabler, never()).onSwitchToggled(anyBoolean());
    }

    @Test
    public void updateState_shouldEnableSwitchBarNotTethering() {
        mSwitchWidgetController.setListener(mEnabler);
        mSwitchWidgetController.startListening();

        ReflectionHelpers.setField(mEnabler, "mDataSaverEnabled", false);
        mEnabler.updateState(null/*tethered*/);
        verify(mSwitchBar).setEnabled(true);
    }

    @Test
    public void updateState_shouldEnableSwitchBarTethering() {
        when(mConnectivityManager.getTetheredIfaces()).thenReturn(USB_TETHERED);
        when(mConnectivityManager.getTetherableUsbRegexs()).thenReturn(USB_TETHERED);

        mSwitchWidgetController.setListener(mEnabler);
        mSwitchWidgetController.startListening();

        ReflectionHelpers.setField(mEnabler, "mDataSaverEnabled", false);
        mEnabler.updateState(null/*tethered*/);
        verify(mSwitchBar).setEnabled(true);
    }

    @Test
    public void updateState_shouldCallListener() {
        OnTetherStateUpdateListener listener = mock(
                OnTetherStateUpdateListener.class);
        List<OnTetherStateUpdateListener> listeners = new ArrayList<>();
        listeners.add(listener);
        ReflectionHelpers.setField(mEnabler, "mListeners", listeners);
        mSwitchWidgetController.setListener(mEnabler);
        mSwitchWidgetController.startListening();

        mEnabler.updateState(null /* tethered */);
        verify(listener).onTetherStateUpdated(anyInt());
    }

    @Test
    public void addListener_listenerShouldAdded() {
        OnTetherStateUpdateListener listener = mock(
                OnTetherStateUpdateListener.class);
        mEnabler.addListener(listener);
        assertThat(mEnabler.mListeners).contains(listener);
    }

    @Test
    public void remListener_listenerShouldBeRemoved() {
        OnTetherStateUpdateListener listener = mock(
                OnTetherStateUpdateListener.class);
        mEnabler.removeListener(listener);
        assertThat(mEnabler.mListeners).doesNotContain(listener);
    }

    @Test
    public void isTethering_shouldReturnCorrectly() {
        assertThat(TetherEnabler.isTethering(TETHERING_WIFI_ON, TETHERING_WIFI)).isTrue();
        assertThat(TetherEnabler.isTethering(~TETHERING_WIFI_ON, TETHERING_WIFI)).isFalse();

        assertThat(TetherEnabler.isTethering(TETHERING_USB_ON, TETHERING_USB)).isTrue();
        assertThat(TetherEnabler.isTethering(~TETHERING_USB_ON, TETHERING_USB)).isFalse();

        assertThat(TetherEnabler.isTethering(TETHERING_BLUETOOTH_ON, TETHERING_BLUETOOTH)).isTrue();
        assertThat(TetherEnabler.isTethering(~TETHERING_BLUETOOTH_ON, TETHERING_BLUETOOTH))
                .isFalse();

        assertThat(TetherEnabler.isTethering(TETHERING_ETHERNET_ON, TETHERING_ETHERNET)).isTrue();
        assertThat(TetherEnabler.isTethering(~TETHERING_ETHERNET_ON, TETHERING_ETHERNET)).isFalse();
    }
}