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

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateMediaManagementAppsBridge;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

/**
 * Class for displaying app info related to {@link AppOpsManager#OP_MANAGE_MEDIA}.
 */
public class MediaManagementAppsDetails extends AppInfoWithHeader implements
        OnPreferenceChangeListener {

    private static final String KEY_SWITCH_PREF = "media_management_apps_toggle";

    private AppStateMediaManagementAppsBridge mAppBridge;
    private AppOpsManager mAppOpsManager;
    private TwoStatePreference mSwitchPref;
    private PermissionState mPermissionState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getActivity();
        mAppBridge = new AppStateMediaManagementAppsBridge(context, mState, null /* callback */);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);

        // initialize preferences
        addPreferencesFromResource(R.xml.media_management_apps);
        mSwitchPref = findPreference(KEY_SWITCH_PREF);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean value = (Boolean) newValue;
        if (preference == mSwitchPref) {
            if (mPermissionState != null && value != mPermissionState.isPermissible()) {
                setCanManageMedia(value);
                logPermissionChange(value, mPackageName);
                refreshUi();
            }
            return true;
        }
        return false;
    }

    private void setCanManageMedia(boolean newState) {
        mAppOpsManager.setUidMode(AppOpsManager.OP_MANAGE_MEDIA, mPackageInfo.applicationInfo.uid,
                newState ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
    }

    private void logPermissionChange(boolean newState, String packageName) {
        mMetricsFeatureProvider.action(
                mMetricsFeatureProvider.getAttribution(getActivity()),
                SettingsEnums.ACTION_MEDIA_MANAGEMENT_APPS_TOGGLE,
                getMetricsCategory(),
                packageName,
                newState ? 1 : 0);
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
            return false;
        }

        mPermissionState = mAppBridge.createPermissionState(mPackageName,
                mPackageInfo.applicationInfo.uid);
        mSwitchPref.setEnabled(mPermissionState.permissionDeclared);
        mSwitchPref.setChecked(mPermissionState.isPermissible());
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MEDIA_MANAGEMENT_APPS;
    }

    /**
     * Returns the string that states whether the app has access to
     * {@link android.Manifest.permission#MANAGE_MEDIA}.
     */
    public static int getSummary(Context context, AppEntry entry) {
        final PermissionState state;
        if (entry.extraInfo instanceof PermissionState) {
            state = (PermissionState) entry.extraInfo;
        } else {
            state = new AppStateMediaManagementAppsBridge(context, null /* appState */,
                    null /* callback */).createPermissionState(entry.info.packageName,
                    entry.info.uid);
        }

        return state.isPermissible() ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed;
    }
}
