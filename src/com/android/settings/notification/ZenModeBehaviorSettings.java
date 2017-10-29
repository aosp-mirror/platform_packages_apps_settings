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

package com.android.settings.notification;

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.Arrays;
import java.util.List;

public class ZenModeBehaviorSettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_ALARMS = "zen_mode_alarms";
    private static final String KEY_MEDIA = "zen_mode_media";
    private static final String KEY_REMINDERS = "zen_mode_reminders";
    private static final String KEY_EVENTS = "zen_mode_events";
    private static final String KEY_MESSAGES = "zen_mode_messages";
    private static final String KEY_CALLS = "zen_mode_calls";
    private static final String KEY_REPEAT_CALLERS = "zen_mode_repeat_callers";
    private static final String KEY_SCREEN_OFF = "zen_mode_screen_off";
    private static final String KEY_SCREEN_ON = "zen_mode_screen_on";

    private SwitchPreference mScreenOff;
    private SwitchPreference mScreenOn;

    private static final int SOURCE_NONE = -1;

    private boolean mDisableListeners;
    private SwitchPreference mReminders;
    private SwitchPreference mEvents;
    private DropDownPreference mMessages;
    private DropDownPreference mCalls;
    private SwitchPreference mRepeatCallers;
    private SwitchPreference mAlarms;
    private SwitchPreference mMediaSystemOther;

    private Policy mPolicy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_behavior_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();

        mReminders = (SwitchPreference) root.findPreference(KEY_REMINDERS);
        mReminders.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_REMINDERS,
                        val);
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
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_EVENTS, val);
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
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_MESSAGES, val);
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
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_CALLS, val);
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
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_REPEAT_CALLS,
                        val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowRepeatCallers=" + val);
                int priorityCategories = getNewPriorityCategories(val,
                        Policy.PRIORITY_CATEGORY_REPEAT_CALLERS);
                savePolicy(priorityCategories, mPolicy.priorityCallSenders,
                        mPolicy.priorityMessageSenders, mPolicy.suppressedVisualEffects);
                return true;
            }
        });

        mAlarms = (SwitchPreference) root.findPreference(KEY_ALARMS);
        mAlarms.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_ALARMS, val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowAlarms=" + val);
                savePolicy(getNewPriorityCategories(val, Policy.PRIORITY_CATEGORY_ALARMS),
                        mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                        mPolicy.suppressedVisualEffects);
                return true;
            }
        });

        mMediaSystemOther = (SwitchPreference) root.findPreference(KEY_MEDIA);
        mMediaSystemOther.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                mMetricsFeatureProvider.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_MEDIA, val);
                if (DEBUG) Log.d(TAG, "onPrefChange allowMediaSystemOther=" + val);
                savePolicy(getNewPriorityCategories(val,
                        Policy.PRIORITY_CATEGORY_MEDIA_SYSTEM_OTHER),
                        mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                        mPolicy.suppressedVisualEffects);
                return true;
            }
        });

        mScreenOff = (SwitchPreference) root.findPreference(KEY_SCREEN_OFF);
        if (!getResources()
                .getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            mScreenOff.setSummary(R.string.zen_mode_screen_off_summary_no_led);
        }

        mScreenOff.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean bypass = (Boolean) newValue;
                mMetricsFeatureProvider.action(mContext,
                        MetricsEvent.ACTION_ZEN_ALLOW_WHEN_SCREEN_OFF, !bypass);
                if (DEBUG) Log.d(TAG, "onPrefChange suppressWhenScreenOff=" + !bypass);
                savePolicy(mPolicy.priorityCategories,
                        mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                        getNewSuppressedEffects(!bypass, Policy.SUPPRESSED_EFFECT_SCREEN_OFF));
                return true;
            }
        });

        mScreenOn = (SwitchPreference) root.findPreference(KEY_SCREEN_ON);
        mScreenOn.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean bypass = (Boolean) newValue;
                mMetricsFeatureProvider.action(mContext,
                        MetricsEvent.ACTION_ZEN_ALLOW_WHEN_SCREEN_ON, bypass);
                if (DEBUG) Log.d(TAG, "onPrefChange allowWhenScreenOn=" + !bypass);
                savePolicy(mPolicy.priorityCategories,
                        mPolicy.priorityCallSenders, mPolicy.priorityMessageSenders,
                        getNewSuppressedEffects(!bypass, Policy.SUPPRESSED_EFFECT_SCREEN_ON));
                return true;
            }
        });

        updateControls();
    }

    @Override
    protected void onZenModeChanged() {
        updateControls();
    }

    @Override
    protected void onZenModeConfigChanged() {
        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();
        updateControls();
    }

    private void updateControlsPolicy() {
        if (mCalls != null) {
            mCalls.setValue(Integer.toString(
                    isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_CALLS)
                            ? mPolicy.priorityCallSenders : SOURCE_NONE));
        }
        mMessages.setValue(Integer.toString(
                isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_MESSAGES)
                        ? mPolicy.priorityMessageSenders : SOURCE_NONE));
        mAlarms.setChecked(isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_ALARMS));
        mMediaSystemOther.setChecked(isPriorityCategoryEnabled(
                Policy.PRIORITY_CATEGORY_MEDIA_SYSTEM_OTHER));
        mReminders.setChecked(isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_REMINDERS));
        mEvents.setChecked(isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_EVENTS));
        mRepeatCallers.setChecked(
                isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_REPEAT_CALLERS));
        mRepeatCallers.setVisible(!isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_CALLS)
                || mPolicy.priorityCallSenders != Policy.PRIORITY_SENDERS_ANY);

    }

    private void updateControls() {
        mDisableListeners = true;
        switch(mZenMode) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                toggleBasicNoInterruptions();
                mAlarms.setChecked(false);
                mMediaSystemOther.setChecked(false);
                setTogglesEnabled(false);
                break;
            case Settings.Global.ZEN_MODE_ALARMS:
                toggleBasicNoInterruptions();
                mAlarms.setChecked(true);
                mMediaSystemOther.setChecked(true);
                setTogglesEnabled(false);
                break;
            default:
                updateControlsPolicy();
                setTogglesEnabled(true);
        }

        mScreenOff.setChecked(isEffectAllowed(Policy.SUPPRESSED_EFFECT_SCREEN_OFF));
        mScreenOn.setChecked(isEffectAllowed(Policy.SUPPRESSED_EFFECT_SCREEN_ON));

        mDisableListeners = false;
    }

    private void toggleBasicNoInterruptions() {
        if (mCalls != null) {
            mCalls.setValue(Integer.toString(SOURCE_NONE));
        }
        mMessages.setValue(Integer.toString(SOURCE_NONE));
        mReminders.setChecked(false);
        mEvents.setChecked(false);
        mRepeatCallers.setChecked(false);
    }

    private void setTogglesEnabled(boolean enable) {
        if (mCalls != null) {
            mCalls.setEnabled(enable);
        }
        mMessages.setEnabled(enable);
        mReminders.setEnabled(enable);
        mEvents.setEnabled(enable);
        mRepeatCallers.setEnabled(enable);
        mAlarms.setEnabled(enable);
        mMediaSystemOther.setEnabled(enable);
    }

    @Override
    public int getMetricsCategory() {
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

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.zen_mode_behavior_settings;
                    return Arrays.asList(sir);
                }
            };

    private int getNewSuppressedEffects(boolean suppress, int effectType) {
        int effects = mPolicy.suppressedVisualEffects;
        if (suppress) {
            effects |= effectType;
        } else {
            effects &= ~effectType;
        }
        return effects;
    }

    private boolean isEffectAllowed(int effect) {
        return (mPolicy.suppressedVisualEffects & effect) == 0;
    }
}
