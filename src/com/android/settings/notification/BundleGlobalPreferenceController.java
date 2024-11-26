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

package com.android.settings.notification;

import android.app.Flags;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.widget.SettingsMainSwitchPreferenceController;

public class BundleGlobalPreferenceController extends
        SettingsMainSwitchPreferenceController {

    NotificationBackend mBackend;

    public BundleGlobalPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mBackend = new NotificationBackend();
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.notificationClassificationUi() && mBackend.isNotificationBundlingSupported()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mBackend.isNotificationBundlingEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setNotificationBundlingEnabled(isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
