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

package com.android.settings.accessibility;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Preference controller that controls the text content of Flash Notifications intro. */
public class FlashNotificationsIntroPreferenceController extends BasePreferenceController {

    public FlashNotificationsIntroPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final int titleResource = FlashNotificationsUtil.isTorchAvailable(mContext)
                ? R.string.flash_notifications_intro
                : R.string.flash_notifications_intro_without_camera_flash;
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setTitle(titleResource);
        }
    }
}
