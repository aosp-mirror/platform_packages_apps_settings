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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Settings page for accessibility shortcut
 */
public class AccessibilityShortcutPreferenceController extends TogglePreferenceController {

    public AccessibilityShortcutPreferenceController(Context context, String preferenceKey) {
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
        return AVAILABLE;
    }
}
