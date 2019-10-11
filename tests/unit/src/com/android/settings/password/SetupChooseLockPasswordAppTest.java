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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.not;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class SetupChooseLockPasswordAppTest {

    private Context mContext;

    @Rule
    public ActivityTestRule<SetupChooseLockPassword> mActivityTestRule =
            new ActivityTestRule<>(
                    SetupChooseLockPassword.class,
                    true /* enable touch at launch */,
                    false /* don't launch at every test */);

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testSkipDialogIsShown() throws Throwable {
        SetupChooseLockPassword activity = mActivityTestRule.launchActivity(null);
        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        final Button skipOrClearButton =
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView();

        assertThat(skipOrClearButton.getText()).isEqualTo(mContext.getString(R.string.skip_label));
        assertThat(skipOrClearButton.getVisibility()).isEqualTo(View.VISIBLE);
        skipOrClearButton.performClick();
        assertThat(activity.isFinishing()).named("Is finishing").isTrue();
    }

    @Test
    public void clearIsNotShown_when_activityLaunchedInitially() {
        SetupChooseLockPassword activity = mActivityTestRule.launchActivity(null);
        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        assertThat(layout.getMixin(FooterBarMixin.class).getSecondaryButtonView().getText())
                .isEqualTo(mContext.getString(R.string.lockpassword_clear_label));
    }

    @Test
    public void clearIsNotShown_when_nothingEntered() throws Throwable {
        SetupChooseLockPassword activity = mActivityTestRule.launchActivity(null);
        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        onView(withId(R.id.password_entry)).perform(ViewActions.typeText("1234"))
                .perform(pressKey(KeyEvent.KEYCODE_ENTER));
        assertThat(
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView().getVisibility())
                        .isEqualTo(View.GONE);
    }

    @Test
    public void clearIsShown_when_somethingEnteredToConfirm() {
        SetupChooseLockPassword activity = mActivityTestRule.launchActivity(null);
        PartnerCustomizationLayout layout = activity.findViewById(R.id.setup_wizard_layout);
        onView(withId(R.id.password_entry)).perform(ViewActions.typeText("1234"))
               .perform(pressKey(KeyEvent.KEYCODE_ENTER));
        mActivityTestRule.launchActivity(null);
        onView(withId(R.id.password_entry)).perform(ViewActions.typeText("1234"))
                .perform(pressKey(KeyEvent.KEYCODE_ENTER))
                .perform(ViewActions.typeText("1"));
        // clear should be present if text field contains content
        assertThat(
                layout.getMixin(FooterBarMixin.class).getSecondaryButtonView().getVisibility())
                        .isEqualTo(View.VISIBLE);
    }
}
