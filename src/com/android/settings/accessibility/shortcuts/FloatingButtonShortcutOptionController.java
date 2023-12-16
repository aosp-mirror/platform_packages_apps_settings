/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility.shortcuts;

import android.content.Context;
import android.text.SpannableStringBuilder;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;

/**
 * A controller handles displaying the floating action button shortcut option preference and
 * configuring the shortcut.
 */
public class FloatingButtonShortcutOptionController
        extends SoftwareShortcutOptionPreferenceController {

    public FloatingButtonShortcutOptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof ShortcutOptionPreference shortcutOptionPreference) {
            shortcutOptionPreference.setTitle(
                    R.string.accessibility_shortcut_edit_dialog_title_software);
            shortcutOptionPreference.setIntroImageResId(
                    R.drawable.a11y_shortcut_type_software_floating);
        }
    }

    @Override
    protected boolean isShortcutAvailable() {
        return AccessibilityUtil.isFloatingMenuEnabled(mContext);
    }

    @Nullable
    @Override
    public CharSequence getSummary() {
        if (isInSetupWizard()) {
            return null;
        }
        return new SpannableStringBuilder().append(getCustomizeAccessibilityButtonLink());
    }
}
