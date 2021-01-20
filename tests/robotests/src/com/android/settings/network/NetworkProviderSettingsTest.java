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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.datausage.DataUsagePreference;
import com.android.settings.testutils.shadow.ShadowDataUsageUtils;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.wifi.AddWifiNetworkPreference;
import com.android.settings.wifi.WifiConfigController2;
import com.android.settings.wifi.WifiDialog2;
import com.android.settingslib.connectivity.ConnectivitySubsystemsRecoveryManager;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.wifi.LongPressWifiEntryPreference;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
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
    private DataUsagePreference mDataUsagePreference;
    private Context mContext;
    private NetworkProviderSettings mNetworkProviderSettings;
    @Mock
    private WifiPickerTracker mMockWifiPickerTracker;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private ConnectivitySubsystemsRecoveryManager mConnectivitySubsystemsRecoveryManager;
    @Mock
    private ViewAirplaneModeNetworksLayoutPreferenceController
            mViewAirplaneModeNetworksButtonPreference;
    @Mock
    private LayoutPreference mResetInternetPreference;
    @Mock
    private MenuItem mMenuItem;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mNetworkProviderSettings = spy(new NetworkProviderSettings());
        doReturn(mContext).when(mNetworkProviderSettings).getContext();
        doReturn(mPreferenceManager).when(mNetworkProviderSettings).getPreferenceManager();
        doReturn(mPowerManager).when(mContext).getSystemService(PowerManager.class);
        doReturn(mWifiManager).when(mContext).getSystemService(WifiManager.class);
        doReturn(mContext).when(mPreferenceManager).getContext();
        mNetworkProviderSettings.mAddWifiNetworkPreference = new AddWifiNetworkPreference(mContext);
        mNetworkProviderSettings.mSavedNetworksPreference = new Preference(mContext);
        mNetworkProviderSettings.mConfigureWifiSettingsPreference =
                new Preference(mContext);
        mNetworkProviderSettings.mWifiPickerTracker = mMockWifiPickerTracker;
        mNetworkProviderSettings.mWifiManager = mWifiManager;
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

        final ContextMenu menu = mock(ContextMenu.class);
        mNetworkProviderSettings.onCreateContextMenu(menu, view, null /* info */);

        verify(menu).add(anyInt(), eq(NetworkProviderSettings.MENU_ID_FORGET), anyInt(), anyInt());
        verify(menu).add(anyInt(), eq(NetworkProviderSettings.MENU_ID_DISCONNECT), anyInt(),
                anyInt());
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
    public void onOptionsItemSelected_fixConnectivity_triggerSubsystemRestart() {
        doReturn(true).when(mConnectivitySubsystemsRecoveryManager).isRecoveryAvailable();
        mNetworkProviderSettings.mConnectivitySubsystemsRecoveryManager =
                mConnectivitySubsystemsRecoveryManager;
        doReturn(false).when(mNetworkProviderSettings).isPhoneOnCall();
        doReturn(NetworkProviderSettings.MENU_FIX_CONNECTIVITY).when(mMenuItem).getItemId();

        mNetworkProviderSettings.onOptionsItemSelected(mMenuItem);

        verify(mConnectivitySubsystemsRecoveryManager).triggerSubsystemRestart(any(), any());
    }

    @Test
    public void onOptionsItemSelected_fixConnectivityOnCall_neverTriggerSubsystemRestart() {
        doReturn(true).when(mConnectivitySubsystemsRecoveryManager).isRecoveryAvailable();
        mNetworkProviderSettings.mConnectivitySubsystemsRecoveryManager =
                mConnectivitySubsystemsRecoveryManager;
        doReturn(true).when(mNetworkProviderSettings).isPhoneOnCall();
        doNothing().when(mNetworkProviderSettings).showResetInternetDialog();
        doReturn(NetworkProviderSettings.MENU_FIX_CONNECTIVITY).when(mMenuItem).getItemId();

        mNetworkProviderSettings.onOptionsItemSelected(mMenuItem);

        verify(mConnectivitySubsystemsRecoveryManager, never()).triggerSubsystemRestart(any(),
                any());
    }

    @Test
    public void onSubsystemRestartOperationBegin_showResetInternetHideApmNetworks() {
        mNetworkProviderSettings.mResetInternetPreference = mResetInternetPreference;
        mNetworkProviderSettings.mViewAirplaneModeNetworksButtonPreference =
                mViewAirplaneModeNetworksButtonPreference;

        mNetworkProviderSettings.onSubsystemRestartOperationBegin();

        verify(mResetInternetPreference).setVisible(true);
        verify(mViewAirplaneModeNetworksButtonPreference).setVisible(false);
    }

    @Test
    public void onSubsystemRestartOperationEnd_showApmNetworksHideResetInternet() {
        mNetworkProviderSettings.mResetInternetPreference = mResetInternetPreference;
        mNetworkProviderSettings.mViewAirplaneModeNetworksButtonPreference =
                mViewAirplaneModeNetworksButtonPreference;
        doReturn(true).when(mViewAirplaneModeNetworksButtonPreference).isAvailable();

        mNetworkProviderSettings.onSubsystemRestartOperationEnd();

        verify(mResetInternetPreference).setVisible(false);
        verify(mViewAirplaneModeNetworksButtonPreference).setVisible(true);
    }
}
