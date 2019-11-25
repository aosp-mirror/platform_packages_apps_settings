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
 * limitations under the License
 */
package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.datausage.DataUsagePreference;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class WifiSettings2Test {

    private static final int NUM_NETWORKS = 4;

    @Mock
    private PowerManager mPowerManager;
    @Mock
    private DataUsagePreference mDataUsagePreference;
    private Context mContext;
    private WifiSettings2 mWifiSettings2;
    @Mock
    private WifiPickerTracker mMockWifiPickerTracker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mWifiSettings2 = spy(new WifiSettings2());
        doReturn(mContext).when(mWifiSettings2).getContext();
        doReturn(mPowerManager).when(mContext).getSystemService(PowerManager.class);
        mWifiSettings2.mAddWifiNetworkPreference = new AddWifiNetworkPreference(mContext);
        mWifiSettings2.mSavedNetworksPreference = new Preference(mContext);
        mWifiSettings2.mConfigureWifiSettingsPreference = new Preference(mContext);
        mWifiSettings2.mWifiPickerTracker = mMockWifiPickerTracker;
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
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(NUM_NETWORKS);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(0 /* count */);

        mWifiSettings2.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings2.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mWifiSettings2.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_access_points_summary,
                        NUM_NETWORKS, NUM_NETWORKS));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasSavedPasspointNetwork_preferenceVisible() {
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(0 /* count */);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(NUM_NETWORKS);

        mWifiSettings2.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings2.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mWifiSettings2.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_passpoint_access_points_summary,
                        NUM_NETWORKS, NUM_NETWORKS));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasTwoKindsSavedNetwork_preferenceVisible() {
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(NUM_NETWORKS);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(NUM_NETWORKS);

        mWifiSettings2.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings2.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mWifiSettings2.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_all_access_points_summary,
                        NUM_NETWORKS*2, NUM_NETWORKS*2));
    }

    @Test
    public void setAdditionalSettingsSummaries_noSavedNetwork_preferenceInvisible() {
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(0 /* count */);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(0 /* count */);

        mWifiSettings2.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings2.mSavedNetworksPreference.isVisible()).isFalse();
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupEnabled_displayOn() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.WIFI_WAKEUP_ENABLED, 1);
        Settings.Global.putInt(contentResolver, Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 1);
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);

        mWifiSettings2.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings2.mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_on));
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupDisabled_displayOff() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putInt(contentResolver, Settings.Global.WIFI_WAKEUP_ENABLED, 0);

        mWifiSettings2.setAdditionalSettingsSummaries();

        assertThat(mWifiSettings2.mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off));
    }

    @Test
    public void checkAddWifiNetworkPrefernce_preferenceVisible() {
        assertThat(mWifiSettings2.mAddWifiNetworkPreference.isVisible()).isTrue();
        assertThat(mWifiSettings2.mAddWifiNetworkPreference.getTitle()).isEqualTo(
                mContext.getString(R.string.wifi_add_network));
    }

    private void setUpForOnCreate() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(mWifiSettings2.getActivity()).thenReturn(activity);
        final Resources.Theme theme = mContext.getTheme();
        when(activity.getTheme()).thenReturn(theme);
        UserManager userManager = mock(UserManager.class);
        when(activity.getSystemService(Context.USER_SERVICE))
                .thenReturn(userManager);

        when(mWifiSettings2.findPreference(WifiSettings.PREF_KEY_DATA_USAGE))
                .thenReturn(mDataUsagePreference);
    }

    @Test
    @Config(shadows = {ShadowDataUsageUtils.class, ShadowFragment.class})
    public void checkDataUsagePreference_perferenceInvisibleIfWifiNotSupported() {
        setUpForOnCreate();
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = false;

        mWifiSettings2.onCreate(Bundle.EMPTY);

        verify(mDataUsagePreference).setVisible(false);
    }

    @Test
    @Config(shadows = {ShadowDataUsageUtils.class, ShadowFragment.class})
    public void checkDataUsagePreference_perferenceVisibleIfWifiSupported() {
        setUpForOnCreate();
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;

        mWifiSettings2.onCreate(Bundle.EMPTY);

        verify(mDataUsagePreference).setVisible(true);
        verify(mDataUsagePreference).setTemplate(any(), eq(0) /*subId*/, eq(null) /*service*/);
    }

    @Test
    public void onCreateAdapter_hasStableIdsTrue() {
        final PreferenceScreen preferenceScreen = mock(PreferenceScreen.class);
        when(preferenceScreen.getContext()).thenReturn(mContext);

        RecyclerView.Adapter adapter = mWifiSettings2.onCreateAdapter(preferenceScreen);

        assertThat(adapter.hasStableIds()).isTrue();
    }

// TODO(b/70983952): Add test for context menu
//    @Test
//    public void onCreateContextMenu_shouldHaveForgetMenuForConnectedAccessPreference() {
//        final FragmentActivity mockActivity = mock(FragmentActivity.class);
//        when(mockActivity.getApplicationContext()).thenReturn(mContext);
//        when(mWifiSettings2.getActivity()).thenReturn(mockActivity);
//
//        final AccessPoint accessPoint = mock(AccessPoint.class);
//        when(accessPoint.isConnectable()).thenReturn(false);
//        when(accessPoint.isSaved()).thenReturn(true);
//        when(accessPoint.isActive()).thenReturn(true);
//
//        final ConnectedAccessPointPreference connectedPreference =
//                mWifiSettings2.createConnectedAccessPointPreference(accessPoint, mContext);
//        final View view = mock(View.class);
//        when(view.getTag()).thenReturn(connectedPreference);
//
//        final ContextMenu menu = mock(ContextMenu.class);
//        mWifiSettings2.onCreateContextMenu(menu, view, null /* info */);
//
//        verify(menu).add(anyInt(), eq(WifiSettings.MENU_ID_FORGET), anyInt(), anyInt());
//    }
}
