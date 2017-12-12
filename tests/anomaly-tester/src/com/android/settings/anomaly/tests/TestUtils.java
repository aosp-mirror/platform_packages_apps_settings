/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.anomaly.tests;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

public class TestUtils {
    private static final String PACKAGE_NAME = "com.android.settings.anomaly.tester";
    private static final long TIME_OUT = 3000;

    /**
     * This method set up the environment for anomaly test
     *
     * @param instrumentation to execute command
     */
    public static void setUp(Instrumentation instrumentation) {
        final UiAutomation uiAutomation = instrumentation.getUiAutomation();
        // pretend unplug and screen off, also reset the battery stats
        uiAutomation.executeShellCommand("dumpsys battery unplug");
        uiAutomation.executeShellCommand("dumpsys batterystats enable pretend-screen-off");
        uiAutomation.executeShellCommand("dumpsys batterystats --reset");
    }

    /**
     * This method cleans up all the commands in {@link #setUp(Instrumentation)}
     *
     * @param instrumentation to execute command
     */
    public static void tearDown(Instrumentation instrumentation) {
        final UiAutomation uiAutomation = instrumentation.getUiAutomation();
        // reset unplug and screen-off
        uiAutomation.executeShellCommand("dumpsys battery reset");
        uiAutomation.executeShellCommand("dumpsys batterystats disable pretend-screen-off");
    }

    public static void startAnomalyApp(Context context, UiDevice uiDevice) {
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        uiDevice.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIME_OUT);
    }

    /**
     * Find {@link android.widget.EditText} with {@code res} and set its {@code value}
     */
    public static void setEditTextWithValue(UiDevice uiDevice, String res, long value) {
        final UiObject2 editText = uiDevice.findObject(By.res(res));
        assertWithMessage("Cannot find editText with res: " + res).that(editText).isNotNull();
        editText.setText(String.valueOf(value));
    }

    /**
     * Find {@link android.widget.Button} with {@code res} and click it
     */
    public static void clickButton(UiDevice uiDevice, String res) {
        final UiObject2 button = uiDevice.findObject(By.res(res));
        assertWithMessage("Cannot find button with res: " + res).that(button).isNotNull();
        button.click();
    }

    /**
     * Make {@link UiDevice} wait for {@code timeMs}
     *
     * @see Thread#sleep(long)
     */
    public static void wait(UiDevice uiDevice, long timeMs) throws InterruptedException {
        uiDevice.waitForIdle();
        Thread.sleep(timeMs);
    }
}
