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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE;

import android.content.Context;
import android.text.SpannableStringBuilder;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;
import com.android.settingslib.utils.StringUtil;

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
                    ? R.drawable.accessibility_shortcut_type_gesture_touch_explore_on
                    : R.drawable.accessibility_shortcut_type_gesture;
            shortcutOptionPreference.setIntroImageResId(resId);
        }
    }

    @Override
    protected int getShortcutType() {
        return android.provider.Flags.a11yStandaloneGestureEnabled()
                ? GESTURE : super.getShortcutType();
    }

    @Override
    protected boolean isShortcutAvailable() {
        if (android.provider.Flags.a11yStandaloneGestureEnabled()) {
            return !isInSetupWizard()
                    && AccessibilityUtil.isGestureNavigateEnabled(mContext);
        } else {
            return !isInSetupWizard()
                    && AccessibilityUtil.isGestureNavigateEnabled(mContext)
                    && !AccessibilityUtil.isFloatingMenuEnabled(mContext);
        }
    }

    @Override
    public CharSequence getSummary() {
        int numFingers = AccessibilityUtil.isTouchExploreEnabled(mContext) ? 3 : 2;
        String instruction = StringUtil.getIcuPluralsString(
                mContext,
                numFingers,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture);

        final SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(instruction);
        if (!isInSetupWizard() && !android.provider.Flags.a11yStandaloneGestureEnabled()) {
            sb.append("\n\n").append(getCustomizeAccessibilityButtonLink());
        }

        return sb;
    }
}
