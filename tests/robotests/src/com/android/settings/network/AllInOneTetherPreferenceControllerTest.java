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

package com.android.settings.network;

import static com.android.settings.network.TetherEnabler.TETHERING_BLUETOOTH_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_ETHERNET_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_OFF;
import static com.android.settings.network.TetherEnabler.TETHERING_USB_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_WIFI_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.widget.MasterSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class AllInOneTetherPreferenceControllerTest {

    @ParameterizedRobolectricTestRunner.Parameters(name = "TetherState: {0}")
    public static List params() {
        return Arrays.asList(new Object[][] {
                {TETHERING_OFF, R.string.tether_settings_summary_off},
                {TETHERING_WIFI_ON, R.string.tether_settings_summary_hotspot_only},
                {TETHERING_USB_ON, R.string.tether_settings_summary_usb_tethering_only},
                {TETHERING_BLUETOOTH_ON, R.string.tether_settings_summary_bluetooth_tethering_only},
                {TETHERING_ETHERNET_ON, R.string.tether_settings_summary_ethernet_tethering_only},
                {
                        TETHERING_WIFI_ON | TETHERING_USB_ON,
                        R.string.tether_settings_summary_hotspot_and_usb
                },
                {
                        TETHERING_WIFI_ON | TETHERING_BLUETOOTH_ON,
                        R.string.tether_settings_summary_hotspot_and_bluetooth
                },
                {
                        TETHERING_WIFI_ON | TETHERING_ETHERNET_ON,
                        R.string.tether_settings_summary_hotspot_and_ethernet
                },
                {
                        TETHERING_USB_ON | TETHERING_BLUETOOTH_ON,
                        R.string.tether_settings_summary_usb_and_bluetooth
                },
                {
                        TETHERING_USB_ON | TETHERING_ETHERNET_ON,
                        R.string.tether_settings_summary_usb_and_ethernet
                },
                {
                        TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON,
                        R.string.tether_settings_summary_bluetooth_and_ethernet
                },
                {
                        TETHERING_WIFI_ON | TETHERING_USB_ON | TETHERING_BLUETOOTH_ON,
                        R.string.tether_settings_summary_hotspot_and_usb_and_bluetooth
                },
                {
                        TETHERING_WIFI_ON | TETHERING_USB_ON | TETHERING_ETHERNET_ON,
                        R.string.tether_settings_summary_hotspot_and_usb_and_ethernet
                },
                {
                        TETHERING_WIFI_ON | TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON,
                        R.string.tether_settings_summary_hotspot_and_bluetooth_and_ethernet
                },
                {
                        TETHERING_USB_ON | TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON,
                        R.string.tether_settings_summary_usb_and_bluetooth_and_ethernet
                },
                {
                        TETHERING_WIFI_ON | TETHERING_USB_ON | TETHERING_BLUETOOTH_ON
                                | TETHERING_ETHERNET_ON,
                        R.string.tether_settings_summary_all
                }
        });
    }

    private Context mContext;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private MasterSwitchPreference mPreference;

    private static final String PREF_KEY = "tether";
    private AllInOneTetherPreferenceController mController;
    private final int mTetherState;
    private final int mSummaryResId;

    public AllInOneTetherPreferenceControllerTest(int tetherState, int summaryResId) {
        mTetherState = tetherState;
        mSummaryResId = summaryResId;
    }

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        MockitoAnnotations.initMocks(this);
        mController = spy(AllInOneTetherPreferenceController.class);
        ReflectionHelpers.setField(mController, "mContext", mContext);
        ReflectionHelpers.setField(mController, "mBluetoothAdapter", mBluetoothAdapter);
        ReflectionHelpers.setField(mController, "mPreferenceKey", PREF_KEY);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(PREF_KEY)).thenReturn(mPreference);
        doReturn(mController.AVAILABLE).when(mController).getAvailabilityStatus();
        mController.displayPreference(screen);
    }

    @Test
    public void onCreate_shouldInitBluetoothPan() {
        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_ON);
        mController.onCreate();

        verify(mBluetoothAdapter).getState();
        verify(mBluetoothAdapter).getProfileProxy(mContext, mController.mBtProfileServiceListener,
                BluetoothProfile.PAN);
    }

    @Test
    public void onCreate_shouldNotInitBluetoothPanWhenBluetoothOff() {
        when(mBluetoothAdapter.getState()).thenReturn(BluetoothAdapter.STATE_OFF);
        mController.onCreate();

        verify(mBluetoothAdapter).getState();
        verifyNoMoreInteractions(mBluetoothAdapter);
    }

    @Test
    public void goThroughLifecycle_shouldDestroyBluetoothProfile() {
        final BluetoothPan pan = mock(BluetoothPan.class);
        final AtomicReference<BluetoothPan> panRef =
                ReflectionHelpers.getField(mController, "mBluetoothPan");
        panRef.set(pan);

        mController.onDestroy();

        verify(mBluetoothAdapter).closeProfileProxy(BluetoothProfile.PAN, pan);
    }

    @Test
    public void getSummary_afterTetherStateChanged() {
        mController.onTetherStateUpdated(mTetherState);
        assertThat(mController.getSummary()).isEqualTo(mContext.getString(mSummaryResId));

        verify(mController).updateState(mPreference);
        verify(mPreference).setSummary(mContext.getString(mSummaryResId));
    }
}
