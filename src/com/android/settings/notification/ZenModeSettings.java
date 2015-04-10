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
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings.Global;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ScrollView;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;

import java.util.ArrayList;
import java.util.List;

public class ZenModeSettings extends ZenModeSettingsBase
        implements Indexable, SwitchBar.OnSwitchChangeListener {
    private static final String KEY_PRIORITY_SETTINGS = "priority_settings";
    private static final String KEY_AUTOMATION_SETTINGS = "automation_settings";

    private Preference mPrioritySettings;
    private AlertDialog mDialog;
    private SwitchBar mSwitchBar;
    private boolean mShowing;
    private boolean mUpdatingControls;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPrioritySettings = root.findPreference(KEY_PRIORITY_SETTINGS);
        if (!isScheduleSupported(mContext)) {
            removePreference(KEY_AUTOMATION_SETTINGS);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateControls();
        mShowing = true;
    }

    @Override
    public void onPause() {
        mShowing = false;
        super.onPause();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (DEBUG) Log.d(TAG, "onSwitchChanged " + isChecked + " mShowing=" + mShowing
                + " mUpdatingControls=" + mUpdatingControls);
        if (!mShowing || mUpdatingControls) return; // not from the user
        if (isChecked) {
            setZenMode(Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, null);
            showConditionSelection(Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        } else {
            setZenMode(Global.ZEN_MODE_OFF, null);
        }
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

    public static String computeZenModeCaption(Resources res, int zenMode) {
        switch (zenMode) {
            case Global.ZEN_MODE_ALARMS:
                return res.getString(R.string.zen_mode_option_alarms);
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                return res.getString(R.string.zen_mode_option_important_interruptions);
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                return res.getString(R.string.zen_mode_option_no_interruptions);
            default:
                return null;
        }
    }

    private String computeZenModeSummaryLine() {
        final String caption = computeZenModeCaption(getResources(), mZenMode);
        if (caption == null) return null;  // zen mode off
        final String conditionText = ZenModeConfig.getConditionLine1(mContext, mConfig,
                UserHandle.myUserId());
        return getString(R.string.zen_mode_summary_combination, caption, conditionText);
    }

    private void updateControls() {
        if (mSwitchBar != null) {
            final String summaryLine = computeZenModeSummaryLine();
            mUpdatingControls = true;
            mSwitchBar.setChecked(summaryLine != null);
            mUpdatingControls = false;
            mSwitchBar.setSummary(summaryLine);
        }
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

    protected void showConditionSelection(final int zenMode) {
        if (mDialog != null) return;

        final ZenModeConditionSelection zenModeConditionSelection =
                new ZenModeConditionSelection(mContext, zenMode);
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                zenModeConditionSelection.confirmCondition();
                mDialog = null;
            }
        };
        ScrollView scrollView = new ScrollView(mContext);
        scrollView.addView(zenModeConditionSelection);
        mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(computeZenModeCaption(getResources(), zenMode))
                .setView(scrollView)
                .setPositiveButton(R.string.okay, positiveListener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelDialog();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancelDialog();
                    }
                }).create();
        mDialog.show();
    }

    private void cancelDialog() {
        if (DEBUG) Log.d(TAG, "cancelDialog");
        // If not making a decision, reset zen to off.
        setZenMode(Global.ZEN_MODE_OFF, null);
        mDialog = null;
    }

    public static String computeConditionText(Condition c) {
        return !TextUtils.isEmpty(c.line1) ? c.line1
                : !TextUtils.isEmpty(c.summary) ? c.summary
                : "";
    }

    private static SparseArray<String> allKeyTitles(Context context) {
        final SparseArray<String> rt = new SparseArray<String>();
        rt.put(R.string.zen_mode_priority_settings_title, KEY_PRIORITY_SETTINGS);
        rt.put(R.string.zen_mode_automation_settings_title, KEY_AUTOMATION_SETTINGS);
        return rt;
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
                if (!isScheduleSupported(context)) {
                    rt.add(KEY_AUTOMATION_SETTINGS);
                }
                return rt;
            }
        };
}
