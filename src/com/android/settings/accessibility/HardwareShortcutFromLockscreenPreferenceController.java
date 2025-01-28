/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Setting to allow the hardware shortcut to turn on from the lock screen
 */
public class HardwareShortcutFromLockscreenPreferenceController
        extends TogglePreferenceController {
    public HardwareShortcutFromLockscreenPreferenceController(
            Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        final ContentResolver cr = mContext.getContentResolver();
        // The shortcut is enabled by default on the lock screen as long as the user has
        // enabled the shortcut with the warning dialog
        final int dialogShown = Settings.Secure.getInt(
                cr, Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN, OFF);
        final boolean enabledFromLockScreen = Settings.Secure.getInt(
                cr, Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, dialogShown) == ON;
        return enabledFromLockScreen;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN, isChecked ? ON : OFF,
                UserHandle.USER_CURRENT);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!com.android.settings.accessibility.Flags.fixA11ySettingsSearch()) {
            return AVAILABLE;
        } else {
            if (mContext.getSystemService(AccessibilityManager.class)
                    .getAccessibilityShortcutTargets(HARDWARE).isEmpty()) {
                return DISABLED_DEPENDENT_SETTING;
            } else {
                return AVAILABLE;
            }
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public @NonNull CharSequence getSummary() {
        if (getAvailabilityStatus() == AVAILABLE) {
            return mContext.getString(R.string.accessibility_shortcut_description);
        } else {
            return mContext.getString(
                    R.string.accessibility_shortcut_unassigned_setting_unavailable_summary,
                    AccessibilityUtil.getShortcutSummaryList(mContext, HARDWARE));
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
