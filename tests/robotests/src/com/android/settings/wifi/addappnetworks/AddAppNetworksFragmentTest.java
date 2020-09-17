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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

import com.android.settings.R;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
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
    private WifiEntry mMockWifiEntry;

    @Mock
    private WifiPickerTracker mMockWifiPickerTracker;

    @Mock
    private WifiManager mMockWifiManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAddAppNetworksFragment = spy(new AddAppNetworksFragment());
        mNewWpaSuggestionEntry = generateRegularWifiSuggestion(FAKE_NEW_WPA_SSID,
                WifiConfiguration.KeyMgmt.WPA_PSK, "1234567890");
        mNewOpenSuggestionEntry = generateRegularWifiSuggestion(FAKE_NEW_OPEN_SSID,
                WifiConfiguration.KeyMgmt.NONE, null);
        mSavedWpaConfigurationEntry = generateRegularWifiConfiguration(FAKE_NEW_SAVED_WPA_SSID,
                WifiConfiguration.KeyMgmt.WPA_PSK, "\"1234567890\"");
        when(mMockWifiManager.isWifiEnabled()).thenReturn(true);
        mAddAppNetworksFragment.mWifiPickerTracker = mMockWifiPickerTracker;
        setUpOneScannedNetworkWithScanedLevel4();
    }

    @Test
    public void callingPackageName_onCreateView_shouldBeCorrect() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        assertThat(mAddAppNetworksFragment.mCallingPackageName).isEqualTo(FAKE_APP_NAME);
    }

    @Test
    public void launchFragment_shouldShowSaveButton() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        assertThat(mAddAppNetworksFragment.mSaveButton).isNotNull();
    }

    @Test
    public void launchFragment_shouldShowCancelButton() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();

        assertThat(mAddAppNetworksFragment.mCancelButton).isNotNull();
    }

    @Test
    public void requestOneNetwork_shouldShowCorrectSSID() {
        addOneSpecifiedRegularNetworkSuggestion(mNewWpaSuggestionEntry);
        setUpBundle(mFakedSpecifiedNetworksList);
        setupFragment();
        TextView ssidView = (TextView) mAddAppNetworksFragment.mLayoutView.findViewById(
                R.id.single_ssid);

        assertThat(ssidView.getText()).isEqualTo(FAKE_NEW_WPA_SSID);
    }

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
        when(mMockWifiPickerTracker.getWifiState()).thenReturn(WifiManager.WIFI_STATE_ENABLED);
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
        when(mMockWifiPickerTracker.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);
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

    @Test
    public void onDestroy_quitWorkerThread() {
        mAddAppNetworksFragment.mWorkerThread = mock(HandlerThread.class);

        try {
            mAddAppNetworksFragment.onDestroy();
        } catch (IllegalArgumentException e) {
            // Ignore the exception from super class.
        }

        verify(mAddAppNetworksFragment.mWorkerThread).quit();
    }

    private void setUpOneScannedNetworkWithScanedLevel4() {
        final ArrayList list = new ArrayList<>();
        list.add(mMockWifiEntry);
        when(mMockWifiPickerTracker.getWifiEntries()).thenReturn(list);
        when(mMockWifiEntry.getSsid()).thenReturn(FAKE_NEW_OPEN_SSID);
        when(mMockWifiEntry.getLevel()).thenReturn(SCANED_LEVEL4);
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
