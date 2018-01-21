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

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;
import android.provider.Settings;
import android.support.test.metricshelper.MetricsAsserts;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.system.helpers.SettingsHelper;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;

import android.metrics.MetricsReader;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class AccessibilitySettingsTests extends InstrumentationTestCase {

    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final int TIMEOUT = 2000;
    private UiDevice mDevice;
    private MetricsReader mMetricsReader;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientaion", e);
        }
        mMetricsReader = new MetricsReader();
        // Clear out old logs
        mMetricsReader.checkpoint();
    }

    @Override
    protected void tearDown() throws Exception {
        // Need to finish settings activity
        mDevice.pressBack();
        mDevice.pressHome();
        mDevice.waitForIdle();
        super.tearDown();
    }

    @Presubmit
    @MediumTest
    public void testHighContrastTextOn() throws Exception {
        verifyAccessibilitySettingOnOrOff("High contrast text",
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, 0, 1);
    }

    @Presubmit
    @MediumTest
    public void testHighContrastTextOff() throws Exception {
        verifyAccessibilitySettingOnOrOff("High contrast text",
               Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, 1, 0);
    }

    @Presubmit
    @MediumTest
    public void testPowerButtonEndsCallOn() throws Exception {
        verifyAccessibilitySettingOnOrOff("Power button ends call",
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, 1, 2);
    }

    @Presubmit
    @MediumTest
    public void testPowerButtonEndsCallOff() throws Exception {
        verifyAccessibilitySettingOnOrOff("Power button ends call",
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, 2, 1);
    }

    /* Suppressing these four tests. The settings don't play
     * nice with Settings.System.putInt or Settings.Secure.putInt.
     * Need further clarification. Filed bug b/27792029
     */
    @Suppress
    @MediumTest
    public void testAutoRotateScreenOn() throws Exception {
        verifyAccessibilitySettingOnOrOff("Auto-rotate screen",
               Settings.System.ACCELEROMETER_ROTATION, 0, 1);
    }

    @Suppress
    @MediumTest
    public void testAutoRotateScreenOff() throws Exception {
       verifyAccessibilitySettingOnOrOff("Auto-rotate screen",
               Settings.System.ACCELEROMETER_ROTATION, 1, 0);
    }

    @Suppress
    @MediumTest
    public void testMonoAudioOn() throws Exception {
        verifyAccessibilitySettingOnOrOff("Mono audio",
               Settings.System.MASTER_MONO, 0, 1);
    }

    @Suppress
    @MediumTest
    public void testMonoAudioOff() throws Exception {
         verifyAccessibilitySettingOnOrOff("Mono audio",
                Settings.System.MASTER_MONO, 1, 0);
    }

    @Presubmit
    @MediumTest
    public void testLargeMousePointerOn() throws Exception {
         verifyAccessibilitySettingOnOrOff("Large mouse pointer",
                 Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, 0, 1);
    }

    @Presubmit
    @MediumTest
    public void testLargeMousePointerOff() throws Exception {
         verifyAccessibilitySettingOnOrOff("Large mouse pointer",
                 Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, 1, 0);
    }

    @Presubmit
    @MediumTest
    public void testColorCorrection() throws Exception {
        verifySettingToggleAfterScreenLoad("Color correction",
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED);
        MetricsAsserts.assertHasVisibilityLog("Missing color correction log",
                mMetricsReader, MetricsEvent.ACCESSIBILITY_TOGGLE_DALTONIZER, true);
    }

    // Suppressing this test, since UiAutomator + talkback don't play nice
    @Suppress
    @MediumTest
    public void testTalkback() throws Exception {
        verifySettingToggleAfterScreenLoad("TalkBack",
                Settings.Secure.ACCESSIBILITY_ENABLED);
    }

    @Presubmit
    @MediumTest
    public void testCaptions() throws Exception {
         verifySettingToggleAfterScreenLoad("Captions",
                 Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED);
        MetricsAsserts.assertHasVisibilityLog("Missing captions log",
                mMetricsReader, MetricsEvent.ACCESSIBILITY_CAPTION_PROPERTIES, true);
    }

    @Presubmit
    @MediumTest
    public void testMagnificationGesture() throws Exception {
        verifySettingToggleAfterScreenLoad("Magnification", "Magnify with triple-tap",
                 Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
        MetricsAsserts.assertHasVisibilityLog("Missing magnification log",
                mMetricsReader, MetricsEvent.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION, true);
    }

    @MediumTest
    public void testClickAfterPointerStopsMoving() throws Exception {
         verifySettingToggleAfterScreenLoad("Click after pointer stops moving",
                  Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED);
    }

    @MediumTest
    public void testAccessibilitySettingsLoadLog() throws Exception {
        launchAccessibilitySettings();
        MetricsAsserts.assertHasVisibilityLog("Missing accessibility settings load log",
                mMetricsReader, MetricsEvent.ACCESSIBILITY, true);
    }

    public void launchAccessibilitySettings() throws Exception {
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    private void verifyAccessibilitySettingOnOrOff(String settingText,
            String settingFlag, int initialFlagValue, int expectedFlagValue)
            throws Exception {
        Settings.Secure.putInt(getInstrumentation().getContext().getContentResolver(),
                settingFlag, initialFlagValue);
        launchAccessibilitySettings();
        UiObject2 settingsTitle = findItemOnScreen(settingText);
        settingsTitle.click();
        Thread.sleep(TIMEOUT);
        int settingValue = Settings.Secure
                .getInt(getInstrumentation().getContext().getContentResolver(), settingFlag);
        assertEquals(settingText + " not correctly set after toggle",
                expectedFlagValue, settingValue);
    }

    private void verifySettingToggleAfterScreenLoad(String settingText, String settingFlag)
            throws Exception {
        verifySettingToggleAfterScreenLoad(settingText, null, settingFlag);
    }

    private void verifySettingToggleAfterScreenLoad
            (String settingText, String subSetting, String settingFlag) throws Exception {
        // Load accessibility settings
        launchAccessibilitySettings();
        Settings.Secure.putInt(getInstrumentation().getContext().getContentResolver(),
                settingFlag, 0);
        Thread.sleep(TIMEOUT);
        // Tap on setting required
        UiObject2 settingTitle = findItemOnScreen(settingText);
        // Load screen
        settingTitle.click();
        Thread.sleep(TIMEOUT);
        if (subSetting != null) {
            UiObject2 subSettingObject = findItemOnScreen(subSetting);
            subSettingObject.click();
            Thread.sleep(TIMEOUT);
        }
        // Toggle value
        UiObject2 settingToggle =  mDevice.wait(Until.findObject(By.text("Off")),
                            TIMEOUT);
        settingToggle.click();
        dismissOpenDialog();
        Thread.sleep(TIMEOUT);
        // Assert new value
        int settingValue = Settings.Secure.
                getInt(getInstrumentation().getContext().getContentResolver(), settingFlag);
        assertEquals(settingText + " value not set correctly", 1, settingValue);
        // Toogle value
        settingToggle.click();
        dismissOpenDialog();
        mDevice.pressBack();
        Thread.sleep(TIMEOUT);
        // Assert reset to old value
        settingValue = Settings.Secure.
                getInt(getInstrumentation().getContext().getContentResolver(), settingFlag);
        assertEquals(settingText + " value not set correctly", 0, settingValue);
    }

    private UiObject2 findItemOnScreen(String item) throws Exception {
        int count = 0;
        UiObject2 settingsPanel = mDevice.wait(Until.findObject
                (By.res(SETTINGS_PACKAGE, "list")), TIMEOUT);
        while (settingsPanel.fling(Direction.UP) && count < 3) {
            count++;
        }
        count = 0;
        UiObject2 setting = null;
        while(count < 3 && setting == null) {
            setting = mDevice.wait(Until.findObject(By.text(item)), TIMEOUT);
            if (setting == null) {
                settingsPanel.scroll(Direction.DOWN, 1.0f);
            }
            count++;
        }
        return setting;
    }

    private void dismissOpenDialog() throws Exception {
        UiObject2 okButton = mDevice.wait(Until.findObject
                (By.res("android:id/button1")), TIMEOUT*2);
        if (okButton != null) {
            okButton.click();
        }
    }
}
