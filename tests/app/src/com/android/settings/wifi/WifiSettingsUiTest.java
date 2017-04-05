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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WifiSettingsUiTest {

    // TODO(sghuman): Investigate why resource ids are not resolving correctly in the test apk,
    // then remove this manual string entry
    private static final String WIFI_PREFERENCES = "Wi\\u2011Fi preferences";

    @Mock private WifiTracker mockWifiTracker;

    @Rule
    public ActivityTestRule<WifiSettingsActivity> mActivityRule =
            new ActivityTestRule<>(WifiSettingsActivity.class, true);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        WifiTrackerFactory.setTestingWifiTracker(mockWifiTracker);
    }

    private void launchActivity() {
        mActivityRule.launchActivity(new Intent("android.settings.WIFI_SETTINGS"));
    }

    @Test
    public void launchActivityShouldSucceed() {
        launchActivity();
    }

    @Test
    public void shouldShowWifiPreferences() {
        launchActivity();
        onView(withText(WIFI_PREFERENCES)).perform(click());
    }
}
