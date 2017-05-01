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
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static org.hamcrest.Matchers.allOf;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CameraLiftTriggerSuggestionActivityTest {
    private Instrumentation mInstrumentation;
    private Context mTargetContext;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = mInstrumentation.getTargetContext();
    }

    @Test
    public void launchCameraLiftTriggerSuggestion_shouldNotCrash() {
        final Intent cameraTriggerSuggestionIntent = new Intent(mTargetContext,
                Settings.CameraLiftTriggerSuggestionActivity.class);
        final boolean cameraLiftTriggerEnabled = mTargetContext.getResources()
                .getBoolean(R.bool.config_cameraLiftTriggerAvailable);

        if (!cameraLiftTriggerEnabled) {
            return;
        }

        mInstrumentation.startActivitySync(cameraTriggerSuggestionIntent);

        onView(allOf(withText(R.string.camera_lift_trigger_title),
                          hasSibling(withText(R.string.camera_lift_trigger_summary))))
                .check(matches(isDisplayed()));
    }
}
