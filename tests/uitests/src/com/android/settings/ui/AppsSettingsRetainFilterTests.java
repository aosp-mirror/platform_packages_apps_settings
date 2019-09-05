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

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.system.helpers.ActivityHelper;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppsSettingsRetainFilterTests {
    private static final int TIMEOUT = 2000;
    private UiDevice mDevice;
    private ActivityHelper mActivityHelper = null;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mActivityHelper = ActivityHelper.getInstance();

        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientation", e);
        }

        mDevice.pressHome();
        mDevice.waitForIdle(TIMEOUT);
    }

    @Test
    public void testDisablingSystemAppAndRotateDevice() throws Exception {
        launchAppsSettings();

        UiObject2 calculator = mDevice.wait(
                Until.findObject(By.text("Calculator")), TIMEOUT);
        assertThat(calculator).isNotNull();
        calculator.click();
        mDevice.waitForIdle(TIMEOUT);

        UiObject2 disableButton = mDevice.wait(
                Until.findObject(By.text("DISABLE")), TIMEOUT);
        assertThat(disableButton).isNotNull();
        disableButton.click();
        mDevice.waitForIdle(TIMEOUT);

        // Click on "Disable App" on dialog.
        UiObject2 dialogDisableButton = mDevice.wait(
                Until.findObject(By.text("DISABLE APP")), TIMEOUT);
        assertThat(dialogDisableButton).isNotNull();
        dialogDisableButton.click();
        mDevice.waitForIdle(TIMEOUT);

        UiObject2 enableButton = mDevice.wait(
                Until.findObject(By.text("ENABLE")), TIMEOUT);
        assertThat(enableButton).isNotNull();

        mDevice.pressBack();
        mDevice.waitForIdle(TIMEOUT);

        UiObject2 spinnerHeader =  mDevice.wait(
                Until.findObject(By.text("All apps")), TIMEOUT);
        assertThat(spinnerHeader).isNotNull();
        spinnerHeader.click();

        UiObject2 optionDisabledApps =  mDevice.wait(
                Until.findObject(By.text("Disabled apps")), TIMEOUT);
        assertThat(optionDisabledApps).isNotNull();
        optionDisabledApps.click();
        mDevice.waitForIdle(TIMEOUT);

        try {
            mDevice.setOrientationLeft();
            mDevice.waitForIdle(TIMEOUT);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to freeze device orientation", e);
        }

        try {
            mDevice.unfreezeRotation();
            mDevice.waitForIdle(TIMEOUT);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to un-freeze device orientation", e);
        }

        UiObject2 spinnerDisabledApps =  mDevice.wait(
                Until.findObject(By.text("Disabled apps")), TIMEOUT);
        assertThat(spinnerDisabledApps).isNotNull();
    }

    private void launchAppsSettings() throws Exception {
        Intent appsSettingsIntent = new
                Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
        mActivityHelper.launchIntent(appsSettingsIntent);
    }
}
