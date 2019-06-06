/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.provider.Settings;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

public class GentleDrawablePreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final int ON = 1;

    private NotificationBackend mBackend;

    public GentleDrawablePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBackend = new NotificationBackend();
    }

    @VisibleForTesting
    void setBackend(NotificationBackend backend) {
        mBackend = backend;
    }

    @Override
    public void updateState(Preference preference) {
        LayoutPreference pref = (LayoutPreference) preference;
        boolean showOnLockscreen = showOnLockscreen();
        boolean showOnStatusBar = showOnStatusBar();

        ImageView view = pref.findViewById(R.id.drawable);

        if (showOnLockscreen) {
            if (showOnStatusBar) {
                view.setImageResource(R.drawable.gentle_notifications_shade_status_lock);
            } else {
                view.setImageResource(R.drawable.gentle_notifications_shade_lock);
            }
        } else if (showOnStatusBar) {
            view.setImageResource(R.drawable.gentle_notifications_shade_status);
        } else {
            view.setImageResource(R.drawable.gentle_notifications_shade);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private boolean showOnLockscreen() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_SILENT_NOTIFICATIONS, ON) == ON;
    }

    private boolean showOnStatusBar() {
        return !mBackend.shouldHideSilentStatusBarIcons(mContext);
    }
}
