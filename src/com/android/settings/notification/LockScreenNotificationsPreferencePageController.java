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

import android.content.Context;

import com.android.server.notification.Flags;
import com.android.settings.core.BasePreferenceController;

// TODO(b/367455695): remove controller when the feature flag is removed!

/**
 * Controller for lock screen notifications settings page.
 */
public class LockScreenNotificationsPreferencePageController extends BasePreferenceController {

    public LockScreenNotificationsPreferencePageController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.notificationLockScreenSettings() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

}
