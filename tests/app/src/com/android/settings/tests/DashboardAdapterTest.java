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
import com.android.settings.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DashboardAdapterTest {
    @Before
    public void SetUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.startActivitySync(new Intent(Settings
                .ACTION_SETTINGS));
    }

    @Test
    public void testTileConsistency_ToggleSuggestionsAndOpenBluetooth_shouldInBluetooth()
            throws Exception{
        final Context context = InstrumentationRegistry.getTargetContext();

        onView(allOf(withText(context.getString(R.string.suggestions_title)),
                withEffectiveVisibility(VISIBLE))).perform(click());
        onView(allOf(withText(context.getString(R.string.bluetooth_settings)),
                withEffectiveVisibility(VISIBLE))).perform(click());

        // It should go to Bluetooth sub page, not other page or crash
        onView(allOf(withText(context.getString(R.string.bluetooth_settings)),
                withEffectiveVisibility(VISIBLE))).check(matches(isDisplayed()));

    }
}
