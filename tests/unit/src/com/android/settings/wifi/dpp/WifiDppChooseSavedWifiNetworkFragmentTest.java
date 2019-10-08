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

package com.android.settings.wifi.dpp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.android.settings.wifi.dpp.WifiDppUtils.TAG_FRAGMENT_ADD_DEVICE;
import static com.android.settings.wifi.dpp.WifiDppUtils.TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import android.provider.Settings;
import androidx.fragment.app.FragmentManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WifiDppChooseSavedWifiNetworkFragmentTest {
    // Valid Wi-Fi DPP QR code
    private static final String VALID_WIFI_DPP_QR_CODE = "DPP:I:SN=4774LH2b4044;M:010203040506;K:"
            + "MDkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDIgADURzxmttZoIRIPWGoQMV00XHWCAQIhXruVWOz0NjlkIA=;;";

    // Keys used to lookup resources by name (see the resourceId/resourceString helper methods).
    private static final String STRING = "string";
    private static final String WIFI_DPP_CHOOSE_DIFFERENT_NETWORK =
            "wifi_dpp_choose_different_network";
    private static final String CANCEL = "cancel";

    @Rule
    public final ActivityTestRule<WifiDppConfiguratorActivity> mActivityRule =
            new ActivityTestRule<>(WifiDppConfiguratorActivity.class, /* initialTouchMode */true,
            /* launchActivity */ false);

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void clickCancelButton_configuratorQrCodeScannerIntent_shouldPopBackStack() {
        final Intent intent =
                new Intent(WifiDppConfiguratorActivity.ACTION_CONFIGURATOR_QR_CODE_SCANNER);
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SECURITY, "WEP");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_SSID, "GoogleGuest");
        intent.putExtra(WifiDppUtils.EXTRA_WIFI_PRE_SHARED_KEY, "password");
        final WifiDppConfiguratorActivity hostActivity = mActivityRule.launchActivity(intent);

        // Go to WifiDppChooseSavedWifiNetworkFragment and click the cancel button
        final FragmentManager fragmentManager = hostActivity.getSupportFragmentManager();
        final WifiQrCode wifiQrCode = new WifiQrCode(VALID_WIFI_DPP_QR_CODE);
        hostActivity.runOnUiThread(() ->
            ((WifiDppConfiguratorActivity)hostActivity).onScanWifiDppSuccess(wifiQrCode)
        );
        onView(withText(resourceString(WIFI_DPP_CHOOSE_DIFFERENT_NETWORK))).perform(click());
        onView(withText(resourceString(CANCEL))).perform(click());

        assertThat(fragmentManager.findFragmentByTag(TAG_FRAGMENT_ADD_DEVICE)).isNotNull();
        assertThat(fragmentManager.findFragmentByTag(TAG_FRAGMENT_CHOOSE_SAVED_WIFI_NETWORK))
                .isNull();
    }

    @Test
    public void clickCancelButton_processWifiDppQrCodeIntent_shouldFinish() {
        final Intent intent = new Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI);
        intent.setData(Uri.parse(VALID_WIFI_DPP_QR_CODE));
        final WifiDppConfiguratorActivity hostActivity = mActivityRule.launchActivity(intent);

        onView(withText(resourceString(CANCEL))).perform(click());

        assertThat(hostActivity.isFinishing()).isEqualTo(true);
    }

    private int resourceId(String type, String name) {
        return mContext.getResources().getIdentifier(name, type, mContext.getPackageName());
    }

    /** Similar to {@link #resourceId}, but for accessing R.string.<name> values. */
    private String resourceString(String name) {
        return mContext.getResources().getString(resourceId(STRING, name));
    }
}
