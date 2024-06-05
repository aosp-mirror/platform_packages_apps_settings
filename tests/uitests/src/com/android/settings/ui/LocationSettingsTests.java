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

import androidx.test.filters.MediumTest;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Ignore;

@Ignore
public class LocationSettingsTests extends InstrumentationTestCase {

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

    @MediumTest
    public void testLoadingLocationSettings () throws Exception {
        // Load Security
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_SECURITY_SETTINGS);

        SettingsHelper helper = new SettingsHelper();
        helper.scrollVert(true);
        // Tap on location
        UiObject2 settingsPanel = mDevice.wait(Until.findObject
                (By.res(SETTINGS_PACKAGE, "main_content")), TIMEOUT);
        int count = 0;
        UiObject2 locationTitle = null;
        while(count < 6 && locationTitle == null) {
            locationTitle = mDevice.wait(Until.findObject(By.text("Location")), TIMEOUT);
            if (locationTitle == null) {
                settingsPanel.scroll(Direction.DOWN, 1.0f);
            }
            count++;
        }
        // Verify location settings loads.
        locationTitle.click();
        Thread.sleep(TIMEOUT);
        assertNotNull("Location screen has not loaded correctly",
                mDevice.wait(Until.findObject(By.text("Location services")), TIMEOUT));
    }

    @Presubmit
    @MediumTest
    public void testLocationSettingOn() throws Exception {
        verifyLocationSettingsOnOrOff(true);
    }

    @MediumTest
    public void testLocationSettingOff() throws Exception {
        verifyLocationSettingsOnOrOff(false);
    }

    @MediumTest
    public void testLocationDeviceOnlyMode() throws Exception {
        // Changing the value from default before testing the toggle to Device only mode
        Settings.Secure.putInt(getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_ON);
        dismissAlertDialogs();
        Thread.sleep(TIMEOUT);
        verifyLocationSettingsMode(Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
    }

    @MediumTest
    public void testLocationBatterySavingMode() throws Exception {
        Settings.Secure.putInt(getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
        Thread.sleep(TIMEOUT);
        verifyLocationSettingsMode(Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
    }

    @MediumTest
    public void testLocationHighAccuracyMode() throws Exception {
        Settings.Secure.putInt(getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
        Thread.sleep(TIMEOUT);
        verifyLocationSettingsMode(Settings.Secure.LOCATION_MODE_ON);
    }

    @MediumTest
    public void testLocationSettingsElements() throws Exception {
        String[] textElements = {"Location", "Mode", "Recent location requests",
                "Location services"};
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        Thread.sleep(TIMEOUT);
        for (String element : textElements) {
            assertNotNull(element + " item not found under Location Settings",
                    mDevice.wait(Until.findObject(By.text(element)), TIMEOUT));
        }
    }

    @MediumTest
    public void testLocationSettingsOverflowMenuElements() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        // Verify help & feedback
        assertNotNull("Help & feedback item not found under Location Settings",
                mDevice.wait(Until.findObject(By.desc("Help & feedback")), TIMEOUT));
        // Verify scanning
        assertNotNull("Scanning item not found under Location Settings",
                mDevice.wait(Until.findObject(By.text("Scanning")), TIMEOUT));
    }

    private void verifyLocationSettingsMode(int mode) throws Exception {
        int modeIntValue = 1;
        String textMode = "Device only";
        if (mode == Settings.Secure.LOCATION_MODE_ON) {
            modeIntValue = 3;
            textMode = "High accuracy";
        }
        else if (mode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING) {
            modeIntValue = 2;
            textMode = "Battery saving";
        }
        // Load location settings
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        // Tap on mode
        dismissAlertDialogs();
        // Load location settings
        mDevice.wait(Until.findObject(By.text("Mode")), TIMEOUT).click();
        Thread.sleep(TIMEOUT);
        assertNotNull("Location mode screen not loaded", mDevice.wait(Until.findObject
                (By.text("Location mode")), TIMEOUT));
        // Choose said mode
        mDevice.wait(Until.findObject(By.text(textMode)), TIMEOUT).click();
        Thread.sleep(TIMEOUT);
        dismissAlertDialogs();
        mDevice.wait(Until.findObject(By.desc("Navigate up")), TIMEOUT).click();
        Thread.sleep(TIMEOUT);
        if (mode == Settings.Secure.LOCATION_MODE_ON ||
                mode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING) {
            dismissAlertDialogs();
        }
        // get setting and verify value
        // Verify change of mode
        int locationSettingMode =
                Settings.Secure.getInt(getInstrumentation().getContext().getContentResolver(),
                Settings.Secure.LOCATION_MODE);
        assertEquals(mode + " value not set correctly for location.", modeIntValue,
                locationSettingMode);
    }

    private void verifyLocationSettingsOnOrOff(boolean verifyOn) throws Exception {
        // Set location flag
        if (verifyOn) {
            Settings.Secure.putInt(getInstrumentation().getContext().getContentResolver(),
                    Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        }
        else {
            Settings.Secure.putInt(getInstrumentation().getContext().getContentResolver(),
                    Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_ON);
        }
        dismissAlertDialogs();
        // Load location settings
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        dismissAlertDialogs();
        // Toggle UI
        mDevice.wait(Until.findObject(By.res(SETTINGS_PACKAGE, "switch_widget")), TIMEOUT).click();
        dismissAlertDialogs();
        Thread.sleep(TIMEOUT);
        // Verify change in setting
        int locationEnabled = Settings.Secure.getInt(getInstrumentation()
                 .getContext().getContentResolver(),
                 Settings.Secure.LOCATION_MODE);
        if (verifyOn) {
            assertFalse("Location not enabled correctly", locationEnabled == 0);
        }
        else {
            assertEquals("Location not disabled correctly", 0, locationEnabled);
        }
    }

    // This method dismisses both alert dialogs that might popup and
    // interfere with the test. Since the order in which the dialog
    // shows up changes in no specific known way, we're checking for
    // both dialogs in any order for a robust test. Bug b/36233151
    // filed against Location team for specifications. This is a
    // workaround in the meantime to ensure coverage.
    private void dismissAlertDialogs() throws Exception {
        for (int count = 0; count < 2; count++) {
            UiObject2 agreeDialog = mDevice.wait(Until.findObject
                    (By.text("Improve location accuracy?")), TIMEOUT);
            UiObject2 previousChoiceYesButton = mDevice.wait(Until.findObject
                    (By.text("YES")), TIMEOUT);
            if (agreeDialog != null) {
                mDevice.wait(Until.findObject
                        (By.text("AGREE")), TIMEOUT).click();
                Thread.sleep(TIMEOUT);
                assertNull("Improve location dialog not dismissed",
                        mDevice.wait(Until.findObject
                        (By.text("Improve location accuracy?")), TIMEOUT));
            }
            if (previousChoiceYesButton != null) {
                previousChoiceYesButton.click();
                // Short sleep to wait for the new screen
                Thread.sleep(TIMEOUT);
            }
        }
    }
}
