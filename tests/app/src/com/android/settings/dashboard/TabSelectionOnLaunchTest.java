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
package com.android.settings.dashboard;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.android.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isSelected;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TabSelectionOnLaunchTest {
    @Rule
    public ActivityTestRule<Settings> mActivityRule =
            new ActivityTestRule<>(Settings.class, true, false);

    private final int FLAG_RESTART = Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK;
    private final String ARG_SELECT_SUPPORT_TAB = "SUPPORT";
    private final String ARG_SELECT_FAKE_TAB = "NOT_SUPPORT";

    @Test
    /* cold start for settings app with correct flags and extra always selects support tab */
    public void test_ColdStartWithCorrectArgsCorrectFlags_SupportSelected() {
        launchSettingsWithFlags(ARG_SELECT_SUPPORT_TAB, FLAG_RESTART);
        verifySupportSelected();
    }

    @Test
    /* cold start with correct flags and wrong extra defaults to all tab */
    public void test_ColdStartWithWrongExtra_DoesNotSelectSupport() {
        launchSettingsWithFlags(ARG_SELECT_FAKE_TAB, FLAG_RESTART);
        verifySupportNotSelected();
    }

    @Test
    /* warm start from elsewhere in settings with wrong flags does not select support */
    public void test_WarmStartSummarySelectedCorrectExtraWrongFlags_DoesNotSelectSupport() {
        InstrumentationRegistry.getContext().
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        launchSettingsNoFlags(ARG_SELECT_SUPPORT_TAB);
        verifySupportNotSelected();
    }

    @Test
    /* warm start from elsewhere in settings with with wrong flags & extra does not select support*/
    public void test_WarmStartSummarySelectedWrongExtraWrongFlags_DoesNotSelectSupport() {
        InstrumentationRegistry.getContext().
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
        launchSettingsNoFlags(ARG_SELECT_FAKE_TAB);
        verifySupportNotSelected();
    }

    @Test
    /* settings does not crash on null string */
    public void test_DoesNotCrashOnNullExtra_DoesNotSelectSupport() {
        launchSettingsWithFlags(null, FLAG_RESTART);
        verifySupportNotSelected();
    }

    private void verifySupportNotSelected() {
        onView(withText(mActivityRule.getActivity().getApplicationContext().
                getString(com.android.settings.R.string.page_tab_title_support))).
                check(matches(not(isSelected())));
    }

    private void verifySupportSelected() {
        onView(withText(mActivityRule.getActivity().getApplicationContext().
                getString(com.android.settings.R.string.page_tab_title_support))).
                check(matches(isSelected()));
    }

    private void launchSettingsWithFlags(String extra, int flags) {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        intent.setFlags(flags);
        intent.putExtra(DashboardContainerFragment.EXTRA_SELECT_SETTINGS_TAB, extra);
        mActivityRule.launchActivity(intent);
    }

    private void launchSettingsNoFlags(String extra) {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        intent.putExtra(DashboardContainerFragment.EXTRA_SELECT_SETTINGS_TAB, extra);
        mActivityRule.launchActivity(intent);
    }
}