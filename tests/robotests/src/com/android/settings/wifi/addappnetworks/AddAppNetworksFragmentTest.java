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

package com.android.settings.wifi.addappnetworks;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.provider.Settings;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class AddAppNetworksFragmentTest {

    private static final String FAKE_APP_NAME = "fake_app_name";
    private static final String FAKE_NEW_WPA_SSID = "fake_new_wpa_ssid";
    private static final String FAKE_NEW_OPEN_SSID = "fake_new_open_ssid";
    private static final String FAKE_NEW_OPEN_SSID_WITH_QUOTE = "\"fake_new_open_ssid\"";
    private static final String FAKE_NEW_SAVED_WPA_SSID = "\"fake_new_wpa_ssid\"";
    private static final String KEY_SSID = "key_ssid";
    private static final String KEY_SECURITY = "key_security";
    private static final int SCANED_LEVEL0 = 0;
    private static final int SCANED_LEVEL4 = 4;

    private AddAppNetworksFragment mAddAppNetworksFragment;
    private List<WifiNetworkSuggestion> mFakedSpecifiedNetworksList;
    private List<WifiConfiguration> mFakeSavedNetworksList;
    private WifiNetworkSuggestion mNewWpaSuggestionEntry;
    private WifiNetworkSuggestion mNewOpenSuggestionEntry;
    private WifiConfiguration mSavedWpaConfigurationEntry;
    private Bundle mBundle;
    private ArrayList<Integer> mFakedResultArrayList = new ArrayList<>();

    @Mock
    private WifiEntry mWifiEntry;

    private FakeFeatureFactory mFakeFeatureFactory;

    @Mock
    private WifiPickerTracker mWifiPickerTracker;

    @Mock
    private WifiManager mWifiManager;

    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAddAppNetworksFragment = spy(new AddAppNetworksFragment());
        mActivity = spy(Robolectric.setupActivity(FragmentActivity.class));
        doReturn(mActivity).when(mAddAppNetworksFragment).getActivity();
        when(mWifiManager.isWifiEnabled()).thenReturn(true);
        when(mActivity.getSystemService(WifiManager.class)).thenReturn(mWifiManager);

        mNewWpaSuggestionEntry = generateRegularWifiSuggestion(FAKE_NEW_WPA_SSID,
                WifiConfiguration.KeyMgmt.WPA_PSK, "1234567890");
        mNewOpenSuggestionEntry = generateRegularWifiSuggestion(FAKE_NEW_OPEN_SSID,
                WifiConfiguration.KeyMgmt.NONE, null);
        mSavedWpaConfigurationEntry = generateRegularWifiConfiguration(FAKE_NEW_SAVED_WPA_SSID,
                WifiConfiguration.KeyMgmt.WPA_PSK, "\"1234567890\"");

        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();

        mAddAppNetworksFragment.mWifiPickerTracker = mWifiPickerTracker;
        setUpOneScannedNetworkWithScanedLevel4();
    }

    @Ignore
    @Test
    public void callingPackageName_onCreateView_shouldBeCorrect() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        assertThat(mAddAppNetworksFragment.mCallingPackageName).isEqualTo(FAKE_APP_NAME);
    }

    @Ignore
    @Test
    public void launchFragment_shouldShowSaveButton() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        assertThat(mAddAppNetworksFragment.mSaveButton).isNotNull();
    }

    @Ignore
    @Test
    public void launchFragment_shouldShowCancelButton() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        assertThat(mAddAppNetworksFragment.mCancelButton).isNotNull();
    }

    @Ignore
    @Test
    public void requestOneNetwork_shouldShowCorrectSSID() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();
        TextView ssidView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.single_ssid);

        assertThat(ssidView.getText()).isEqualTo(FAKE_NEW_WPA_SSID);
    }

    @Ignore
    @Test
    public void withNoExtra_requestNetwork_shouldFinished() {
        addOneSpecifiedRegularNetworkSuggestion(null);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        assertThat(mAddAppNetworksFragment.mActivity.isFinishing()).isTrue();
    }

    @Test
    public void withOneHalfSavedNetworks_uiListAndResultListShouldBeCorrect() {
        // Arrange
        // Setup a fake saved network list and assign to fragment.
        addOneSavedNetworkConfiguration(mSavedWpaConfigurationEntry);
        // Setup two specified networks and their results and assign to fragment.
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        mAddAppNetworksFragment.mAllSpecifiedNetworksList = mFakedSpecifiedNetworksList;
        mFakedResultArrayList.add(mAddAppNetworksFragment.RESULT_NETWORK_SUCCESS);
        mFakedResultArrayList.add(mAddAppNetworksFragment.RESULT_NETWORK_SUCCESS);
        mAddAppNetworksFragment.mResultCodeArrayList = mFakedResultArrayList;

        // Act
        mAddAppNetworksFragment.filterSavedNetworks(mFakeSavedNetworksList);

        // Assert
        assertThat(mAddAppNetworksFragment.mUiToRequestedList).hasSize(1);
        assertThat(mAddAppNetworksFragment.mResultCodeArrayList.get(0)).isEqualTo(
                mAddAppNetworksFragment.RESULT_NETWORK_ALREADY_EXISTS);
        assertThat(mAddAppNetworksFragment.mUiToRequestedList.get(
                0).mWifiNetworkSuggestion.getWifiConfiguration().SSID).isEqualTo(
                FAKE_NEW_OPEN_SSID_WITH_QUOTE);
    }

    @Test
    public void getMetricsCategory_shouldReturnPanelAddWifiNetworks() {
        assertThat(mAddAppNetworksFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.PANEL_ADD_WIFI_NETWORKS);
    }

    @Ignore
    @Test
    public void getThreeNetworksNewIntent_shouldHaveThreeItemsInUiList() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        // Add two more networks and update framework bundle.
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        Bundle bundle = mAddAppNetworksFragment.getArguments();
        mAddAppNetworksFragment.createContent(bundle);

        // Ui list should contain 3 networks.
        assertThat(mAddAppNetworksFragment.mUiToRequestedList).hasSize(3);
    }

    @Test
    public void withOneSuggestion_uiElementShouldHaveInitLevel() {
        // Arrange
        // Setup a fake saved network list and assign to fragment.
        addOneSavedNetworkConfiguration(mSavedWpaConfigurationEntry);
        // Setup one specified networks and its results and assign to fragment.
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        mAddAppNetworksFragment.mAllSpecifiedNetworksList = mFakedSpecifiedNetworksList;

        // Act
        mAddAppNetworksFragment.filterSavedNetworks(mFakeSavedNetworksList);

        // Assert
        assertThat(mAddAppNetworksFragment.mUiToRequestedList).hasSize(1);
        assertThat(mAddAppNetworksFragment.mUiToRequestedList.get(0).mLevel).isEqualTo(
                mAddAppNetworksFragment.INITIAL_RSSI_SIGNAL_LEVEL);
    }

    @Test
    public void withOneSuggestion_whenScanResultChanged_uiListShouldHaveNewLevel() {
        // Arrange
        when(mWifiPickerTracker.getWifiState()).thenReturn(WifiManager.WIFI_STATE_ENABLED);
        // Setup a fake saved network list and assign to fragment.
        addOneSavedNetworkConfiguration(mSavedWpaConfigurationEntry);
        // Setup one specified networks and its results and assign to fragment.
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        mAddAppNetworksFragment.mAllSpecifiedNetworksList = mFakedSpecifiedNetworksList;
        // Call filterSavedNetworks to generate necessary objects.
        mAddAppNetworksFragment.filterSavedNetworks(mFakeSavedNetworksList);

        // Act
        mAddAppNetworksFragment.onWifiEntriesChanged();

        // Assert
        assertThat(mAddAppNetworksFragment.mUiToRequestedList.get(0).mLevel).isEqualTo(
                SCANED_LEVEL4);
    }

    @Test
    public void withOneSuggestion_whenScanResultChangedButWifiOff_uiListShouldHaveZeroLevel() {
        // Arrange
        when(mWifiPickerTracker.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);
        // Setup a fake saved network list and assign to fragment.
        addOneSavedNetworkConfiguration(mSavedWpaConfigurationEntry);
        // Setup one specified networks and its results and assign to fragment.
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        mAddAppNetworksFragment.mAllSpecifiedNetworksList = mFakedSpecifiedNetworksList;
        // Call filterSavedNetworks to generate necessary objects.
        mAddAppNetworksFragment.filterSavedNetworks(mFakeSavedNetworksList);

        // Act
        mAddAppNetworksFragment.onWifiEntriesChanged();

        // Assert
        assertThat(mAddAppNetworksFragment.mUiToRequestedList.get(0).mLevel).isEqualTo(
                SCANED_LEVEL0);
    }

    @Ignore
    @Test
    public void onDestroy_quitWorkerThread() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();
        mAddAppNetworksFragment.mWorkerThread = mock(HandlerThread.class);

        try {
            mAddAppNetworksFragment.onDestroy();
        } catch (IllegalArgumentException e) {
            // Ignore the exception from super class.
        }

        verify(mAddAppNetworksFragment.mWorkerThread).quit();
    }

    @Ignore
    @Test
    public void status_withOneNetworkSave_shouldShowOneNetworkSaving() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        mAddAppNetworksFragment.showSaveStatusByState(
                AddAppNetworksFragment.MESSAGE_START_SAVING_NETWORK);

        final TextView textView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.single_status);
        assertThat(textView.getText()).isEqualTo(mAddAppNetworksFragment.getString(
                R.string.wifi_add_app_single_network_saving_summary));
    }

    @Ignore
    @Test
    public void status_withTwoNetworksSave_shouldShowMultipleNetworksSaving() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        mAddAppNetworksFragment.showSaveStatusByState(
                AddAppNetworksFragment.MESSAGE_START_SAVING_NETWORK);

        final TextView textView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.app_summary);
        assertThat(textView.getText()).isEqualTo(
                mAddAppNetworksFragment.getString(R.string.wifi_add_app_networks_saving_summary,
                        2));
    }

    @Ignore
    @Test
    public void status_withOneNetworkSaved_shouldShowOneNetworkSaved() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        mAddAppNetworksFragment.showSaveStatusByState(
                AddAppNetworksFragment.MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK);

        final TextView textView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.single_status);
        assertThat(textView.getText()).isEqualTo(mAddAppNetworksFragment.getString(
                R.string.wifi_add_app_single_network_saved_summary));
    }

    @Ignore
    @Test
    public void status_withTwoNetworksSaved_shouldShowMultipleNetworksSaved() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        mAddAppNetworksFragment.showSaveStatusByState(
                AddAppNetworksFragment.MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK);

        final TextView textView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.app_summary);
        assertThat(textView.getText()).isEqualTo(
                mAddAppNetworksFragment.getString(R.string.wifi_add_app_networks_saved_summary));
    }

    @Ignore
    @Test
    public void status_withOneNetworkSaveFailed_shouldShowOneNetworkFailed() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        mAddAppNetworksFragment.showSaveStatusByState(
                AddAppNetworksFragment.MESSAGE_SHOW_SAVE_FAILED);

        final TextView textView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.single_status);
        assertThat(textView.getText()).isEqualTo(mAddAppNetworksFragment.getString(
                R.string.wifi_add_app_network_save_failed_summary));
    }

    @Ignore
    @Test
    public void status_withTwoNetworksSaveFailed_shouldShowMultipleNetworksFailed() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        mAddAppNetworksFragment.showSaveStatusByState(
                AddAppNetworksFragment.MESSAGE_SHOW_SAVE_FAILED);

        final TextView textView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.app_summary);
        assertThat(textView.getText()).isEqualTo(mAddAppNetworksFragment.getString(
                R.string.wifi_add_app_network_save_failed_summary));
    }

    @Ignore
    @Test
    public void saveOneNetwork_shouldCallWifiManagerSaveOnce() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        mAddAppNetworksFragment.saveNetwork(0 /* index */);

        verify(mWifiManager, times(1)).save(any(), any());
    }

    @Ignore
    @Test
    public void onSuccess_saveTwoNetworks_shouldCallWifiNamagerSaveTwice() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();
        mAddAppNetworksFragment.saveNetwork(0 /* index */);

        final ArgumentCaptor<WifiManager.ActionListener> wifiCallbackCaptor =
                ArgumentCaptor.forClass(WifiManager.ActionListener.class);
        verify(mWifiManager).save(any(WifiConfiguration.class), wifiCallbackCaptor.capture());
        wifiCallbackCaptor.getValue().onSuccess();

        verify(mWifiManager, times(2)).save(any(), any());
    }

    @Ignore
    @Test
    public void onFailedFirstOne_saveTwoNetworks_shouldAlsoCallWifiNamagerSaveTwice() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        addOneSpecifiedRegularNetworkSuggestion(mNewOpenSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();
        mAddAppNetworksFragment.saveNetwork(0 /* index */);

        final ArgumentCaptor<WifiManager.ActionListener> wifiCallbackCaptor =
                ArgumentCaptor.forClass(WifiManager.ActionListener.class);
        verify(mWifiManager).save(any(WifiConfiguration.class), wifiCallbackCaptor.capture());
        wifiCallbackCaptor.getValue().onFailure(anyInt());

        verify(mWifiManager, times(2)).save(any(), any());
    }

    @Ignore
    @Test
    public void saveSuccess_shouldEnterShowAndConnectState() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();
        mAddAppNetworksFragment.saveNetwork(0 /* index */);

        final ArgumentCaptor<WifiManager.ActionListener> wifiCallbackCaptor =
                ArgumentCaptor.forClass(WifiManager.ActionListener.class);
        verify(mWifiManager).save(any(WifiConfiguration.class), wifiCallbackCaptor.capture());
        wifiCallbackCaptor.getValue().onSuccess();

        assertThat(mAddAppNetworksFragment.mHandler.hasMessages(
                AddAppNetworksFragment.MESSAGE_SHOW_SAVED_AND_CONNECT_NETWORK)).isTrue();
    }

    @Ignore
    @Test
    public void saveFailed_shouldEnterFailedState() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();
        mAddAppNetworksFragment.saveNetwork(0 /* index */);

        final ArgumentCaptor<WifiManager.ActionListener> wifiCallbackCaptor =
                ArgumentCaptor.forClass(
                        WifiManager.ActionListener.class);
        verify(mWifiManager).save(any(WifiConfiguration.class), wifiCallbackCaptor.capture());
        wifiCallbackCaptor.getValue().onFailure(1 /* reason */);

        assertThat(mAddAppNetworksFragment.mHandler.hasMessages(
                AddAppNetworksFragment.MESSAGE_SHOW_SAVE_FAILED)).isTrue();
    }

    @Test
    public void uiConfigurationItem_putCrToDisplayedSsid_shouldRemoveCr() {
        String testSsid = "\r" + FAKE_NEW_WPA_SSID + "\r";

        AddAppNetworksFragment.UiConfigurationItem item =
                new AddAppNetworksFragment.UiConfigurationItem(testSsid, null, 0, 0);

        assertThat(item.mDisplayedSsid).isEqualTo(FAKE_NEW_WPA_SSID);
    }

    @Test
    public void uiConfigurationItem_putLfToDisplayedSsid_shouldRemoveLf() {
        String testSsid = "\n" + FAKE_NEW_WPA_SSID + "\n";

        AddAppNetworksFragment.UiConfigurationItem item =
                new AddAppNetworksFragment.UiConfigurationItem(testSsid, null, 0, 0);

        assertThat(item.mDisplayedSsid).isEqualTo(FAKE_NEW_WPA_SSID);
    }

    @Test
    public void uiConfigurationItem_putCrLfToDisplayedSsid_shouldRemoveCrLf() {
        String testSsid = "\r\n" + FAKE_NEW_WPA_SSID + "\r\n";

        AddAppNetworksFragment.UiConfigurationItem item =
                new AddAppNetworksFragment.UiConfigurationItem(testSsid, null, 0, 0);

        assertThat(item.mDisplayedSsid).isEqualTo(FAKE_NEW_WPA_SSID);
    }

    @Test
    public void getWifiIconResource_wifiLevelIsUnreachable_shouldNotCrash() {
        AddAppNetworksFragment.getWifiIconResource(WifiEntry.WIFI_LEVEL_UNREACHABLE);
    }

    private void setUpOneScannedNetworkWithScanedLevel4() {
        final ArrayList list = new ArrayList<>();
        list.add(mWifiEntry);
        when(mWifiPickerTracker.getWifiEntries()).thenReturn(list);
        when(mWifiEntry.getSsid()).thenReturn(FAKE_NEW_OPEN_SSID);
        when(mWifiEntry.getLevel()).thenReturn(SCANED_LEVEL4);
    }

    private void addOneSavedNetworkConfiguration(@NonNull WifiConfiguration wifiConfiguration) {
        if (mFakeSavedNetworksList == null) {
            mFakeSavedNetworksList = new ArrayList<>();
        }

        mFakeSavedNetworksList.add(wifiConfiguration);
    }

    private void addOneSpecifiedRegularNetworkSuggestion(
            @NonNull WifiNetworkSuggestion wifiNetworkSuggestion) {
        if (wifiNetworkSuggestion != null) {
            if (mFakedSpecifiedNetworksList == null) {
                mFakedSpecifiedNetworksList = new ArrayList<>();
            }
            mFakedSpecifiedNetworksList.add(wifiNetworkSuggestion);
        }
    }

    private void setUpBundle(List<WifiNetworkSuggestion> allFakedNetworksList) {
        // Set up bundle.
        final Bundle bundle = new Bundle();
        bundle.putString(AddAppNetworksActivity.KEY_CALLING_PACKAGE_NAME, FAKE_APP_NAME);
        bundle.putParcelableArrayList(Settings.EXTRA_WIFI_NETWORK_LIST,
                (ArrayList<? extends Parcelable>) allFakedNetworksList);
        doReturn(bundle).when(mAddAppNetworksFragment).getArguments();
    }

    private void setupFragment() {
        when(mFakeFeatureFactory.wifiTrackerLibProvider.createWifiPickerTracker(
                any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(mWifiPickerTracker);
        FragmentController.setupFragment(mAddAppNetworksFragment);
    }

    private static WifiConfiguration generateRegularWifiConfiguration(String ssid, int
            securityType,
            String password) {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = ssid;
        config.allowedKeyManagement.set(securityType);

        if (password != null) {
            config.preSharedKey = password;
        }
        return config;
    }

    private static WifiNetworkSuggestion generateRegularWifiSuggestion(String ssid,
            int securityType,
            String password) {
        WifiNetworkSuggestion suggestion = null;

        switch (securityType) {
            case WifiConfiguration.KeyMgmt.NONE:
                suggestion = new WifiNetworkSuggestion.Builder()
                        .setSsid(ssid)
                        .build();
                break;
            case WifiConfiguration.KeyMgmt.WPA_PSK:
                suggestion = new WifiNetworkSuggestion.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(password)
                        .build();
                break;
            default:
                break;

        }

        return suggestion;
    }
}
