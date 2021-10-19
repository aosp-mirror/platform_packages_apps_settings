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

import static com.android.settings.wifi.WifiDialogActivity.REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER;
import static com.android.settings.wifi.WifiDialogActivity.RESULT_CONNECTED;
import static com.android.settings.wifi.WifiDialogActivity.RESULT_OK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;
import com.android.settings.testutils.shadow.ShadowWifiManager;
import com.android.settingslib.wifi.AccessPoint;

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowConnectivityManager.class,
        ShadowWifiManager.class,
        ShadowAlertDialogCompat.class
})
public class WifiDialogActivityTest {

    private static final String CALLING_PACKAGE = "calling_package";
    private static final String AP1_SSID = "\"ap1\"";

    @Mock
    PackageManager mPackageManager;
    @Mock
    WifiManager mWifiManager;
    @Mock
    WifiDialog mWifiDialog;
    @Mock
    WifiConfiguration mWifiConfiguration;
    @Mock
    AccessPoint mAccessPoint;
    @Mock
    Intent mResultData;
    @Mock
    private WifiConfigController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mWifiDialog.getController()).thenReturn(mController);
        when(mController.getConfig()).thenReturn(mWifiConfiguration);
        when(mController.getAccessPoint()).thenReturn(mAccessPoint);

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = AP1_SSID;
        doReturn(wifiConfig).when(mController).getConfig();
    }

    @Test
    public void onSubmit_shouldConnectToNetwork() {
        WifiDialogActivity activity = Robolectric.setupActivity(WifiDialogActivity.class);
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController);

        activity.onSubmit(dialog);

        assertThat(ShadowWifiManager.get().savedWifiConfig.SSID).isEqualTo(AP1_SSID);
    }

    @Test
    public void onSubmit_noPermissionForResult_setResultWithoutData() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.hasPermissionForResult()).thenReturn(false);
        when(activity.getSystemService(WifiManager.class)).thenReturn(mWifiManager);

        activity.onSubmit(mWifiDialog);

        verify(activity).setResult(RESULT_CONNECTED, null);
    }

    @Test
    public void onSubmit_hasPermissionForResult_setResultWithData() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.hasPermissionForResult()).thenReturn(true);
        when(activity.createResultData(any(), any())).thenReturn(mResultData);
        when(activity.getSystemService(WifiManager.class)).thenReturn(mWifiManager);

        activity.onSubmit(mWifiDialog);

        verify(activity).setResult(RESULT_CONNECTED, mResultData);
    }

    @Test
    public void onSubmit_whenConnectForCallerIsFalse_shouldNotConnectToNetwork() {
        WifiDialogActivity activity =
                Robolectric.buildActivity(
                        WifiDialogActivity.class,
                        new Intent().putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false))
                        .setup().get();
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        ReflectionHelpers.setField(dialog, "mController", mController);

        activity.onSubmit(dialog);

        assertThat(ShadowWifiManager.get().savedWifiConfig).isNull();
    }

    @Test
    public void onSubmit_whenLaunchInSetupFlow_shouldBeLightThemeForWifiDialog() {
        WifiDialogActivity activity =
                Robolectric.buildActivity(
                        WifiDialogActivity.class,
                        new Intent()
                                .putExtra(WifiDialogActivity.KEY_CONNECT_FOR_CALLER, false)
                                .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
                                .putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true))
                        .setup().get();
        WifiDialog dialog = (WifiDialog) ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        activity.onSubmit(dialog);

        assertThat(dialog.getContext().getThemeResId())
                .isEqualTo(R.style.SuwAlertDialogThemeCompat_Light);
    }

    @Test
    public void onActivityResult_noPermissionForResult_setResultWithoutData() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.hasPermissionForResult()).thenReturn(false);
        final Intent data = new Intent();

        activity.onActivityResult(REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER, RESULT_OK,
                data);

        verify(activity).setResult(RESULT_CONNECTED);
    }

    @Test
    public void onActivityResult_hasPermissionForResult_setResultWithData() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.hasPermissionForResult()).thenReturn(true);
        final Intent data = new Intent();

        activity.onActivityResult(REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER, RESULT_OK,
                data);

        verify(activity).setResult(RESULT_CONNECTED, data);
    }

    @Test
    public void hasPermissionForResult_noCallingPackage_returnFalse() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.getCallingPackage()).thenReturn(null);

        final boolean result = activity.hasPermissionForResult();

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermissionForResult_noPermission_returnFalse() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.getCallingPackage()).thenReturn(null);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        final boolean result = activity.hasPermissionForResult();

        assertThat(result).isFalse();
    }

    @Test
    public void hasPermissionForResult_hasCoarseLocationPermission_returnTrue() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.getCallingPackage()).thenReturn(CALLING_PACKAGE);
        when(activity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        final boolean result = activity.hasPermissionForResult();

        assertThat(result).isTrue();
    }

    @Test
    public void hasPermissionForResult_hasFineLocationPermission_returnTrue() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.getCallingPackage()).thenReturn(CALLING_PACKAGE);
        when(activity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        final boolean result = activity.hasPermissionForResult();

        assertThat(result).isTrue();
    }

    @Test
    public void hasPermissionForResult_haveBothLocationPermissions_returnTrue() {
        WifiDialogActivity activity = spy(Robolectric.setupActivity(WifiDialogActivity.class));
        when(activity.getCallingPackage()).thenReturn(CALLING_PACKAGE);
        when(activity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.checkPermission(ACCESS_COARSE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManager.checkPermission(ACCESS_FINE_LOCATION, CALLING_PACKAGE))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        final boolean result = activity.hasPermissionForResult();

        assertThat(result).isTrue();
    }
}
