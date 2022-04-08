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
package com.android.settings.wifi;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SavedNetworkSettingsTest {

    // Keys used to lookup resources by name (see the resourceId helper method).
    private static final String STRING = "string";
    private static final String WIFI_ADD_NETWORK = "wifi_add_network";
    private static final String WIFI_NETWORK_LABEL = "wifi_ssid";

    private Context mContext;

    @Rule
    public ActivityTestRule<Settings.SavedAccessPointsSettingsActivity> mActivityRule =
            new ActivityTestRule<>(Settings.SavedAccessPointsSettingsActivity.class, true);

    private int resourceId(String type, String name) {
        return mContext.getResources().getIdentifier(name, type, mContext.getPackageName());
    }

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    private void launchSavedNetworksSettings() {
        Intent intent = new Intent()
                .setClassName(mContext.getPackageName(),
                        Settings.SavedAccessPointsSettingsActivity.class.getName())
                .setPackage(mContext.getPackageName());
        mActivityRule.launchActivity(intent);
    }

    @Test
    public void launchSavedNetworkSettings_shouldHaveAddNetworkField() {
        launchSavedNetworksSettings();
        onView(withText(resourceId(STRING, WIFI_ADD_NETWORK))).check(matches(isDisplayed()))
                .perform(click());
        onView(withText(resourceId(STRING, WIFI_NETWORK_LABEL))).check(matches(isDisplayed()));
    }
}
