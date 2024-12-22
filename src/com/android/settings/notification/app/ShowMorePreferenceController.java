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

package com.android.settings.notification.app;

import static com.android.server.notification.Flags.notificationHideUnusedChannels;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;

import org.jetbrains.annotations.NotNull;

public class ShowMorePreferenceController extends NotificationPreferenceController {

    private static final String KEY = "more";
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    public ShowMorePreferenceController(Context context,
            NotificationSettings.DependentFieldListener dependentFieldListener,
            NotificationBackend backend) {
        super(context, backend);
        mDependentFieldListener = dependentFieldListener;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!notificationHideUnusedChannels()) {
            return false;
        }
        if (mAppRow == null) {
            return false;
        }
        if (mAppRow.banned || mAppRow.showAllChannels) {
            return false;
        }
        return true;
    }

    @Override
    boolean isIncludedInFilter() {
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setOnPreferenceClickListener(preference1 -> {
            mAppRow.showAllChannels = true;
            mDependentFieldListener.onFieldValueChanged();
            return true;
        });
    }
}
