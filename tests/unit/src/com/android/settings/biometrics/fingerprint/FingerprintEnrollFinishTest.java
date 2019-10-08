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
package com.android.settings.biometrics.fingerprint;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.ComponentName;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FingerprintEnrollFinishTest {

    @Rule
    public IntentsTestRule<FingerprintEnrollFinish> mActivityRule =
            new IntentsTestRule<>(FingerprintEnrollFinish.class);

    @Test
    public void clickAddAnother_shouldLaunchEnrolling() {
        final ComponentName enrollingComponent = new ComponentName(
                getTargetContext(),
                FingerprintEnrollEnrolling.class);

        intending(hasComponent(enrollingComponent))
                .respondWith(new ActivityResult(Activity.RESULT_CANCELED, null));

        PartnerCustomizationLayout layout =
                mActivityRule.getActivity().findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();

        intended(hasComponent(enrollingComponent));
        assertFalse(mActivityRule.getActivity().isFinishing());
    }

    @Test
    public void clickAddAnother_shouldPropagateResults() {
        final ComponentName enrollingComponent = new ComponentName(
                getTargetContext(),
                FingerprintEnrollEnrolling.class);

        intending(hasComponent(enrollingComponent))
                .respondWith(new ActivityResult(Activity.RESULT_OK, null));

        PartnerCustomizationLayout layout =
                mActivityRule.getActivity().findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();

        intended(hasComponent(enrollingComponent));
        assertTrue(mActivityRule.getActivity().isFinishing());
    }

    @Test
    public void clickNext_shouldFinish() {
        onView(withId(R.id.next_button)).perform(click());
        assertTrue(mActivityRule.getActivity().isFinishing());
    }
}