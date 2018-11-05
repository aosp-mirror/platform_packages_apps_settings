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
 * limitations under the License
 */
package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.wifi.WifiTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiSettingsTest {

    private static final int NUM_NETWORKS = 4;

    @Mock
    private WifiTracker mWifiTracker;
    @Mock
    private PowerManager mPowerManager;
    private Context mContext;
    private WifiSettings mWifiSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mWifiSettings = spy(new WifiSettings());
        doReturn(mContext).when(mWifiSettings).getContext();
        doReturn(mPowerManager).when(mContext).getSystemService(PowerManager.class);
        mWifiSettings.mSavedNetworksPreference = new Preference(mContext);
        mWifiSettings.mConfigureWifiSettingsPreference = new Preference(mContext);
        mWifiSettings.mWifiTracker = mWifiTracker;
    }

    @Test
    public void testSearchIndexProvider_shouldIndexFragmentTitle() {
        final List<SearchIndexableRaw> indexRes =
                WifiSettings.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext,
                        true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).key).isEqualTo(WifiSettings.DATA_KEY_REFERENCE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testSearchIndexProvider_ifWifiSettingsNotVisible_shouldNotIndexFragmentTitle() {
        final List<SearchIndexableRaw> indexRes =
                WifiSettings.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext,
                        true /* enabled */);

        assertThat(indexRes).isEmpty();
    }

    @Test
    public void addNetworkFragmentSendResult_onActivityResult_shouldHandleEvent() {
        final WifiSettings wifiSettings = spy(new WifiSettings());
        final Intent intent = new Intent();
        doNothing().when(wifiSettings).handleAddNetworkRequest(anyInt(), any(Intent.class));

        wifiSettings.onActivityResult(WifiSettings.ADD_NETWORK_REQUEST, Activity.RESULT_OK, intent);

        verify(wifiSettings).handleAddNetworkRequest(anyInt(), any(Intent.class));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasSavedNetwork_preferenceVisible() {
        when(mWifiTracker.getNumSavedNetworks()).thenReturn(NUM_NETWORKS);

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mWifiSettings.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_access_points_summary,
                        NUM_NETWORKS, NUM_NETWORKS));
    }

    @Test
    public void setAdditionalSettingsSummaries_noSavedNetwork_preferenceInvisible() {
        when(mWifiTracker.getNumSavedNetworks()).thenReturn(0);

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mSavedNetworksPreference.isVisible()).isFalse();
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupEnabled_displayOn() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.WIFI_WAKEUP_ENABLED, 1);
        Settings.Global.putInt(contentResolver, Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 1);
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_on));
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupDisabled_displayOff() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.WIFI_WAKEUP_ENABLED, 0);

        mWifiSettings.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings.mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off));
    }
}