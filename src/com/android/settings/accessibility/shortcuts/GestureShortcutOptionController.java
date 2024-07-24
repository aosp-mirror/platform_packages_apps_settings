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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;

/**
 * A controller handles displaying the gesture shortcut option preference and
 * configuring the shortcut.
 */
public class GestureShortcutOptionController extends SoftwareShortcutOptionPreferenceController {

    public GestureShortcutOptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof ShortcutOptionPreference shortcutOptionPreference) {
            shortcutOptionPreference.setTitle(
                    R.string.accessibility_shortcut_edit_dialog_title_software_by_gesture);

            int resId = AccessibilityUtil.isTouchExploreEnabled(mContext)
                    ? R.drawable.a11y_shortcut_type_software_gesture_talkback
                    : R.drawable.a11y_shortcut_type_software_gesture;
            shortcutOptionPreference.setIntroImageResId(resId);
        }
    }

    @Override
    protected boolean isShortcutAvailable() {
        return !isInSetupWizard()
                && !AccessibilityUtil.isFloatingMenuEnabled(mContext)
                && AccessibilityUtil.isGestureNavigateEnabled(mContext);
    }

    @Override
    public CharSequence getSummary() {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        final int resId = AccessibilityUtil.isTouchExploreEnabled(mContext)
                ? R.string.accessibility_shortcut_edit_dialog_summary_software_gesture_talkback
                : R.string.accessibility_shortcut_edit_dialog_summary_software_gesture;
        sb.append(mContext.getText(resId));
        sb.append("\n\n");
        sb.append(getCustomizeAccessibilityButtonLink());

        return sb;
    }
}
