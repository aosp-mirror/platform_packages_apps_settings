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
package com.android.settings.applications.appinfo;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.applications.AppInfoWithHeader;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateManageExternalStorageBridge;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Class for displaying app info related to {@link AppOpsManager#OP_MANAGE_EXTERNAL_STORAGE}.
 */
public class ManageExternalStorageDetails extends AppInfoWithHeader implements
        OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String KEY_APP_OPS_SETTINGS_SWITCH = "app_ops_settings_switch";

    private AppStateManageExternalStorageBridge mBridge;
    private AppOpsManager mAppOpsManager;
    private TwoStatePreference mSwitchPref;
    private PermissionState mPermissionState;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getActivity();
        mBridge = new AppStateManageExternalStorageBridge(context, mState, null);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        // initialize preferences
        addPreferencesFromResource(R.xml.manage_external_storage_permission_details);
        mSwitchPref = findPreference(KEY_APP_OPS_SETTINGS_SWITCH);

        // install event listeners
        mSwitchPref.setOnPreferenceChangeListener(this);

        mMetricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        // if we don't have a package info, show a page saying this is unsupported
        if (mPackageInfo == null) {
            return inflater.inflate(R.layout.manage_applications_apps_unsupported, null);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBridge.release();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSwitchPref) {
            if (mPermissionState != null && !newValue.equals(mPermissionState.isPermissible())) {
                setManageExternalStorageState((Boolean) newValue);
                refreshUi();
            }
            return true;
        }
        return false;
    }

    /**
     * Toggles {@link AppOpsManager#OP_MANAGE_EXTERNAL_STORAGE} for the app.
     */
    private void setManageExternalStorageState(boolean newState) {
        logSpecialPermissionChange(newState, mPackageName);
        mAppOpsManager.setUidMode(AppOpsManager.OP_MANAGE_EXTERNAL_STORAGE,
                mPackageInfo.applicationInfo.uid, newState
                        ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
    }

    private void logSpecialPermissionChange(boolean newState, String packageName) {
        int logCategory = newState ? SettingsEnums.APP_SPECIAL_PERMISSION_MANAGE_EXT_STRG_ALLOW
                : SettingsEnums.APP_SPECIAL_PERMISSION_MANAGE_EXT_STRG_DENY;

        mMetricsFeatureProvider.action(
                mMetricsFeatureProvider.getAttribution(getActivity()),
                logCategory,
                getMetricsCategory(),
                packageName,
                0 /* value */);
    }

    @Override
    protected boolean refreshUi() {
        if (mPackageInfo == null) {
            return true;
        }

        mPermissionState = mBridge.getManageExternalStoragePermState(mPackageName,
                mPackageInfo.applicationInfo.uid);

        mSwitchPref.setChecked(mPermissionState.isPermissible());

        // you cannot ask a user to grant you a permission you did not have!
        mSwitchPref.setEnabled(mPermissionState.permissionDeclared);

        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MANAGE_EXTERNAL_STORAGE;
    }

    /**
     * Returns the string that states whether whether the app has access to
     * {@link AppOpsManager#OP_MANAGE_EXTERNAL_STORAGE}.
     * <p>This string is used in the "All files access" page that displays all apps requesting
     * {@link android.Manifest.permission#MANAGE_EXTERNAL_STORAGE}
     */
    public static CharSequence getSummary(Context context, AppEntry entry) {
        final PermissionState state;
        if (entry.extraInfo instanceof PermissionState) {
            state = (PermissionState) entry.extraInfo;
        } else {
            state = new AppStateManageExternalStorageBridge(context, null, null)
                    .getManageExternalStoragePermState(entry.info.packageName, entry.info.uid);
        }

        return getSummary(context, state);
    }

    private static CharSequence getSummary(Context context, PermissionState state) {
        return context.getString(state.isPermissible()
                ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed);
    }
}
