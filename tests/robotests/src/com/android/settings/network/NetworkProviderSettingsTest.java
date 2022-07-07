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

import static com.android.settings.network.NetworkProviderSettings.MENU_ID_DISCONNECT;
import static com.android.settings.network.NetworkProviderSettings.MENU_ID_FORGET;
import static com.android.settings.network.NetworkProviderSettings.MENU_ID_SHARE;
import static com.android.settings.wifi.WifiConfigUiBase2.MODE_CONNECT;
import static com.android.settings.wifi.WifiConfigUiBase2.MODE_MODIFY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.util.FeatureFlagUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.datausage.DataUsagePreference;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.wifi.AddWifiNetworkPreference;
import com.android.settings.wifi.ConnectedWifiEntryPreference;
import com.android.settings.wifi.WifiConfigController2;
import com.android.settings.wifi.WifiDialog2;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.wifi.LongPressWifiEntryPreference;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
public class NetworkProviderSettingsTest {

    private static final int NUM_NETWORKS = 4;
    private static final String FAKE_URI_STRING = "fakeuri";

    @Mock
    private PowerManager mPowerManager;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private LocationManager mLocationManager;
    @Mock
    private AirplaneModeEnabler mAirplaneModeEnabler;
    @Mock
    private DataUsagePreference mDataUsagePreference;
    private Context mContext;
    private NetworkProviderSettings mNetworkProviderSettings;
    @Mock
    private WifiPickerTracker mMockWifiPickerTracker;
    @Mock
    private WifiEntry mWifiEntry;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private InternetResetHelper mInternetResetHelper;
    @Mock
    private Preference mAirplaneModeMsgPreference;
    @Mock
    private LayoutPreference mResetInternetPreference;
    @Mock
    private ContextMenu mContextMenu;
    @Mock
    private MenuItem mMenuItem;
    @Mock
    InternetUpdater mInternetUpdater;
    @Mock
    PreferenceCategory mConnectedWifiEntryPreferenceCategory;
    @Mock
    PreferenceCategory mFirstWifiEntryPreferenceCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mNetworkProviderSettings = spy(new NetworkProviderSettings());
        doReturn(mContext).when(mNetworkProviderSettings).getContext();
        doReturn(mPreferenceManager).when(mNetworkProviderSettings).getPreferenceManager();
        doReturn(mPowerManager).when(mContext).getSystemService(PowerManager.class);
        doReturn(mWifiManager).when(mContext).getSystemService(WifiManager.class);
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(mLocationManager).when(mContext).getSystemService(LocationManager.class);
        when(mUserManager.hasBaseUserRestriction(any(), any())).thenReturn(true);
        doReturn(mContext).when(mPreferenceManager).getContext();
        mNetworkProviderSettings.mAddWifiNetworkPreference = new AddWifiNetworkPreference(mContext);
        mNetworkProviderSettings.mSavedNetworksPreference = new Preference(mContext);
        mNetworkProviderSettings.mConfigureWifiSettingsPreference =
                new Preference(mContext);
        mNetworkProviderSettings.mWifiPickerTracker = mMockWifiPickerTracker;
        mNetworkProviderSettings.mWifiManager = mWifiManager;
        mNetworkProviderSettings.mResetInternetPreference = mResetInternetPreference;
        mNetworkProviderSettings.mAirplaneModeMsgPreference = mAirplaneModeMsgPreference;
        mNetworkProviderSettings.mAirplaneModeEnabler = mAirplaneModeEnabler;
        mNetworkProviderSettings.mInternetUpdater = mInternetUpdater;
        mNetworkProviderSettings.mWifiStatusMessagePreference = new FooterPreference(mContext);
        doReturn(NetworkProviderSettings.PREF_KEY_CONNECTED_ACCESS_POINTS)
                .when(mConnectedWifiEntryPreferenceCategory).getKey();
        mNetworkProviderSettings.mConnectedWifiEntryPreferenceCategory =
                mConnectedWifiEntryPreferenceCategory;
        doReturn(NetworkProviderSettings.PREF_KEY_FIRST_ACCESS_POINTS)
                .when(mFirstWifiEntryPreferenceCategory).getKey();
        mNetworkProviderSettings.mFirstWifiEntryPreferenceCategory =
                mFirstWifiEntryPreferenceCategory;
    }

    @Test
    public void addNetworkFragmentSendResult_onActivityResult_shouldHandleEvent() {
        final NetworkProviderSettings NetworkProviderSettings = spy(new NetworkProviderSettings());
        final Intent intent = new Intent();
        doNothing().when(NetworkProviderSettings).handleAddNetworkRequest(anyInt(),
                any(Intent.class));

        NetworkProviderSettings.onActivityResult(NetworkProviderSettings.ADD_NETWORK_REQUEST,
                Activity.RESULT_OK, intent);

        verify(NetworkProviderSettings).handleAddNetworkRequest(anyInt(), any(Intent.class));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasSavedNetwork_preferenceVisible() {
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(NUM_NETWORKS);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(0 /* count */);

        mNetworkProviderSettings.setAdditionalSettingsSummaries();

        assertThat(mNetworkProviderSettings.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mNetworkProviderSettings.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_access_points_summary,
                        NUM_NETWORKS, NUM_NETWORKS));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasSavedPasspointNetwork_preferenceVisible() {
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(0 /* count */);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(NUM_NETWORKS);

        mNetworkProviderSettings.setAdditionalSettingsSummaries();

        assertThat(mNetworkProviderSettings.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mNetworkProviderSettings.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                        R.plurals.wifi_saved_passpoint_access_points_summary,
                        NUM_NETWORKS, NUM_NETWORKS));
    }

    @Test
    public void setAdditionalSettingsSummaries_hasTwoKindsSavedNetwork_preferenceVisible() {
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(NUM_NETWORKS);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(NUM_NETWORKS);

        mNetworkProviderSettings.setAdditionalSettingsSummaries();

        assertThat(mNetworkProviderSettings.mSavedNetworksPreference.isVisible()).isTrue();
        assertThat(mNetworkProviderSettings.mSavedNetworksPreference.getSummary()).isEqualTo(
                mContext.getResources().getQuantityString(
                R.plurals.wifi_saved_all_access_points_summary,
                NUM_NETWORKS * 2, NUM_NETWORKS * 2));
    }

    @Test
    public void setAdditionalSettingsSummaries_noSavedNetwork_preferenceInvisible() {
        when(mMockWifiPickerTracker.getNumSavedNetworks()).thenReturn(0 /* count */);
        when(mMockWifiPickerTracker.getNumSavedSubscriptions()).thenReturn(0 /* count */);

        mNetworkProviderSettings.setAdditionalSettingsSummaries();

        assertThat(mNetworkProviderSettings.mSavedNetworksPreference.isVisible()).isFalse();
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupEnabled_displayOn() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(true);
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        Settings.Global.putInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0);
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);

        mNetworkProviderSettings.setAdditionalSettingsSummaries();

        assertThat(mNetworkProviderSettings
                .mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_on));
    }

    @Test
    public void setAdditionalSettingsSummaries_wifiWakeupDisabled_displayOff() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        when(mWifiManager.isAutoWakeupEnabled()).thenReturn(false);

        mNetworkProviderSettings.setAdditionalSettingsSummaries();

        assertThat(mNetworkProviderSettings
                .mConfigureWifiSettingsPreference.getSummary()).isEqualTo(
                mContext.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off));
    }

    @Test
    public void checkAddWifiNetworkPreference_preferenceVisible() {
        assertThat(mNetworkProviderSettings.mAddWifiNetworkPreference.isVisible()).isTrue();
        assertThat(mNetworkProviderSettings.mAddWifiNetworkPreference.getTitle()).isEqualTo(
                mContext.getString(R.string.wifi_add_network));
    }

    private void setUpForOnCreate() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(mNetworkProviderSettings.getActivity()).thenReturn(activity);
        final Resources.Theme theme = mContext.getTheme();
        when(activity.getTheme()).thenReturn(theme);
        UserManager userManager = mock(UserManager.class);
        when(activity.getSystemService(Context.USER_SERVICE))
                .thenReturn(userManager);

        when(mNetworkProviderSettings.findPreference(NetworkProviderSettings.PREF_KEY_DATA_USAGE))
                .thenReturn(mDataUsagePreference);
    }

    @Test
    @Config(shadows = {ShadowDataUsageUtils.class, ShadowFragment.class})
    public void checkDataUsagePreference_perferenceInvisibleIfWifiNotSupported() {
        setUpForOnCreate();
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = false;

        mNetworkProviderSettings.onCreate(Bundle.EMPTY);

        verify(mDataUsagePreference).setVisible(false);
    }

    @Test
    @Config(shadows = {ShadowDataUsageUtils.class, ShadowFragment.class})
    public void checkDataUsagePreference_perferenceVisibleIfWifiSupported() {
        setUpForOnCreate();
        ShadowDataUsageUtils.IS_WIFI_SUPPORTED = true;

        mNetworkProviderSettings.onCreate(Bundle.EMPTY);

        verify(mDataUsagePreference).setVisible(true);
        verify(mDataUsagePreference).setTemplate(any(), eq(0) /*subId*/, eq(null) /*service*/);
    }

    @Test
    public void onCreateAdapter_hasStableIdsTrue() {
        final PreferenceScreen preferenceScreen = mock(PreferenceScreen.class);
        when(preferenceScreen.getContext()).thenReturn(mContext);

        RecyclerView.Adapter adapter = mNetworkProviderSettings.onCreateAdapter(preferenceScreen);

        assertThat(adapter.hasStableIds()).isTrue();
    }

    @Test
    public void onCreateContextMenu_shouldHaveForgetAndDisconnectMenuForConnectedWifiEntry() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getApplicationContext()).thenReturn(mContext);
        when(mNetworkProviderSettings.getActivity()).thenReturn(activity);

        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.canDisconnect()).thenReturn(true);
        when(wifiEntry.canForget()).thenReturn(true);
        when(wifiEntry.isSaved()).thenReturn(true);
        when(wifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTED);

        final LongPressWifiEntryPreference connectedWifiEntryPreference =
                mNetworkProviderSettings.createLongPressWifiEntryPreference(wifiEntry);
        final View view = mock(View.class);
        when(view.getTag()).thenReturn(connectedWifiEntryPreference);

        mNetworkProviderSettings.onCreateContextMenu(mContextMenu, view, null /* info */);

        verify(mContextMenu).add(anyInt(), eq(MENU_ID_FORGET), anyInt(), anyInt());
        verify(mContextMenu).add(anyInt(), eq(MENU_ID_DISCONNECT), anyInt(), anyInt());
    }

    @Test
    public void onCreateContextMenu_canShare_shouldHaveShareMenuForConnectedWifiEntry() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getApplicationContext()).thenReturn(mContext);
        when(mNetworkProviderSettings.getActivity()).thenReturn(activity);

        when(mWifiEntry.canDisconnect()).thenReturn(true);
        when(mWifiEntry.canShare()).thenReturn(true);
        when(mWifiEntry.canForget()).thenReturn(true);
        when(mWifiEntry.isSaved()).thenReturn(true);
        when(mWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTED);

        final LongPressWifiEntryPreference connectedWifiEntryPreference =
                mNetworkProviderSettings.createLongPressWifiEntryPreference(mWifiEntry);
        final View view = mock(View.class);
        when(view.getTag()).thenReturn(connectedWifiEntryPreference);

        mNetworkProviderSettings.onCreateContextMenu(mContextMenu, view, null /* info */);

        verify(mContextMenu).add(anyInt(), eq(MENU_ID_SHARE), anyInt(), anyInt());
    }

    @Test
    public void onCreateContextMenu_canNotShare_shouldDisappearShareMenuForConnectedWifiEntry() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.getApplicationContext()).thenReturn(mContext);
        when(mNetworkProviderSettings.getActivity()).thenReturn(activity);

        when(mWifiEntry.canDisconnect()).thenReturn(true);
        when(mWifiEntry.canShare()).thenReturn(false);
        when(mWifiEntry.canForget()).thenReturn(true);
        when(mWifiEntry.isSaved()).thenReturn(true);
        when(mWifiEntry.getConnectedState()).thenReturn(WifiEntry.CONNECTED_STATE_CONNECTED);

        final LongPressWifiEntryPreference connectedWifiEntryPreference =
                mNetworkProviderSettings.createLongPressWifiEntryPreference(mWifiEntry);
        final View view = mock(View.class);
        when(view.getTag()).thenReturn(connectedWifiEntryPreference);

        mNetworkProviderSettings.onCreateContextMenu(mContextMenu, view, null /* info */);

        verify(mContextMenu, never()).add(anyInt(), eq(MENU_ID_SHARE), anyInt(), anyInt());
    }

    @Test
    public void onWifiEntriesChanged_shouldChangeNextButtonState() {
        mNetworkProviderSettings.onWifiEntriesChanged();

        verify(mNetworkProviderSettings).changeNextButtonState(anyBoolean());
    }

    @Test
    public void openSubscriptionHelpPage_shouldCallStartActivityForResult() {
        doReturn(new Intent()).when(mNetworkProviderSettings).getHelpIntent(mContext,
                FAKE_URI_STRING);
        doNothing().when(mNetworkProviderSettings).startActivityForResult(any(Intent.class),
                anyInt());
        final WifiEntry mockWifiEntry = mock(WifiEntry.class);
        when(mockWifiEntry.getHelpUriString()).thenReturn(FAKE_URI_STRING);

        mNetworkProviderSettings.openSubscriptionHelpPage(mockWifiEntry);

        verify(mNetworkProviderSettings, times(1)).startActivityForResult(any(), anyInt());
    }

    @Test
    public void onNumSavedNetworksChanged_isFinishing_ShouldNotCrash() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.isFinishing()).thenReturn(true);
        when(mNetworkProviderSettings.getActivity()).thenReturn(activity);
        when(mNetworkProviderSettings.getContext()).thenReturn(null);

        mNetworkProviderSettings.onNumSavedNetworksChanged();
    }

    @Test
    public void onNumSavedSubscriptionsChanged_isFinishing_ShouldNotCrash() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(activity.isFinishing()).thenReturn(true);
        when(mNetworkProviderSettings.getActivity()).thenReturn(activity);
        when(mNetworkProviderSettings.getContext()).thenReturn(null);

        mNetworkProviderSettings.onNumSavedSubscriptionsChanged();
    }

    @Test
    public void onSubmit_modeModifyNoConfig_toastErrorMessage() {
        WifiDialog2 dialog = createWifiDialog2(MODE_MODIFY, null /* config */);

        mNetworkProviderSettings.onSubmit(dialog);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.wifi_failed_save_message));
    }

    @Test
    public void onSubmit_modeModifyHasConfig_saveWifiManager() {
        final WifiConfiguration config = mock(WifiConfiguration.class);
        WifiDialog2 dialog = createWifiDialog2(MODE_MODIFY, config);

        mNetworkProviderSettings.onSubmit(dialog);

        verify(mWifiManager).save(eq(config), any());
    }

    @Test
    public void onSubmit_modeConnectNoConfig_connectWifiEntry() {
        WifiDialog2 dialog = createWifiDialog2(MODE_CONNECT, null /* config */);
        final WifiEntry wifiEntry = dialog.getWifiEntry();

        mNetworkProviderSettings.onAttach(mContext);
        mNetworkProviderSettings.onSubmit(dialog);

        verify(mNetworkProviderSettings).connect(wifiEntry, false /* editIfNoConfig */,
                false /* fullScreenEdit*/);
    }

    @Test
    public void onSubmit_modeConnectHasConfig_connectWifiManager() {
        final WifiConfiguration config = mock(WifiConfiguration.class);
        WifiDialog2 dialog = createWifiDialog2(MODE_CONNECT, config);

        mNetworkProviderSettings.onSubmit(dialog);

        verify(mWifiManager).connect(eq(config), any(WifiManager.ActionListener.class));
    }

    private WifiDialog2 createWifiDialog2(int mode, WifiConfiguration config) {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        when(wifiEntry.canConnect()).thenReturn(true);
        final WifiConfigController2 controller = mock(WifiConfigController2.class);
        when(controller.getConfig()).thenReturn(config);
        final WifiDialog2 wifiDialog2 =  spy(WifiDialog2.createModal(mContext, null /* listener */,
                wifiEntry, mode));
        when(wifiDialog2.getController()).thenReturn(controller);
        return wifiDialog2;
    }

    @Test
    public void onCreateOptionsMenu_airplanModeOn_fixConnectivityMenuInvisible() {
        doReturn(true).when(mAirplaneModeEnabler).isAirplaneModeOn();
        final Menu menu = mock(Menu.class);
        mNetworkProviderSettings.onCreateOptionsMenu(menu, null /* inflater */);

        verify(menu, never()).add(anyInt(), eq(NetworkProviderSettings.MENU_FIX_CONNECTIVITY),
            anyInt(), eq(R.string.fix_connectivity));
    }

    @Test
    public void onCreateOptionsMenu_airplanModeOff_fixConnectivityMenuVisible() {
        doReturn(false).when(mAirplaneModeEnabler).isAirplaneModeOn();
        final Menu menu = mock(Menu.class);
        when(menu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mock(MenuItem.class));
        mNetworkProviderSettings.onCreateOptionsMenu(menu, null /* inflater */);

        verify(menu).add(anyInt(), eq(NetworkProviderSettings.MENU_FIX_CONNECTIVITY),
            anyInt(), eq(R.string.fix_connectivity));
    }

    @Test
    public void onOptionsItemSelected_fixConnectivity_restartInternet() {
        mNetworkProviderSettings.mInternetResetHelper = mInternetResetHelper;
        doReturn(false).when(mNetworkProviderSettings).isPhoneOnCall();
        doReturn(NetworkProviderSettings.MENU_FIX_CONNECTIVITY).when(mMenuItem).getItemId();

        mNetworkProviderSettings.onOptionsItemSelected(mMenuItem);

        verify(mInternetResetHelper).restart();
    }

    @Test
    public void onAirplaneModeChanged_apmIsOn_showApmMsg() {
        mNetworkProviderSettings.onAirplaneModeChanged(true);

        verify(mAirplaneModeMsgPreference).setVisible(true);
    }

    @Test
    public void onAirplaneModeChanged_apmIsOff_hideApmMsg() {
        mNetworkProviderSettings.onAirplaneModeChanged(false);

        verify(mAirplaneModeMsgPreference).setVisible(false);
    }

    @Test
    public void getConnectedWifiPreferenceCategory_internetWiFi_getConnectedAccessPoints() {
        doReturn(InternetUpdater.INTERNET_WIFI).when(mInternetUpdater).getInternetType();

        final PreferenceCategory pc = mNetworkProviderSettings.getConnectedWifiPreferenceCategory();

        assertThat(pc.getKey()).isEqualTo(NetworkProviderSettings.PREF_KEY_CONNECTED_ACCESS_POINTS);
    }

    @Test
    public void getConnectedWifiPreferenceCategory_internetCellular_getFirstAccessPoints() {
        doReturn(InternetUpdater.INTERNET_CELLULAR).when(mInternetUpdater).getInternetType();

        final PreferenceCategory pc = mNetworkProviderSettings.getConnectedWifiPreferenceCategory();

        assertThat(pc.getKey()).isEqualTo(NetworkProviderSettings.PREF_KEY_FIRST_ACCESS_POINTS);
    }

    @Test
    public void createConnectedWifiEntryPreference_internetWiFi_createConnectedPreference() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        doReturn(InternetUpdater.INTERNET_WIFI).when(mInternetUpdater).getInternetType();

        final Preference p = mNetworkProviderSettings.createConnectedWifiEntryPreference(wifiEntry);

        assertThat(p instanceof ConnectedWifiEntryPreference).isTrue();
    }

    @Test
    public void createConnectedWifiEntryPreference_internetCellular_createFirstWifiPreference() {
        final WifiEntry wifiEntry = mock(WifiEntry.class);
        doReturn(InternetUpdater.INTERNET_CELLULAR).when(mInternetUpdater).getInternetType();

        final Preference p = mNetworkProviderSettings.createConnectedWifiEntryPreference(wifiEntry);

        assertThat(p instanceof NetworkProviderSettings.FirstWifiEntryPreference).isTrue();
    }

    @Test
    public void updateWifiEntryPreferences_activityIsNull_ShouldNotCrash() {
        when(mNetworkProviderSettings.getActivity()).thenReturn(null);

        // should not crash
        mNetworkProviderSettings.updateWifiEntryPreferences();
    }

    @Test
    public void updateWifiEntryPreferences_viewIsNull_ShouldNotCrash() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        when(mNetworkProviderSettings.getActivity()).thenReturn(activity);
        when(mNetworkProviderSettings.getView()).thenReturn(null);

        // should not crash
        mNetworkProviderSettings.updateWifiEntryPreferences();
    }

    @Test
    public void updateWifiEntryPreferences_isRestricted_bypassUpdate() {
        mNetworkProviderSettings.mIsRestricted = true;
        mNetworkProviderSettings.mWifiEntryPreferenceCategory = mock(PreferenceCategory.class);

        mNetworkProviderSettings.updateWifiEntryPreferences();

        verify(mNetworkProviderSettings.mWifiEntryPreferenceCategory, never()).setVisible(true);
    }

    @Test
    public void setWifiScanMessage_wifiOnScanOn_footerIsInvisible() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);

        mNetworkProviderSettings.setWifiScanMessage(/* isWifiEnabled */ true);

        assertThat(mNetworkProviderSettings.mWifiStatusMessagePreference.isVisible()).isFalse();
    }

    @Test
    public void setWifiScanMessage_wifiOffLocationOnScanOn_footerIsVisible() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        when(mLocationManager.isLocationEnabled()).thenReturn(true);

        mNetworkProviderSettings.setWifiScanMessage(/* isWifiEnabled */ false);

        assertThat(mNetworkProviderSettings.mWifiStatusMessagePreference.isVisible()).isTrue();
        assertThat(mNetworkProviderSettings.mWifiStatusMessagePreference.getTitle().length())
            .isNotEqualTo(0);
    }

    @Test
    public void setWifiScanMessage_wifiOffLocationOnScanOff_footerIsInvisible() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);
        when(mLocationManager.isLocationEnabled()).thenReturn(true);

        mNetworkProviderSettings.setWifiScanMessage(/* isWifiEnabled */ false);

        assertThat(mNetworkProviderSettings.mWifiStatusMessagePreference.isVisible()).isFalse();
    }

    @Test
    public void setWifiScanMessage_wifiOffLocationOffScanOn_footerIsInvisible() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(true);
        when(mLocationManager.isLocationEnabled()).thenReturn(false);

        mNetworkProviderSettings.setWifiScanMessage(/* isWifiEnabled */ false);

        assertThat(mNetworkProviderSettings.mWifiStatusMessagePreference.isVisible()).isFalse();
    }

    @Test
    public void setWifiScanMessage_wifiOffLocationOffScanOff_footerIsInvisible() {
        when(mWifiManager.isScanAlwaysAvailable()).thenReturn(false);
        when(mLocationManager.isLocationEnabled()).thenReturn(false);

        mNetworkProviderSettings.setWifiScanMessage(/* isWifiEnabled */ false);

        assertThat(mNetworkProviderSettings.mWifiStatusMessagePreference.isVisible()).isFalse();
    }

    @Test
    @Config(shadows = ShadowPreferenceFragmentCompat.class)
    public void onStop_shouldRemoveCallbacks() {
        View fragmentView = mock(View.class);
        when(mNetworkProviderSettings.getView()).thenReturn(fragmentView);

        mNetworkProviderSettings.onStop();

        verify(fragmentView).removeCallbacks(mNetworkProviderSettings.mRemoveLoadingRunnable);
        verify(fragmentView).removeCallbacks(
                mNetworkProviderSettings.mUpdateWifiEntryPreferencesRunnable);
        verify(fragmentView).removeCallbacks(mNetworkProviderSettings.mHideProgressBarRunnable);
        verify(mAirplaneModeEnabler).stop();
    }

    @Test
    public void addShareMenuIfSuitable_isAdmin_addMenu() {
        mNetworkProviderSettings.mIsAdmin = true;
        Mockito.reset(mContextMenu);

        mNetworkProviderSettings.addShareMenuIfSuitable(mContextMenu);

        verify(mContextMenu).add(anyInt(), eq(MENU_ID_SHARE), anyInt(), anyInt());
    }

    @Test
    public void addShareMenuIfSuitable_isNotAdmin_notAddMenu() {
        mNetworkProviderSettings.mIsAdmin = false;
        Mockito.reset(mContextMenu);

        mNetworkProviderSettings.addShareMenuIfSuitable(mContextMenu);

        verify(mContextMenu, never()).add(anyInt(), eq(MENU_ID_SHARE), anyInt(), anyInt());
    }

    @Test
    public void addForgetMenuIfSuitable_isAdmin_addMenu() {
        mNetworkProviderSettings.mIsAdmin = true;
        Mockito.reset(mContextMenu);

        mNetworkProviderSettings.addForgetMenuIfSuitable(mContextMenu);

        verify(mContextMenu).add(anyInt(), eq(MENU_ID_FORGET), anyInt(), anyInt());
    }

    @Test
    public void addForgetMenuIfSuitable_isNotAdmin_notAddMenu() {
        mNetworkProviderSettings.mIsAdmin = false;
        Mockito.reset(mContextMenu);

        mNetworkProviderSettings.addForgetMenuIfSuitable(mContextMenu);

        verify(mContextMenu, never()).add(anyInt(), eq(MENU_ID_FORGET), anyInt(), anyInt());
    }

    @Implements(PreferenceFragmentCompat.class)
    public static class ShadowPreferenceFragmentCompat {

        @Implementation
        public void onStop() {
            // do nothing
        }
    }
}
