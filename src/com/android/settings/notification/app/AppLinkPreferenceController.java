/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;

/**
 * Controls link to reach more preference settings inside the app.
 */
public class AppLinkPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "AppLinkPrefContr";
    private static final String KEY_APP_LINK = "app_link";

    public AppLinkPreferenceController(Context context) {
        super(context, null);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_APP_LINK;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        return mAppRow.settingsIntent != null;
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            preference.setIntent(mAppRow.settingsIntent);
        }
    }
}
