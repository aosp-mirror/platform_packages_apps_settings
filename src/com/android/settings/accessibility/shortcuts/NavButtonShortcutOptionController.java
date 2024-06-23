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
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;

/**
 * A controller handles displaying the nav button shortcut option preference and
 * configuring the shortcut.
 */
public class NavButtonShortcutOptionController extends SoftwareShortcutOptionPreferenceController {

    public NavButtonShortcutOptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof ShortcutOptionPreference shortcutOptionPreference) {
            shortcutOptionPreference.setTitle(
                    R.string.accessibility_shortcut_edit_dialog_title_software);
            shortcutOptionPreference.setIntroImageResId(R.drawable.a11y_shortcut_type_software);
            shortcutOptionPreference.setSummaryProvider(
                    new Preference.SummaryProvider<ShortcutOptionPreference>() {
                        @Override
                        public CharSequence provideSummary(
                                @NonNull ShortcutOptionPreference preference) {
                            return getSummary(preference.getSummaryTextLineHeight());
                        }
                    });
        }
    }

    @Override
    protected boolean isShortcutAvailable() {
        return !AccessibilityUtil.isFloatingMenuEnabled(mContext)
                && !AccessibilityUtil.isGestureNavigateEnabled(mContext);
    }

    private CharSequence getSummary(int lineHeight) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(getSummaryStringWithIcon(lineHeight));

        if (!isInSetupWizard()) {
            sb.append("\n\n");
            sb.append(getCustomizeAccessibilityButtonLink());
        }

        return sb;
    }

    private SpannableString getSummaryStringWithIcon(int lineHeight) {
        final String summary = mContext
                .getString(R.string.accessibility_shortcut_edit_dialog_summary_software);
        final SpannableString spannableMessage = SpannableString.valueOf(summary);

        // Icon
        final int indexIconStart = summary.indexOf("%s");
        final int indexIconEnd = indexIconStart + 2;
        final Drawable icon = mContext.getDrawable(R.drawable.ic_accessibility_new);
        final ImageSpan imageSpan = new ImageSpan(icon);
        imageSpan.setContentDescription("");
        icon.setBounds(
                /* left= */ 0, /* top= */ 0, /* right= */ lineHeight, /* bottom= */ lineHeight);
        spannableMessage.setSpan(
                imageSpan, indexIconStart, indexIconEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableMessage;
    }
}
