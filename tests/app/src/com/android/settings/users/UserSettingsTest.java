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

import android.content.Context;
import android.content.Intent;
import android.support.test.filters.SmallTest;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.UiScrollable;
import android.test.InstrumentationTestCase;

import com.android.settings.R;

import org.junit.Test;

@SmallTest
public class UserSettingsTest extends InstrumentationTestCase {

    private static final String USER_AND_ACCOUNTS = "Users & accounts";
    private static final String USERS = "Users";
    private static final String EMERGNENCY_INFO = "Emergency information";
    private static final String ADD_USERS_WHEN_LOCKED = "Add users";

    private UiDevice mDevice;
    private Context mContext;
    private String mTargetPackage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mContext = getInstrumentation().getTargetContext();
        mTargetPackage = mContext.getPackageName();
    }

    @Test
    public void testEmergencyInfoNotExists() throws Exception {
        launchUserSettings();
        UiObject emergencyInfoPreference =
            mDevice.findObject(new UiSelector().text(EMERGNENCY_INFO));
        assertFalse(emergencyInfoPreference.exists());
    }

    @Test
    public void testAddUsersWhenLockedNotExists() throws Exception {
        launchUserSettings();
        UiObject addUsersPreference =
            mDevice.findObject(new UiSelector().text(ADD_USERS_WHEN_LOCKED));
        assertFalse(addUsersPreference.exists());
    }

    private void launchSettings() {
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(mTargetPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(settingsIntent);
    }

    private void launchUserSettings() throws Exception  {
        launchSettings();
        final UiScrollable settings = new UiScrollable(
                new UiSelector().packageName(mTargetPackage).scrollable(true));
        final String titleUsersAndAccounts = USER_AND_ACCOUNTS;
        settings.scrollTextIntoView(titleUsersAndAccounts);
        mDevice.findObject(new UiSelector().text(titleUsersAndAccounts)).click();
        mDevice.findObject(new UiSelector().text(USERS)).click();
    }

}
