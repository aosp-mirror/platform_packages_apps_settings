/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.wifitrackerlib.WifiEntry.SECURITY_PSK;
import static com.android.wifitrackerlib.WifiEntry.SECURITY_SAE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class NetworkRequestDialogFragmentTest {

    private static final String KEY_SSID = "key_ssid";
    private static final String TEST_CAPABILITIES_OPEN = "[ESS]";
    private static final String TEST_CAPABILITIES_WPA2_PSK = "[WPA2-PSK-CCMP][ESS]";
    private static final String TEST_CAPABILITIES_WPA3_SAE = "[RSN-PSK+SAE-CCMP][ESS]";
    private static final String TEST_APP_NAME = "TestAppName";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    WifiPickerTracker mWifiPickerTracker;
    @Mock
    WifiEntry mWifiEntry;

    private FragmentActivity mActivity;
    private NetworkRequestDialogFragment networkRequestDialogFragment;

    ScanResult mScanResult = new ScanResult();

    @Before
    public void setUp() {
        when(mWifiEntry.getSsid()).thenReturn(KEY_SSID);
        when(mWifiEntry.getSecurityTypes()).thenReturn(Arrays.asList(SECURITY_PSK, SECURITY_SAE));
        when(mWifiEntry.getSecurity()).thenReturn(SECURITY_PSK);
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(null);
        when(mWifiPickerTracker.getWifiEntries()).thenReturn(Arrays.asList(mWifiEntry));

        mScanResult.SSID = KEY_SSID;
        mScanResult.capabilities = TEST_CAPABILITIES_OPEN;

        FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        when(fakeFeatureFactory.wifiTrackerLibProvider.createWifiPickerTracker(
                any(), any(), any(), any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(mock(WifiPickerTracker.class));

        mActivity = Robolectric.buildActivity(FragmentActivity.class,
                new Intent().putExtra(NetworkRequestDialogFragment.EXTRA_APP_NAME,
                        TEST_APP_NAME)).setup().get();
        networkRequestDialogFragment = spy(NetworkRequestDialogFragment.newInstance());
        networkRequestDialogFragment.mWifiPickerTracker = mWifiPickerTracker;
    }

    @Ignore
    @Test
    public void display_shouldShowTheDialog() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), null);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();
    }

    @Ignore
    @Test
    public void display_shouldShowTitleWithAppName() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), /* tag */ null);
        final AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        final String targetTitle = RuntimeEnvironment.application.getString(
                R.string.network_connection_request_dialog_title, TEST_APP_NAME);
        final TextView view = alertDialog.findViewById(R.id.network_request_title_text);
        assertThat(view.getText()).isEqualTo(targetTitle);
    }

    @Ignore
    @Test
    public void clickNegativeButton_shouldCloseTheDialog() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), null);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(positiveButton).isNotNull();

        positiveButton.performClick();
        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void onClick_validSelection_shouldCallSelect() {
        final int indexClickItem = 3;
        final List<WifiEntry> wifiEntryList = createWifiEntryList();
        final WifiEntry clickedWifiEntry = wifiEntryList.get(indexClickItem);
        final WifiConfiguration wifiConfig = new WifiConfiguration();
        when(clickedWifiEntry.getWifiConfiguration()).thenReturn(wifiConfig);
        networkRequestDialogFragment.mFilteredWifiEntries = wifiEntryList;
        final NetworkRequestUserSelectionCallback selectionCallback = mock(
                NetworkRequestUserSelectionCallback.class);
        final AlertDialog dialog = mock(AlertDialog.class);
        networkRequestDialogFragment.onUserSelectionCallbackRegistration(selectionCallback);

        networkRequestDialogFragment.onClick(dialog, indexClickItem);

        verify(selectionCallback, times(1)).select(wifiConfig);
    }

    @Test
    public void onMatch_shouldUpdateWifiEntries() {
        final InOrder inOrder = inOrder(networkRequestDialogFragment);

        networkRequestDialogFragment.onMatch(new ArrayList<ScanResult>());

        inOrder.verify(networkRequestDialogFragment).updateWifiEntries();
        inOrder.verify(networkRequestDialogFragment).updateUi();
    }

    @Test
    public void onWifiStateChanged_nonEmptyMatchedScanResults_shouldUpdateWifiEntries() {
        final InOrder inOrder = inOrder(networkRequestDialogFragment);
        mScanResult.capabilities = TEST_CAPABILITIES_OPEN;
        networkRequestDialogFragment.onMatch(Arrays.asList(mScanResult));

        networkRequestDialogFragment.onWifiStateChanged();

        inOrder.verify(networkRequestDialogFragment).updateWifiEntries();
        inOrder.verify(networkRequestDialogFragment).updateUi();
    }

    @Test
    public void onWifiEntriesChanged_nonEmptyMatchedScanResults_shouldUpdateWifiEntries() {
        final InOrder inOrder = inOrder(networkRequestDialogFragment);
        mScanResult.capabilities = TEST_CAPABILITIES_OPEN;
        networkRequestDialogFragment.onMatch(Arrays.asList(mScanResult));

        networkRequestDialogFragment.onWifiEntriesChanged();

        inOrder.verify(networkRequestDialogFragment).updateWifiEntries();
        inOrder.verify(networkRequestDialogFragment).updateUi();
    }

    private List<WifiEntry> createWifiEntryList() {
        List<WifiEntry> wifiEntryList = new ArrayList<>();

        final WifiEntry wifiEntry1 = mock(WifiEntry.class);
        when(wifiEntry1.getSsid()).thenReturn("Test AP 1");
        when(wifiEntry1.getSecurity()).thenReturn(WifiEntry.SECURITY_WEP);
        wifiEntryList.add(wifiEntry1);

        final WifiEntry wifiEntry2 = mock(WifiEntry.class);
        when(wifiEntry2.getSsid()).thenReturn("Test AP 2");
        when(wifiEntry2.getSecurity()).thenReturn(WifiEntry.SECURITY_WEP);
        wifiEntryList.add(wifiEntry2);

        final WifiEntry wifiEntry3 = mock(WifiEntry.class);
        when(wifiEntry3.getSsid()).thenReturn("Test AP 3");
        when(wifiEntry3.getSecurity()).thenReturn(WifiEntry.SECURITY_WEP);
        wifiEntryList.add(wifiEntry3);

        final WifiEntry wifiEntry4 = mock(WifiEntry.class);
        when(wifiEntry4.getSsid()).thenReturn("Test AP 4");
        when(wifiEntry4.getSecurity()).thenReturn(WifiEntry.SECURITY_NONE);
        wifiEntryList.add(wifiEntry4);

        final WifiEntry wifiEntry5 = mock(WifiEntry.class);
        when(wifiEntry5.getSsid()).thenReturn("Test AP 5");
        when(wifiEntry5.getSecurity()).thenReturn(WifiEntry.SECURITY_WEP);
        wifiEntryList.add(wifiEntry5);

        final WifiEntry wifiEntry6 = mock(WifiEntry.class);
        when(wifiEntry6.getSsid()).thenReturn("Test AP 6");
        when(wifiEntry6.getSecurity()).thenReturn(WifiEntry.SECURITY_WEP);
        wifiEntryList.add(wifiEntry6);

        return wifiEntryList;
    }

    @Ignore
    @Test
    public void display_shouldNotShowNeutralButton() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), /* tag */ null);
        final AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        final Button button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.GONE);
    }

    @Ignore
    @Test
    public void onMatchManyResult_showNeutralButton() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), /* tag */ null);
        final AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        List<WifiEntry> wifiEntryList = createWifiEntryList();
        final WifiPickerTracker wifiPickerTracker = mock(WifiPickerTracker.class);
        when(wifiPickerTracker.getWifiEntries()).thenReturn(wifiEntryList);
        networkRequestDialogFragment.mWifiPickerTracker = wifiPickerTracker;

        final String ssidAp = "Test AP ";
        final List<ScanResult> scanResults = new ArrayList<>();
        for (int i = 0; i < 7 ; i ++) {
            ScanResult scanResult = mock(ScanResult.class);
            scanResult.SSID = ssidAp + i;
            scanResult.capabilities = "WEP";
            scanResults.add(scanResult);
        }
        networkRequestDialogFragment.onMatch(scanResults);

        final Button button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        assertThat(button).isNotNull();
        assertThat(button.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Ignore
    @Test
    public void clickNeutralButton_hideNeutralButton() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), /* tag */ null);
        final AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        final String ssidAp = "Test AP ";
        final List<ScanResult> scanResults = new ArrayList<>();
        for (int i = 0; i < 6 ; i ++) {
            ScanResult scanResult = mock(ScanResult.class);
            scanResult.SSID = ssidAp + i;
            scanResult.capabilities = "WEP";
            scanResults.add(scanResult);
        }
        networkRequestDialogFragment.onMatch(scanResults);

        // Action
        final Button button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        button.performClick();

        // Check
        assertThat(button.getVisibility()).isEqualTo(View.GONE);
    }

    @Ignore
    @Test
    public void cancelDialog_callsReject() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), /* tag */ null);
        final AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        final NetworkRequestUserSelectionCallback selectionCallback = mock(
                NetworkRequestUserSelectionCallback.class);
        networkRequestDialogFragment.onUserSelectionCallbackRegistration(selectionCallback);

        // Action
        final Button button = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        button.performClick();

        // Check
        verify(selectionCallback, times(1)).reject();
    }

    @Test
    public void updateWifiEntries_noMatchSecurityWifi_filteredWifiIsEmpty() {
        mScanResult.capabilities = TEST_CAPABILITIES_OPEN;
        networkRequestDialogFragment.onMatch(Arrays.asList(mScanResult));

        networkRequestDialogFragment.updateWifiEntries();

        assertThat(networkRequestDialogFragment.mFilteredWifiEntries.size()).isEqualTo(0);
    }

    @Test
    public void updateWifiEntries_matchWpa2Wifi_filteredWifiNotEmpty() {
        mScanResult.capabilities = TEST_CAPABILITIES_WPA2_PSK;
        networkRequestDialogFragment.onMatch(Arrays.asList(mScanResult));

        networkRequestDialogFragment.updateWifiEntries();

        assertThat(networkRequestDialogFragment.mFilteredWifiEntries.size()).isNotEqualTo(0);
    }

    @Test
    public void updateWifiEntries_matchWpa3Wifi_filteredWifiNotEmpty() {
        mScanResult.capabilities = TEST_CAPABILITIES_WPA3_SAE;
        networkRequestDialogFragment.onMatch(Arrays.asList(mScanResult));

        networkRequestDialogFragment.updateWifiEntries();

        assertThat(networkRequestDialogFragment.mFilteredWifiEntries.size()).isNotEqualTo(0);
    }
}
