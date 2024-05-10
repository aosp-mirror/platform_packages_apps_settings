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
 * limitations under the License
 */

package com.android.settings.wifi.dpp;

import static com.android.settings.wifi.dpp.WifiDppEnrolleeActivity.ACTION_ENROLLEE_QR_CODE_SCANNER;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import com.android.settingslib.wifi.WifiRestrictionsCache;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@Ignore("b/314867581")
@RunWith(RobolectricTestRunner.class)
public class WifiDppEnrolleeActivityTest {

    private static final String WIFI_SSID = "wifi-ssid";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    WifiRestrictionsCache mWifiRestrictionsCache;
    @Mock
    Intent mIntent;

    WifiDppEnrolleeActivity mActivity;

    @Before
    public void setUp() {
        when(mWifiRestrictionsCache.isConfigWifiAllowed()).thenReturn(true);
        when(mIntent.getAction()).thenReturn(ACTION_ENROLLEE_QR_CODE_SCANNER);
        when(mIntent.getStringExtra(WifiDppUtils.EXTRA_WIFI_SSID)).thenReturn(WIFI_SSID);

        mActivity = spy(Robolectric.setupActivity(WifiDppEnrolleeActivity.class));
        mActivity.mWifiRestrictionsCache = mWifiRestrictionsCache;
    }

    @Test
    public void launchActivity_noIntentAction_shouldNotFatalException() {
        WifiDppEnrolleeActivity wifiDppEnrolleeActivity =
                Robolectric.setupActivity(WifiDppEnrolleeActivity.class);
    }

    @Test
    public void handleIntent_noIntentAction_shouldFinish() {
        when(mIntent.getAction()).thenReturn(null);

        mActivity.handleIntent(mIntent);

        verify(mActivity).finish();
    }

    @Test
    public void handleIntent_notAllowedConfigWifi_shouldFinish() {
        when(mWifiRestrictionsCache.isConfigWifiAllowed()).thenReturn(false);

        mActivity.handleIntent(mIntent);

        verify(mActivity).finish();
    }

    @Test
    public void handleIntent_hasIntentDataAndAllowedConfigWifi_shouldShowFragment() {
        when(mWifiRestrictionsCache.isConfigWifiAllowed()).thenReturn(true);
        doNothing().when(mActivity).showQrCodeScannerFragment(WIFI_SSID);

        mActivity.handleIntent(mIntent);

        verify(mActivity).showQrCodeScannerFragment(WIFI_SSID);
    }
}
