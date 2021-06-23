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

package com.android.settings.wifi.dpp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.HandlerThread;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.test.InstrumentationRegistry;

import com.android.settings.wifi.WifiEntryPreference;
import com.android.wifitrackerlib.SavedNetworkTracker;
import com.android.wifitrackerlib.WifiEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class WifiNetworkListFragmentTest {
    private WifiNetworkListFragment mWifiNetworkListFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(InstrumentationRegistry.getTargetContext());
        mWifiNetworkListFragment = spy(new WifiNetworkListFragment());
        doReturn(mContext).when(mWifiNetworkListFragment).getContext();
        mWifiNetworkListFragment.mWifiManager = mock(WifiManager.class);
        mWifiNetworkListFragment.mSavedNetworkTracker = mock(SavedNetworkTracker.class);
        mWifiNetworkListFragment.mPreferenceGroup = mock(PreferenceCategory.class);
        mWifiNetworkListFragment.mAddPreference = mock(Preference.class);
        mWifiNetworkListFragment.mOnChooseNetworkListener =
                mock(WifiNetworkListFragment.OnChooseNetworkListener.class);
    }

    @Test
    public void onActivityResult_addNetworkRequestOk_shouldSaveNetwork() {
        final WifiConfiguration wifiConfig = new WifiConfiguration();
        final Intent intent = new Intent();
        intent.putExtra(WifiNetworkListFragment.WIFI_CONFIG_KEY, wifiConfig);

        mWifiNetworkListFragment.onActivityResult(WifiNetworkListFragment.ADD_NETWORK_REQUEST,
                Activity.RESULT_OK, intent);

        verify(mWifiNetworkListFragment.mWifiManager).save(eq(wifiConfig), any());
    }

    @Test
    public void onSavedWifiEntriesChanged_noSavedWifiEntry_onlyAddNetworkPreference() {
        when(mWifiNetworkListFragment.mSavedNetworkTracker.getSavedWifiEntries())
                .thenReturn(Arrays.asList());

        mWifiNetworkListFragment.onSavedWifiEntriesChanged();

        verify(mWifiNetworkListFragment.mPreferenceGroup).addPreference(
                mWifiNetworkListFragment.mAddPreference);
    }

    @Test
    public void onSavedWifiEntriesChanged_openSavedWifiEntry_onlyAddNetworkPreference() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_NONE);
        when(mWifiNetworkListFragment.mSavedNetworkTracker.getSavedWifiEntries())
                .thenReturn(Arrays.asList(wifiEntry));

        mWifiNetworkListFragment.onSavedWifiEntriesChanged();

        verify(mWifiNetworkListFragment.mPreferenceGroup).addPreference(
                mWifiNetworkListFragment.mAddPreference);
    }

    @Test
    public void onSavedWifiEntriesChanged_pskSavedWifiEntry_add2Preferences() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_PSK);
        when(mWifiNetworkListFragment.mSavedNetworkTracker.getSavedWifiEntries())
                .thenReturn(Arrays.asList(wifiEntry));

        mWifiNetworkListFragment.onSavedWifiEntriesChanged();

        verify(mWifiNetworkListFragment.mPreferenceGroup, times(2)).addPreference(any());
    }

    @Test
    public void onSavedWifiEntriesChanged_saeSavedWifiEntry_add2Preferences() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getSecurity()).thenReturn(WifiEntry.SECURITY_SAE);
        when(mWifiNetworkListFragment.mSavedNetworkTracker.getSavedWifiEntries())
                .thenReturn(Arrays.asList(wifiEntry));

        mWifiNetworkListFragment.onSavedWifiEntriesChanged();

        verify(mWifiNetworkListFragment.mPreferenceGroup, times(2)).addPreference(any());
    }

    @Test
    public void onPreferenceClick_validWifiEntryPreference_onChooseNetwork() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.getSecurityString(true /* concise */)).thenReturn("WPA3");
        final WifiConfiguration wifiConfig = mock(WifiConfiguration.class);
        when(wifiConfig.getPrintableSsid()).thenReturn("ssid");
        when(wifiEntry.getWifiConfiguration()).thenReturn(wifiConfig);
        final WifiEntryPreference preference = new WifiEntryPreference(mContext, wifiEntry);

        mWifiNetworkListFragment.onPreferenceClick(preference);

        verify(mWifiNetworkListFragment.mOnChooseNetworkListener).onChooseNetwork(any());
    }

    @Test
    public void onDestroy_quitWorkerThread() {
        mWifiNetworkListFragment.mWorkerThread = mock(HandlerThread.class);

        try {
            mWifiNetworkListFragment.onDestroyView();
        } catch (IllegalArgumentException e) {
            // Ignore the exception from super class.
        }

        verify(mWifiNetworkListFragment.mWorkerThread).quit();
    }
}
