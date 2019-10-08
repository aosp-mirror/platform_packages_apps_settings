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

import static com.android.settings.ui.testutils.SettingsTestUtils.SETTINGS_PACKAGE;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.text.TextUtils;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/** Verifies basic functionality of the About Phone screen */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class AboutPhoneSettingsTests {
    private static final int TIMEOUT = 2000;

    // TODO: retrieve using name/ids from com.android.settings package
    private static final String[] sResourceTexts = {
            "Phone number",
            "Legal information",
            "Regulatory labels"
    };

    private UiDevice mDevice;
    private Instrumentation mInstrumentation;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mDevice = UiDevice.getInstance(mInstrumentation);
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
        assertThat(view).isNotNull();
        view.scroll(Direction.UP, 1.0f);
    }

    @After
    public void tearDown() throws Exception {
        // Adding an extra pressBack so we exit About Phone Settings
        // and finish the test cleanly
        mDevice.pressBack();
        mDevice.pressHome(); // finish settings activity
        mDevice.waitForIdle(TIMEOUT * 2); // give UI time to finish animating
    }

    @Test
    public void testAllMenuEntriesExist() {
        searchForItemsAndTakeAction(mDevice, sResourceTexts);
    }

    private void launchAboutPhoneSettings(String aboutSetting) {
        Intent aboutIntent = new Intent(aboutSetting);
        aboutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InstrumentationRegistry.getTargetContext().startActivity(aboutIntent);
    }

    /**
     * Removes items found in the view and optionally takes some action.
     */
    private void removeItemsAndTakeAction(UiDevice device, ArrayList<String> itemsLeftToFind) {
        for (Iterator<String> iterator = itemsLeftToFind.iterator(); iterator.hasNext(); ) {
            String itemText = iterator.next();
            UiObject2 item = device.wait(Until.findObject(By.text(itemText)), TIMEOUT);
            if (item != null) {
                iterator.remove();
            }
        }
    }

    /**
     * Searches for UI elements in the current view and optionally takes some action.
     *
     * <p>Will scroll down the screen until it has found all elements or reached the bottom.
     * This allows elements to be found and acted on even if they change order.
     */
    private void searchForItemsAndTakeAction(UiDevice device, String[] itemsToFind) {

        ArrayList<String> itemsLeftToFind = new ArrayList<>(Arrays.asList(itemsToFind));
        assertWithMessage("There must be at least one item to search for on the screen!")
                .that(itemsLeftToFind)
                .isNotEmpty();

        boolean canScrollDown = true;
        while (canScrollDown && !itemsLeftToFind.isEmpty()) {
            removeItemsAndTakeAction(device, itemsLeftToFind);

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
        removeItemsAndTakeAction(device, itemsLeftToFind);

        assertWithMessage("The following items were not found on the screen: "
                + TextUtils.join(", ", itemsLeftToFind))
                .that(itemsLeftToFind)
                .isEmpty();
    }
}
