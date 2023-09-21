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
import android.system.helpers.ActivityHelper;
import android.system.helpers.SettingsHelper;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.widget.ListView;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Ignore;

/** Verifies that you can get to the notification app listing page from the apps & notifications
 * page */
@Ignore
public class NotificationSettingsTests extends InstrumentationTestCase {
    private static final boolean LOCAL_LOGV = false;
    private static final String TAG = "NotifiSettingsTests";
    private static final int TIMEOUT = 2000;
    private ActivityHelper mActivityHelper = null;
    private SettingsHelper mSettingsHelper = null;

    private UiDevice mDevice;
    @Override
    public void setUp() throws Exception {
        if (LOCAL_LOGV) {
            Log.d(TAG, "-------");
        }
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mActivityHelper = ActivityHelper.getInstance();
        mSettingsHelper = SettingsHelper.getInstance();
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to freeze device orientaion", e);
        }

        // make sure we are in a clean state before starting the test
        mDevice.pressHome();
        Thread.sleep(TIMEOUT * 2);
        launchAppsSettings();
    }

    @Override
    protected void tearDown() throws Exception {
        mDevice.pressBack();
        mDevice.pressHome(); // finish settings activity
        mDevice.waitForIdle(TIMEOUT * 2); // give UI time to finish animating
        super.tearDown();
    }

    @MediumTest
    public void testNotificationsSettingsListForCalculator() {
        UiObject2 configureNotifications = mDevice.wait(
                Until.findObject(By.text("Notifications")), TIMEOUT);
        configureNotifications.click();
        mDevice.wait(Until.findObject(By.text("Blink light")), TIMEOUT);
        UiObject2 appNotifications = mDevice.wait(
                Until.findObject(By.text("On for all apps")), TIMEOUT);
        appNotifications.click();
        UiObject2 view =
                mDevice.wait(
                        Until.findObject(By.text("All apps")), TIMEOUT);
        assertNotNull("Could not find Settings > Apps screen", view);
        UiObject2 app = mDevice.wait(Until.findObject(By.text("Calculator")), TIMEOUT);
        assertNotNull("Could not find Calculator notification settings", app);
    }


    @MediumTest
    public void testNotificationsSettingsListForPhone() {
        UiObject2 configureNotifications = mDevice.wait(
                Until.findObject(By.text("Notifications")), TIMEOUT);
        configureNotifications.click();
        mDevice.wait(Until.findObject(By.text("Blink light")), TIMEOUT);
        UiObject2 appNotifications = mDevice.wait(
                Until.findObject(By.text("On for all apps")), TIMEOUT);
        appNotifications.click();
        UiObject2 view =
                mDevice.wait(
                        Until.findObject(By.text("All apps")), TIMEOUT);
        assertNotNull("Could not find Settings > Apps screen", view);

        final BySelector preferenceListSelector = By.clazz(ListView.class).res("android:id/list");
        UiObject2 apps = mDevice.wait(Until.findObject(preferenceListSelector), TIMEOUT);

        UiObject2 phone = scrollTo(mDevice, apps, By.text("Phone"), Direction.DOWN);
        assertNotNull("Could not find Phone notification settings", phone);
        phone.click();
        UiObject2 incomingCalls = mDevice.wait(Until.findObject(By.text("Incoming calls")), TIMEOUT);
        assertNotNull("Could not find incoming calls channel", incomingCalls);
        incomingCalls.click();

        // here's the meat of this test: make sure that you cannot change
        // most settings for this channel

        UiObject2 importance = mDevice.wait(Until.findObject(By.text("Importance")), TIMEOUT);
        assertNotNull("Could not find importance toggle", importance);
        assertFalse(importance.isEnabled());
        assertFalse(mDevice.wait(Until.findObject(By.text("Sound")), TIMEOUT).isEnabled());;
        assertFalse(mDevice.wait(Until.findObject(By.text("Vibrate")), TIMEOUT).isEnabled());
        assertFalse(mDevice.wait(Until.findObject(By.text("Override Do Not Disturb")), TIMEOUT).isEnabled());






    }

    private UiObject2 scrollTo(UiDevice device, UiObject2 scrollable,
                                          BySelector target, Direction direction) {
        while (!device.hasObject(target) && scrollable.scroll(direction, 1.0f)) {
            // continue
        }
        if (!device.hasObject(target)) {
            // Scroll once more if not found; in some cases UiObject2.scroll can return false when
            // the last item is not fully visible yet for list views.
            scrollable.scroll(direction, 1.0f);
        }
        return device.findObject(target);
    }


    private void launchAppsSettings() throws Exception {
        Intent appsSettingsIntent = new Intent(Settings.ACTION_SETTINGS);
        mActivityHelper.launchIntent(appsSettingsIntent);
        mSettingsHelper.flingSettingsToStart();
        UiObject2 view = mDevice.wait(
                Until.findObject(By.text("Apps & notifications")), TIMEOUT);
        view.click();
        UiObject2 title = mDevice.wait(
                Until.findObject(By.text("Apps & notifications")), TIMEOUT);
        assertNotNull("Could not find Settings > Apps & notifications screen", title);
    }
}
