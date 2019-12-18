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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.wifi.WifiManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class TetherEnablerTest {
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private BluetoothPan mBluetoothPan;
    @Mock
    private SharedPreferences mSharedPreferences;

    private SwitchBar mSwitchBar;
    private TetherEnabler mEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = spy(ApplicationProvider.getApplicationContext());
        AtomicReference<BluetoothPan> panReference = spy(AtomicReference.class);
        mSwitchBar = new SwitchBar(context);
        when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
                mConnectivityManager);
        when(context.getSystemService(Context.NETWORK_POLICY_SERVICE)).thenReturn(
                mNetworkPolicyManager);
        when(mConnectivityManager.getTetherableIfaces()).thenReturn(new String[0]);
        panReference.set(mBluetoothPan);
        mEnabler = new TetherEnabler(context, new SwitchBarController(mSwitchBar), panReference);
    }

    @Test
    public void lifecycle_onStart_setCheckedCorrectly() {
        when(mConnectivityManager.getTetheredIfaces()).thenReturn(new String[]{""});

        mEnabler.onStart();
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    public void startTether_fail_resetSwitchBar() {
        when(mNetworkPolicyManager.getRestrictBackground()).thenReturn(false);

        mEnabler.startTether();
        mEnabler.mOnStartTetheringCallback.onTetheringFailed();

        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onDataSaverChanged_setsEnabledCorrectly() {
        assertThat(mSwitchBar.isEnabled()).isTrue();

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
        when(mWifiManager.isWifiApEnabled()).thenReturn(true);
        mEnabler.onSwitchToggled(true);

        verify(mConnectivityManager, never()).startTethering(anyInt(), anyBoolean(), any(), any());

        doReturn(false).when(mWifiManager).isWifiApEnabled();
        mEnabler.onSwitchToggled(true);

        verify(mConnectivityManager, times(1))
                .startTethering(anyInt(), anyBoolean(), any(), any());
    }

    @Test
    public void onSwitchToggled_shouldStartUSBTetherWhenSelected() {
        SharedPreferences preference = mock(SharedPreferences.class);
        ReflectionHelpers.setField(mEnabler, "mSharedPreferences", preference);
        when(preference.getBoolean(mEnabler.WIFI_TETHER_KEY, true)).thenReturn(false);
        when(preference.getBoolean(mEnabler.USB_TETHER_KEY, false)).thenReturn(true);
        when(preference.getBoolean(mEnabler.BLUETOOTH_TETHER_KEY, true)).thenReturn(false);

        mEnabler.startTether();
        verify(mConnectivityManager, times(1))
                .startTethering(eq(ConnectivityManager.TETHERING_USB), anyBoolean(), any(), any());
        verify(mConnectivityManager, never())
                .startTethering(eq(ConnectivityManager.TETHERING_WIFI), anyBoolean(), any(), any());
        verify(mConnectivityManager, never()).startTethering(
                eq(ConnectivityManager.TETHERING_BLUETOOTH), anyBoolean(), any(), any());
    }

    @Test
    public void startTether_startsBluetoothTetherWhenOff() {
        BluetoothAdapter adapter = mock(BluetoothAdapter.class);
        ReflectionHelpers.setField(mEnabler, "mBluetoothAdapter", adapter);
        when(adapter.getState()).thenReturn(BluetoothAdapter.STATE_OFF);

        mEnabler.startTethering(ConnectivityManager.TETHERING_BLUETOOTH);
        verify(adapter, times(1)).enable();

        when(adapter.getState()).thenReturn(BluetoothAdapter.STATE_ON);
        mEnabler.startTethering(ConnectivityManager.TETHERING_BLUETOOTH);
        verify(mConnectivityManager, times(1)).startTethering(
                eq(ConnectivityManager.TETHERING_BLUETOOTH), anyBoolean(), any(), any());
    }
}
