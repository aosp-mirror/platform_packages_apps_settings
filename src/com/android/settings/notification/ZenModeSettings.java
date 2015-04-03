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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.util.SparseArray;
import android.widget.ScrollView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class ZenModeSettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_ZEN_MODE = "zen_mode";
    private static final String KEY_PRIORITY_SETTINGS = "priority_settings";
    private static final String KEY_AUTOMATION_SETTINGS = "automation_settings";

    private static final SettingPrefWithCallback PREF_ZEN_MODE = new SettingPrefWithCallback(
            SettingPref.TYPE_GLOBAL, KEY_ZEN_MODE, Global.ZEN_MODE, Global.ZEN_MODE_OFF,
            Global.ZEN_MODE_OFF, Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, Global.ZEN_MODE_ALARMS,
            Global.ZEN_MODE_NO_INTERRUPTIONS) {
        protected String getCaption(Resources res, int value) {
            switch (value) {
                case Global.ZEN_MODE_NO_INTERRUPTIONS:
                    return res.getString(R.string.zen_mode_option_no_interruptions);
                case Global.ZEN_MODE_ALARMS:
                    return res.getString(R.string.zen_mode_option_alarms);
                case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                    return res.getString(R.string.zen_mode_option_important_interruptions);
                default:
                    return res.getString(R.string.zen_mode_option_off);
            }
        }
    };

    private Preference mPrioritySettings;
    private AlertDialog mDialog;

    private static SparseArray<String> allKeyTitles(Context context) {
        final SparseArray<String> rt = new SparseArray<String>();
        rt.put(R.string.zen_mode_option_title, KEY_ZEN_MODE);
        rt.put(R.string.zen_mode_priority_settings_title, KEY_PRIORITY_SETTINGS);
        rt.put(R.string.zen_mode_automation_settings_title, KEY_AUTOMATION_SETTINGS);
        return rt;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected void onZenModeChanged() {
        PREF_ZEN_MODE.update(mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        PREF_ZEN_MODE.init(this);
        PREF_ZEN_MODE.setCallback(new SettingPrefWithCallback.Callback() {
            @Override
            public void onSettingSelected(int value) {
                if (value != Global.ZEN_MODE_OFF) {
                    showConditionSelection(value);
                }
            }
        });

        mPrioritySettings = root.findPreference(KEY_PRIORITY_SETTINGS);
        if (!isDowntimeSupported(mContext)) {
            removePreference(KEY_AUTOMATION_SETTINGS);
        }

        updateControls();
    }

    @Override
    protected void updateControls() {
        updatePrioritySettingsSummary();
    }

    private void updatePrioritySettingsSummary() {
        String s = getResources().getString(R.string.zen_mode_alarms);
        s = appendLowercase(s, mConfig.allowReminders, R.string.zen_mode_reminders);
        s = appendLowercase(s, mConfig.allowEvents, R.string.zen_mode_events);
        s = appendLowercase(s, mConfig.allowCalls, R.string.zen_mode_calls);
        s = appendLowercase(s, mConfig.allowMessages, R.string.zen_mode_messages);
        mPrioritySettings.setSummary(s);
    }

    private String appendLowercase(String s, boolean condition, int resId) {
        if (condition) {
            return getResources().getString(R.string.join_many_items_middle, s,
                    getResources().getString(resId).toLowerCase());
        }
        return s;
    }

    protected void showConditionSelection(final int newSettingsValue) {
        if (mDialog != null) return;

        final ZenModeConditionSelection zenModeConditionSelection =
                new ZenModeConditionSelection(mContext);
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                zenModeConditionSelection.confirmCondition();
                mDialog = null;
            }
        };
        final int oldSettingsValue = PREF_ZEN_MODE.getValue(mContext);
        ScrollView scrollView = new ScrollView(mContext);
        scrollView.addView(zenModeConditionSelection);
        mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(PREF_ZEN_MODE.getCaption(getResources(), newSettingsValue))
                .setView(scrollView)
                .setPositiveButton(R.string.okay, positiveListener)
                .setNegativeButton(R.string.cancel_all_caps, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelDialog(oldSettingsValue);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancelDialog(oldSettingsValue);
                    }
                }).create();
        mDialog.show();
    }

    protected void cancelDialog(int oldSettingsValue) {
        // If not making a decision, reset drop down to current setting.
        PREF_ZEN_MODE.setValueWithoutCallback(mContext, oldSettingsValue);
        mDialog = null;
    }

    // Enable indexing of searchable data
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final SparseArray<String> keyTitles = allKeyTitles(context);
                final int N = keyTitles.size();
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>(N);
                final Resources res = context.getResources();
                for (int i = 0; i < N; i++) {
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.key = keyTitles.valueAt(i);
                    data.title = res.getString(keyTitles.keyAt(i));
                    data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                    result.add(data);
                }
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> rt = new ArrayList<String>();
                if (!isDowntimeSupported(context)) {
                    rt.add(KEY_AUTOMATION_SETTINGS);
                }
                return rt;
            }
        };

    private static class SettingPrefWithCallback extends SettingPref {

        private Callback mCallback;
        private int mValue;

        public SettingPrefWithCallback(int type, String key, String setting, int def,
                int... values) {
            super(type, key, setting, def, values);
        }

        public void setCallback(Callback callback) {
            mCallback = callback;
        }

        @Override
        public void update(Context context) {
            // Avoid callbacks from non-user changes.
            mValue = getValue(context);
            super.update(context);
        }

        @Override
        protected boolean setSetting(Context context, int value) {
            if (value == mValue) return true;
            mValue = value;
            if (mCallback != null) {
                mCallback.onSettingSelected(value);
            }
            return super.setSetting(context, value);
        }

        @Override
        public Preference init(SettingsPreferenceFragment settings) {
            Preference ret = super.init(settings);
            mValue = getValue(settings.getActivity());

            return ret;
        }

        public boolean setValueWithoutCallback(Context context, int value) {
            // Set the current value ahead of time, this way we won't trigger a callback.
            mValue = value;
            return putInt(mType, context.getContentResolver(), mSetting, value);
        }

        public int getValue(Context context) {
            return getInt(mType, context.getContentResolver(), mSetting, mDefault);
        }

        public interface Callback {
            void onSettingSelected(int value);
        }
    }
}
