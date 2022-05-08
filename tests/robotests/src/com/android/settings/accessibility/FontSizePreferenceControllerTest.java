/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.VISION_FRAGMENT_NO;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;

import androidx.preference.Preference;

import com.android.settings.accessibility.AccessibilityScreenSizeForSetupWizardActivity.FragmentType;
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link FontSizePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class FontSizePreferenceControllerTest {
    private static final String TEST_KEY = "test_key";

    private Activity mActivity;
    private FontSizePreferenceController mController;
    Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(Activity.class);
        mController = new FontSizePreferenceController(mActivity, TEST_KEY);
        mPreference = new Preference(mActivity);
        mPreference.setKey(TEST_KEY);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_launchActivityWithExpectedValues() {
        mController.handlePreferenceTreeClick(mPreference);

        final Intent nextActivity = shadowOf(mActivity).getNextStartedActivity();
        assertThat(nextActivity.getIntExtra(VISION_FRAGMENT_NO, /* defaultValue= */-1))
                .isEqualTo(FragmentType.FONT_SIZE);
        assertThat(nextActivity.getIntExtra(EXTRA_PAGE_TRANSITION_TYPE, /* defaultValue= */-1))
                .isEqualTo(TransitionType.TRANSITION_FADE);
    }
}
