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

import static com.android.settings.AllInOneTetherSettings.WIFI_TETHER_DISABLE_KEY;
import static com.android.settings.network.TetherEnabler.TETHERING_BLUETOOTH_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_ETHERNET_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_OFF;
import static com.android.settings.network.TetherEnabler.TETHERING_USB_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.List;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class WifiTetherDisablePreferenceControllerTest {

    @ParameterizedRobolectricTestRunner.Parameters(name = "TetherState: {0}")
    public static List params() {
        return Arrays.asList(new Object[][] {
                {TETHERING_OFF, R.string.summary_placeholder},
                {TETHERING_USB_ON, R.string.disable_wifi_hotspot_when_usb_on},
                {TETHERING_BLUETOOTH_ON, R.string.disable_wifi_hotspot_when_bluetooth_on},
                {TETHERING_ETHERNET_ON, R.string.disable_wifi_hotspot_when_ethernet_on},
                {
                        TETHERING_USB_ON | TETHERING_BLUETOOTH_ON,
                        R.string.disable_wifi_hotspot_when_usb_and_bluetooth_on
                },
                {
                        TETHERING_USB_ON | TETHERING_ETHERNET_ON,
                        R.string.disable_wifi_hotspot_when_usb_and_ethernet_on
                },
                {
                        TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON,
                        R.string.disable_wifi_hotspot_when_bluetooth_and_ethernet_on
                },
                {
                        TETHERING_USB_ON | TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON,
                        R.string.disable_wifi_hotspot_when_usb_and_bluetooth_and_ethernet_on
                }
        });
    }

    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private TetherEnabler mTetherEnabler;

    private SwitchPreference mPreference;
    private Context mContext;
    private WifiTetherDisablePreferenceController mController;
    private final int mTetherState;
    private final int mSummaryResId;

    public WifiTetherDisablePreferenceControllerTest(int tetherState, int summaryResId) {
        mTetherState = tetherState;
        mSummaryResId = summaryResId;
    }

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
    public void shouldShow_noTetherableWifi() {
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[0]);
        assertThat(mController.shouldShow()).isFalse();
    }

    @Test
    public void onTetherStateUpdated_visibilityChangeCorrectly() {
        int state = TetherEnabler.TETHERING_BLUETOOTH_ON;
        mController.onTetherStateUpdated(state);
        assertThat(mController.shouldShow()).isTrue();

        state |= TetherEnabler.TETHERING_USB_ON;
        mController.onTetherStateUpdated(state);
        assertThat(mController.shouldShow()).isTrue();

        state = TetherEnabler.TETHERING_USB_ON;
        mController.onTetherStateUpdated(state);
        assertThat(mController.shouldShow()).isTrue();

        state = TetherEnabler.TETHERING_OFF;
        mController.onTetherStateUpdated(state);
        assertThat(mController.shouldShow()).isFalse();
    }

    @Test
    public void getSummary_onTetherStateUpdated() {
        mController.onTetherStateUpdated(mTetherState);
        assertThat(mController.getSummary()).isEqualTo(mContext.getString(mSummaryResId));
    }
}
