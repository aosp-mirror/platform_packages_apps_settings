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

package com.android.settings.network;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class TetherPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private UserManager mUserManager;
    @Mock
    private Preference mPreference;

    private TetherPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = spy(TetherPreferenceController.class);
        ReflectionHelpers.setField(mController, "mContext", mContext);
        ReflectionHelpers.setField(mController, "mConnectivityManager", mConnectivityManager);
        ReflectionHelpers.setField(mController, "mBluetoothAdapter", mBluetoothAdapter);
        ReflectionHelpers.setField(mController, "mUserManager", mUserManager);
    }

    @Test
    public void updateSummary_noPreference_noInteractionWithConnectivityManager() {
        mController.updateSummary();
        verifyNoMoreInteractions(mConnectivityManager);
    }

    @Test
    public void updateSummary_wifiTethered_shouldShowHotspotMessage() {
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
        when(mConnectivityManager.getTetheredIfaces()).thenReturn(new String[]{"123"});
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"123"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.tether_settings_summary_hotspot_on_tether_off);
    }

    @Test
    public void updateSummary_btThetherOn_shouldShowTetherMessage() {
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
        when(mConnectivityManager.getTetheredIfaces()).thenReturn(new String[]{"123"});
        when(mConnectivityManager.getTetherableBluetoothRegexs()).thenReturn(new String[]{"123"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.tether_settings_summary_hotspot_off_tether_on);
    }

    @Test
    public void updateSummary_tetherOff_shouldShowTetherOffMessage() {
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
        when(mConnectivityManager.getTetherableBluetoothRegexs()).thenReturn(new String[]{"123"});
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"456"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.switch_off_text);
    }

    @Test
    public void updateSummary_wifiBtTetherOn_shouldShowHotspotAndTetherMessage() {
        ReflectionHelpers.setField(mController, "mPreference", mPreference);
        when(mConnectivityManager.getTetheredIfaces()).thenReturn(new String[]{"123", "456"});
        when(mConnectivityManager.getTetherableWifiRegexs()).thenReturn(new String[]{"456"});
        when(mConnectivityManager.getTetherableBluetoothRegexs()).thenReturn(new String[]{"23"});

        mController.updateSummary();
        verify(mPreference).setSummary(R.string.tether_settings_summary_hotspot_on_tether_on);
    }

}
