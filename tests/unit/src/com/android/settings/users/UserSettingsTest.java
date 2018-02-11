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
package com.android.settings.users;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiObjectNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UserSettingsTest {

    private static final String SYSTEM = "System";
    private static final String ADVANCED = "Advanced";
    private static final String USERS = "Multiple users";
    private static final String EMERGNENCY_INFO = "Emergency information";
    private static final String ADD_USERS_WHEN_LOCKED = "Add users";
    private static final String SWITCH_USER_BUTTON = "com.android.systemui:id/multi_user_switch";
    private static final String SETTINGS_BUTTON = "com.android.systemui:id/settings_button";
    private static final String PRIMARY_USER = "Owner";
    private static final String GUEST_USER = "Guest";
    private static final String ADD_GUEST = "Add guest";
    private static final String CONTINUE = "Yes, continue";

    private UiDevice mDevice;
    private Context mContext;
    private String mTargetPackage;

    @Before
    public void setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mContext = InstrumentationRegistry.getTargetContext();
        mTargetPackage = mContext.getPackageName();
    }

    @Test
    public void testEmergencyInfoNotExists() throws Exception {
        launchUserSettings();
        UiObject emergencyInfoPreference =
            mDevice.findObject(new UiSelector().text(EMERGNENCY_INFO));

        assertThat(emergencyInfoPreference.exists()).isFalse();
    }

    @Test
    public void testAddUsersWhenLockedNotExists() throws Exception {
        launchUserSettings();
        UiObject addUsersPreference =
            mDevice.findObject(new UiSelector().text(ADD_USERS_WHEN_LOCKED));
        assertThat(addUsersPreference.exists()).isFalse();
    }

    @Test
    public void testUsersExistsOnSecondaryUser() throws Exception {
        // switch to guest user
        switchToOrCreateGuest();
        // launch settings (launch from intent doesn't work, hence launch from quick settings)
        mDevice.openQuickSettings();
        mDevice.findObject(new UiSelector().resourceId(SETTINGS_BUTTON)).click();
        // launch system settings and expand whole screen
        final UiScrollable settings = new UiScrollable(
            new UiSelector().packageName(mTargetPackage).scrollable(true));
        final String titleSystem = SYSTEM;
        settings.scrollTextIntoView(titleSystem);
        mDevice.findObject(new UiSelector().text(titleSystem)).click();
        mDevice.findObject(new UiSelector().text(ADVANCED)).click();

        final boolean hasUsersSettings = mDevice.findObject(new UiSelector().text(USERS)).exists();

        // switch back to primary user
        mDevice.openQuickSettings();
        mDevice.findObject(new UiSelector().resourceId(SWITCH_USER_BUTTON)).click();
        mDevice.findObject(new UiSelector().text(PRIMARY_USER)).click();

        assertThat(hasUsersSettings).isTrue();
    }

    private void launchSettings() {
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(mTargetPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(settingsIntent);
    }

    private void launchUserSettings() throws Exception  {
        launchSettings();
        final UiScrollable settings = new UiScrollable(
                new UiSelector().packageName(mTargetPackage).scrollable(true));
        final String titleSystem = SYSTEM;
        settings.scrollTextIntoView(titleSystem);
        mDevice.findObject(new UiSelector().text(titleSystem)).click();
        mDevice.findObject(new UiSelector().text(ADVANCED)).click();
        mDevice.findObject(new UiSelector().text(USERS)).click();
    }

    private void switchToOrCreateGuest() throws UiObjectNotFoundException {
        mDevice.openQuickSettings();
        mDevice.findObject(new UiSelector().resourceId(SWITCH_USER_BUTTON)).click();
        // if no existing guest user, select "Add guest", otherwise select "Guest"
        final UiObject addGuest = mDevice.findObject(new UiSelector().text(ADD_GUEST));
        if (addGuest.exists()) {
            addGuest.click();
            mDevice.waitForIdle();
            mDevice.pressBack();
        } else {
            mDevice.findObject(new UiSelector().text(GUEST_USER)).click();
            mDevice.waitForIdle();
            mDevice.findObject(new UiSelector().text(CONTINUE)).click();
            mDevice.waitForIdle();
        }
    }
}
