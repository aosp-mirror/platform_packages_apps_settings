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

package com.android.settings.notification;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controls the toggle that determines whether notification cooldown
 * should apply to work profiles.
 */
public class PoliteNotifWorkProfileToggleController extends TogglePreferenceController {

    private final int mManagedProfileId;

    public PoliteNotifWorkProfileToggleController(Context context, String preferenceKey) {
        this(context, preferenceKey, new AudioHelper(context));
    }

    @VisibleForTesting
    PoliteNotifWorkProfileToggleController(Context context, String preferenceKey,
                AudioHelper helper) {
        super(context, preferenceKey);
        mManagedProfileId = helper.getManagedProfileId(UserManager.get(mContext));
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO: b/291897570 - remove this when the feature flag is removed!
        if (!Flags.politeNotifications()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        return (mManagedProfileId != UserHandle.USER_NULL) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON, mManagedProfileId) != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, (isChecked ? ON : OFF),
                mManagedProfileId);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }
}
