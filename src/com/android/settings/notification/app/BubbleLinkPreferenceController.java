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
import android.content.Intent;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.notification.BubbleHelper;

public class BubbleLinkPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "notification_bubbles";

    public BubbleLinkPreferenceController(Context context) {
        super(context, null);
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        return BubbleHelper.isEnabledSystemWide(mContext);
    }

    @Override
    boolean isIncludedInFilter() {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (mAppRow != null) {
            final Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, mAppRow.pkg);
            intent.putExtra(Settings.EXTRA_APP_UID, mAppRow.uid);
            preference.setIntent(intent);
        }
    }
}
