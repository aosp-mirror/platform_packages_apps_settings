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

package com.android.settings.ui;

import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/** Verifies basic functionality of the About Phone screen */
public class AboutPhoneSettingsTests extends InstrumentationTestCase {
    private static final boolean LOCAL_LOGV = false;
    private static final String TAG = "AboutPhoneSettingsTest";
    private static final int TIMEOUT = 2000;
    private static final String SETTINGS_PACKAGE = "com.android.settings";

    private UiDevice mDevice;

    // TODO: retrieve using name/ids from com.android.settings package
    private static final String[] sResourceTexts = {
        "Status",
        "Legal information",
        "Regulatory labels",
        "Model",
        "Android version",
        "Android security patch level",
        "Baseband version",
        "Kernel version",
        "Build number"
    };

    private static final String[] sClickableResourceTexts = {
        "Status", "Legal information", "Regulatory labels",
    };

    @Override
    public void setUp() throws Exception {
        if (LOCAL_LOGV) {
            Log.d(TAG, "-------");
        }
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to freeze device orientaion", e);
        }

        // make sure we are in a clean state before starting the test
        mDevice.pressHome();
        Thread.sleep(TIMEOUT * 2);
        launchAboutPhoneSettings(Settings.ACTION_DEVICE_INFO_SETTINGS);
        // TODO: make sure we are always at the top of the app
        // currently this will fail if the user has navigated into submenus
        UiObject2 view =
                mDevice.wait(
                        Until.findObject(By.res(SETTINGS_PACKAGE + ":id/main_content")), TIMEOUT);
        assertNotNull("Could not find main About Phone screen", view);
        view.scroll(Direction.UP, 1.0f);
    }

    @Override
    protected void tearDown() throws Exception {
        // Adding an extra pressBack so we exit About Phone Settings
        // and finish the test cleanly
        mDevice.pressBack();
        mDevice.pressHome(); // finish settings activity
        mDevice.waitForIdle(TIMEOUT * 2); // give UI time to finish animating
        super.tearDown();
    }

    private void launchAboutPhoneSettings(String aboutSetting) throws Exception {
        Intent aboutIntent = new Intent(aboutSetting);
        aboutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(aboutIntent);
    }

    /**
     * Callable actions that can be taken when a UIObject2 is found
     *
     * @param device The current UiDevice
     * @param item The UiObject2 that was found and can be acted on
     *
     * @return {@code true} if the call was successful, and {@code false} otherwise
     */
    public interface UIObject2Callback {
        boolean call(UiDevice device, UiObject2 item) throws Exception;
    }

    /**
     * Clicks the given item and then presses the Back button
     *
     * <p>Used to test whether a given UiObject2 can be successfully clicked.
     * Presses Back to restore state to the previous screen.
     *
     * @param device The device that can be used to press Back
     * @param item The item to click
     *
     * @return {@code true} if clicking the item succeeded, and {@code false} otherwise
     */
    public class UiObject2Clicker implements UIObject2Callback {
        public boolean call(UiDevice device, UiObject2 item) throws Exception {
            item.click();
            Thread.sleep(TIMEOUT * 2); // give UI time to finish animating
            boolean pressWorked = device.pressBack();
            Thread.sleep(TIMEOUT * 2);
            return pressWorked;
        }
    }

    /**
     * Removes items found in the view and optionally takes some action.
     *
     * @param device The current UiDevice
     * @param itemsLeftToFind The items to search for in the current view
     * @param action Action to call on each item that is found; pass {@code null} to take no action
     */
    private void removeItemsAndTakeAction(
            UiDevice device, ArrayList<String> itemsLeftToFind, UIObject2Callback action) throws Exception {
        for (Iterator<String> iterator = itemsLeftToFind.iterator(); iterator.hasNext(); ) {
            String itemText = iterator.next();
            UiObject2 item = device.wait(Until.findObject(By.text(itemText)), TIMEOUT);
            if (item != null) {
                if (LOCAL_LOGV) {
                    Log.d(TAG, itemText + " is present");
                }
                iterator.remove();
                if (action != null) {
                    boolean success = action.call(device, item);
                    assertTrue("Calling action after " + itemText + " did not work", success);
                }
            } else {
                if (LOCAL_LOGV) {
                    Log.d(TAG, "Could not find " + itemText);
                }
            }
        }
    }

    /**
     * Searches for UI elements in the current view and optionally takes some action.
     *
     * <p>Will scroll down the screen until it has found all elements or reached the bottom.
     * This allows elements to be found and acted on even if they change order.
     *
     * @param device The current UiDevice
     * @param itemsToFind The items to search for in the current view
     * @param action Action to call on each item that is found; pass {@code null} to take no action
     */
    public void searchForItemsAndTakeAction(UiDevice device, String[] itemsToFind, UIObject2Callback action)
            throws Exception {

        ArrayList<String> itemsLeftToFind = new ArrayList<String>(Arrays.asList(itemsToFind));
        assertFalse(
                "There must be at least one item to search for on the screen!",
                itemsLeftToFind.isEmpty());

        if (LOCAL_LOGV) {
            Log.d(TAG, "items: " + TextUtils.join(", ", itemsLeftToFind));
        }
        boolean canScrollDown = true;
        while (canScrollDown && !itemsLeftToFind.isEmpty()) {
            removeItemsAndTakeAction(device, itemsLeftToFind, action);

            // when we've finished searching the current view, scroll down
            UiObject2 view =
                    device.wait(
                            Until.findObject(By.res(SETTINGS_PACKAGE + ":id/main_content")),
                            TIMEOUT * 2);
            if (view != null) {
                canScrollDown = view.scroll(Direction.DOWN, 1.0f);
            } else {
                canScrollDown = false;
            }
        }
        // check the last items once we have reached the bottom of the view
        removeItemsAndTakeAction(device, itemsLeftToFind, action);

        assertTrue(
                "The following items were not found on the screen: "
                        + TextUtils.join(", ", itemsLeftToFind),
                itemsLeftToFind.isEmpty());
    }

    @MediumTest // UI interaction
    public void testAllMenuEntriesExist() throws Exception {
        searchForItemsAndTakeAction(mDevice, sResourceTexts, null);
    }

    // Suppressing this test as it might be causing other test failures
    // Will verify that this test is the cause before proceeding with solution
    @Suppress
    @MediumTest // UI interaction
    public void testClickableEntriesCanBeClicked() throws Exception {
        searchForItemsAndTakeAction(mDevice, sClickableResourceTexts, new UiObject2Clicker());
    }
}
