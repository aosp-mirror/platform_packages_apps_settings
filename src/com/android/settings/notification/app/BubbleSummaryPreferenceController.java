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

import static android.app.NotificationManager.BUBBLE_PREFERENCE_ALL;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_NONE;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.notification.BubbleHelper;
import com.android.settings.notification.NotificationBackend;

/**
 * Summary of the app setting for bubbles, available through app notification settings.
 */
public class BubbleSummaryPreferenceController extends NotificationPreferenceController {
    private static final String KEY = "bubble_pref_link";

    public BubbleSummaryPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        if (mAppRow == null) {
            return false;
        }
        if (mChannel != null) {
            if (!isGloballyEnabled()) {
                return false;
            }
            if (isDefaultChannel()) {
                return true;
            } else {
                return mAppRow != null;
            }
        }
        return isGloballyEnabled() && mBackend.hasSentValidBubble(mAppRow.pkg, mAppRow.uid);
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

    @Override
    public CharSequence getSummary() {
        if (mAppRow == null) {
            return null;
        }
        int backEndPref = mAppRow.bubblePreference;
        Resources res = mContext.getResources();
        if (backEndPref == BUBBLE_PREFERENCE_NONE || !isGloballyEnabled()) {
            return res.getString(R.string.bubble_app_setting_none);
        } else if (backEndPref == BUBBLE_PREFERENCE_ALL) {
            return res.getString(R.string.bubble_app_setting_all);
        } else {
            return res.getString(R.string.bubble_app_setting_selected);
        }
    }

    private boolean isGloballyEnabled() {
        return BubbleHelper.isEnabledSystemWide(mContext);
    }
}
