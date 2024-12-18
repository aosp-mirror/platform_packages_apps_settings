/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_ADD_WIFI_CONFIG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.wifi.factory.WifiFeatureProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
@UiThreadTest
public class WifiDppConfiguratorActivityTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private UserManager mUserManager;
    @Mock
    private FragmentManager mFragmentManager;

    // Mock, created by FakeFeatureFactory
    private WifiFeatureProvider mWifiFeatureProviderMock;

    @Spy
    private WifiDppConfiguratorActivity mActivity;

    @Before
    public void setUp() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);

        mActivity.mFragmentManager = mFragmentManager;
        doReturn(mContext).when(mActivity).getApplicationContext();

        FragmentTransaction mockTransaction = mock(FragmentTransaction.class);
        when(mFragmentManager.beginTransaction()).thenReturn(mockTransaction);
        when(mockTransaction.replace(anyInt(), any(Fragment.class), anyString()))
                .thenReturn(mockTransaction);

        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        mWifiFeatureProviderMock = featureFactory.mWifiFeatureProvider;
    }

    @Test
    public void isAddWifiConfigAllowed_hasNoUserRestriction_returnTrue() {
        when(mUserManager.hasUserRestriction(DISALLOW_ADD_WIFI_CONFIG)).thenReturn(false);

        assertThat(mActivity.isAddWifiConfigAllowed(mContext)).isTrue();
    }

    @Test
    public void isAddWifiConfigAllowed_hasUserRestriction_returnFalse() {
        when(mUserManager.hasUserRestriction(DISALLOW_ADD_WIFI_CONFIG)).thenReturn(true);

        assertThat(mActivity.isAddWifiConfigAllowed(mContext)).isFalse();
    }


    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WIFI_SHARING_RUNTIME_FRAGMENT)
    public void showQrCodeGeneratorFragment_shouldUseFeatureFactory() {
        when(mUserManager.isGuestUser()).thenReturn(false);
        when(mWifiFeatureProviderMock.getWifiDppQrCodeGeneratorFragment())
                .thenReturn(new WifiDppQrCodeGeneratorFragment());

        mActivity.handleIntent(createQrCodeGeneratorIntent());

        verify(mWifiFeatureProviderMock).getWifiDppQrCodeGeneratorFragment();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_WIFI_SHARING_RUNTIME_FRAGMENT)
    public void showQrCodeGeneratorFragment_shouldNotUseFeatureFactory() {
        when(mUserManager.isGuestUser()).thenReturn(false);

        mActivity.handleIntent(createQrCodeGeneratorIntent());

        verify(mWifiFeatureProviderMock, never())
                .getWifiDppQrCodeGeneratorFragment();
    }

    private static Intent createQrCodeGeneratorIntent() {
        Intent intent = new Intent(
                WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WPA");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "\\012345678,");
        return intent;
    }
}
