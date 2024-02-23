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

package com.android.settings.privatespace;

import static android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.settings.core.TogglePreferenceController;

import java.util.Objects;

/**
 * A controller object for sensitive notifications in Private Space settings page.
 */
public class HidePrivateSpaceSensitiveNotificationsController extends TogglePreferenceController {
    private final PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    private final UserHandle mPrivateProfileId;
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;
    private static final int DEVICE_SENSITIVE_NOTIFICATIONS_DEFAULT = ENABLED;
    private static final int DEVICE_LOCK_SCREEN_NOTIFICATIONS_DEFAULT = ENABLED;
    private static final int PRIVATE_SPACE_SENSITIVE_NOTIFICATIONS_DEFAULT = DISABLED;

    public HidePrivateSpaceSensitiveNotificationsController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(context);
        mPrivateProfileId = Objects.requireNonNull(
                mPrivateSpaceMaintainer.getPrivateProfileHandle());
    }

    @Override
    public int getAvailabilityStatus() {
        if (!android.os.Flags.allowPrivateProfile()
                || !android.multiuser.Flags.enablePsSensitiveNotificationsToggle()
                || !mPrivateSpaceMaintainer.doesPrivateSpaceExist()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!getLockscreenNotificationsEnabled(mContext)
                || !getLockscreenSensitiveNotificationsEnabledOnDevice(mContext)) {
            return DISABLED_DEPENDENT_SETTING;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                PRIVATE_SPACE_SENSITIVE_NOTIFICATIONS_DEFAULT, mPrivateProfileId.getIdentifier())
                != DISABLED;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                isChecked ? ENABLED : DISABLED, mPrivateProfileId.getIdentifier());
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    /**
     * If notifications are disabled on the device, the toggle for private space sensitive
     * notifications should be unavailable.
     */
    private static boolean getLockscreenNotificationsEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                DEVICE_LOCK_SCREEN_NOTIFICATIONS_DEFAULT) != DISABLED;
    }

    /**
     * If sensitive notifications are hidden on the device, they should be hidden for private space
     * also.
     */
    private static boolean getLockscreenSensitiveNotificationsEnabledOnDevice(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                DEVICE_SENSITIVE_NOTIFICATIONS_DEFAULT) != DISABLED;
    }
}
