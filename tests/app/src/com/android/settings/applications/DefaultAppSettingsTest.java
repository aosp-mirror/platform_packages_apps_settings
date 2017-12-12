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
package com.android.settings.applications;

import android.content.Context;
import android.content.Intent;
import android.support.test.filters.SmallTest;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.test.InstrumentationTestCase;
import android.widget.TextView;

import com.android.settings.R;

import org.junit.Test;

/**
 * Test for Advanced App preferences.
 */
@SmallTest
public class DefaultAppSettingsTest extends InstrumentationTestCase {

    private UiDevice mDevice;
    private Context mTargetContext;
    private String mTargetPackage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        mTargetContext = getInstrumentation().getTargetContext();
        mTargetPackage = mTargetContext.getPackageName();
    }

    @Test
    public void testSelectDefaultHome_shouldLaunchHomePicker() throws Exception {
        launchDefaultApps();
        final String titleHomeApp = mTargetContext.getResources().getString(R.string.home_app);
        mDevice.findObject(new UiSelector().text(titleHomeApp)).click();
        final UiObject actionBar = mDevice.findObject(new UiSelector().resourceId(
            "com.android.settings:id/action_bar"));
        final UiObject title = actionBar.getChild(
            new UiSelector().className(TextView.class.getName()));
        assertEquals(titleHomeApp, title.getText());
    }

    private void launchDefaultApps() throws Exception  {
        final Intent settingsIntent = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(mTargetPackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(settingsIntent);
        final String titleApps = mTargetContext.getResources().getString(
            R.string.app_and_notification_dashboard_title);
        mDevice.findObject(new UiSelector().text(titleApps)).click();
        final String titleDefaultApps = mTargetContext.getResources().getString(
            R.string.app_default_dashboard_title);
        mDevice.findObject(new UiSelector().text(titleDefaultApps)).click();
    }

}
