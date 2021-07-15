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

package com.android.settings.accessibility;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Instrumentation;
import android.os.Bundle;
import android.os.Looper;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.Settings.AccessibilitySettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ToggleFeaturePreferenceFragmentTest {
    private static final String SUMMARY_TEXT = "Here's some summary text";

    @Rule
    public final ActivityTestRule<AccessibilitySettingsActivity> mActivityRule =
            new ActivityTestRule<>(AccessibilitySettingsActivity.class, true);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    @BeforeClass
    public static void oneTimeSetup() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Before
    public void setUp() {
        mInstrumentation.runOnMainSync(() -> {
            MyToggleFeaturePreferenceFragment fragment = new MyToggleFeaturePreferenceFragment();
            Bundle args = new Bundle();
            args.putString(AccessibilitySettings.EXTRA_SUMMARY, SUMMARY_TEXT);
            fragment.setArguments(args);
            new SubSettingLauncher(mActivityRule.getActivity())
                    .setDestination(MyToggleFeaturePreferenceFragment.class.getName())
                    .setArguments(args)
                    .setSourceMetricsCategory(
                            InstrumentedPreferenceFragment.METRICS_CATEGORY_UNKNOWN)
                    .launch();
        });
    }

    @Test
    public void testSummaryTestDisplayed() {
        onView(withText(SUMMARY_TEXT)).check(matches(isDisplayed()));
    }

    public static class MyToggleFeaturePreferenceFragment extends ToggleFeaturePreferenceFragment {
        @Override
        protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }

        @Override
        int getUserShortcutTypes() {
            return 0;
        }
    }
}