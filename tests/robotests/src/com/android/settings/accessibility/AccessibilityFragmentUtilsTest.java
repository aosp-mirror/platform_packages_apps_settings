/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.IllustrationPreference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityFragmentUtils} */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityFragmentUtilsTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void isPreferenceImportantToA11y_basicPreference_isImportant() {
        final Preference pref = new ShortcutPreference(mContext, /* attrs= */ null);

        assertThat(AccessibilityFragmentUtils.isPreferenceImportantToA11y(pref)).isTrue();
    }

    @Test
    public void isPreferenceImportantToA11y_illustrationPreference_hasContentDesc_isImportant() {
        final IllustrationPreference pref =
                new IllustrationPreference(mContext, /* attrs= */ null);
        pref.setContentDescription("content desc");

        assertThat(AccessibilityFragmentUtils.isPreferenceImportantToA11y(pref)).isTrue();
    }

    @Test
    public void isPreferenceImportantToA11y_illustrationPreference_noContentDesc_notImportant() {
        final IllustrationPreference pref =
                new IllustrationPreference(mContext, /* attrs= */ null);
        pref.setContentDescription(null);

        assertThat(AccessibilityFragmentUtils.isPreferenceImportantToA11y(pref)).isFalse();
    }

    @Test
    public void isPreferenceImportantToA11y_paletteListPreference_notImportant() {
        final PaletteListPreference pref =
                new PaletteListPreference(mContext, /* attrs= */ null);

        assertThat(AccessibilityFragmentUtils.isPreferenceImportantToA11y(pref)).isFalse();
    }
}
