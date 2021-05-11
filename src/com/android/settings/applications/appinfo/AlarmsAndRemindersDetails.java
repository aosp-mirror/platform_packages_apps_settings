/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.applications.appinfo;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAlarmsAndRemindersBridge;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

/**
 * App specific activity to show details about
 * {@link android.Manifest.permission#SCHEDULE_EXACT_ALARM}.
 */
public class AlarmsAndRemindersDetails extends AppInfoWithHeader
        implements OnPreferenceChangeListener {

    private static final String KEY_SWITCH = "alarms_and_reminders_switch";
    private static final String UNCOMMITTED_STATE_KEY = "uncommitted_state";

    private AppStateAlarmsAndRemindersBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private RestrictedSwitchPreference mSwitchPref;
    private AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState mPermissionState;
    private ActivityManager mActivityManager;
    private volatile Boolean mUncommittedState;

    /**
     * Returns the string that states whether the app has access to
     * {@link android.Manifest.permission#SCHEDULE_EXACT_ALARM}.
     */
    public static int getSummary(Context context, AppEntry entry) {
        final AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState state;
        if (entry.extraInfo instanceof AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState) {
            state = (AppStateAlarmsAndRemindersBridge.AlarmsAndRemindersState) entry.extraInfo;
        } else {
            state = new AppStateAlarmsAndRemindersBridge(context, /*appState=*/null,
                    /*callback=*/null).createPermissionState(entry.info.packageName,
                    entry.info.uid);
        }

        return state.isAllowed() ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getActivity();
        mAppBridge = new AppStateAlarmsAndRemindersBridge(context, mState, /*callback=*/null);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mActivityManager = context.getSystemService(ActivityManager.class);

        if (savedInstanceState != null) {
            mUncommittedState = (Boolean) savedInstanceState.get(UNCOMMITTED_STATE_KEY);
        }
        addPreferencesFromResource(R.xml.alarms_and_reminders);
        mSwitchPref = findPreference(KEY_SWITCH);
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mUncommittedState != null) {
            outState.putObject(UNCOMMITTED_STATE_KEY, mUncommittedState);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean checked = (Boolean) newValue;
        if (preference == mSwitchPref) {
            mUncommittedState = checked;
            refreshUi();
            return true;
        }
        return false;
    }

    private void setCanScheduleAlarms(boolean newState) {
        final int uid = mPackageInfo.applicationInfo.uid;
        mAppOpsManager.setUidMode(AppOpsManager.OPSTR_SCHEDULE_EXACT_ALARM, uid,
                newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
        if (!newState) {
            mActivityManager.killUid(uid,
                    AppOpsManager.OPSTR_SCHEDULE_EXACT_ALARM + " no longer allowed.");
        }
    }

    private void logPermissionChange(boolean newState, String packageName) {
        mMetricsFeatureProvider.action(
                mMetricsFeatureProvider.getAttribution(getActivity()),
                SettingsEnums.ACTION_ALARMS_AND_REMINDERS_TOGGLE,
                getMetricsCategory(),
                packageName,
                newState ? 1 : 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity().isChangingConfigurations()) {
            return;
        }
        if (mPermissionState != null && mUncommittedState != null
                && mUncommittedState != mPermissionState.isAllowed()) {
            if (Settings.AlarmsAndRemindersAppActivity.class.getName().equals(
                    getIntent().getComponent().getClassName())) {
                setResult(mUncommittedState ? RESULT_OK : RESULT_CANCELED);
            }
            setCanScheduleAlarms(mUncommittedState);
            logPermissionChange(mUncommittedState, mPackageName);
            mUncommittedState = null;
        }
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }
        mPermissionState = mAppBridge.createPermissionState(mPackageName,
                mPackageInfo.applicationInfo.uid);
        mSwitchPref.setEnabled(mPermissionState.shouldBeVisible());
        mSwitchPref.setChecked(
                mUncommittedState != null ? mUncommittedState : mPermissionState.isAllowed());
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ALARMS_AND_REMINDERS;
    }
}
