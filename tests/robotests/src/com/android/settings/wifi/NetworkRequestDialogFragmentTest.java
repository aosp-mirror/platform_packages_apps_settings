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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.wifi.NetworkRequestErrorDialogFragment.ERROR_DIALOG_TYPE;
import com.android.settingslib.wifi.AccessPoint;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class NetworkRequestDialogFragmentTest {

    private static final String KEY_SSID = "key_ssid";
    private static final String KEY_SECURITY = "key_security";

    private FragmentActivity mActivity;
    private NetworkRequestDialogFragment networkRequestDialogFragment;
    private Context mContext;
    private WifiTracker mWifiTracker;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        networkRequestDialogFragment = spy(NetworkRequestDialogFragment.newInstance());
        mContext = spy(RuntimeEnvironment.application);

        mWifiTracker = mock(WifiTracker.class);
        WifiTrackerFactory.setTestingWifiTracker(mWifiTracker);
    }

    @Test
    public void display_shouldShowTheDialog() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), null);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();
    }

    @Test
    public void clickPositiveButton_shouldCloseTheDialog() {
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), null);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertThat(positiveButton).isNotNull();

        positiveButton.performClick();
        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void onResumeAndWaitTimeout_shouldCallTimeoutDialog() {
        FakeNetworkRequestDialogFragment fakeFragment = new FakeNetworkRequestDialogFragment();
        FakeNetworkRequestDialogFragment spyFakeFragment = spy(fakeFragment);
        spyFakeFragment.show(mActivity.getSupportFragmentManager(), null);

        assertThat(fakeFragment.bCalledStopAndPop).isFalse();

        ShadowLooper.getShadowMainLooper().runToEndOfTasks();

        assertThat(fakeFragment.bCalledStopAndPop).isTrue();
    }

    class FakeNetworkRequestDialogFragment extends NetworkRequestDialogFragment {
        boolean bCalledStopAndPop = false;

        @Override
        public void stopScanningAndPopErrorDialog(ERROR_DIALOG_TYPE type) {
            bCalledStopAndPop = true;
        }
    }

    @Test
    public void onResume_shouldRegisterCallback() {
        when(networkRequestDialogFragment.getContext()).thenReturn(mContext);
        Context applicationContext = spy(RuntimeEnvironment.application.getApplicationContext());
        when(mContext.getApplicationContext()).thenReturn(applicationContext);
        WifiManager wifiManager = mock(WifiManager.class);
        when(applicationContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);

        networkRequestDialogFragment.onResume();

        verify(wifiManager).registerNetworkRequestMatchCallback(any(), any());
    }

    @Test
    public void onPause_shouldUnRegisterCallback() {
        when(networkRequestDialogFragment.getContext()).thenReturn(mContext);
        Context applicationContext = spy(RuntimeEnvironment.application.getApplicationContext());
        when(mContext.getApplicationContext()).thenReturn(applicationContext);
        WifiManager wifiManager = mock(WifiManager.class);
        when(applicationContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);

        networkRequestDialogFragment.onPause();

        verify(wifiManager).unregisterNetworkRequestMatchCallback(networkRequestDialogFragment);
    }

    @Test
    public void updateAccessPointList_onUserSelectionConnectSuccess_shouldCloseTheDialog() {
        List<AccessPoint> accessPointList = createAccessPointList();
        when(networkRequestDialogFragment.getAccessPointList()).thenReturn(accessPointList);
        networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), null);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        // Test if config would update list.
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "Test AP 3";
        networkRequestDialogFragment.onUserSelectionConnectSuccess(config);

        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void updateAccessPointList_onUserSelectionConnectFailure_shouldCallTimeoutDialog() {
        FakeNetworkRequestDialogFragment fakeFragment = new FakeNetworkRequestDialogFragment();
        FakeNetworkRequestDialogFragment spyFakeFragment = spy(fakeFragment);
        List<AccessPoint> accessPointList = createAccessPointList();
        when(spyFakeFragment.getAccessPointList()).thenReturn(accessPointList);
        spyFakeFragment.show(mActivity.getSupportFragmentManager(), null);

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();

        // Test if config would update list.
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "Test AP 3";
        fakeFragment.onUserSelectionConnectFailure(config);

        assertThat(fakeFragment.bCalledStopAndPop).isTrue();
    }

    @Test
    public void onUserSelectionCallbackRegistration_onClick_shouldCallSelect() {
        // Assert.
        final int indexClickItem = 3;
        List<AccessPoint> accessPointList = createAccessPointList();
        AccessPoint clickedAccessPoint = accessPointList.get(indexClickItem);
        clickedAccessPoint.generateOpenNetworkConfig();
        when(networkRequestDialogFragment.getAccessPointList()).thenReturn(accessPointList);

        NetworkRequestUserSelectionCallback selectionCallback = mock(
                NetworkRequestUserSelectionCallback.class);
        AlertDialog dialog = mock(AlertDialog.class);
        networkRequestDialogFragment.onUserSelectionCallbackRegistration(selectionCallback);

        // Act.
        networkRequestDialogFragment.onClick(dialog, indexClickItem);

        // Check.
        verify(selectionCallback, times(1)).select(clickedAccessPoint.getConfig());
    }

    @Test
    public void onMatch_shouldUpdatedList() {
        // Assert.
        when(networkRequestDialogFragment.getContext()).thenReturn(mContext);
        Context applicationContext = spy(RuntimeEnvironment.application.getApplicationContext());
        when(mContext.getApplicationContext()).thenReturn(applicationContext);
        WifiManager wifiManager = mock(WifiManager.class);
        when(applicationContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
        networkRequestDialogFragment.onResume();

        List<AccessPoint> accessPointList = createAccessPointList();
        when(mWifiTracker.getAccessPoints()).thenReturn(accessPointList);

        final String SSID_AP1 = "Test AP 1";
        final String SSID_AP2 = "Test AP 2";
        List<ScanResult> scanResults = new ArrayList<>();
        ScanResult scanResult = new ScanResult();
        scanResult.SSID = SSID_AP1;
        scanResult.capabilities = "WEP";
        scanResults.add(scanResult);
        scanResult = new ScanResult();
        scanResult.SSID = SSID_AP2;
        scanResult.capabilities = "WEP";
        scanResults.add(scanResult);

        // Act.
        networkRequestDialogFragment.onMatch(scanResults);

        // Check.
        List<AccessPoint> returnList = networkRequestDialogFragment.getAccessPointList();
        assertThat(returnList).isNotEmpty();
        assertThat(returnList.size()).isEqualTo(2);
        assertThat(returnList.get(0).getSsid()).isEqualTo(SSID_AP1);
        assertThat(returnList.get(1).getSsid()).isEqualTo(SSID_AP2);
    }

    private List<AccessPoint> createAccessPointList() {
        List<AccessPoint> accessPointList = spy(new ArrayList<>());
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SSID, "Test AP 1");
        bundle.putInt(KEY_SECURITY, 1);
        accessPointList.add(new AccessPoint(mContext, bundle));
        bundle.putString(KEY_SSID, "Test AP 2");
        bundle.putInt(KEY_SECURITY, 1);
        accessPointList.add(new AccessPoint(mContext, bundle));
        bundle.putString(KEY_SSID, "Test AP 3");
        bundle.putInt(KEY_SECURITY, 2);
        AccessPoint clickedAccessPoint = new AccessPoint(mContext, bundle);
        accessPointList.add(clickedAccessPoint);
        bundle.putString(KEY_SSID, "Test AP 4");
        bundle.putInt(KEY_SECURITY, 0);
        accessPointList.add(new AccessPoint(mContext, bundle));

        return accessPointList;
    }
}
