/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development.qstile;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.Global;
import android.widget.Toast;

import com.android.settings.testutils.shadow.ShadowWirelessDebuggingPreferenceController;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowWirelessDebuggingPreferenceController.class})
public class WirelessDebuggingTest {
    @Mock
    private Toast mToast;
    @Mock
    private KeyguardManager mKeyguardManager;

    private Context mContext;
    private DevelopmentTiles.WirelessDebugging mWirelessDebugging;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mWirelessDebugging = spy(new DevelopmentTiles.WirelessDebugging());
        doReturn(mContext.getContentResolver()).when(mWirelessDebugging).getContentResolver();
        ReflectionHelpers.setField(mWirelessDebugging, "mKeyguardManager", mKeyguardManager);
        ReflectionHelpers.setField(mWirelessDebugging, "mToast", mToast);
    }

    @After
    public void tearDown() {
        ShadowWirelessDebuggingPreferenceController.reset();
    }

    @Test
    public void adbWifiEnabled_shouldReturnEnabled() {
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);

        assertThat(mWirelessDebugging.isEnabled()).isTrue();
    }

    @Test
    public void adbWifiDisabled_shouldReturnDisabled() {
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting disabled */);

        assertThat(mWirelessDebugging.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnabled_false_keyguardUnlocked_WifiDisconnected_shouldDisableAdbWifi() {
        // Precondition: set the tile to enabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);
        // Unlocked keyguard
        doReturn(false).when(mKeyguardManager).isKeyguardLocked();
        // Wifi disconnected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);

        mWirelessDebugging.setIsEnabled(false);

        assertThat(mWirelessDebugging.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnabled_false_keyguardLocked_WifiDisconnected_shouldDisableAdbWifi() {
        // Precondition: set the tile to enabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);
        // Locked keyguard
        doReturn(true).when(mKeyguardManager).isKeyguardLocked();
        // Wifi disconnected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);

        mWirelessDebugging.setIsEnabled(false);

        assertThat(mWirelessDebugging.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnabled_false_keyguardUnlocked_WifiConnected_shouldDisableAdbWifi() {
        // Precondition: set the tile to enabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);
        // Unlocked keyguard
        doReturn(false).when(mKeyguardManager).isKeyguardLocked();
        // Wifi connected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);

        mWirelessDebugging.setIsEnabled(false);

        assertThat(mWirelessDebugging.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnabled_false_keyguardLocked_WifiConnected_shouldDisableAdbWifi() {
        // Precondition: set the tile to enabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 1 /* setting enabled */);
        // Locked keyguard
        doReturn(true).when(mKeyguardManager).isKeyguardLocked();
        // Wifi connected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);

        mWirelessDebugging.setIsEnabled(false);

        assertThat(mWirelessDebugging.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnabled_true_keyguardUnlocked_WifiDisconnected_shouldDisableAdbWifi() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mWirelessDebugging).sendBroadcast(intentCaptor.capture());
        // Precondition: set the tile to disabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting enabled */);
        // Unlocked keyguard
        doReturn(false).when(mKeyguardManager).isKeyguardLocked();
        // Wifi disconnected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);

        mWirelessDebugging.setIsEnabled(true);

        assertThat(mWirelessDebugging.isEnabled()).isFalse();
        // The notification shade should be hidden by sending a broadcast to SysUI
        // so the toast can be seen
        verify(mWirelessDebugging, times(1)).sendBroadcast(eq(intentCaptor.getValue()));
        assertThat(intentCaptor.getValue().getAction())
            .isEqualTo(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        // Should also get a toast that wifi is not connected
        verify(mToast).show();
    }

    @Test
    public void setIsEnabled_true_keyguardLocked_WifiDisconnected_shouldDisableAdbWifi() {
        // Precondition: set the tile to disabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting enabled */);
        // Locked keyguard
        doReturn(true).when(mKeyguardManager).isKeyguardLocked();
        // Wifi disconnected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(false);

        mWirelessDebugging.setIsEnabled(true);

        // Shouldn't be able to enable wireless debugging from locked screen
        assertThat(mWirelessDebugging.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnabled_true_keyguardUnlocked_WifiConnected_shouldDisableAdbWifi() {
        // Precondition: set the tile to disabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting enabled */);
        // Unlocked keyguard
        doReturn(false).when(mKeyguardManager).isKeyguardLocked();
        // Wifi connected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);

        mWirelessDebugging.setIsEnabled(true);

        assertThat(mWirelessDebugging.isEnabled()).isTrue();
    }

    @Test
    public void setIsEnabled_true_keyguardLocked_WifiConnected_shouldDisableAdbWifi() {
        // Precondition: set the tile to disabled
        Global.putInt(mContext.getContentResolver(),
                Global.ADB_WIFI_ENABLED, 0 /* setting enabled */);
        // Locked keyguard
        doReturn(true).when(mKeyguardManager).isKeyguardLocked();
        // Wifi connected
        ShadowWirelessDebuggingPreferenceController.setIsWifiConnected(true);

        mWirelessDebugging.setIsEnabled(true);

        // Shouldn't be able to enable wireless debugging from locked screen
        assertThat(mWirelessDebugging.isEnabled()).isFalse();
    }
}
