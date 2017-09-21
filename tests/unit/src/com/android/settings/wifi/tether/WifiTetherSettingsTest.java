/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.google.common.truth.Truth.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import com.android.settings.Settings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WifiTetherSettingsTest {

    private static final long TIMEOUT = 2000L;

    private Instrumentation mInstrumentation;
    private Intent mTetherActivityIntent;
    private UiDevice mDevice;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mDevice = UiDevice.getInstance(mInstrumentation);
        mTetherActivityIntent = new Intent()
                .setClassName(mInstrumentation.getTargetContext().getPackageName(),
                        Settings.TetherSettingsActivity.class.getName())
                .setPackage(mInstrumentation.getTargetContext().getPackageName());
    }

    @After
    public void tearDown() {
        mDevice.pressHome();
    }

    @Test
    public void verifyPageIsDisabledByDefault() {
        assertThat(WifiTetherSettings.isTetherSettingPageEnabled()).isFalse();
    }

    @Test
    public void launchTetherSettings_shouldHaveAllFields() {
        launchWifiTetherActivity();
        onView(withText("Network name")).check(matches(isDisplayed()));
        onView(withText("Password")).check(matches(isDisplayed()));
        onView(withText("Select AP Band")).check(matches(isDisplayed()));
    }

    private void launchWifiTetherActivity() {
        mInstrumentation.startActivitySync(mTetherActivityIntent);
        onView(withText("Portable Wi‑Fi hotspot")).perform();
        UiObject2 item = mDevice.wait(Until.findObject(By.text("Portable Wi‑Fi hotspot")), TIMEOUT);
        item.click();
    }
}
