/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.privacy;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

/**
 * Controller for preference to toggle whether clipboard access notifications should be shown.
 */
public class ShowClipAccessNotificationPreferenceController extends TogglePreferenceController {

    private static final String KEY_SHOW_CLIP_ACCESS_NOTIFICATION = "show_clip_access_notification";

    public ShowClipAccessNotificationPreferenceController(Context context) {
        super(context, KEY_SHOW_CLIP_ACCESS_NOTIFICATION);
    }

    @Override
    public boolean isChecked() {
        // TODO(b/182349993) Retrieve default value from DeviceConfig.
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS, isChecked ? 1 : 0);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

}
