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

package com.android.settings.notification.app;

import android.content.Context;
import android.util.Slog;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

public class AddToHomeScreenPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "HomeScreenPref";
    private static final String KEY = "add_to_home";

    public AddToHomeScreenPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        return mConversationInfo != null;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY.equals(preference.getKey())) {
            try {
                mBackend.requestPinShortcut(mContext, mConversationInfo);
                return true;
            } catch (SecurityException e) {
                Slog.e(TAG, "Cannot add to home screen", e);
            }
        }
        return false;
    }
}
