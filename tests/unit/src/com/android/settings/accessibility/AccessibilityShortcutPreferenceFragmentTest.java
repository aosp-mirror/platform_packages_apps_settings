/*
 * Copyright (C) 2018 The Android Open Source Project
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
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;

import android.app.Instrumentation;
import android.os.Bundle;

import android.provider.Settings;
import android.widget.CompoundButton;

import androidx.test.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.Settings.AccessibilitySettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AccessibilityShortcutPreferenceFragmentTest {
    @Rule
    public final ActivityTestRule<AccessibilitySettingsActivity> mActivityRule =
            new ActivityTestRule<>(AccessibilitySettingsActivity.class, true);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private AccessibilityShortcutPreferenceFragment mAccessibilityShortcutPreferenceFragment;
    private AccessibilitySettingsActivity mActivity;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
    }

    @Test
    public void lockScreenPreference_setOnBeforeDialogShown_isOn() {
        setDialogShown(false);
        setOnLockscreen(true);
        startFragment();
        assertLockscreenSwitchIsCheckedIs(true);
    }

    @Test
    public void lockScreenPreference_defaultAfterDialogShown_isOn() {
        setDialogShown(true);
        setOnLockscreen(null);
        startFragment();
        assertLockscreenSwitchIsCheckedIs(true);
    }

    private void startFragment() {
        mInstrumentation.runOnMainSync(() -> {
            new SubSettingLauncher(mActivity)
                    .setDestination(AccessibilityShortcutPreferenceFragment.class.getName())
                    .setArguments(new Bundle())
                    .setSourceMetricsCategory(
                            InstrumentedPreferenceFragment.METRICS_CATEGORY_UNKNOWN)
                    .launch();
        });
    }

    private void setDialogShown(boolean shown) {
        Settings.Secure.putInt(mActivity.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN, shown ? 1 : 0);
    }

    private void setOnLockscreen(Boolean onLockscreen) {
        if (onLockscreen == null) {
            Settings.Secure.putString(mActivity.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, null);
        } else {
            Settings.Secure.putInt(mActivity.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, onLockscreen ? 1 : 0);
        }
    }

    private void assertLockscreenSwitchIsCheckedIs(boolean isChecked) {
        // Identify the switch by looking for a grandparent that has a descendent with the
        // switch label. To disambiguate, make sure that grandparent doesn't also have a descendant
        // with the title of the main switch
        final String lockScreenSwitchTitle =
                mActivity.getString(R.string.accessibility_shortcut_service_on_lock_screen_title);
        final String mainSwitchTitle =
                mActivity.getString(R.string.accessibility_service_master_switch_title);
        Matcher isCheckedMatcher = (isChecked) ? isChecked() : isNotChecked();
        Matcher hasLockScreenTitleDescendant = hasDescendant(withText(lockScreenSwitchTitle));
        Matcher noMainSwitchTitleDescendant = not(hasDescendant(withText(mainSwitchTitle)));
        onView(allOf(withParent(withParent(allOf(
                hasLockScreenTitleDescendant, noMainSwitchTitleDescendant))),
                instanceOf(CompoundButton.class))).check(matches(isCheckedMatcher));
    }
}
