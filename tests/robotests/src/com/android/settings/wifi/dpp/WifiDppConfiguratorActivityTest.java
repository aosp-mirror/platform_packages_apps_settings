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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@Ignore("b/314867581")
@RunWith(RobolectricTestRunner.class)
public class WifiDppConfiguratorActivityTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    UserManager mUserManager;

    WifiDppConfiguratorActivity mActivity;
    Intent mIntent;

    @Before
    public void setUp() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isGuestUser()).thenReturn(false);

        mIntent = new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        mIntent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        mIntent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WPA");
        mIntent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "\\012345678,");

        mActivity = spy(Robolectric.setupActivity(WifiDppConfiguratorActivity.class));
        when(mActivity.getApplicationContext()).thenReturn(mContext);
    }

    @Test
    public void launchActivity_noIntentAction_shouldNotFatalException() {
        WifiDppConfiguratorActivity wifiDppConfiguratorActivity =
                Robolectric.setupActivity(WifiDppConfiguratorActivity.class);
    }

    @Test
    public void handleIntent_isGuestUser_shouldFinish() {
        when(mUserManager.isGuestUser()).thenReturn(true);

        mActivity.handleIntent(mIntent);

        verify(mActivity).finish();
    }

    @Test
    public void handleIntent_notGuestUser_shouldNotFinish() {
        when(mUserManager.isGuestUser()).thenReturn(false);
        doNothing().when(mActivity).showQrCodeScannerFragment();

        mActivity.handleIntent(mIntent);

        verify(mActivity, never()).finish();
    }
}
