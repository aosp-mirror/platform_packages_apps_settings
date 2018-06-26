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

import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.system.helpers.ActivityHelper;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

/** Verifies basic functionality of the About Phone screen */
public class AppsSettingsTests extends InstrumentationTestCase {
    private static final boolean LOCAL_LOGV = false;
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final String TAG = "AboutPhoneSettingsTest";
    private static final int TIMEOUT = 2000;
    private ActivityHelper mActivityHelper = null;

    private UiDevice mDevice;

    private static final String[] sResourceTexts = {
        "Storage",
        "Data usage",
        "Permissions",
        "App notifications",
        "Open by default",
        "Battery",
        "Memory"
    };

    @Override
    public void setUp() throws Exception {
        if (LOCAL_LOGV) {
            Log.d(TAG, "-------");
        }
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mActivityHelper = ActivityHelper.getInstance();
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to freeze device orientaion", e);
        }

        // make sure we are in a clean state before starting the test
        mDevice.pressHome();
        Thread.sleep(TIMEOUT * 2);
        launchAppsSettings();
        UiObject2 view =
                mDevice.wait(
                        Until.findObject(By.text("All apps")), TIMEOUT);
        assertNotNull("Could not find Settings > Apps screen", view);
    }

    @Override
    protected void tearDown() throws Exception {
        mDevice.pressBack();
        mDevice.pressHome(); // finish settings activity
        mDevice.waitForIdle(TIMEOUT * 2); // give UI time to finish animating
        super.tearDown();
    }

    @MediumTest
    public void testAppSettingsListForCalculator() {
        UiObject2 calculator = mDevice.wait(
                Until.findObject(By.text("Calculator")), TIMEOUT);
        calculator.click();
        for (String setting : sResourceTexts) {
            UiObject2 appSetting =
                mDevice.wait(
                        Until.findObject(By.text(setting)), TIMEOUT);
            assertNotNull("Missing setting for Calculator: " + setting, appSetting);
            appSetting.scroll(Direction.DOWN, 10.0f);
        }
    }

    @MediumTest
    public void testDisablingAndEnablingSystemApp() throws Exception {
        launchAppsSettings();
        UiObject2 calculator = mDevice.wait(
                Until.findObject(By.text("Calculator")), TIMEOUT);
        calculator.click();
        mDevice.waitForIdle(TIMEOUT);
        UiObject2 appInfoList = mDevice.wait(
            Until.findObject(By.res(SETTINGS_PACKAGE, "list")), TIMEOUT);
        appInfoList.scroll(Direction.DOWN, 100.0f);
        UiObject2 disableButton = mDevice.wait(
                Until.findObject(By.text("DISABLE")), TIMEOUT);
        disableButton.click();
        mDevice.waitForIdle(TIMEOUT);
        // Click on "Disable App" on dialog.
        mDevice.wait(
                Until.findObject(By.text("DISABLE APP")), TIMEOUT).click();
        mDevice.waitForIdle(TIMEOUT);
        UiObject2 enableButton = mDevice.wait(
                Until.findObject(By.text("ENABLE")), TIMEOUT);
        assertNotNull("App not disabled successfully", enableButton);
        enableButton.click();
        mDevice.waitForIdle(TIMEOUT);
        disableButton = mDevice.wait(
                Until.findObject(By.text("DISABLE")), TIMEOUT);
        assertNotNull("App not enabled successfully", disableButton);
    }

    private void launchAppsSettings() throws Exception {
        Intent appsSettingsIntent = new
                Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
        mActivityHelper.launchIntent(appsSettingsIntent);
    }
}
