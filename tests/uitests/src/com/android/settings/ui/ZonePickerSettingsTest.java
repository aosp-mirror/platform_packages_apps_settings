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
import static com.android.settings.ui.testutils.SettingsTestUtils.TIMEOUT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.system.helpers.SettingsHelper;
import android.system.helpers.SettingsHelper.SettingsType;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.TimeZone;

@Ignore
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ZonePickerSettingsTest {

    private static final BySelector SELECTOR_SELECT_TIME_ZONE =
            By.hasChild(By.text("Select time zone"));

    private UiDevice mDevice;
    private SettingsHelper mHelper;
    private String mIsV2EnabledByDefault;
    private int mIsAutoZoneEnabled;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mHelper = SettingsHelper.getInstance();
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientation", e);
        }
        mIsV2EnabledByDefault = mHelper.getStringSetting(SettingsType.GLOBAL,
                "settings_zone_picker_v2");
        mHelper.setStringSetting(SettingsType.GLOBAL, "settings_zone_picker_v2", "true");
        mIsAutoZoneEnabled = mHelper.getIntSetting(SettingsType.GLOBAL,
                Settings.Global.AUTO_TIME_ZONE);
    }

    @After
    public void tearDown() throws Exception {
        // Go back to home for next test.
        mDevice.pressBack();
        mDevice.pressBack();
        mDevice.pressHome();
        mDevice.waitForIdle(TIMEOUT * 2);
        mHelper.setStringSetting(SettingsType.GLOBAL, "settings_zone_picker_v2",
                mIsV2EnabledByDefault);
        mHelper.setIntSetting(SettingsType.GLOBAL, Settings.Global.AUTO_TIME_ZONE,
                mIsAutoZoneEnabled);
    }

    @Test
    public void zonePickerDisabled() throws Exception {
        mHelper.setIntSetting(SettingsType.GLOBAL, Settings.Global.AUTO_TIME_ZONE, 1);

        SettingsHelper.launchSettingsPage(
                InstrumentationRegistry.getContext(), Settings.ACTION_DATE_SETTINGS);
        UiObject2 selectTimeZone = wait(SELECTOR_SELECT_TIME_ZONE);
        assertFalse(selectTimeZone.isEnabled());
    }

    // Test 2 time zones with no DST
    @Test
    public void testSelectReykjavik() throws Exception {
        testSelectTimeZone("Iceland", "Reykjavik", "GMT+00:00", "Atlantic/Reykjavik", true);
    }

    @Test
    public void testSelectPhoenix() throws Exception {
        testSelectTimeZone("United States", "Phoenix", "GMT-07:00", "America/Phoenix", false);
    }

    private void testSelectTimeZone(String region, String timezone, String expectedTimeZoneOffset,
            String expectedTimeZoneId, boolean assumeOneTimeZoneInRegion) throws Exception {
        mHelper.setIntSetting(SettingsType.GLOBAL, Settings.Global.AUTO_TIME_ZONE, 0);

        SettingsHelper.launchSettingsPage(
                InstrumentationRegistry.getContext(), Settings.ACTION_DATE_SETTINGS);

        UiObject2 selectTimeZone = wait(SELECTOR_SELECT_TIME_ZONE);
        assertTrue(selectTimeZone.isEnabled());
        selectTimeZone.click();

        wait(By.text("Region")).click();
        // Speed-up the test by searching with the first 2 characters of the region name
        wait(By.res("android", "search_src_text")).setText(region.substring(0, 2));
        // Select region in the list
        selectItemInList(new UiSelector().textContains(region))
                .click();

        // Only select time zone explicitly if there are more than one time zones in a region
        if (!assumeOneTimeZoneInRegion) {
            wait(By.text("Time zone"));
            selectItemInList(new UiSelector().textContains(timezone))
                    .click();
        }

        mDevice.pressBack();
        // The select button should include the GMT offset in the summary
        BySelector summarySelector = By.res("android:id/summary");
        UiObject2 selectedTimeZone = selectTimeZone.findObject(summarySelector);
        assertUiObjectFound(selectedTimeZone, summarySelector);
        assertTrue("Expect " + expectedTimeZoneOffset + " is shown for " + timezone,
                selectedTimeZone.getText().startsWith(expectedTimeZoneOffset));

        waitAndAssertTimeGetDefault(expectedTimeZoneId);
        assertEquals("Time zone change in Settings should update persist.sys.timezone",
                expectedTimeZoneId, SystemProperties.get("persist.sys.timezone"));
    }

    private static final long CHECK_DEFAULT_TIMEZONE_INTERVAL = 200L;
    private static final long CHECK_DEFAULT_TIMEZONE_TIMEOUT = 3000L;

    /**
     * Wait for the broadcast ACTION_TIMEZONE_CHANGED propagated, and update the default TimeZone
     * by ApplicationThread.
     */
    private static void waitAndAssertTimeGetDefault(String expectedTimeZoneId)
            throws InterruptedException {
        for (int i = 0; i < CHECK_DEFAULT_TIMEZONE_TIMEOUT / CHECK_DEFAULT_TIMEZONE_INTERVAL; i++) {
            if (expectedTimeZoneId.equals(TimeZone.getDefault().getID())) {
                return;
            }
            Thread.sleep(CHECK_DEFAULT_TIMEZONE_INTERVAL);
        }

        assertEquals(expectedTimeZoneId, TimeZone.getDefault().getID());
    }

    private UiObject selectItemInList(UiSelector childSelector) throws UiObjectNotFoundException {
        UiScrollable recyclerView = new UiScrollable(
                new UiSelector().resourceId(SETTINGS_PACKAGE + ":id/recycler_view"));
        return selectScrollableItem(recyclerView, childSelector);
    }

    /**
     * Select the child object in the UiScrollable
     * @throws UiObjectNotFoundException if scrollable or child is not found
     */
    private UiObject selectScrollableItem(UiScrollable scrollable, UiSelector childSelector)
            throws UiObjectNotFoundException {
        if (!scrollable.waitForExists(TIMEOUT)) {
            throw newUiObjectNotFoundException(scrollable.getSelector());
        }
        scrollable.scrollIntoView(childSelector);

        UiObject child = mDevice.findObject(childSelector);
        assertUiObjectFound(child, childSelector);
        return child;
    }

    /**
     * @throws UiObjectNotFoundException if UiDevice.wait returns null
     */
    private UiObject2 wait(BySelector selector) throws UiObjectNotFoundException {
        UiObject2 item = mDevice.wait(Until.findObject(selector), TIMEOUT);
        assertUiObjectFound(item, selector);
        return item;
    }

    private static void assertUiObjectFound(UiObject2 obj, BySelector selector)
            throws UiObjectNotFoundException {
        if (obj == null) {
            throw newUiObjectNotFoundException(selector);
        }
    }


    private static void assertUiObjectFound(UiObject obj, UiSelector selector)
            throws UiObjectNotFoundException {
        if (obj == null) {
            throw newUiObjectNotFoundException(selector);
        }
    }

    private static UiObjectNotFoundException newUiObjectNotFoundException(Object selector) {
        return new UiObjectNotFoundException(
                String.format("UI object not found: %s", selector.toString()));
    }
}
