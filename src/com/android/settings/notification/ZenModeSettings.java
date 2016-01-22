/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;

public class ZenModeSettings extends ZenModeSettingsBase {
    private static final String KEY_PRIORITY_SETTINGS = "priority_settings";

    private Preference mPrioritySettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPrioritySettings = root.findPreference(KEY_PRIORITY_SETTINGS);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
        updateControls();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected void onZenModeChanged() {
        updateControls();
    }

    @Override
    protected void onZenModeConfigChanged() {
        updateControls();
    }

    private void updateControls() {
        updatePrioritySettingsSummary();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.zen_settings_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.zen_access:
                ((SettingsActivity) getActivity()).startPreferencePanel(
                            ZenAccessSettings.class.getCanonicalName(), null,
                            R.string.manage_zen_access_title, null, this, 0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updatePrioritySettingsSummary() {
        Policy policy = NotificationManager.from(mContext).getNotificationPolicy();
        String s = getResources().getString(R.string.zen_mode_alarms);
        s = appendLowercase(s, isCategoryEnabled(policy, Policy.PRIORITY_CATEGORY_REMINDERS),
                R.string.zen_mode_reminders);
        s = appendLowercase(s, isCategoryEnabled(policy, Policy.PRIORITY_CATEGORY_EVENTS),
                R.string.zen_mode_events);
        if (isCategoryEnabled(policy, Policy.PRIORITY_CATEGORY_MESSAGES)) {
            if (policy.priorityMessageSenders == Policy.PRIORITY_SENDERS_ANY) {
                s = appendLowercase(s, true, R.string.zen_mode_all_messages);
            } else {
                s = appendLowercase(s, true, R.string.zen_mode_selected_messages);
            }
        }
        if (isCategoryEnabled(policy, Policy.PRIORITY_CATEGORY_CALLS)) {
            if (policy.priorityCallSenders == Policy.PRIORITY_SENDERS_ANY) {
                s = appendLowercase(s, true, R.string.zen_mode_all_callers);
            } else {
                s = appendLowercase(s, true, R.string.zen_mode_selected_callers);
            }
        } else if (isCategoryEnabled(policy, Policy.PRIORITY_CATEGORY_REPEAT_CALLERS)) {
            s = appendLowercase(s, true, R.string.zen_mode_repeat_callers);
        }
        mPrioritySettings.setSummary(s);
    }

    private boolean isCategoryEnabled(Policy policy, int categoryType) {
        return (policy.priorityCategories & categoryType) != 0;
    }

    private String appendLowercase(String s, boolean condition, int resId) {
        if (condition) {
            return getResources().getString(R.string.join_many_items_middle, s,
                    getResources().getString(resId).toLowerCase());
        }
        return s;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_interruptions;
    }
}
