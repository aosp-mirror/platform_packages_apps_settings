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

package com.android.settings.ui.testutils;

import static org.junit.Assert.assertNotNull;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

public class SettingsTestUtils {

    public static final String SETTINGS_PACKAGE = "com.android.settings";
    public static final int TIMEOUT = 2000;

    private void scrollToTop(UiDevice device) throws Exception {
        int count = 5;
        UiObject2 view = null;
        while (count >= 0) {
            view = device.wait(
                    Until.findObject(By.res(SETTINGS_PACKAGE, "main_content")),
                    TIMEOUT);
            view.scroll(Direction.UP, 1.0f);
            count--;
        }
    }

    public static void assertTitleMatch(UiDevice device, String title) {
        int maxAttempt = 5;
        UiObject2 item = null;
        UiObject2 view = null;
        while (maxAttempt-- > 0) {
            item = device.wait(Until.findObject(By.res("android:id/title").text(title)), TIMEOUT);
            if (item == null) {
                view = device.wait(
                        Until.findObject(By.res(SETTINGS_PACKAGE, "main_content")),
                        TIMEOUT);
                view.scroll(Direction.DOWN, 1.0f);
            } else {
                return;
            }
        }
        assertNotNull(String.format("%s in Setting has not been loaded correctly", title), item);
    }
}
