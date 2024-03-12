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
 * limitations under the License.
 */

package com.android.settings.wifi;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import static com.android.settings.wifi.WifiDialogActivity.REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER;
import static com.android.settings.wifi.WifiDialogActivity.RESULT_CONNECTED;
import static com.android.settings.wifi.WifiDialogActivity.RESULT_OK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.UserManager;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.wifi.AccessPoint;
import com.android.wifitrackerlib.NetworkDetailsTracker;
import com.android.wifitrackerlib.WifiEntry;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@Ignore("b/314867581")
@RunWith(RobolectricTestRunner.class)
public class WifiDialogActivityTest {

    static final String CALLING_PACKAGE = "calling_package";
    static final int REQUEST_CODE = REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER;
    private static final String SSID = "SSID";

    @Mock
    UserManager mUserManager;
    @Mock
    PackageManager mPackageManager;
    @Mock
    WifiDialog mWifiDialog;
    @Mock
    WifiConfiguration mWifiConfiguration;
    @Mock
    AccessPoint mAccessPoint;
    @Mock
    WifiDialog2 mWifiDialog2;
    @Mock
    WifiConfigController2 mWifiConfiguration2;
    @Mock
    WifiEntry mWifiEntry;
    @Mock
    Intent mResultData;
    @Mock
    WifiConfigController mController;
    @Mock
    KeyguardManager mKeyguardManager;

    WifiDialogActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mWifiDialog.getController()).thenReturn(mController);
        mWifiConfiguration.SSID = SSID;
        when(mController.getConfig()).thenReturn(mWifiConfiguration);
        when(mController.getAccessPoint()).thenReturn(mAccessPoint);
        when(mWifiDialog2.getController()).thenReturn(mWifiConfiguration2);
        when(mWifiConfiguration2.getWifiEntry()).thenReturn(mWifiEntry);
        when(mWifiEntry.canConnect()).thenReturn(true);
        FakeFeatureFactory.setupForTest();
        WifiTrackerLibProvider mockWifiTrackerLibProvider =
                FakeFeatureFactory.getFeatureFactory().getWifiTrackerLibProvider();
        when(mockWifiTrackerLibProvider.createNetworkDetailsTracker(
                any(), any(), any(), any(), any(), anyLong(), anyLong(), any())
        ).thenReturn(mock(NetworkDetailsTracker.class));

        mActivity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(mActivity.getSystemService(UserManager.class)).thenReturn(mUserManager);

        when(mActivity.getSystemService(KeyguardManager.class)).thenReturn(mKeyguardManager);
    }

    @Test
    public void onSubmit_shouldConnectToNetwork() {
        mActivity.onSubmit(mWifiDialog);

        WifiManager wifiManager = mActivity.getSystemService(WifiManager.class);
        assertThat(wifiManager.getConnectionInfo().getSSID()).isEqualTo("\"SSID\"");
    }

    @Test
    public void onSubmit_noPermissionForResult_setResultWithoutData() {
        when(mActivity.hasPermissionForResult()).thenReturn(false);

        mActivity.onSubmit(mWifiDialog);

        verify(mActivity).setResult(RESULT_CONNECTED, null);
    }

    @Test
    public void onSubmit_hasPermissionForResult_setResultWithData() {
        when(mActivity.hasPermissionForResult()).thenReturn(true);
        when(mActivity.createResultData(any(), any())).thenReturn(mResultData);

        mActivity.onSubmit(mWifiDialog);

        verify(mActivity).setResult(RESULT_CONNECTED, mResultData);
    }

    @Test
    public void onSubmit2_noPermissionForResult_setResultWithoutData() {
        when(mActivity.hasPermissionForResult()).thenReturn(false);

        mActivity.onSubmit(mWifiDialog2);

        verify(mActivity).setResult(RESULT_CONNECTED, null);
    }

    @Test
    public void onSubmit2_hasPermissionForResult_setResultWithData() {
        when(mActivity.hasPermissionForResult()).thenReturn(true);
        when(mActivity.createResultData(any(), any())).thenReturn(mResultData);

        mActivity.onSubmit(mWifiDialog2);

        verify(mActivity).setResult(RESULT_CONNECTED, mResultData);
    }

    @Test
    public void onSubmit2_whenConnectForCallerIsTrue_shouldConnectToNetwork() {
        final Intent intent = new Intent("com.android.settings.WIFI_DIALOG");
        intent.putExtra(WifiDialogActivity.KEY_CHOSEN_WIFIENTRY_KEY, "FAKE_KEY");
        intent.putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, true);
        mActivity = spy(Robolectric.buildActivity(WifiDialogActivity.class, intent).setup().get());

        mActivity.onSubmit(mWifiDialog2);

        verify(mWifiEntry).connect(any());
    }

    @Test
    public void onSubmit_whenConnectForCallerIsFalse_shouldNotConnectToNetwork() {
        final Intent intent = new Intent();
        intent.putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false);
        mActivity = spy(Robolectric.buildActivity(WifiDialogActivity.class, intent).setup().get());

        mActivity.onSubmit(mWifiDialog);

        WifiManager wifiManager = mActivity.getSystemService(WifiManager.class);
        assertThat(wifiManager.getConnectionInfo().getSSID()).isEqualTo(WifiManager.UNKNOWN_SSID);
    }

    @Test
    public void onSubmit2_whenConnectForCallerIsFalse_shouldNotConnectToNetwork() {
        final Intent intent = new Intent("com.android.settings.WIFI_DIALOG");
        intent.putExtra(WifiDialogActivity.KEY_CHOSEN_WIFIENTRY_KEY, "FAKE_KEY");
        intent.putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false);
        mActivity = spy(Robolectric.buildActivity(WifiDialogActivity.class, intent).setup().get());

        mActivity.onSubmit(mWifiDialog2);

        verify(mWifiEntry, never()).connect(any());
    }

    @Test
    public void onStart_whenLaunchInSetupFlow_shouldCreateDialogWithSuwTheme() {
        final Intent intent = new Intent();
        intent.putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false);
        intent.putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true);
        intent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        mActivity = spy(Robolectric.buildActivity(WifiDialogActivity.class, intent).create().get());
        doNothing().when(mActivity).createDialogWithSuwTheme();

        mActivity.onStart();

        verify(mActivity).createDialogWithSuwTheme();
    }

    @Test
    public void onActivityResult_noPermissionForResult_setResultWithoutData() {
        when(mActivity.hasPermissionForResult()).thenReturn(false);

        mActivity.onActivityResult(REQUEST_CODE, RESULT_OK, mResultData);

        verify(mActivity).setResult(RESULT_CONNECTED);
    }

    @Test
    public void onActivityResult_hasPermissionForResult_setResultWithData() {
        when(mActivity.hasPermissionForResult()).thenReturn(true);

        mActivity.onActivityResult(REQUEST_CODE, RESULT_OK, mResultData);

        verify(mActivity).setResult(RESULT_CONNECTED, mResultData);
    }

    @Test
    public void isConfigWifiAllowed_hasNoUserRestriction_returnTrue() {
        when(mUserManager.hasUserRestriction(DISALLOW_CONFIG_WIFI)).thenReturn(false);

        assertThat(mActivity.isConfigWifiAllowed()).isTrue();
    }

    @Test
    public void isConfigWifiAllowed_hasUserRestriction_returnFalse() {
        when(mUserManager.hasUserRestriction(DISALLOW_CONFIG_WIFI)).thenReturn(true);

        assertThat(mActivity.isConfigWifiAllowed()).isFalse();
    }

    @Test
    public void hasPermissionForResult_noCallingPackage_returnFalse() {
        when(mActivity.getCallingPackage()).thenReturn(null);

        final boolean result = mActivity.hasPermissionForResult();

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermissionForResult_noPermission_returnFalse() {
        when(mActivity.getCallingPackage()).thenReturn(null);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        final boolean result = mActivity.hasPermissionForResult();

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermissionForResult_hasCoarseLocationPermission_returnFalse() {
        when(mActivity.getCallingPackage()).thenReturn(CALLING_PACKAGE);
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        final boolean result = mActivity.hasPermissionForResult();

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermissionForResult_hasFineLocationPermission_returnTrue() {
        when(mActivity.getCallingPackage()).thenReturn(CALLING_PACKAGE);
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        final boolean result = mActivity.hasPermissionForResult();

        assertThat(result).isTrue();
    }

    @Test
    public void hasPermissionForResult_haveBothLocationPermissions_returnTrue() {
        when(mActivity.getCallingPackage()).thenReturn(CALLING_PACKAGE);
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        final boolean result = mActivity.hasPermissionForResult();

        assertThat(result).isTrue();
    }

    @Test
    public void dismissDialog_hasDialog_dialogDismiss() {
        mActivity.mDialog = mWifiDialog;
        mActivity.mDialog2 = mWifiDialog2;

        mActivity.dismissDialog();

        verify(mWifiDialog).dismiss();
        verify(mWifiDialog2).dismiss();
    }

    @Test
    public void onKeyguardLockedStateChanged_keyguardIsNotLocked_doNotDismissDialog() {
        WifiDialogActivity.LockScreenMonitor lockScreenMonitor =
                new WifiDialogActivity.LockScreenMonitor(mActivity);

        lockScreenMonitor.onKeyguardLockedStateChanged(false /* isKeyguardLocked */);

        verify(mActivity, never()).dismissDialog();
    }

    @Test
    public void onKeyguardLockedStateChanged_keyguardIsLocked_dismissDialog() {
        WifiDialogActivity.LockScreenMonitor lockScreenMonitor =
                new WifiDialogActivity.LockScreenMonitor(mActivity);

        lockScreenMonitor.onKeyguardLockedStateChanged(true /* isKeyguardLocked */);

        verify(mActivity).dismissDialog();
    }
}
