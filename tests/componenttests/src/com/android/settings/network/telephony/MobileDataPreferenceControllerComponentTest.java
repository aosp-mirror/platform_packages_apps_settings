/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.android.settings.testutils.CommonUtils.set_wifi_enabled;

import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.Settings;
import com.android.settings.testutils.CommonUtils;
import com.android.settings.testutils.UiUtils;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MobileDataPreferenceControllerComponentTest {
    public static final int TIMEOUT = 2000;
    private static int sSubscriptionId = 2;
    public final String TAG = this.getClass().getName();
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final WifiManager mWifiManager =
            (WifiManager) mInstrumentation.getTargetContext().getSystemService(
                    Context.WIFI_SERVICE);
    private final TelephonyManager mTelephonyManager =
            (TelephonyManager) mInstrumentation.getTargetContext().getSystemService(
                    Context.TELEPHONY_SERVICE);
    private final TelecomManager mTelecomManager =
            (TelecomManager) mInstrumentation.getTargetContext().getSystemService(
                    Context.TELECOM_SERVICE);

    @Rule
    public ActivityScenarioRule<Settings.MobileNetworkActivity>
            rule = new ActivityScenarioRule<>(
            new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    private boolean mOriginDataEnabled;
    private boolean mOriginWifiEnabled;

    @Before
    public void setUp() {
        mOriginWifiEnabled = mWifiManager.isWifiEnabled();
        // Disable wifi
        set_wifi_enabled(false);

        // Enable mobile data
        mOriginDataEnabled = mTelephonyManager.isDataEnabled();
        if (!mOriginDataEnabled) {
            mTelephonyManager.enableDataConnectivity();
        }

        // Current sim card is not available for data network.
        sSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        Assume.assumeTrue("Device cannot mobile network! Should ignore test.",
                sSubscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        int simState = mTelephonyManager.getSimState();
        Assume.assumeTrue("Sim card is not ready. Expect: " + TelephonyManager.SIM_STATE_READY
                + ", Actual: " + simState, simState == TelephonyManager.SIM_STATE_READY);
    }

    /**
     * Tests the mobile network is disabled.
     * Precondition:
     * Disabled wifi, and enabled mobile network.
     * Steps:
     * 1. Launch mobile data page.
     * 2. Turn off mobile data from switch.
     * [Check]
     * - Mobile data is turned off via TelephonyManager.
     * - Open socket connection https://www.google.net and check the connection failed.
     */
    @Test
    public void test_disable_mobile_network() {
        ActivityScenario scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            try {
                URL url = new URL("https://www.google.net");
                MobileDataPreferenceController controller = new MobileDataPreferenceController(
                        mInstrumentation.getTargetContext(), "mobile_data");
                FragmentManager manager = ((FragmentActivity) activity).getSupportFragmentManager();
                controller.init(manager, sSubscriptionId);

                // Make sure mobile network can connect at first.
                assertThat(UiUtils.waitUntilCondition(1000,
                        () -> CommonUtils.connectToURL(url))).isTrue();

                Log.d(TAG, "Start to click ");
                controller.setChecked(false);
                Log.d(TAG, "Set Checked, wait for fully close.");

                // Assert the configuration is set.
                assertThat(UiUtils.waitUntilCondition(10000,
                        () -> !mTelephonyManager.isDataEnabled())).isTrue();

                // Assert the network is not connectable.
                assertThat(UiUtils.waitUntilCondition(1000,
                        () -> CommonUtils.connectToURL(url))).isFalse();

            } catch (IOException e) {

            }
        });
    }

    @After
    public void tearDown() {
        // Restore wifi status wifi
        set_wifi_enabled(mOriginWifiEnabled);

        // Restore mobile data status
        if (mOriginDataEnabled != mTelephonyManager.isDataEnabled()) {
            if (mOriginDataEnabled) {
                mTelephonyManager.enableDataConnectivity();
            } else {
                mTelephonyManager.disableDataConnectivity();
            }
        }
    }
}
