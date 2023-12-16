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

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;

import android.content.Context;
import android.icu.text.MessageFormat;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;

import java.util.Set;

/**
 * A controller handles displaying the two fingers double tap shortcut option preference and
 * configuring the shortcut.
 */
public class TwoFingersDoubleTapShortcutOptionController
        extends ShortcutOptionPreferenceController {

    public TwoFingersDoubleTapShortcutOptionController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @ShortcutConstants.UserShortcutType
    @Override
    protected int getShortcutType() {
        return ShortcutConstants.UserShortcutType.TRIPLETAP;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof ShortcutOptionPreference shortcutOptionPreference) {
            // TODO (b/306153204): Update shortcut string and image when UX provides them
            shortcutOptionPreference.setTitle(
                    R.string.accessibility_shortcut_edit_dialog_title_two_finger_double_tap);
            String summary = mContext.getString(
                    R.string.accessibility_shortcut_edit_dialog_summary_two_finger_double_tap);
            // Format the number '2' in the summary.
            final Object[] arguments = {2};
            summary = MessageFormat.format(summary, arguments);

            shortcutOptionPreference.setSummary(summary);
            shortcutOptionPreference.setIntroImageRawResId(
                    R.raw.a11y_shortcut_type_triple_tap);
        }
    }

    @Override
    protected boolean isShortcutAvailable() {
        if (!Flags.enableMagnificationMultipleFingerMultipleTapGesture()) {
            return false;
        }
        // Only Magnification has two fingers triple tap shortcut option.
        Set<String> shortcutTargets = getShortcutTargets();
        return shortcutTargets.size() == 1
                && shortcutTargets.contains(MAGNIFICATION_CONTROLLER_NAME);
    }

    @Override
    protected boolean isChecked() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                AccessibilityUtil.State.OFF) == AccessibilityUtil.State.ON;
    }

    @Override
    protected void enableShortcutForTargets(boolean enable) {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_TWO_FINGER_TRIPLE_TAP_ENABLED,
                enable ? AccessibilityUtil.State.ON : AccessibilityUtil.State.OFF);
    }
}
