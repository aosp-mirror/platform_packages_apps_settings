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

import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowConnectivityManager.class)
public class ConfigureAccessPointFragmentTest {

    private static final String KEY_SSID = "key_ssid";
    private static final String KEY_SECURITY = "key_security";

    private ConfigureAccessPointFragment mConfigureAccessPointFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Bundle bundle = new Bundle();

        bundle.putString(KEY_SSID, "Test AP");
        bundle.putInt(KEY_SECURITY, 1 /* WEP */);
        mConfigureAccessPointFragment = spy(new ConfigureAccessPointFragment());
        mConfigureAccessPointFragment.setArguments(bundle);
        FragmentController.setupFragment(mConfigureAccessPointFragment);
    }

    @Test
    public void getMetricsCategory_shouldReturnConfigureNetwork() {
        assertThat(mConfigureAccessPointFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.SETTINGS_WIFI_CONFIGURE_NETWORK);
    }

    @Test
    public void getMode_shouldBeModeConnected() {
        assertThat(mConfigureAccessPointFragment.getMode()).isEqualTo(
                WifiConfigUiBase.MODE_CONNECT);
    }

    @Test
    public void launchFragment_shouldShowSubmitButton() {
        assertThat(mConfigureAccessPointFragment.getSubmitButton()).isNotNull();
    }

    @Test
    public void launchFragment_shouldShowCancelButton() {
        assertThat(mConfigureAccessPointFragment.getCancelButton()).isNotNull();
    }

    @Test
    public void onClickSubmitButton_shouldHandleSubmitAction() {
        mConfigureAccessPointFragment.getSubmitButton().performClick();

        verify(mConfigureAccessPointFragment).handleSubmitAction();
    }

    @Test
    public void dispatchSubmit_shouldHandleSubmitAction() {
        mConfigureAccessPointFragment.dispatchSubmit();

        verify(mConfigureAccessPointFragment).handleSubmitAction();
    }

    @Test
    public void onClickCancelButton_shouldHandleCancelAction() {
        mConfigureAccessPointFragment.getCancelButton().performClick();

        verify(mConfigureAccessPointFragment).handleCancelAction();
    }
}
