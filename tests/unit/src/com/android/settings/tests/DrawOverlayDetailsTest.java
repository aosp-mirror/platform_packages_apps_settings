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

package com.android.settings.tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.core.IsNot.not;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DrawOverlayDetailsTest {
    private final static String PACKAGE_SYSTEM_UI = "com.android.systemui";

    @Test
    public void testSystemUiDrawOverlayDetails_Disabled() throws Exception{
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(new Intent(android.provider.Settings
                .ACTION_MANAGE_OVERLAY_PERMISSION));

        final Context targetContext = instrumentation.getTargetContext();

        final PackageManager packageManager = targetContext.getPackageManager();
        final String appName = (String) packageManager.getApplicationLabel(packageManager
                .getApplicationInfo(PACKAGE_SYSTEM_UI, PackageManager.GET_META_DATA));

        final UiDevice device = UiDevice.getInstance(instrumentation);
        device.waitForIdle();

        openActionBarOverflowOrOptionsMenu(targetContext);
        onView(withText(targetContext.getString(R.string.menu_show_system))).perform(click());
        device.waitForIdle();

        final UiScrollable settings = new UiScrollable(
                new UiSelector().packageName(targetContext.getPackageName()).scrollable(true));
        settings.scrollTextIntoView(appName);
        onView(withText(appName)).perform(click());
        onView(withText(targetContext.getString(R.string.permit_draw_overlay))).check(matches
                (not(isEnabled())));
    }

}
