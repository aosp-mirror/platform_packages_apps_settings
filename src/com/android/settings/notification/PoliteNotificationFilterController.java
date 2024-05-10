/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;


/**
 * Controls whether polite notifications are enabled and apply to all apps or just to conversations.
 */
public class PoliteNotificationFilterController extends BasePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {
    static final String TAG = "PoliteNotificationFilterController";

    private static final int POLITE_NOTIFICATIONS_ALL = 0;
    private static final int POLITE_NOTIFICATIONS_CONVERSATIONS = 1;
    private static final int POLITE_NOTIFICATIONS_DISABLED = 2;

    public PoliteNotificationFilterController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO: b/291897570 - remove this when the feature flag is removed!
        return Flags.politeNotifications() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference pref = (ListPreference) preference;

        if (isPoliteNotifDisabled()) {
            pref.setValue(Integer.toString(POLITE_NOTIFICATIONS_DISABLED));
        } else if (shouldApplyForAllApps()) {
            pref.setValue(Integer.toString(POLITE_NOTIFICATIONS_ALL));
        } else {
            pref.setValue(Integer.toString(POLITE_NOTIFICATIONS_CONVERSATIONS));
        }
    }

    @Override
    public CharSequence getSummary() {
        if (isPoliteNotifDisabled()) {
            return mContext.getString(R.string.notification_polite_disabled_summary);
        }
        if (shouldApplyForAllApps()) {
            return mContext.getString(R.string.notification_polite_all_apps_summary);
        } else {
            return mContext.getString(R.string.notification_polite_conversations_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int prefValue = Integer.parseInt((String) newValue);
        if (prefValue == POLITE_NOTIFICATIONS_ALL) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON);
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.NOTIFICATION_COOLDOWN_ALL, ON);
        } else if (prefValue == POLITE_NOTIFICATIONS_CONVERSATIONS) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON);
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.NOTIFICATION_COOLDOWN_ALL, OFF);
        } else if (prefValue == POLITE_NOTIFICATIONS_DISABLED) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.NOTIFICATION_COOLDOWN_ENABLED, OFF);
        } else {
            Log.e(TAG, "Unexpected preference value: " + prefValue);
        }
        refreshSummary(preference);
        return true;
    }

    private boolean isPoliteNotifDisabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON) == OFF;
    }

    private boolean shouldApplyForAllApps() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ALL, ON) != OFF;
    }
}
