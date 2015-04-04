/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.service.notification.ZenModeConfig;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.android.settings.search.Indexable;

public class ZenModePrioritySettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_REMINDERS = "reminders";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CALLS = "calls";
    private static final String KEY_STARRED = "starred";

    private boolean mDisableListeners;
    private SwitchPreference mReminders;
    private SwitchPreference mEvents;
    private SwitchPreference mMessages;
    private SwitchPreference mCalls;
    private DropDownPreference mStarred;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_priority_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mReminders = (SwitchPreference) root.findPreference(KEY_REMINDERS);
        mReminders.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                if (val == mConfig.allowReminders) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowReminders=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowReminders = val;
                return setZenModeConfig(newConfig);
            }
        });

        mEvents = (SwitchPreference) root.findPreference(KEY_EVENTS);
        mEvents.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                if (val == mConfig.allowEvents) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowEvents=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowEvents = val;
                return setZenModeConfig(newConfig);
            }
        });

        mMessages = (SwitchPreference) root.findPreference(KEY_MESSAGES);
        mMessages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                if (val == mConfig.allowMessages) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowMessages=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowMessages = val;
                return setZenModeConfig(newConfig);
            }
        });

        mCalls = (SwitchPreference) root.findPreference(KEY_CALLS);
        mCalls.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                if (val == mConfig.allowCalls) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowCalls=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowCalls = val;
                return setZenModeConfig(newConfig);
            }
        });

        mStarred = (DropDownPreference) root.findPreference(KEY_STARRED);
        mStarred.addItem(R.string.zen_mode_from_anyone, ZenModeConfig.SOURCE_ANYONE);
        mStarred.addItem(R.string.zen_mode_from_contacts, ZenModeConfig.SOURCE_CONTACT);
        mStarred.addItem(R.string.zen_mode_from_starred, ZenModeConfig.SOURCE_STAR);
        mStarred.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object newValue) {
                if (mDisableListeners) return true;
                final int val = (Integer) newValue;
                if (val == mConfig.allowFrom) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowFrom=" +
                        ZenModeConfig.sourceToString(val));
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowFrom = val;
                return setZenModeConfig(newConfig);
            }
        });

        updateControls();
    }

    @Override
    protected void onZenModeChanged() {
        // don't care
    }

    @Override
    protected void onZenModeConfigChanged() {
        updateControls();
    }

    private void updateControls() {
        mDisableListeners = true;
        if (mCalls != null) {
            mCalls.setChecked(mConfig.allowCalls);
        }
        mMessages.setChecked(mConfig.allowMessages);
        mStarred.setSelectedValue(mConfig.allowFrom);
        mReminders.setChecked(mConfig.allowReminders);
        mEvents.setChecked(mConfig.allowEvents);
        updateStarredEnabled();
        mDisableListeners = false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_PRIORITY;
    }

    private void updateStarredEnabled() {
        mStarred.setEnabled(mConfig.allowCalls || mConfig.allowMessages);
    }

}
