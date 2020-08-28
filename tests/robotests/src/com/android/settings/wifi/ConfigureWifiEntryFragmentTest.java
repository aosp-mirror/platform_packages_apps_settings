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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.os.Bundle;

import com.android.wifitrackerlib.NetworkDetailsTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class ConfigureWifiEntryFragmentTest {

    private static final String KEY_SSID = "key_ssid";
    private static final String KEY_SECURITY = "key_security";

    private ConfigureWifiEntryFragment mConfigureWifiEntryFragment;

    @Mock
    private NetworkDetailsTracker mNetworkDetailsTracker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Bundle bundle = new Bundle();

        bundle.putString(KEY_SSID, "Test AP");
        bundle.putInt(KEY_SECURITY, 1 /* WEP */);
        mConfigureWifiEntryFragment = spy(new ConfigureWifiEntryFragment());
        mConfigureWifiEntryFragment.setArguments(bundle);
        mConfigureWifiEntryFragment.mNetworkDetailsTracker = mNetworkDetailsTracker;

        FragmentController.setupFragment(mConfigureWifiEntryFragment);
    }

    @Test
    public void getMetricsCategory_shouldReturnConfigureNetwork() {
        assertThat(mConfigureWifiEntryFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.SETTINGS_WIFI_CONFIGURE_NETWORK);
    }

    @Test
    public void getMode_shouldBeModeConnected() {
        assertThat(mConfigureWifiEntryFragment.getMode()).isEqualTo(
                WifiConfigUiBase.MODE_CONNECT);
    }

    @Test
    public void launchFragment_shouldShowSubmitButton() {
        assertThat(mConfigureWifiEntryFragment.getSubmitButton()).isNotNull();
    }

    @Test
    public void launchFragment_shouldShowCancelButton() {
        assertThat(mConfigureWifiEntryFragment.getCancelButton()).isNotNull();
    }

    @Test
    public void onClickSubmitButton_shouldHandleSubmitAction() {
        mConfigureWifiEntryFragment.getSubmitButton().performClick();

        verify(mConfigureWifiEntryFragment).handleSubmitAction();
    }

    @Test
    public void dispatchSubmit_shouldHandleSubmitAction() {
        mConfigureWifiEntryFragment.dispatchSubmit();

        verify(mConfigureWifiEntryFragment).handleSubmitAction();
    }

    @Test
    public void onClickCancelButton_shouldHandleCancelAction() {
        mConfigureWifiEntryFragment.getCancelButton().performClick();

        verify(mConfigureWifiEntryFragment).handleCancelAction();
    }
}
