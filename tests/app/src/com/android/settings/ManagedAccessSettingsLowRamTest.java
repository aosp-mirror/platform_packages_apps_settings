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

package com.android.settings;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ManagedAccessSettingsLowRamTest {

    private Instrumentation mInstrumentation;
    private Context mTargetContext;

    @Before
    public void setUp() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = mInstrumentation.getTargetContext();
    }

    @Test
    public void testManagedAccessOptionsVisibility() throws Exception {
        mInstrumentation.startActivitySync(new Intent(mTargetContext,
                com.android.settings.Settings.SpecialAccessSettingsActivity.class));

        String[] managedServiceLabels = new String[] {"Do Not Disturb access",
                "VR helper services", "Notification access", "Picture-in-picture"};
        for (String label : managedServiceLabels) {
            if (ActivityManager.isLowRamDeviceStatic()) {
                onView(withText(label)).check(doesNotExist());
            } else {
                onView(withText(label)).check(matches(isDisplayed()));
            }
        }
    }

    @Test
    public void launchNotificationSetting_onlyWorksIfNotLowRam() {
        final Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);

        mInstrumentation.startActivitySync(intent);

        final String label = "This feature is not available on this device";
        if (ActivityManager.isLowRamDeviceStatic()) {
            onView(withText(label)).check(matches(isDisplayed()));
        } else {
            onView(withText(label)).check(doesNotExist());
        }
    }

    @Test
    public void launchDndSetting_onlyWorksIfNotLowRam() {
        final Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

        mInstrumentation.startActivitySync(intent);

        final String label = "This feature is not available on this device";
        if (ActivityManager.isLowRamDeviceStatic()) {
            onView(withText(label)).check(matches(isDisplayed()));
        } else {
            onView(withText(label)).check(doesNotExist());
        }
    }

    @Test
    public void launchVrSetting_onlyWorksIfNotLowRam() {
        final Intent intent = new Intent(Settings.ACTION_VR_LISTENER_SETTINGS);

        mInstrumentation.startActivitySync(intent);

        final String label = "This feature is not available on this device";
        if (ActivityManager.isLowRamDeviceStatic()) {
            onView(withText(label)).check(matches(isDisplayed()));
        } else {
            onView(withText(label)).check(doesNotExist());
        }
    }

    @Test
    public void launchPictureInPictureSetting_onlyWorksIfNotLowRam() {
        final Intent intent = new Intent(Settings.ACTION_PICTURE_IN_PICTURE_SETTINGS);

        mInstrumentation.startActivitySync(intent);

        final String label = "This feature is not available on this device";
        if (ActivityManager.isLowRamDeviceStatic()) {
            onView(withText(label)).check(matches(isDisplayed()));
        } else {
            onView(withText(label)).check(doesNotExist());
        }
    }
}
