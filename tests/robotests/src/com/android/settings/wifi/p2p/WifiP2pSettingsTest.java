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

package com.android.settings.wifi.p2p;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowInteractionJankMonitor.class)
@Ignore
public class WifiP2pSettingsTest {

    private Context mContext;
    private FragmentActivity mActivity;
    private TestWifiP2pSettings mFragment;

    @Mock
    public WifiP2pManager mWifiP2pManager;

    @Mock
    private WifiP2pManager.Channel mChannel;

    @Mock
    private WifiP2pPeer mWifiP2pPeer;

    @Mock
    private WifiP2pGroup mWifiP2pGroup;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        TestWifiP2pSettings.sMockWifiP2pManager = mWifiP2pManager;

        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragment = new TestWifiP2pSettings();
        mFragment.mWifiP2pManager = mWifiP2pManager;
        doReturn(mChannel).when(mWifiP2pManager).initialize(any(), any(), any());
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(mFragment, null);
        fragmentTransaction.commit();
    }

    @Test
    public void preferenceScreenKey_shouldContainsAllControllerKeys() {
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(mContext,
                mFragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : mFragment.createPreferenceControllers(
                mContext)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAtLeastElementsIn(preferenceKeys);
    }

    @Test
    public void onCreateView_withNullBundle_canNotGetValue() {
        mFragment.onCreateView(LayoutInflater.from(mContext), null, null);

        assertThat(mFragment.mSelectedWifiPeer).isNull();
    }

    @Test
    public void onCreateView_withDeviceName_shouldGetDeviceName() {
        final String fakeDeviceName = "fakename";
        final Bundle bundle = new Bundle();
        bundle.putString(WifiP2pSettings.SAVE_DEVICE_NAME, fakeDeviceName);

        mFragment.onCreateView(LayoutInflater.from(mContext), null, bundle);

        assertThat(mFragment.mSavedDeviceName).isEqualTo(fakeDeviceName);
    }

    @Test
    public void onCreateView_withGroupName_shouldGetGroupName() {
        final String fakeGroupName = "fakegroup";
        final Bundle bundle = new Bundle();
        bundle.putString(WifiP2pSettings.SAVE_SELECTED_GROUP, fakeGroupName);

        mFragment.onCreateView(LayoutInflater.from(mContext), null, bundle);

        assertThat(mFragment.mSelectedGroupName).isEqualTo(fakeGroupName);
        assertThat(mFragment.mSavedDeviceName).isNull();
    }

    @Test
    public void networkInfo_afterOnDeviceInfoAvailable_shouldBeRequested() {
        mFragment.onDeviceInfoAvailable(mock(WifiP2pDevice.class));
        verify(mWifiP2pManager, times(1)).requestNetworkInfo(any(), any());
    }

    @Test
    public void onDeviceInfoAvailable_nullChannel_shouldBeIgnored() {
        mFragment.sChannel = null;
        mFragment.onDeviceInfoAvailable(mock(WifiP2pDevice.class));
        verify(mWifiP2pManager, never()).requestNetworkInfo(any(), any());
    }

    @Test
    public void beSearching_getP2pStateDisabledIntent_shouldBeFalse() {
        final Bundle bundle = new Bundle();
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        bundle.putInt(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED);
        intent.putExtras(bundle);

        mFragment.mReceiver.onReceive(mContext, intent);

        assertThat(mFragment.mWifiP2pSearching).isFalse();
    }

    @Test
    public void beSearching_getP2pStateEnabledIntent_shouldBeTrue() {
        final Bundle bundle = new Bundle();
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        bundle.putInt(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        intent.putExtras(bundle);

        mFragment.mReceiver.onReceive(mContext, intent);

        assertThat(mFragment.mWifiP2pSearching).isTrue();
    }

    @Test
    public void withEmptyP2pDeviceList_getP2pPeerChangeIntent_connectedDevicesShouldBeZero() {
        final WifiP2pDeviceList peers = new WifiP2pDeviceList();
        final Bundle bundle = new Bundle();
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        bundle.putParcelable(WifiP2pManager.EXTRA_P2P_DEVICE_LIST, peers);
        intent.putExtras(bundle);

        mFragment.mReceiver.onReceive(mContext, intent);

        assertThat(mFragment.mConnectedDevices).isEqualTo(0);
    }

    @Test
    public void lastGroupForm_whenGroupFormInWifiP2pInfoIsFalse_beSetAsFalse() {
        final NetworkInfo networkInfo = mock(NetworkInfo.class);
        doReturn(true).when(networkInfo).isConnected();
        final WifiP2pInfo wifiP2pInfo = mock(WifiP2pInfo.class);
        wifiP2pInfo.groupFormed = false;
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(WifiP2pManager.EXTRA_NETWORK_INFO, networkInfo);
        bundle.putParcelable(WifiP2pManager.EXTRA_WIFI_P2P_INFO, wifiP2pInfo);
        intent.putExtras(bundle);

        mFragment.mReceiver.onReceive(mContext, intent);

        assertThat(mFragment.mLastGroupFormed).isFalse();
    }

    @Test
    public void lastGroupForm_whenGroupFormInWifiP2pInfoIsTrue_beSetAsTrue() {
        final NetworkInfo networkInfo = mock(NetworkInfo.class);
        doReturn(true).when(networkInfo).isConnected();
        final WifiP2pInfo wifiP2pInfo = mock(WifiP2pInfo.class);
        wifiP2pInfo.groupFormed = true;
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(WifiP2pManager.EXTRA_NETWORK_INFO, networkInfo);
        bundle.putParcelable(WifiP2pManager.EXTRA_WIFI_P2P_INFO, wifiP2pInfo);
        intent.putExtras(bundle);

        mFragment.mReceiver.onReceive(mContext, intent);

        assertThat(mFragment.mLastGroupFormed).isTrue();
    }

    @Test
    public void clickCancel_withInvitedPeerDialog_shouldCallCancelConnection() {
        setupOneP2pPeer(WifiP2pDevice.INVITED);
        mFragment.mSelectedWifiPeer = mWifiP2pPeer;
        final Dialog dialog = mFragment.onCreateDialog(WifiP2pSettings.DIALOG_CANCEL_CONNECT);

        mFragment.mCancelConnectListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);

        verify(mWifiP2pManager, times(1)).cancelConnect(any(), any());
    }

    @Test
    public void wifiP2pManager_clickOnFailedPeer_shouldTryToConnect() {
        setupOneP2pPeer(WifiP2pDevice.FAILED);

        mFragment.onPreferenceTreeClick(mWifiP2pPeer);

        verify(mWifiP2pManager, times(1)).connect(any(), any(), any());
    }

    @Test
    public void withNoStage_discoveryChanged_shouldStopSearching() {
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);

        mFragment.mReceiver.onReceive(mContext, intent);

        assertThat(mFragment.mWifiP2pSearching).isFalse();
    }

    @Test
    public void withStartedStage_discoveryChanged_shouldStartSearching() {
        final Bundle bundle = new Bundle();
        bundle.putInt(WifiP2pManager.EXTRA_DISCOVERY_STATE,
                WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED);
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intent.putExtras(bundle);

        mFragment.mReceiver.onReceive(mContext, intent);

        assertThat(mFragment.mWifiP2pSearching).isTrue();
    }

    @Test
    public void clickPositiveButton_whenDeleteGroupDialogShow_shouldDeleteGroup() {
        final WifiP2pPersistentGroup wifiP2pPersistentGroup = new WifiP2pPersistentGroup(mContext,
                mWifiP2pGroup);
        mFragment.mSelectedGroup = wifiP2pPersistentGroup;
        final Dialog dialog = mFragment.onCreateDialog(WifiP2pSettings.DIALOG_DELETE_GROUP);

        mFragment.mDeleteGroupListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);

        assertThat(mFragment.mSelectedGroup).isNull();
    }

    @Test
    public void noLastGroupForm_whenP2pDisconnected_shouldStartSearch() {
        final NetworkInfo networkInfo = mock(NetworkInfo.class);
        doReturn(false).when(networkInfo).isConnected();
        final WifiP2pInfo wifiP2pInfo = mock(WifiP2pInfo.class);
        wifiP2pInfo.groupFormed = false;
        final Bundle bundle = new Bundle();
        bundle.putParcelable(WifiP2pManager.EXTRA_NETWORK_INFO, networkInfo);
        bundle.putParcelable(WifiP2pManager.EXTRA_WIFI_P2P_INFO, wifiP2pInfo);
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intent.putExtras(bundle);

        mFragment.mReceiver.onReceive(mContext, intent);

        verify(mWifiP2pManager, times(1)).discoverPeers(any(), any());
    }

    @Test
    public void withValidName_clickRenameDialog_shouldSetName() {
        final String fakeDeviceName = "fakeName";
        final Bundle bundle = new Bundle();
        bundle.putString(WifiP2pSettings.SAVE_DEVICE_NAME, fakeDeviceName);
        mFragment.onCreateView(LayoutInflater.from(mContext), null, bundle);
        final Dialog dialog = mFragment.onCreateDialog(WifiP2pSettings.DIALOG_RENAME);

        mFragment.mRenameListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);

        verify(mWifiP2pManager, times(1)).setDeviceName(any(), any(), any());
    }

    @Test
    public void withInValidName_whenGetRenameRequest_shouldNotSetName() {
        final String fakeDeviceName = "wrongName***";
        final Bundle bundle = new Bundle();
        bundle.putString(WifiP2pSettings.SAVE_DEVICE_NAME, fakeDeviceName);
        mFragment.onCreateView(LayoutInflater.from(mContext), null, bundle);

        final Dialog dialog = mFragment.onCreateDialog(WifiP2pSettings.DIALOG_RENAME);

        mFragment.mRenameListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);

        verify(mWifiP2pManager, times(0)).setDeviceName(any(), any(), any());
    }

    @Test
    public void pressDisconnectDialog_clickDisconnectDialog_shouldRemoveGroup() {
        setupOneP2pPeer(WifiP2pDevice.CONNECTED);
        mFragment.mSelectedWifiPeer = mWifiP2pPeer;
        final Dialog dialog = mFragment.onCreateDialog(WifiP2pSettings.DIALOG_DISCONNECT);

        mFragment.mDisconnectListener.onClick(dialog,
                DialogInterface.BUTTON_POSITIVE);

        verify(mWifiP2pManager, times(1)).removeGroup(any(), any());
    }

    @Test
    public void onCreateDialog_withUnknownId_shouldReturnNull() {
        assertThat(mFragment.onCreateDialog(-1 /* id */)).isNull();
    }

    @Test
    public void onStop_notLastGroupFormed_shouldCloseChannel() {
        mFragment.onStop();

        assertThat(mFragment.sChannel).isNull();
    }

    @Test
    public void peerDiscovery_whenOnStop_shouldStop() {
        mFragment.onStop();

        verify(mWifiP2pManager, times(1)).stopPeerDiscovery(any(), any());
    }

    @Test
    public void peerDiscovery_whenOnStart_shouldInitChannelAgain() {
        mFragment.onStop();

        verify(mWifiP2pManager, times(1)).stopPeerDiscovery(any(), any());

        mFragment.onStart();
        assertThat(mFragment.sChannel).isNotNull();
    }

    @Test
    public void whenGetSearchRequest_shouldStartToDiscoverPeer() {
        final MenuItem menuItem = mock(MenuItem.class);
        doReturn(mFragment.MENU_ID_SEARCH).when(menuItem).getItemId();

        mFragment.onOptionsItemSelected(menuItem);

        verify(mWifiP2pManager, times(1)).discoverPeers(any(), any());
    }

    @Test
    public void getMetrics_withDisconnectId_shouldReturnDisconnectMetricsCategory() {
        assertThat(mFragment.getDialogMetricsCategory(WifiP2pSettings.DIALOG_DISCONNECT)).isEqualTo(
                SettingsEnums.DIALOG_WIFI_P2P_DISCONNECT);
    }

    @Test
    public void getMetrics_withCancelConnectId_shouldReturnCancelConnectMetricsCategory() {
        assertThat(mFragment.getDialogMetricsCategory(
                WifiP2pSettings.DIALOG_CANCEL_CONNECT)).isEqualTo(
                SettingsEnums.DIALOG_WIFI_P2P_CANCEL_CONNECT);
    }

    @Test
    public void getMetrics_withRenameId_shouldReturnRenameMetricsCategory() {
        assertThat(mFragment.getDialogMetricsCategory(WifiP2pSettings.DIALOG_RENAME)).isEqualTo(
                SettingsEnums.DIALOG_WIFI_P2P_RENAME);
    }

    @Test
    public void getMetrics_withDeleteGroupId_shouldReturnDeleteGroupMetricsCategory() {
        assertThat(
                mFragment.getDialogMetricsCategory(WifiP2pSettings.DIALOG_DELETE_GROUP)).isEqualTo(
                SettingsEnums.DIALOG_WIFI_P2P_DELETE_GROUP);
    }

    @Test
    public void getMetrics_withUnknownId_shouldReturnZero() {
        assertThat(mFragment.getDialogMetricsCategory(-1 /* dialogId */)).isEqualTo(0);
    }

    @Test
    public void onSaveInstanceState_withWiFiPeer_shouldGetP2pDeviceType() {
        setupOneP2pPeer(WifiP2pDevice.CONNECTED);
        mFragment.onPreferenceTreeClick(mWifiP2pPeer);
        final Bundle outBundle = new Bundle();

        mFragment.onSaveInstanceState(outBundle);

        final Object object = outBundle.getParcelable(WifiP2pSettings.SAVE_DIALOG_PEER);
        assertThat(object instanceof WifiP2pDevice).isTrue();
    }

    @Test
    public void onSaveInstanceState_withDeviceNameText_shouldSaveName() {
        final String fakeDeviceName = "fakeName";
        final Bundle createBundle = new Bundle();
        createBundle.putString(WifiP2pSettings.SAVE_DEVICE_NAME, fakeDeviceName);
        mFragment.onCreateView(LayoutInflater.from(mContext), null, createBundle);
        final Bundle outBundle = new Bundle();
        final Dialog dialog = mFragment.onCreateDialog(WifiP2pSettings.DIALOG_RENAME);

        mFragment.onSaveInstanceState(outBundle);

        final String string = outBundle.getString(WifiP2pSettings.SAVE_DEVICE_NAME);
        assertThat(string).isEqualTo(fakeDeviceName);
    }

    @Test
    public void onSaveInstanceState_withSelectedGroup_shouldSaveGroupName() {
        final String fakeGroupName = "fakeGroupName";
        final WifiP2pPersistentGroup wifiP2pPersistentGroup = spy(
                new WifiP2pPersistentGroup(mContext,
                        mWifiP2pGroup));
        doReturn(fakeGroupName).when(wifiP2pPersistentGroup).getGroupName();
        mFragment.mSelectedGroup = wifiP2pPersistentGroup;
        final Bundle outBundle = new Bundle();

        mFragment.onSaveInstanceState(outBundle);

        assertThat(outBundle.getString(WifiP2pSettings.SAVE_SELECTED_GROUP)).isEqualTo(
                fakeGroupName);
    }

    @Test
    public void persistentController_withOneGroup_shouldBeAvailable() {
        final String fakeGroupName = new String("fakeGroupName");
        doReturn(fakeGroupName).when(mWifiP2pGroup).getNetworkName();
        final List<WifiP2pGroup> groupList = new ArrayList<>();
        groupList.add(mWifiP2pGroup);
        final WifiP2pGroupList wifiP2pGroupList = mock(WifiP2pGroupList.class);
        doReturn(groupList).when(wifiP2pGroupList).getGroupList();
        final Bundle bundle = new Bundle();
        bundle.putString(WifiP2pSettings.SAVE_SELECTED_GROUP, fakeGroupName);
        mFragment.onCreateView(LayoutInflater.from(mContext), null, bundle);

        mFragment.onPersistentGroupInfoAvailable(wifiP2pGroupList);

        assertThat(mFragment.mPersistentCategoryController.isAvailable()).isTrue();
    }

    @Test
    public void persistentController_withNoGroup_shouldBeUnavailable() {
        final WifiP2pGroupList wifiP2pGroupList = mock(WifiP2pGroupList.class);
        final List<WifiP2pGroup> groupList = new ArrayList<>();
        doReturn(groupList).when(wifiP2pGroupList).getGroupList();

        mFragment.onPersistentGroupInfoAvailable(wifiP2pGroupList);

        assertThat(mFragment.mPersistentCategoryController.isAvailable()).isFalse();
    }

    @Test
    public void peersCategoryController_withOnePeerDevice_shouldBeAvailable() {
        final WifiP2pDevice wifiP2pDevice = mock(WifiP2pDevice.class);
        final ArrayList<WifiP2pDevice> deviceList = new ArrayList<>();
        deviceList.add(wifiP2pDevice);
        final WifiP2pDeviceList peers = mock(WifiP2pDeviceList.class);
        doReturn(deviceList).when(peers).getDeviceList();

        mFragment.onPeersAvailable(peers);

        assertThat(mFragment.mPeerCategoryController.isAvailable()).isTrue();
    }

    @Test
    public void peersCategoryController_withNoPeerDevice_shouldBeUnavailable() {
        final ArrayList<WifiP2pDevice> deviceList = new ArrayList<>();
        final WifiP2pDeviceList peers = mock(WifiP2pDeviceList.class);
        doReturn(deviceList).when(peers).getDeviceList();

        mFragment.onPeersAvailable(peers);

        assertThat(mFragment.mPeerCategoryController.isAvailable()).isFalse();
    }

    @Test
    public void thisDeviceController_onDeviceInfoAvailable_shouldUpdateDeviceName() {
        final WifiP2pDevice wifiP2pDevice = mock(WifiP2pDevice.class);
        final P2pThisDevicePreferenceController thisDevicePreferenceController = mock(
                P2pThisDevicePreferenceController.class);
        mFragment.mThisDevicePreferenceController = thisDevicePreferenceController;

        mFragment.onDeviceInfoAvailable(wifiP2pDevice);

        verify(thisDevicePreferenceController, times(1)).updateDeviceName(any());
    }

    @Test
    public void p2pThisDeviceChange_shouldRequestDeviceInfoAgain() {
        final Intent intent = new Intent(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mFragment.mReceiver.onReceive(mContext, intent);

        verify(mWifiP2pManager, times(2)).requestDeviceInfo(any(), any());
    }

    @Test
    public void p2pPersistentGroupChange_shouldRequestGroupInfo() {
        final Intent intent = new Intent(WifiP2pManager.ACTION_WIFI_P2P_PERSISTENT_GROUPS_CHANGED);

        mFragment.mReceiver.onReceive(mContext, intent);

        verify(mWifiP2pManager, times(1)).requestPersistentGroupInfo(any(), any());
    }

    @Test
    public void onCreateView_withNullP2pManager_shouldGetP2pManagerAgain() {
        mFragment.sChannel = null; // Reset channel to re-test onCreateView flow
        mFragment.mWifiP2pManager = null;

        mFragment.onCreateView(LayoutInflater.from(mContext), null, new Bundle());

        assertThat(mFragment.mWifiP2pManager).isNotNull();
    }

    @Test
    public void onCreateView_withNullChannel_shouldSetP2pManagerNull() {
        doReturn(null).when(mWifiP2pManager).initialize(any(), any(), any());
        mFragment.sChannel = null; // Reset channel to re-test onCreateView flow
        mFragment.onCreateView(LayoutInflater.from(mContext), null, new Bundle());

        assertThat(mFragment.mWifiP2pManager).isNull();
    }

    @Test
    public void clickNegativeButton_whenDeleteGroupDialogShow_shouldSetGroupNull() {
        final WifiP2pPersistentGroup wifiP2pPersistentGroup = new WifiP2pPersistentGroup(mContext,
                mWifiP2pGroup);
        mFragment.mSelectedGroup = wifiP2pPersistentGroup;
        final Dialog dialog = mFragment.onCreateDialog(WifiP2pSettings.DIALOG_DELETE_GROUP);

        mFragment.mDeleteGroupListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);

        assertThat(mFragment.mSelectedGroup).isNull();
    }

    private void setupOneP2pPeer(int status) {
        final WifiP2pDevice wifiP2pDevice = mock(WifiP2pDevice.class);
        wifiP2pDevice.status = status;
        wifiP2pDevice.deviceAddress = "testAddress";
        wifiP2pDevice.deviceName = "testName";
        mWifiP2pPeer.device = wifiP2pDevice;
    }

    public static class TestWifiP2pSettings extends WifiP2pSettings {
        static WifiP2pManager sMockWifiP2pManager;
        @Override
        protected Object getSystemService(final String name) {
            if (Context.WIFI_P2P_SERVICE.equals(name)) return sMockWifiP2pManager;
            return getActivity().getSystemService(name);
        }
    }
}
