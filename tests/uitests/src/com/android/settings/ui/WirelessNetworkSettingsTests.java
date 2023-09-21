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

package com.android.settings.ui;

import static com.android.settings.ui.testutils.SettingsTestUtils.SETTINGS_PACKAGE;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;
import android.provider.Settings;
import android.system.helpers.CommandsHelper;
import android.system.helpers.SettingsHelper;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Core tests for Wifi Settings.
 */
@Ignore
@RunWith(AndroidJUnit4.class)
@MediumTest
public class WirelessNetworkSettingsTests {
    // These back button presses are performed in tearDown() to exit Wifi
    // Settings sub-menus that a test might finish in. This number should be
    // high enough to account for the deepest sub-menu a test might enter.
    private static final int NUM_BACK_BUTTON_PRESSES = 5;
    private static final int TIMEOUT = 20000;
    private static final int SLEEP_TIME = 500;
    private static final String AIRPLANE_MODE_BROADCAST =
            "am broadcast -a android.intent.action.AIRPLANE_MODE";
    private static final String TAG = "WirelessNetworkTests";


    private UiDevice mDevice;
    private CommandsHelper mCommandsHelper;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientation", e);
        }
        // Ensure airplane mode is OFF so that wifi can be enabled using WiFiManager.
        Settings.Global.putString(InstrumentationRegistry.getTargetContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, "0");

        Log.d(TAG, "sending airplane mode broadcast to device");
        mCommandsHelper = CommandsHelper.getInstance(InstrumentationRegistry.getInstrumentation());
        mCommandsHelper.executeShellCommand(AIRPLANE_MODE_BROADCAST);
    }

    @After
    public void tearDown() {
        // Exit all settings sub-menus.
        for (int i = 0; i < NUM_BACK_BUTTON_PRESSES; ++i) {
            mDevice.pressBack();
        }
        mDevice.pressHome();
    }

    @Presubmit
    @Test
    public void testWiFiEnabled() throws Exception {
        verifyWiFiOnOrOff(true);
    }

    @Presubmit
    @Test
    public void testWiFiDisabled() throws Exception {
        verifyWiFiOnOrOff(false);
    }

    private void verifyWiFiOnOrOff(boolean verifyOn) throws Exception {
        loadWiFiSettingsPage(!verifyOn);
        mDevice.wait(Until.findObject(By.res(SETTINGS_PACKAGE, "switch_widget")), TIMEOUT)
                .click();
        Thread.sleep(SLEEP_TIME);
        final String wifiValue = Settings.Global.getString(
                InstrumentationRegistry.getTargetContext().getContentResolver(),
                Settings.Global.WIFI_ON);
        if (verifyOn) {
            // 1 is Enabled, 2 is Enabled while airplane mode is ON.
            assertThat(wifiValue).isAnyOf("1", "2");
        } else {
            assertThat(wifiValue).isEqualTo("0");
        }
    }

    private void loadWiFiSettingsPage(boolean wifiEnabled) throws Exception {
        WifiManager wifiManager = (WifiManager) InstrumentationRegistry.getTargetContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(wifiEnabled);
        SettingsHelper.launchSettingsPage(InstrumentationRegistry.getTargetContext(),
                Settings.ACTION_WIFI_SETTINGS);
    }
}
