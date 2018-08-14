/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.tests;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SettingsRestoreAfterCloseTest {
    private static final String PACKAGE_SETTINGS = "com.android.settings";
    private static final int TIME_OUT = 2000;

    private boolean mAlwaysFinish;

    @Before
    public void setUp() throws Exception {
        // To make sure when we press home button, the activity will be destroyed by OS
        Context context = InstrumentationRegistry.getContext();
        mAlwaysFinish = Settings.Global.getInt(
                context.getContentResolver(), Settings.Global
                .ALWAYS_FINISH_ACTIVITIES, 0)
                != 0;

        ActivityManager.getService().setAlwaysFinish(true);
    }

    @After
    public void tearDown() throws Exception {
        ActivityManager.getService().setAlwaysFinish(mAlwaysFinish);
    }

    @Test
    public void testRtlStability_AppCloseAndReOpen_shouldNotCrash() throws Exception {

        final UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation
                ());
        uiDevice.pressHome();

        // Open the settings app
        startSettingsMainActivity(uiDevice);

        // Press home button
        uiDevice.pressHome();
        final String launcherPackage = uiDevice.getLauncherPackageName();
        uiDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), TIME_OUT);

        // Open the settings again
        startSettingsMainActivity(uiDevice);
    }

    private void startSettingsMainActivity(UiDevice uiDevice) {
        Context context = InstrumentationRegistry.getContext();
        context.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        uiDevice.wait(Until.hasObject(By.pkg(PACKAGE_SETTINGS).depth(0)), TIME_OUT);
    }
}
