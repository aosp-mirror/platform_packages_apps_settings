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
package com.android.settings.wifi.p2p;

import static android.arch.lifecycle.Lifecycle.Event.ON_PAUSE;
import static android.arch.lifecycle.Lifecycle.Event.ON_RESUME;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.lifecycle.LifecycleOwner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiP2PPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mWifiDirectPreference;

    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private WifiP2pPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mScreen.findPreference(anyString())).thenReturn(mWifiDirectPreference);
        mController = new WifiP2pPreferenceController(mContext, mLifecycle, mWifiManager);
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testOnResume_shouldRegisterListener() {
        mLifecycle.handleLifecycleEvent(ON_RESUME);
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));
    }

    @Test
    public void testOnPause_shouldUnregisterListener() {
        mLifecycle.handleLifecycleEvent(ON_RESUME);
        mLifecycle.handleLifecycleEvent(ON_PAUSE);
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void testWifiStateChange_shouldToggleEnabledState() {
        when(mWifiManager.isWifiEnabled()).thenReturn(true);

        //Sets the preferences.
        mController.displayPreference(mScreen);
        verify(mWifiDirectPreference).setEnabled(true);

        Intent dummyIntent = new Intent();
        mController.mReceiver.onReceive(mContext, dummyIntent);
        verify(mWifiDirectPreference, times(2)).setEnabled(true);

        when(mWifiManager.isWifiEnabled()).thenReturn(false);
        mController.mReceiver.onReceive(mContext, dummyIntent);
        verify(mWifiDirectPreference).setEnabled(false);
    }

    @Test
    public void testDisplayPreference_shouldToggleEnabledState() {
        when(mWifiManager.isWifiEnabled()).thenReturn(true);
        mController.displayPreference(mScreen);
        verify(mWifiDirectPreference).setEnabled(true);

        when(mWifiManager.isWifiEnabled()).thenReturn(false);
        mController.displayPreference(mScreen);
        verify(mWifiDirectPreference).setEnabled(false);
    }
}
