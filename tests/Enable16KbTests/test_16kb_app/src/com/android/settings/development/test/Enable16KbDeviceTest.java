/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.development.test;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.system.helpers.SettingsHelper;

import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class Enable16KbDeviceTest {
    private static final long TIMEOUT = 2000;

    private static final String ENABLE_16K_TOGGLE = "Boot with 16 KB page size";
    private static final String BUILD_NUMBER = "Build number";
    private static final String USE_DEVELOPER_OPTIONS = "Use developer options";
    private static final String EXT4_CONFIRMATION = "Erase all data";
    private static final String EXT4_TITLE = "Reformat device to ext4? (required for 16 KB mode)";
    private static final String TOGGLE_16K_TITLE = "Switch from 4 KB mode to 16 KB mode";
    private static final String TOGGLE_4K_TITLE = "Switch from 16 KB mode to 4 KB mode";
    private static final String ANDROID_WIDGET_SCROLLVIEW = "android.widget.ScrollView";
    private static final String OKAY = "OK";
    private static final String NOTIFICATION_TITLE_4K = "Using 4 KB page-agnostic mode";
    private static final String NOTIFICATION_TITLE_16K = "Using 16 KB page-agnostic mode";

    private Context mContext;
    private UiDevice mDevice;
    private SettingsHelper mHelper;

    @Before
    public void setUp() throws Exception {
        mContext = getInstrumentation().getTargetContext();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mHelper = SettingsHelper.getInstance();
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientation", e);
        }

        mDevice.executeShellCommand("am start -a com.android.setupwizard.FOUR_CORNER_EXIT");
        mDevice.waitForWindowUpdate(null, TIMEOUT);
        mDevice.executeShellCommand("input keyevent KEYCODE_WAKEUP");
        mDevice.executeShellCommand("wm dismiss-keyguard");
    }

    private void unlockDeveloperOptions() throws Exception {
        SettingsHelper.launchSettingsPage(mContext, Settings.ACTION_DEVICE_INFO_SETTINGS);
        // Click 7 times on build number to unlock the dev options
        for (int i = 0; i < 7; i++) {
            mHelper.clickSetting(BUILD_NUMBER);
        }
    }

    @Test
    public void enable16k_switchToExt4() throws Exception {
        unlockDeveloperOptions();
        SettingsHelper.launchSettingsPage(
                mContext, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        clickOnObject(By.text(ENABLE_16K_TOGGLE));

        // Verify that ext4 toggle is visible
        verifyTextOnScreen(EXT4_TITLE);

        UiObject2 confirmationObject =
            mDevice.wait(Until.findObject(By.text(EXT4_CONFIRMATION)), TIMEOUT);
        if (confirmationObject == null) {
            // Workaround for (b/390535191). AOSP targets display the string in all caps.
            confirmationObject = mDevice.wait(
                Until.findObject(By.text(EXT4_CONFIRMATION.toUpperCase(Locale.ROOT))), TIMEOUT);
        }
        assertTrue(confirmationObject != null);
        confirmationObject.click();
    }

    @Test
    public void enable16k_switchTo16Kb() throws Exception {
        // Device will be in 4kb mode
        openPersistentNotification(NOTIFICATION_TITLE_4K);
        unlockDeveloperOptions();
        SettingsHelper.launchSettingsPage(
                mContext, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);

        clickOnObject(By.text(ENABLE_16K_TOGGLE));
        // Verify that text is displayed to switch to 16kb
        verifyTextOnScreen(TOGGLE_16K_TITLE);

        mDevice.wait(Until.findObject(By.text(OKAY)), TIMEOUT).click();
    }

    @Test
    public void enable16k_switchTo4Kb() throws Exception {
        // Device will be in 16kb mode
        openPersistentNotification(NOTIFICATION_TITLE_16K);
        SettingsHelper.launchSettingsPage(
                mContext, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);

        clickOnObject(By.text(ENABLE_16K_TOGGLE));
        //  Verify that text is displayed to switch to 4kb
        verifyTextOnScreen(TOGGLE_4K_TITLE);

        mDevice.wait(Until.findObject(By.text(OKAY)), TIMEOUT).click();
    }

    private void clickOnObject(BySelector target) {
        mDevice.waitForWindowUpdate(null, TIMEOUT);
        UiObject2 scrollView =
                mDevice.wait(
                        Until.findObject(By.scrollable(true).clazz(ANDROID_WIDGET_SCROLLVIEW)),
                        TIMEOUT);
        UiObject2 targetObject = scrollTo(scrollView, target, Direction.DOWN);
        assertTrue(targetObject != null);
        targetObject.click();
    }

    private UiObject2 scrollTo(UiObject2 scrollable, BySelector target, Direction direction) {
        while (!mDevice.hasObject(target) && scrollable.scroll(direction, 1.0f)) {
            // continue
        }
        if (!mDevice.hasObject(target)) {
            scrollable.scroll(direction, 1.0f);
        }
        return mDevice.findObject(target);
    }

    private void verifyTextOnScreen(String displayedText) {
        UiObject2 targetObject = mDevice.wait(Until.findObject(By.text(displayedText)), TIMEOUT);
        assertTrue(targetObject != null);
    }

    private void openPersistentNotification(String title) {
        mDevice.openNotification();
        mDevice.waitForWindowUpdate(null, TIMEOUT);
        verifyTextOnScreen(title);
        mDevice.wait(Until.findObject(By.text(title)), TIMEOUT).click();
        mDevice.waitForWindowUpdate(null, TIMEOUT);
        verifyTextOnScreen(title);
        mDevice.wait(Until.findObject(By.text(OKAY)), TIMEOUT).click();
        mDevice.waitForWindowUpdate(null, TIMEOUT);
    }

    @Test
    public void enable16k_disableDeveloperOption() throws Exception {
        // Device will be in 4KB mode when this test will be run
        SettingsHelper.launchSettingsPage(
                mContext, Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        mDevice.wait(Until.findObject(By.text(USE_DEVELOPER_OPTIONS)), TIMEOUT).click();
        verifyTextOnScreen(NOTIFICATION_TITLE_4K);
        mDevice.wait(Until.findObject(By.text(OKAY)), TIMEOUT).click();
    }
}
