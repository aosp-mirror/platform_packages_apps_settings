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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil;

/**
 * A controller handles displaying the volume keys shortcut option preference and
 * configuring the shortcut.
 */
public class VolumeKeysShortcutOptionController extends ShortcutOptionPreferenceController {

    public VolumeKeysShortcutOptionController(
            Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @ShortcutConstants.UserShortcutType
    @Override
    protected int getShortcutType() {
        return ShortcutConstants.UserShortcutType.HARDWARE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof ShortcutOptionPreference shortcutOptionPreference) {
            shortcutOptionPreference.setTitle(
                    R.string.accessibility_shortcut_edit_dialog_title_hardware);
            shortcutOptionPreference.setSummary(
                    R.string.accessibility_shortcut_edit_dialog_summary_hardware);
            shortcutOptionPreference.setIntroImageResId(
                    R.drawable.a11y_shortcut_type_hardware);
        }
    }

    @Override
    protected boolean isShortcutAvailable() {
        return true;
    }

    @Override
    protected void enableShortcutForTargets(boolean enable) {
        super.enableShortcutForTargets(enable);
        if (enable) {
            AccessibilityUtil.skipVolumeShortcutDialogTimeoutRestriction(mContext);
        }
    }
}
