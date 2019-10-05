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
 * limitations under the License
 */

package com.android.settings.notification;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ConfigureNotificationPreferenceController extends BasePreferenceController {

    private NotificationBackend mBackend;

    public ConfigureNotificationPreferenceController(Context context, String key) {
        super(context, key);
        mBackend = new NotificationBackend();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final int blockedAppCount = mBackend.getBlockedAppCount();
        if (blockedAppCount == 0) {
            return mContext.getText(R.string.app_notification_listing_summary_zero);
        }
        return mContext.getResources().getQuantityString(
                R.plurals.app_notification_listing_summary_others,
                blockedAppCount, blockedAppCount);
    }
}