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

import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;
import android.provider.Settings;
import android.system.helpers.SettingsHelper;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;


public class MoreWirelessSettingsTests extends InstrumentationTestCase {

    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final int TIMEOUT = 2000;
    private UiDevice mDevice;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientaion", e);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        mDevice.pressBack();
        mDevice.pressHome();
        super.tearDown();
    }

    @Presubmit
    @MediumTest
    public void testAirplaneModeEnabled() throws Exception {
        verifyAirplaneModeOnOrOff(true);
        // Toggling this via the wifi network settings page
        // because of bug b/34858716. Once that is fixed,
        // we should be able to set this via Settings putString.
        toggleAirplaneModeSwitch();
    }

    @Presubmit
    @MediumTest
    public void testAirplaneModeDisabled() throws Exception {
        verifyAirplaneModeOnOrOff(false);
    }

    @MediumTest
    public void testTetheringMenuLoad() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_WIRELESS_SETTINGS);
        mDevice.wait(Until
                 .findObject(By.text("Hotspot & tethering")), TIMEOUT)
                 .click();
        Thread.sleep(TIMEOUT);
        UiObject2 usbTethering = mDevice.wait(Until
                 .findObject(By.text("USB tethering")), TIMEOUT);
        assertNotNull("Tethering screen did not load correctly", usbTethering);
    }

    @MediumTest
    public void testVPNMenuLoad() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_WIRELESS_SETTINGS);
        mDevice.findObject(By.res(SETTINGS_PACKAGE, "main_content"))
                .scrollUntil(Direction.DOWN, Until.findObject(By.text("VPN")))
                 .click();
        Thread.sleep(TIMEOUT);
        UiObject2 usbTethering = mDevice.wait(Until
                 .findObject(By.res(SETTINGS_PACKAGE, "vpn_create")), TIMEOUT);
        assertNotNull("VPN screen did not load correctly", usbTethering);
    }

    private void verifyAirplaneModeOnOrOff(boolean verifyOn) throws Exception {
        if (verifyOn) {
            Settings.Global.putString(getInstrumentation().getContext().getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, "0");
        }
        else {
            Settings.Global.putString(getInstrumentation().getContext().getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, "1");
        }
        toggleAirplaneModeSwitch();
        String airplaneModeValue = Settings.Global
                .getString(getInstrumentation().getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON);
        if (verifyOn) {
            assertEquals("1", airplaneModeValue);
        }
        else {
            assertEquals("0", airplaneModeValue);
        }
    }

    private void toggleAirplaneModeSwitch() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_WIRELESS_SETTINGS);
        mDevice.wait(Until
                .findObject(By.text("Airplane mode")), TIMEOUT)
                .click();
        Thread.sleep(TIMEOUT);
    }
}
