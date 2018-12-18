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
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.test.InstrumentationTestCase;
import android.widget.TextView;

import androidx.test.filters.SmallTest;

import com.android.settings.R;

import org.junit.Test;

/**
 * Test for Special App Access preferences.
 */
@SmallTest
public class SpecialAppAccessSettingsTest extends InstrumentationTestCase {

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
    public void testSelectPictureInPicture_shouldNotCrash() throws Exception {
        launchSpecialApps();
        final String titlePictureInPictureApp =
                mTargetContext.getResources().getString(R.string.picture_in_picture_title);

        // select Picture-in-Picture
        mDevice.findObject(new UiSelector().text(titlePictureInPictureApp)).click();

        // Picture-in-picture settings page should launch and no crash
        final UiObject actionBar = mDevice.findObject(new UiSelector().resourceId(
            "com.android.settings:id/action_bar"));
        final UiObject title = actionBar.getChild(
            new UiSelector().className(TextView.class.getName()));
        assertEquals(titlePictureInPictureApp, title.getText());
    }

    private void launchSpecialApps() throws Exception  {
        final Intent settingsIntent = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(mTargetPackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(settingsIntent);
        final String titleApps = mTargetContext.getResources().getString(
            R.string.app_and_notification_dashboard_title);
        mDevice.findObject(new UiSelector().text(titleApps)).click();
        final String titleAdvance = mTargetContext.getResources().getString(
                R.string.advanced_section_header);
        mDevice.findObject(new UiSelector().text(titleAdvance)).click();
        final String titleSpecialApps = mTargetContext.getResources().getString(
            R.string.special_access);

        try {
            // scollbar may or may not be present, depending on how many recents app are there. If
            // the page is scrollable, scroll to the bottom to show the special app access settings.
            final UiScrollable settings = new UiScrollable(
                    new UiSelector().packageName(mTargetContext.getPackageName()).scrollable(true));
            settings.scrollTextIntoView(titleSpecialApps);
        } catch (UiObjectNotFoundException e) {
            // ignore
        }

        mDevice.findObject(new UiSelector().text(titleSpecialApps)).click();
    }

}
