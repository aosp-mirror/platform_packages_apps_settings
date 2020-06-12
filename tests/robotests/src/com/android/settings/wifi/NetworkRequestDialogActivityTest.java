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
 * limitations under the License
 */

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.wifi.NetworkRequestErrorDialogFragment.ERROR_DIALOG_TYPE;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class NetworkRequestDialogActivityTest {

    private static final String TEST_SSID = "testssid";
    private static final String TEST_CAPABILITY = "wep";

    NetworkRequestDialogActivity mActivity;
    WifiManager mWifiManager;
    Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        WifiTracker wifiTracker = mock(WifiTracker.class);
        WifiTrackerFactory.setTestingWifiTracker(wifiTracker);

        NetworkRequestDialogActivity activity =
            Robolectric.setupActivity(NetworkRequestDialogActivity.class);
        mActivity = spy(activity);

        mWifiManager = mock(WifiManager.class);
        when(mActivity.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
    }

    @Test
    public void LaunchActivity_shouldShowNetworkRequestDialog() {
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog.isShowing()).isTrue();
    }

    @Test
    public void onResume_shouldRegisterCallback() {
        mActivity.onResume();

        verify(mWifiManager).registerNetworkRequestMatchCallback(any(), any());
    }

    @Test
    public void onPause_shouldUnRegisterCallback() {
        mActivity.onPause();

        verify(mWifiManager).unregisterNetworkRequestMatchCallback(mActivity);
    }

    @Test
    public void onResumeAndWaitTimeout_shouldCallTimeoutDialog() {
        FakeNetworkRequestDialogActivity fakeActivity =
            Robolectric.setupActivity(FakeNetworkRequestDialogActivity.class);

        fakeActivity.onResume();
        ShadowLooper.getShadowMainLooper().runToEndOfTasks();

        assertThat(fakeActivity.bCalledStopAndPop).isTrue();
        assertThat(fakeActivity.errorType).isEqualTo(ERROR_DIALOG_TYPE.TIME_OUT);
    }

    public static class FakeNetworkRequestDialogActivity extends NetworkRequestDialogActivity {
        boolean bCalledStopAndPop = false;
        ERROR_DIALOG_TYPE errorType = null;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void stopScanningAndPopErrorDialog(ERROR_DIALOG_TYPE type) {
            bCalledStopAndPop = true;
            errorType = type;
        }
    }

    private void startSpecifiedActivity() {
        final Intent intent = new Intent().setClassName(
                RuntimeEnvironment.application.getPackageName(),
                NetworkRequestDialogActivity.class.getName());
        intent.putExtra(NetworkRequestDialogActivity.EXTRA_IS_SPECIFIED_SSID, true);
        mActivity = spy(Robolectric.buildActivity(NetworkRequestDialogActivity.class,
                intent).create().get());
    }

    @Test
    public void updateAccessPointList_onUserSelectionConnectSuccess_shouldFinishActivity() {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "Test AP 3";
        mActivity.onUserSelectionConnectSuccess(config);

        verify(mActivity).finish();
    }

    @Test
    public void specifiedSsid_onCreate_shouldShowProgressDialog() {
        startSpecifiedActivity();

        assertThat(mActivity.mProgressDialog).isNotNull();
        assertThat(mActivity.mProgressDialog.isShowing()).isTrue();
    }

    private ScanResult getScanResult(String ssid, String capability) {
        final ScanResult scanResult = mock(ScanResult.class);
        scanResult.SSID = ssid;
        scanResult.capabilities = capability;

        return scanResult;
    }

    @Test
    public void specifiedSsid_onMatch_shouldShowDialogFragment() {
        startSpecifiedActivity();

        final List<ScanResult> scanResults = new ArrayList<>();
        scanResults.add(getScanResult(TEST_SSID, TEST_CAPABILITY));

        mActivity.onMatch(scanResults);

        assertThat(mActivity.mProgressDialog).isNull();
        assertThat(mActivity.mDialogFragment).isNotNull();
    }

    @Test
    public void onAbort_withFakeActivity_callStopAndPopShouldBeTrue() {
        final FakeNetworkRequestDialogActivity fakeActivity =
                Robolectric.setupActivity(FakeNetworkRequestDialogActivity.class);

        fakeActivity.onResume();
        fakeActivity.onAbort();

        assertThat(fakeActivity.bCalledStopAndPop).isTrue();
    }

    @Test
    public void onUserSelectionConnectFailure_shouldShowDialogFragment() {
        WifiConfiguration wifiConfiguration = mock(WifiConfiguration.class);
        startSpecifiedActivity();
        final List<ScanResult> scanResults = new ArrayList<>();
        scanResults.add(getScanResult(TEST_SSID, TEST_CAPABILITY));
        mActivity.onMatch(scanResults);

        mActivity.onUserSelectionConnectFailure(wifiConfiguration);

        assertThat(mActivity.mProgressDialog).isNull();
        assertThat(mActivity.mDialogFragment).isNotNull();
    }

    @Test
    public void onClickConnectButton_shouldShowProgressDialog() {
        NetworkRequestUserSelectionCallback networkRequestUserSelectionCallback = mock(
                NetworkRequestUserSelectionCallback.class);
        startSpecifiedActivity();
        final List<ScanResult> scanResults = new ArrayList<>();
        scanResults.add(getScanResult(TEST_SSID, TEST_CAPABILITY));
        mActivity.onMatch(scanResults);
        mActivity.onUserSelectionCallbackRegistration(networkRequestUserSelectionCallback);

        mActivity.onClickConnectButton();

        assertThat(mActivity.mProgressDialog.isShowing()).isTrue();
        assertThat(mActivity.mDialogFragment).isNull();
    }

    @Test
    public void onCancel_shouldCloseAllUI() {
        startSpecifiedActivity();
        final List<ScanResult> scanResults = new ArrayList<>();
        scanResults.add(getScanResult(TEST_SSID, TEST_CAPABILITY));
        mActivity.onMatch(scanResults);

        mActivity.onCancel();

        assertThat(mActivity.mProgressDialog).isNull();
        assertThat(mActivity.mDialogFragment).isNull();
    }

    @Test
    public void updateAccessPointList_onUserSelectionConnectFailure_shouldFinishActivity() {
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = "Test AP 3";
        mActivity.onUserSelectionConnectFailure(config);

        verify(mActivity).finish();
    }
}
