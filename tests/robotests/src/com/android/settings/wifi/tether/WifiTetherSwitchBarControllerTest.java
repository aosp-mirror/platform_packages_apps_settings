/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_FAILED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.widget.Switch;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.SettingsMainSwitchBar;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherSwitchBarControllerTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private DataSaverBackend mDataSaverBackend;
    @Mock
    private Switch mSwitch;

    private SettingsMainSwitchBar mSwitchBar;
    private WifiTetherSwitchBarController mController;

    @Before
    public void setUp() {
        mSwitchBar = new SettingsMainSwitchBar(mContext);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
                mConnectivityManager);
        when(mDataSaverBackend.isDataSaverEnabled()).thenReturn(false);
        when(mSwitch.isEnabled()).thenReturn(true);

        mController = new WifiTetherSwitchBarController(mContext, mSwitchBar);
        mController.mDataSaverBackend = mDataSaverBackend;
    }

    @Test
    public void startTether_wifiApIsActivated_doNothing() {
        when(mWifiManager.getWifiApState()).thenReturn(WIFI_AP_STATE_ENABLED);

        mController.startTether();

        verify(mConnectivityManager, never()).startTethering(anyInt(), anyBoolean(), any(), any());
    }

    @Test
    public void startTether_wifiApNotActivated_startTethering() {
        when(mWifiManager.getWifiApState()).thenReturn(WIFI_AP_STATE_DISABLED);

        mController.startTether();

        verify(mConnectivityManager).startTethering(anyInt(), anyBoolean(), any(), any());
    }

    @Test
    public void stopTether_wifiApIsActivated_stopTethering() {
        when(mWifiManager.getWifiApState()).thenReturn(WIFI_AP_STATE_ENABLED);

        mController.stopTether();

        verify(mConnectivityManager).stopTethering(anyInt());
    }

    @Test
    public void stopTether_wifiApNotActivated_doNothing() {
        when(mWifiManager.getWifiApState()).thenReturn(WIFI_AP_STATE_DISABLED);

        mController.stopTether();

        verify(mConnectivityManager, never()).stopTethering(anyInt());
    }

    @Test
    public void startTether_fail_resetSwitchBar() {
        when(mWifiManager.getWifiApState()).thenReturn(WIFI_AP_STATE_DISABLED);
        when(mDataSaverBackend.isDataSaverEnabled()).thenReturn(false);

        mController.startTether();
        mController.mOnStartTetheringCallback.onTetheringFailed();

        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onDataSaverChanged_setsEnabledCorrectly() {
        assertThat(mSwitchBar.isEnabled()).isTrue();

        // try to turn data saver on
        when(mDataSaverBackend.isDataSaverEnabled()).thenReturn(true);
        mController.onDataSaverChanged(true);
        assertThat(mSwitchBar.isEnabled()).isFalse();

        // lets turn data saver off again
        when(mDataSaverBackend.isDataSaverEnabled()).thenReturn(false);
        mController.onDataSaverChanged(false);
        assertThat(mSwitchBar.isEnabled()).isTrue();
    }

    @Test
    public void onSwitchChanged_switchNotEnabled_doNothingForTethering() {
        when(mSwitch.isEnabled()).thenReturn(false);

        mController.onCheckedChanged(mSwitch, true);

        verify(mConnectivityManager, never()).startTethering(anyInt(), anyBoolean(), any(), any());
        verify(mConnectivityManager, never()).stopTethering(anyInt());
    }

    @Test
    public void onSwitchChanged_isChecked_startTethering() {
        mController.onCheckedChanged(mSwitch, true);

        verify(mConnectivityManager).startTethering(anyInt(), anyBoolean(), any(), any());
    }

    @Test
    public void onSwitchChanged_isNotChecked_stopTethering() {
        when(mWifiManager.getWifiApState()).thenReturn(WIFI_AP_STATE_ENABLED);
        mController.onCheckedChanged(mSwitch, false);

        verify(mConnectivityManager).stopTethering(anyInt());
    }

    @Test
    public void handleWifiApStateChanged_stateIsEnabling_notEnabledSwitchBar() {
        mSwitchBar.setEnabled(false);

        mController.handleWifiApStateChanged(WIFI_AP_STATE_ENABLING);

        assertThat(mSwitchBar.isEnabled()).isFalse();
    }

    @Test
    public void handleWifiApStateChanged_stateIsDisabling_notEnabledSwitchBar() {
        mSwitchBar.setEnabled(false);

        mController.handleWifiApStateChanged(WIFI_AP_STATE_DISABLING);

        assertThat(mSwitchBar.isEnabled()).isFalse();
    }

    @Test
    public void handleWifiApStateChanged_stateIsEnabled_enabledAndCheckedSwitchBar() {
        mSwitchBar.setEnabled(false);
        mSwitchBar.setChecked(false);

        mController.handleWifiApStateChanged(WIFI_AP_STATE_ENABLED);

        assertThat(mSwitchBar.isEnabled()).isTrue();
        assertThat(mSwitchBar.isChecked()).isTrue();
    }

    @Test
    public void handleWifiApStateChanged_stateIsDisabled_enabledAndUncheckedSwitchBar() {
        mSwitchBar.setEnabled(false);
        mSwitchBar.setChecked(true);

        mController.handleWifiApStateChanged(WIFI_AP_STATE_DISABLED);

        assertThat(mSwitchBar.isEnabled()).isTrue();
        assertThat(mSwitchBar.isChecked()).isFalse();
    }

    @Test
    public void handleWifiApStateChanged_stateIsFailed_enabledAndUncheckedSwitchBar() {
        mSwitchBar.setEnabled(false);
        mSwitchBar.setChecked(true);

        mController.handleWifiApStateChanged(WIFI_AP_STATE_FAILED);

        assertThat(mSwitchBar.isEnabled()).isTrue();
        assertThat(mSwitchBar.isChecked()).isFalse();
    }
}
