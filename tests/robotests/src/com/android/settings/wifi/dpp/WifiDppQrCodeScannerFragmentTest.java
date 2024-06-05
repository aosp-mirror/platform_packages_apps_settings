/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.wifi.dpp;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;

import androidx.fragment.app.FragmentActivity;

import com.android.settingslib.wifi.WifiPermissionChecker;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class WifiDppQrCodeScannerFragmentTest {

    static final String WIFI_SSID = "wifi-ssid";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    WifiPickerTracker mWifiPickerTracker;
    @Mock
    WifiEntry mWifiEntry;
    @Mock
    WifiPermissionChecker mWifiPermissionChecker;
    @Mock
    FragmentActivity mActivity;

    WifiDppQrCodeScannerFragment mFragment;

    @Before
    public void setUp() {
        when(mWifiEntry.getSsid()).thenReturn(WIFI_SSID);
        when(mWifiPickerTracker.getWifiEntries()).thenReturn(Arrays.asList(mWifiEntry));

        mFragment = spy(
                new WifiDppQrCodeScannerFragment(mWifiPickerTracker, mWifiPermissionChecker));
    }

    @Test
    public void canConnectWifi_noAvailableWifiMatch_returnTrue() {
        when(mWifiEntry.getSsid()).thenReturn("diff-wifi-ssid");
        when(mWifiEntry.canConnect()).thenReturn(false);

        assertThat(mFragment.canConnectWifi(WIFI_SSID)).isTrue();
    }

    @Test
    public void canConnectWifi_wifiCanConnect_returnTrue() {
        when(mWifiEntry.canConnect()).thenReturn(true);

        assertThat(mFragment.canConnectWifi(WIFI_SSID)).isTrue();
    }

    @Test
    public void canConnectWifi_wifiCanNotConnect_returnFalseAndShowError() {
        when(mWifiEntry.canConnect()).thenReturn(false);
        doNothing().when(mFragment).showErrorMessageAndRestartCamera(anyInt());

        assertThat(mFragment.canConnectWifi(WIFI_SSID)).isFalse();
        verify(mFragment).showErrorMessageAndRestartCamera(anyInt());
    }

    @Test
    public void onSuccess_noWifiPermission_finishActivityWithoutSetResult() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mWifiPermissionChecker.canAccessWifiState()).thenReturn(false);
        when(mWifiPermissionChecker.canAccessFineLocation()).thenReturn(false);

        mFragment.onSuccess();

        verify(mActivity).finish();
        verify(mActivity, never()).setResult(eq(Activity.RESULT_OK), any());
    }

    @Test
    public void onSuccess_hasAccessWifiStatePermissionOnly_finishActivityWithoutSetResult() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mWifiPermissionChecker.canAccessWifiState()).thenReturn(true);
        when(mWifiPermissionChecker.canAccessFineLocation()).thenReturn(false);

        mFragment.onSuccess();

        verify(mActivity).finish();
        verify(mActivity, never()).setResult(eq(Activity.RESULT_OK), any());
    }

    @Test
    public void onSuccess_hasAccessFineLocationPermissionOnly_finishActivityWithoutSetResult() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mWifiPermissionChecker.canAccessWifiState()).thenReturn(false);
        when(mWifiPermissionChecker.canAccessFineLocation()).thenReturn(true);

        mFragment.onSuccess();

        verify(mActivity).finish();
        verify(mActivity, never()).setResult(eq(Activity.RESULT_OK), any());
    }

    @Test
    public void onSuccess_hasRequiredPermissions_finishActivityWithSetResult() {
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mWifiPermissionChecker.canAccessWifiState()).thenReturn(true);
        when(mWifiPermissionChecker.canAccessFineLocation()).thenReturn(true);

        mFragment.onSuccess();

        verify(mActivity).setResult(eq(Activity.RESULT_OK), any());
        verify(mActivity).finish();
    }
}
