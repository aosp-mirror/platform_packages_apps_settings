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

import static android.provider.Settings.Global.NOTIFICATION_BUBBLES;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

public class BubbleCategoryPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "bubbles";
    @VisibleForTesting
    static final int ON = 1;

    public BubbleCategoryPreferenceController(Context context) {
        super(context, null);
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        return areBubblesEnabled();
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


    private boolean areBubblesEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUBBLES, ON) == ON;
    }
}
