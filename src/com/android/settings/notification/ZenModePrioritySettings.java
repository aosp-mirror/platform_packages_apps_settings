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

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.os.Bundle;
import android.service.notification.ZenModeConfig;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.Indexable;

public class ZenModePrioritySettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_REMINDERS = "reminders";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CALLS = "calls";
    private static final String KEY_REPEAT_CALLERS = "repeat_callers";

    private static final int SOURCE_NONE = -1;

    private boolean mDisableListeners;
    private SwitchPreference mReminders;
    private SwitchPreference mEvents;
    private DropDownPreference mMessages;
    private DropDownPreference mCalls;
    private SwitchPreference mRepeatCallers;

    private Policy mPolicy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_priority_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();

        mReminders = (SwitchPreference) root.findPreference(KEY_REMINDERS);
        mReminders.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_REMINDERS, val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowReminders=" + val);
                savePolicy(getNewPriorityCategories(val, Policy.PRIORITY_CATEGORY_REMINDERS),
                        mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                        mPolicy.suppressedVisualEffects);
                return true;
            }
        });

        mEvents = (SwitchPreference) root.findPreference(KEY_EVENTS);
        mEvents.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_EVENTS, val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowEvents=" + val);
                savePolicy(getNewPriorityCategories(val, Policy.PRIORITY_CATEGORY_EVENTS),
                        mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                        mPolicy.suppressedVisualEffects);
                return true;
            }
        });

        mMessages = (DropDownPreference) root.findPreference(KEY_MESSAGES);
        addSources(mMessages);
        mMessages.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return false;
                final int val = Integer.parseInt((String) newValue);
                final boolean allowMessages = val != SOURCE_NONE;
                final int allowMessagesFrom =
                        val == SOURCE_NONE ? mPolicy.priorityMessageSenders : val;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_MESSAGES, val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowMessages=" + allowMessages
                        + " allowMessagesFrom=" + ZenModeConfig.sourceToString(allowMessagesFrom));
                savePolicy(
                        getNewPriorityCategories(allowMessages, Policy.PRIORITY_CATEGORY_MESSAGES),
                        mPolicy.priorityCallSenders, allowMessagesFrom,
                        mPolicy.suppressedVisualEffects);
                return true;
            }
        });

        mCalls = (DropDownPreference) root.findPreference(KEY_CALLS);
        addSources(mCalls);
        mCalls.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return false;
                final int val = Integer.parseInt((String) newValue);
                final boolean allowCalls = val != SOURCE_NONE;
                final int allowCallsFrom = val == SOURCE_NONE ? mPolicy.priorityCallSenders : val;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_CALLS, val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowCalls=" + allowCalls
                        + " allowCallsFrom=" + ZenModeConfig.sourceToString(allowCallsFrom));
                savePolicy(getNewPriorityCategories(allowCalls, Policy.PRIORITY_CATEGORY_CALLS),
                        allowCallsFrom, mPolicy.priorityMessageSenders,
                        mPolicy.suppressedVisualEffects);
                return true;
            }
        });

        mRepeatCallers = (SwitchPreference) root.findPreference(KEY_REPEAT_CALLERS);
        mRepeatCallers.setSummary(mContext.getString(R.string.zen_mode_repeat_callers_summary,
                mContext.getResources().getInteger(com.android.internal.R.integer
                        .config_zen_repeat_callers_threshold)));
        mRepeatCallers.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_REPEAT_CALLS, val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowRepeatCallers=" + val);
                int priorityCategories = getNewPriorityCategories(val,
                        NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS);
                savePolicy(priorityCategories, mPolicy.priorityCallSenders,
                        mPolicy.priorityMessageSenders, mPolicy.suppressedVisualEffects);
                return true;
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
        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();
        updateControls();
    }

    private void updateControls() {
        mDisableListeners = true;
        if (mCalls != null) {
            mCalls.setValue(Integer.toString(
                    isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_CALLS)
                            ? mPolicy.priorityCallSenders : SOURCE_NONE));
        }
        mMessages.setValue(Integer.toString(
                isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_MESSAGES)
                        ? mPolicy.priorityMessageSenders : SOURCE_NONE));
        mReminders.setChecked(isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_REMINDERS));
        mEvents.setChecked(isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_EVENTS));
        mRepeatCallers.setChecked(
                isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_REPEAT_CALLERS));
        mRepeatCallers.setVisible(!isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_CALLS)
                || mPolicy.priorityCallSenders != Policy.PRIORITY_SENDERS_ANY);
        mDisableListeners = false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_PRIORITY;
    }

    private static void addSources(DropDownPreference pref) {
        pref.setEntries(new CharSequence[]{
                pref.getContext().getString(R.string.zen_mode_from_anyone),
                pref.getContext().getString(R.string.zen_mode_from_contacts),
                pref.getContext().getString(R.string.zen_mode_from_starred),
                pref.getContext().getString(R.string.zen_mode_from_none),
        });
        pref.setEntryValues(new CharSequence[] {
                Integer.toString(Policy.PRIORITY_SENDERS_ANY),
                Integer.toString(Policy.PRIORITY_SENDERS_CONTACTS),
                Integer.toString(Policy.PRIORITY_SENDERS_STARRED),
                Integer.toString(SOURCE_NONE),
        });
    }

    private boolean isPriorityCategoryEnabled(int categoryType) {
        return (mPolicy.priorityCategories & categoryType) != 0;
    }

    private int getNewPriorityCategories(boolean allow, int categoryType) {
        int priorityCategories = mPolicy.priorityCategories;
        if (allow) {
            priorityCategories |= categoryType;
        } else {
            priorityCategories &= ~categoryType;
        }
        return priorityCategories;
    }

    private void savePolicy(int priorityCategories, int priorityCallSenders,
            int priorityMessageSenders, int suppressedVisualEffects) {
        mPolicy = new Policy(priorityCategories, priorityCallSenders, priorityMessageSenders,
                suppressedVisualEffects);
        NotificationManager.from(mContext).setNotificationPolicy(mPolicy);
    }

}
