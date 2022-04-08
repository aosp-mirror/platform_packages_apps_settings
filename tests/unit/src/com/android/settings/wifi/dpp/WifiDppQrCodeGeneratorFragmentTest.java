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

import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiDppQrCodeGeneratorFragmentTest {
    @Rule
    public final ActivityTestRule<WifiDppConfiguratorActivity> mActivityRule =
            new ActivityTestRule<>(WifiDppConfiguratorActivity.class, true);

    @Before
    public void setUp() {
        Intent intent =
                new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_GENERATOR);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        mActivityRule.launchActivity(intent);
    }

    @Test
    public void rotateScreen_shouldNotCrash() {
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}
