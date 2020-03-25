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

import static com.android.settings.network.TetherEnabler.WIFI_TETHER_DISABLE_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class WifiTetherDisablePreferenceControllerTest {

    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private TetherEnabler mTetherEnabler;

    private SwitchPreference mPreference;
    private Context mContext;
    private WifiTetherDisablePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mPreference = spy(SwitchPreference.class);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(
                mConnectivityManager);
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{""});
        mController = new WifiTetherDisablePreferenceController(mContext, WIFI_TETHER_DISABLE_KEY);
        mController.setTetherEnabler(mTetherEnabler);
        ReflectionHelpers.setField(mController, "mScreen", mPreferenceScreen);
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
        when(mPreferenceScreen.findPreference(WIFI_TETHER_DISABLE_KEY)).thenReturn(mPreference);
    }

    @Test
    public void display_availableChangedCorrectly() {
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[0]);
        assertThat(mController.isAvailable()).isFalse();

        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"test"});
        ReflectionHelpers.setField(mController, "mBluetoothTethering", false);
        ReflectionHelpers.setField(mController, "mUsbTethering", false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void switch_shouldListenToUsbAndBluetooth() {
        int state = TetherEnabler.TETHERING_BLUETOOTH_ON;
        mController.onTetherStateUpdated(state);
        verify(mPreference).setVisible(eq(true));

        state |= TetherEnabler.TETHERING_USB_ON;
        mController.onTetherStateUpdated(state);
        assertThat(mController.shouldShow()).isTrue();

        state = TetherEnabler.TETHERING_USB_ON;
        mController.onTetherStateUpdated(state);
        assertThat(mController.shouldShow()).isTrue();

        state = TetherEnabler.TETHERING_OFF;
        mController.onTetherStateUpdated(state);
        verify(mPreference).setVisible(eq(false));
    }
}
