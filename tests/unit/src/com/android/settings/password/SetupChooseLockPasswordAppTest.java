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

package com.android.settings.password;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.not;

import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;

import com.android.settings.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class SetupChooseLockPasswordAppTest {

    @Rule
    public ActivityTestRule<SetupChooseLockPassword> mActivityTestRule =
            new ActivityTestRule<>(
                    SetupChooseLockPassword.class,
                    true /* enable touch at launch */,
                    false /* don't launch at every test */);

    @Test
    public void testSkipDialogIsShown() throws Throwable {
        SetupChooseLockPassword activity = mActivityTestRule.launchActivity(null);

        onView(withId(R.id.cancel_button))
                .check(matches(withText(R.string.skip_label)))
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withId(android.R.id.button1)).check(matches(isDisplayed())).perform(click());

        assertThat(activity.isFinishing()).named("Is finishing").isTrue();
    }

    @Test
    public void clearNotVisible_when_activityLaunchedInitially() {
        mActivityTestRule.launchActivity(null);
        onView(withId(R.id.clear_button)).check(matches(
                withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void clearNotEnabled_when_nothingEntered() throws Throwable {
        mActivityTestRule.launchActivity(null);
        onView(withId(R.id.password_entry)).perform(ViewActions.typeText("1234"))
                .perform(pressKey(KeyEvent.KEYCODE_ENTER));
        onView(withId(R.id.clear_button)).check(matches(isDisplayed()))
                .check(matches(not(isEnabled())));
    }

    @Test
    public void clearEnabled_when_somethingEnteredToConfirm() {
        mActivityTestRule.launchActivity(null);
        onView(withId(R.id.password_entry)).perform(ViewActions.typeText("1234"))
                .perform(pressKey(KeyEvent.KEYCODE_ENTER))
                .perform(ViewActions.typeText("1"));
        // clear should be present if text field contains content
        onView(withId(R.id.clear_button)).check(matches(isDisplayed()));
    }
}
