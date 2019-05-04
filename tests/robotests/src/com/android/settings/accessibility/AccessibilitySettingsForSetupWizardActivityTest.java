/*
 * Copyright 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilitySettingsForSetupWizardActivity.EXTRA_GO_TO_FONT_SIZE_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;

import androidx.test.filters.SmallTest;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.display.FontSizePreferenceFragmentForSetupWizard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
@SmallTest
public class AccessibilitySettingsForSetupWizardActivityTest {

  @Test
  public void createSetupAccessibilityActivity_shouldBeSUWTheme() {
    final Intent intent = new Intent();
    AccessibilitySettingsForSetupWizardActivity activity =
        Robolectric.buildActivity(AccessibilitySettingsForSetupWizardActivity.class, intent).get();

    assertThat(activity.getThemeResId()).isEqualTo(R.style.GlifV3Theme_Light);
  }

  @Test
  public void onCreate_whenHasFontSizeExtra_shouldGoToFontSizePreferenceDirectly() {
    AccessibilitySettingsForSetupWizardActivity activity =
            Robolectric.buildActivity(AccessibilitySettingsForSetupWizardActivity.class,
                    new Intent().putExtra(EXTRA_GO_TO_FONT_SIZE_PREFERENCE, true).
                                 putExtra("isSetupFlow", true)).get();

    activity.tryLaunchFontSizePreference();

    final Intent launchIntent = Shadows.shadowOf(activity).getNextStartedActivity();
    assertThat(launchIntent).isNotNull();
    assertThat(launchIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
            FontSizePreferenceFragmentForSetupWizard.class.getName());
    assertThat(activity.isFinishing()).isTrue();
  }
}
