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

import android.content.ContentResolver;
import android.platform.test.annotations.Presubmit;
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.system.helpers.SettingsHelper;
import android.system.helpers.SettingsHelper.SettingsType;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;

import java.util.regex.Pattern;

public class DisplaySettingsTest extends InstrumentationTestCase {

    private static final String PAGE = Settings.ACTION_DISPLAY_SETTINGS;
    private static final int TIMEOUT = 2000;
    private static final FontSetting FONT_SMALL = new FontSetting("Small", 0.85f);
    private static final FontSetting FONT_NORMAL = new FontSetting("Default", 1.00f);
    private static final FontSetting FONT_LARGE = new FontSetting("Large", 1.15f);
    private static final FontSetting FONT_HUGE = new FontSetting("Largest", 1.30f);

    private UiDevice mDevice;
    private ContentResolver mResolver;
    private SettingsHelper mHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mDevice.setOrientationNatural();
        mResolver = getInstrumentation().getContext().getContentResolver();
        mHelper = new SettingsHelper();
    }

    @Override
    public void tearDown() throws Exception {
        // reset settings we touched that may impact others
        Settings.System.putFloat(mResolver, Settings.System.FONT_SCALE, 1.00f);
        mDevice.waitForIdle();
        super.tearDown();
    }

    @Presubmit
    @MediumTest
    public void testAdaptiveBrightness() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        Thread.sleep(1000);

        assertTrue(mHelper.verifyToggleSetting(SettingsType.SYSTEM, PAGE, "Adaptive brightness",
                Settings.System.SCREEN_BRIGHTNESS_MODE));
        assertTrue(mHelper.verifyToggleSetting(SettingsType.SYSTEM, PAGE, "Adaptive brightness",
                Settings.System.SCREEN_BRIGHTNESS_MODE));
    }


    // blocked on b/27487224
    @MediumTest
    @Suppress
    public void testDaydreamToggle() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        clickMore();
        Pattern p = Pattern.compile("On|Off");
        mHelper.clickSetting("Screen saver");
        Thread.sleep(1000);
        try {
            assertTrue(mHelper.verifyToggleSetting(SettingsType.SECURE, PAGE, p,
                    Settings.Secure.SCREENSAVER_ENABLED, false));
            assertTrue(mHelper.verifyToggleSetting(SettingsType.SECURE, PAGE, p,
                    Settings.Secure.SCREENSAVER_ENABLED, false));
        } finally {
            mDevice.pressBack();
        }
    }

    @MediumTest
    public void testAccelRotation() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        clickMore();
        Thread.sleep(4000);
        int currentAccelSetting = Settings.System.getInt(
                mResolver, Settings.System.ACCELEROMETER_ROTATION);
        mHelper.clickSetting("Auto-rotate screen");
        int newAccelSetting = Settings.System.getInt(
                mResolver, Settings.System.ACCELEROMETER_ROTATION);
        assertTrue("Accelorometer setting unchanged after toggle", currentAccelSetting != newAccelSetting);
        mHelper.clickSetting("Auto-rotate screen");
        int revertedAccelSetting = Settings.System.getInt(
                mResolver, Settings.System.ACCELEROMETER_ROTATION);
        assertTrue("Accelorometer setting unchanged after toggle", revertedAccelSetting != newAccelSetting);
    }

    @MediumTest
    public void testDaydream() throws Exception {
        Settings.Secure.putInt(mResolver, Settings.Secure.SCREENSAVER_ENABLED, 1);
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        clickMore();
        mHelper.scrollVert(false);
        mDevice.wait(Until.findObject(By.text("Screen saver")), TIMEOUT).click();
        try {
            assertTrue(mHelper.verifyRadioSetting(SettingsType.SECURE, PAGE,
                    "Current screen saver", "Clock", Settings.Secure.SCREENSAVER_COMPONENTS,
                    "com.google.android.deskclock/com.android.deskclock.Screensaver"));
            assertTrue(mHelper.verifyRadioSetting(SettingsType.SECURE, PAGE,
                    "Current screen saver", "Colors", Settings.Secure.SCREENSAVER_COMPONENTS,
                    "com.android.dreams.basic/com.android.dreams.basic.Colors"));
            assertTrue(mHelper.verifyRadioSetting(SettingsType.SECURE, PAGE,
                    "Current screen saver", "Photos", Settings.Secure.SCREENSAVER_COMPONENTS,
                    "com.google.android.apps.photos/com.google.android.apps.photos.daydream"
                            + ".PhotosDreamService"));
        } finally {
            mDevice.pressBack();
            Thread.sleep(2000);
        }
    }

    @Presubmit
    @MediumTest
    public void testSleep15Seconds() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        assertTrue(mHelper.verifyRadioSetting(SettingsType.SYSTEM, PAGE,
                "Sleep", "15 seconds", Settings.System.SCREEN_OFF_TIMEOUT, "15000"));
    }

    @MediumTest
    public void testSleep30Seconds() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        assertTrue(mHelper.verifyRadioSetting(SettingsType.SYSTEM, PAGE,
                "Sleep", "30 seconds", Settings.System.SCREEN_OFF_TIMEOUT, "30000"));
    }

    @MediumTest
    public void testSleep1Minute() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        assertTrue(mHelper.verifyRadioSetting(SettingsType.SYSTEM, PAGE,
                "Sleep", "1 minute", Settings.System.SCREEN_OFF_TIMEOUT, "60000"));
    }

    @MediumTest
    public void testSleep2Minutes() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        assertTrue(mHelper.verifyRadioSetting(SettingsType.SYSTEM, PAGE,
                "Sleep", "2 minutes", Settings.System.SCREEN_OFF_TIMEOUT, "120000"));
    }

    @MediumTest
    public void testSleep5Minutes() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        assertTrue(mHelper.verifyRadioSetting(SettingsType.SYSTEM, PAGE,
                "Sleep", "5 minutes", Settings.System.SCREEN_OFF_TIMEOUT, "300000"));
    }

    @MediumTest
    public void testSleep10Minutes() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        assertTrue(mHelper.verifyRadioSetting(SettingsType.SYSTEM, PAGE,
                "Sleep", "10 minutes", Settings.System.SCREEN_OFF_TIMEOUT, "600000"));
    }

    @MediumTest
    public void testSleep30Minutes() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        mHelper.scrollVert(true);
        assertTrue(mHelper.verifyRadioSetting(SettingsType.SYSTEM, PAGE,
                "Sleep", "30 minutes", Settings.System.SCREEN_OFF_TIMEOUT, "1800000"));
    }

    @Presubmit
    @MediumTest
    public void testFontSizeLarge() throws Exception {
        verifyFontSizeSetting(1.00f, FONT_LARGE);
        // Leaving the font size at large can make later tests fail, so reset it
        Settings.System.putFloat(mResolver, Settings.System.FONT_SCALE, 1.00f);
        // It takes a second for the new font size to be picked up
        Thread.sleep(2000);
    }

    @MediumTest
    public void testFontSizeDefault() throws Exception {
        verifyFontSizeSetting(1.15f, FONT_NORMAL);
    }

    @MediumTest
    public void testFontSizeLargest() throws Exception {
        verifyFontSizeSetting(1.00f, FONT_HUGE);
        // Leaving the font size at huge can make later tests fail, so reset it
        Settings.System.putFloat(mResolver, Settings.System.FONT_SCALE, 1.00f);
        // It takes a second for the new font size to be picked up
        Thread.sleep(2000);
    }

    @MediumTest
    public void testFontSizeSmall() throws Exception {
        verifyFontSizeSetting(1.00f, FONT_SMALL);
    }

    private void verifyFontSizeSetting(float resetValue, FontSetting setting)
            throws Exception {
        Settings.System.putFloat(mResolver, Settings.System.FONT_SCALE, resetValue);
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(), PAGE);
        clickMore();
        mHelper.clickSetting("Font size");
        try {
            mDevice.wait(Until.findObject(By.desc(setting.getName())), TIMEOUT).click();
            Thread.sleep(1000);
            float changedValue = Settings.System.getFloat(
                    mResolver, Settings.System.FONT_SCALE);
            assertEquals(setting.getSize(), changedValue, 0.0001);
        } finally {
            // Make sure to back out of the font menu
            mDevice.pressBack();
        }
    }

    private void clickMore() throws InterruptedException {
        UiObject2 more = mDevice.wait(Until.findObject(By.text("Advanced")), TIMEOUT);
        if (more != null) {
            more.click();
            Thread.sleep(TIMEOUT);
        }
    }

    private static class FontSetting {
        private final String mSizeName;
        private final float mSizeVal;

        public FontSetting(String sizeName, float sizeVal) {
            mSizeName = sizeName;
            mSizeVal = sizeVal;
        }

        public String getName() {
            return mSizeName;
        }

        public float getSize() {
            return mSizeVal;
        }
    }
}
