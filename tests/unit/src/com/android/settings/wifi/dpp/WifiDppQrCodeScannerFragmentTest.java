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

package com.android.settings.wifi.dpp;

import static com.android.settings.wifi.dpp.WifiDppUtils.TAG_FRAGMENT_QR_CODE_SCANNER;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.fragment.app.FragmentManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiDppQrCodeScannerFragmentTest {
    @Rule
    public final ActivityTestRule<WifiDppConfiguratorActivity> mActivityRule =
            new ActivityTestRule<>(WifiDppConfiguratorActivity.class, true);

    @Before
    public void setUp() {
        Intent intent = new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "password");
        mActivityRule.launchActivity(intent);
    }

    @Test
    public void rotateScreen_shouldNotCrash() {
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void onPause_shouldNotDecodeQrCode() {
        final WifiDppConfiguratorActivity hostActivity =
                (WifiDppConfiguratorActivity) mActivityRule.getActivity();
        final FragmentManager fragmentManager = hostActivity.getSupportFragmentManager();
        final WifiDppQrCodeScannerFragment scannerFragment =
                (WifiDppQrCodeScannerFragment) fragmentManager
                .findFragmentByTag(TAG_FRAGMENT_QR_CODE_SCANNER);
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        instrumentation.runOnMainSync(() -> {
            instrumentation.callActivityOnPause(hostActivity);

            assertThat(scannerFragment.isDecodeTaskAlive()).isEqualTo(false);
        });
    }
}
