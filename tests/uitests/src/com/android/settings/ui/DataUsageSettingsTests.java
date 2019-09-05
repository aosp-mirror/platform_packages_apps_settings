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
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.system.helpers.SettingsHelper;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

public class DataUsageSettingsTests extends InstrumentationTestCase {

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
        // Need to finish settings activity
        mDevice.pressBack();
        mDevice.pressHome();
        super.tearDown();
    }

    @MediumTest
    public void testElementsOnDataUsageScreen() throws Exception {
        launchDataUsageSettings();
        assertNotNull("Data usage element not found",
                mDevice.wait(Until.findObject(By.text("Usage")),
                TIMEOUT));
        assertNotNull("Data usage bar not found",
                mDevice.wait(Until.findObject(By.res(SETTINGS_PACKAGE,
                "color_bar")), TIMEOUT));
        assertNotNull("WiFi Data usage element not found",
                mDevice.wait(Until.findObject(By.text("Wi-Fi data usage")),
                TIMEOUT));
        assertNotNull("Network restrictions element not found",
                mDevice.wait(Until.findObject(By.text("Network restrictions")),
                TIMEOUT));
    }

    public void launchDataUsageSettings() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_SETTINGS);
        mDevice.wait(Until
                .findObject(By.text("Network & Internet")), TIMEOUT)
                .click();
        Thread.sleep(TIMEOUT * 2);
        assertNotNull("Network & internet screen not loaded", mDevice.wait(
                Until.findObject(By.text("Data usage")), TIMEOUT));
        mDevice.wait(Until
                .findObject(By.text("Data usage")), TIMEOUT)
                .click();
    }
}
