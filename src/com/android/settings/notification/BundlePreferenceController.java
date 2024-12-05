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

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for the bundled notifications settings page.
 */
public class BundlePreferenceController extends BasePreferenceController {

    NotificationBackend mBackend;

    public BundlePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBackend = new NotificationBackend();
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.notificationClassificationUi() && mBackend.isNotificationBundlingSupported()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mBackend.isNotificationBundlingEnabled(mContext)
                ? mContext.getString(R.string.notification_bundle_on)
                : mContext.getString(R.string.notification_bundle_off);
    }
}
