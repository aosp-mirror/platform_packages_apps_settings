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

package com.android.settings.wifi;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiInfoPreferenceControllerTest {

    private static final String TEST_MAC_ADDRESS = "42:0a:23:43:ac:02";

    @Mock
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WifiManager mWifiManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mIpPreference;
    @Mock
    private Preference mMacPreference;
    @Mock
    private WifiInfo mWifiInfo;
    @Mock
    private Resources mResources;

    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private WifiInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getSystemService(WifiManager.class))
                .thenReturn(mWifiManager);
        when(mScreen.findPreference(anyString()))
                .thenReturn(mMacPreference)
                .thenReturn(mIpPreference);
        when(mWifiManager.getConnectionInfo()).thenReturn(mWifiInfo);
        when(mWifiManager.getCurrentNetwork()).thenReturn(null);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new WifiInfoPreferenceController(mContext, mLifecycle, mWifiManager);
    }

    @Test
    public void testIsAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getPreferenceKey_shouldReturnNull() {
        assertThat(mController.getPreferenceKey()).isNull();
    }

    @Test
    public void runThroughLifecycle_shouldInstallListenerOnResume() {
        mLifecycle.handleLifecycleEvent(ON_RESUME);
        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class));

        mLifecycle.handleLifecycleEvent(ON_PAUSE);
        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
    }

    @Test
    public void onResume_shouldUpdateWifiInfo() {
        when(mWifiInfo.getMacAddress()).thenReturn(TEST_MAC_ADDRESS);

        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mMacPreference).setSummary(TEST_MAC_ADDRESS);
        verify(mIpPreference).setSummary(any());
    }

    @Test
    public void updateWifiInfo_nullWifiInfoWithMacRandomizationOff_setMacUnavailable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(false);
        mController.displayPreference(mScreen);
        when(mWifiManager.getConnectionInfo()).thenReturn(null);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(R.string.status_unavailable);
    }

    @Test
    public void updateWifiInfo_nullMacWithMacRandomizationOff_setMacUnavailable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(false);
        mController.displayPreference(mScreen);
        when(mWifiInfo.getMacAddress()).thenReturn(null);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(R.string.status_unavailable);
    }

    @Test
    public void updateWifiInfo_defaultMacWithMacRandomizationOff_setMacUnavailable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(false);
        mController.displayPreference(mScreen);
        when(mWifiInfo.getMacAddress()).thenReturn(WifiInfo.DEFAULT_MAC_ADDRESS);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(R.string.status_unavailable);
    }

    @Test
    public void updateWifiInfo_validMacWithMacRandomizationOff_setValidMac() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(false);
        mController.displayPreference(mScreen);
        when(mWifiInfo.getMacAddress()).thenReturn(TEST_MAC_ADDRESS);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(TEST_MAC_ADDRESS);
    }

    @Test
    public void updateWifiInfo_nullWifiInfoWithMacRandomizationOn_setMacUnavailable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(true);
        mController.displayPreference(mScreen);
        when(mWifiManager.getConnectionInfo()).thenReturn(null);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(R.string.status_unavailable);
    }

    @Test
    public void updateWifiInfo_nullMacWithMacRandomizationOn_setMacUnavailable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(true);
        mController.displayPreference(mScreen);
        when(mWifiInfo.getMacAddress()).thenReturn(null);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(R.string.status_unavailable);
    }

    @Test
    public void updateWifiInfo_defaultMacWithMacRandomizationOn_setMacRandomized() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(true);
        mController.displayPreference(mScreen);
        when(mWifiInfo.getMacAddress()).thenReturn(WifiInfo.DEFAULT_MAC_ADDRESS);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(R.string.wifi_status_mac_randomized);
    }

    @Test
    public void updateWifiInfo_validMacWithMacRandomizationOn_setValidMac() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_wifi_connected_mac_randomization_supported))
                .thenReturn(true);
        mController.displayPreference(mScreen);
        when(mWifiInfo.getMacAddress()).thenReturn(TEST_MAC_ADDRESS);

        mController.updateWifiInfo();

        verify(mMacPreference).setSummary(TEST_MAC_ADDRESS);
    }
}
